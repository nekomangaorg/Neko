package eu.kanade.tachiyomi.data.download

import androidx.core.text.isDigitsOnly
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.util.lang.isUUID
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.constants.Constants.TMP_DIR_SUFFIX
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slowdowns the app. The cache is invalidated by the time
 * defined in [renewInterval] as we don't have any control over the filesystem and the user can
 * delete the folders at any time without the app noticing.
 *
 * @param provider the downloads directories provider.
 * @param sourceManager the source manager.
 * @param preferences the preferences of the app.
 */
class DownloadCache(
    private val provider: DownloadProvider,
    private val sourceManager: SourceManager,
    private val storageManager: StorageManager = Injekt.get(),
) {

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = TimeUnit.HOURS.toMillis(1)

    /** The last time the cache was refreshed. */
    private var lastRenew = 0L

    private var mangaFiles: MutableMap<Long, Pair<MutableSet<String>, MutableSet<String>>> =
        mutableMapOf()

    val scope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        storageManager.baseDirChanges
            .onEach { forceRenewCache() } // invalidate cache
            .launchIn(scope)
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
            return provider.findChapterDir(chapter, manga) != null
        }

        checkRenew()

        val fileNames = mangaFiles[manga.id]?.first ?: return false
        val mangadexIds = mangaFiles[manga.id]?.second ?: return false

        if (
            !chapter.isMergedChapter() &&
                chapter.mangadex_chapter_id.isNotEmpty() &&
                chapter.mangadex_chapter_id in mangadexIds
        ) {
            return true
        }
        if (
            !chapter.isMergedChapter() &&
                chapter.old_mangadex_id != null &&
                chapter.old_mangadex_id in mangadexIds
        ) {
            return true
        }

        val validChapterDirNames = provider.getValidChapterDirNames(chapter)
        return validChapterDirNames.any { it in fileNames || "$it.cbz" in fileNames }
    }

    fun findChapterDirName(chapter: Chapter, manga: Manga): String {
        return provider.findChapterDir(chapter, manga)?.name ?: ""
    }

    /**
     * Returns the amount of downloaded chapters for a manga.
     *
     * @param manga the manga to check.
     */
    fun getDownloadCount(manga: Manga, forceCheckFolder: Boolean = false): Int {
        checkRenew()

        if (forceCheckFolder) {
            val mangaDir = provider.findMangaDir(manga)

            mangaDir ?: return 0

            val listFiles =
                mangaDir.listFiles { _, filename -> !filename.endsWith(TMP_DIR_SUFFIX) }.orEmpty()

            return listFiles.size
        } else {
            mangaFiles[manga.id] ?: return 0
            val files = mangaFiles[manga.id]!!.first.filter { !it.endsWith(TMP_DIR_SUFFIX) }
            val ids = mangaFiles[manga.id]!!.second
            var count = files.size
            files.forEach {
                if (!MergeType.containsMergeSourceName(it)) {
                    val mangadexId = it.substringAfterLast("- ")
                    if (
                        mangadexId.isNotBlank() &&
                            mangadexId.isDigitsOnly() &&
                            !ids.contains(mangadexId)
                    ) {
                        count++
                    }
                }
            }
            return count
        }
    }

    fun getDownloadCounts(mangaIds: List<Long>): Map<Long, Int> {
        checkRenew()
        return mangaIds.associateWith { mangaId ->
            mangaFiles[mangaId]?.first?.count { !it.endsWith(TMP_DIR_SUFFIX) } ?: 0
        }
    }

    fun getAllDownloadFiles(manga: Manga): List<UniFile> {
        val mangaDir = provider.findMangaDir(manga)

        mangaDir ?: return emptyList()
        return mangaDir
            .listFiles { _, filename -> !filename.endsWith(TMP_DIR_SUFFIX) }
            .orEmpty()
            .toList()
    }

    /** Checks if the cache needs a renewal and performs it if needed. */
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

    /** Renews the downloads cache. */
    private fun renew() {
        TimberKt.d { "Renewing cache" }
        val onlineSources = listOf(sourceManager.mangaDex)

        val sourceDirs =
            storageManager
                .getDownloadsDirectory()
                ?.listFiles()
                .orEmpty()
                .associate { it.name to SourceDirectory(it) }
                .mapNotNullKeys { entry ->
                    onlineSources.find { provider.getSourceDirName() == entry.key }?.id
                }

        val db: DatabaseHelper by injectLazy()
        val mangaGroupedBySource = db.getMangaList().executeAsBlocking().groupBy { it.source }

        sourceDirs.forEach { sourceValue ->
            val sourceMangaRaw =
                mangaGroupedBySource[sourceValue.key]?.toMutableSet() ?: return@forEach
            val sourceMangaPair = sourceMangaRaw.partition { it.favorite }

            val sourceDir = sourceValue.value

            val mangaDirs =
                sourceDir.dir
                    .listFiles()
                    .orEmpty()
                    .mapNotNull { mangaDir ->
                        val name = mangaDir.name ?: return@mapNotNull null
                        val chapterDirs =
                            mangaDir
                                .listFiles()
                                .orEmpty()
                                .mapNotNull { chapterFile ->
                                    chapterFile.name?.substringBeforeLast(".cbz")
                                }
                                .toHashSet()
                        name to MangaDirectory(mangaDir, chapterDirs)
                    }
                    .toMap()

            val trueMangaDirs =
                mangaDirs
                    .mapNotNull { mangaDir ->
                        val manga =
                            findManga(sourceMangaPair.first, mangaDir.key, sourceValue.key)
                                ?: findManga(sourceMangaPair.second, mangaDir.key, sourceValue.key)
                        val id = manga?.id ?: return@mapNotNull null

                        val mangadexIds =
                            mangaDir.value.files
                                .map { it.takeLast(36) }
                                .filter { it.isUUID() }
                                .toMutableSet()

                        id to Pair(mangaDir.value.files, mangadexIds)
                    }
                    .toMap()

            mangaFiles.putAll(trueMangaDirs)
        }
    }

    /** Searches a manga list and matches the given mangakey and source key */
    private fun findManga(mangaList: List<Manga>, mangaKey: String, sourceKey: Long): Manga? {
        return mangaList.find {
            DiskUtil.buildValidFilename(it.title).lowercase(Locale.US) ==
                mangaKey.lowercase(Locale.US) && it.source == sourceKey
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
    fun addChapter(chapterDirName: String, manga: Manga) {
        val id = manga.id ?: return
        val files = mangaFiles[id]
        val mangadexId = chapterDirName.substringAfterLast("- ")

        val set =
            when (MergeType.containsMergeSourceName(chapterDirName)) {
                true -> mutableSetOf()
                false -> mutableSetOf(mangadexId)
            }

        if (files == null) {
            mangaFiles[id] = Pair(mutableSetOf(chapterDirName), set)
        } else {
            mangaFiles[id]?.first?.add(chapterDirName)
            if (!MergeType.containsMergeSourceName(chapterDirName)) {
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
        manga.id ?: return
        mangaFiles[manga.id] ?: return

        for (chapter in chapters) {
            if (!chapter.isMergedChapter()) {
                mangaFiles[manga.id]!!.second.remove(chapter.mangadex_chapter_id)
            }

            provider.getValidChapterDirNames(chapter).forEach {
                if (it in mangaFiles[manga.id]!!.first) {
                    mangaFiles[manga.id]!!.first.remove(it)
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
            if (!MergeType.containsMergeSourceName(chapter)) {
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

    /** Class to store the files under the root downloads directory. */
    private class RootDirectory(
        val dir: UniFile,
        var files: Map<Long, SourceDirectory> = hashMapOf(),
    )

    /** Class to store the files under a source directory. */
    private class SourceDirectory(
        val dir: UniFile,
        var files: Map<Long, MutableSet<String>> = hashMapOf(),
    )

    /** Class to store the files under a manga directory. */
    private class MangaDirectory(val dir: UniFile, var files: MutableSet<String> = hashSetOf())

    /** Returns a new map containing only the key entries of [transform] that are not null. */
    private inline fun <K, V, R> Map<out K, V>.mapNotNullKeys(
        transform: (Map.Entry<K?, V>) -> R?
    ): Map<R, V> {
        val mutableMap = ConcurrentHashMap<R, V>()
        forEach { element -> transform(element)?.let { mutableMap[it] = element.value } }
        return mutableMap
    }
}
