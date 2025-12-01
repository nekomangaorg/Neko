package eu.kanade.tachiyomi.data.download

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
import kotlinx.coroutines.launch
import org.nekomanga.constants.Constants.TMP_DIR_SUFFIX
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slows down the app.
 */
class DownloadCache(
    private val provider: DownloadProvider,
    private val sourceManager: SourceManager,
    private val storageManager: StorageManager = Injekt.get(),
) {

    private val renewInterval = TimeUnit.HOURS.toMillis(1)
    private var lastRenew = 0L

    // Optimization: Use ConcurrentHashMap for thread safety between UI reads and IO writes
    private val mangaFiles: ConcurrentHashMap<Long, MangaFiles> = ConcurrentHashMap()

    // Optimization: Data class is lighter than Pair<MutableSet, MutableSet>
    private data class MangaFiles(
        val files: MutableSet<String>,
        val mangadexIds: MutableSet<String>,
    )

    val scope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        storageManager.baseDirChanges.onEach { forceRenewCache() }.launchIn(scope)
        scope.launch { renew() }
    }

    fun isChapterDownloaded(chapter: Chapter, manga: Manga, skipCache: Boolean): Boolean {
        if (skipCache) {
            return provider.findChapterDir(chapter, manga) != null
        }

        checkRenew()

        val cache = mangaFiles[manga.id] ?: return false
        val fileNames = cache.files
        val mangadexIds = cache.mangadexIds

        // Fast path: Check UUIDs/Mangadex IDs from memory
        if (!chapter.isMergedChapter()) {
            if (
                chapter.mangadex_chapter_id.isNotEmpty() &&
                    chapter.mangadex_chapter_id in mangadexIds
            ) {
                return true
            }
            if (chapter.old_mangadex_id != null && chapter.old_mangadex_id in mangadexIds) {
                return true
            }
        }

        // Slow path: Check directory names (requires provider generation)
        val validChapterDirNames = provider.getValidChapterDirNames(chapter)
        return validChapterDirNames.any { it in fileNames }
    }

    @Synchronized
    fun updateManga(manga: Manga) {
        val id = manga.id ?: return
        val mangaDir = provider.findMangaDir(manga)

        if (mangaDir == null) {
            mangaFiles.remove(id)
            return
        }

        val fileNames =
            mangaDir.listFiles().orEmpty().mapNotNullTo(mutableSetOf()) {
                it.name?.substringBeforeLast(".cbz")
            }

        val mangadexIds = fileNames.map { it.takeLast(36) }.filterTo(mutableSetOf()) { it.isUUID() }

        mangaFiles[id] = MangaFiles(fileNames, mangadexIds)
    }

    fun getDownloadCount(manga: Manga, forceCheckFolder: Boolean = false): Int {
        checkRenew()
        if (forceCheckFolder) {
            val mangaDir = provider.findMangaDir(manga) ?: return 0
            return mangaDir.listFiles { _, filename -> !filename.endsWith(TMP_DIR_SUFFIX) }?.size
                ?: 0
        } else {
            val cache = mangaFiles[manga.id] ?: return 0
            return cache.files.count { !it.endsWith(TMP_DIR_SUFFIX) }
        }
    }

    fun getDownloadCounts(mangaIds: List<Long>): Map<Long, Int> {
        checkRenew()
        return mangaIds.associateWith { mangaId ->
            mangaFiles[mangaId]?.files?.count { !it.endsWith(TMP_DIR_SUFFIX) } ?: 0
        }
    }

    fun getAllDownloadFiles(manga: Manga): List<UniFile> {
        val mangaDir = provider.findMangaDir(manga) ?: return emptyList()
        return mangaDir
            .listFiles { _, filename -> !filename.endsWith(TMP_DIR_SUFFIX) }
            .orEmpty()
            .toList()
    }

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
    @Synchronized
    private fun renew() {
        TimberKt.d { "Renewing cache" }

        // Map Source ID to the directory on disk
        val sourceDir =
            storageManager.getDownloadsDirectory()?.listFiles()?.find {
                it.name == provider.getSourceDirName()
            } ?: return

        val db: DatabaseHelper by injectLazy()
        // Optimization: Fetch once
        val allManga = db.getMangaList().executeAsBlocking()

        // 3. Create lookup map for O(1) access
        val mangaLookup =
            allManga.associateBy {
                DiskUtil.buildValidFilename(it.displayTitle()).lowercase(Locale.getDefault())
            }

        // 4. Iterate over the folders on disk
        sourceDir.listFiles().orEmpty().forEach { mangaDir ->
            val dirName = mangaDir.name ?: return@forEach
            val manga = mangaLookup[dirName.lowercase(Locale.getDefault())] ?: return@forEach
            val id = manga.id ?: return@forEach

            val files =
                mangaDir.listFiles().orEmpty().mapNotNullTo(mutableSetOf()) {
                    it.name?.substringBeforeLast(".cbz")
                }

            val mangadexIds = files.map { it.takeLast(36) }.filterTo(mutableSetOf()) { it.isUUID() }

            mangaFiles[id] = MangaFiles(files, mangadexIds)
        }
    }

    @Synchronized
    fun addChapter(chapterDirName: String, manga: Manga) {
        val id = manga.id ?: return
        val cleanName = chapterDirName.substringBeforeLast(".cbz")
        val mangadexId = cleanName.substringAfterLast("- ")

        // Utilize computeIfAbsent for atomic initialization if needed,
        // though we are in a Synchronized block so standard get/put is fine.
        val entry = mangaFiles.getOrPut(id) { MangaFiles(mutableSetOf(), mutableSetOf()) }
        entry.files.add(cleanName)
        if (!MergeType.containsMergeSourceName(chapterDirName)) {
            entry.mangadexIds.add(mangadexId)
        }
    }

    @Synchronized
    fun removeChapters(chapters: List<Chapter>, manga: Manga) {
        val id = manga.id ?: return
        val cache = mangaFiles[id] ?: return

        for (chapter in chapters) {
            if (!chapter.isMergedChapter()) {
                cache.mangadexIds.remove(chapter.mangadex_chapter_id)
            }

            val validNames = provider.getValidChapterDirNames(chapter)
            validNames.forEach { cache.files.remove(it) }
        }
    }

    @Synchronized
    fun removeFolders(folders: List<String>, manga: Manga) {
        val id = manga.id ?: return
        val cache = mangaFiles[id] ?: return

        for (folder in folders) {
            cache.files.remove(folder)
            if (!MergeType.containsMergeSourceName(folder)) {
                val mangadexId = folder.substringAfterLast("- ")
                cache.mangadexIds.remove(mangadexId)
            }
        }
    }

    @Synchronized
    fun removeManga(manga: Manga) {
        manga.id?.let { mangaFiles.remove(it) }
    }
}
