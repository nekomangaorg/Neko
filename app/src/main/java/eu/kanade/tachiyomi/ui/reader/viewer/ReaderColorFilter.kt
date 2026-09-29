package eu.kanade.tachiyomi.ui.reader.viewer

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Utility for generating Jetpack Compose [ColorFilter] and [ColorMatrix] instances for Reader
 * display effects such as grayscale and inverted colors.
 */
object ReaderColorFilter {
    val INVERTED_MATRIX =
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

    val GRAYSCALE_MATRIX =
        floatArrayOf(
            0.213f,
            0.715f,
            0.072f,
            0f,
            0f,
            0.213f,
            0.715f,
            0.072f,
            0f,
            0f,
            0.213f,
            0.715f,
            0.072f,
            0f,
            0f,
            0f,
            0f,
            0f,
            1f,
            0f,
        )

    val INVERTED_GRAYSCALE_MATRIX =
        floatArrayOf(
            -0.213f,
            -0.715f,
            -0.072f,
            0f,
            255f,
            -0.213f,
            -0.715f,
            -0.072f,
            0f,
            255f,
            -0.213f,
            -0.715f,
            -0.072f,
            0f,
            255f,
            0f,
            0f,
            0f,
            1f,
            0f,
        )

    private val grayscaleMatrix by lazy { ColorMatrix(GRAYSCALE_MATRIX) }
    private val invertedMatrix by lazy { ColorMatrix(INVERTED_MATRIX) }
    private val invertedGrayscaleMatrix by lazy { ColorMatrix(INVERTED_GRAYSCALE_MATRIX) }

    private val grayscaleFilter by lazy { ColorFilter.colorMatrix(grayscaleMatrix) }
    private val invertedFilter by lazy { ColorFilter.colorMatrix(invertedMatrix) }
    private val invertedGrayscaleFilter by lazy { ColorFilter.colorMatrix(invertedGrayscaleMatrix) }

    /**
     * Returns a cached [ColorFilter] combining [grayscale] and [invertedColors] if either is
     * active, or `null` if both are disabled.
     */
    fun getColorFilter(grayscale: Boolean, invertedColors: Boolean): ColorFilter? {
        return when {
            grayscale && invertedColors -> invertedGrayscaleFilter
            grayscale -> grayscaleFilter
            invertedColors -> invertedFilter
            else -> null
        }
    }

    /**
     * Returns a cached [ColorMatrix] combining [grayscale] and [invertedColors] if either is
     * active, or `null` if both are disabled.
     */
    fun getColorMatrix(grayscale: Boolean, invertedColors: Boolean): ColorMatrix? {
        return when {
            grayscale && invertedColors -> invertedGrayscaleMatrix
            grayscale -> grayscaleMatrix
            invertedColors -> invertedMatrix
            else -> null
        }
    }
}
