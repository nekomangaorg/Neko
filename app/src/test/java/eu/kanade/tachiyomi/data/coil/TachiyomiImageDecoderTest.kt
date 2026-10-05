package eu.kanade.tachiyomi.data.coil

import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class TachiyomiImageDecoderTest {

    private val textureBox = Size(4096, 4096)

    @Test
    fun `tall page shrinks to fit a size box`() {
        // Coil's BitmapFactoryDecoder gave the same page as a JPEG 410x4096 on neko36.
        val target =
            nativeDecodeTarget(
                2000,
                20_000,
                textureBox,
                Scale.FIT,
                Precision.INEXACT,
                Size.ORIGINAL,
            )
        assertEquals(4, target.sampleSize)
        assertEquals(410 to 4096, target.outputSize(500, 5000))
    }

    @Test
    fun `tall page shrinks to fit maxBitmapSize when the size is original`() {
        val target =
            nativeDecodeTarget(2000, 20_000, Size.ORIGINAL, Scale.FIT, Precision.EXACT, textureBox)
        assertEquals(4, target.sampleSize)
        assertEquals(410 to 4096, target.outputSize(500, 5000))
    }

    @Test
    fun `original size without a max keeps full resolution`() {
        val target =
            nativeDecodeTarget(
                2000,
                20_000,
                Size.ORIGINAL,
                Scale.FIT,
                Precision.EXACT,
                Size.ORIGINAL,
            )
        assertEquals(1, target.sampleSize)
        assertEquals(2000 to 20_000, target.outputSize(2000, 20_000))
    }

    @Test
    fun `inexact request does not upscale a page smaller than the box`() {
        val target =
            nativeDecodeTarget(1500, 2200, textureBox, Scale.FIT, Precision.INEXACT, Size.ORIGINAL)
        assertEquals(1, target.sampleSize)
        assertEquals(1500 to 2200, target.outputSize(1500, 2200))
    }

    @Test
    fun `exact request samples then scales to the requested size`() {
        val target =
            nativeDecodeTarget(
                1000,
                1500,
                Size(300, 450),
                Scale.FIT,
                Precision.EXACT,
                Size.ORIGINAL,
            )
        assertEquals(2, target.sampleSize)
        assertEquals(300 to 450, target.outputSize(500, 750))
    }

    @Test
    fun `exact request upscales like BitmapFactoryDecoder`() {
        val target =
            nativeDecodeTarget(1500, 2200, textureBox, Scale.FIT, Precision.EXACT, Size.ORIGINAL)
        assertEquals(1, target.sampleSize)
        assertEquals(2793 to 4096, target.outputSize(1500, 2200))
    }

    @Test
    fun `sampled size the decoder returned is kept when no scaling is needed`() {
        val target =
            nativeDecodeTarget(
                2000,
                20_000,
                Size(1000, 10_000),
                Scale.FIT,
                Precision.EXACT,
                Size.ORIGINAL,
            )
        assertEquals(2, target.sampleSize)
        assertEquals(999 to 9999, target.outputSize(999, 9999))
    }
}
