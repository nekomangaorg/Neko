package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import io.mockk.mockk
import kotlin.math.ceil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaxBitmapBytesTest {

    private val textureBox = Size(32_767, 32_767)
    private val maxBytes = 100L * 1024 * 1024

    @Test
    fun `page within the byte limit keeps the texture box`() {
        // 38 MB, so it decodes at full size; the 4096 box made it 273x4096.
        assertEquals(textureBox, byteLimitedMaxSize(800, 12_000, textureBox, maxBytes))
    }

    @Test
    fun `wide page over the byte limit gets a smaller box`() {
        // The 8016x6000 PNG from the Discord crash report takes 192 MB at full size.
        assertEquals(Size(5916, 5916), byteLimitedMaxSize(8016, 6000, textureBox, maxBytes))
    }

    @Test
    fun `smaller texture box wins over the byte box`() {
        assertEquals(Size(4096, 4096), byteLimitedMaxSize(8016, 6000, Size(4096, 4096), maxBytes))
    }

    @Test
    fun `original max size gets the byte box`() {
        assertEquals(
            Size(16_185, 16_185),
            byteLimitedMaxSize(2000, 20_000, Size.ORIGINAL, maxBytes),
        )
    }

    @Test
    fun `high bit depth page counts 8 bytes per pixel`() {
        // A 16-bit RGB PNG decodes to RGBA_F16: 800x20000 takes 128 MB, 800x12000 takes 77 MB.
        assertEquals(Size(18_088, 18_088), byteLimitedMaxSize(800, 20_000, textureBox, maxBytes, 8))
        assertEquals(textureBox, byteLimitedMaxSize(800, 12_000, textureBox, maxBytes, 8))
    }

    @Test
    fun `no byte limit keeps the max size`() {
        assertEquals(Size.ORIGINAL, byteLimitedMaxSize(8016, 6000, Size.ORIGINAL, 0L))
    }

    @Test
    fun `page fitted to the box stays within the limit in either orientation`() {
        // Decoders round each side, and Coil swaps the sides of a JPEG rotated by EXIF.
        val pages =
            listOf(8016 to 6000, 2000 to 20_000, 800 to 40_000, 5121 to 5121, 30_000 to 30_000)
        for ((width, height) in pages) {
            for (bytesPerPixel in listOf(4, 8)) {
                val box = byteLimitedMaxSize(width, height, textureBox, maxBytes, bytesPerPixel)
                val boxWidth = (box.width as Dimension.Pixels).px
                val boxHeight = (box.height as Dimension.Pixels).px
                for ((w, h) in listOf(width to height, height to width)) {
                    val scale = minOf(boxWidth.toDouble() / w, boxHeight.toDouble() / h)
                    val pixels = ceil(w * scale).toLong() * ceil(h * scale).toLong()
                    val bytes = pixels * bytesPerPixel
                    assertTrue("${w}x$h in $box takes $bytes bytes", bytes <= maxBytes)
                }
            }
        }
    }

    @Test
    fun `pager request for a page over the byte limit is byte limited`() {
        val limited = byteLimitedMaxSize(8016, 6000, Size.ORIGINAL, maxBytes)
        assertTrue(
            isByteLimited(
                8016,
                6000,
                textureBox,
                Scale.FIT,
                Precision.INEXACT,
                Size.ORIGINAL,
                limited,
            )
        )
    }

    @Test
    fun `webtoon request for a page over the byte limit is byte limited`() {
        val limited = byteLimitedMaxSize(8016, 6000, textureBox, maxBytes)
        assertTrue(
            isByteLimited(
                8016,
                6000,
                Size.ORIGINAL,
                Scale.FIT,
                Precision.EXACT,
                textureBox,
                limited,
            )
        )
    }

    @Test
    fun `page that the texture box already shrinks below the byte limit is not byte limited`() {
        val box = Size(4096, 4096)
        val pagerLimit = byteLimitedMaxSize(8016, 6000, Size.ORIGINAL, maxBytes)
        assertFalse(
            isByteLimited(8016, 6000, box, Scale.FIT, Precision.INEXACT, Size.ORIGINAL, pagerLimit)
        )
        val webtoonLimit = byteLimitedMaxSize(8016, 6000, box, maxBytes)
        assertFalse(
            isByteLimited(8016, 6000, Size.ORIGINAL, Scale.FIT, Precision.EXACT, box, webtoonLimit)
        )
    }

    @Test
    fun `byte box below a texture box that also shrinks the page is byte limited`() {
        // 16384 is a common phone texture size. The box makes this page 1638x16384, the byte
        // limit 1618x16185.
        val box = Size(16_384, 16_384)
        val pagerLimit = byteLimitedMaxSize(2000, 20_000, Size.ORIGINAL, maxBytes)
        assertTrue(
            isByteLimited(
                2000,
                20_000,
                box,
                Scale.FIT,
                Precision.INEXACT,
                Size.ORIGINAL,
                pagerLimit,
            )
        )
        val webtoonLimit = byteLimitedMaxSize(2000, 20_000, box, maxBytes)
        assertTrue(
            isByteLimited(
                2000,
                20_000,
                Size.ORIGINAL,
                Scale.FIT,
                Precision.EXACT,
                box,
                webtoonLimit,
            )
        )
    }

    @Test
    fun `page under the byte limit is not byte limited`() {
        val limited = byteLimitedMaxSize(800, 12_000, textureBox, maxBytes)
        assertFalse(
            isByteLimited(
                800,
                12_000,
                Size.ORIGINAL,
                Scale.FIT,
                Precision.EXACT,
                textureBox,
                limited,
            )
        )
    }

    @Test
    fun `byte limit is part of the memory cache key`() {
        val context = mockk<Context>(relaxed = true)
        val capped = ImageRequest.Builder(context).data("page_0").maxBitmapBytes(maxBytes).build()
        assertEquals(maxBytes.toString(), capped.memoryCacheKeyExtras[maxBitmapBytesMemoryCacheKey])
        val uncapped = ImageRequest.Builder(context).data("page_0").maxBitmapBytes(0L).build()
        assertNull(uncapped.memoryCacheKeyExtras[maxBitmapBytesMemoryCacheKey])
    }
}
