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
import kotlinx.coroutines.Dispatchers
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

/**
 * Size limit of [ReaderPageSplitFetcher]'s cache of full page decodes, and the budget for one
 * decode. LruCache.put evicts a value bigger than the limit right after adding it, so a bigger
 * decode would be repeated for every later slice of the page.
 */
internal fun fallbackBitmapCacheMaxBytes(maxMemory: Long = Runtime.getRuntime().maxMemory()): Int =
    (maxMemory / 4).coerceAtMost(256L * 1024 * 1024).toInt()

/**
 * Power-of-two sample size that keeps an ARGB_8888 decode of the whole page within
 * [fallbackBitmapCacheMaxBytes].
 */
internal fun fallbackDecodeSampleSize(
    imageWidth: Int,
    imageHeight: Int,
    maxMemory: Long = Runtime.getRuntime().maxMemory(),
): Int {
    val maxPixels = fallbackBitmapCacheMaxBytes(maxMemory) / 4

    var sampleSize = 1
    while (
        sampledDimension(imageWidth, sampleSize) * sampledDimension(imageHeight, sampleSize) >
            maxPixels
    ) {
        sampleSize *= 2
    }
    return sampleSize
}

/** Upper bound on the byte count of an ARGB_8888 decode of the whole page at [sampleSize]. */
internal fun fallbackDecodeBytes(imageWidth: Int, imageHeight: Int, sampleSize: Int): Long =
    sampledDimension(imageWidth, sampleSize) * sampledDimension(imageHeight, sampleSize) * 4

/**
 * Runs [decode] at [sampleSize]. When it returns null, runs [freeMemory] and tries once more at
 * double the sample size, and returns that result.
 */
internal fun <T : Any> decodeOrRetrySmaller(
    sampleSize: Int,
    freeMemory: () -> Unit,
    decode: (sampleSize: Int) -> T?,
): T? {
    decode(sampleSize)?.let {
        return it
    }
    freeMemory()
    return decode(sampleSize * 2)
}

