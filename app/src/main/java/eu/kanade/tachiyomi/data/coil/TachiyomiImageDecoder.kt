package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.util.component1
import coil3.util.component2
import eu.kanade.tachiyomi.util.system.ImageUtil
import kotlin.math.roundToInt
import okio.BufferedSource
import tachiyomi.decoder.ImageDecoder

/**
 * A [Decoder] that uses built-in [ImageDecoder] to decode images that is not supported by the
 * system.
 */
class TachiyomiImageDecoder(private val resources: ImageSource, private val options: Options) :
    Decoder {

    override suspend fun decode(): DecodeResult {
        val decoder = resources.sourceOrNull()?.use { ImageDecoder.newInstance(it.inputStream()) }

        check(decoder != null && decoder.width > 0 && decoder.height > 0) {
            "Failed to initialize decoder."
        }

        val target =
            nativeDecodeTarget(
                srcWidth = decoder.width,
                srcHeight = decoder.height,
                size = options.size,
                scale = options.scale,
                precision = options.precision,
                maxSize = options.maxBitmapSize,
            )
        val sampled =
            try {
                decoder.decode(sampleSize = target.sampleSize)
            } finally {
                decoder.recycle()
            }

        check(sampled != null) { "Failed to decode image." }

        val (width, height) = target.outputSize(sampled.width, sampled.height)
        val bitmap =
            if (width == sampled.width && height == sampled.height) {
                sampled
            } else {
                Bitmap.createScaledBitmap(sampled, width, height, true).also { sampled.recycle() }
            }
        val isSampled = target.sampleSize > 1 || bitmap !== sampled

        if (
            options.bitmapConfig == Bitmap.Config.HARDWARE && ImageUtil.canUseHardwareBitmap(bitmap)
        ) {
            bitmap.copy(Bitmap.Config.HARDWARE, false)?.let {
                bitmap.recycle()
                return DecodeResult(image = it.asImage(), isSampled = isSampled)
            }
        }

        return DecodeResult(image = bitmap.asImage(), isSampled = isSampled)
    }

    class Factory : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return TachiyomiImageDecoder(result.source, options)
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            val type = source.peek().inputStream().use { ImageUtil.findImageType(it) }
            return type?.needsNativeDecoder == true
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }
}

/**
 * Sample size for the native decode and the multiplier for the sampled bitmap, worked out the way
 * Coil's BitmapFactoryDecoder does it for JPEG and PNG. The native decoder knows nothing of the
 * request's size or maxBitmapSize, so without this an AVIF, HEIF or JXL page comes back at full
 * resolution, and a tall one is then too large to draw.
 */
@OptIn(ExperimentalCoilApi::class)
internal fun nativeDecodeTarget(
    srcWidth: Int,
    srcHeight: Int,
    size: Size,
    scale: Scale,
    precision: Precision,
    maxSize: Size,
): NativeDecodeTarget {
    val (dstWidth, dstHeight) =
        DecodeUtils.computeDstSize(srcWidth, srcHeight, size, scale, maxSize)
    val sampleSize =
        DecodeUtils.calculateInSampleSize(srcWidth, srcHeight, dstWidth, dstHeight, scale)
    var multiplier =
        DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth / sampleSize.toDouble(),
            srcHeight = srcHeight / sampleSize.toDouble(),
            dstWidth = dstWidth.toDouble(),
            dstHeight = dstHeight.toDouble(),
            scale = scale,
            maxSize = maxSize,
        )
    // Like BitmapFactoryDecoder, only an exact request may upscale.
    if (precision == Precision.INEXACT) {
        multiplier = multiplier.coerceAtMost(1.0)
    }
    return NativeDecodeTarget(sampleSize, multiplier)
}

internal data class NativeDecodeTarget(val sampleSize: Int, val multiplier: Double) {

    /** Final size for a bitmap the native decoder returned at [sampleSize]. */
    fun outputSize(sampledWidth: Int, sampledHeight: Int): Pair<Int, Int> =
        if (multiplier == 1.0) {
            sampledWidth to sampledHeight
        } else {
            scaled(sampledWidth) to scaled(sampledHeight)
        }

    private fun scaled(dimension: Int) = (dimension * multiplier).roundToInt().coerceAtLeast(1)
}
