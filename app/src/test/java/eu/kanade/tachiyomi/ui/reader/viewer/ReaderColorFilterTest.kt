package eu.kanade.tachiyomi.ui.reader.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReaderColorFilterTest {

    @Test
    fun `given neither grayscale nor inverted, when getting color matrix, then returns null`() {
        val matrix = ReaderColorFilter.getColorMatrix(grayscale = false, invertedColors = false)
        assertNull(matrix)
    }

    @Test
    fun `given neither grayscale nor inverted, when getting color filter, then returns null`() {
        val filter = ReaderColorFilter.getColorFilter(grayscale = false, invertedColors = false)
        assertNull(filter)
    }

    @Test
    fun `given only grayscale, when getting color matrix, then returns saturation 0 matrix`() {
        val matrix = ReaderColorFilter.getColorMatrix(grayscale = true, invertedColors = false)
        assertNotNull(matrix)

        val values = matrix!!.values
        val r = 0.213f
        val g = 0.715f
        val b = 0.072f

        // Row 0 (Red)
        assertEquals(r, values[0], 0.001f)
        assertEquals(g, values[1], 0.001f)
        assertEquals(b, values[2], 0.001f)
        assertEquals(0f, values[3], 0.001f)
        assertEquals(0f, values[4], 0.001f)

        // Row 1 (Green)
        assertEquals(r, values[5], 0.001f)
        assertEquals(g, values[6], 0.001f)
        assertEquals(b, values[7], 0.001f)
        assertEquals(0f, values[8], 0.001f)
        assertEquals(0f, values[9], 0.001f)

        // Row 2 (Blue)
        assertEquals(r, values[10], 0.001f)
        assertEquals(g, values[11], 0.001f)
        assertEquals(b, values[12], 0.001f)
        assertEquals(0f, values[13], 0.001f)
        assertEquals(0f, values[14], 0.001f)

        // Row 3 (Alpha)
        assertEquals(0f, values[15], 0.001f)
        assertEquals(0f, values[16], 0.001f)
        assertEquals(0f, values[17], 0.001f)
        assertEquals(1f, values[18], 0.001f)
        assertEquals(0f, values[19], 0.001f)
    }

    @Test
    fun `given only inverted, when getting color matrix, then returns inversion matrix`() {
        val matrix = ReaderColorFilter.getColorMatrix(grayscale = false, invertedColors = true)
        assertNotNull(matrix)

        val values = matrix!!.values

        // Row 0 (Red inverted: -1*R + 255)
        assertEquals(-1f, values[0], 0.001f)
        assertEquals(0f, values[1], 0.001f)
        assertEquals(0f, values[2], 0.001f)
        assertEquals(0f, values[3], 0.001f)
        assertEquals(255f, values[4], 0.001f)

        // Row 1 (Green inverted: -1*G + 255)
        assertEquals(0f, values[5], 0.001f)
        assertEquals(-1f, values[6], 0.001f)
        assertEquals(0f, values[7], 0.001f)
        assertEquals(0f, values[8], 0.001f)
        assertEquals(255f, values[9], 0.001f)

        // Row 2 (Blue inverted: -1*B + 255)
        assertEquals(0f, values[10], 0.001f)
        assertEquals(0f, values[11], 0.001f)
        assertEquals(-1f, values[12], 0.001f)
        assertEquals(0f, values[13], 0.001f)
        assertEquals(255f, values[14], 0.001f)

        // Row 3 (Alpha untouched: 1*A + 0)
        assertEquals(0f, values[15], 0.001f)
        assertEquals(0f, values[16], 0.001f)
        assertEquals(0f, values[17], 0.001f)
        assertEquals(1f, values[18], 0.001f)
        assertEquals(0f, values[19], 0.001f)
    }

    @Test
    fun `given both grayscale and inverted, when getting color matrix, then returns combined inverted grayscale matrix`() {
        val matrix = ReaderColorFilter.getColorMatrix(grayscale = true, invertedColors = true)
        assertNotNull(matrix)

        val values = matrix!!.values
        val r = -0.213f
        val g = -0.715f
        val b = -0.072f

        // Inverted grayscale: 255 - (r*R + g*G + b*B)
        // Row 0
        assertEquals(r, values[0], 0.001f)
        assertEquals(g, values[1], 0.001f)
        assertEquals(b, values[2], 0.001f)
        assertEquals(0f, values[3], 0.001f)
        assertEquals(255f, values[4], 0.001f)

        // Row 1
        assertEquals(r, values[5], 0.001f)
        assertEquals(g, values[6], 0.001f)
        assertEquals(b, values[7], 0.001f)
        assertEquals(0f, values[8], 0.001f)
        assertEquals(255f, values[9], 0.001f)

        // Row 2
        assertEquals(r, values[10], 0.001f)
        assertEquals(g, values[11], 0.001f)
        assertEquals(b, values[12], 0.001f)
        assertEquals(0f, values[13], 0.001f)
        assertEquals(255f, values[14], 0.001f)

        // Row 3 (Alpha)
        assertEquals(0f, values[15], 0.001f)
        assertEquals(0f, values[16], 0.001f)
        assertEquals(0f, values[17], 0.001f)
        assertEquals(1f, values[18], 0.001f)
        assertEquals(0f, values[19], 0.001f)
    }

    @Test
    fun `getColorFilter returns cached instance for repeated calls`() {
        val filter1 = ReaderColorFilter.getColorFilter(grayscale = true, invertedColors = false)
        val filter2 = ReaderColorFilter.getColorFilter(grayscale = true, invertedColors = false)
        assertSame(filter1, filter2)

        val invert1 = ReaderColorFilter.getColorFilter(grayscale = false, invertedColors = true)
        val invert2 = ReaderColorFilter.getColorFilter(grayscale = false, invertedColors = true)
        assertSame(invert1, invert2)

        val both1 = ReaderColorFilter.getColorFilter(grayscale = true, invertedColors = true)
        val both2 = ReaderColorFilter.getColorFilter(grayscale = true, invertedColors = true)
        assertSame(both1, both2)
    }
}
