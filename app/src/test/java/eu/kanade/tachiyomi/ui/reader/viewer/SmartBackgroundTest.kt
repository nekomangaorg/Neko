package eu.kanade.tachiyomi.ui.reader.viewer

import org.junit.Assert.assertEquals
import org.junit.Test

class SmartBackgroundTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val art = 0xFF808080.toInt()

    private fun page(width: Int, height: Int, color: (x: Int, y: Int) -> Int): PagePixels =
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

    private fun solid(color: Int) = SmartBackground(color, color)

    @Test
    fun `no image gives the base color`() {
        assertEquals(
            solid(white),
            SmartBackgroundAnalyzer.analyze(null, backgroundColor = white, isLandscape = false),
        )
    }

    @Test
    fun `image under 50 px gives the base color`() {
        val image = page(49, 200) { _, _ -> black }

        assertEquals(
            solid(white),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = white, isLandscape = false),
        )
    }

    @Test
    fun `white page with a thin frame inside white margins gives the base color`() {
        // The page from the report: white margins around a thin black frame.
        val image =
            page(200, 300) { x, y ->
                val onFrame =
                    (x in 20..180 && (y in 20..21 || y in 278..279)) ||
                        (y in 20..279 && (x in 20..21 || x in 179..180))
                when {
                    onFrame -> black
                    x in 30..170 && y in 30..270 -> art
                    else -> white
                }
            }

        assertEquals(
            solid(white),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = white, isLandscape = false),
        )
        assertEquals(
            solid(black),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = black, isLandscape = false),
        )
    }

    @Test
    fun `page with uniform dark edges gives the edge color`() {
        val nearBlack = 0xFF101010.toInt()
        val image = page(200, 200) { x, y -> if (x in 40..159 && y in 40..159) art else nearBlack }

        assertEquals(
            solid(nearBlack),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = white, isLandscape = false),
        )
    }

    @Test
    fun `page with uniform colored edges gives the edge color`() {
        val sepia = 0xFFF0E0C0.toInt()
        val image = page(200, 200) { x, y -> if (x in 40..159 && y in 40..159) art else sepia }

        assertEquals(
            solid(sepia),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = black, isLandscape = false),
        )
    }

    @Test
    fun `dark top band in portrait gives a dark top and the base color at the bottom`() {
        val image = page(200, 200) { _, y -> if (y < 80) black else white }

        assertEquals(
            SmartBackground(top = black, bottom = white),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = white, isLandscape = false),
        )
    }

    @Test
    fun `dark top band in landscape gives the base color`() {
        val image = page(200, 200) { _, y -> if (y < 80) black else white }

        assertEquals(
            solid(white),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = white, isLandscape = true),
        )
    }

    @Test
    fun `dark bottom band in portrait gives the base color at the top and a dark bottom`() {
        val image = page(200, 200) { _, y -> if (y < 120) white else black }

        assertEquals(
            SmartBackground(top = white, bottom = black),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = white, isLandscape = false),
        )
    }

    @Test
    fun `dark page with white bottom corners gives a dark top and the base color at the bottom`() {
        val image = page(200, 200) { _, y -> if (y < 170) black else white }

        assertEquals(
            SmartBackground(top = black, bottom = white),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = white, isLandscape = false),
        )
    }

    @Test
    fun `saturated dark color is not counted as dark`() {
        val grey = 0xFF202020.toInt()
        val red = 0xFF200000.toInt()
        val greyBand = page(200, 200) { _, y -> if (y < 80) grey else white }
        val redBand = page(200, 200) { _, y -> if (y < 80) red else white }

        assertEquals(
            SmartBackground(top = grey, bottom = white),
            SmartBackgroundAnalyzer.analyze(greyBand, backgroundColor = white, isLandscape = false),
        )
        assertEquals(
            solid(white),
            SmartBackgroundAnalyzer.analyze(redBand, backgroundColor = white, isLandscape = false),
        )
    }

    @Test
    fun `image 100 px wide is read inside its bounds`() {
        // The right offset column plus its offset lands on x = width for widths 100 to 109.
        val image = page(100, 200) { _, _ -> white }

        assertEquals(
            solid(black),
            SmartBackgroundAnalyzer.analyze(image, backgroundColor = black, isLandscape = false),
        )
    }

    @Test
    fun `scaled pixels read the smaller image at the matching position`() {
        val small = page(2, 2) { x, y -> y * 10 + x }
        val scaled = small.scaledTo(width = 4, height = 4)

        assertEquals(4, scaled.width)
        assertEquals(4, scaled.height)
        assertEquals(11, scaled.getPixel(3, 3))
        assertEquals(10, scaled.getPixel(1, 2))
        assertEquals(0, scaled.getPixel(1, 1))
    }

    @Test
    fun `clockwise rotation moves the left column to the top row`() {
        // 0  1  2
        // 10 11 12
        val image = page(3, 2) { x, y -> y * 10 + x }
        val rotated = image.rotated(clockwise = true)

        assertEquals(2, rotated.width)
        assertEquals(3, rotated.height)
        assertEquals(10, rotated.getPixel(0, 0))
        assertEquals(0, rotated.getPixel(1, 0))
        assertEquals(12, rotated.getPixel(0, 2))
        assertEquals(2, rotated.getPixel(1, 2))
    }

    @Test
    fun `counterclockwise rotation moves the right column to the top row`() {
        val image = page(3, 2) { x, y -> y * 10 + x }
        val rotated = image.rotated(clockwise = false)

        assertEquals(2, rotated.width)
        assertEquals(3, rotated.height)
        assertEquals(2, rotated.getPixel(0, 0))
        assertEquals(12, rotated.getPixel(1, 0))
        assertEquals(0, rotated.getPixel(0, 2))
        assertEquals(10, rotated.getPixel(1, 2))
    }

    @Test
    fun `half reads the left or right part of the image`() {
        val image = page(5, 1) { x, _ -> x }

        val left = image.half(left = true)
        assertEquals(2, left.width)
        assertEquals(1, left.height)
        assertEquals(0, left.getPixel(0, 0))
        assertEquals(1, left.getPixel(1, 0))

        val right = image.half(left = false)
        assertEquals(3, right.width)
        assertEquals(2, right.getPixel(0, 0))
        assertEquals(4, right.getPixel(2, 0))
    }

    @Test
    fun `merged pages sit side by side, centered vertically on the fill color`() {
        val fill = 9
        val merged =
            mergedPagePixels(
                left = page(2, 4) { _, _ -> 1 },
                right = page(3, 2) { _, _ -> 2 },
                fill = fill,
            )

        assertEquals(5, merged.width)
        assertEquals(4, merged.height)
        assertEquals(1, merged.getPixel(0, 0))
        assertEquals(1, merged.getPixel(1, 3))
        assertEquals(fill, merged.getPixel(2, 0))
        assertEquals(2, merged.getPixel(2, 1))
        assertEquals(2, merged.getPixel(4, 2))
        assertEquals(fill, merged.getPixel(4, 3))
    }

    @Test
    fun `sample size keeps the longer side at 1024 px or less`() {
        assertEquals(1, SmartBackgroundAnalyzer.sampleSizeFor(1024, 1024))
        assertEquals(2, SmartBackgroundAnalyzer.sampleSizeFor(1000, 1500))
        assertEquals(4, SmartBackgroundAnalyzer.sampleSizeFor(3000, 2000))
        assertEquals(16, SmartBackgroundAnalyzer.sampleSizeFor(800, 12000))
        assertEquals(1, SmartBackgroundAnalyzer.sampleSizeFor(0, 0))
    }
}
