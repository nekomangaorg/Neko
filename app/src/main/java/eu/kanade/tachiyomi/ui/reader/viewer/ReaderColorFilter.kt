package eu.kanade.tachiyomi.ui.reader.viewer

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Utility for generating Jetpack Compose [ColorFilter] and [ColorMatrix] instances for Reader
 * display effects such as grayscale and inverted colors.
 */
object ReaderColorFilter {
    private val INVERTED_COLOR_MATRIX =
        floatArrayOf(
            -1f,
            0f,
            0f,
            0f,
            255f,
            0f,
            -1f,
            0f,
            0f,
            255f,
            0f,
            0f,
            -1f,
            0f,
            255f,
            0f,
            0f,
            0f,
            1f,
            0f,
        )

    /**
     * Returns a [ColorFilter] combining [grayscale] and [invertedColors] if either is active, or
     * `null` if both are disabled.
     */
    fun getColorFilter(grayscale: Boolean, invertedColors: Boolean): ColorFilter? {
        val matrix = getColorMatrix(grayscale, invertedColors) ?: return null
        return ColorFilter.colorMatrix(matrix)
    }

    /**
     * Returns a [ColorMatrix] combining [grayscale] and [invertedColors] if either is active, or
     * `null` if both are disabled.
     */
    fun getColorMatrix(grayscale: Boolean, invertedColors: Boolean): ColorMatrix? {
        if (!grayscale && !invertedColors) return null

        if (grayscale && !invertedColors) {
            return ColorMatrix().apply { setToSaturation(0f) }
        }

        val invertMatrix = ColorMatrix(INVERTED_COLOR_MATRIX)
        if (grayscale) {
            val satMatrix = ColorMatrix().apply { setToSaturation(0f) }
            invertMatrix *= satMatrix
        }
        return invertMatrix
    }
}
