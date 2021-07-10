package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.MergeSource
import eu.kanade.tachiyomi.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slowdowns the app. The cache is invalidated by the time
 * defined in [renewInterval] as we don't have any control over the filesystem and the user can
 * delete the folders at any time without the app noticing.
 *
 * @param context the application context.
 * @param provider the downloads directories provider.
 * @param sourceManager the source manager.
 * @param preferences the preferences of the app.
 */
class DownloadCache(
    private val context: Context,
    private val provider: DownloadProvider,
    private val sourceManager: SourceManager,
    private val preferences: PreferencesHelper = Injekt.get(),
) {

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = TimeUnit.HOURS.toMillis(1)

    /**
     * The last time the cache was refreshed.
     */
    private var lastRenew = 0L

    private var mangaFiles: MutableMap<Long, Pair<MutableSet<String>, MutableSet<String>>> =
        mutableMapOf()

    init {
        preferences.downloadsDirectory().asObservable().skip(1).subscribe {
            lastRenew = 0L // invalidate cache
        }
    }

    /**
     * Returns the downloads directory from the user's preferences.
     */
    private fun getDirectoryFromPreference(): UniFile {
        val dir = preferences.downloadsDirectory().getOrDefault()
        return UniFile.fromUri(context, dir.toUri())
    }

    /**
     * Returns true if the chapter is downloaded.
     *
     * @param chapter the chapter to check.
     * @param manga the manga of the chapter.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isChapterDownloaded(chapter: Chapter, manga: Manga, skipCache: Boolean): Boolean {
        if (skipCache) {
            val source = sourceManager.getMangadex()
            return provider.findChapterDir(chapter, manga, source) != null
        }

        checkRenew()

        val fileNames = mangaFiles[manga.id]?.first?.toHashSet() ?: return false
        val mangadexIds = mangaFiles[manga.id]?.second?.toHashSet() ?: return false

        if (!chapter.isMergedChapter() && chapter.mangadex_chapter_id.isNotEmpty() && chapter.mangadex_chapter_id in mangadexIds) {
            return true
        }
        if (!chapter.isMergedChapter() && chapter.old_mangadex_id != null && chapter.old_mangadex_id in mangadexIds) {
            return true
        }
        val validChapterDirNames = provider.getValidChapterDirNames(chapter)
        return validChapterDirNames.any {
            it in fileNames
        }
    }

    /**
     * Returns the amount of downloaded chapters for a manga.
     *
     * @param manga the manga to check.
     */
    fun getDownloadCount(manga: Manga, forceCheckFolder: Boolean = false): Int {
        checkRenew()

        if (forceCheckFolder) {
            val mangaDir = provider.findMangaDir(manga, sourceManager.getMangadex())

            if (mangaDir != null) {
                val listFiles =
                    mangaDir.listFiles { dir, filename -> !filename.endsWith(Downloader.TMP_DIR_SUFFIX) }
                if (!listFiles.isNullOrEmpty()) {
                    return listFiles.size
                }
            }
            return 0
        } else {
            mangaFiles[manga.id] ?: return 0
            val files =
                mangaFiles[manga.id]!!.first.filter { !it.endsWith(Downloader.TMP_DIR_SUFFIX) }
            val ids = mangaFiles[manga.id]!!.second
            var count = files.size
            files.forEach {
                if (!it.contains(MergeSource.name)) {
                    val mangadexId = it.substringAfterLast("- ")
                    if (mangadexId.isNotBlank() && mangadexId.isDigitsOnly() && !ids.contains(
                            mangadexId
                        )
                    ) {
                        count++
                    }
                }
            }
            return count
        }
    }

    /**
     * Checks if the cache needs a renewal and performs it if needed.
     */
    @Synchronized
    private fun checkRenew() {
        if (lastRenew + renewInterval < System.currentTimeMillis()) {
            renew()
            lastRenew = System.currentTimeMillis()
        }
    }

    fun forceRenewCache() {
        renew()
        lastRenew = System.currentTimeMillis()
    }

    /**
     * Renews the downloads cache.
     */
    private fun renew() {
        val onlineSources = listOf(sourceManager.getMangadex())

        val sourceDirs = getDirectoryFromPreference().listFiles().orEmpty()
            .associate { it.name to SourceDirectory(it) }.mapNotNullKeys { entry ->
                onlineSources.find { provider.getSourceDirName(it) == entry.key }?.id
            }

        val db: DatabaseHelper by injectLazy()
        val mangaGroupedBySource = db.getMangaList().executeAsBlocking().groupBy { it.source }

        sourceDirs.forEach { sourceValue ->
            val sourceMangaRaw =
                mangaGroupedBySource[sourceValue.key]?.toMutableSet() ?: return@forEach
            val sourceMangaPair = sourceMangaRaw.partition { it.favorite }

            val sourceDir = sourceValue.value

            val mangaDirs = sourceDir.dir.listFiles().orEmpty().mapNotNull { mangaDir ->
                val name = mangaDir.name ?: return@mapNotNull null
                val chapterDirs =
                    mangaDir.listFiles().orEmpty().mapNotNull { chapterFile -> chapterFile.name }
                        .toHashSet()
                name to MangaDirectory(mangaDir, chapterDirs)
            }.toMap()

            val trueMangaDirs = mangaDirs.mapNotNull { mangaDir ->
                val manga =
                    findManga(sourceMangaPair.first, mangaDir.key, sourceValue.key) ?: findManga(
                        sourceMangaPair.second,
                        mangaDir.key,
                        sourceValue.key
                    )
                val id = manga?.id ?: return@mapNotNull null

                val mangadexIds = mangaDir.value.files.map { it.substringAfterLast("- ", "") }
                    .filter { it.isNotEmpty() }.toMutableSet()

                id to Pair(mangaDir.value.files, mangadexIds)
            }.toMap()

            mangaFiles.putAll(trueMangaDirs)
        }
    }

    /**
     * Searches a manga list and matches the given mangakey and source key
     */
    private fun findManga(mangaList: List<Manga>, mangaKey: String, sourceKey: Long): Manga? {
        return mangaList.find {
            DiskUtil.buildValidFilename(it.title)
                .lowercase(Locale.US) == mangaKey.lowercase(Locale.US) && it.source == sourceKey
        }
    }

    /**
     * Adds a chapter that has just been download to this cache.
     *
     * @param chapterDirName the downloaded chapter's directory name.
     * @param mangaUniFile the directory of the manga.
     * @param manga the manga of the chapter.
     */
    @Synchronized
    fun addChapter(chapterDirName: String, mangaUniFile: UniFile, manga: Manga) {
        val id = manga.id ?: return
        val files = mangaFiles[id]
        val mangadexId = chapterDirName.substringAfterLast("- ")

        val set = if (chapterDirName.contains(MergeSource.name)) {
            mutableSetOf()
        } else {
            mutableSetOf(mangadexId)
        }

        if (files == null) {
            mangaFiles[id] = Pair(mutableSetOf(chapterDirName), set)
        } else {
            mangaFiles[id]?.first?.add(chapterDirName)
            if (!chapterDirName.contains(MergeSource.name)) {
                mangaFiles[id]?.second?.add(mangadexId)
            }
        }
    }

    /**
     * Removes a list of chapters that have been deleted from this cache.
     *
     * @param chapters the list of chapter to remove.
     * @param manga the manga of the chapter.
     */
    @Synchronized
    fun removeChapters(chapters: List<Chapter>, manga: Manga) {
        val id = manga.id ?: return
        mangaFiles[id] ?: return

        for (chapter in chapters) {
            if (!chapter.isMergedChapter()) {
                mangaFiles[id]!!.second.remove(chapter.mangadex_chapter_id)
            }

            provider.getValidChapterDirNames(chapter).forEach {
                if (it in mangaFiles[id]!!.first) {
                    mangaFiles[id]!!.first.remove(it)
                }
            }
        }
    }

    fun removeFolders(folders: List<String>, manga: Manga) {
        val id = manga.id ?: return
        mangaFiles[id] ?: return
        for (chapter in folders) {
            if (chapter in mangaFiles[id]!!.first) {
                mangaFiles[id]!!.first.remove(chapter)
            }
            if (!chapter.contains(MergeSource.name)) {
                val mangadexId = chapter.substringAfterLast("- ")
                mangaFiles[id]!!.second.remove(mangadexId)
            }
        }
    }

    /**
     * Removes a manga that has been deleted from this cache.
     *
     * @param manga the manga to remove.
     */
    @Synchronized
    fun removeManga(manga: Manga) {
        mangaFiles.remove(manga.id)
    }

    /**
     * Class to store the files under the root downloads directory.
     */
    private class RootDirectory(
        val dir: UniFile,
        var files: Map<Long, SourceDirectory> = hashMapOf(),
    )

    /**
     * Class to store the files under a source directory.
     */
    private class SourceDirectory(
        val dir: UniFile,
        var files: Map<Long, MutableSet<String>> = hashMapOf(),
    )

    /**
     * Class to store the files under a manga directory.
     */
    private class MangaDirectory(
        val dir: UniFile,
        var files: MutableSet<String> = hashSetOf(),
    )

    /**
     * Returns a new map containing only the key entries of [transform] that are not null.
     */
    private inline fun <K, V, R> Map<out K, V>.mapNotNullKeys(transform: (Map.Entry<K?, V>) -> R?): Map<R, V> {
        val destination = LinkedHashMap<R, V>()
        forEach { element -> transform(element)?.let { destination.put(it, element.value) } }
        return destination
    }

    /**
     * Returns a map from a list containing only the key entries of [transform] that are not null.
     */
    private inline fun <T, K, V> Array<T>.associateNotNullKeys(transform: (T) -> Pair<K?, V>): Map<K, V> {
        val destination = LinkedHashMap<K, V>()
        for (element in this) {
            val (key, value) = transform(element)
            if (key != null) {
                destination[key] = value
            }
        }
        return destination
    }
}
