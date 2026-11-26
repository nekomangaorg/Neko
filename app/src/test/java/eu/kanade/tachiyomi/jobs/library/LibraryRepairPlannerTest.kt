package eu.kanade.tachiyomi.jobs.library

import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.nekomanga.data.database.entity.ArtworkEntity
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.entity.MangaAggregateEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.entity.MergeMangaEntity
import org.nekomanga.data.database.entity.TrackEntity

class LibraryRepairPlannerTest {

    private val planner =
        LibraryRepairPlanner(
            MANGADEX,
            { it == MANGADEX },
            ::trackStatuses,
            { it.userTitle ?: it.title },
        )

    @Test
    fun `given title and manga urls of one uuid when inventory then one group of two rows`() {
        val inventory =
            planner.inventory(listOf(manga(1, "/title/$U"), manga(2, "/manga/$U")), emptySet())

        inventory.work.single().rowIds shouldBe listOf(1L, 2L)
        inventory.duplicateTitles shouldBe 1
    }

    @Test
    fun `given uuids differing only in case when inventory then one group`() {
        val rows = listOf(manga(1, "/title/${U.uppercase()}"), manga(2, "/manga/$U"))

        planner.inventory(rows, emptySet()).work.single().rowIds shouldBe listOf(1L, 2L)
    }

    @Test
    fun `given a url ending in a slash when inventory then it folds with its uuid`() {
        val rows = listOf(manga(1, "/title/$U/"), manga(2, "/manga/$U"))

        planner.inventory(rows, emptySet()).work.single().rowIds shouldBe listOf(1L, 2L)
    }

    @Test
    fun `given two uuids with the same title when inventory then nothing to do`() {
        val rows =
            listOf(manga(1, "/title/$U", title = "Same"), manga(2, "/title/$U2", title = "Same"))

        planner.inventory(rows, emptySet()).hasWork shouldBe false
    }

    @Test
    fun `given an invalid url or source when inventory then it is skipped and left out`() {
        val rows =
            listOf(
                manga(1, "/chapter/$U", source = -1L, title = "A"),
                manga(2, "/title/$U2", source = 7L, title = "B"),
            )

        val inventory = planner.inventory(rows, emptySet())

        inventory.skipped shouldBe
            listOf(
                SkippedEntry("A", SkipReason.NotMangaDex),
                SkippedEntry("B", SkipReason.NotMangaDex),
            )
        inventory.hasWork shouldBe false
    }

    @Test
    fun `given a damaged single row when inventory then it counts as damaged`() {
        val inventory = planner.inventory(listOf(manga(1, source = -1L)), emptySet())

        inventory.damagedEntries shouldBe 1
        inventory.work.single().rowIds shouldBe listOf(1L)
    }

    @Test
    fun `given a healthy single row when planGroup then untouched`() {
        planner.planGroup(group(manga(1)), emptySet()) shouldBe GroupPlan.Untouched
    }

    @Test
    fun `given a damaged single row when planGroup then only source and date added change`() {
        val row = manga(1, source = -1L).copy(dateAdded = 0L, viewerFlags = -1)
        val data =
            group(
                row,
                chapters = listOf(chapter(1, dateFetch = 500L), chapter(1, dateFetch = 300L)),
            )

        val changes = (planner.planGroup(data, emptySet()) as GroupPlan.Apply).changes

        changes.keeper shouldBe row.copy(source = MANGADEX, dateAdded = 300L)
        changes.deletedRows shouldBe emptyList()
    }

    @Test
    fun `given dates 0 and 1000 and a chapter fetched at 400 when planGroup then date added is 400`() {
        val data =
            group(
                manga(1).copy(dateAdded = 0L),
                manga(2, "/manga/$U").copy(dateAdded = 1000L),
                chapters = listOf(chapter(2, dateFetch = 400L)),
            )

        keeperOf(data).dateAdded shouldBe 400L
    }

