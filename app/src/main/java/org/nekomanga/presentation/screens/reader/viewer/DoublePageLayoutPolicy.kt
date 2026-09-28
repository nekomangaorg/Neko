package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig

/** Type-safe representation of Neko reader image scale preferences. */
enum class ReaderScaleType(val value: Int) {
    FitScreen(1),
    Stretch(2),
    FitWidth(3),
    FitHeight(4),
    OriginalSize(5),
    SmartFit(6);

    companion object {
        fun fromPreference(value: Int): ReaderScaleType {
            return entries.firstOrNull { it.value == value } ?: FitScreen
        }
    }
}

/** Type-safe representation of Neko reader zoom start preferences. */
enum class ZoomStartPosition(val value: Int) {
    Auto(1),
    Left(2),
    Right(3),
    Center(4);

    companion object {
        fun fromPreference(value: Int): ZoomStartPosition {
            return entries.firstOrNull { it.value == value } ?: Auto
        }
    }
}

/** Pre-computed dimensions for a paired double-page spread. */
@Immutable
data class DoublePageDimensions(
    val maxHeight: Float,
    val w1: Float,
    val w2: Float,
    val totalWidth: Float,
)

/** Centralized calculation and placement policy for double-page spreads in Compose Pager. */
object DoublePageLayoutPolicy {

    /** Calculates normalized composite dimensions for two pages with a gap. */
    fun calculateDimensions(
        fWidth: Float?,
        fHeight: Float?,
        sWidth: Float?,
        sHeight: Float?,
        gapPx: Float,
    ): DoublePageDimensions {
        if (fWidth != null && fHeight != null && sWidth != null && sHeight != null) {
            val maxHeight = maxOf(fHeight, sHeight)
            val w1 = if (fHeight > 0f) fWidth * (maxHeight / fHeight) else fWidth
            val w2 = if (sHeight > 0f) sWidth * (maxHeight / sHeight) else sWidth
            val totalWidth = w1 + w2 + gapPx
            return DoublePageDimensions(
                maxHeight = maxHeight,
                w1 = w1,
                w2 = w2,
                totalWidth = totalWidth,
            )
        }
        return DoublePageDimensions(
            maxHeight = 0f,
            w1 = fWidth ?: 0f,
            w2 = sWidth ?: 0f,
            totalWidth = 0f,
        )
    }

    /** Calculates the effective scale factor based on scale type and viewport constraints. */
    fun calculateScale(
        scaleType: ReaderScaleType,
        totalWidth: Float,
        maxHeight: Float,
        viewportWidth: Float,
        viewportHeight: Float,
    ): Float {
        if (totalWidth <= 0f || maxHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) {
            return 1f
        }
        return when (scaleType) {
            ReaderScaleType.FitHeight -> viewportHeight / maxHeight
            ReaderScaleType.FitWidth -> viewportWidth / totalWidth
            ReaderScaleType.OriginalSize -> 1f
            ReaderScaleType.SmartFit ->
                if (maxHeight > totalWidth) viewportWidth / totalWidth
                else viewportHeight / maxHeight
            ReaderScaleType.FitScreen,
            ReaderScaleType.Stretch ->
                minOf(1f, viewportWidth / totalWidth, viewportHeight / maxHeight)
        }
    }

    /**
     * Determines whether auto-zoom should be applied to a spread. Auto-zoom is only applied to true
     * wide spreads in Fit Screen mode on narrow (phone) viewports.
     */
    fun shouldAutoZoom(
        zoomEnabled: Boolean,
        scaleType: ReaderScaleType,
        isTrueSpread: Boolean,
        viewportWidth: Float,
        viewportHeight: Float,
    ): Boolean {
        if (!zoomEnabled || scaleType != ReaderScaleType.FitScreen) return false
        if (viewportWidth <= 0f || viewportHeight <= 0f) return false
        val isTabletLandscape = (viewportWidth / viewportHeight) >= 1.33f
        return isTrueSpread && !isTabletLandscape
    }

    /** Resolves the default horizontal alignment based on reading direction and zoom start. */
    fun resolveAlignment(
        zoomStart: ZoomStartPosition,
        isRtl: Boolean,
    ): Alignment {
        val zoomType =
            when (zoomStart) {
                ZoomStartPosition.Auto ->
                    if (isRtl) PagerConfig.ZoomType.Right else PagerConfig.ZoomType.Left
                ZoomStartPosition.Left -> PagerConfig.ZoomType.Left
                ZoomStartPosition.Right -> PagerConfig.ZoomType.Right
                ZoomStartPosition.Center -> PagerConfig.ZoomType.Center
            }
        return when (zoomType) {
            PagerConfig.ZoomType.Left -> Alignment.CenterStart
            PagerConfig.ZoomType.Right -> Alignment.CenterEnd
            PagerConfig.ZoomType.Center -> Alignment.Center
        }
    }

    /**
     * Computes the alignment for the Row container.
     * - If overflowing horizontally and vertically: aligns to reading edge and top.
     * - If overflowing horizontally: aligns to reading edge, vertically centered.
     * - If overflowing vertically: aligns to top, horizontally centered.
     * - If fitting inside viewport: centered both ways.
     */
    fun calculateRowAlignment(
        doublePageAlignment: Alignment,
        renderedWidthPx: Float,
        renderedHeightPx: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): Alignment {
        val isHorizontalOverflow = renderedWidthPx > viewportWidthPx
        val isVerticalOverflow = renderedHeightPx > viewportHeightPx

        return when {
            isHorizontalOverflow && isVerticalOverflow -> {
                when (doublePageAlignment) {
                    Alignment.CenterStart -> Alignment.TopStart
                    Alignment.CenterEnd -> Alignment.TopEnd
                    else -> Alignment.TopCenter
                }
            }
            isHorizontalOverflow -> doublePageAlignment
            isVerticalOverflow -> Alignment.TopCenter
            else -> Alignment.Center
        }
    }
}
