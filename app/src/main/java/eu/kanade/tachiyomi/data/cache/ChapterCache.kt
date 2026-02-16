package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.saveTo
import eu.kanade.tachiyomi.util.system.toast
import java.io.File
import java.io.IOException
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Response
import okio.buffer
import okio.sink
import org.nekomanga.R
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.injectLazy

/**
 * Class used to create chapter cache For each image in a chapter a file is created For each chapter
 * a Json list is created and converted to a file. The files are in format *md5key*.0
 *
 * @param context the application context.
 * @constructor creates an instance of the chapter cache.
 */
class ChapterCache(private val context: Context) {

    companion object {
        /** Name of cache directory. */
        const val PARAMETER_CACHE_DIRECTORY = "chapter_disk_cache"

        /** Application cache version. */
        const val PARAMETER_APP_VERSION = 1

        /** The number of values per cache entry. Must be positive. */
        const val PARAMETER_VALUE_COUNT = 1

        /** The maximum number of bytes this cache should use to store. */
        const val PARAMETER_CACHE_SIZE = 50L * 1024 * 1024
    }

    private val json: Json by injectLazy()

    private val readerPreferences: ReaderPreferences by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    /** Cache class used for cache management. */
    private var diskCache = setupDiskCache(readerPreferences.preloadPageAmount().get())

    /** Returns directory of cache. */
    val cacheDir: File
        get() = diskCache.directory

    /** Returns real size of directory. */
    suspend fun getReadableSize(): String =
        withContext(Dispatchers.IO) {
            val size = DiskUtil.getDirectorySize(cacheDir)
            Formatter.formatFileSize(context, size)
        }

    init {
        readerPreferences
            .preloadPageAmount()
            .changes()
            .drop(1)
            .onEach {
                // Save old cache for destruction later
                val oldCache = diskCache
                diskCache = setupDiskCache(it)
                oldCache.close()
            }
            .launchIn(scope)
    }

    private fun setupDiskCache(cacheSize: Int): DiskLruCache {
        return DiskLruCache.open(
            File(context.cacheDir, PARAMETER_CACHE_DIRECTORY),
            PARAMETER_APP_VERSION,
            PARAMETER_VALUE_COUNT,
            // 4 pages = 115MB, 6 = ~150MB, 10 = ~200MB, 20 = ~300MB
            (PARAMETER_CACHE_SIZE * cacheSize.toFloat().pow(0.6f)).roundToLong(),
        )
    }

    /**
     * Remove file from cache.
     *
     * @param file name of file "md5.0".
     * @return status of deletion for the file.
     */
    fun removeFileFromCache(file: String): Boolean {
        TimberKt.d { "remove file from cache" }
        // Make sure we don't delete the journal file (keeps track of cache).
        if (file == "journal" || file.startsWith("journal.")) {
            return false
        }

        return try {
            // Remove the extension from the file to get the key of the cache
            val key = file.substringBeforeLast(".")
            // Remove file from cache.
            diskCache.remove(key)
        } catch (e: Exception) {
            TimberKt.e(e) { "Error removing from from cache" }
            false
        } finally {
            TimberKt.d { "finished removing file from cache" }
        }
    }

    /**
     * Add page list to disk cache.
     *
     * @param chapter the chapter.
     * @param pages list of pages.
     */
    fun putPageListToCache(chapter: Chapter, pages: List<Page>) {
        // Convert list of pages to json string.
        val cachedValue = json.encodeToString(pages)
        TimberKt.d { "put page list to cache $cachedValue" }

        // Initialize the editor (edits the values for an entry).
        var editor: DiskLruCache.Editor? = null

        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(getKey(chapter))
            editor = diskCache.edit(key) ?: return

            // Write chapter urls to cache.
            editor.newOutputStream(0).sink().buffer().use {
                it.write(cachedValue.toByteArray())
                it.flush()
            }

            diskCache.flush()
            editor.commit()
            editor.abortUnlessCommitted()
        } catch (e: Exception) {
            TimberKt.e(e) { "Error putting page list to cache" }
            // Ignore.
        } finally {
            TimberKt.d { "finishing putting pagelist to cache" }
            editor?.abortUnlessCommitted()
        }
    }

    /**
     * Returns true if image is in cache.
     *
     * @param imageUrl url of image.
     * @return true if in cache otherwise false.
     */
    fun isImageInCache(imageUrl: String): Boolean {
        return try {
            TimberKt.d { "is image in cache $imageUrl" }
            diskCache.get(DiskUtil.hashKeyForDisk(imageUrl)).use { it != null }
        } catch (e: IOException) {
            TimberKt.e(e) { "Error checking if image in cache" }
            false
        }
    }

    /**
     * Get image file from url.
     *
     * @param imageUrl url of image.
     * @return path of image.
     */
    fun getImageFile(imageUrl: String): File {
        // Get file from md5 key.
        TimberKt.d { "get image file $imageUrl" }
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
    }

    /** Clear chapter cache */
    suspend fun deleteCache() {
        var deletedSize = 0L
        val files = cacheDir.listFiles()?.iterator() ?: return
        while (files.hasNext()) {
            val file = files.next()
            deletedSize += file.length()
            file.delete()
        }
        withContext(Dispatchers.Main) {
            context.toast(
                context.getString(R.string.deleted_, Formatter.formatFileSize(context, deletedSize))
            )
        }
    }

    /**
     * Add image to cache.
     *
     * @param imageUrl url of image.
     * @param response http response from page.
     * @throws IOException image error.
     */
    fun putImageToCache(imageUrl: String, response: Response) {
        // Initialize editor (edits the values for an entry).
        var editor: DiskLruCache.Editor? = null
        TimberKt.d { "put image to cache $imageUrl" }
        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            editor = diskCache.edit(key) ?: throw IOException("Unable to edit key")

            // Get OutputStream and write image with Okio.
            response.body.source().saveTo(editor.newOutputStream(0))

            diskCache.flush()
            editor.commit()
        } catch (e: Exception) {
            TimberKt.e(e) { "Error puting image to Cache" }
        } finally {
            response.body.close()
            editor?.abortUnlessCommitted()
            TimberKt.d { "finished image to cache $imageUrl" }
        }
    }

    private fun getKey(chapter: Chapter): String {
        TimberKt.d { "get key" }
        return "${chapter.manga_id}${chapter.url}"
    }
}
