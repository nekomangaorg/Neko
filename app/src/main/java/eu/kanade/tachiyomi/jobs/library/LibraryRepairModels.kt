package eu.kanade.tachiyomi.jobs.library

import org.nekomanga.data.database.entity.ArtworkEntity
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.entity.MangaAggregateEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.entity.MergeMangaEntity
import org.nekomanga.data.database.entity.TrackEntity

/** The rows of one MangaDex title that the repair writes, keyed by the lowercase UUID. */
data class RepairGroup(val key: String, val rowIds: List<Long>, val title: String)

/**
 * What a repair would do. [duplicateTitles] counts groups of two or more rows, [damagedEntries]
 * single rows that lost their source.
 */
data class RepairInventory(
    val work: List<RepairGroup>,
    val duplicateTitles: Int,
    val damagedEntries: Int,
    val skipped: List<SkippedEntry>,
) {
    val hasWork: Boolean
        get() = work.isNotEmpty()
}

/** What tapping Repair library leads to: a toast, or the confirmation dialog for [Confirm]. */
sealed interface RepairCheck {
    /** The repair job holds the gate. */
    data object Running : RepairCheck

    /** Work that writes the same rows is running. */
    data class Blocked(val work: List<BlockingWork>) : RepairCheck

    data object NothingToRepair : RepairCheck

    data class Confirm(val inventory: RepairInventory) : RepairCheck
}

data class SkippedEntry(val title: String, val reason: SkipReason)

sealed interface SkipReason {
    /** The url is not a MangaDex title url, or the source is neither MangaDex nor unset. */
    data object NotMangaDex : SkipReason

    /** Download folders exist under more than one of the group's names. */
    data class DownloadFolders(val names: List<String>) : SkipReason
}

/** Every row of one group with the records that reference them, read in one transaction. */
data class RepairGroupData(
    val rows: List<MangaEntity>,
    val chapters: List<ChapterEntity>,
    val history: List<HistoryEntity>,
    val tracks: List<TrackEntity>,
    val merges: List<MergeMangaEntity>,
    val categories: List<MangaCategoryEntity>,
    val artwork: List<ArtworkEntity>,
    val aggregates: List<MangaAggregateEntity>,
)

sealed interface GroupPlan {
    data object Untouched : GroupPlan

    data class Skip(val reason: SkipReason.DownloadFolders) : GroupPlan

    data class Apply(val changes: GroupChanges) : GroupPlan
}

/**
 * The writes for one group. [trackIdMoves] maps a deleted track id to the track that replaces it,
 * and [trackConflictServices] lists the services whose tracks pointed at different media ids.
 */
data class GroupChanges(
    val keeper: MangaEntity,
    val deletedRows: List<MangaEntity>,
    val chapterUpdates: List<ChapterEntity> = emptyList(),
    val historyUpserts: List<HistoryEntity> = emptyList(),
    val trackUpserts: List<TrackEntity> = emptyList(),
    val mergeUpserts: List<MergeMangaEntity> = emptyList(),
    val categoryInserts: List<MangaCategoryEntity> = emptyList(),
    val artworkUpserts: List<ArtworkEntity> = emptyList(),
    val aggregateInsert: MangaAggregateEntity? = null,
    val trackIdMoves: Map<Long, Long> = emptyMap(),
    val trackConflictServices: List<Int> = emptyList(),
)
