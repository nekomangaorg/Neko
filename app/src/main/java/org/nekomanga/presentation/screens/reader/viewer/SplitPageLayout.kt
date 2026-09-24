package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size as CoilSize
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.GLUtil
import kotlin.math.roundToInt
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.zoomable

/**
 * Pure stateless Composable rendering a split double-page half (firstHalf = true or false). In
 * single-page mode with [eu.kanade.tachiyomi.ui.reader.settings.PageLayout.SPLIT_PAGES], crops the
 * designated 50% half based on reading direction.
 */
@Composable
fun SplitPageLayout(
    page: ReaderPage,
    config: PagerViewerConfigUiModel,
    zoomableState: ZoomableState,
    doubleClickToZoomListener: DoubleClickToZoomListener,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // In Japanese manga reading (RTL), page reads from right to left:
    // firstHalf = true is the right half (start of reading), firstHalf = false is the left half.
    // In LTR: firstHalf = true is the left half, firstHalf = false is the right half.
    val isFirstHalf = page.firstHalf == true
    val showLeftHalf = if (config.isRtl) !isFirstHalf else isFirstHalf

    var imageSize by remember(page) { mutableStateOf<ComposeSize?>(null) }

    val model =
        remember(page) {
            ImageRequest.Builder(context)
                .data(page)
                .size(CoilSize.ORIGINAL)
                .maxBitmapSize(CoilSize(GLUtil.maxTextureSize, GLUtil.maxTextureSize))
                .precision(Precision.EXACT)
                .crossfade(true)
                .build()
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .zoomable(
                    state = zoomableState,
                    onDoubleClick = doubleClickToZoomListener,
                ),
        contentAlignment = Alignment.Center,
    ) {
        SplitPageHalfLayout(
            showLeftHalf = showLeftHalf,
            contentScale = contentScale,
            imageSize = imageSize,
        ) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                onSuccess = { state ->
                    val img = state.result.image
                    if (img.width > 0 && img.height > 0) {
                        imageSize = ComposeSize(img.width.toFloat(), img.height.toFloat())
                        zoomableState.setContentLocation(
                            ZoomableContentLocation.scaledInsideAndCenterAligned(
                                ComposeSize(img.width / 2f, img.height.toFloat())
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SplitPageHalfLayout(
    showLeftHalf: Boolean,
    contentScale: ContentScale,
    imageSize: ComposeSize?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.clipToBounds(),
        content = content,
    ) { measurables, constraints ->
        val img = imageSize
        if (
            img == null ||
                img.width <= 0f ||
                img.height <= 0f ||
                constraints.maxWidth <= 0 ||
                constraints.maxHeight <= 0
        ) {
            val placeable = measurables.first().measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            val srcHalfSize = ComposeSize(img.width / 2f, img.height)
            val dstSize =
                ComposeSize(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            val scaleFactor = contentScale.computeScaleFactor(srcHalfSize, dstSize)
            val halfWidth = (srcHalfSize.width * scaleFactor.scaleX).roundToInt()
            val halfHeight = (srcHalfSize.height * scaleFactor.scaleY).roundToInt()
            val fullWidth = halfWidth * 2

            val placeable = measurables.first().measure(Constraints.fixed(fullWidth, halfHeight))

            layout(halfWidth, halfHeight) {
                val x = if (showLeftHalf) 0 else -halfWidth
                placeable.place(x, 0)
            }
        }
    }
}
