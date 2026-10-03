package org.nekomanga.presentation.screens.reader.viewer

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.tachiyomi.data.coil.RotateWidePageTransformation
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.PagePixels
import eu.kanade.tachiyomi.ui.reader.viewer.SmartBackground
import eu.kanade.tachiyomi.ui.reader.viewer.SmartBackgroundAnalyzer
import eu.kanade.tachiyomi.ui.reader.viewer.half
import eu.kanade.tachiyomi.ui.reader.viewer.mergedPagePixels
import eu.kanade.tachiyomi.ui.reader.viewer.rotated
import eu.kanade.tachiyomi.ui.reader.viewer.scaledTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.logging.TimberKt

/** The page images a pager item shows, for the smart background analysis. */
sealed interface SmartBackgroundSource {
    /** One page, turned a quarter turn when [rotateWide] is set and the page is wide. */
    data class Single(
        val page: ReaderPage,
        val rotateWide: Boolean,
        val rotateReverse: Boolean,
    ) : SmartBackgroundSource

    /** One half of a split double page. */
    data class Split(val page: ReaderPage, val leftHalf: Boolean) : SmartBackgroundSource

    /** Two pages side by side. */
    data class Double(val left: ReaderPage, val right: ReaderPage) : SmartBackgroundSource
}

/** Picks the source the way [PagerPageItem] picks its layout. */
internal fun smartBackgroundSource(
    page: ReaderPage,
    extraPage: ReaderPage?,
    isRtl: Boolean,
    invertDoublePages: Boolean,
    rotateWide: Boolean,
    rotateReverse: Boolean,
): SmartBackgroundSource =
    when {
        extraPage != null -> {
            val isLTR = (!isRtl).xor(invertDoublePages)
            if (isLTR) {
                SmartBackgroundSource.Double(left = page, right = extraPage)
            } else {
                SmartBackgroundSource.Double(left = extraPage, right = page)
            }
        }
        page.firstHalf != null ->
            SmartBackgroundSource.Split(
                page = page,
                leftHalf = shouldShowLeftHalf(firstHalf = page.firstHalf == true, isRtl = isRtl),
            )
        else -> SmartBackgroundSource.Single(page, rotateWide, rotateReverse)
    }

/**
 * Fills the pager item with the background the smart reader themes pick from the page edges. Draws
 * nothing when [baseColor] is null, and only while the page is ready and has no error.
 */
@Composable
fun SmartPageBackground(
    source: SmartBackgroundSource,
    baseColor: Color?,
    isReady: Boolean,
    isError: Boolean,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
) {
    if (baseColor == null) return
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Keyed on the source only, so a theme or orientation change keeps the old background until
    // the new one is ready.
    var background by remember(source) { mutableStateOf<SmartBackground?>(null) }
    LaunchedEffect(source, baseColor, isLandscape, isReady) {
        if (isReady) {
            background =
                withContext(Dispatchers.IO) {
                    loadSmartBackground(source, baseColor.toArgb(), isLandscape)
                }
        }
    }

    val current = background
    if (current == null || !isReady || isError) return
    val top = Color(current.top)
    val bottom = Color(current.bottom)
    Canvas(modifier.fillMaxSize()) {
        if (top == bottom) {
            drawRect(top, colorFilter = colorFilter)
        } else {
            drawRect(
                Brush.verticalGradient(listOf(top, top, bottom, bottom)),
                colorFilter = colorFilter,
            )
        }
    }
}

private fun loadSmartBackground(
    source: SmartBackgroundSource,
    @ColorInt baseColor: Int,
    isLandscape: Boolean,
): SmartBackground {
    val bitmaps = mutableListOf<Bitmap>()
    return try {
        val image =
            when (source) {
                is SmartBackgroundSource.Single ->
                    source.page.decodePixels(bitmaps)?.let { image ->
                        if (
                            source.rotateWide &&
                                RotateWidePageTransformation.shouldRotate(
                                    image.width,
                                    image.height,
                                    rotateWide = true,
                                )
                        ) {
                            val degrees =
                                RotateWidePageTransformation.rotationDegrees(source.rotateReverse)
                            image.rotated(clockwise = degrees > 0f)
                        } else {
                            image
                        }
                    }
                is SmartBackgroundSource.Split ->
                    source.page.decodePixels(bitmaps)?.half(source.leftHalf)
                is SmartBackgroundSource.Double -> {
                    val left = source.left.decodePixels(bitmaps)
                    val right = source.right.decodePixels(bitmaps)
                    if (left != null && right != null) {
                        mergedPagePixels(left, right, fill = AndroidColor.WHITE)
                    } else {
                        null
                    }
                }
            }
        SmartBackgroundAnalyzer.analyze(image, baseColor, isLandscape)
    } catch (e: Exception) {
        TimberKt.e(e) { "Failed to pick the smart reader background" }
        SmartBackground(baseColor, baseColor)
    } catch (e: OutOfMemoryError) {
        TimberKt.e(e) { "Out of memory picking the smart reader background" }
        SmartBackground(baseColor, baseColor)
    } finally {
        bitmaps.forEach { it.recycle() }
    }
}

/**
 * Decodes the page with the sample size from [SmartBackgroundAnalyzer.sampleSizeFor] and returns
 * its pixels in full-size coordinates. Returns null when the page has no stream or BitmapFactory
 * cannot decode it.
 */
private fun ReaderPage.decodePixels(bitmaps: MutableList<Bitmap>): PagePixels? {
    val openStream = stream ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openStream().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options =
        BitmapFactory.Options().apply {
            inSampleSize = SmartBackgroundAnalyzer.sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
    val bitmap = openStream().use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    bitmaps += bitmap
    return BitmapPixels(bitmap).scaledTo(bounds.outWidth, bounds.outHeight)
}

private class BitmapPixels(private val bitmap: Bitmap) : PagePixels {
    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override fun getPixel(x: Int, y: Int): Int = bitmap.getPixel(x, y)
}
