import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Coil Transformation for 12-bit RGB Colored E-Ink.
 *
 * Quantizes colors of a [Bitmap] to ARGB4444 (4096 colors)
 *
 * @param useDither applies Floyd-Steinberg dithering to the quantized colors
 */
class ColorEInk16Bit(private val useDither: Boolean = true) : Transformation() {

    override val cacheKey: String = "${this::class.java.name}-$useDither"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val out = input.copy(Bitmap.Config.ARGB_8888, true)

        return if (useDither) {
            floydSteinbergDither(out)
        } else {
            quantizeToRGB444(out)
        }
    }

    /** Quantize directly to RGB444 without dithering */
    private fun quantizeToRGB444(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            // keep high 4 bits
            val r = (Color.red(c) shr 4) shl 4
            val g = (Color.green(c) shr 4) shl 4
            val b = (Color.blue(c) shr 4) shl 4
            pixels[i] = Color.rgb(r, g, b)
        }

        val out = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /** Floyd–Steinberg dithering for RGB444 */
    private fun floydSteinbergDither(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        fun clamp8bit(x: Int) = x.coerceIn(0, 255)

        fun addQuantizationError(nx: Int, ny: Int, er: Int, eg: Int, eb: Int, factor: Float) {
            if (nx in 0 until w && ny in 0 until h) {
                val j = ny * w + nx
                val c = pixels[j]
                val r = clamp8bit(Color.red(c) + (er * factor).toInt())
                val g = clamp8bit(Color.green(c) + (eg * factor).toInt())
                val b = clamp8bit(Color.blue(c) + (eb * factor).toInt())
                pixels[j] = Color.rgb(r, g, b)
            }
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val oldColor = pixels[i]
                val oldR = Color.red(oldColor)
                val newR = (oldR shr 4) shl 4
                val errR = oldR - newR
                val oldG = Color.green(oldColor)
                val newG = (oldG shr 4) shl 4
                val errG = oldG - newG
                val oldB = Color.blue(oldColor)
                val newB = (oldB shr 4) shl 4
                val errB = oldB - newB
                pixels[i] = Color.rgb(newR, newG, newB)
                addQuantizationError(x + 1, y, errR, errG, errB, 7f / 16f)
                addQuantizationError(x - 1, y + 1, errR, errG, errB, 3f / 16f)
                addQuantizationError(x, y + 1, errR, errG, errB, 5f / 16f)
                addQuantizationError(x + 1, y + 1, errR, errG, errB, 1f / 16f)
            }
        }

        src.setPixels(pixels, 0, w, 0, 0, w, h)
        return src
    }
}
