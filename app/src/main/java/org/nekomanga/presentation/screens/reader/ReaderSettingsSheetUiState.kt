package org.nekomanga.presentation.screens.reader

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderSliderPosition
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

const val COLOR_FILTER_ALPHA_MASK: Int = 0xFF000000.toInt()
const val COLOR_FILTER_RED_MASK: Int = 0x00FF0000
const val COLOR_FILTER_GREEN_MASK: Int = 0x0000FF00
const val COLOR_FILTER_BLUE_MASK: Int = 0x000000FF

@Immutable
data class ReaderSettingsUiState(
    val selectedTab: Int = 0,
    val isWebtoon: Boolean = false,
    val hasMargins: Boolean = false,
    val hasCutout: Boolean = false,
    val readingMode: ReadingModeType = ReadingModeType.DEFAULT,
    val orientation: OrientationType = OrientationType.DEFAULT,
    val readerTheme: Int = 0,
    val sliderPosition: ReaderSliderPosition = ReaderSliderPosition.RIGHT,
    val showPageNumber: Boolean = true,
    val keepScreenOn: Boolean = true,
    val alwaysShowChapterTransition: Boolean = true,
    val cropBordersWebtoon: Boolean = false,
    val webtoonSidePadding: Int = 0,
    val webtoonEnableZoomOut: Boolean = false,
    val webtoonNav: Int = 0,
    val webtoonInvert: ViewerNavigation.TappingInvertMode = ViewerNavigation.TappingInvertMode.NONE,
    val webtoonPageLayout: PageLayout = PageLayout.SINGLE_PAGE,
    val webtoonInvertDoublePages: Boolean = false,
    val webtoonPageTransitions: Boolean = false,
    val splitTallImages: Boolean = false,
    val imageScaleType: Int = 1,
    val zoomStart: Int = 1,
    val cropBorders: Boolean = false,
    val pageTransitions: Boolean = false,
    val pagerNav: Int = 0,
    val pagerInvert: ViewerNavigation.TappingInvertMode = ViewerNavigation.TappingInvertMode.NONE,
    val pageLayout: PageLayout = PageLayout.SINGLE_PAGE,
    val invertDoublePages: Boolean = false,
    val doublePageGap: Int = 0,
    val landscapeZoom: Boolean = false,
    val navigateToPan: Boolean = false,
    val doublePageRotate: Boolean = false,
    val doublePageRotateReverse: Boolean = false,
    val pagerCutoutBehavior: Int = 0,
    val grayscale: Boolean = false,
    val invertedColors: Boolean = false,
    val customBrightness: Boolean = false,
    val customBrightnessValue: Int = 0,
    val colorFilter: Boolean = false,
    val colorFilterMode: Int = 0,
    val colorFilterValue: Int = 0,
) {
    val readingModeIndex: Int
        get() = readingMode.prefValue

    val rotationIndex: Int
        get() = orientation.prefValue

    val effectiveCropBorders: Boolean
        get() = if (hasMargins) cropBorders else cropBordersWebtoon

    val isCutoutVisible: Boolean
        get() = imageScaleType in listOf(2, 4, 6) && hasCutout && keepScreenOn

    val colorFilterAlpha: Int
        get() = (colorFilterValue shr 24) and 0xFF

    val colorFilterRed: Int
        get() = (colorFilterValue shr 16) and 0xFF

    val colorFilterGreen: Int
        get() = (colorFilterValue shr 8) and 0xFF

    val colorFilterBlue: Int
        get() = colorFilterValue and 0xFF

    fun withColorComponent(value: Int, mask: Int, bitShift: Int): Int {
        return (value shl bitShift) or (colorFilterValue and mask.inv())
    }
}

sealed interface ReaderSettingsAction {
    // General Settings
    data class SetReadingMode(val type: ReadingModeType) : ReaderSettingsAction

    data class SetOrientation(val type: OrientationType) : ReaderSettingsAction

    data class SetTheme(val themeIndex: Int) : ReaderSettingsAction

    data class SetSliderPosition(val position: ReaderSliderPosition) : ReaderSettingsAction

    data class ToggleShowPageNumber(val show: Boolean) : ReaderSettingsAction

    data class ToggleKeepScreenOn(val keep: Boolean) : ReaderSettingsAction

    data class ToggleAlwaysShowChapterTransition(val show: Boolean) : ReaderSettingsAction

    // Layout Settings - Webtoon
    data class ToggleWebtoonCropBorders(val crop: Boolean) : ReaderSettingsAction

    data class SetWebtoonSidePadding(val padding: Int) : ReaderSettingsAction

    data class ToggleWebtoonEnableZoomOut(val enable: Boolean) : ReaderSettingsAction

    data class SetWebtoonNav(val mode: Int) : ReaderSettingsAction

    data class SetWebtoonInvertTapping(val mode: ViewerNavigation.TappingInvertMode) :
        ReaderSettingsAction

    data class SetWebtoonPageLayout(val layout: PageLayout) : ReaderSettingsAction

    data class ToggleWebtoonInvertDoublePages(val invert: Boolean) : ReaderSettingsAction

    data class ToggleWebtoonPageTransitions(val animate: Boolean) : ReaderSettingsAction

    data class ToggleSplitTallImages(val split: Boolean) : ReaderSettingsAction

    data class ToggleWebtoonDisableGaps(val disabled: Boolean) : ReaderSettingsAction

    // Layout Settings - Paged
    data class SetImageScaleType(val scaleType: Int) : ReaderSettingsAction

    data class SetZoomStart(val zoomStart: Int) : ReaderSettingsAction

    data class ToggleCropBorders(val crop: Boolean) : ReaderSettingsAction

    data class TogglePageTransitions(val animate: Boolean) : ReaderSettingsAction

    data class SetPagerNav(val mode: Int) : ReaderSettingsAction

    data class SetPagerInvertTapping(val mode: ViewerNavigation.TappingInvertMode) :
        ReaderSettingsAction

    data class SetPageLayout(val layout: PageLayout) : ReaderSettingsAction

    data class ToggleInvertDoublePages(val invert: Boolean) : ReaderSettingsAction

    data class SetDoublePageGap(val gap: Int) : ReaderSettingsAction

    data class ToggleZoomDoublePageSpreads(val zoom: Boolean) : ReaderSettingsAction

    data class ToggleNavigateToPan(val navigate: Boolean) : ReaderSettingsAction

    data class ToggleDoublePageRotate(val rotate: Boolean) : ReaderSettingsAction

    data class ToggleDoublePageRotateReverse(val reverse: Boolean) : ReaderSettingsAction

    data class SetPagerCutoutBehavior(val behavior: Int) : ReaderSettingsAction

    // Filter Settings
    data class ToggleGrayscale(val grayscale: Boolean) : ReaderSettingsAction

    data class ToggleInvertedColors(val inverted: Boolean) : ReaderSettingsAction

    data class ToggleCustomBrightness(val enable: Boolean) : ReaderSettingsAction

    data class SetCustomBrightnessValue(val value: Int) : ReaderSettingsAction

    data class ToggleColorFilter(val enable: Boolean) : ReaderSettingsAction

    data class SetColorFilterMode(val mode: Int) : ReaderSettingsAction

    data class SetColorFilterValue(val value: Int) : ReaderSettingsAction

    // Sheet Controls
    object OpenFullSettings : ReaderSettingsAction

    object Dismiss : ReaderSettingsAction
}
