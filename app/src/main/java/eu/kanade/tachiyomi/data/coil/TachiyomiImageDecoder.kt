package eu.kanade.tachiyomi.data.image.coil

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import eu.kanade.tachiyomi.util.system.ImageUtil
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

        val bitmap = decoder.decode()
        decoder.recycle()

        check(bitmap != null) { "Failed to decode image." }

        return DecodeResult(image = bitmap.asImage(), isSampled = false)
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
            return when (type) {
                ImageUtil.ImageType.HEIF,
                ImageUtil.ImageType.AVIF,
                ImageUtil.ImageType.JXL -> true
                else -> false
            }
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }
}
