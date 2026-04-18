package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.notificationManager
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class BackupRestorer(val context: Context, val notifier: BackupNotifier) {

    private val restoreHelper = RestoreHelper(context)

    private var restoreProgress = AtomicInteger(0)
    private var restoreAmount = 0
    private var totalAmount = 0
    private var skippedAmount = 0
    private var skippedTitles = emptyList<String>()
    private var categoriesAmount = 0

    // Thread-safe error lists
    private val errors = Collections.synchronizedList(mutableListOf<String>())
    private var cancelled = 0
    private val trackingErrors = Collections.synchronizedList(mutableListOf<String>())

    private val categoryRepository: CategoryRepository by injectLazy()
    private val chapterRepository: ChapterRepository by injectLazy()
    private val historyRepository: HistoryRepository by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    internal val trackManager: TrackManager by injectLazy()
    val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by injectLazy()

    // Notification Throttling
    private var lastNotificationTime = 0L
    private val NOTIFICATION_DELAY = 500L

    // Holder for prepared data to bridge Phase 1 (Prep) and Phase 2 (Write)
    private data class RestorableItem(
        val manga: Manga,
        val chapters: List<Chapter>,
        val categories: List<Int>,
        val history: List<BackupHistory>,
        val tracks: List<Track>,
        val mergeMangaList: List<MergeMangaImpl>,
        val backupCategories: List<BackupCategory>,
    )

    suspend fun restoreBackup(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            restoreProgress.set(0)
            errors.clear()
            trackingErrors.clear()
            cancelled = 0

            if (!performRestore(uri)) {
                false
            } else {

                val endTime = System.currentTimeMillis()
                val time = endTime - startTime

                val logFile = writeErrorLog()
                notifier.showRestoreComplete(time, errors.size, logFile?.parent, logFile?.name)
                true
            }
        }
    }

    private suspend fun performRestore(uri: Uri): Boolean {
        val backup = BackupUtil.decodeBackup(context, uri)
        val partitionedList = backup.backupManga.partition { sourceManager.isMangadex(it.source) }

        // [OPTIMIZATION] Pre-fetch all existing MangaDex manga IDs to avoid N read queries
        val dbMangaMap =
            db.getMangaList()
                .executeOnIO()
                .filter { sourceManager.isMangadex(it.source) }
                .associateBy { MdUtil.getMangaUUID(it.url) }

        return coroutineScope {
            val dexManga = partitionedList.first
            skippedTitles = partitionedList.second.map { it.title }
            totalAmount = backup.backupManga.size
            skippedAmount = totalAmount - dexManga.size
            restoreAmount = dexManga.size

            if (backup.backupCategories.isNotEmpty()) {
                restoreCategories(backup.backupCategories)
            }

            // Batch parameters
            val batchSize = 100 // Safe size for SQLite transactions
            val chunks = dexManga.groupBy { MdUtil.getMangaUUID(it.url) }.entries.chunked(batchSize)
            val semaphore = Semaphore(8) // concurrency limit for CPU prep

            chunks.forEach { chunk ->
                if (!isActive) return@coroutineScope false

                // PHASE 1: Parallel Preparation (CPU)
                val preparedItems =
                    chunk
                        .map { (uuid, mangaList) ->
                            async(Dispatchers.IO) {
                                semaphore.withPermit {
                                    ensureActive()
                                    prepareManga(mangaList, backup.backupCategories)
                                }
                            }
                        }
                        .awaitAll()
                        .filterNotNull()

                // PHASE 2: Transactional Write (DB I/O)
                val itemsWithCovers = mutableListOf<RestorableItem>()
                try {
                    db.inTransaction {
                        // [OPTIMIZATION] Fetch dbCategories once per chunk instead of per manga
                        val dbCategories = categoryRepository.getCategories()

                        // [OVERCLOCK] Pre-fetch related data for all existing manga in this chunk
                        // to prevent N+1 queries
                        val existingMangaIds = preparedItems.mapNotNull { item ->
                            dbMangaMap[MdUtil.getMangaUUID(item.manga.url)]?.id
                        }

                        val dbChaptersMap =
                            if (existingMangaIds.isNotEmpty())
                                chapterRepository.getChaptersForMangaIds(existingMangaIds).groupBy {
                                    it.manga_id
                                }
                            else emptyMap()
                        val dbMergeMangaMap =
                            if (existingMangaIds.isNotEmpty())
                                db.getMergeMangaList(existingMangaIds).executeAsBlocking().groupBy {
                                    it.mangaId
                                }
                            else emptyMap()
                        val dbTracksMap =
                            if (existingMangaIds.isNotEmpty())
                                db.getTracks(existingMangaIds).executeAsBlocking().groupBy {
                                    it.manga_id
                                }
                            else emptyMap()

                        // 1. Create a bridge map linking chapter_id -> manga_id
                        val chapterToMangaMap =
                            dbChaptersMap.values.flatten().associate { it.id to it.manga_id }

                        // 2. Fetch the bulk histories (use whatever plural name the dev gave the
                        // query)
                        // and group them using our bridge map!
                        val dbHistoriesMap =
                            if (existingMangaIds.isNotEmpty())
                                historyRepository.getHistoryByMangaIds(existingMangaIds).groupBy {
                                    history ->
                                    chapterToMangaMap[history.chapter_id]
                                }
                            else emptyMap()

                        preparedItems.forEach { item ->
                            val existingManga = dbMangaMap[MdUtil.getMangaUUID(item.manga.url)]
                            val mangaId = existingManga?.id
                            val dbChapters = dbChaptersMap[mangaId] ?: emptyList()
                            val dbMergeMangaList = dbMergeMangaMap[mangaId] ?: emptyList()
                            val dbTracks = dbTracksMap[mangaId] ?: emptyList()
                            val dbHistories = dbHistoriesMap[mangaId] ?: emptyList() // <-- Add this

                            writeMangaToDb(
                                item = item,
                                existingDbManga = existingManga,
                                dbCategories = dbCategories,
                                dbChapters = dbChapters,
                                dbMergeMangaList = dbMergeMangaList,
                                dbTracks = dbTracks,
                                dbHistories = dbHistories,
                            )

                            if (!item.manga.user_cover.isNullOrBlank()) {
                                itemsWithCovers.add(item)
                            }

                            updateProgress(item.manga.title)
                        }
                    }
                } catch (e: Exception) {
                    TimberKt.e(e) { "Batch transaction failed" }
                    preparedItems.forEach {
                        errors.add("${it.manga.title} - Batch Error: ${e.message}")
                    }
                }

                // 3. Process Covers Immediately (Clear memory now)
                if (itemsWithCovers.isNotEmpty()) {
                    restoreBatchCovers(itemsWithCovers)
                }

                // [FIX] Mandatory pause + yield
                // Allows SQLite WAL Checkpoint to clear the disk queue and UI to draw
                delay(200)
                kotlinx.coroutines.yield()
            }

            context.notificationManager.cancel(Notifications.ID_RESTORE_PROGRESS)

            // Process error lists
            val currentErrors = errors.toList()
            cancelled = currentErrors.count { it.contains("cancelled", true) }
            val finalErrors = currentErrors.filter { !it.contains("cancelled", true) }
            errors.clear()
            errors.addAll(finalErrors)

            val logFile = writeErrorLog()
            restoreHelper.showResultNotification(
                logFile?.parent,
                logFile?.name,
                categoriesAmount,
                restoreProgress.get(),
                restoreAmount,
                skippedAmount,
                totalAmount,
                cancelled,
                errors,
                trackingErrors,
            )

            true
        }
    }

    /** Prepares the objects in memory without touching the DB or FileSystem */
    private fun prepareManga(
        backupMangaList: List<BackupManga>,
        backupCategories: List<BackupCategory>,
    ): RestorableItem? {
        val title = backupMangaList.firstOrNull()?.title ?: return null
        try {
            val backupManga =
                if (backupMangaList.size == 1) {
                    backupMangaList.first()
                } else {
                    val tempCategories = backupMangaList.flatMap { it.categories }.distinct()
                    val tempChapters = backupMangaList.flatMap { it.chapters }.distinct()
                    val tempHistory = backupMangaList.flatMap { it.history }.distinct()
                    val tempTracks = backupMangaList.flatMap { it.tracking }.distinct()

                    backupMangaList
                        .first()
                        .copy(
                            categories = tempCategories,
                            chapters = tempChapters,
                            history = tempHistory,
                            tracking = tempTracks,
                        )
                }

            backupManga.source = SourceManager.getId(MdLang.ENGLISH.lang)
            val manga = backupManga.getMangaImpl()
            val chapters = backupManga.getChaptersImpl()
            val categories = backupManga.categories
            val history = backupManga.history
            val tracks = backupManga.getTrackingImpl()
            val mergeMangaList = backupManga.getMergeMangaImpl()

            return RestorableItem(
                manga = manga,
                chapters = chapters,
                categories = categories,
                history = history,
                tracks = tracks,
                mergeMangaList = mergeMangaList,
                backupCategories = backupCategories,
            )
        } catch (e: Exception) {
            TimberKt.e(e)
            errors.add("$title - Preparation Error: ${e.message}")
            return null
        }
    }

    /** Writes data to DB. Must be called inside a transaction */
    private suspend fun writeMangaToDb(
        item: RestorableItem,
        existingDbManga: Manga?,
        dbCategories: List<Category>,
        dbChapters: List<Chapter>,
        dbMergeMangaList: List<MergeMangaImpl>,
        dbTracks: List<Track>,
        dbHistories: List<History>,
    ) {
        try {
            val manga = item.manga

            // Use pre-fetched DB object to avoid read query
            if (existingDbManga != null) {
                restoreHelper.restoreMangaNoFetch(manga, existingDbManga)
            } else {
                manga.initialized = false
                manga.id = db.insertManga(manga).executeAsBlocking().insertedId()
            }

            // Restore related data using helper (Blocking calls are fine inside transaction)
            val updatedChapters =
                restoreHelper.restoreChaptersForMangaOffline(manga, item.chapters, dbChapters)
            restoreHelper.restoreMergeMangaForManga(manga, item.mergeMangaList, dbMergeMangaList)
            restoreHelper.restoreCategoriesForManga(
                manga,
                item.categories,
                item.backupCategories,
                dbCategories,
            )
            restoreHelper.restoreHistoryForManga(item.history, manga, updatedChapters, dbHistories)
            restoreHelper.restoreTrackForManga(manga, item.tracks, dbTracks)
        } catch (e: Exception) {
            TimberKt.e(e)
            errors.add("${item.manga.title} - Write Error: ${e.message}")
        }
    }

    private suspend fun restoreBatchCovers(items: List<RestorableItem>) = coroutineScope {
        val coverSemaphore = Semaphore(5)

        items
            .map { item ->
                async(Dispatchers.IO) {
                    coverSemaphore.withPermit {
                        runCatching {
                            item.manga.user_cover?.let { url ->
                                coverCache.setCustomCoverToCache(item.manga, url)
                                MangaCoverMetadata.remove(item.manga.id!!)
                            }
                        }
                    }
                }
            }
            .awaitAll()
    }

    private suspend fun restoreCategories(backupCategories: List<BackupCategory>) {
        restoreHelper.restoreCategories(backupCategories)
        categoriesAmount = backupCategories.size
        restoreAmount += 1
        restoreProgress.incrementAndGet()
        totalAmount += 1
        restoreHelper.showProgressNotification(
            restoreProgress.get(),
            totalAmount,
            context.getString(R.string.categories),
        )
    }

    private fun updateProgress(title: String) {
        val currentProgress = restoreProgress.incrementAndGet()
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime > NOTIFICATION_DELAY || currentProgress == totalAmount) {
            restoreHelper.showProgressNotification(currentProgress, totalAmount, title)
            lastNotificationTime = now
        }
    }

    fun writeErrorLog(): File? {
        return restoreHelper.writeErrorLog(errors, skippedAmount, skippedTitles)
    }
}
