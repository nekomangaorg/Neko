package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil3.Extras
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.util.component1
import coil3.util.component2
import kotlin.math.sqrt

private val maxBitmapBytesKey = Extras.Key(default = 0L)

internal const val maxBitmapBytesMemoryCacheKey = "neko#max_bitmap_bytes"

/**
 * Caps the decoded bitmap at [bytes]; 0 means no cap. A [maxBitmapSize] box small enough to keep
 * every page under the canvas byte limit also shrinks tall narrow pages that are far below it, so
 * reader pages use a texture-sized box plus this cap. [MaxBitmapBytesDecoderFactory] applies it for
 * Coil's decoders, [TachiyomiImageDecoder] for its own formats. The cap is part of the memory cache
 * key, because a bitmap that the cap made smaller than the request's box is cached as not sampled
 * (see [isByteLimited]).
 */
fun ImageRequest.Builder.maxBitmapBytes(bytes: Long) = apply {
    extras.set(maxBitmapBytesKey, bytes)
    memoryCacheKeyExtra(maxBitmapBytesMemoryCacheKey, bytes.takeIf { it > 0 }?.toString())
}

val Options.maxBitmapBytes: Long
    get() = getExtra(maxBitmapBytesKey)

/**
 * [maxSize] shrunk so that a [srcWidth] x [srcHeight] image decoded to fit it takes at most
 * [maxBytes] at [bytesPerPixel], or [maxSize] itself when the full size fits. The box is square, so
 * it holds when Coil swaps the sides of a JPEG for its EXIF orientation, and its scale leaves room
 * for decoders to round each side up by a pixel.
 */
internal fun byteLimitedMaxSize(
    srcWidth: Int,
    srcHeight: Int,
    maxSize: Size,
    maxBytes: Long,
    bytesPerPixel: Int = 4,
): Size {
    val maxPixels = maxBytes / bytesPerPixel
    if (maxBytes <= 0 || srcWidth.toLong() * srcHeight <= maxPixels) return maxSize
    val w = srcWidth.toDouble()
    val h = srcHeight.toDouble()
    // Largest scale m with (w * m + 1) * (h * m + 1) <= maxPixels.
    val scale = (-(w + h) + sqrt((w + h) * (w + h) + 4 * w * h * (maxPixels - 1))) / (2 * w * h)
    val side = (maxOf(w, h) * scale).toInt().coerceAtLeast(1)
    return Size(
        minOf(side, maxSize.width.pxOrElse { side }),
        minOf(side, maxSize.height.pxOrElse { side }),
    )
}

/**
 * Whether [limitedMaxSize] from [byteLimitedMaxSize] makes a [srcWidth] x [srcHeight] image decode
 * smaller than [size] and [maxSize] alone would. Decoders then report the image as not sampled,
 * because every request with the same cap and box decodes it to that size. Coil's memory cache
 * compares a sampled bitmap with the request's box, finds it smaller and decodes the page again.
 */
internal fun isByteLimited(
    srcWidth: Int,
    srcHeight: Int,
    size: Size,
    scale: Scale,
    precision: Precision,
    maxSize: Size,
    limitedMaxSize: Size,
): Boolean =
    limitedMaxSize != maxSize &&
        fitMultiplier(srcWidth, srcHeight, size, scale, precision, limitedMaxSize) <
            fitMultiplier(srcWidth, srcHeight, size, scale, precision, maxSize)

@OptIn(ExperimentalCoilApi::class)
private fun fitMultiplier(
    srcWidth: Int,
    srcHeight: Int,
    size: Size,
    scale: Scale,
    precision: Precision,
    maxSize: Size,
): Double {
    val (dstWidth, dstHeight) =
        DecodeUtils.computeDstSize(srcWidth, srcHeight, size, scale, maxSize)
    val multiplier =
        DecodeUtils.computeSizeMultiplier(srcWidth, srcHeight, dstWidth, dstHeight, scale, maxSize)
    return if (precision == Precision.INEXACT) multiplier.coerceAtMost(1.0) else multiplier
}

/**
 * Applies [maxBitmapBytes] to the decoders after it, which only read [maxBitmapSize]: when the
 * image is too large at full size, the request goes on to the next decoder with a smaller
 * [maxBitmapSize]. Must be added before the other decoders.
 */
class MaxBitmapBytesDecoderFactory : Decoder.Factory {

    override fun create(
        result: SourceFetchResult,
        options: Options,
        imageLoader: ImageLoader,
    ): Decoder? {
        val maxBytes = options.maxBitmapBytes
        if (maxBytes <= 0) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        result.source.source().peek().inputStream().use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        // BitmapFactory cannot read JXL, or AVIF before Android 12. TachiyomiImageDecoder applies
        // the cap to those itself.
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        // BitmapFactory reports RGBA_F16 for images it decodes at 8 bytes a pixel, such as
        // 16-bit RGB PNGs.
        val bytesPerPixel = if (bounds.outConfig == Bitmap.Config.RGBA_F16) 8 else 4
        val maxSize =
            byteLimitedMaxSize(
                bounds.outWidth,
                bounds.outHeight,
                options.maxBitmapSize,
                maxBytes,
                bytesPerPixel,
            )
        if (maxSize == options.maxBitmapSize) return null
        val limited =
            options.copy(
                extras = options.extras.newBuilder().set(Extras.Key.maxBitmapSize, maxSize).build()
            )
        val components = imageLoader.components
        val next = components.decoderFactories.indexOf(this) + 1
        val decoder =
            components.newDecoder(result, limited, imageLoader, next)?.first ?: return null
        val byteLimited =
            isByteLimited(
                bounds.outWidth,
                bounds.outHeight,
                options.size,
                options.scale,
                options.precision,
                options.maxBitmapSize,
                maxSize,
            )
        if (!byteLimited) return decoder
        return Decoder {
            decoder.decode()?.let { DecodeResult(image = it.image, isSampled = false) }
        }
    }

    override fun equals(other: Any?) = other is MaxBitmapBytesDecoderFactory

    override fun hashCode() = javaClass.hashCode()
}
