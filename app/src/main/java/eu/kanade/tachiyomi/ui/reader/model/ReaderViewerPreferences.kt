package eu.kanade.tachiyomi.ui.reader.model

import androidx.compose.runtime.Immutable

/**
 * Immutable domain representation of hoisted viewer preferences. Decouples Compose viewers from
 * Service Locator lookups and inline preference observations.
 */
@Immutable
data class ReaderViewerPreferences(
    val animatedTransitions: Boolean = true,
    val animatedTransitionsWebtoon: Boolean = true,
    val imageScaleType: Int = 1,
    val doublePageGap: Int = 0,
    val invertDoublePages: Boolean = false,
    val readerTheme: Int = 1,
    val landscapeZoom: Boolean = false,
    val zoomStart: Int = 1,
    val preloadPageAmount: Int = 4,
    val cropBorders: Boolean = false,
    val cropBordersWebtoon: Boolean = false,
    val grayscale: Boolean = false,
    val invertedColors: Boolean = false,
    val doublePageRotate: Boolean = false,
    val doublePageRotateReverse: Boolean = false,
    val navigateToPan: Boolean = true,
    val webtoonSidePadding: Int = 0,
    val webtoonDisableGaps: Boolean = false,
    val webtoonEnableZoomOut: Boolean = false,
)
