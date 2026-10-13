package org.nekomanga.presentation.screens.reader

import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderSliderPosition
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSettingsSheetUiStateTest {

    @Test
    fun `given default ReaderSettingsUiState, verify default properties`() {
        val state = ReaderSettingsUiState()

        assertEquals(0, state.selectedTab)
        assertFalse(state.isWebtoon)
        assertFalse(state.hasMargins)
        assertFalse(state.hasCutout)
        assertEquals(ReadingModeType.DEFAULT, state.readingMode)
        assertEquals(OrientationType.DEFAULT, state.orientation)
        assertEquals(0, state.readingModeIndex)
        assertEquals(0, state.rotationIndex)
        assertEquals(0, state.readerTheme)
        assertEquals(ReaderSliderPosition.RIGHT, state.sliderPosition)
        assertTrue(state.showPageNumber)
        assertTrue(state.keepScreenOn)
        assertTrue(state.alwaysShowChapterTransition)
        assertFalse(state.cropBordersWebtoon)
        assertEquals(0, state.webtoonSidePadding)
        assertFalse(state.webtoonEnableZoomOut)
        assertEquals(0, state.webtoonNav)
        assertEquals(ViewerNavigation.TappingInvertMode.NONE, state.webtoonInvert)
        assertEquals(PageLayout.SINGLE_PAGE, state.webtoonPageLayout)
        assertFalse(state.webtoonInvertDoublePages)
        assertFalse(state.webtoonPageTransitions)
        assertFalse(state.splitTallImages)
        assertEquals(1, state.imageScaleType)
        assertEquals(1, state.zoomStart)
        assertFalse(state.cropBorders)
        assertFalse(state.pageTransitions)
        assertEquals(0, state.pagerNav)
        assertEquals(ViewerNavigation.TappingInvertMode.NONE, state.pagerInvert)
        assertEquals(PageLayout.SINGLE_PAGE, state.pageLayout)
        assertFalse(state.invertDoublePages)
        assertEquals(0, state.doublePageGap)
        assertFalse(state.landscapeZoom)
        assertFalse(state.navigateToPan)
        assertFalse(state.doublePageRotate)
        assertFalse(state.doublePageRotateReverse)
        assertEquals(0, state.pagerCutoutBehavior)
        assertFalse(state.grayscale)
        assertFalse(state.invertedColors)
        assertFalse(state.customBrightness)
        assertEquals(0, state.customBrightnessValue)
        assertFalse(state.colorFilter)
        assertEquals(0, state.colorFilterMode)
        assertEquals(0, state.colorFilterValue)
    }

    @Test
    fun `given ReaderSettingsUiState, when copied, updates properties while preserving others`() {
        val original = ReaderSettingsUiState()
        val modified =
            original.copy(
                isWebtoon = true,
                selectedTab = 1,
                readerTheme = 2,
                grayscale = true,
                webtoonSidePadding = 15,
                cropBorders = true,
                cropBordersWebtoon = false,
            )

        assertTrue(modified.isWebtoon)
        assertEquals(1, modified.selectedTab)
        assertEquals(2, modified.readerTheme)
        assertTrue(modified.grayscale)
        assertEquals(15, modified.webtoonSidePadding)
        assertTrue(modified.cropBorders)
        assertFalse(modified.cropBordersWebtoon)
        assertNotEquals(original, modified)
    }

    @Test
    fun `given identical ReaderSettingsUiState, equals and hashCode contract is satisfied`() {
        val state1 =
            ReaderSettingsUiState(
                isWebtoon = true,
                selectedTab = 2,
                readingMode = ReadingModeType.WEBTOON,
                orientation = OrientationType.LOCKED_PORTRAIT,
                readerTheme = 2,
                grayscale = true,
                customBrightness = true,
                customBrightnessValue = -10,
                colorFilter = true,
                colorFilterValue = 0x7F112233,
            )
        val state2 =
            ReaderSettingsUiState(
                isWebtoon = true,
                selectedTab = 2,
                readingMode = ReadingModeType.WEBTOON,
                orientation = OrientationType.LOCKED_PORTRAIT,
                readerTheme = 2,
                grayscale = true,
                customBrightness = true,
                customBrightnessValue = -10,
                colorFilter = true,
                colorFilterValue = 0x7F112233,
            )

        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
        assertEquals(ReadingModeType.WEBTOON.prefValue, state1.readingModeIndex)
        assertEquals(OrientationType.LOCKED_PORTRAIT.prefValue, state1.rotationIndex)
    }

    @Test
    fun `given base ReaderSettingsUiState, modifying any individual property produces unequal instance`() {
        val base = ReaderSettingsUiState()

        assertNotEquals(base, base.copy(selectedTab = 1))
        assertNotEquals(base, base.copy(isWebtoon = true))
        assertNotEquals(base, base.copy(hasMargins = true))
        assertNotEquals(base, base.copy(hasCutout = true))
        assertNotEquals(base, base.copy(readingMode = ReadingModeType.WEBTOON))
        assertNotEquals(base, base.copy(orientation = OrientationType.LOCKED_PORTRAIT))
        assertNotEquals(base, base.copy(readerTheme = 2))
        assertNotEquals(base, base.copy(sliderPosition = ReaderSliderPosition.LEFT))
        assertNotEquals(base, base.copy(showPageNumber = false))
        assertNotEquals(base, base.copy(keepScreenOn = false))
        assertNotEquals(base, base.copy(alwaysShowChapterTransition = false))
        assertNotEquals(base, base.copy(cropBordersWebtoon = true))
        assertNotEquals(base, base.copy(webtoonSidePadding = 10))
        assertNotEquals(base, base.copy(webtoonEnableZoomOut = true))
        assertNotEquals(base, base.copy(webtoonNav = 1))
        assertNotEquals(base, base.copy(webtoonInvert = ViewerNavigation.TappingInvertMode.BOTH))
        assertNotEquals(base, base.copy(webtoonPageLayout = PageLayout.SPLIT_PAGES))
        assertNotEquals(base, base.copy(webtoonInvertDoublePages = true))
        assertNotEquals(base, base.copy(webtoonPageTransitions = true))
        assertNotEquals(base, base.copy(splitTallImages = true))
        assertNotEquals(base, base.copy(imageScaleType = 2))
        assertNotEquals(base, base.copy(zoomStart = 2))
        assertNotEquals(base, base.copy(cropBorders = true))
        assertNotEquals(base, base.copy(pageTransitions = true))
        assertNotEquals(base, base.copy(pagerNav = 1))
        assertNotEquals(base, base.copy(pagerInvert = ViewerNavigation.TappingInvertMode.BOTH))
        assertNotEquals(base, base.copy(pageLayout = PageLayout.DOUBLE_PAGES))
        assertNotEquals(base, base.copy(invertDoublePages = true))
        assertNotEquals(base, base.copy(doublePageGap = 5))
        assertNotEquals(base, base.copy(landscapeZoom = true))
        assertNotEquals(base, base.copy(navigateToPan = true))
        assertNotEquals(base, base.copy(doublePageRotate = true))
        assertNotEquals(base, base.copy(doublePageRotateReverse = true))
        assertNotEquals(base, base.copy(pagerCutoutBehavior = 1))
        assertNotEquals(base, base.copy(grayscale = true))
        assertNotEquals(base, base.copy(invertedColors = true))
        assertNotEquals(base, base.copy(customBrightness = true))
        assertNotEquals(base, base.copy(customBrightnessValue = 25))
        assertNotEquals(base, base.copy(colorFilter = true))
        assertNotEquals(base, base.copy(colorFilterMode = 2))
        assertNotEquals(base, base.copy(colorFilterValue = 0x12345678))
    }

    @Test
    fun `given colorFilterValue, verify channel components are correctly extracted`() {
        // Color: ARGB = 128 (0x80), 64 (0x40), 32 (0x20), 16 (0x10)
        // 0x80402010.toInt()
        val colorValue = ((0x80 shl 24) or (0x40 shl 16) or (0x20 shl 8) or 0x10)
        val state = ReaderSettingsUiState(colorFilterValue = colorValue)

        assertEquals(0x80, state.colorFilterAlpha)
        assertEquals(0x40, state.colorFilterRed)
        assertEquals(0x20, state.colorFilterGreen)
        assertEquals(0x10, state.colorFilterBlue)
    }

    @Test
    fun `given colorFilterValue at extreme boundaries, verify channel components are correctly unpacked`() {
        val stateZero = ReaderSettingsUiState(colorFilterValue = 0)
        assertEquals(0, stateZero.colorFilterAlpha)
        assertEquals(0, stateZero.colorFilterRed)
        assertEquals(0, stateZero.colorFilterGreen)
        assertEquals(0, stateZero.colorFilterBlue)

        val stateMax = ReaderSettingsUiState(colorFilterValue = -1) // 0xFFFFFFFF
        assertEquals(255, stateMax.colorFilterAlpha)
        assertEquals(255, stateMax.colorFilterRed)
        assertEquals(255, stateMax.colorFilterGreen)
        assertEquals(255, stateMax.colorFilterBlue)
    }

    @Test
    fun `given withColorComponent, verify updating individual channel preserves other channels`() {
        val initialColor = ((0xAA shl 24) or (0xBB shl 16) or (0xCC shl 8) or 0xDD)
        val state = ReaderSettingsUiState(colorFilterValue = initialColor)

        // Update Alpha to 0xFF
        val withNewAlpha = state.withColorComponent(0xFF, COLOR_FILTER_ALPHA_MASK, 24)
        val stateAlpha = state.copy(colorFilterValue = withNewAlpha)
        assertEquals(0xFF, stateAlpha.colorFilterAlpha)
        assertEquals(0xBB, stateAlpha.colorFilterRed)
        assertEquals(0xCC, stateAlpha.colorFilterGreen)
        assertEquals(0xDD, stateAlpha.colorFilterBlue)

        // Update Red to 0x11
        val withNewRed = state.withColorComponent(0x11, COLOR_FILTER_RED_MASK, 16)
        val stateRed = state.copy(colorFilterValue = withNewRed)
        assertEquals(0xAA, stateRed.colorFilterAlpha)
        assertEquals(0x11, stateRed.colorFilterRed)
        assertEquals(0xCC, stateRed.colorFilterGreen)
        assertEquals(0xDD, stateRed.colorFilterBlue)

        // Update Green to 0x22
        val withNewGreen = state.withColorComponent(0x22, COLOR_FILTER_GREEN_MASK, 8)
        val stateGreen = state.copy(colorFilterValue = withNewGreen)
        assertEquals(0xAA, stateGreen.colorFilterAlpha)
        assertEquals(0xBB, stateGreen.colorFilterRed)
        assertEquals(0x22, stateGreen.colorFilterGreen)
        assertEquals(0xDD, stateGreen.colorFilterBlue)

        // Update Blue to 0x33
        val withNewBlue = state.withColorComponent(0x33, COLOR_FILTER_BLUE_MASK, 0)
        val stateBlue = state.copy(colorFilterValue = withNewBlue)
        assertEquals(0xAA, stateBlue.colorFilterAlpha)
        assertEquals(0xBB, stateBlue.colorFilterRed)
        assertEquals(0xCC, stateBlue.colorFilterGreen)
        assertEquals(0x33, stateBlue.colorFilterBlue)
    }

    @Test
    fun `given effectiveCropBorders, when hasMargins is true uses cropBorders else cropBordersWebtoon`() {
        val marginsTrue =
            ReaderSettingsUiState(
                hasMargins = true,
                cropBorders = true,
                cropBordersWebtoon = false,
            )
        assertTrue(marginsTrue.effectiveCropBorders)

        val marginsFalse =
            ReaderSettingsUiState(
                hasMargins = false,
                cropBorders = true,
                cropBordersWebtoon = false,
            )
        assertFalse(marginsFalse.effectiveCropBorders)

        val marginsFalseWebtoonCrop =
            ReaderSettingsUiState(
                hasMargins = false,
                cropBorders = false,
                cropBordersWebtoon = true,
            )
        assertTrue(marginsFalseWebtoonCrop.effectiveCropBorders)

        val marginsTrueWebtoonCrop =
            ReaderSettingsUiState(
                hasMargins = true,
                cropBorders = false,
                cropBordersWebtoon = true,
            )
        assertFalse(marginsTrueWebtoonCrop.effectiveCropBorders)
    }

    @Test
    fun `given isCutoutVisible, verify complete scaleType partition matrix`() {
        // scaleType 1 (Fit screen), 3 (Fit width), 5 (Stretch), 7 (None) should be false even with
        // cutout and keepScreenOn
        listOf(1, 3, 5, 7).forEach { scaleType ->
            assertFalse(
                "ScaleType $scaleType should not show cutout behavior",
                ReaderSettingsUiState(
                        imageScaleType = scaleType,
                        hasCutout = true,
                        keepScreenOn = true,
                    )
                    .isCutoutVisible,
            )
        }

        // Full fits (2 = Fit height, 4 = Original, 6 = Smart fit) should be true when both cutout
        // and keepScreenOn are true
        listOf(2, 4, 6).forEach { scaleType ->
            assertTrue(
                "ScaleType $scaleType should show cutout behavior when cutout and keepScreenOn are present",
                ReaderSettingsUiState(
                        imageScaleType = scaleType,
                        hasCutout = true,
                        keepScreenOn = true,
                    )
                    .isCutoutVisible,
            )

            // When hasCutout is false -> false
            assertFalse(
                "ScaleType $scaleType should not show cutout behavior when hasCutout is false",
                ReaderSettingsUiState(
                        imageScaleType = scaleType,
                        hasCutout = false,
                        keepScreenOn = true,
                    )
                    .isCutoutVisible,
            )

            // When keepScreenOn is false -> false
            assertFalse(
                "ScaleType $scaleType should not show cutout behavior when keepScreenOn is false",
                ReaderSettingsUiState(
                        imageScaleType = scaleType,
                        hasCutout = true,
                        keepScreenOn = false,
                    )
                    .isCutoutVisible,
            )
        }
    }

    @Test
    fun `given ReaderSettingsAction variants, verify correct payload equality and hash code`() {
        // General
        assertEquals(
            ReaderSettingsAction.SetReadingMode(ReadingModeType.WEBTOON),
            ReaderSettingsAction.SetReadingMode(ReadingModeType.WEBTOON),
        )
        assertEquals(
            ReaderSettingsAction.SetReadingMode(ReadingModeType.WEBTOON).hashCode(),
            ReaderSettingsAction.SetReadingMode(ReadingModeType.WEBTOON).hashCode(),
        )
        assertNotEquals(
            ReaderSettingsAction.SetReadingMode(ReadingModeType.WEBTOON),
            ReaderSettingsAction.SetReadingMode(ReadingModeType.RIGHT_TO_LEFT),
        )

        assertEquals(
            ReaderSettingsAction.SetOrientation(OrientationType.LOCKED_PORTRAIT),
            ReaderSettingsAction.SetOrientation(OrientationType.LOCKED_PORTRAIT),
        )
        assertNotEquals(
            ReaderSettingsAction.SetOrientation(OrientationType.LOCKED_PORTRAIT),
            ReaderSettingsAction.SetOrientation(OrientationType.LOCKED_LANDSCAPE),
        )

        assertEquals(
            ReaderSettingsAction.SetTheme(2),
            ReaderSettingsAction.SetTheme(2),
        )
        assertNotEquals(
            ReaderSettingsAction.SetTheme(2),
            ReaderSettingsAction.SetTheme(1),
        )

        assertEquals(
            ReaderSettingsAction.SetSliderPosition(ReaderSliderPosition.LEFT),
            ReaderSettingsAction.SetSliderPosition(ReaderSliderPosition.LEFT),
        )
        assertNotEquals(
            ReaderSettingsAction.SetSliderPosition(ReaderSliderPosition.LEFT),
            ReaderSettingsAction.SetSliderPosition(ReaderSliderPosition.RIGHT),
        )

        assertEquals(
            ReaderSettingsAction.ToggleShowPageNumber(true),
            ReaderSettingsAction.ToggleShowPageNumber(true),
        )
        assertNotEquals(
            ReaderSettingsAction.ToggleShowPageNumber(true),
            ReaderSettingsAction.ToggleShowPageNumber(false),
        )

        assertEquals(
            ReaderSettingsAction.ToggleKeepScreenOn(true),
            ReaderSettingsAction.ToggleKeepScreenOn(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleAlwaysShowChapterTransition(true),
            ReaderSettingsAction.ToggleAlwaysShowChapterTransition(true),
        )

        // Webtoon
        assertEquals(
            ReaderSettingsAction.ToggleWebtoonCropBorders(true),
            ReaderSettingsAction.ToggleWebtoonCropBorders(true),
        )
        assertEquals(
            ReaderSettingsAction.SetWebtoonSidePadding(20),
            ReaderSettingsAction.SetWebtoonSidePadding(20),
        )
        assertEquals(
            ReaderSettingsAction.ToggleWebtoonEnableZoomOut(true),
            ReaderSettingsAction.ToggleWebtoonEnableZoomOut(true),
        )
        assertEquals(
            ReaderSettingsAction.SetWebtoonNav(2),
            ReaderSettingsAction.SetWebtoonNav(2),
        )
        assertEquals(
            ReaderSettingsAction.SetWebtoonInvertTapping(ViewerNavigation.TappingInvertMode.BOTH),
            ReaderSettingsAction.SetWebtoonInvertTapping(ViewerNavigation.TappingInvertMode.BOTH),
        )
        assertEquals(
            ReaderSettingsAction.SetWebtoonPageLayout(PageLayout.SPLIT_PAGES),
            ReaderSettingsAction.SetWebtoonPageLayout(PageLayout.SPLIT_PAGES),
        )
        assertEquals(
            ReaderSettingsAction.ToggleWebtoonInvertDoublePages(true),
            ReaderSettingsAction.ToggleWebtoonInvertDoublePages(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleWebtoonPageTransitions(true),
            ReaderSettingsAction.ToggleWebtoonPageTransitions(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleSplitTallImages(true),
            ReaderSettingsAction.ToggleSplitTallImages(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleWebtoonDisableGaps(true),
            ReaderSettingsAction.ToggleWebtoonDisableGaps(true),
        )

        // Paged
        assertEquals(
            ReaderSettingsAction.SetImageScaleType(3),
            ReaderSettingsAction.SetImageScaleType(3),
        )
        assertEquals(
            ReaderSettingsAction.SetZoomStart(2),
            ReaderSettingsAction.SetZoomStart(2),
        )
        assertEquals(
            ReaderSettingsAction.ToggleCropBorders(true),
            ReaderSettingsAction.ToggleCropBorders(true),
        )
        assertEquals(
            ReaderSettingsAction.TogglePageTransitions(true),
            ReaderSettingsAction.TogglePageTransitions(true),
        )
        assertEquals(
            ReaderSettingsAction.SetPagerNav(1),
            ReaderSettingsAction.SetPagerNav(1),
        )
        assertEquals(
            ReaderSettingsAction.SetPagerInvertTapping(ViewerNavigation.TappingInvertMode.NONE),
            ReaderSettingsAction.SetPagerInvertTapping(ViewerNavigation.TappingInvertMode.NONE),
        )
        assertEquals(
            ReaderSettingsAction.SetPageLayout(PageLayout.DOUBLE_PAGES),
            ReaderSettingsAction.SetPageLayout(PageLayout.DOUBLE_PAGES),
        )
        assertEquals(
            ReaderSettingsAction.ToggleInvertDoublePages(true),
            ReaderSettingsAction.ToggleInvertDoublePages(true),
        )
        assertEquals(
            ReaderSettingsAction.SetDoublePageGap(10),
            ReaderSettingsAction.SetDoublePageGap(10),
        )
        assertEquals(
            ReaderSettingsAction.ToggleZoomDoublePageSpreads(true),
            ReaderSettingsAction.ToggleZoomDoublePageSpreads(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleNavigateToPan(true),
            ReaderSettingsAction.ToggleNavigateToPan(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleDoublePageRotate(true),
            ReaderSettingsAction.ToggleDoublePageRotate(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleDoublePageRotateReverse(true),
            ReaderSettingsAction.ToggleDoublePageRotateReverse(true),
        )
        assertEquals(
            ReaderSettingsAction.SetPagerCutoutBehavior(1),
            ReaderSettingsAction.SetPagerCutoutBehavior(1),
        )

        // Filter
        assertEquals(
            ReaderSettingsAction.ToggleGrayscale(true),
            ReaderSettingsAction.ToggleGrayscale(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleInvertedColors(true),
            ReaderSettingsAction.ToggleInvertedColors(true),
        )
        assertEquals(
            ReaderSettingsAction.ToggleCustomBrightness(true),
            ReaderSettingsAction.ToggleCustomBrightness(true),
        )
        assertEquals(
            ReaderSettingsAction.SetCustomBrightnessValue(-25),
            ReaderSettingsAction.SetCustomBrightnessValue(-25),
        )
        assertEquals(
            ReaderSettingsAction.ToggleColorFilter(true),
            ReaderSettingsAction.ToggleColorFilter(true),
        )
        assertEquals(
            ReaderSettingsAction.SetColorFilterMode(3),
            ReaderSettingsAction.SetColorFilterMode(3),
        )
        assertEquals(
            ReaderSettingsAction.SetColorFilterValue(0x12345678),
            ReaderSettingsAction.SetColorFilterValue(0x12345678),
        )

        // Controls
        assertEquals(
            ReaderSettingsAction.OpenFullSettings,
            ReaderSettingsAction.OpenFullSettings,
        )
        assertEquals(
            ReaderSettingsAction.Dismiss,
            ReaderSettingsAction.Dismiss,
        )
    }

    @Test
    fun `given different ReaderSettingsAction types, verify they are not equal`() {
        val action1: ReaderSettingsAction = ReaderSettingsAction.OpenFullSettings
        val action2: ReaderSettingsAction = ReaderSettingsAction.Dismiss
        assertNotEquals(action1, action2)

        val action3: ReaderSettingsAction = ReaderSettingsAction.ToggleGrayscale(true)
        val action4: ReaderSettingsAction = ReaderSettingsAction.ToggleInvertedColors(true)
        assertNotEquals(action3, action4)
    }
}
