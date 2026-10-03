package eu.kanade.tachiyomi.ui.reader.viewer

import androidx.annotation.ColorInt
import eu.kanade.tachiyomi.util.system.toInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** ARGB pixels of a page image. */
interface PagePixels {
    val width: Int
    val height: Int

    @ColorInt fun getPixel(x: Int, y: Int): Int
}

/**
 * Background behind a page for the smart reader themes. [top] fills the top third and [bottom] the
 * bottom third, blending in the middle third. Both are the same color for a solid background.
 */
data class SmartBackground(@param:ColorInt val top: Int, @param:ColorInt val bottom: Int)

/** Picks the background for a page from the colors at its edges. */
object SmartBackgroundAnalyzer {

    private const val EDGE_OFFSET = 5
    private const val MARGIN_PERCENTAGE = 0.0275
    private const val CENTER_OFFSET_PERCENTAGE = 0.01
    private const val MAX_SAMPLED_SIDE = 1024

    /**
     * @param image the page, or null when it could not be decoded
     * @param backgroundColor the color used where the page edges are not dark
     */
    fun analyze(
        image: PagePixels?,
        @ColorInt backgroundColor: Int,
        isLandscape: Boolean,
    ): SmartBackground {
        if (image == null) return SmartBackground(backgroundColor, backgroundColor)
        if (image.width < 50 || image.height < 50) {
            return SmartBackground(backgroundColor, backgroundColor)
        }
        val top = EDGE_OFFSET
        val bot = image.height - EDGE_OFFSET
        val left = (image.width * MARGIN_PERCENTAGE).toInt()
        val right = image.width - left
        val midX = image.width / 2
        val midY = image.height / 2
        val offsetX = (image.width * CENTER_OFFSET_PERCENTAGE).toInt()
        val topLeftIsDark = image.getPixel(left, top).isDark
        val topRightIsDark = image.getPixel(right, top).isDark
        val midLeftIsDark = image.getPixel(left, midY).isDark
        val midRightIsDark = image.getPixel(right, midY).isDark
        val topMidIsDark = image.getPixel(midX, top).isDark
        val botLeftIsDark = image.getPixel(left, bot).isDark
        val botRightIsDark = image.getPixel(right, bot).isDark

        var darkBG =
            (topLeftIsDark &&
                (botLeftIsDark ||
                    botRightIsDark ||
                    topRightIsDark ||
                    midLeftIsDark ||
                    topMidIsDark)) ||
                (topRightIsDark &&
                    (botRightIsDark || botLeftIsDark || midRightIsDark || topMidIsDark))

        if (
            !image.getPixel(left, top).isWhite &&
                pixelIsClose(image.getPixel(left, top), image.getPixel(midX, top)) &&
                !image.getPixel(right, top).isWhite &&
                pixelIsClose(image.getPixel(right, top), image.getPixel(right, bot)) &&
                !image.getPixel(right, bot).isWhite &&
                pixelIsClose(image.getPixel(right, bot), image.getPixel(midX, bot)) &&
                !image.getPixel(midX, top).isWhite &&
                pixelIsClose(image.getPixel(midX, top), image.getPixel(right, top)) &&
                !image.getPixel(midX, bot).isWhite &&
                pixelIsClose(image.getPixel(midX, bot), image.getPixel(left, bot)) &&
                !image.getPixel(left, bot).isWhite &&
                pixelIsClose(image.getPixel(left, bot), image.getPixel(left, top))
        ) {
            val edgeColor = image.getPixel(left, top)
            return SmartBackground(edgeColor, edgeColor)
        }

        if (
            image.getPixel(left, top).isWhite.toInt() +
                image.getPixel(right, top).isWhite.toInt() +
                image.getPixel(left, bot).isWhite.toInt() +
                image.getPixel(right, bot).isWhite.toInt() > 2
        ) {
            darkBG = false
        }

        var blackPixel =
            when {
                topLeftIsDark -> image.getPixel(left, top)
                topRightIsDark -> image.getPixel(right, top)
                botLeftIsDark -> image.getPixel(left, bot)
                botRightIsDark -> image.getPixel(right, bot)
                else -> backgroundColor
            }

        var overallWhitePixels = 0
        var overallBlackPixels = 0
        var topBlackStreak = 0
        var topWhiteStreak = 0
        var botBlackStreak = 0
        var botWhiteStreak = 0
        outer@ for (x in intArrayOf(left, right, left - offsetX, right + offsetX)) {
            var whitePixelsStreak = 0
            var whitePixels = 0
            var blackPixelsStreak = 0
            var blackPixels = 0
            var blackStreak = false
            var whiteStrak = false
            val notOffset = x == left || x == right
            // Widths 100 to 109 put the outer right column plus its offset at x = width.
            val offX =
                (x + (if (x < image.width / 2) -offsetX else offsetX)).coerceAtMost(image.width - 1)
            for ((index, y) in (0 until image.height step image.height / 25).withIndex()) {
                val pixel = image.getPixel(x, y)
                val pixelOff = image.getPixel(offX, y)
                if (pixel.isWhite) {
                    whitePixelsStreak++
                    whitePixels++
                    if (notOffset) {
                        overallWhitePixels++
                    }
                    if (whitePixelsStreak > 14) {
                        whiteStrak = true
                    }
                    if (whitePixelsStreak > 6 && whitePixelsStreak >= index - 1) {
                        topWhiteStreak = whitePixelsStreak
                    }
                } else {
                    whitePixelsStreak = 0
                    if (pixel.isDark && pixelOff.isDark) {
                        blackPixels++
                        if (notOffset) {
                            overallBlackPixels++
                        }
                        blackPixelsStreak++
                        if (blackPixelsStreak >= 14) {
                            blackStreak = true
                        }
                        continue
                    }
                }
                if (blackPixelsStreak > 6 && blackPixelsStreak >= index - 1) {
                    topBlackStreak = blackPixelsStreak
                }
                blackPixelsStreak = 0
            }
            if (blackPixelsStreak > 6) {
                botBlackStreak = blackPixelsStreak
            } else if (whitePixelsStreak > 6) {
                botWhiteStreak = whitePixelsStreak
            }
            when {
                blackPixels > 22 -> {
                    if (x == right || x == right + offsetX) {
                        blackPixel =
                            when {
                                topRightIsDark -> image.getPixel(right, top)
                                botRightIsDark -> image.getPixel(right, bot)
                                else -> blackPixel
                            }
                    }
                    darkBG = true
                    overallWhitePixels = 0
                    break@outer
                }
                blackStreak -> {
                    darkBG = true
                    if (x == right || x == right + offsetX) {
                        blackPixel =
                            when {
                                topRightIsDark -> image.getPixel(right, top)
                                botRightIsDark -> image.getPixel(right, bot)
                                else -> blackPixel
                            }
                    }
                    if (blackPixels > 18) {
                        overallWhitePixels = 0
                        break@outer
                    }
                }
                whiteStrak || whitePixels > 22 -> darkBG = false
            }
        }

        val topIsBlackStreak = topBlackStreak > topWhiteStreak
        val bottomIsBlackStreak = botBlackStreak > botWhiteStreak
        if (overallWhitePixels > 9 && overallWhitePixels > overallBlackPixels) {
            darkBG = false
        }
        if (topIsBlackStreak && bottomIsBlackStreak) {
            darkBG = true
        }
        if (darkBG) {
            return if (
                !isLandscape &&
                    image.getPixel(left, bot).isWhite &&
                    image.getPixel(right, bot).isWhite
            ) {
                SmartBackground(top = blackPixel, bottom = backgroundColor)
            } else if (
                !isLandscape &&
                    image.getPixel(left, top).isWhite &&
                    image.getPixel(right, top).isWhite
            ) {
                SmartBackground(top = backgroundColor, bottom = blackPixel)
            } else {
                SmartBackground(blackPixel, blackPixel)
            }
        }
        if (
            !isLandscape &&
                (topIsBlackStreak ||
                    (topLeftIsDark &&
                        topRightIsDark &&
                        image.getPixel(left - offsetX, top).isDark &&
                        image.getPixel(right + offsetX, top).isDark &&
                        (topMidIsDark || overallBlackPixels > 9)))
        ) {
            return SmartBackground(top = blackPixel, bottom = backgroundColor)
        } else if (
            !isLandscape &&
                (bottomIsBlackStreak ||
                    (botLeftIsDark &&
                        botRightIsDark &&
                        image.getPixel(left - offsetX, bot).isDark &&
                        image.getPixel(right + offsetX, bot).isDark &&
                        (image.getPixel(midX, bot).isDark || overallBlackPixels > 9)))
        ) {
            return SmartBackground(top = backgroundColor, bottom = blackPixel)
        }
        return SmartBackground(backgroundColor, backgroundColor)
    }

