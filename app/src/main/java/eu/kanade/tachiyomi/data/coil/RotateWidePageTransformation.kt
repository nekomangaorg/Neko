package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.Matrix
import coil3.size.Size
import coil3.transform.Transformation
import org.nekomanga.logging.TimberKt

/**
 * Coil 3 image transformation that rotates wide pages (spreads where aspect ratio width /
 * height > [MIN_WIDE_RATIO]) to fit portrait screens.
 *
 * @param rotateWide Whether wide pages should be rotated.
 * @param reverse Whether the rotation direction is flipped (-90 degrees instead of 90 degrees).
 */
class RotateWidePageTransformation(
    val rotateWide: Boolean = true,
    val reverse: Boolean = false,
) : Transformation() {

    override val cacheKey: String =
        "RotateWidePageTransformation_rotate_${rotateWide}_rev_${reverse}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (!shouldRotate(input.width, input.height, rotateWide) || input.isRecycled) {
            return input
        }

        val degrees = rotationDegrees(reverse)
        val sourceBitmap =
            if (input.config == Bitmap.Config.HARDWARE) {
                val copy = input.copy(Bitmap.Config.ARGB_8888, false)
                if (copy == null) {
                    TimberKt.e { "Failed to copy HARDWARE bitmap, returning original input" }
                    return input
                }
                copy
            } else {
                input
            }

        val matrix = Matrix().apply { postRotate(degrees) }

        return try {
            val rotated =
                Bitmap.createBitmap(
                    sourceBitmap,
                    0,
                    0,
                    sourceBitmap.width,
                    sourceBitmap.height,
                    matrix,
                    true,
                )
            if (sourceBitmap !== input) {
                sourceBitmap.recycle()
            }
            rotated
        } catch (e: OutOfMemoryError) {
            TimberKt.e(e) { "OutOfMemoryError rotating wide page" }
            if (sourceBitmap !== input) {
                sourceBitmap.recycle()
            }
            input
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RotateWidePageTransformation) return false
        return rotateWide == other.rotateWide && reverse == other.reverse
    }

    override fun hashCode(): Int {
        var result = rotateWide.hashCode()
        result = 31 * result + reverse.hashCode()
        return result
    }

    override fun toString(): String {
        return "RotateWidePageTransformation(rotateWide=$rotateWide, reverse=$reverse)"
    }

    companion object {
        const val MIN_WIDE_RATIO = 1.05f

        fun shouldRotate(width: Int, height: Int, rotateWide: Boolean): Boolean {
            return rotateWide &&
                width > 0 &&
                height > 0 &&
                (width.toFloat() / height.toFloat() > MIN_WIDE_RATIO)
        }

        fun rotationDegrees(reverse: Boolean): Float {
            return if (reverse) -90f else 90f
        }

        /**
         * Determines if wide pages should be rotated given the viewport orientation. Wide pages are
         * only rotated when the user setting is enabled and the viewport is strictly in portrait
         * orientation (viewport width < viewport height).
         */
        fun shouldRotateForViewport(
            rotateWide: Boolean,
            viewportWidth: Float,
            viewportHeight: Float,
        ): Boolean {
            val isPortrait =
                viewportWidth > 0f && viewportHeight > 0f && viewportWidth < viewportHeight
            return rotateWide && isPortrait
        }
    }
}
