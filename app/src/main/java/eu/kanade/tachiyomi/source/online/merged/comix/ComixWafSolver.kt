package eu.kanade.tachiyomi.source.online.merged.comix

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Recovers the clockwise angle by which a centered square crop was rotated.
 *
 * `rotatedCrop` is the central `size×size` region of `original`, rotated around its center so that
 * pixels with no source data became transparent. The reference is the same central region read
 * straight from `original`, so only the rotation angle needs to be estimated.
 *
 * Scoring samples `rotatedCrop` at the back-rotated coordinates of each reference pixel (bilinear
 * interpolation) and accumulates mean-squared error over pixels that are both in-bounds and opaque.
 * Out-of-bounds and transparent samples are skipped entirely - the transparent corner wedges are
 * invalid data, not black. Normalizing by the valid pixel count keeps the metric comparable across
 * angles that expose different amounts of valid area.
 *
 * A 1-degree scan on a downsampled grid locates the peak cheaply; the returned angle is the integer
 * degree (0-359) the server expects. The whole search reuses a single set of pixel buffers; no
 * rotated bitmap is allocated per angle.
 */
object ComixWafSolver {

    private const val ALPHA_THRESHOLD = 128f
    private const val COARSE_GRID = 128
    private const val MIN_VALID_PIXELS = 100

    fun estimateRotationAngle(original: Bitmap, rotatedCrop: Bitmap): Int {
        val size = rotatedCrop.width
        val total = size * size
        val offset = (original.width - size) / 2

        val refGray = FloatArray(total)
        val rotGray = FloatArray(total)
        val rotAlpha = FloatArray(total)

        val refPixels = IntArray(total)
        val rotPixels = IntArray(total)
        original.getPixels(refPixels, 0, size, offset, offset, size, size)
        rotatedCrop.getPixels(rotPixels, 0, size, 0, 0, size, size)

        for (i in 0 until total) {
            val r = refPixels[i]
            refGray[i] = (((r ushr 16) and 0xFF) + ((r ushr 8) and 0xFF) + (r and 0xFF)) / 3f
            val t = rotPixels[i]
            rotGray[i] = (((t ushr 16) and 0xFF) + ((t ushr 8) and 0xFF) + (t and 0xFF)) / 3f
            rotAlpha[i] = (t ushr 24).toFloat()
        }

        val center = (size - 1) / 2f

        // Coarse scan: 1-degree steps over the whole circle on a small grid.
        val scale = size.toFloat() / COARSE_GRID
        val gridPoints = COARSE_GRID * COARSE_GRID
        val gridX = FloatArray(gridPoints)
        val gridY = FloatArray(gridPoints)
        val refSamples = FloatArray(gridPoints)
        for (i in 0 until gridPoints) {
            val gx = (i % COARSE_GRID) * scale
            val gy = (i / COARSE_GRID) * scale
            gridX[i] = gx
            gridY[i] = gy
            refSamples[i] = refGray[floor(gy).toInt() * size + floor(gx).toInt()]
        }

        var bestAngle = 0
        var bestScore = Float.MAX_VALUE
        for (a in 0 until 360) {
            val rad = a * PI / 180.0
            val ca = cos(rad).toFloat()
            val sa = sin(rad).toFloat()
            var sum = 0f
            var valid = 0
            for (i in 0 until gridPoints) {
                val vx = gridX[i] - center
                val vy = gridY[i] - center
                val qx = center + vx * ca + vy * sa
                val qy = center - vx * sa + vy * ca
                if (qx < 0f || qx >= size || qy < 0f || qy >= size) continue
                val idx = floor(qy).toInt() * size + floor(qx).toInt()
                if (rotAlpha[idx] <= ALPHA_THRESHOLD) continue
                val d = refSamples[i] - rotGray[idx]
                sum += d * d
                valid++
            }
            if (valid < MIN_VALID_PIXELS) continue
            val score = sum / valid
            if (score < bestScore) {
                bestScore = score
                bestAngle = a
            }
        }

        return bestAngle
    }
}