// Round up. BitmapFactory rounds a sampled GIF to the nearest pixel, so a 2305x65535 GIF
// decodes to 1153x32768 at sample size 2.
private fun sampledDimension(size: Int, sampleSize: Int): Long =
    (size.toLong() + sampleSize - 1) / sampleSize

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

        private val maxBitmapCacheSizeBytes = fallbackBitmapCacheMaxBytes()

        private val rawBytesCache =
            object : LruCache<String, ByteArray>(maxCacheSizeBytes) {
                override fun sizeOf(key: String, value: ByteArray): Int = value.size
            }

        private val fallbackBitmapCache =
            object : LruCache<String, CachedDecodedImage>(maxBitmapCacheSizeBytes) {
                override fun sizeOf(key: String, value: CachedDecodedImage): Int =
                    value.bitmap.byteCount
            }

        private val activeFetches = SharedWork<ByteArray>()
        private val activeDecodes = SharedFullDecodes<CachedDecodedImage>()

        fun clearCache() {
            // clear() leaves running work alone because the next reader can already be waiting on
            // it when the old reader's destroy calls this. Clearing the shared work before the
            // evictions makes that work skip its put, so it cannot refill the caches.
            activeFetches.clear()
            activeDecodes.clear()
            rawBytesCache.evictAll()
            fallbackBitmapCache.evictAll()
        }

        private fun performFullDecode(
            imageBytes: ByteArray,
            pageIndex: Int,
        ): FullDecode<CachedDecodedImage> {
            // BitmapFactory decodes AVIF with the platform AV1 codec, and the AOSP software one
            // (c2.android.av1-dav1d) rejects frames wider or taller than 4096 px, so a tall AVIF
            // page decodes to null. Formats with needsNativeDecoder set use the bundled decoder
            // instead, as they do in TachiyomiImageDecoder.
            val needsNativeDecoder =
                ImageUtil.findImageType(imageBytes.inputStream())?.needsNativeDecoder == true
            return if (needsNativeDecoder) {
                nativeFullDecode(imageBytes, pageIndex)
            } else {
                platformFullDecode(imageBytes, pageIndex)
            }
        }

        private fun nativeFullDecode(
            imageBytes: ByteArray,
            pageIndex: Int,
        ): FullDecode<CachedDecodedImage> {
            val decoder = ImageDecoder.newInstance(imageBytes.inputStream())
            if (decoder == null || decoder.width <= 0 || decoder.height <= 0) {
                decoder?.recycle()
                TimberKt.e {
                    "Cannot fallback decode region: native decoder could not open page $pageIndex"
                }
                return FullDecode.Failed(permanent = true)
            }

            val imageWidth = decoder.width
            val imageHeight = decoder.height
            val sampleSize = fallbackDecodeSampleSize(imageWidth, imageHeight)
            trimCacheForFullDecode(imageWidth, imageHeight, sampleSize)
            // decode() returns null both for bad data and when it runs out of memory. Freeing the
            // cached pages and trying once more at double the sample size lets a page that only
            // ran out of memory load. A second null counts as permanent, so a corrupt page is
            // decoded twice and then left alone until Retry.
            val full =
                try {
                    decodeOrRetrySmaller(
                        sampleSize = sampleSize,
                        freeMemory = {
                            TimberKt.w {
                                "Native fallback decode returned no bitmap for page $pageIndex ($imageWidth x $imageHeight, sample size $sampleSize), trying sample size ${sampleSize * 2}"
                            }
                            fallbackBitmapCache.evictAll()
                            rawBytesCache.evictAll()
                        },
                        decode = { decoder.decode(sampleSize = it) },
                    )
                } finally {
                    decoder.recycle()
                }
            if (full == null) {
                TimberKt.e {
                    "Native fallback decode returned no bitmap for page $pageIndex ($imageWidth x $imageHeight, sample size ${sampleSize * 2})"
                }
                return FullDecode.Failed(permanent = true)
            }
            return FullDecode.Decoded(CachedDecodedImage(full, imageWidth, imageHeight))
        }

        private fun platformFullDecode(
            imageBytes: ByteArray,
            pageIndex: Int,
        ): FullDecode<CachedDecodedImage> {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, boundsOptions)
            val imageWidth = boundsOptions.outWidth
            val imageHeight = boundsOptions.outHeight

            if (imageWidth <= 0 || imageHeight <= 0) {
                TimberKt.e {
                    "Cannot fallback decode region: invalid image dimensions ($imageWidth x $imageHeight)"
                }
                return FullDecode.Failed(permanent = true)
            }

            val sampleSize = fallbackDecodeSampleSize(imageWidth, imageHeight)
            trimCacheForFullDecode(imageWidth, imageHeight, sampleSize)

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
                    FullDecode.Decoded(CachedDecodedImage(full, imageWidth, imageHeight))
                } else {
                    TimberKt.e {
                        "BitmapFactory returned no bitmap for page $pageIndex ($imageWidth x $imageHeight)"
                    }
                    FullDecode.Failed(permanent = true)
                }
            } catch (e: OutOfMemoryError) {
                TimberKt.e(e) { "OutOfMemoryError during fallback decode for page $pageIndex" }
                fallbackBitmapCache.evictAll()
                rawBytesCache.evictAll()
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
                        FullDecode.Decoded(CachedDecodedImage(full, imageWidth, imageHeight))
                    } else {
                        FullDecode.Failed(permanent = true)
                    }
                } catch (e2: Throwable) {
                    TimberKt.e(e2) { "Secondary OOM during fallback decode for page $pageIndex" }
                    FullDecode.Failed(permanent = false)
                }
            } catch (e: Exception) {
                TimberKt.e(e) { "Unexpected error during fallback decode for page $pageIndex" }
                FullDecode.Failed(permanent = true)
            }
        }

        // LruCache evicts on put, after the new bitmap is allocated. Evicting first keeps cached
        // pages plus the new bitmap within the budget when pages decode one at a time.
        private fun trimCacheForFullDecode(imageWidth: Int, imageHeight: Int, sampleSize: Int) {
            val expectedBytes = fallbackDecodeBytes(imageWidth, imageHeight, sampleSize)
            fallbackBitmapCache.trimToSize(
                (fallbackBitmapCache.maxSize() - expectedBytes).coerceAtLeast(0).toInt()
            )
        }
    }

    override suspend fun fetch(): FetchResult = coroutineScope {
        val chapterKey = split.page.chapter.chapter.id ?: split.page.chapter.chapter.url.hashCode()
        val cacheKey = "${chapterKey}_${split.page.index}"

        // A page with a cached full decode serves its slices from that decode, without reading
        // the file again.
        val cached = fallbackBitmapCache.get(cacheKey)?.takeUnless { it.bitmap.isRecycled }
        if (cached != null) {
            val slice =
                withContext(Dispatchers.IO) {
                    cropFallbackSlice(cached, split.topOffset, split.splitHeight)
                }
            if (slice != null) {
                return@coroutineScope ImageFetchResult(
                    image = slice.asImage(),
                    isSampled = false,
                    dataSource = DataSource.MEMORY,
                )
            }
        }

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

        if (isRetry()) {
            // Read the file again, so a retry after the page was downloaded again decodes the
            // new file.
            rawBytesCache.remove(cacheKey)
        }
        val imageBytes =
            rawBytesCache.get(cacheKey)
                ?: activeFetches.await(cacheKey) {
                    actualStream()
                        .use { it.readBytes() }
                        .also {
                            // LruCache.put would evict a file bigger than the cache right away,
                            // with every other entry.
                            if (it.size <= rawBytesCache.maxSize()) {
                                unlessCleared { rawBytesCache.put(cacheKey, it) }
                            }
                        }
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
    ): Bitmap? {
        val cached = fallbackBitmapCache.get(cacheKey)?.takeUnless { it.bitmap.isRecycled }
        // The decode can outlive this fetcher, so it reads no fetcher fields. A reference to the
        // fetcher would keep its Options and their context alive until the decode ends.
        val pageIndex = split.page.index
        val decoded =
            cached
                ?: activeDecodes.await(cacheKey, retry = isRetry()) {
                    performFullDecode(imageBytes, pageIndex).also {
                        if (it is FullDecode.Decoded) {
                            unlessCleared { fallbackBitmapCache.put(cacheKey, it.image) }
                        }
                    }
                }
                ?: return null

        return cropFallbackSlice(decoded, top, height)
    }

    // The reader's Retry button loads the page with memory cache reads turned off.
    private fun isRetry(): Boolean = !options.memoryCachePolicy.readEnabled

    private fun cropFallbackSlice(decoded: CachedDecodedImage, top: Int, height: Int): Bitmap? =
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