    @Test
    fun `given dates 2000 and 1000 when planGroup then date added is 1000 and chapter dates are ignored`() {
        val data =
            group(
                manga(1).copy(dateAdded = 2000L),
                manga(2, "/manga/$U").copy(dateAdded = 1000L),
                chapters = listOf(chapter(1, dateFetch = 400L)),
            )

        keeperOf(data).dateAdded shouldBe 1000L
    }

    @Test
    fun `given a damaged keeper and a healthy copy when planGroup then the keeper id stays with MangaDex details`() {
        val keeperRow = manga(1, source = -1L, title = "Old").copy(description = null)
        val copy = manga(2, "/manga/$U", title = "New").copy(description = "d")

        val changes =
            (planner.planGroup(group(keeperRow, copy), emptySet()) as GroupPlan.Apply).changes

        changes.keeper.id shouldBe 1L
        changes.keeper.source shouldBe MANGADEX
        changes.keeper.title shouldBe "New"
        changes.keeper.description shouldBe "d"
        changes.deletedRows.map { it.id } shouldBe listOf(2L)
    }

    @Test
    fun `given user settings split over rows when planGroup then each comes from the first row that has it`() {
        val first =
            manga(1, source = -1L)
                .copy(
                    userTitle = null,
                    viewerFlags = -1,
                    chapterFlags = 0,
                    followStatus = FollowStatus.UNFOLLOWED,
                    filteredLanguage = " ",
                    favorite = false,
                    lastUpdate = 10L,
                    nextUpdate = 7L,
                )
        val second =
            manga(2, "/manga/$U")
                .copy(
                    userTitle = "Mine",
                    viewerFlags = 0,
                    chapterFlags = 5,
                    followStatus = FollowStatus.READING,
                    filteredLanguage = "en",
                    favorite = true,
                    lastUpdate = 20L,
                    nextUpdate = 9L,
                )
        val third = manga(3, "/title/$U/").copy(viewerFlags = 2)

        val keeper = keeperOf(group(first, second, third))

        keeper.userTitle shouldBe "Mine"
        keeper.viewerFlags shouldBe 2
        keeper.chapterFlags shouldBe 5
        keeper.followStatus shouldBe FollowStatus.READING
        keeper.filteredLanguage shouldBe "en"
        keeper.favorite shouldBe true
        keeper.lastUpdate shouldBe 20L
        keeper.nextUpdate shouldBe 7L
    }

    @Test
    fun `given only the final folder exists when planGroup then it applies with the merged title`() {
        val data =
            group(manga(1, source = -1L, title = "Old"), manga(2, "/manga/$U", title = "New"))

        keeperOf(data, folders = setOf("New")).title shouldBe "New"
    }

    @Test
    fun `given only the copy's folder exists when planGroup then the keeper keeps the copy's title and user title`() {
        val keeperRow = manga(1, title = "New").copy(userTitle = "Ours")
        val copy = manga(2, "/manga/$U", title = "Old").copy(userTitle = "Mine")

        val keeper = keeperOf(group(keeperRow, copy), folders = setOf("Mine"))

        keeper.title shouldBe "Old"
        keeper.userTitle shouldBe "Mine"
    }

    @Test
    fun `given folders of two rows exist when planGroup then it skips with both names`() {
        val data =
            group(manga(1, source = -1L, title = "Old"), manga(2, "/manga/$U", title = "New"))

        planner.planGroup(data, setOf("Old", "New")) shouldBe
            GroupPlan.Skip(SkipReason.DownloadFolders(listOf("New", "Old")))
    }

    @Test
    fun `given read and bookmark only on the copy when planGroup then the keeper chapter gets them`() {
        val ch10 = chapter(1, id = 10L, url = "/chapter/a").copy(lastPageRead = 3, pagesLeft = 7)
        val ch20 =
            chapter(2, id = 20L, url = "/chapter/a")
                .copy(read = true, bookmark = true, lastPageRead = 9, pagesLeft = 1)

        changes(chapters = listOf(ch10, ch20)).chapterUpdates shouldBe
            listOf(ch10.copy(read = true, bookmark = true, lastPageRead = 9, pagesLeft = 0))
    }