    /** Power of two sample size that keeps the longer side at [MAX_SAMPLED_SIDE] px or less. */
    fun sampleSizeFor(width: Int, height: Int): Int {
        val longerSide = max(width, height)
        var sampleSize = 1
        while (longerSide / sampleSize > MAX_SAMPLED_SIDE) {
            sampleSize *= 2
        }
        return sampleSize
    }

    internal fun isWhite(@ColorInt color: Int): Boolean = color.isWhite

    private val Int.red: Int
        get() = (this shr 16) and 0xFF

    private val Int.green: Int
        get() = (this shr 8) and 0xFF

    private val Int.blue: Int
        get() = this and 0xFF

    private val Int.alpha: Int
        get() = (this ushr 24) and 0xFF

    private val Int.isWhite: Boolean
        get() = red + blue + green > 740

    private val Int.isDark: Boolean
        get() = red < 40 && blue < 40 && green < 40 && alpha > 200 && saturation <= 0.2f

    // HSL saturation as androidx ColorUtils.RGBToHSL computes it. ColorUtils.colorToHSL reads the
    // channels through android.graphics.Color, which plain JVM unit tests cannot call.
    private val Int.saturation: Float
        get() {
            val rf = red / 255f
            val gf = green / 255f
            val bf = blue / 255f
            val max = max(rf, max(gf, bf))
            val min = min(rf, min(gf, bf))
            if (max == min) return 0f
            val l = (max + min) / 2f
            return ((max - min) / (1f - abs(2f * l - 1f))).coerceIn(0f, 1f)
        }

