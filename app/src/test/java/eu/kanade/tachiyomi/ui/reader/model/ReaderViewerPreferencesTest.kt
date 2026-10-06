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

    @Test
    fun `given base ReaderViewerPreferences, modifying any individual property produces unequal instance`() {
        val base = ReaderViewerPreferences()

        assertNotEquals(base, base.copy(animatedTransitions = !base.animatedTransitions))
        assertNotEquals(
            base,
            base.copy(animatedTransitionsWebtoon = !base.animatedTransitionsWebtoon),
        )
        assertNotEquals(base, base.copy(imageScaleType = base.imageScaleType + 1))
        assertNotEquals(base, base.copy(doublePageGap = base.doublePageGap + 10))
        assertNotEquals(base, base.copy(invertDoublePages = !base.invertDoublePages))
        assertNotEquals(base, base.copy(readerTheme = base.readerTheme + 1))
        assertNotEquals(base, base.copy(landscapeZoom = !base.landscapeZoom))
        assertNotEquals(base, base.copy(zoomStart = base.zoomStart + 1))
        assertNotEquals(base, base.copy(preloadPageAmount = base.preloadPageAmount + 2))
        assertNotEquals(base, base.copy(cropBorders = !base.cropBorders))
        assertNotEquals(base, base.copy(cropBordersWebtoon = !base.cropBordersWebtoon))
        assertNotEquals(base, base.copy(grayscale = !base.grayscale))
        assertNotEquals(base, base.copy(invertedColors = !base.invertedColors))
        assertNotEquals(base, base.copy(doublePageRotate = !base.doublePageRotate))
        assertNotEquals(base, base.copy(doublePageRotateReverse = !base.doublePageRotateReverse))
        assertNotEquals(base, base.copy(navigateToPan = !base.navigateToPan))
        assertNotEquals(base, base.copy(webtoonSidePadding = base.webtoonSidePadding + 5))
        assertNotEquals(base, base.copy(webtoonDisableGaps = !base.webtoonDisableGaps))
        assertNotEquals(base, base.copy(webtoonEnableZoomOut = !base.webtoonEnableZoomOut))
    }
}
