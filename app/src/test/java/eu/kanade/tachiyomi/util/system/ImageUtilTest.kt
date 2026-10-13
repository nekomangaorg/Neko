package eu.kanade.tachiyomi.util.system

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.IOException
import java.io.InputStream
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tachiyomi.decoder.Format
import tachiyomi.decoder.ImageType

class ImageUtilTest {

    @Test
    fun testToImageUtilType() {
        with(ImageUtil) {
            fun mockType(fmt: Format): ImageType {
                val m = mockk<ImageType>()
                every { m.format } returns fmt
                return m
            }

            val typeMappings =
                mapOf(
                    Format.Avif to ImageUtil.ImageType.AVIF,
                    Format.Gif to ImageUtil.ImageType.GIF,
                    Format.Heif to ImageUtil.ImageType.HEIF,
                    Format.Jpeg to ImageUtil.ImageType.JPEG,
                    Format.Jxl to ImageUtil.ImageType.JXL,
                    Format.Png to ImageUtil.ImageType.PNG,
                    Format.Webp to ImageUtil.ImageType.WEBP,
                )

            typeMappings.forEach { (format, expectedType) ->
                assertEquals(expectedType, mockType(format).toImageUtilType())
            }
        }
    }

    @Test
    fun testIsAnimatedAndSupported() {
        with(ImageUtil) {
            fun mockType(fmt: Format, animated: Boolean): ImageType {
                val m = mockk<ImageType>()
                every { m.format } returns fmt
                every { m.isAnimated } returns animated
                return m
            }

            listOf(
                    // Gif is always supported
                    TestCase(Format.Gif, true, 21, true, "Animated GIF below P"),
                    TestCase(Format.Gif, true, 30, true, "Animated GIF above P"),

                    // WebP supported on P (28) +
                    TestCase(Format.Webp, true, 28, true, "Animated WebP on P"),
                    TestCase(Format.Webp, true, 29, true, "Animated WebP on Q"),
                    TestCase(Format.Webp, true, 27, false, "Animated WebP below P"),
                    TestCase(Format.Webp, false, 28, false, "Non-animated WebP on P"),

                    // Heif supported on R (30) +
                    TestCase(Format.Heif, true, 30, true, "Animated HEIF on R"),
                    TestCase(Format.Heif, true, 31, true, "Animated HEIF on S"),
                    TestCase(Format.Heif, true, 29, false, "Animated HEIF below R"),
                    TestCase(Format.Heif, false, 30, false, "Non-animated HEIF on R"),

                    // Others are not supported
                    TestCase(Format.Jpeg, true, 30, false, "Animated JPEG"),
                    TestCase(Format.Png, true, 30, false, "Animated PNG"),
                )
                .forEach { (format, isAnimated, sdkInt, expected, description) ->
                    val mock = mockType(format, isAnimated)
                    val actual = mock.isAnimatedAndSupported(sdkInt = sdkInt)
                    assertEquals("Test case '$description' failed", expected, actual)
                }
        }
    }

    @Test
    fun testNeedsNativeDecoder() {
        assertEquals(
            listOf(ImageUtil.ImageType.AVIF, ImageUtil.ImageType.HEIF, ImageUtil.ImageType.JXL),
            ImageUtil.ImageType.entries.filter { it.needsNativeDecoder },
        )
    }

    @Test
    fun `hardware bitmap check counts the padded row`() {
        mockkObject(GLUtil.Companion)
        try {
            every { GLUtil.maxTextureSize } returns 16_384
            fun bitmap(width: Int, height: Int) =
                mockk<Bitmap> {
                    every { this@mockk.width } returns width
                    every { this@mockk.height } returns height
                }
            // 104,856,000 bytes at 5100 pixels a row, 105,267,200 with the row padded to 5120.
            assertFalse(ImageUtil.canUseHardwareBitmap(bitmap(5100, 5140)))
            assertTrue(ImageUtil.canUseHardwareBitmap(bitmap(5120, 5120)))
        } finally {
            unmockkAll()
        }
    }

    @Test
    fun `findPlatformImageMime returns null when the stream cannot be opened`() {
        mockkConstructor(BitmapFactory.Options::class)
        var opened = false

        val mime =
            try {
                ImageUtil.findPlatformImageMime {
                    opened = true
                    throw IOException("gone")
                }
            } finally {
                unmockkAll()
            }

        assertTrue(opened)
        assertNull(mime)
    }

    @Test
    fun `extractImageOptions reads the dimensions without reading the whole file`() {
        val upstream = CountingSource(size = 8L * 1024 * 1024)
        mockkConstructor(BitmapFactory.Options::class)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), any(), any()) } answers
            {
                // The PNG signature and IHDR chunk are the first 33 bytes.
                firstArg<InputStream>().read(ByteArray(33))
                thirdArg<BitmapFactory.Options>().apply {
                    outWidth = 750
                    outHeight = 129357
                }
                null
            }
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } answers
            {
                lastArg<BitmapFactory.Options>().apply {
                    outWidth = 750
                    outHeight = 129357
                }
                null
            }

        val options =
            try {
                upstream.buffer().use { ImageUtil.extractImageOptions(it) }
            } finally {
                unmockkAll()
            }

        assertEquals(750, options.outWidth)
        assertEquals(129357, options.outHeight)
        assertTrue("read ${upstream.bytesRead} bytes", upstream.bytesRead < 1024 * 1024)
    }

    @Test
    fun `extractImageOptions reads the whole file when the stream gives no dimensions`() {
        val upstream = CountingSource(size = 64L * 1024)
        mockkConstructor(BitmapFactory.Options::class)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), any(), any()) } returns null
        var decodedBytes = -1
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } answers
            {
                decodedBytes = thirdArg()
                lastArg<BitmapFactory.Options>().apply {
                    outWidth = 750
                    outHeight = 129357
                }
                null
            }

        val options =
            try {
                upstream.buffer().use { ImageUtil.extractImageOptions(it) }
            } finally {
                unmockkAll()
            }

        assertEquals(64 * 1024, decodedBytes)
        assertEquals(750, options.outWidth)
        assertEquals(129357, options.outHeight)
    }

    /** A source of [size] zero bytes that counts how many of them were read. */
    private class CountingSource(private val size: Long) : Source {
        var bytesRead = 0L
            private set

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (bytesRead == size) return -1
            val count = minOf(byteCount, size - bytesRead)
            sink.write(ByteArray(count.toInt()))
            bytesRead += count
            return count
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() {}
    }

    data class TestCase(
        val format: Format,
        val isAnimated: Boolean,
        val sdkInt: Int,
        val expected: Boolean,
        val description: String,
    )
}
