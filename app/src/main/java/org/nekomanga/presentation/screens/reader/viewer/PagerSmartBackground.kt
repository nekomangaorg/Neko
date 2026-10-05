package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import eu.kanade.tachiyomi.data.coil.RotateWidePageTransformation
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.PagePixels
import eu.kanade.tachiyomi.ui.reader.viewer.SmartBackground
import eu.kanade.tachiyomi.ui.reader.viewer.SmartBackgroundAnalyzer
import eu.kanade.tachiyomi.ui.reader.viewer.croppedBorders
import eu.kanade.tachiyomi.ui.reader.viewer.half
import eu.kanade.tachiyomi.ui.reader.viewer.mergedPagePixels
import eu.kanade.tachiyomi.ui.reader.viewer.rotated
import eu.kanade.tachiyomi.ui.reader.viewer.scaledTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.logging.TimberKt

/** The page images a pager item shows, for the smart background analysis. */
sealed interface SmartBackgroundSource {
    /**
     * One page, cropped when [cropBorders] is set, then turned a quarter turn when [rotateWide] is
     * set and the page is wide.
     */
    data class Single(
        val page: ReaderPage,
        val rotateWide: Boolean,
        val rotateReverse: Boolean,
        val cropBorders: Boolean,
    ) : SmartBackgroundSource

    /** One half of a split double page. SplitPageLayout does not crop. */
    data class Split(val page: ReaderPage, val leftHalf: Boolean) : SmartBackgroundSource

    /** Two pages side by side, each cropped when [cropBorders] is set. */
    data class Double(val left: ReaderPage, val right: ReaderPage, val cropBorders: Boolean) :
        SmartBackgroundSource
}

/** Picks the source the way [PagerPageItem] picks its layout. */
internal fun smartBackgroundSource(
    page: ReaderPage,
    extraPage: ReaderPage?,
    isRtl: Boolean,
    invertDoublePages: Boolean,
    rotateWide: Boolean,
    rotateReverse: Boolean,
    cropBorders: Boolean,
): SmartBackgroundSource =
    when {
        extraPage != null -> {
            val isLTR = (!isRtl).xor(invertDoublePages)
            if (isLTR) {
                SmartBackgroundSource.Double(left = page, right = extraPage, cropBorders)
            } else {
                SmartBackgroundSource.Double(left = extraPage, right = page, cropBorders)
            }
        }
        page.firstHalf != null ->
            SmartBackgroundSource.Split(
                page = page,
                leftHalf = shouldShowLeftHalf(firstHalf = page.firstHalf == true, isRtl = isRtl),
            )
        else -> SmartBackgroundSource.Single(page, rotateWide, rotateReverse, cropBorders)
    }

/** The inputs a smart background is picked from. */
data class SmartBackgroundKey(
    val source: SmartBackgroundSource,
    @param:ColorInt val baseColor: Int,
    val isLandscape: Boolean,
    /** Retry generation of the pager item's page, as a retry can load a different image. */
    val retryGeneration: Int,
)

/** A smart background and the inputs it was picked from. */
data class CachedSmartBackground(val key: SmartBackgroundKey, val background: SmartBackground)

/** The page that keeps the background picked for this source. */
private val SmartBackgroundSource.cachePage: ReaderPage
    get() =
        when (this) {
            is SmartBackgroundSource.Single -> page
            is SmartBackgroundSource.Split -> page
            is SmartBackgroundSource.Double -> left
        }

internal fun cachedSmartBackground(key: SmartBackgroundKey): SmartBackground? =
    key.source.cachePage.smartBackground?.takeIf { it.key == key }?.background

/**
 * Returns the background kept for [key], or picks one with [pick] and keeps it on the page. A null
 * from [pick] means the page could not be read. Then this returns the base color and keeps nothing,
 * so the next look at the page tries again.
 */
internal fun smartBackgroundFor(
    key: SmartBackgroundKey,
    pick: () -> SmartBackground?,
): SmartBackground {
    cachedSmartBackground(key)?.let {
        return it
    }
    val picked = pick() ?: return SmartBackground(key.baseColor, key.baseColor)
    key.source.cachePage.smartBackground = CachedSmartBackground(key, picked)
    return picked
}

