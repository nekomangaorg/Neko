package eu.kanade.tachiyomi.util.system

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
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

    data class TestCase(
        val format: Format,
        val isAnimated: Boolean,
        val sdkInt: Int,
        val expected: Boolean,
        val description: String,
    )
}