    @Test
    fun `given a chapter only on the copy when planGroup then it moves to the keeper`() {
        val ch21 = chapter(2, id = 21L, url = "/chapter/b")

        changes(chapters = listOf(ch21)).chapterUpdates shouldBe listOf(ch21.copy(mangaId = 1L))
    }

    @Test
    fun `given one chapter url on two copies and not the keeper when planGroup then the lower row's chapter moves`() {
        val rows = listOf(manga(1, source = -1L), manga(2, "/manga/$U"), manga(3, "/title/$U/"))
        val ch21 = chapter(2, id = 21L, url = "/chapter/c").copy(pagesLeft = 5)
        val ch31 = chapter(3, id = 31L, url = "/chapter/c").copy(read = true)

        changes(rows = rows, chapters = listOf(ch31, ch21)).chapterUpdates shouldBe
            listOf(ch21.copy(mangaId = 1L, read = true, pagesLeft = 0))
    }

    @Test
    fun `given equal progress on both copies when planGroup then no chapter update`() {
        val ch10 = chapter(1, id = 10L, url = "/chapter/a").copy(read = true, lastPageRead = 4)
        val ch20 = chapter(2, id = 20L, url = "/chapter/a").copy(read = true, lastPageRead = 4)

        changes(chapters = listOf(ch10, ch20)).chapterUpdates shouldBe emptyList()
    }

    @Test
    fun `given history on both copies of a chapter when planGroup then one entry with the latest read and longest time`() {
        val history =
            listOf(
                HistoryEntity(id = 100L, chapterId = 10L, lastRead = 1000L, timeRead = 50L),
                HistoryEntity(id = 200L, chapterId = 20L, lastRead = 2000L, timeRead = 30L),
            )

        changes(chapters = sharedChapter(), history = history).historyUpserts shouldBe
            listOf(HistoryEntity(id = 100L, chapterId = 10L, lastRead = 2000L, timeRead = 50L))
    }

    @Test
    fun `given history only on the copy's chapter when planGroup then it moves to the surviving chapter`() {
        val history =
            listOf(HistoryEntity(id = 200L, chapterId = 20L, lastRead = 2000L, timeRead = 30L))

        changes(chapters = sharedChapter(), history = history).historyUpserts shouldBe
            listOf(HistoryEntity(id = 0L, chapterId = 10L, lastRead = 2000L, timeRead = 30L))
    }

    @Test
    fun `given tracks on both rows for one service when planGroup then one track with the furthest chapter and a conflict`() {
        val t7 = track(7L, mangaId = 1L, mediaId = 11L, lastChapterRead = 5f)
        val t8 = track(8L, mangaId = 2L, mediaId = 12L, lastChapterRead = 9f, score = 7f)

        val changes = changes(tracks = listOf(t7, t8))

        changes.trackUpserts shouldBe listOf(t8.copy(id = 7L, mangaId = 1L, lastChapterRead = 9f))
        changes.trackIdMoves shouldBe mapOf(8L to 7L)
        changes.trackConflictServices shouldBe listOf(1)
    }

    @Test
    fun `given a track only on the copy when planGroup then it moves to the keeper`() {
        val t8 = track(8L, mangaId = 2L, mediaId = 12L, lastChapterRead = 9f)

        val changes = changes(tracks = listOf(t8))

        changes.trackUpserts shouldBe listOf(t8.copy(mangaId = 1L))
        changes.trackIdMoves shouldBe emptyMap()
    }

    @Test
    fun `given a completed MDList track on the keeper and a reading one on the copy when planGroup then the combined track is completed`() {
        val completed = FollowStatus.COMPLETED.int
        val t7 = mdListTrack(7L, mangaId = 1L, lastChapterRead = 5f, status = completed)
        val t8 = mdListTrack(8L, mangaId = 2L, lastChapterRead = 0f, FollowStatus.READING.int)

        changes(tracks = listOf(t7, t8)).trackUpserts shouldBe
            listOf(t8.copy(id = 7L, mangaId = 1L, lastChapterRead = 5f, status = completed))
    }

