package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.ui.Alignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoublePageLayoutTest {

    // --- Step R16: Scale calculations & Fit Height Overflow ---

    @Test
    fun `calculateDoublePageScale with Fit Height scales to match viewport height exactly`() {
        // Two pages of height 1500, combined width 2000 in viewport 1600x1200
        val scale =
            calculateDoublePageScale(
                imageScaleType = 4, // Fit height
                totalWidth = 2000f,
                maxHeight = 1500f,
                viewportWidth = 1600f,
                viewportHeight = 1200f,
            )

        // Expected scale: 1200 / 1500 = 0.8
        assertEquals(0.8f, scale, 0.001f)
        val renderedWidth = 2000f * scale
        val renderedHeight = 1500f * scale
        assertEquals(1600f, renderedWidth, 0.001f)
        assertEquals(1200f, renderedHeight, 0.001f)
    }

    @Test
    fun `calculateDoublePageScale with Fit Height allows horizontal overflow without clamping`() {
        // Phone portrait viewport: 1080x1920. Two square pages: 1000x1000, totalWidth = 2000
        val scale =
            calculateDoublePageScale(
                imageScaleType = 4, // Fit height
                totalWidth = 2000f,
                maxHeight = 1000f,
                viewportWidth = 1080f,
                viewportHeight = 1920f,
            )

        // Expected scale: 1920 / 1000 = 1.92
        assertEquals(1.92f, scale, 0.001f)
        val renderedWidth = 2000f * scale
        val renderedHeight = 1000f * scale
        assertEquals(3840f, renderedWidth, 0.001f)
        assertEquals(1920f, renderedHeight, 0.001f)
        assertTrue(renderedWidth > 1080f) // Overflows horizontally by 2760px
    }

    @Test
    fun `calculateDoublePageScale with Fit Width scales to match viewport width exactly`() {
        val scale =
            calculateDoublePageScale(
                imageScaleType = 3, // Fit width
                totalWidth = 2000f,
                maxHeight = 1500f,
                viewportWidth = 1600f,
                viewportHeight = 1200f,
            )

        // Expected scale: 1600 / 2000 = 0.8
        assertEquals(0.8f, scale, 0.001f)
        assertEquals(1600f, 2000f * scale, 0.001f)
    }

    @Test
    fun `calculateDoublePageScale with Original Size returns 1f`() {
        val scale =
            calculateDoublePageScale(
                imageScaleType = 5, // Original size
                totalWidth = 2000f,
                maxHeight = 1500f,
                viewportWidth = 1600f,
                viewportHeight = 1200f,
            )

        assertEquals(1f, scale, 0.001f)
    }

    @Test
    fun `calculateDoublePageScale with Smart Fit picks Fit Height for landscape spreads`() {
        // totalWidth 2000 > maxHeight 1000 -> picks Fit Height
        val scale =
            calculateDoublePageScale(
                imageScaleType = 6, // Smart fit
                totalWidth = 2000f,
                maxHeight = 1000f,
                viewportWidth = 1080f,
                viewportHeight = 1920f,
            )

        assertEquals(1.92f, scale, 0.001f)
    }

    @Test
    fun `calculateDoublePageScale with Fit Screen clamps both dimensions inside viewport`() {
        val scale =
            calculateDoublePageScale(
                imageScaleType = 1, // Fit screen
                totalWidth = 2000f,
                maxHeight = 1000f,
                viewportWidth = 1000f,
                viewportHeight = 1000f,
            )

        // Clamped by width: 1000 / 2000 = 0.5
        assertEquals(0.5f, scale, 0.001f)
    }

    @Test
    fun `calculateDoublePageScale handles zero or invalid dimensions gracefully`() {
        assertEquals(1f, calculateDoublePageScale(4, 0f, 1000f, 1000f, 1000f), 0.001f)
        assertEquals(1f, calculateDoublePageScale(4, 1000f, 0f, 1000f, 1000f), 0.001f)
        assertEquals(1f, calculateDoublePageScale(4, 1000f, 1000f, 0f, 1000f), 0.001f)
        assertEquals(1f, calculateDoublePageScale(4, 1000f, 1000f, 1000f, 0f), 0.001f)
    }

    // --- Step R15: Auto-Zoom Disambiguation & Tablet Landscape ---

    @Test
    fun `shouldAutoZoomSpread returns false on tablet landscape even for true spread`() {
        // Tablet landscape 1600x1200 -> aspect ratio 1.333 >= 1.33
        val shouldZoom =
            shouldAutoZoomSpread(
                zoomEnabled = true,
                imageScaleType = 1,
                isTrueSpread = true,
                viewportWidth = 1600f,
                viewportHeight = 1200f,
            )

        assertFalse(shouldZoom)
    }

    @Test
    fun `shouldAutoZoomSpread returns false on widescreen phone landscape`() {
        // Phone landscape 2400x1080 -> aspect ratio 2.22 >= 1.33
        val shouldZoom =
            shouldAutoZoomSpread(
                zoomEnabled = true,
                imageScaleType = 1,
                isTrueSpread = true,
                viewportWidth = 2400f,
                viewportHeight = 1080f,
            )

        assertFalse(shouldZoom)
    }

    @Test
    fun `shouldAutoZoomSpread returns false for synthetic portrait pairs on phone`() {
        // Phone portrait 1080x1920 -> aspect ratio 0.5625 < 1.33, but synthetic pair (isTrueSpread
        // = false)
        val shouldZoom =
            shouldAutoZoomSpread(
                zoomEnabled = true,
                imageScaleType = 1,
                isTrueSpread = false,
                viewportWidth = 1080f,
                viewportHeight = 1920f,
            )

        assertFalse(shouldZoom)
    }

    @Test
    fun `shouldAutoZoomSpread returns true for true wide spreads on phone in fit screen`() {
        // Phone portrait 1080x1920 -> aspect ratio 0.5625 < 1.33, true spread, fit screen
        val shouldZoom =
            shouldAutoZoomSpread(
                zoomEnabled = true,
                imageScaleType = 1,
                isTrueSpread = true,
                viewportWidth = 1080f,
                viewportHeight = 1920f,
            )

        assertTrue(shouldZoom)
    }

    @Test
    fun `shouldAutoZoomSpread returns false when zoomEnabled is false`() {
        val shouldZoom =
            shouldAutoZoomSpread(
                zoomEnabled = false,
                imageScaleType = 1,
                isTrueSpread = true,
                viewportWidth = 1080f,
                viewportHeight = 1920f,
            )

        assertFalse(shouldZoom)
    }

    @Test
    fun `shouldAutoZoomSpread returns false when scaleType is not Fit Screen`() {
        // Fit Height (scaleType = 4)
        val shouldZoom =
            shouldAutoZoomSpread(
                zoomEnabled = true,
                imageScaleType = 4,
                isTrueSpread = true,
                viewportWidth = 1080f,
                viewportHeight = 1920f,
            )

        assertFalse(shouldZoom)
    }

    // --- Alignment & Overflow Positioning ---

    @Test
    fun `resolveDoublePageAlignment in LTR with auto zoomStart returns CenterStart`() {
        assertEquals(
            Alignment.CenterStart,
            resolveDoublePageAlignment(zoomStart = 1, isRtl = false),
        )
    }

    @Test
    fun `resolveDoublePageAlignment in RTL with auto zoomStart returns CenterEnd`() {
        assertEquals(Alignment.CenterEnd, resolveDoublePageAlignment(zoomStart = 1, isRtl = true))
    }

    @Test
    fun `resolveDoublePageAlignment with explicit zoomStart returns corresponding alignment`() {
        assertEquals(
            Alignment.CenterStart,
            resolveDoublePageAlignment(zoomStart = 2, isRtl = false),
        )
        assertEquals(Alignment.CenterEnd, resolveDoublePageAlignment(zoomStart = 3, isRtl = false))
        assertEquals(Alignment.Center, resolveDoublePageAlignment(zoomStart = 4, isRtl = false))
    }

    @Test
    fun `calculateRowAlignment returns doublePageAlignment when content overflows horizontally`() {
        val alignment =
            calculateRowAlignment(
                doublePageAlignment = Alignment.CenterStart,
                renderedWidthPx = 2000f,
                viewportWidthPx = 1080f,
            )

        assertEquals(Alignment.CenterStart, alignment)
    }

    @Test
    fun `calculateRowAlignment returns Center when content fits inside viewport width`() {
        val alignment =
            calculateRowAlignment(
                doublePageAlignment = Alignment.CenterStart,
                renderedWidthPx = 900f,
                viewportWidthPx = 1080f,
            )

        assertEquals(Alignment.Center, alignment)
    }
}
