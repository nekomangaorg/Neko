package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toOffset
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.nekomanga.presentation.theme.Size

@Immutable
private data class DoublePageContentLocation(
    private val contentSize: ComposeSize,
    private val alignment: Alignment,
) : ZoomableContentLocation {
    override fun location(layoutSize: ComposeSize, direction: LayoutDirection): Rect {
        val alignedOffset =
            alignment.align(
                size = contentSize.roundToIntSize(),
                space = layoutSize.roundToIntSize(),
                layoutDirection = direction,
            )
        return Rect(
            offset = alignedOffset.toOffset(),
            size = contentSize,
        )
    }
}

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

    val doublePageAlignment =
        remember(config.zoomStart, config.isRtl, config.invertDoublePages) {
            resolveDoublePageAlignment(
                zoomStart = config.zoomStart,
                isRtl = config.isRtl.xor(config.invertDoublePages),
            )
        }

    val fSize = firstSize
    val sSize = secondSize
    val hasBothSizes = fSize != null && sSize != null

    val maxHeight: Float
    val w1: Float
    val w2: Float
    val totalWidth: Float
    if (fSize != null && sSize != null) {
        val mh = maxOf(fSize.height, sSize.height)
        maxHeight = mh
        w1 = if (fSize.height > 0f) fSize.width * (mh / fSize.height) else fSize.width
        w2 = if (sSize.height > 0f) sSize.width * (mh / sSize.height) else sSize.width
        totalWidth = w1 + w2 + gapPx
    } else {
        maxHeight = 0f
        w1 = fSize?.width ?: 0f
        w2 = sSize?.width ?: 0f
        totalWidth = 0f
    }

    val effectiveScale =
        remember(config.imageScaleType, totalWidth, maxHeight, viewportWidthPx, viewportHeightPx) {
            calculateDoublePageScale(
                imageScaleType = config.imageScaleType,
                totalWidth = totalWidth,
                maxHeight = maxHeight,
                viewportWidth = viewportWidthPx,
                viewportHeight = viewportHeightPx,
            )
        }

    val renderedWidthPx = totalWidth * effectiveScale
    val renderedHeightPx = maxHeight * effectiveScale

    val rowAlignment =
        remember(doublePageAlignment, renderedWidthPx, viewportWidthPx) {
            calculateRowAlignment(doublePageAlignment, renderedWidthPx, viewportWidthPx)
        }

    val isTrueSpread =
        first.fullPage == true ||
            second.fullPage == true ||
            (fSize?.let { it.width > it.height } == true) ||
            (sSize?.let { it.width > it.height } == true)
    val shouldAutoZoom =
        remember(
            config.zoomDoublePageSpreads,
            config.landscapeZoom,
            config.imageScaleType,
            isTrueSpread,
            viewportWidthPx,
            viewportHeightPx,
        ) {
            shouldAutoZoomSpread(
                zoomEnabled = config.zoomDoublePageSpreads || config.landscapeZoom,
                imageScaleType = config.imageScaleType,
                isTrueSpread = isTrueSpread,
                viewportWidth = viewportWidthPx,
                viewportHeight = viewportHeightPx,
            )
        }

    LaunchedEffect(
        hasBothSizes,
        renderedWidthPx,
        renderedHeightPx,
        rowAlignment,
        shouldAutoZoom,
        isReady,
        viewportWidthPx,
        viewportHeightPx,
    ) {
        if (!hasBothSizes) return@LaunchedEffect

        zoomableState.contentScale = ContentScale.None
        zoomableState.contentAlignment = rowAlignment
        zoomableState.setContentLocation(
            DoublePageContentLocation(
                contentSize = ComposeSize(renderedWidthPx, renderedHeightPx),
                alignment = rowAlignment,
            )
        )

        if (
            !autoZoomApplied &&
                isReady &&
                shouldAutoZoom &&
                viewportWidthPx > 0f &&
                viewportHeightPx > 0f
        ) {
            @Suppress("DEPRECATION")
            val bounds =
                withTimeoutOrNull(2000L) {
                    snapshotFlow { zoomableState.transformedContentBounds }
                        .filter { !it.isEmpty }
                        .first()
                }

            if (bounds != null) {
                val isLandscape = bounds.width > bounds.height
                if (isLandscape && bounds.height < viewportHeightPx) {
                    val targetScale = (viewportHeightPx / bounds.height).coerceIn(1f, 3f)
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
            val w1Dp: Dp
            val w2Dp: Dp
            val hDp: Dp
            val effectiveGapDp: Dp

            if (hasBothSizes) {
                w1Dp = with(density) { (w1 * effectiveScale).toDp() }
                w2Dp = with(density) { (w2 * effectiveScale).toDp() }
                hDp = with(density) { (renderedHeightPx).toDp() }
                effectiveGapDp = with(density) { (gapPx * effectiveScale).toDp() }
            } else {
                w1Dp = Dp.Unspecified
                w2Dp = Dp.Unspecified
                hDp = Dp.Unspecified
                effectiveGapDp = Size.tiny * config.doublePageGap
            }

            Row(
                modifier =
                    if (hasBothSizes) {
                        Modifier.wrapContentSize(align = rowAlignment, unbounded = true)
                    } else {
                        Modifier.fillMaxSize()
                    },
                horizontalArrangement = Arrangement.spacedBy(effectiveGapDp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val imageScale = if (hasBothSizes) ContentScale.Fit else contentScale
                AsyncImage(
                    model = firstModel,
                    contentDescription = null,
                    contentScale = imageScale,
                    alignment = Alignment.CenterEnd,
                    modifier =
                        if (hasBothSizes) {
                            Modifier.size(width = w1Dp, height = hDp)
                        } else {
                            Modifier.weight(1f).fillMaxHeight()
                        },
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
                    contentScale = imageScale,
                    alignment = Alignment.CenterStart,
                    modifier =
                        if (hasBothSizes) {
                            Modifier.size(width = w2Dp, height = hDp)
                        } else {
                            Modifier.weight(1f).fillMaxHeight()
                        },
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

internal fun calculateDoublePageScale(
    imageScaleType: Int,
    totalWidth: Float,
    maxHeight: Float,
    viewportWidth: Float,
    viewportHeight: Float,
): Float {
    if (totalWidth <= 0f || maxHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) {
        return 1f
    }
    return when (imageScaleType) {
        4 -> viewportHeight / maxHeight // Fit height
        3 -> viewportWidth / totalWidth // Fit width
        5 -> 1f // Original size
        6 ->
            if (maxHeight > totalWidth) viewportWidth / totalWidth
            else viewportHeight / maxHeight // Smart fit
        else ->
            minOf(
                1f,
                viewportWidth / totalWidth,
                viewportHeight / maxHeight,
            ) // Fit screen / Default
    }
}

internal fun shouldAutoZoomSpread(
    zoomEnabled: Boolean,
    imageScaleType: Int,
    isTrueSpread: Boolean,
    viewportWidth: Float,
    viewportHeight: Float,
): Boolean {
    if (!zoomEnabled || imageScaleType != 1) return false
    if (viewportWidth <= 0f || viewportHeight <= 0f) return false
    val isTabletLandscape = (viewportWidth / viewportHeight) >= 1.33f
    return isTrueSpread && !isTabletLandscape
}

internal fun resolveDoublePageAlignment(
    zoomStart: Int,
    isRtl: Boolean,
): Alignment {
    val zoomType =
        when (zoomStart) {
            1 -> if (isRtl) PagerConfig.ZoomType.Right else PagerConfig.ZoomType.Left
            2 -> PagerConfig.ZoomType.Left
            3 -> PagerConfig.ZoomType.Right
            else -> PagerConfig.ZoomType.Center
        }
    return when (zoomType) {
        PagerConfig.ZoomType.Left -> Alignment.CenterStart
        PagerConfig.ZoomType.Right -> Alignment.CenterEnd
        PagerConfig.ZoomType.Center -> Alignment.Center
    }
}

internal fun calculateRowAlignment(
    doublePageAlignment: Alignment,
    renderedWidthPx: Float,
    viewportWidthPx: Float,
): Alignment {
    return if (renderedWidthPx > viewportWidthPx) doublePageAlignment else Alignment.Center
}
