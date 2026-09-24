package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size as CoilSize
import eu.kanade.tachiyomi.ui.reader.domain.CheckWidePageUseCase
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.util.system.GLUtil
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.nekomanga.presentation.theme.Size

/**
 * Pure stateless Composable rendering a paired dual-page spread in paginated reader viewers.
 * Handles reading direction ordering (RTL/LTR), gap spacing, combined bounding box calculation, and
 * double-spread landscape auto-zooming.
 */
@Composable
fun DoublePageLayout(
    page: ReaderPage,
    extraPage: ReaderPage,
    config: PagerViewerConfigUiModel,
    zoomableState: ZoomableState,
    contentScale: ContentScale,
    doubleClickToZoomListener: DoubleClickToZoomListener,
    constraints: Constraints,
    isReady: Boolean,
    modifier: Modifier = Modifier,
    checkWidePage: CheckWidePageUseCase = remember { CheckWidePageUseCase() },
) {
    val context = LocalContext.current
    val isLTR = (!config.isRtl).xor(config.invertDoublePages)
    val (first, second) = if (isLTR) page to extraPage else extraPage to page

    var firstSize by remember(first) { mutableStateOf<ComposeSize?>(null) }
    var secondSize by remember(second) { mutableStateOf<ComposeSize?>(null) }
    val density = LocalDensity.current
    val gapPx =
        remember(config.doublePageGap, density) {
            with(density) { (Size.tiny * config.doublePageGap).toPx() }
        }

    val viewportWidthPx = constraints.maxWidth.toFloat()
    val viewportHeightPx = constraints.maxHeight.toFloat()
    var autoZoomApplied by
        rememberSaveable(
            page.chapter.chapter.id,
            page.index,
            constraints.maxWidth,
            constraints.maxHeight,
        ) {
            mutableStateOf(false)
        }

    LaunchedEffect(firstSize, secondSize) {
        val fSize = firstSize
        val sSize = secondSize
        if (fSize != null && sSize != null) {
            val totalWidth = fSize.width + sSize.width
            val maxHeight = maxOf(fSize.height, sSize.height)
            zoomableState.setContentLocation(
                ZoomableContentLocation.scaledInsideAndCenterAligned(
                    ComposeSize(totalWidth, maxHeight)
                )
            )
        }
    }

    LaunchedEffect(isReady, config.landscapeZoom, config.imageScaleType, firstSize, secondSize) {
        if (
            !autoZoomApplied &&
                isReady &&
                config.landscapeZoom &&
                config.imageScaleType == 1 &&
                viewportWidthPx > 0f &&
                viewportHeightPx > 0f
        ) {
            val fSize = firstSize
            val sSize = secondSize
            if (fSize != null && sSize != null) {
                val availableColWidth = (viewportWidthPx - gapPx) / 2f
                if (
                    availableColWidth > 0f &&
                        fSize.width > 0f &&
                        sSize.width > 0f &&
                        fSize.height > 0f &&
                        sSize.height > 0f
                ) {
                    val scale1 =
                        minOf(
                            availableColWidth / fSize.width,
                            viewportHeightPx / fSize.height,
                        )
                    val scale2 =
                        minOf(
                            availableColWidth / sSize.width,
                            viewportHeightPx / sSize.height,
                        )
                    val renderedHeight = maxOf(fSize.height * scale1, sSize.height * scale2)
                    val isLandscape =
                        (fSize.width + sSize.width) > maxOf(fSize.height, sSize.height)

                    if (isLandscape && renderedHeight < viewportHeightPx) {
                        val targetScale = (viewportHeightPx / renderedHeight).coerceIn(1f, 3f)
                        if (targetScale > 1.05f) {
                            val isRtl = config.isRtl.xor(config.invertDoublePages)
                            val doublePageZoomType =
                                when (config.zoomStart) {
                                    1 ->
                                        if (isRtl) PagerConfig.ZoomType.Right
                                        else PagerConfig.ZoomType.Left
                                    2 -> PagerConfig.ZoomType.Left
                                    3 -> PagerConfig.ZoomType.Right
                                    else -> PagerConfig.ZoomType.Center
                                }
                            val centroid =
                                when (doublePageZoomType) {
                                    PagerConfig.ZoomType.Right ->
                                        Offset(viewportWidthPx, viewportHeightPx / 2f)
                                    PagerConfig.ZoomType.Left -> Offset(0f, viewportHeightPx / 2f)
                                    PagerConfig.ZoomType.Center ->
                                        Offset(
                                            viewportWidthPx / 2f,
                                            viewportHeightPx / 2f,
                                        )
                                }
                            zoomableState.zoomTo(
                                zoomFactor = targetScale,
                                centroid = centroid,
                            )
                        }
                    }
                    autoZoomApplied = true
                }
            }
        }
    }

    val firstModel =
        remember(first) {
            ImageRequest.Builder(context)
                .data(first)
                .size(CoilSize.ORIGINAL)
                .maxBitmapSize(CoilSize(GLUtil.maxTextureSize, GLUtil.maxTextureSize))
                .precision(Precision.EXACT)
                .crossfade(true)
                .build()
        }
    val secondModel =
        remember(second) {
            ImageRequest.Builder(context)
                .data(second)
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(Size.tiny * config.doublePageGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = firstModel,
                    contentDescription = null,
                    contentScale = contentScale,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onSuccess = { state ->
                        val img = state.result.image
                        if (img.width > 0 && img.height > 0) {
                            firstSize = ComposeSize(img.width.toFloat(), img.height.toFloat())
                            val wasWide = first.fullPage == true
                            val isWide = checkWidePage(first, img.width, img.height)
                            if (isWide && !wasWide) {
                                config.onWidePageDetected?.invoke(first)
                            }
                        }
                    },
                )
                AsyncImage(
                    model = secondModel,
                    contentDescription = null,
                    contentScale = contentScale,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onSuccess = { state ->
                        val img = state.result.image
                        if (img.width > 0 && img.height > 0) {
                            secondSize = ComposeSize(img.width.toFloat(), img.height.toFloat())
                            val wasWide = second.fullPage == true
                            val isWide = checkWidePage(second, img.width, img.height)
                            if (isWide && !wasWide) {
                                config.onWidePageDetected?.invoke(second)
                            }
                        }
                    },
                )
            }
        }
    }
}
