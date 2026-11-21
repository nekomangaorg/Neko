package eu.kanade.tachiyomi.jobs.library

import androidx.room.withTransaction
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.dao.ArtworkDao
import org.nekomanga.data.database.dao.ChapterDao
import org.nekomanga.data.database.dao.HistoryDao
import org.nekomanga.data.database.dao.MangaAggregateDao
import org.nekomanga.data.database.dao.MangaCategoryDao
import org.nekomanga.data.database.dao.MangaDao
import org.nekomanga.data.database.dao.MergeMangaDao
import org.nekomanga.data.database.dao.TrackDao
import org.nekomanga.domain.track.store.DelayedTrackingStore
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** What one repair run did, for its notification and its log. */
class RepairResult {
    var merged = 0
    var fixed = 0
    val skipped = mutableListOf<SkippedEntry>()
    /** "<title>: <message>" for each title whose transaction failed. */
    val failed = mutableListOf<String>()
    val trackingConflicts = mutableListOf<TrackingConflict>()
}

/** A title whose tracks for one service pointed at different media ids. */
data class TrackingConflict(val title: String, val serviceId: Int)

/**
 * Applies the [LibraryRepairPlanner] plans. Each title is read and written in its own transaction,
 * so a failure or a stop rolls back only the title in flight and keeps the titles already done.
 */
