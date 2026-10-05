package org.nekomanga.presentation.screens.reader.viewer

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.PagePixels
import eu.kanade.tachiyomi.ui.reader.viewer.SmartBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PagerSmartBackgroundTest {

    private val page = ReaderPage(index = 0)
    private val extraPage = ReaderPage(index = 1)

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val art = 0xFF808080.toInt()
    private val blue = 0xFF406080.toInt()

    private fun source(
        page: ReaderPage = this.page,
        extraPage: ReaderPage? = null,
        isRtl: Boolean = false,
        invertDoublePages: Boolean = false,
        rotateWide: Boolean = false,
        rotateReverse: Boolean = false,
        cropBorders: Boolean = false,
    ) =
        smartBackgroundSource(
            page = page,
            extraPage = extraPage,
            isRtl = isRtl,
            invertDoublePages = invertDoublePages,
            rotateWide = rotateWide,
            rotateReverse = rotateReverse,
            cropBorders = cropBorders,
        )

    private fun pixels(width: Int, height: Int, color: (x: Int, y: Int) -> Int): PagePixels =
        object : PagePixels {
            override val width = width
            override val height = height

            override fun getPixel(x: Int, y: Int): Int {
                require(x in 0 until width && y in 0 until height) {
                    "($x, $y) is outside ${width}x$height"
                }
                return color(x, y)
            }
        }

    private fun decoded(pixels: PagePixels) = DecodedPage(pixels, pixels.width, pixels.height)

    @Test
    fun `double pages are ordered like DoublePageLayout`() {
        assertEquals(
            SmartBackgroundSource.Double(left = page, right = extraPage, cropBorders = false),
            source(extraPage = extraPage),
        )
        assertEquals(
            SmartBackgroundSource.Double(left = extraPage, right = page, cropBorders = false),
            source(extraPage = extraPage, isRtl = true),
        )
        assertEquals(
            SmartBackgroundSource.Double(left = extraPage, right = page, cropBorders = false),
            source(extraPage = extraPage, invertDoublePages = true),
        )
        assertEquals(
            SmartBackgroundSource.Double(left = page, right = extraPage, cropBorders = false),
            source(extraPage = extraPage, isRtl = true, invertDoublePages = true),
        )
    }

    @Test
    fun `split page uses the half SplitPageLayout shows`() {
        val firstHalf = ReaderPage(index = 0).apply { this.firstHalf = true }
        val secondHalf = ReaderPage(index = 0).apply { this.firstHalf = false }

        assertEquals(
            SmartBackgroundSource.Split(firstHalf, leftHalf = true),
            source(page = firstHalf),
        )
        assertEquals(
            SmartBackgroundSource.Split(firstHalf, leftHalf = false),
            source(page = firstHalf, isRtl = true),
        )
        assertEquals(
            SmartBackgroundSource.Split(secondHalf, leftHalf = false),
            source(page = secondHalf),
        )
        assertEquals(
            SmartBackgroundSource.Split(secondHalf, leftHalf = true),
            source(page = secondHalf, isRtl = true),
        )
    }

    @Test
    fun `split page is not cropped, as SplitPageLayout does not crop`() {
        val firstHalf = ReaderPage(index = 0).apply { this.firstHalf = true }

        assertEquals(
            SmartBackgroundSource.Split(firstHalf, leftHalf = true),
            source(page = firstHalf, cropBorders = true),
        )
    }

    @Test
    fun `single page carries the rotation and crop settings`() {
        assertEquals(
            SmartBackgroundSource.Single(
                page,
                rotateWide = true,
                rotateReverse = true,
                cropBorders = true,
            ),
            source(rotateWide = true, rotateReverse = true, cropBorders = true),
        )
        assertEquals(
            SmartBackgroundSource.Single(
                page,
                rotateWide = false,
                rotateReverse = false,
                cropBorders = false,
            ),
            source(),
        )
    }

    @Test
    fun `sampled decode is read in full image coordinates`() {
        val sampled = pixels(10, 10) { x, y -> y * 10 + x }
        val image =
            smartBackgroundImage(source()) { DecodedPage(sampled, fullWidth = 20, fullHeight = 20) }

        assertEquals(20, image?.width)
        assertEquals(20, image?.height)
        assertEquals(99, image?.getPixel(19, 19))
        assertEquals(12, image?.getPixel(4, 3))
    }

    @Test
    fun `single page is cropped before the wide check, like the Coil request`() {
        // Wide with its white side margins, tall without them.
        val wide = pixels(60, 40) { x, _ -> if (x in 15..44) art else white }

        val cropped =
            smartBackgroundImage(source(rotateWide = true, cropBorders = true)) { decoded(wide) }
        val uncropped = smartBackgroundImage(source(rotateWide = true)) { decoded(wide) }

        assertEquals(30, cropped?.width)
        assertEquals(40, cropped?.height)
        assertEquals(40, uncropped?.width)
        assertEquals(60, uncropped?.height)
    }

    @Test
    fun `double pages are cropped, then the shorter one is scaled to the taller one's height`() {
        // Midtone, so the crop leaves it whole.
        val tall = pixels(20, 40) { _, _ -> blue }
        // A 20 x 20 page inside a 4 px white margin.
        val small = pixels(28, 28) { x, y -> if (x in 4..23 && y in 4..23) art else white }
        val decodes = mapOf(page to decoded(tall), extraPage to decoded(small))

        val image =
            smartBackgroundImage(source(extraPage = extraPage, cropBorders = true)) { decodes[it] }

        assertEquals(60, image?.width)
        assertEquals(40, image?.height)
        assertEquals(blue, image?.getPixel(19, 39))
        assertEquals(art, image?.getPixel(20, 0))
        assertEquals(art, image?.getPixel(59, 39))
    }

    @Test
    fun `no image when a page cannot be decoded`() {
        val decodes = mapOf(page to decoded(pixels(20, 20) { _, _ -> art }))

        assertNull(smartBackgroundImage(source(page = extraPage)) { decodes[it] })
        assertNull(smartBackgroundImage(source(extraPage = extraPage)) { decodes[it] })
    }

    @Test
    fun `picked background is reused for the same inputs`() {
        val key = key()
        val picked = SmartBackground(black, white)
        var picks = 0

        assertEquals(
            picked,
            smartBackgroundFor(key) {
                picks++
                picked
            },
        )
        assertEquals(
            picked,
            smartBackgroundFor(key()) {
                picks++
                null
            },
        )
        assertEquals(picked, cachedSmartBackground(key()))
        assertEquals(1, picks)
    }

    @Test
    fun `background is picked again when an input changes`() {
        val first = SmartBackground(black, black)
        val second = SmartBackground(white, white)
        smartBackgroundFor(key()) { first }

        assertNull(cachedSmartBackground(key(baseColor = black)))
        assertNull(cachedSmartBackground(key(isLandscape = true)))
        assertNull(cachedSmartBackground(key(retryGeneration = 1)))
        assertNull(cachedSmartBackground(key(cropBorders = true)))
        assertEquals(second, smartBackgroundFor(key(isLandscape = true)) { second })
    }

    @Test
    fun `background is not reused when the page could not be read`() {
        var picks = 0

        assertEquals(
            SmartBackground(white, white),
            smartBackgroundFor(key()) {
                picks++
                null
            },
        )
        assertNull(cachedSmartBackground(key()))
        smartBackgroundFor(key()) {
            picks++
            null
        }
        assertEquals(2, picks)
    }

    private fun key(
        baseColor: Int = white,
        isLandscape: Boolean = false,
        retryGeneration: Int = 0,
        cropBorders: Boolean = false,
    ) =
        SmartBackgroundKey(
            source = source(cropBorders = cropBorders),
            baseColor = baseColor,
            isLandscape = isLandscape,
            retryGeneration = retryGeneration,
        )
}
