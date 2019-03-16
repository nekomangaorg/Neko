package eu.kanade.tachiyomi.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.*
import java.io.InputStream
import java.net.URLConnection

object ImageUtil {

    fun isImage(name: String, openStream: (() -> InputStream)? = null): Boolean {
        val contentType = try {
            URLConnection.guessContentTypeFromName(name)
        } catch (e: Exception) {
            null
        } ?: openStream?.let { findImageType(it)?.mime }
        return contentType?.startsWith("image/") ?: false
    }

    fun findImageType(openStream: () -> InputStream): ImageType? {
        return openStream().use { findImageType(it) }
    }

    fun findImageType(stream: InputStream): ImageType? {
        try {
            val bytes = ByteArray(8)

            val length = if (stream.markSupported()) {
                stream.mark(bytes.size)
                stream.read(bytes, 0, bytes.size).also { stream.reset() }
            } else {
                stream.read(bytes, 0, bytes.size)
            }

            if (length == -1)
                return null

            if (bytes.compareWith(charByteArrayOf(0xFF, 0xD8, 0xFF))) {
                return ImageType.JPG
            }
            if (bytes.compareWith(charByteArrayOf(0x89, 0x50, 0x4E, 0x47))) {
                return ImageType.PNG
            }
            if (bytes.compareWith("GIF8".toByteArray())) {
                return ImageType.GIF
            }
            if (bytes.compareWith("RIFF".toByteArray())) {
                return ImageType.WEBP
            }
        } catch(e: Exception) {
        }
        return null
    }

    fun autoSetBackground(image: Bitmap): Drawable {
        if (image.width < 50 || image.height < 50)
            return ColorDrawable(Color.WHITE)
        val topLeftIsDark = isDark(image.getPixel(2,2))
        val topRightIsDark = isDark(image.getPixel(image.width - 2,2))
        val midLeftIsDark = isDark(image.getPixel(2,image.height/2))
        val midRightIsDark = isDark(image.getPixel(image.width - 2,image.height/2))
        val topMidIsDark = isDark(image.getPixel(image.width/2, 2))
        val botLeftIsDark = isDark(image.getPixel(2,image.height - 2))
        val botRightIsDark = isDark(image.getPixel(image.width - 2,image.height - 2))

        var darkBG = (topLeftIsDark && (botLeftIsDark || botRightIsDark || topRightIsDark || midLeftIsDark || topMidIsDark))
                || (topRightIsDark && (botRightIsDark || botLeftIsDark || midRightIsDark || topMidIsDark))
        if (darkBG) {
            if (isWhite(image.getPixel(2,2)).toInt() +
                    isWhite(image.getPixel(image.width - 2,2)).toInt() +
                    isWhite(image.getPixel(2,image.height - 2)).toInt() +
                    isWhite(image.getPixel(image.width - 2,image.height - 2)).toInt() > 2)
                darkBG = false
            var overallWhitePixels = 0
            var overallBlackPixels = 0
            outer@ for (x in intArrayOf(2,image.width-2)) {
                var whitePixelsStreak = 0
                var whitePixels = 0
                var blackPixelsStreak = 0
                var blackPixels = 0
                var blackStreak = false
                var whiteStrak = false
                for (y in (0 until image.height step image.height / 25)) {
                    val pixel = image.getPixel(x, y)
                    if (isWhite(pixel)) {
                        blackPixelsStreak = 0
                        whitePixelsStreak++
                        whitePixels++
                        overallWhitePixels++
                        if (whitePixelsStreak > 14) {
                            whiteStrak = true
                        }
                    }
                    else {
                        whitePixelsStreak = 0
                        if (isDark(pixel)) {
                            blackPixels++
                            overallBlackPixels++
                            blackPixelsStreak++
                            if (blackPixelsStreak > 14) {
                                blackStreak = true
                            }
                        }
                        else {
                            blackPixelsStreak = 0
                        }
                    }
                }
                when {
                    blackPixels > 22 -> return ColorDrawable(Color.BLACK)
                    blackStreak -> darkBG = true
                    whiteStrak || whitePixels > 22 -> darkBG = false
                }
            }
            if (overallWhitePixels > 9 && overallWhitePixels >= overallBlackPixels)
                darkBG = false
        }
        if (darkBG)
        {
            if (isWhite(image.getPixel(2,image.height - 2)) && isWhite(image.getPixel(image.width - 2,image.height - 2)))
                return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(Color.BLACK, Color.BLACK, Color.WHITE, Color.WHITE))
            else
                return ColorDrawable(Color.BLACK)
        }
        return ColorDrawable(Color.WHITE)
    }

    fun Boolean.toInt() = if (this) 1 else 0
    private fun isDark(color: Int): Boolean {
        return Color.red(color) < 33 && Color.blue(color) < 33 && Color.green(color) < 33
    }


    private fun isWhite(color: Int): Boolean {
        return Color.red(color) + Color.blue(color) + Color.green(color) > 740
    }

    private fun ByteArray.compareWith(magic: ByteArray): Boolean {
        for (i in 0 until magic.size) {
            if (this[i] != magic[i]) return false
        }
        return true
    }

    private fun charByteArrayOf(vararg bytes: Int): ByteArray {
        return ByteArray(bytes.size).apply {
            for (i in 0 until bytes.size) {
                set(i, bytes[i].toByte())
            }
        }
    }

    enum class ImageType(val mime: String, val extension: String) {
        JPG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        GIF("image/gif", "gif"),
        WEBP("image/webp", "webp")
    }

}
