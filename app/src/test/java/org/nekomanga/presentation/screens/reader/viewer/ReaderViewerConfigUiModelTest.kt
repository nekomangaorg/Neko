package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderColorFilter
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReaderViewerConfigUiModelTest {

    @Test
    fun `given default PagerViewerConfigUiModel, then colorFilter is null and background is Black`() {
        val config = PagerViewerConfigUiModel()

        assertNull(config.colorFilter)
        assertEquals(Color.Black, config.backgroundColor)
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
}
