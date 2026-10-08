package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.abs
import org.nekomanga.logging.TimberKt

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
 * Coil 3 image transformation that trims solid black, white, cream, or monotone margins from manga
 * pages.
 *
 * @param cropTopBottom Whether top and bottom margins should be cropped. Set to true for paginated
 *   readers (horizontal/vertical pages). Set to false for continuous webtoons to preserve vertical
 *   slice seam continuity.
 * @param tolerance Max luminance divergence (0-255) considered uniform margin.
 */
class CropBordersTransformation(
    val cropTopBottom: Boolean = true,
    val tolerance: Int = DEFAULT_TOLERANCE,
) : Transformation() {

    override val cacheKey: String = "${this::class.java.name}_tb_${cropTopBottom}_tol_${tolerance}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (input.isRecycled) return input

        // Defensively guard against Bitmap.Config.HARDWARE: Bitmap.getPixels throws
        // IllegalArgumentException
        // on hardware bitmaps. If input is hardware-backed, make a software copy for margin
        // scanning.
        val softwareBitmap =
            if (input.config == Bitmap.Config.HARDWARE) {
                input.copy(Bitmap.Config.ARGB_8888, false) ?: return input
            } else {
                input
            }

        val cropRect = calculateCropBounds(softwareBitmap, cropTopBottom, tolerance)

        if (cropRect == null || cropRect.width() <= 0 || cropRect.height() <= 0) {
            if (softwareBitmap !== input) {
                softwareBitmap.recycle()
            }
            return input
        }

        // Return original if margins trimmed are negligible (<= 2 pixels trimmed on all active
        // edges)
        val leftTrimmed = cropRect.left
        val topTrimmed = if (cropTopBottom) cropRect.top else 0
        val rightTrimmed = input.width - cropRect.right
        val bottomTrimmed = if (cropTopBottom) input.height - cropRect.bottom else 0

        if (leftTrimmed <= 2 && topTrimmed <= 2 && rightTrimmed <= 2 && bottomTrimmed <= 2) {
            if (softwareBitmap !== input) {
                softwareBitmap.recycle()
            }
            return input
        }

        val cropped =
            try {
                Bitmap.createBitmap(
                    softwareBitmap,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height(),
                )
            } catch (e: IllegalArgumentException) {
                TimberKt.w(e) { "Failed to create cropped bitmap directly, falling back to Canvas" }
                val output =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val cs =
                            softwareBitmap.colorSpace?.takeIf {
                                it.model == ColorSpace.Model.RGB &&
                                    (it as? ColorSpace.Rgb)?.transferParameters != null
                            } ?: ColorSpace.get(ColorSpace.Named.SRGB)
                        val cfg =
                            softwareBitmap.config?.takeUnless { it == Bitmap.Config.HARDWARE }
                                ?: Bitmap.Config.ARGB_8888
                        Bitmap.createBitmap(
                            cropRect.width(),
                            cropRect.height(),
                            cfg,
                            softwareBitmap.hasAlpha(),
                            cs,
                        )
                    } else {
                        Bitmap.createBitmap(
                            cropRect.width(),
                            cropRect.height(),
                            softwareBitmap.config ?: Bitmap.Config.ARGB_8888,
                        )
                    }
                output.density = softwareBitmap.density
                val canvas = Canvas(output)
                val srcRect = Rect(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom)
                val dstRect = Rect(0, 0, cropRect.width(), cropRect.height())
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(softwareBitmap, srcRect, dstRect, paint)
                output
            } catch (e: Throwable) {
                TimberKt.e(e) { "Failed to crop bitmap" }
                if (softwareBitmap !== input) {
                    softwareBitmap.recycle()
                }
                return input
            }

        if (softwareBitmap !== input) {
            softwareBitmap.recycle()
        }

        return cropped
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CropBordersTransformation) return false
        return cropTopBottom == other.cropTopBottom && tolerance == other.tolerance
    }

    override fun hashCode(): Int {
        var result = cropTopBottom.hashCode()
        result = 31 * result + tolerance.hashCode()
        return result
    }

    override fun toString(): String {
        return "CropBordersTransformation(cropTopBottom=$cropTopBottom, tolerance=$tolerance)"
    }

    companion object {
        const val DEFAULT_TOLERANCE = 15

        /**
         * Scans image boundaries inward up to 25% of dimensions and determines the bounding crop
         * rectangle excluding monotone margins.
         */
        fun calculateCropBounds(
            bitmap: Bitmap,
            cropTopBottom: Boolean = true,
            tolerance: Int = DEFAULT_TOLERANCE,
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
            tolerance: Int = DEFAULT_TOLERANCE,
        ): CropBounds? {
            if (width < 10 || height < 10) return null

            val tolNormalized = tolerance / 255f
            val pixels = IntArray(maxOf(width, height))

            // 1. Scan Top edge (if enabled)
            var top = 0
            if (cropTopBottom) {
                reader.readRow(0, width, pixels)
                val topRefLuma = pixelLuminance(pixels[0])
                if (
                    isMarginLuma(topRefLuma) && isUniform(pixels, width, topRefLuma, tolNormalized)
                ) {
                    top = 1
                    val maxTop = height / 4
                    for (y in 1 until maxTop) {
                        reader.readRow(y, width, pixels)
                        if (!isUniform(pixels, width, topRefLuma, tolNormalized)) {
                            top = y
                            break
                        }
                        top = y + 1
                    }
                }
            }

            // 2. Scan Bottom edge (if enabled)
            var bottom = height
            if (cropTopBottom) {
                reader.readRow(height - 1, width, pixels)
                val bottomRefLuma = pixelLuminance(pixels[0])
                if (
                    isMarginLuma(bottomRefLuma) &&
                        isUniform(pixels, width, bottomRefLuma, tolNormalized)
                ) {
                    bottom = height - 1
                    val minBottom = height - (height / 4)
                    for (y in height - 2 downTo minBottom) {
                        reader.readRow(y, width, pixels)
                        if (!isUniform(pixels, width, bottomRefLuma, tolNormalized)) {
                            bottom = y + 1
                            break
                        }
                        bottom = y
                    }
                }
            }

            // 3. Scan Left edge
            var left = 0
            reader.readCol(0, height, pixels)
            val leftRefLuma = pixelLuminance(pixels[0])
            if (
                isMarginLuma(leftRefLuma) && isUniform(pixels, height, leftRefLuma, tolNormalized)
            ) {
                left = 1
                val maxLeft = width / 4
                for (x in 1 until maxLeft) {
                    reader.readCol(x, height, pixels)
                    if (!isUniform(pixels, height, leftRefLuma, tolNormalized)) {
                        left = x
                        break
                    }
                    left = x + 1
                }
            }

            // 4. Scan Right edge
            var right = width
            reader.readCol(width - 1, height, pixels)
            val rightRefLuma = pixelLuminance(pixels[0])
            if (
                isMarginLuma(rightRefLuma) && isUniform(pixels, height, rightRefLuma, tolNormalized)
            ) {
                right = width - 1
                val minRight = width - (width / 4)
                for (x in width - 2 downTo minRight) {
                    reader.readCol(x, height, pixels)
                    if (!isUniform(pixels, height, rightRefLuma, tolNormalized)) {
                        right = x + 1
                        break
                    }
                    right = x
                }
            }

            if (left >= right || top >= bottom) return null
            return CropBounds(left, top, right, bottom)
        }

        /**
         * Checks whether a reference luminance qualifies as a margin. Valid margins are dark
         * margins (<= 0.10f) or light paper margins (>= 0.80f), supporting aged or off-white manga
         * paper while ignoring midtones.
         */
        private fun isMarginLuma(luma: Float): Boolean {
            return luma <= 0.10f || luma >= 0.80f
        }

        /**
         * Verifies all pixels in the array match the reference margin luminance within tolerance.
         */
        private fun isUniform(
            pixels: IntArray,
            length: Int,
            refLuma: Float,
            tolNormalized: Float,
        ): Boolean {
            for (i in 0 until length) {
                if (abs(pixelLuminance(pixels[i]) - refLuma) > tolNormalized) {
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