    @Test
    fun `given a reading MDList track on the keeper and a planned one on the copy when planGroup then the combined track is reading`() {
        val reading = FollowStatus.READING.int
        val t7 = mdListTrack(7L, mangaId = 1L, lastChapterRead = 5f, status = reading)
        val t8 = mdListTrack(8L, mangaId = 2L, lastChapterRead = 0f, FollowStatus.PLAN_TO_READ.int)

        changes(tracks = listOf(t7, t8)).trackUpserts shouldBe
            listOf(t8.copy(id = 7L, mangaId = 1L, lastChapterRead = 5f, status = reading))
    }

    @Test
    fun `given a MangaUpdates reading list track on the keeper and a wish list one on the copy when planGroup then the combined track is on the reading list`() {
        val service = TrackManager.MANGA_UPDATES
        val t7 = track(7L, 1L, 11L, 5f, service = service, status = MangaUpdates.READING_LIST)
        val t8 = track(8L, 2L, 11L, 0f, service = service, status = MangaUpdates.WISH_LIST)

        changes(tracks = listOf(t7, t8)).trackUpserts shouldBe
            listOf(
                t8.copy(
                    id = 7L,
                    mangaId = 1L,
                    lastChapterRead = 5f,
                    status = MangaUpdates.READING_LIST,
                ),
            )
    }

    @Test
    fun `given MDList tracks neither completed nor reading when planGroup then the combined track has the copy's status`() {
        val t7 = mdListTrack(7L, mangaId = 1L, lastChapterRead = 5f, FollowStatus.ON_HOLD.int)
        val t8 = mdListTrack(8L, mangaId = 2L, lastChapterRead = 0f, FollowStatus.PLAN_TO_READ.int)

        changes(tracks = listOf(t7, t8)).trackUpserts shouldBe
            listOf(t8.copy(id = 7L, mangaId = 1L, lastChapterRead = 5f))
    }

    @Test
    fun `given tracks of a service without known statuses when planGroup then the combined track has the copy's status`() {
        val t7 = track(7L, mangaId = 1L, mediaId = 11L, lastChapterRead = 5f, status = 2)
        val t8 = track(8L, mangaId = 2L, mediaId = 11L, lastChapterRead = 0f, status = 1)

        changes(tracks = listOf(t7, t8)).trackUpserts shouldBe
            listOf(t8.copy(id = 7L, mangaId = 1L, lastChapterRead = 5f))
    }

    @Test
    fun `given two Komga merges with different urls when planGroup then both stay`() {
        val keeperMerge = merge(3L, mangaId = 1L, url = "a", type = MergeType.Komga)
        val copyMerge = merge(4L, mangaId = 2L, url = "b", type = MergeType.Komga)

        changes(merges = listOf(keeperMerge, copyMerge)).mergeUpserts shouldBe
            listOf(copyMerge.copy(mangaId = 1L))
    }

    @Test
    fun `given one MangaBall merge on each row when planGroup then only the keeper's stays`() {
        val merges =
            listOf(
                merge(3L, mangaId = 1L, url = "a", type = MergeType.MangaBall),
                merge(4L, mangaId = 2L, url = "b", type = MergeType.MangaBall),
            )

        changes(merges = merges).mergeUpserts shouldBe emptyList()
    }

    @Test
    fun `given a category only on the copy when planGroup then the keeper gets it`() {
        val categories = listOf(MangaCategoryEntity(id = 40L, mangaId = 2L, categoryId = 5))

        changes(categories = categories).categoryInserts shouldBe
            listOf(MangaCategoryEntity(mangaId = 1L, categoryId = 5))
    }

    @Test
    fun `given the same category on both rows when planGroup then no category insert`() {
        val categories =
            listOf(
                MangaCategoryEntity(id = 40L, mangaId = 1L, categoryId = 5),
                MangaCategoryEntity(id = 41L, mangaId = 2L, categoryId = 5),
            )

        changes(categories = categories).categoryInserts shouldBe emptyList()
    }

