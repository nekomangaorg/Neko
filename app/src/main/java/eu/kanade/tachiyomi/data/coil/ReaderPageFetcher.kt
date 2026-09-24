package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import android.util.LruCache
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.util.system.ImageUtil
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import org.nekomanga.logging.TimberKt
import tachiyomi.decoder.ImageDecoder

class ReaderPageFetcher(private val page: ReaderPage, private val options: Options) : Fetcher {

    override suspend fun fetch(): FetchResult = coroutineScope {
        var streamFn = page.stream
        if (streamFn == null) {
            val loader = page.chapter.pageLoader
            val loadJob =
                if (loader != null && page.status == Page.State.QUEUE) {
                    launch(Dispatchers.IO) { loader.loadPage(page) }
                } else {
                    null
                }
            try {
                page.statusFlow.first {
                    (it == Page.State.READY && page.stream != null) || it == Page.State.ERROR
                }
            } finally {
                loadJob?.cancel()
            }
            streamFn = page.stream
        }

        val actualStream = streamFn ?: error("Page stream not available for page ${page.index}")
        val source = actualStream().source().buffer()
        SourceFetchResult(
            source = ImageSource(source = source, fileSystem = options.fileSystem),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<ReaderPage> {
        override fun create(
            data: ReaderPage,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return ReaderPageFetcher(data, options)
        }
    }
}

class ReaderPageSplitFetcher(private val split: ReaderPageSplit, private val options: Options) :
    Fetcher {

    companion object {
        private class CachedDecodedImage(
            val bitmap: Bitmap,
            val originalWidth: Int,
            val originalHeight: Int,
        )

        private val maxCacheSizeBytes =
            (Runtime.getRuntime().maxMemory() / 16)
                .coerceIn(16L * 1024 * 1024, 64L * 1024 * 1024)
                .toInt()

        private val maxBitmapCacheSizeBytes =
            (Runtime.getRuntime().maxMemory() / 8)
                .coerceIn(32L * 1024 * 1024, 128L * 1024 * 1024)
                .toInt()

        private val rawBytesCache =
            object : LruCache<String, ByteArray>(maxCacheSizeBytes) {
                override fun sizeOf(key: String, value: ByteArray): Int = value.size
            }

        private val fallbackBitmapCache =
            object : LruCache<String, CachedDecodedImage>(maxBitmapCacheSizeBytes) {
                override fun sizeOf(key: String, value: CachedDecodedImage): Int =
                    value.bitmap.byteCount
            }

        private val activeFetches = ConcurrentHashMap<String, Deferred<ByteArray>>()
        private val activeDecodes = ConcurrentHashMap<String, Deferred<CachedDecodedImage?>>()

        fun clearCache() {
            rawBytesCache.evictAll()
            fallbackBitmapCache.evictAll()
            activeFetches.clear()
            activeDecodes.clear()
        }
    }

    override suspend fun fetch(): FetchResult = coroutineScope {
        var streamFn = split.page.stream
        if (streamFn == null) {
            val loader = split.page.chapter.pageLoader
            val loadJob =
                if (loader != null && split.page.status == Page.State.QUEUE) {
                    launch(Dispatchers.IO) { loader.loadPage(split.page) }
                } else {
                    null
                }
            try {
                split.page.statusFlow.first {
                    (it == Page.State.READY && split.page.stream != null) || it == Page.State.ERROR
                }
            } finally {
                loadJob?.cancel()
            }
            streamFn = split.page.stream
        }
        val actualStream =
            streamFn ?: error("Page stream not available for page ${split.page.index}")

        val chapterKey = split.page.chapter.chapter.id ?: split.page.chapter.chapter.url.hashCode()
        val cacheKey = "${chapterKey}_${split.page.index}"
        val imageBytes =
            rawBytesCache.get(cacheKey)
                ?: run {
                    val deferred =
                        activeFetches.compute(cacheKey) { _, existing ->
                            existing?.takeIf { it.isActive }
                                ?: async(Dispatchers.IO) {
                                    try {
                                        actualStream()
                                            .use { it.readBytes() }
                                            .also { rawBytesCache.put(cacheKey, it) }
                                    } finally {
                                        activeFetches.remove(cacheKey)
                                    }
                                }
                        }!!
                    deferred.await()
                }

        val bitmap =
            withContext(Dispatchers.IO) {
                decodeRegion(imageBytes, split.topOffset, split.splitHeight, cacheKey)
            }

        if (bitmap != null) {
            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = false,
                dataSource = DataSource.MEMORY,
            )
        } else {
            error(
                "Failed to decode webtoon slice for page ${split.page.index} at offset ${split.topOffset}"
            )
        }
    }

    private suspend fun decodeRegion(
        imageBytes: ByteArray,
        top: Int,
        height: Int,
        cacheKey: String,
    ): Bitmap? {
        val decoder =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size, false)
                }
            } catch (e: Exception) {
                TimberKt.d(e) {
                    "BitmapRegionDecoder not supported for page ${split.page.index}, slice offset $top, falling back to full decode"
                }
                null
            }

        if (decoder != null) {
            try {
                val bottom = minOf(decoder.height, top + height)
                if (top < decoder.height && bottom > top) {
                    val region = Rect(0, top, decoder.width, bottom)
                    val sliceBitmap =
                        decoder.decodeRegion(
                            region,
                            BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            },
                        )
                    if (sliceBitmap != null) {
                        return sliceBitmap
                    }
                }
            } catch (e: Exception) {
                TimberKt.e(e) {
                    "BitmapRegionDecoder failed during decodeRegion for page ${split.page.index}, slice offset $top"
                }
            } finally {
                decoder.recycle()
            }
        }

        return fallbackDecodeRegion(imageBytes, top, height, cacheKey)
    }

    private suspend fun fallbackDecodeRegion(
        imageBytes: ByteArray,
        top: Int,
        height: Int,
        cacheKey: String,
    ): Bitmap? = coroutineScope {
        val cached = fallbackBitmapCache.get(cacheKey)?.takeUnless { it.bitmap.isRecycled }
        val decoded =
            cached
                ?: run {
                    val deferred =
                        activeDecodes.compute(cacheKey) { _, existing ->
                            existing?.takeIf { it.isActive }
                                ?: async(Dispatchers.IO) {
                                    try {
                                        performFullDecode(imageBytes, cacheKey)
                                    } finally {
                                        activeDecodes.remove(cacheKey)
                                    }
                                }
                        }!!
                    deferred.await()
                }
                ?: return@coroutineScope null

        try {
            val scale = decoded.bitmap.height.toFloat() / decoded.originalHeight.toFloat()
            val scaledTop = (top * scale).toInt().coerceIn(0, decoded.bitmap.height - 1)
            val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
            val cropHeight = minOf(scaledHeight, decoded.bitmap.height - scaledTop)
            if (cropHeight <= 0) {
                null
            } else {
                Bitmap.createBitmap(decoded.bitmap, 0, scaledTop, decoded.bitmap.width, cropHeight)
            }
        } catch (e: Exception) {
            TimberKt.e(e) {
                "Error cropping fallback slice for page ${split.page.index}, offset $top"
            }
            null
        }
    }

    private fun performFullDecode(imageBytes: ByteArray, cacheKey: String): CachedDecodedImage? {
        // BitmapFactory decodes AVIF with the platform AV1 codec, and the AOSP software one
        // (c2.android.av1-dav1d) rejects frames wider or taller than 4096 px, so a tall AVIF page
        // decodes to null. Formats with needsNativeDecoder set use the bundled decoder instead,
        // as they do in TachiyomiImageDecoder.
        val decoded =
            if (ImageUtil.findImageType(imageBytes.inputStream())?.needsNativeDecoder == true) {
                nativeFullDecode(imageBytes)
            } else {
                platformFullDecode(imageBytes)
            }
        return decoded?.also { fallbackBitmapCache.put(cacheKey, it) }
    }

    private fun nativeFullDecode(imageBytes: ByteArray): CachedDecodedImage? {
        val decoder = ImageDecoder.newInstance(imageBytes.inputStream())
        if (decoder == null || decoder.width <= 0 || decoder.height <= 0) {
            decoder?.recycle()
            TimberKt.e {
                "Cannot fallback decode region: native decoder could not open page ${split.page.index}"
            }
            return null
        }

        val imageWidth = decoder.width
        val imageHeight = decoder.height
        val sampleSize = fullDecodeSampleSize(imageWidth, imageHeight)
        // decode() catches its own out-of-memory errors and returns null
        val full =
            try {
                decoder.decode(sampleSize = sampleSize)
            } finally {
                decoder.recycle()
            }
        if (full == null) {
            TimberKt.e {
                "Native fallback decode returned no bitmap for page ${split.page.index} ($imageWidth x $imageHeight, sample size $sampleSize)"
            }
            return null
        }
        return CachedDecodedImage(full, imageWidth, imageHeight)
    }

    private fun platformFullDecode(imageBytes: ByteArray): CachedDecodedImage? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, boundsOptions)
        val imageWidth = boundsOptions.outWidth
        val imageHeight = boundsOptions.outHeight

        if (imageWidth <= 0 || imageHeight <= 0) {
            TimberKt.e {
                "Cannot fallback decode region: invalid image dimensions ($imageWidth x $imageHeight)"
            }
            return null
        }

        val sampleSize = fullDecodeSampleSize(imageWidth, imageHeight)

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

        return try {
            val full =
                BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.size,
                    decodeOptions,
                )
            if (full != null) {
                CachedDecodedImage(full, imageWidth, imageHeight)
            } else {
                TimberKt.e {
                    "BitmapFactory returned no bitmap for page ${split.page.index} ($imageWidth x $imageHeight)"
                }
                null
            }
        } catch (e: OutOfMemoryError) {
            TimberKt.e(e) { "OutOfMemoryError during fallback decode for page ${split.page.index}" }
            try {
                val fallbackOptions =
                    BitmapFactory.Options().apply {
                        inSampleSize = (sampleSize * 2).coerceAtLeast(2)
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                val full =
                    BitmapFactory.decodeByteArray(
                        imageBytes,
                        0,
                        imageBytes.size,
                        fallbackOptions,
                    )
                if (full != null) {
                    CachedDecodedImage(full, imageWidth, imageHeight)
                } else {
                    null
                }
            } catch (e2: Throwable) {
                TimberKt.e(e2) {
                    "Secondary OOM during fallback decode for page ${split.page.index}"
                }
                null
            }
        } catch (e: Exception) {
            TimberKt.e(e) { "Unexpected error during fallback decode for page ${split.page.index}" }
            null
        }
    }

    private fun fullDecodeSampleSize(imageWidth: Int, imageHeight: Int): Int {
        val maxSafeMemoryBytes =
            (Runtime.getRuntime().maxMemory() / 4).coerceIn(
                64L * 1024 * 1024,
                256L * 1024 * 1024,
            )

        var sampleSize = 1
        var testWidth = imageWidth
        var testHeight = imageHeight
        while (testWidth.toLong() * testHeight.toLong() * 4L > maxSafeMemoryBytes) {
            sampleSize *= 2
            testWidth /= 2
            testHeight /= 2
        }
        return sampleSize
    }

    class Factory : Fetcher.Factory<ReaderPageSplit> {
        override fun create(
            data: ReaderPageSplit,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return ReaderPageSplitFetcher(data, options)
        }
    }
}

class ReaderPageKeyer : Keyer<ReaderPage> {
    override fun key(data: ReaderPage, options: Options): String {
        val chapterKey = data.chapter.chapter.id ?: data.chapter.chapter.url.hashCode()
        return "reader_page_${chapterKey}_${data.index}"
    }
}

class ReaderPageSplitKeyer : Keyer<ReaderPageSplit> {
    override fun key(data: ReaderPageSplit, options: Options): String {
        val chapterKey = data.page.chapter.chapter.id ?: data.page.chapter.chapter.url.hashCode()
        return "reader_split_${chapterKey}_${data.page.index}_${data.topOffset}"
    }
}
