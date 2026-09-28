package org.nekomanga.domain.reader.image

import android.graphics.Bitmap
import android.graphics.Rect
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.abs

/** Pure data model representing calculated crop coordinates. */
data class CropBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top
}

/**
 * Functional interface abstracting pixel retrieval across rows and columns. Enables fast JVM unit
 * testing without Android Bitmap dependencies.
 */
interface PixelRowColReader {
    fun readRow(y: Int, width: Int, pixels: IntArray)

    fun readCol(x: Int, height: Int, pixels: IntArray)
}

/**
 * Coil 3 image transformation that trims solid black, white, or monotone margins from manga pages.
 *
 * @param enabled Whether margin cropping is active.
 * @param cropTopBottom Whether top and bottom margins should be cropped. Set to true for paginated
 *   readers (horizontal/vertical pages). Set to false for continuous webtoons to preserve vertical
 *   slice seam continuity.
 * @param tolerance Max luminance divergence (0-255) considered uniform margin.
 */
class CropBordersTransformation(
    val enabled: Boolean = true,
    val cropTopBottom: Boolean = true,
    val tolerance: Int = 15,
) : Transformation() {

    override val cacheKey: String =
        "org.nekomanga.crop_borders_enabled_${enabled}_tb_${cropTopBottom}_tol_${tolerance}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (!enabled || input.isRecycled) return input

        val cropRect = calculateCropBounds(input, cropTopBottom, tolerance) ?: return input

        if (cropRect.width() <= 0 || cropRect.height() <= 0) return input

        // Return original if margins trimmed are negligible (<= 2 pixels trimmed on all active
        // edges)
        val leftTrimmed = cropRect.left
        val topTrimmed = if (cropTopBottom) cropRect.top else 0
        val rightTrimmed = input.width - cropRect.right
        val bottomTrimmed = if (cropTopBottom) input.height - cropRect.bottom else 0

        if (leftTrimmed <= 2 && topTrimmed <= 2 && rightTrimmed <= 2 && bottomTrimmed <= 2) {
            return input
        }

        return Bitmap.createBitmap(
            input,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height(),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CropBordersTransformation) return false
        return enabled == other.enabled &&
            cropTopBottom == other.cropTopBottom &&
            tolerance == other.tolerance
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + cropTopBottom.hashCode()
        result = 31 * result + tolerance.hashCode()
        return result
    }

    override fun toString(): String {
        return "CropBordersTransformation(enabled=$enabled, cropTopBottom=$cropTopBottom, tolerance=$tolerance)"
    }

    companion object {
        /**
         * Scans image boundaries inward up to 25% of dimensions and determines the bounding crop
         * rectangle excluding monotone margins.
         */
        fun calculateCropBounds(
            bitmap: Bitmap,
            cropTopBottom: Boolean = true,
            tolerance: Int = 15,
        ): Rect? {
            val bounds =
                calculateCropBounds(
                    width = bitmap.width,
                    height = bitmap.height,
                    reader =
                        object : PixelRowColReader {
                            override fun readRow(y: Int, width: Int, pixels: IntArray) {
                                bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
                            }

                            override fun readCol(x: Int, height: Int, pixels: IntArray) {
                                bitmap.getPixels(pixels, 0, 1, x, 0, 1, height)
                            }
                        },
                    cropTopBottom = cropTopBottom,
                    tolerance = tolerance,
                ) ?: return null

            return Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)
        }

        /** Core pure Kotlin algorithm for scanning exterior boundaries and finding crop bounds. */
        fun calculateCropBounds(
            width: Int,
            height: Int,
            reader: PixelRowColReader,
            cropTopBottom: Boolean = true,
            tolerance: Int = 15,
        ): CropBounds? {
            if (width < 10 || height < 10) return null

            val pixels = IntArray(maxOf(width, height))

            // 1. Scan Top edge (if enabled)
            var top = 0
            if (cropTopBottom) {
                val maxTop = height / 4
                for (y in 0 until maxTop) {
                    reader.readRow(y, width, pixels)
                    if (!isUniformRow(pixels, width, tolerance)) {
                        top = y
                        break
                    }
                    top = y + 1
                }
            }

            // 2. Scan Bottom edge (if enabled)
            var bottom = height
            if (cropTopBottom) {
                val minBottom = height - (height / 4)
                for (y in height - 1 downTo minBottom) {
                    reader.readRow(y, width, pixels)
                    if (!isUniformRow(pixels, width, tolerance)) {
                        bottom = y + 1
                        break
                    }
                    bottom = y
                }
            }

            // 3. Scan Left edge
            var left = 0
            val maxLeft = width / 4
            for (x in 0 until maxLeft) {
                reader.readCol(x, height, pixels)
                if (!isUniformColumn(pixels, height, tolerance)) {
                    left = x
                    break
                }
                left = x + 1
            }

            // 4. Scan Right edge
            var right = width
            val minRight = width - (width / 4)
            for (x in width - 1 downTo minRight) {
                reader.readCol(x, height, pixels)
                if (!isUniformColumn(pixels, height, tolerance)) {
                    right = x + 1
                    break
                }
                right = x
            }

            if (left >= right || top >= bottom) return null
            return CropBounds(left, top, right, bottom)
        }

        private fun isUniformRow(pixels: IntArray, length: Int, tolerance: Int): Boolean {
            val firstLuma = pixelLuminance(pixels[0])
            // Only crop if border is black-ish or white-ish/clear
            if (firstLuma > 0.10f && firstLuma < 0.90f) return false
            val tolNormalized = tolerance / 255f
            for (i in 1 until length) {
                if (abs(pixelLuminance(pixels[i]) - firstLuma) > tolNormalized) {
                    return false
                }
            }
            return true
        }

        private fun isUniformColumn(pixels: IntArray, length: Int, tolerance: Int): Boolean {
            val firstLuma = pixelLuminance(pixels[0])
            if (firstLuma > 0.10f && firstLuma < 0.90f) return false
            val tolNormalized = tolerance / 255f
            for (i in 1 until length) {
                if (abs(pixelLuminance(pixels[i]) - firstLuma) > tolNormalized) {
                    return false
                }
            }
            return true
        }

        fun pixelLuminance(color: Int): Float {
            val alpha = color ushr 24
            // Treat fully transparent pixels as clear/white margin
            if (alpha < 16) return 1f
            val r = (color shr 16 and 0xFF) / 255f
            val g = (color shr 8 and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            return 0.2126f * r + 0.7152f * g + 0.0722f * b
        }
    }
}