    @Test
    fun `given artwork only on the copy when planGroup then it moves`() {
        val art = artwork(51L, mangaId = 2L)

        changes(artwork = listOf(art)).artworkUpserts shouldBe listOf(art.copy(mangaId = 1L))
    }

    @Test
    fun `given artwork on both rows when planGroup then nothing moves`() {
        val artwork = listOf(artwork(50L, mangaId = 1L), artwork(51L, mangaId = 2L))

        changes(artwork = artwork).artworkUpserts shouldBe emptyList()
    }

    @Test
    fun `given an aggregate only on the copy when planGroup then the keeper gets it`() {
        val agg2 = MangaAggregateEntity(mangaId = 2L, volumes = "{}")

        changes(aggregates = listOf(agg2)).aggregateInsert shouldBe agg2.copy(mangaId = 1L)
    }

    @Test
    fun `given a score and reading dates only on the keeper's track when planGroup then the combined track keeps them`() {
        val t7 =
            track(7L, mangaId = 1L, mediaId = 11L, lastChapterRead = 5f, score = 6f)
                .copy(startedReadingDate = 100L, finishedReadingDate = 200L)
        val t8 = track(8L, mangaId = 2L, mediaId = 11L, lastChapterRead = 9f)

        changes(tracks = listOf(t7, t8)).trackUpserts shouldBe
            listOf(
                t8.copy(
                    id = 7L,
                    mangaId = 1L,
                    score = 6f,
                    startedReadingDate = 100L,
                    finishedReadingDate = 200L,
                ),
            )
    }

    @Test
    fun `given an aggregate on both rows when planGroup then no aggregate insert`() {
        val aggregates =
            listOf(
                MangaAggregateEntity(mangaId = 1L, volumes = "a"),
                MangaAggregateEntity(mangaId = 2L, volumes = "b"),
            )

        changes(aggregates = aggregates).aggregateInsert shouldBe null
    }

    @Test
    fun `given two keeper chapters with one url when planGroup then neither changes`() {
        val ch10 = chapter(1, id = 10L, url = "/chapter/a")
        val ch11 = chapter(1, id = 11L, url = "/chapter/a").copy(read = true)

        changes(chapters = listOf(ch10, ch11)).chapterUpdates shouldBe emptyList()
    }

    /** Planned changes for the damaged keeper (row 1) and its healthy copy (row 2). */
    private fun changes(
        rows: List<MangaEntity> = listOf(manga(1, source = -1L), manga(2, "/manga/$U")),
        chapters: List<ChapterEntity> = emptyList(),
        history: List<HistoryEntity> = emptyList(),
        tracks: List<TrackEntity> = emptyList(),
        merges: List<MergeMangaEntity> = emptyList(),
        categories: List<MangaCategoryEntity> = emptyList(),
        artwork: List<ArtworkEntity> = emptyList(),
        aggregates: List<MangaAggregateEntity> = emptyList(),
    ): GroupChanges {
        val data =
            RepairGroupData(
                rows,
                chapters,
                history,
                tracks,
                merges,
                categories,
                artwork,
                aggregates,
            )
        return (planner.planGroup(data, emptySet()) as GroupPlan.Apply).changes
    }

    /** Chapter "/chapter/a" as id 10 on the keeper and id 20 on the copy. */
    private fun sharedChapter() =
        listOf(chapter(1, id = 10L, url = "/chapter/a"), chapter(2, id = 20L, url = "/chapter/a"))

    private fun track(
        id: Long,
        mangaId: Long,
        mediaId: Long,
        lastChapterRead: Float,
        score: Float = 0f,
        service: Int = TrackManager.MYANIMELIST,
        status: Int = 0,
    ) =
        TrackEntity(
            id = id,
            mangaId = mangaId,
            trackServiceId = service,
            mediaId = mediaId,
            libraryId = null,
            title = "Track $id",
            lastChapterRead = lastChapterRead,
            totalChapters = 0,
            status = status,
            score = score,
            trackingUrl = "",
            startedReadingDate = 0L,
            finishedReadingDate = 0L,
        )