    private fun pixelIsClose(color1: Int, color2: Int): Boolean {
        return abs(color1.red - color2.red) < 30 &&
            abs(color1.green - color2.green) < 30 &&
            abs(color1.blue - color2.blue) < 30
    }
}

/**
 * Reads a smaller copy of an image, such as a sampled decode, at the coordinates of the full one.
 */
fun PagePixels.scaledTo(width: Int, height: Int): PagePixels {
    val source = this
    return object : PagePixels {
        override val width = width
        override val height = height

        override fun getPixel(x: Int, y: Int): Int =
            source.getPixel(
                (x.toLong() * source.width / width).toInt(),
                (y.toLong() * source.height / height).toInt(),
            )
    }
}

/** The image turned a quarter turn, as RotateWidePageTransformation turns wide pages. */
fun PagePixels.rotated(clockwise: Boolean): PagePixels {
    val source = this
    return object : PagePixels {
        override val width = source.height
        override val height = source.width

        override fun getPixel(x: Int, y: Int): Int =
            if (clockwise) {
                source.getPixel(y, source.height - 1 - x)
            } else {
                source.getPixel(source.width - 1 - y, x)
            }
    }
}

/** The left or right half of the image, as a split double page shows it. */
fun PagePixels.half(left: Boolean): PagePixels {
    val source = this
    val start = if (left) 0 else source.width / 2
    return object : PagePixels {
        override val width = if (left) source.width / 2 else source.width - source.width / 2
        override val height = source.height

        override fun getPixel(x: Int, y: Int): Int = source.getPixel(start + x, y)
    }
}

/**
 * Two pages side by side, each centered vertically, with [fill] around the shorter one. This is the
 * layout the View reader merged double pages into before analyzing them.
 */
fun mergedPagePixels(left: PagePixels, right: PagePixels, @ColorInt fill: Int): PagePixels =
    object : PagePixels {
        override val width = left.width + right.width
        override val height = max(left.height, right.height)

        override fun getPixel(x: Int, y: Int): Int {
            val page = if (x < left.width) left else right
            val pageX = if (x < left.width) x else x - left.width
            val pageY = y - (height - page.height) / 2
            return if (pageY in 0 until page.height) page.getPixel(pageX, pageY) else fill
        }
    }
