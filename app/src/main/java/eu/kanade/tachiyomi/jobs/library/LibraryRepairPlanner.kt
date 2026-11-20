package eu.kanade.tachiyomi.jobs.library

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.MangaEntity

/**
 * Decides how the rows of one MangaDex title fold into one. It reads nothing and writes nothing:
 * [LibraryRepairer] hands it rows and applies the plan. [folderName] gives the download folder name
 * of a row, and a row's folder is found only by that name.
 */
class LibraryRepairPlanner(
    private val mangaDexSourceId: Long,
    private val isMangadex: (Long) -> Boolean,
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
            is Folded.Kept ->
                GroupPlan.Apply(GroupChanges(keeper = folded.keeper, deletedRows = rows.drop(1)))
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
        val donor = rows.firstOrNull { isMangadex(it.source) } ?: keeper
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

    private sealed interface Folded {
        data class Kept(val keeper: MangaEntity) : Folded

        data class Skipped(val merged: MangaEntity, val reason: SkipReason.DownloadFolders) : Folded
    }
}
