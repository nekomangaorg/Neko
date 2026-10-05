package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderColorFilter
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderViewerConfigUiModelTest {

    @Test
    fun `given default PagerViewerConfigUiModel, then colorFilter is null, background is Black, and rotation flags are false`() {
        val config = PagerViewerConfigUiModel()

        assertNull(config.colorFilter)
        assertEquals(Color.Black, config.backgroundColor)
        assertFalse(config.doublePageRotate)
        assertFalse(config.doublePageRotateReverse)
    }

    @Test
    fun `given default WebtoonViewerConfigUiModel, then colorFilter is null and background is Transparent`() {
        val config = WebtoonViewerConfigUiModel(navigator = DisabledNavigation())

        assertNull(config.colorFilter)
        assertEquals(Color.Transparent, config.backgroundColor)
    }

    @Test
    fun `given PagerViewerConfigUiModel with colorFilter, when copied, then maintains colorFilter reference`() {
        val filter = ReaderColorFilter.getColorFilter(grayscale = true, invertedColors = false)
        assertNotNull(filter)

        val config = PagerViewerConfigUiModel(colorFilter = filter)
        assertSame(filter, config.colorFilter)

        val updated = config.copy(initialIndex = 5)
        assertSame(filter, updated.colorFilter)
        assertEquals(5, updated.initialIndex)
    }

    @Test
    fun `given PagerViewerConfigUiModel with rotation flags, when copied, then maintains rotation preferences`() {
        val config =
            PagerViewerConfigUiModel(
                doublePageRotate = true,
                doublePageRotateReverse = true,
            )
        assertTrue(config.doublePageRotate)
        assertTrue(config.doublePageRotateReverse)

        val updated = config.copy(initialIndex = 2)
        assertTrue(updated.doublePageRotate)
        assertTrue(updated.doublePageRotateReverse)
        assertEquals(2, updated.initialIndex)
    }

    @Test
    fun `given WebtoonViewerConfigUiModel with colorFilter, when copied, then maintains colorFilter reference`() {
        val filter = ReaderColorFilter.getColorFilter(grayscale = false, invertedColors = true)
        assertNotNull(filter)

        val config =
            WebtoonViewerConfigUiModel(navigator = DisabledNavigation(), colorFilter = filter)
        assertSame(filter, config.colorFilter)

        val updated = config.copy(initialIndex = 3)
        assertSame(filter, updated.colorFilter)
        assertEquals(3, updated.initialIndex)
    }

    @Test
    fun `given configs with differing colorFilters, then models are not equal`() {
        val grayscaleFilter =
            ReaderColorFilter.getColorFilter(grayscale = true, invertedColors = false)
        val invertedFilter =
            ReaderColorFilter.getColorFilter(grayscale = false, invertedColors = true)

        val pagerConfig1 = PagerViewerConfigUiModel(colorFilter = grayscaleFilter)
        val pagerConfig2 = PagerViewerConfigUiModel(colorFilter = invertedFilter)
        val pagerConfig3 = PagerViewerConfigUiModel(colorFilter = null)

        assertNotEquals(pagerConfig1, pagerConfig2)
        assertNotEquals(pagerConfig1, pagerConfig3)

        val nav = DisabledNavigation()
        val webtoonConfig1 =
            WebtoonViewerConfigUiModel(navigator = nav, colorFilter = grayscaleFilter)
        val webtoonConfig2 =
            WebtoonViewerConfigUiModel(navigator = nav, colorFilter = invertedFilter)
        val webtoonConfig3 = WebtoonViewerConfigUiModel(navigator = nav, colorFilter = null)

        assertNotEquals(webtoonConfig1, webtoonConfig2)
        assertNotEquals(webtoonConfig1, webtoonConfig3)
    }

    @Test
    fun `given configs with differing rotation preferences, then models are not equal`() {
        val default = PagerViewerConfigUiModel()
        val rotated = PagerViewerConfigUiModel(doublePageRotate = true)
        val rotatedFlipped =
            PagerViewerConfigUiModel(doublePageRotate = true, doublePageRotateReverse = true)

        assertNotEquals(default, rotated)
        assertNotEquals(rotated, rotatedFlipped)
        assertNotEquals(default, rotatedFlipped)
    }

    @Test
    fun `given PagerViewerConfigUiModel, modifying dual page properties produces unequal instances`() {
        val base = PagerViewerConfigUiModel()

        assertNotEquals(base, base.copy(doublePages = true))
        assertNotEquals(base, base.copy(shiftDoublePage = true))
        assertNotEquals(base, base.copy(invertDoublePages = true))
        assertNotEquals(base, base.copy(doublePageGap = 16))
        assertNotEquals(base, base.copy(zoomDoublePageSpreads = true))
    }

    @Test
    fun `given PagerViewerConfigUiModel, modifying navigation or scale properties produces unequal instances`() {
        val base = PagerViewerConfigUiModel()

        assertNotEquals(base, base.copy(isRtl = true))
        assertNotEquals(base, base.copy(isVertical = true))
        assertNotEquals(base, base.copy(imageScaleType = 2))
        assertNotEquals(base, base.copy(zoomStart = 2))
        assertNotEquals(base, base.copy(landscapeZoom = true))
        assertNotEquals(base, base.copy(navigateToPan = true))
        assertNotEquals(base, base.copy(animatedTransitions = false))
        assertNotEquals(base, base.copy(preloadPageAmount = 8))
    }

    @Test
    fun `given WebtoonViewerConfigUiModel, modifying webtoon-specific properties produces unequal instances`() {
        val nav = DisabledNavigation()
        val base = WebtoonViewerConfigUiModel(navigator = nav)

        assertNotEquals(base, base.copy(hasGaps = false))
        assertNotEquals(base, base.copy(enableZoomOut = true))
        assertNotEquals(base, base.copy(animatedTransitions = false))
        assertNotEquals(base, base.copy(sidePaddingPercent = 0.15f))
        assertNotEquals(base, base.copy(cropBorders = true))
        assertNotEquals(base, base.copy(preloadPageAmount = 6))
    }

    @Test
    fun `given identical configs, equals and hashCode contracts are satisfied`() {
        val nav = DisabledNavigation()
        val pager1 =
            PagerViewerConfigUiModel(
                navigator = nav,
                initialIndex = 3,
                isRtl = true,
                doublePages = true,
            )
        val pager2 =
            PagerViewerConfigUiModel(
                navigator = nav,
                initialIndex = 3,
                isRtl = true,
                doublePages = true,
            )

        assertEquals(pager1, pager2)
        assertEquals(pager1.hashCode(), pager2.hashCode())

        val webtoon1 =
            WebtoonViewerConfigUiModel(navigator = nav, initialIndex = 2, hasGaps = false)
        val webtoon2 =
            WebtoonViewerConfigUiModel(navigator = nav, initialIndex = 2, hasGaps = false)

        assertEquals(webtoon1, webtoon2)
        assertEquals(webtoon1.hashCode(), webtoon2.hashCode())
    }
}