class LibraryRepairer(
    private val appDatabase: AppDatabase,
    private val mangaDao: MangaDao,
    private val chapterDao: ChapterDao,
    private val historyDao: HistoryDao,
    private val trackDao: TrackDao,
    private val mergeMangaDao: MergeMangaDao,
    private val mangaCategoryDao: MangaCategoryDao,
    private val artworkDao: ArtworkDao,
    private val mangaAggregateDao: MangaAggregateDao,
    private val planner: LibraryRepairPlanner,
    private val folderNames: () -> Set<String>,
    private val delayedTrackingStore: DelayedTrackingStore,
    private val refreshDownloadCache: () -> Unit,
) {

    suspend fun inventory(): RepairInventory = withIOContext { inventory(folderNames()) }

    /**
     * Says whether a repair can start now. The library is read only once the gate is free and no
     * work that writes the same rows is running. Skipped entries alone still lead to the dialog,
     * because only the job's log names them.
     */
    suspend fun check(gate: LibraryRepairGate): RepairCheck {
        if (gate.isRunning) return RepairCheck.Running
        val blocking = gate.blockingWork()
        if (blocking.isNotEmpty()) return RepairCheck.Blocked(blocking)
        val inventory = inventory()
        return if (inventory.hasWork || inventory.skipped.isNotEmpty()) {
            RepairCheck.Confirm(inventory)
        } else {
            RepairCheck.NothingToRepair
        }
    }

    suspend fun repair(result: RepairResult, onProgress: (done: Int, total: Int) -> Unit) {
        try {
            withIOContext {
                val folders = folderNames()
                val inventory = inventory(folders)
                result.skipped += inventory.skipped
                inventory.work.forEachIndexed { index, group ->
                    ensureActive()
                    try {
                        // Room commits a group whose last write is done even when this job is
                        // cancelled, so a stop lets the group finish and be recorded
                        withContext(NonCancellable) {
                            val plan = appDatabase.withTransaction { applyGroup(group, folders) }
                            record(group, plan, result)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        result.failed += "${group.title}: ${e.message}"
                    }
                    onProgress(index + 1, inventory.work.size)
                }
            }
        } finally {
            if (result.merged + result.fixed > 0) refreshDownloadCache()
        }
    }

    // A large library's rows span several cursor windows, and a write between two of them fails
    // the read, so the rows are read in a transaction
    private suspend fun inventory(folders: Set<String>): RepairInventory {
        val rows = appDatabase.withTransaction { mangaDao.getMangaList() }
        return planner.inventory(rows, folders)
    }

    private suspend fun applyGroup(group: RepairGroup, folders: Set<String>): GroupPlan {
        val rows = mangaDao.getMangaByIds(group.rowIds)
        val ids = rows.map { it.id }
        val data =
            RepairGroupData(
                rows = rows,
                chapters = chapterDao.getChaptersForMangaIds(ids),
                history = historyDao.getHistoryByMangaIds(ids),
                tracks = trackDao.getTracksForMangaByIds(ids),
                merges = mergeMangaDao.getMergeMangaList(ids),
                categories = mangaCategoryDao.getMangaCategories(ids),
                artwork = artworkDao.getArtworkForMangaIds(ids),
                aggregates = ids.mapNotNull { mangaAggregateDao.getMangaAggregateSync(it) },
            )
        val plan = planner.planGroup(data, folders)
        if (plan is GroupPlan.Apply) write(plan.changes)
        return plan
    }

    // The rows go last, since deleting them cascades to every record the writes above did not move
    private suspend fun write(changes: GroupChanges) {
        mangaDao.updateManga(changes.keeper)
        if (changes.chapterUpdates.isNotEmpty()) chapterDao.updateChapters(changes.chapterUpdates)
        if (changes.historyUpserts.isNotEmpty()) {
            historyDao.upsertHistoryList(changes.historyUpserts)
        }
        if (changes.trackUpserts.isNotEmpty()) trackDao.insertTracks(changes.trackUpserts)
        if (changes.mergeUpserts.isNotEmpty()) mergeMangaDao.insertMergeManga(changes.mergeUpserts)
        if (changes.categoryInserts.isNotEmpty()) {
            mangaCategoryDao.insertMangaListCategories(changes.categoryInserts)
        }
        if (changes.artworkUpserts.isNotEmpty()) artworkDao.insertArtworks(changes.artworkUpserts)
        changes.aggregateInsert?.let { mangaAggregateDao.insertMangaAggregate(it) }
        if (changes.deletedRows.isNotEmpty()) mangaDao.deleteMangaList(changes.deletedRows)
    }

    private fun record(group: RepairGroup, plan: GroupPlan, result: RepairResult) {
        when (plan) {
            GroupPlan.Untouched -> Unit
            is GroupPlan.Skip -> result.skipped += SkippedEntry(group.title, plan.reason)
            is GroupPlan.Apply -> {
                val changes = plan.changes
                if (changes.deletedRows.isEmpty()) result.fixed++ else result.merged++
                moveDelayedTracking(changes.trackIdMoves)
                changes.trackConflictServices.forEach {
                    result.trackingConflicts += TrackingConflict(group.title, it)
                }
            }
        }
    }

    // Delayed tracking updates are kept by track id, so an update queued on a deleted track goes
    // to the track that replaced it
    private fun moveDelayedTracking(moves: Map<Long, Long>) {
        if (moves.isEmpty()) return
        val items = delayedTrackingStore.getItems().associateBy { it.trackId }
        moves.forEach { (oldId, newId) ->
            val item = items[oldId] ?: return@forEach
            delayedTrackingStore.add(newId, item.lastChapterRead)
            delayedTrackingStore.remove(oldId)
        }
    }

    companion object {
        fun create(): LibraryRepairer {
            val appDatabase = Injekt.get<AppDatabase>()
            val sourceManager = Injekt.get<SourceManager>()
            val downloadManager = Injekt.get<DownloadManager>()
            return LibraryRepairer(
                appDatabase = appDatabase,
                mangaDao = appDatabase.mangaDao(),
                chapterDao = appDatabase.chapterDao(),
                historyDao = appDatabase.historyDao(),
                trackDao = appDatabase.trackDao(),
                mergeMangaDao = appDatabase.mergeMangaDao(),
                mangaCategoryDao = appDatabase.mangaCategoryDao(),
                artworkDao = appDatabase.artworkDao(),
                mangaAggregateDao = appDatabase.mangaAggregateDao(),
                planner =
                    LibraryRepairPlanner(sourceManager.mangaDex.id, sourceManager::isMangadex) {
                        DiskUtil.buildValidFilename(it.displayTitle())
                    },
                folderNames = { downloadManager.getMangaFolders().mapNotNull { it.name }.toSet() },
                delayedTrackingStore = Injekt.get(),
                refreshDownloadCache = downloadManager::refreshCache,
            )
        }
    }
}