    private fun mdListTrack(id: Long, mangaId: Long, lastChapterRead: Float, status: Int) =
        track(id, mangaId, 11L, lastChapterRead, service = TrackManager.MDLIST, status = status)

    private fun merge(id: Long, mangaId: Long, url: String, type: MergeType) =
        MergeMangaEntity(
            id = id,
            mangaId = mangaId,
            coverUrl = "",
            title = "Merge $id",
            url = url,
            mergeType = type,
        )

    private fun artwork(id: Long, mangaId: Long) =
        ArtworkEntity(
            id = id,
            mangaId = mangaId,
            fileName = "$id.jpg",
            volume = "1",
            locale = "en",
            description = "",
        )

    private fun keeperOf(data: RepairGroupData, folders: Set<String> = emptySet()): MangaEntity =
        (planner.planGroup(data, folders) as GroupPlan.Apply).changes.keeper

    private fun group(vararg rows: MangaEntity, chapters: List<ChapterEntity> = emptyList()) =
        RepairGroupData(
            rows = rows.toList(),
            chapters = chapters,
            history = emptyList(),
            tracks = emptyList(),
            merges = emptyList(),
            categories = emptyList(),
            artwork = emptyList(),
            aggregates = emptyList(),
        )

    private fun manga(
        id: Long,
        url: String = "/title/$U",
        source: Long = MANGADEX,
        title: String = "Title",
    ) =
        MangaEntity(
            id = id,
            source = source,
            url = url,
            artist = null,
            author = null,
            description = null,
            genre = null,
            title = title,
            status = 0,
            thumbnailUrl = null,
            favorite = true,
            lastUpdate = null,
            nextUpdate = null,
            dateAdded = 1000L,
            initialized = true,
            viewerFlags = 0,
            chapterFlags = 0,
            langFlag = null,
            followStatus = FollowStatus.UNFOLLOWED,
            anilistId = null,
            kitsuId = null,
            myAnimeListId = null,
            mangaUpdatesId = null,
            animePlanetId = null,
            otherUrls = null,
            filteredScanlators = null,
            missingChapters = null,
            rating = null,
            users = null,
            threadId = null,
            repliesCount = null,
            mergeMangaUrl = null,
            lastVolumeNumber = null,
            lastChapterNumber = null,
            mergeMangaImageUrl = null,
            altTitles = null,
            userCover = null,
            userTitle = null,
            filteredLanguage = null,
            dynamicCover = null,
        )

    private fun chapter(
        mangaId: Long,
        dateFetch: Long = 0L,
        id: Long = 0L,
        url: String = "/chapter/$dateFetch",
    ) =
        ChapterEntity(
            id = id,
            mangaId = mangaId,
            url = url,
            name = "Ch. 1",
            chapterTxt = "Ch. 1",
            chapterTitle = "",
            vol = "",
            scanlator = null,
            uploader = null,
            isUnavailable = false,
            read = false,
            bookmark = false,
            lastPageRead = 0,
            pagesLeft = 0,
            chapterNumber = 1f,
            sourceOrder = 0,
            smartOrder = 0,
            dateFetch = dateFetch,
            dateUpload = 0L,
            mangadexChapterId = null,
            language = null,
        )

    private companion object {
        const val MANGADEX = 2499283573021220255L
        const val U = "f9c33607-9180-4ba6-b85c-e4b5faee7192"
        const val U2 = "a1b2c3d4-0000-4abc-8def-1234567890ab"

        /** The completed and reading values of MDList and MangaUpdates, as their services give. */
        fun trackStatuses(service: Int): TrackStatuses? =
            when (service) {
                TrackManager.MDLIST ->
                    TrackStatuses(FollowStatus.COMPLETED.int, FollowStatus.READING.int)
                TrackManager.MANGA_UPDATES ->
                    TrackStatuses(MangaUpdates.COMPLETE_LIST, MangaUpdates.READING_LIST)
                else -> null
            }
    }
}
