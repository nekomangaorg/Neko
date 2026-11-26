package eu.kanade.tachiyomi.jobs.library

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import org.nekomanga.data.database.entity.ArtworkEntity
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.entity.MangaAggregateEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.entity.MergeMangaEntity
import org.nekomanga.data.database.entity.TrackEntity

/**
 * Decides how the rows of one MangaDex title fold into one. It reads nothing and writes nothing:
 * [LibraryRepairer] hands it rows and applies the plan. [folderName] gives the download folder name
 * of a row, and a row's folder is found only by that name. [trackStatuses] gives a tracker's
 * completed and reading values by service id, since each tracker numbers its statuses its own way.
 */
class LibraryRepairPlanner(
    private val mangaDexSourceId: Long,
    private val isMangadex: (Long) -> Boolean,
    private val trackStatuses: (Int) -> TrackStatuses?,
    private val folderName: (MangaEntity) -> String,
) {

    /**
     * Groups [rows] by MangaDex UUID and counts the work, with the same row merge and folder rule
     * as [planGroup]. Rows that are not MangaDex titles are listed as skipped and never grouped.
     */
    fun inventory(rows: List<MangaEntity>, existingFolders: Set<String>): RepairInventory {
        val (candidates, invalid) = rows.sortedBy { it.id }.partition { isCandidate(it) }
        val skipped =
            invalid.mapTo(mutableListOf()) {
                SkippedEntry(it.displayTitle(), SkipReason.NotMangaDex)
            }
        val work = mutableListOf<RepairGroup>()
        candidates
            .groupBy { MdUtil.getMangaUUID(it.url).lowercase() }
            .forEach { (key, group) ->
                if (isHealthy(group)) return@forEach
                when (val folded = fold(group, emptyList(), existingFolders)) {
                    is Folded.Kept ->
                        work += RepairGroup(key, group.map { it.id }, folded.keeper.displayTitle())
                    is Folded.Skipped ->
                        skipped += SkippedEntry(folded.merged.displayTitle(), folded.reason)
                }
            }
        return RepairInventory(
            work = work,
            duplicateTitles = work.count { it.rowIds.size > 1 },
            damagedEntries = work.count { it.rowIds.size == 1 },
            skipped = skipped,
        )
    }

    fun planGroup(data: RepairGroupData, existingFolders: Set<String>): GroupPlan {
        val rows = data.rows.sortedBy { it.id }
        if (rows.isEmpty() || isHealthy(rows)) return GroupPlan.Untouched
        return when (val folded = fold(rows, data.chapters, existingFolders)) {
            is Folded.Skipped -> GroupPlan.Skip(folded.reason)
            is Folded.Kept -> GroupPlan.Apply(foldRecords(rows, folded.keeper, data))
        }
    }

    private fun isCandidate(row: MangaEntity): Boolean =
        MdUtil.isMangaUrl(row.url) &&
            (isMangadex(row.source) || row.source == SourceManager.UNSET_SOURCE)

    private fun isHealthy(rows: List<MangaEntity>): Boolean =
        rows.size == 1 && isMangadex(rows.single().source)

    /**
     * The download folder rule. When only one of the group's folder names exists on disk and it is
     * not the merged row's, the kept row takes the title and user title of the row that owns that
     * folder, so the folder is still found. Folders under two or more names skip the group.
     */
    private fun fold(
        rows: List<MangaEntity>,
        chapters: List<ChapterEntity>,
        existingFolders: Set<String>,
    ): Folded {
        val merged = mergeRows(rows, chapters)
        val final = folderName(merged)
        val existing =
            (listOf(merged) + rows).map(folderName).distinct().filter { it in existingFolders }
        return when {
            existing.isEmpty() || existing == listOf(final) -> Folded.Kept(merged)
            existing.size == 1 -> {
                val owner = rows.first { folderName(it) == existing.single() }
                Folded.Kept(merged.copy(title = owner.title, userTitle = owner.userTitle))
            }
            else -> Folded.Skipped(merged, SkipReason.DownloadFolders(existing))
        }
    }

    /**
     * The kept row is the oldest. MangaDex details come from the first row with a MangaDex source,
     * each user setting from the first row that has it, and the id and next update stay the
     * keeper's.
     */
    private fun mergeRows(rows: List<MangaEntity>, chapters: List<ChapterEntity>): MangaEntity {
        val keeper = rows.first()
        val donor = donorOf(rows)
        return keeper.copy(
            source = if (isMangadex(keeper.source)) keeper.source else mangaDexSourceId,
            title = donor.title,
            artist = donor.artist,
            author = donor.author,
            description = donor.description,
            genre = donor.genre,
            status = donor.status,
            thumbnailUrl = donor.thumbnailUrl,
            langFlag = donor.langFlag,
            altTitles = donor.altTitles,
            otherUrls = donor.otherUrls,
            anilistId = donor.anilistId,
            kitsuId = donor.kitsuId,
            myAnimeListId = donor.myAnimeListId,
            mangaUpdatesId = donor.mangaUpdatesId,
            animePlanetId = donor.animePlanetId,
            missingChapters = donor.missingChapters,
            rating = donor.rating,
            users = donor.users,
            threadId = donor.threadId,
            repliesCount = donor.repliesCount,
            lastVolumeNumber = donor.lastVolumeNumber,
            lastChapterNumber = donor.lastChapterNumber,
            dynamicCover = donor.dynamicCover,
            initialized = donor.initialized,
            userTitle = rows.firstSet(keeper) { it.userTitle },
            userCover = rows.firstSet(keeper) { it.userCover },
            filteredScanlators = rows.firstSet(keeper) { it.filteredScanlators },
            filteredLanguage = rows.firstSet(keeper) { it.filteredLanguage },
            mergeMangaUrl = rows.firstSet(keeper) { it.mergeMangaUrl },
            mergeMangaImageUrl = rows.firstSet(keeper) { it.mergeMangaImageUrl },
            viewerFlags =
                rows.map { it.viewerFlags }.firstOrNull { it != 0 && it != -1 }
                    ?: keeper.viewerFlags,
            chapterFlags = rows.map { it.chapterFlags }.firstOrNull { it != 0 } ?: 0,
            followStatus =
                rows.map { it.followStatus }.firstOrNull { it != FollowStatus.UNFOLLOWED }
                    ?: FollowStatus.UNFOLLOWED,
            favorite = rows.any { it.favorite },
            lastUpdate = rows.mapNotNull { it.lastUpdate }.maxOrNull(),
            dateAdded = dateAdded(rows, chapters),
        )
    }

    /**
     * The rule of `combineDateAdded` in BackupManga.kt: the earliest positive date, with the
     * chapter fetch times counted when a row has no date, since the copy Pull inserted holds the
     * time of that Pull.
     */
    private fun dateAdded(rows: List<MangaEntity>, chapters: List<ChapterEntity>): Long {
        val dates = rows.map { it.dateAdded ?: 0L }
        val known = if (0L in dates) dates + chapters.map { it.dateFetch } else dates
        return known.filter { it > 0L }.minOrNull() ?: 0L
    }

    /** The first non-blank value in id order, or the keeper's own value when no row has one. */
    private inline fun List<MangaEntity>.firstSet(
        keeper: MangaEntity,
        field: (MangaEntity) -> String?,
    ): String? =
        firstNotNullOfOrNull { row -> field(row)?.takeIf { it.isNotBlank() } } ?: field(keeper)

    private fun donorOf(rows: List<MangaEntity>): MangaEntity =
        rows.firstOrNull { isMangadex(it.source) } ?: rows.first()

    /**
     * Moves the records of the other rows to [keeper] and combines the ones both sides hold. Only
     * records whose values change are listed; the cascade deletes the rest with their rows.
     */
    private fun foldRecords(
        rows: List<MangaEntity>,
        keeper: MangaEntity,
        data: RepairGroupData,
    ): GroupChanges {
        val order = rows.withIndex().associate { (index, row) -> row.id to index }
        val chapters = data.chapters.inRowOrder(order, ChapterEntity::mangaId, ChapterEntity::id)
        val survivors =
            chapters
                .groupBy { it.url }
                .values
                .map { sameUrl ->
                    Survivor(sameUrl.first(), sameUrl.drop(1).filter { it.mangaId != keeper.id })
                }
        val history = data.history.associateBy { it.chapterId }
        val tracks =
            foldTracks(
                keeper.id,
                donorOf(rows).id,
                data.tracks.inRowOrder(order, TrackEntity::mangaId, TrackEntity::id),
            )
        val merges = data.merges.inRowOrder(order, MergeMangaEntity::mangaId, MergeMangaEntity::id)
        val categories =
            data.categories.inRowOrder(order, MangaCategoryEntity::mangaId, MangaCategoryEntity::id)
        val keeperCategories =
            categories.filter { it.mangaId == keeper.id }.map { it.categoryId }.toSet()
        val artwork = data.artwork.inRowOrder(order, ArtworkEntity::mangaId, ArtworkEntity::id)
        val aggregates =
            data.aggregates.inRowOrder(
                order,
                MangaAggregateEntity::mangaId,
                MangaAggregateEntity::mangaId,
            )
        return GroupChanges(
            keeper = keeper,
            deletedRows = rows.drop(1),
            chapterUpdates = survivors.mapNotNull { it.update(keeper.id) },
            historyUpserts = survivors.mapNotNull { it.history(history) },
            trackUpserts = tracks.upserts,
            mergeUpserts =
                merges
                    .distinctBy { it.mergeType.mergeKey(it.url) }
                    .filter { it.mangaId != keeper.id }
                    .map { it.copy(mangaId = keeper.id) },
            categoryInserts =
                categories
                    .filter { it.mangaId != keeper.id }
                    .map { it.categoryId }
                    .distinct()
                    .filter { it !in keeperCategories }
                    .map { MangaCategoryEntity(mangaId = keeper.id, categoryId = it) },
            artworkUpserts =
                if (artwork.any { it.mangaId == keeper.id }) emptyList()
                else
                    artwork
                        .groupBy { it.mangaId }
                        .values
                        .firstOrNull()
                        .orEmpty()
                        .map { it.copy(mangaId = keeper.id) },
            aggregateInsert =
                if (aggregates.any { it.mangaId == keeper.id }) null
                else aggregates.firstOrNull()?.copy(mangaId = keeper.id),
            trackIdMoves = tracks.moves,
            trackConflictServices = tracks.conflicts,
        )
    }

    /**
     * One track per service, built the way restore's `combineTracking` does: the details donor's
     * track, or the first one, with the furthest chapter of any track. Unlike restore, a completed
     * or reading status on any track of the service wins over the donor's. The track is written
     * into the keeper's track for that service, or moves over when the keeper has none. The other
     * tracks of the service map to that track id in [TrackFold.moves].
     */
    private fun foldTracks(keeperId: Long, donorId: Long, tracks: List<TrackEntity>): TrackFold {
        val upserts = mutableListOf<TrackEntity>()
        val moves = mutableMapOf<Long, Long>()
        val conflicts = mutableListOf<Int>()
        tracks
            .groupBy { it.trackServiceId }
            .forEach { (service, sameService) ->
                val details =
                    sameService.firstOrNull { it.mangaId == donorId } ?: sameService.first()
                val keeperTrack = sameService.firstOrNull { it.mangaId == keeperId }
                val target = keeperTrack?.id ?: details.id
                val combined =
                    details.combinedWith(sameService).copy(id = target, mangaId = keeperId)
                if (combined != keeperTrack) upserts += combined
                sameService.filter { it.id != target }.forEach { moves[it.id] = target }
                if (sameService.map { it.mediaId }.distinct().size > 1) conflicts += service
            }
        return TrackFold(upserts, moves, conflicts)
    }

    /**
     * A score or reading date left at 0 comes from the first of [tracks] that has one. The status
     * is completed when any of [tracks] is, else reading when any is, else this track's own, with
     * the values [trackStatuses] gives for the service.
     */
    private fun TrackEntity.combinedWith(tracks: List<TrackEntity>): TrackEntity {
        val statuses = tracks.map { it.status }
        val known = trackStatuses(trackServiceId)
        return copy(
            status =
                when {
                    known == null -> status
                    known.completed in statuses -> known.completed
                    known.reading in statuses -> known.reading
                    else -> status
                },
            lastChapterRead = tracks.maxOf { it.lastChapterRead },
            score =
                score.takeIf { it > 0f } ?: tracks.map { it.score }.firstOrNull { it > 0f } ?: 0f,
            startedReadingDate =
                startedReadingDate.takeIf { it > 0L }
                    ?: tracks.map { it.startedReadingDate }.firstOrNull { it > 0L }
                    ?: 0L,
            finishedReadingDate =
                finishedReadingDate.takeIf { it > 0L }
                    ?: tracks.map { it.finishedReadingDate }.firstOrNull { it > 0L }
                    ?: 0L,
        )
    }

    /**
     * Sorts records by the row they belong to, then by their own id. Records of rows outside
     * [order] are dropped.
     */
    private fun <T> List<T>.inRowOrder(
        order: Map<Long, Int>,
        mangaId: (T) -> Long,
        id: (T) -> Long,
    ): List<T> {
        val ofGroup = filter { mangaId(it) in order }
        return ofGroup.sortedWith(compareBy({ order.getValue(mangaId(it)) }, { id(it) }))
    }

    /**
     * The chapter that stays for one url: the keeper's, or else the first copy's. [copies] are the
     * other rows' chapters with that url; the keeper's own chapters never fold.
     */
    private class Survivor(val chapter: ChapterEntity, val copies: List<ChapterEntity>) {

        /**
         * The chapter on the keeper with restore's `combineProgress` over it and [copies], or null
         * when nothing changes.
         */
        fun update(keeperId: Long): ChapterEntity? {
            val updated =
                if (copies.isEmpty()) {
                    chapter.copy(mangaId = keeperId)
                } else {
                    val all = listOf(chapter) + copies
                    val isRead = all.any { it.read }
                    val furthest = all.maxBy { it.lastPageRead }
                    chapter.copy(
                        mangaId = keeperId,
                        read = isRead,
                        bookmark = all.any { it.bookmark },
                        lastPageRead = furthest.lastPageRead,
                        pagesLeft = if (isRead) 0 else furthest.pagesLeft,
                    )
                }
            return updated.takeIf { it != chapter }
        }

        /**
         * One history entry for the chapter with the latest read and the longest reading time, or
         * null when there is nothing to combine. Id 0 inserts a new entry.
         */
        fun history(entries: Map<Long, HistoryEntity>): HistoryEntity? {
            if (copies.isEmpty()) return null
            val own = entries[chapter.id]
            val all = (listOf(chapter) + copies).mapNotNull { entries[it.id] }
            if (all.isEmpty()) return null
            val combined =
                HistoryEntity(
                    id = own?.id ?: 0L,
                    chapterId = chapter.id,
                    lastRead = all.maxOf { it.lastRead },
                    timeRead = all.maxOf { it.timeRead },
                )
            return combined.takeIf { it != own }
        }
    }

    private class TrackFold(
        val upserts: List<TrackEntity>,
        val moves: Map<Long, Long>,
        val conflicts: List<Int>,
    )

    private sealed interface Folded {
        data class Kept(val keeper: MangaEntity) : Folded

        data class Skipped(val merged: MangaEntity, val reason: SkipReason.DownloadFolders) : Folded
    }
}
