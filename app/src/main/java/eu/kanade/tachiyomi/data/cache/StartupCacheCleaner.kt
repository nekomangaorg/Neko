package eu.kanade.tachiyomi.data.cache

import java.io.File
import org.nekomanga.constants.Constants.TMP_FILE_SUFFIX
import org.nekomanga.logging.TimberKt

/**
 * Cleans up cache files that grow without a limit. The reader copies a downloaded CBZ chapter into
 * the cache folder and deletes the copy when it releases the chapter, so a copy stays behind when
 * the app is killed with a chapter open or when the reader closes before the chapter has loaded.
 */
class StartupCacheCleaner(private val cacheDir: File, private val coverCache: CoverCache) {

    /**
     * Deletes chapter copies last written before [startedAt] and trims the online covers. A chapter
     * opened after the app started keeps its copy.
     */
    suspend fun clean(startedAt: Long) {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (
                    file.isFile &&
                        file.name.endsWith(TMP_FILE_SUFFIX) &&
                        file.lastModified() < startedAt
                ) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            TimberKt.e(e) { "Failed to delete leftover chapter copies" }
        }
        coverCache.deleteCachedCovers()
    }
}
