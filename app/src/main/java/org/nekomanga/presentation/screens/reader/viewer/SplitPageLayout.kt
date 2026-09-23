package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size as CoilSize
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.GLUtil
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // In Japanese manga reading (RTL), page reads from right to left:
    // firstHalf = true is the right half (start of reading), firstHalf = false is the left half.
    // In LTR: firstHalf = true is the left half, firstHalf = false is the right half.
    val isFirstHalf = page.firstHalf == true
    val showLeftHalf = if (config.isRtl) !isFirstHalf else isFirstHalf
    val alignment = if (showLeftHalf) Alignment.CenterStart else Alignment.CenterEnd

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
                .clipToBounds()
                .zoomable(
                    state = zoomableState,
                    onDoubleClick = doubleClickToZoomListener,
                ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            alignment = alignment,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
