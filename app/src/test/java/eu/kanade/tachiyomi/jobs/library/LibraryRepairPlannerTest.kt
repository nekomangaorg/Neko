package eu.kanade.tachiyomi.jobs.library

import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.MangaEntity

class LibraryRepairPlannerTest {

    private val planner =
        LibraryRepairPlanner(MANGADEX, { it == MANGADEX }, { it.userTitle ?: it.title })

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

    private fun chapter(mangaId: Long, dateFetch: Long) =
        ChapterEntity(
            mangaId = mangaId,
            url = "/chapter/$dateFetch",
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
    }
}
