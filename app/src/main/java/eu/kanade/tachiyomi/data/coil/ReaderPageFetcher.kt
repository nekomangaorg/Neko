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
import eu.kanade.tachiyomi.util.system.GLUtil
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import org.nekomanga.logging.TimberKt

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
        private val rawBytesCache =
            object : LruCache<String, ByteArray>(16 * 1024 * 1024) {
                override fun sizeOf(key: String, value: ByteArray): Int = value.size
            }
    }

    override suspend fun fetch(): FetchResult = coroutineScope {
        val bytes = split.cachedBytes
        if (bytes != null) {
            val source = ByteArrayInputStream(bytes).source().buffer()
            return@coroutineScope SourceFetchResult(
                source = ImageSource(source = source, fileSystem = options.fileSystem),
                mimeType = null,
                dataSource = DataSource.MEMORY,
            )
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

        val cacheKey = "${split.page.chapter.chapter.id}_${split.page.index}"
        val imageBytes =
            rawBytesCache.get(cacheKey)
                ?: withContext(Dispatchers.IO) {
                    synchronized(split.page) {
                        rawBytesCache.get(cacheKey)
                            ?: actualStream()
                                .use { it.readBytes() }
                                .also { rawBytesCache.put(cacheKey, it) }
                    }
                }

        val bitmap =
            withContext(Dispatchers.IO) {
                decodeRegion(imageBytes, split.topOffset, split.splitHeight)
            }
        if (bitmap != null) {
            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = false,
                dataSource = DataSource.MEMORY,
            )
        } else {
            SourceFetchResult(
                source =
                    ImageSource(
                        source = ByteArrayInputStream(imageBytes).source().buffer(),
                        fileSystem = options.fileSystem,
                    ),
                mimeType = null,
                dataSource = DataSource.MEMORY,
            )
        }
    }

    private fun decodeRegion(
        imageBytes: ByteArray,
        top: Int,
        height: Int,
    ): Bitmap? {
        val decoder =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size, false)
                }
            } catch (e: IOException) {
                TimberKt.e(e) {
                    "Failed to create BitmapRegionDecoder for page ${split.page.index}, slice offset $top"
                }
                return fallbackDecodeRegion(imageBytes, top, height)
            } catch (e: IllegalArgumentException) {
                TimberKt.e(e) {
                    "Illegal arguments creating BitmapRegionDecoder for page ${split.page.index}, slice offset $top"
                }
                return fallbackDecodeRegion(imageBytes, top, height)
            }

        return try {
            val bottom = minOf(decoder.height, top + height)
            if (top >= decoder.height || bottom <= top) {
                null
            } else {
                val region = Rect(0, top, decoder.width, bottom)
                decoder.decodeRegion(
                    region,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 },
                )
            }
        } finally {
            decoder.recycle()
        }
    }

    private fun fallbackDecodeRegion(
        imageBytes: ByteArray,
        top: Int,
        height: Int,
    ): Bitmap? {
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

        // Avoid allocating massive uncompressed bitmaps in memory to prevent OutOfMemoryError
        val estimatedMemoryBytes = imageWidth.toLong() * imageHeight.toLong() * 4L
        val maxSafeMemoryBytes = 30L * 1024L * 1024L // 30 MB
        if (estimatedMemoryBytes > maxSafeMemoryBytes || imageHeight > GLUtil.maxTextureSize) {
            TimberKt.w {
                "Skipping full bitmap fallback decode to avoid OutOfMemoryError: dimensions $imageWidth x $imageHeight ($estimatedMemoryBytes bytes), limit $maxSafeMemoryBytes bytes"
            }
            return null
        }

        return try {
            val fullBitmap =
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
            val cropHeight = minOf(height, fullBitmap.height - top)
            if (cropHeight <= 0 || top >= fullBitmap.height) {
                fullBitmap
            } else {
                val cropped = Bitmap.createBitmap(fullBitmap, 0, top, fullBitmap.width, cropHeight)
                if (cropped != fullBitmap) {
                    fullBitmap.recycle()
                }
                cropped
            }
        } catch (e: OutOfMemoryError) {
            TimberKt.e(e) {
                "OutOfMemoryError during fallback decode for page ${split.page.index}, slice offset $top"
            }
            null
        } catch (e: Exception) {
            TimberKt.e(e) {
                "Unexpected error during fallback decode for page ${split.page.index}, slice offset $top"
            }
            null
        }
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
        return "reader_page_${data.chapter.chapter.id}_${data.index}"
    }
}

class ReaderPageSplitKeyer : Keyer<ReaderPageSplit> {
    override fun key(data: ReaderPageSplit, options: Options): String {
        return "reader_split_${data.page.chapter.chapter.id}_${data.page.index}_${data.topOffset}"
    }
}