/**
 * Fills the pager item with the background the smart reader themes pick from the page edges. Draws
 * nothing when [baseColor] is null, and only while the page is ready and has no error.
 */
@Composable
fun SmartPageBackground(
    source: SmartBackgroundSource,
    baseColor: Color?,
    isLandscape: Boolean,
    retryGeneration: Int,
    isReady: Boolean,
    isError: Boolean,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
) {
    if (baseColor == null) return
    val key = SmartBackgroundKey(source, baseColor.toArgb(), isLandscape, retryGeneration)

    // Keyed on the source only, so a theme or orientation change keeps the old background until
    // the new one is ready. A page seen before starts with the background kept for it.
    var background by remember(source) { mutableStateOf(cachedSmartBackground(key)) }
    LaunchedEffect(key, isReady) {
        if (isReady) {
            background =
                withContext(Dispatchers.IO) { smartBackgroundFor(key) { loadSmartBackground(key) } }
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

/**
 * Pixels of a bitmap decoded from a [fullWidth] by [fullHeight] image, which can be a sampled down
 * copy.
 */
internal class DecodedPage(val pixels: PagePixels, val fullWidth: Int, val fullHeight: Int)

/**
 * The image the pager item shows for [source], cropped, turned, split or merged the way its image
 * requests and layout do, and read at full image coordinates. Null when [decode] returns null for a
 * page.
 */
internal fun smartBackgroundImage(
    source: SmartBackgroundSource,
    decode: (ReaderPage) -> DecodedPage?,
): PagePixels? {
    return when (source) {
        is SmartBackgroundSource.Single -> {
            val image = decode(source.page)?.fullSize(source.cropBorders) ?: return null
            if (
                source.rotateWide &&
                    RotateWidePageTransformation.shouldRotate(
                        image.width,
                        image.height,
                        rotateWide = true,
                    )
            ) {
                val degrees = RotateWidePageTransformation.rotationDegrees(source.rotateReverse)
                image.rotated(clockwise = degrees > 0f)
            } else {
                image
            }
        }
        is SmartBackgroundSource.Split ->
            decode(source.page)?.fullSize(cropBorders = false)?.half(source.leftHalf)
        is SmartBackgroundSource.Double -> {
            val left = decode(source.left)?.fullSize(source.cropBorders) ?: return null
            val right = decode(source.right)?.fullSize(source.cropBorders) ?: return null
            mergedPagePixels(left, right)
        }
    }
}

/**
 * Crops the pixels when [cropBorders] is set, then reads them at full image coordinates. The crop
 * runs on the decoded pixels, so on a sampled copy its edges can be a few pixels off from where the
 * image request crops.
 */
private fun DecodedPage.fullSize(cropBorders: Boolean): PagePixels {
    val image = if (cropBorders) pixels.croppedBorders() else pixels
    return image.scaledTo(
        (image.width.toLong() * fullWidth / pixels.width).toInt(),
        (image.height.toLong() * fullHeight / pixels.height).toInt(),
    )
}

/** Picks the background from the page images, or returns null when they could not be read. */
private fun loadSmartBackground(key: SmartBackgroundKey): SmartBackground? {
    val bitmaps = mutableListOf<Bitmap>()
    return try {
        smartBackgroundImage(key.source) { it.decode(bitmaps) }
            ?.let { image ->
                SmartBackgroundAnalyzer.analyze(image, key.baseColor, key.isLandscape)
            }
    } catch (e: Exception) {
        TimberKt.e(e) { "Failed to pick the smart reader background" }
        null
    } catch (e: OutOfMemoryError) {
        TimberKt.e(e) { "Out of memory picking the smart reader background" }
        null
    } finally {
        bitmaps.forEach { it.recycle() }
    }
}

/**
 * Decodes the page with the sample size from [SmartBackgroundAnalyzer.sampleSizeFor]. Returns null
 * when the page has no stream or BitmapFactory cannot decode it.
 */
private fun ReaderPage.decode(bitmaps: MutableList<Bitmap>): DecodedPage? {
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
    return DecodedPage(BitmapPixels(bitmap), bounds.outWidth, bounds.outHeight)
}

private class BitmapPixels(private val bitmap: Bitmap) : PagePixels {
    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override fun getPixel(x: Int, y: Int): Int = bitmap.getPixel(x, y)
}
