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
}
