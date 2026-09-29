package eu.kanade.tachiyomi.ui.reader.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
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

    @Test
    fun `getColorFilter returns distinct singletons for distinct active modes`() {
        val grayscale = ReaderColorFilter.getColorFilter(grayscale = true, invertedColors = false)
        val inverted = ReaderColorFilter.getColorFilter(grayscale = false, invertedColors = true)
        val both = ReaderColorFilter.getColorFilter(grayscale = true, invertedColors = true)

        assertNotNull(grayscale)
        assertNotNull(inverted)
        assertNotNull(both)

        assertNotSame(grayscale, inverted)
        assertNotSame(grayscale, both)
        assertNotSame(inverted, both)
    }

    @Test
    fun `getColorMatrix returns cached instance for repeated calls`() {
        val matrix1 = ReaderColorFilter.getColorMatrix(grayscale = true, invertedColors = false)
        val matrix2 = ReaderColorFilter.getColorMatrix(grayscale = true, invertedColors = false)
        assertNotNull(matrix1)
        assertNotNull(matrix2)
        assertSame(matrix1?.values, matrix2?.values)
        assertSame(ReaderColorFilter.GRAYSCALE_MATRIX, matrix1?.values)

        val invert1 = ReaderColorFilter.getColorMatrix(grayscale = false, invertedColors = true)
        val invert2 = ReaderColorFilter.getColorMatrix(grayscale = false, invertedColors = true)
        assertNotNull(invert1)
        assertNotNull(invert2)
        assertSame(invert1?.values, invert2?.values)
        assertSame(ReaderColorFilter.INVERTED_MATRIX, invert1?.values)

        val both1 = ReaderColorFilter.getColorMatrix(grayscale = true, invertedColors = true)
        val both2 = ReaderColorFilter.getColorMatrix(grayscale = true, invertedColors = true)
        assertNotNull(both1)
        assertNotNull(both2)
        assertSame(both1?.values, both2?.values)
        assertSame(ReaderColorFilter.INVERTED_GRAYSCALE_MATRIX, both1?.values)
    }

    @Test
    fun `getColorMatrix returns distinct singletons for distinct active modes`() {
        val grayscale = ReaderColorFilter.getColorMatrix(grayscale = true, invertedColors = false)
        val inverted = ReaderColorFilter.getColorMatrix(grayscale = false, invertedColors = true)
        val both = ReaderColorFilter.getColorMatrix(grayscale = true, invertedColors = true)

        assertNotNull(grayscale)
        assertNotNull(inverted)
        assertNotNull(both)

        assertNotSame(grayscale?.values, inverted?.values)
        assertNotSame(grayscale?.values, both?.values)
        assertNotSame(inverted?.values, both?.values)
    }

    @Test
    fun `static matrix arrays have exactly 20 elements`() {
        assertEquals(20, ReaderColorFilter.INVERTED_MATRIX.size)
        assertEquals(20, ReaderColorFilter.GRAYSCALE_MATRIX.size)
        assertEquals(20, ReaderColorFilter.INVERTED_GRAYSCALE_MATRIX.size)
    }

    private fun transformColor(
        matrix: FloatArray,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ): FloatArray {
        val outR = matrix[0] * r + matrix[1] * g + matrix[2] * b + matrix[3] * a + matrix[4]
        val outG = matrix[5] * r + matrix[6] * g + matrix[7] * b + matrix[8] * a + matrix[9]
        val outB = matrix[10] * r + matrix[11] * g + matrix[12] * b + matrix[13] * a + matrix[14]
        val outA = matrix[15] * r + matrix[16] * g + matrix[17] * b + matrix[18] * a + matrix[19]
        return floatArrayOf(outR, outG, outB, outA)
    }

    @Test
    fun `inverted matrix accurately inverts black and white while preserving alpha`() {
        val black = transformColor(ReaderColorFilter.INVERTED_MATRIX, 0f, 0f, 0f, 1f)
        assertEquals(255f, black[0], 0.001f)
        assertEquals(255f, black[1], 0.001f)
        assertEquals(255f, black[2], 0.001f)
        assertEquals(1f, black[3], 0.001f)

        val white = transformColor(ReaderColorFilter.INVERTED_MATRIX, 255f, 255f, 255f, 1f)
        assertEquals(0f, white[0], 0.001f)
        assertEquals(0f, white[1], 0.001f)
        assertEquals(0f, white[2], 0.001f)
        assertEquals(1f, white[3], 0.001f)
    }

    @Test
    fun `grayscale matrix accurately converts RGB primaries to luminance`() {
        val white = transformColor(ReaderColorFilter.GRAYSCALE_MATRIX, 255f, 255f, 255f, 1f)
        assertEquals(255f, white[0], 0.001f)
        assertEquals(255f, white[1], 0.001f)
        assertEquals(255f, white[2], 0.001f)
        assertEquals(1f, white[3], 0.001f)

        val black = transformColor(ReaderColorFilter.GRAYSCALE_MATRIX, 0f, 0f, 0f, 1f)
        assertEquals(0f, black[0], 0.001f)
        assertEquals(0f, black[1], 0.001f)
        assertEquals(0f, black[2], 0.001f)
        assertEquals(1f, black[3], 0.001f)

        // Pure Red: 0.213 * 255 = 54.315
        val red = transformColor(ReaderColorFilter.GRAYSCALE_MATRIX, 255f, 0f, 0f, 1f)
        assertEquals(54.315f, red[0], 0.001f)
        assertEquals(54.315f, red[1], 0.001f)
        assertEquals(54.315f, red[2], 0.001f)

        // Pure Green: 0.715 * 255 = 182.325
        val green = transformColor(ReaderColorFilter.GRAYSCALE_MATRIX, 0f, 255f, 0f, 1f)
        assertEquals(182.325f, green[0], 0.001f)
        assertEquals(182.325f, green[1], 0.001f)
        assertEquals(182.325f, green[2], 0.001f)

        // Pure Blue: 0.072 * 255 = 18.36
        val blue = transformColor(ReaderColorFilter.GRAYSCALE_MATRIX, 0f, 0f, 255f, 1f)
        assertEquals(18.36f, blue[0], 0.001f)
        assertEquals(18.36f, blue[1], 0.001f)
        assertEquals(18.36f, blue[2], 0.001f)
    }

    @Test
    fun `inverted grayscale matrix accurately computes inverted luminance`() {
        // Pure White -> Inverted Grayscale -> 255 - 255 = 0
        val white =
            transformColor(ReaderColorFilter.INVERTED_GRAYSCALE_MATRIX, 255f, 255f, 255f, 1f)
        assertEquals(0f, white[0], 0.001f)
        assertEquals(0f, white[1], 0.001f)
        assertEquals(0f, white[2], 0.001f)

        // Pure Black -> Inverted Grayscale -> 255 - 0 = 255
        val black = transformColor(ReaderColorFilter.INVERTED_GRAYSCALE_MATRIX, 0f, 0f, 0f, 1f)
        assertEquals(255f, black[0], 0.001f)
        assertEquals(255f, black[1], 0.001f)
        assertEquals(255f, black[2], 0.001f)

        // Pure Red -> 255 - 54.315 = 200.685
        val red = transformColor(ReaderColorFilter.INVERTED_GRAYSCALE_MATRIX, 255f, 0f, 0f, 1f)
        assertEquals(200.685f, red[0], 0.001f)
        assertEquals(200.685f, red[1], 0.001f)
        assertEquals(200.685f, red[2], 0.001f)
    }
}
