package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.util.lang.isUUID
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.nekomanga.constants.Constants.TMP_DIR_SUFFIX
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var renewJob: Job? = null

    private val _changes: MutableSharedFlow<Unit> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    init {
        storageManager.baseDirChanges.onEach { forceRenewCache() }.launchIn(scope)
        renew()
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

        val mangadexIds =
            fileNames.mapNotNullTo(mutableSetOf()) {
                it.takeLast(36).takeIf { uuid -> uuid.isUUID() }
            }

        mangaFiles[id] = MangaFiles(fileNames, mangadexIds)
        _changes.tryEmit(Unit)
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
        }
    }

    fun forceRenewCache() {
        renew()
    }

    /** Renews the downloads cache. */
    @Synchronized
    private fun renew() {
        if (renewJob?.isActive == true) return

        renewJob = scope.launch {
            try {
                renewCache()
            } catch (e: Exception) {
                TimberKt.e(e) { "Failed to renew cache" }
            }
        }
    }

    private suspend fun renewCache() {
        TimberKt.d { "Renewing cache" }

        // Map Source ID to the directory on disk
        val sourceDir =
            storageManager.getDownloadsDirectory()?.listFiles()?.find {
                it.name == provider.getSourceDirName()
            } ?: return

        val mangaRepository: MangaRepository = Injekt.get()
        val mangaDexId = sourceManager.mangaDex.id
        val allManga = mangaRepository.getMangaList().filter { it.source == mangaDexId }

        // UUID-keyed map for new-format folders ("Title [uuid]")
        val mangaByUuid = allManga.associateBy { it.uuid().lowercase(Locale.getDefault()) }
        // Title-keyed map for legacy-format folders ("Title")
        val mangaByTitle = allManga.associateBy {
            DiskUtil.buildValidFilename(it.displayTitle()).lowercase(Locale.getDefault())
        }
        // Count per normalised title to detect unambiguous legacy folders eligible for migration
        val titleCount =
            allManga
                .groupBy {
                    DiskUtil.buildValidFilename(it.displayTitle()).lowercase(Locale.getDefault())
                }
                .mapValues { it.value.size }

        val newMangaFiles = ConcurrentHashMap<Long, MangaFiles>()
        coroutineScope {
            sourceDir
                .listFiles()
                .orEmpty()
                .map { mangaDir ->
                    async {
                        val dirName = mangaDir.name ?: return@async
                        val dirLower = dirName.lowercase(Locale.getDefault())

                        val uuidFromSuffix =
                            dirLower.substringAfterLast("[", "").removeSuffix("]").takeIf {
                                it.isUUID()
                            }

                        val manga =
                            if (uuidFromSuffix != null) {
                                mangaByUuid[uuidFromSuffix]
                            } else {
                                // Legacy title-only folder: rename to UUID-based name when
                                // the title is unambiguous so future cache cycles use the new
                                // format and avoid the same-title collision.
                                val legacyManga = mangaByTitle[dirLower]
                                if (legacyManga != null && titleCount[dirLower] == 1) {
                                    try {
                                        mangaDir.renameTo(provider.getMangaDirName(legacyManga))
                                    } catch (e: Exception) {
                                        TimberKt.e(e) {
                                            "Failed to migrate download folder for " +
                                                legacyManga.displayTitle()
                                        }
                                    }
                                    legacyManga
                                } else {
                                    null
                                }
                            } ?: return@async

                        val id = manga.id ?: return@async

                        val files =
                            mangaDir.listFiles().orEmpty().mapNotNullTo(mutableSetOf()) {
                                it.name?.substringBeforeLast(".cbz")
                            }

                        val mangadexIds =
                            files.mapNotNullTo(mutableSetOf()) {
                                it.takeLast(36).takeIf { uuid -> uuid.isUUID() }
                            }

                        newMangaFiles[id] = MangaFiles(files, mangadexIds)
                    }
                }
                .awaitAll()
        }
        mangaFiles.clear()
        mangaFiles.putAll(newMangaFiles)

        lastRenew = System.currentTimeMillis()
        _changes.tryEmit(Unit)
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
