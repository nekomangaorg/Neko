package eu.kanade.tachiyomi.util.system

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

            val typeMappings = mapOf(
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

            // Gif is always supported
            assertTrue(mockType(Format.Gif, true).isAnimatedAndSupported(sdkInt = 21))
            assertTrue(mockType(Format.Gif, true).isAnimatedAndSupported(sdkInt = 30))

            // WebP supported on P (28) +
            assertTrue(mockType(Format.Webp, true).isAnimatedAndSupported(sdkInt = 28))
            assertTrue(mockType(Format.Webp, true).isAnimatedAndSupported(sdkInt = 29))
            assertFalse(mockType(Format.Webp, true).isAnimatedAndSupported(sdkInt = 27))
            // Non animated WebP is false
            assertFalse(mockType(Format.Webp, false).isAnimatedAndSupported(sdkInt = 28))

            // Heif supported on R (30) +
            assertTrue(mockType(Format.Heif, true).isAnimatedAndSupported(sdkInt = 30))
            assertTrue(mockType(Format.Heif, true).isAnimatedAndSupported(sdkInt = 31))
            assertFalse(mockType(Format.Heif, true).isAnimatedAndSupported(sdkInt = 29))
            assertFalse(mockType(Format.Heif, false).isAnimatedAndSupported(sdkInt = 30))

            // Others false
            assertFalse(mockType(Format.Jpeg, true).isAnimatedAndSupported(sdkInt = 30))
            assertFalse(mockType(Format.Png, true).isAnimatedAndSupported(sdkInt = 30))
        }
    }
}
