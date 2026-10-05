package eu.kanade.tachiyomi.ui.reader.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderViewerPreferencesTest {

    @Test
    fun `given default ReaderViewerPreferences, then expected defaults are set`() {
        val prefs = ReaderViewerPreferences()

        assertTrue(prefs.animatedTransitions)
        assertTrue(prefs.animatedTransitionsWebtoon)
        assertEquals(1, prefs.imageScaleType)
        assertEquals(0, prefs.doublePageGap)
        assertFalse(prefs.invertDoublePages)
        assertEquals(1, prefs.readerTheme)
        assertFalse(prefs.landscapeZoom)
        assertEquals(1, prefs.zoomStart)
        assertEquals(4, prefs.preloadPageAmount)
        assertFalse(prefs.cropBorders)
        assertFalse(prefs.cropBordersWebtoon)
        assertFalse(prefs.grayscale)
        assertFalse(prefs.invertedColors)
        assertFalse(prefs.doublePageRotate)
        assertFalse(prefs.doublePageRotateReverse)
        assertTrue(prefs.navigateToPan)
        assertEquals(0, prefs.webtoonSidePadding)
        assertFalse(prefs.webtoonDisableGaps)
        assertFalse(prefs.webtoonEnableZoomOut)
    }

    @Test
    fun `given ReaderViewerPreferences, when copied with modifications, then updated values are reflected`() {
        val original = ReaderViewerPreferences()
        val modified =
            original.copy(
                grayscale = true,
                invertedColors = true,
                preloadPageAmount = 8,
                webtoonSidePadding = 15,
                webtoonDisableGaps = true,
                webtoonEnableZoomOut = true,
                animatedTransitions = false,
                animatedTransitionsWebtoon = false,
            )

        assertTrue(modified.grayscale)
        assertTrue(modified.invertedColors)
        assertEquals(8, modified.preloadPageAmount)
        assertEquals(15, modified.webtoonSidePadding)
        assertTrue(modified.webtoonDisableGaps)
        assertTrue(modified.webtoonEnableZoomOut)
        assertFalse(modified.animatedTransitions)
        assertFalse(modified.animatedTransitionsWebtoon)
        assertNotEquals(original, modified)
    }

    @Test
    fun `given identical ReaderViewerPreferences, then instances are equal`() {
        val prefs1 = ReaderViewerPreferences(readerTheme = 2, grayscale = true)
        val prefs2 = ReaderViewerPreferences(readerTheme = 2, grayscale = true)

        assertEquals(prefs1, prefs2)
        assertEquals(prefs1.hashCode(), prefs2.hashCode())
    }
}
