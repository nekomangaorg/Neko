package eu.kanade.tachiyomi.jobs.library

import androidx.room.withTransaction
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verifyOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.dao.ArtworkDao
import org.nekomanga.data.database.dao.ChapterDao
import org.nekomanga.data.database.dao.HistoryDao
import org.nekomanga.data.database.dao.MangaAggregateDao
import org.nekomanga.data.database.dao.MangaCategoryDao
import org.nekomanga.data.database.dao.MangaDao
import org.nekomanga.data.database.dao.MergeMangaDao
import org.nekomanga.data.database.dao.TrackDao
import org.nekomanga.data.database.entity.ArtworkEntity
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.entity.MangaAggregateEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.entity.MergeMangaEntity
import org.nekomanga.data.database.entity.TrackEntity
import org.nekomanga.domain.track.store.DelayedTrackingStore

class LibraryRepairerTest {

    // The tables. Each DAO mock answers from them the way its queries would.
    private val manga = mutableListOf<MangaEntity>()
    private val chapters = mutableListOf<ChapterEntity>()
    private val history = mutableListOf<HistoryEntity>()
    private val tracks = mutableListOf<TrackEntity>()
    private val merges = mutableListOf<MergeMangaEntity>()
    private val categories = mutableListOf<MangaCategoryEntity>()
    private val artwork = mutableListOf<ArtworkEntity>()
    private val aggregates = mutableListOf<MangaAggregateEntity>()
    private var nextId = 1000L
    private var folders = emptySet<String>()
    private var inTransaction = false
    private val readsInTransaction = mutableListOf<Boolean>()
    private var cacheRefreshes = 0

    private val appDatabase = mockk<AppDatabase>()

    private val mangaDao: MangaDao = mockk {
        coEvery { getMangaList() } answers
            {
                readsInTransaction += inTransaction
                manga.toList()
            }
        coEvery { getMangaByIds(any()) } answers
            {
                manga.filter { it.id in firstArg<List<Long>>() }
            }
        coEvery { updateManga(any()) } answers
            {
                val row = firstArg<MangaEntity>()
                manga[manga.indexOfFirst { it.id == row.id }] = row
            }
        // The foreign keys cascade from manga to its records and from chapters to history
        coEvery { deleteMangaList(any()) } answers
            {
                val ids = firstArg<List<MangaEntity>>().map { it.id }.toSet()
                val chapterIds = chapters.filter { it.mangaId in ids }.map { it.id }.toSet()
                manga.removeAll { it.id in ids }
                chapters.removeAll { it.mangaId in ids }
                history.removeAll { it.chapterId in chapterIds }
                tracks.removeAll { it.mangaId in ids }
                merges.removeAll { it.mangaId in ids }
                categories.removeAll { it.mangaId in ids }
                artwork.removeAll { it.mangaId in ids }
                aggregates.removeAll { it.mangaId in ids }
            }
    }

    private val chapterDao: ChapterDao = mockk {
        coEvery { getChaptersForMangaIds(any()) } answers
            {
                chapters.filter { it.mangaId in firstArg<List<Long>>() }
            }
        coEvery { updateChapters(any()) } answers
            {
                firstArg<List<ChapterEntity>>().forEach { chapter ->
                    chapters[chapters.indexOfFirst { it.id == chapter.id }] = chapter
                }
            }
    }

    private val historyDao: HistoryDao = mockk {
        coEvery { getHistoryByMangaIds(any()) } answers
            {
                val ids = firstArg<List<Long>>()
                val chapterIds = chapters.filter { it.mangaId in ids }.map { it.id }.toSet()
                history.filter { it.chapterId in chapterIds }
            }
        // upsertHistory updates the entry of the chapter or inserts one
        coEvery { upsertHistoryList(any()) } answers
            {
                firstArg<List<HistoryEntity>>().forEach { entry ->
                    val index = history.indexOfFirst { it.chapterId == entry.chapterId }
                    if (index >= 0) {
                        history[index] =
                            history[index].copy(
                                lastRead = entry.lastRead,
                                timeRead = entry.timeRead,
                            )
                    } else {
                        history += entry.copy(id = entry.id.takeIf { it != 0L } ?: nextId++)
                    }
                }
            }
    }

    // The inserts replace rows on the primary key or on (manga_id, track_service_id)
    private val trackDao: TrackDao = mockk {
        coEvery { getTracksForMangaByIds(any()) } answers
            {
                tracks.filter { it.mangaId in firstArg<List<Long>>() }
            }
        coEvery { insertTracks(any()) } answers
            {
                firstArg<List<TrackEntity>>().forEach { track ->
                    tracks.removeAll {
                        it.id == track.id ||
                            (it.mangaId == track.mangaId &&
                                it.trackServiceId == track.trackServiceId)
                    }
                    tracks += track
                }
            }
    }

    private val mergeMangaDao: MergeMangaDao = mockk {
        coEvery { getMergeMangaList(any<List<Long>>()) } answers
            {
                merges.filter { it.mangaId in firstArg<List<Long>>() }
            }
        coEvery { insertMergeManga(any<List<MergeMangaEntity>>()) } answers
            {
                firstArg<List<MergeMangaEntity>>().forEach { merge ->
                    merges.removeAll { it.id == merge.id }
                    merges += merge
                }
            }
    }

    private val mangaCategoryDao: MangaCategoryDao = mockk {
        coEvery { getMangaCategories(any()) } answers
            {
                categories.filter { it.mangaId in firstArg<List<Long>>() }
            }
        coEvery { insertMangaListCategories(any()) } answers
            {
                firstArg<List<MangaCategoryEntity>>().forEach {
                    categories += it.copy(id = nextId++)
                }
            }
    }

    private val artworkDao: ArtworkDao = mockk {
        coEvery { getArtworkForMangaIds(any()) } answers
            {
                artwork.filter { it.mangaId in firstArg<List<Long>>() }
            }
        coEvery { insertArtworks(any()) } answers
            {
                firstArg<List<ArtworkEntity>>().forEach { entry ->
                    artwork.removeAll { it.id == entry.id }
                    artwork += entry
                }
            }
    }

    private val mangaAggregateDao: MangaAggregateDao = mockk {
        coEvery { getMangaAggregateSync(any()) } answers
            {
                aggregates.firstOrNull { it.mangaId == firstArg<Long>() }
            }
        coEvery { insertMangaAggregate(any()) } answers
            {
                val aggregate = firstArg<MangaAggregateEntity>()
                aggregates.removeAll { it.mangaId == aggregate.mangaId }
                aggregates += aggregate
            }
    }

    private val store: DelayedTrackingStore = mockk(relaxed = true)

    private val gate: LibraryRepairGate = mockk {
        every { isRunning } returns false
        coEvery { blockingWork() } returns emptyList()
    }

    private val repairer =
        LibraryRepairer(
            appDatabase = appDatabase,
            mangaDao = mangaDao,
            chapterDao = chapterDao,
            historyDao = historyDao,
            trackDao = trackDao,
            mergeMangaDao = mergeMangaDao,
            mangaCategoryDao = mangaCategoryDao,
            artworkDao = artworkDao,
            mangaAggregateDao = mangaAggregateDao,
            planner =
                LibraryRepairPlanner(MANGADEX, { it == MANGADEX }, { it.userTitle ?: it.title }),
            folderNames = { folders },
            delayedTrackingStore = store,
            refreshDownloadCache = { cacheRefreshes++ },
        )

    @Before
    fun setup() {
        every { store.getItems() } returns emptyList()
        // A transaction runs the block, and an exception puts every table back the way it was
        mockkStatic("androidx.room.RoomDatabaseKt")
        val transaction = slot<suspend () -> Any?>()
        coEvery { appDatabase.withTransaction(capture(transaction)) } coAnswers
            {
                val snapshot = snapshot()
                inTransaction = true
                try {
                    transaction.captured.invoke()
                } catch (e: Throwable) {
                    restore(snapshot)
                    throw e
                } finally {
                    inTransaction = false
                }
            }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a doubled title when repair then one row keeps every chapter, history entry, track, merge and category`() =
        runTest {
            seedDoubledTitle()
            val result = RepairResult()

            repairer.repair(result) { _, _ -> }

            manga.map { it.id to it.source } shouldBe listOf(1L to MANGADEX)
            chapters.sortedBy { it.id }.map { Triple(it.id, it.mangaId, it.read) } shouldBe
                listOf(Triple(10L, 1L, true), Triple(21L, 1L, false))
            history.sortedBy { it.chapterId }.map { it.chapterId to it.lastRead } shouldBe
                listOf(10L to 2000L, 21L to 3000L)
            tracks.map { it.mangaId to it.trackServiceId } shouldBe listOf(1L to 1)
            merges.map { it.mangaId to it.url } shouldBe listOf(1L to "b")
            categories.map { it.mangaId to it.categoryId }.sortedBy { it.second } shouldBe
                listOf(1L to 3, 1L to 5)
            artwork.map { it.id to it.mangaId } shouldBe listOf(60L to 1L)
            aggregates shouldBe listOf(MangaAggregateEntity(mangaId = 1L, volumes = "{}"))
            result.merged shouldBe 1
            result.failed shouldBe emptyList()
        }

    @Test
    fun `given a healthy library when repair then nothing is written`() = runTest {
        manga += manga(1, "/title/$U")
        manga += manga(2, "/title/$U2")

        repairer.repair(RepairResult()) { _, _ -> }

        verifyNothingWritten()
        cacheRefreshes shouldBe 0
    }

    @Test
    fun `given a repaired library when repair runs again then nothing is written`() = runTest {
        seedDoubledTitle()
        repairer.repair(RepairResult()) { _, _ -> }
        clearMocks(
            mangaDao,
            chapterDao,
            historyDao,
            trackDao,
            mergeMangaDao,
            mangaCategoryDao,
            artworkDao,
            mangaAggregateDao,
            answers = false,
            recordedCalls = true,
        )
        cacheRefreshes = 0
        val second = RepairResult()

        repairer.repair(second) { _, _ -> }

        verifyNothingWritten()
        cacheRefreshes shouldBe 0
        second.merged shouldBe 0
        second.fixed shouldBe 0
    }

    @Test
    fun `given a failing group when repair then it rolls back and the next group is repaired`() =
        runTest {
            seedTwoDoubledTitles()
            val before = manga.filter { it.id in listOf(1L, 2L) }
            coEvery {
                chapterDao.updateChapters(match { list -> list.any { it.id == 21L } })
            } throws IllegalStateException("disk full")
            val result = RepairResult()

            repairer.repair(result) { _, _ -> }

            manga.filter { it.id in listOf(1L, 2L) } shouldBe before
            result.failed shouldBe listOf("Real: disk full")
            manga.filter { it.id in listOf(3L, 4L) }.map { it.id } shouldBe listOf(3L)
            result.merged shouldBe 1
        }

    @Test
    fun `given cancellation after the first group when repair then the first group stays repaired and the second is untouched`() =
        runTest {
            seedTwoDoubledTitles()
            val result = RepairResult()
            lateinit var job: Job

            job = launch { repairer.repair(result) { done, _ -> if (done == 1) job.cancel() } }
            job.join()

            manga.map { it.id } shouldBe listOf(1L, 3L, 4L)
            result.merged shouldBe 1
        }

    @Test
    fun `given cancellation inside a group's transaction when repair then the group rolls back and repair stops`() =
        runTest {
            seedTwoDoubledTitles()
            val before = manga.toList()
            val result = RepairResult()
            lateinit var job: Job
            coEvery {
                chapterDao.updateChapters(match { list -> list.any { it.id == 21L } })
            } answers
                {
                    job.cancel()
                    throw CancellationException("stopped")
                }

            job = launch { repairer.repair(result) { _, _ -> } }
            job.join()

            manga shouldBe before
            result.failed shouldBe emptyList()
        }

    @Test
    fun `given a row that is not a MangaDex title when repair then it is listed as skipped`() =
        runTest {
            manga += manga(5, "/title/$U2", source = 123L, title = "Other")
            val result = RepairResult()

            repairer.repair(result) { _, _ -> }

            result.skipped shouldBe listOf(SkippedEntry("Other", SkipReason.NotMangaDex))
            verifyNothingWritten()
        }

    @Test
    fun `given a title renamed into a second download folder during the run when repair then it is skipped`() =
        runTest {
            seedTwoDoubledTitles()
            folders = setOf("X", "Real two")
            val result = RepairResult()

            repairer.repair(result) { done, _ ->
                if (done == 1) {
                    val index = manga.indexOfFirst { it.id == 3L }
                    manga[index] = manga[index].copy(userTitle = "X")
                }
            }

            result.skipped shouldBe
                listOf(
                    SkippedEntry("Real two", SkipReason.DownloadFolders(listOf("X", "Real two"))),
                )
            manga.map { it.id } shouldBe listOf(1L, 3L, 4L)
            result.merged shouldBe 1
        }

    @Test
    fun `given a doubled title with no records when repair then only the rows are written`() =
        runTest {
            manga += manga(1, "/title/$U", source = -1L)
            manga += manga(2, "/manga/$U", title = "Real")

            repairer.repair(RepairResult()) { _, _ -> }

            manga.map { it.id } shouldBe listOf(1L)
            coVerify(exactly = 0) {
                chapterDao.updateChapters(any())
                historyDao.upsertHistoryList(any())
                trackDao.insertTracks(any())
                mergeMangaDao.insertMergeManga(any<List<MergeMangaEntity>>())
                mangaCategoryDao.insertMangaListCategories(any())
                artworkDao.insertArtworks(any())
                mangaAggregateDao.insertMangaAggregate(any())
            }
        }

    @Test
    fun `given a delayed tracking item on the copy's track when repair then it moves to the keeper's track`() =
        runTest {
            manga += manga(1, "/title/$U", source = -1L)
            manga += manga(2, "/manga/$U", title = "Real")
            tracks += track(7L, mangaId = 1L, mediaId = 11L)
            tracks += track(8L, mangaId = 2L, mediaId = 11L)
            every { store.getItems() } returns
                listOf(DelayedTrackingStore.DelayedTrackingItem(trackId = 8L, lastChapterRead = 9f))

            repairer.repair(RepairResult()) { _, _ -> }

            verifyOrder {
                store.add(7L, 9f)
                store.remove(8L)
            }
        }

    @Test
    fun `given a group was repaired when repair ends then the download cache is refreshed once`() =
        runTest {
            seedTwoDoubledTitles()

            repairer.repair(RepairResult()) { _, _ -> }

            cacheRefreshes shouldBe 1
        }

    @Test
    fun `given tracks of one service with different media ids when repair then the conflict is in the result`() =
        runTest {
            manga += manga(1, "/title/$U", source = -1L)
            manga += manga(2, "/manga/$U", title = "Real")
            tracks += track(7L, mangaId = 1L, mediaId = 11L)
            tracks += track(8L, mangaId = 2L, mediaId = 12L)
            val result = RepairResult()

            repairer.repair(result) { _, _ -> }

            result.trackingConflicts shouldBe listOf(TrackingConflict("Real", serviceId = 1))
        }

    @Test
    fun `given the library is read when inventory then the manga rows are read inside a transaction`() =
        runTest {
            manga += manga(1, "/title/$U", source = -1L)

            repairer.inventory()

            readsInTransaction shouldBe listOf(true)
        }

    @Test
    fun `given the gate is held when check then Running`() = runTest {
        every { gate.isRunning } returns true

        repairer.check(gate) shouldBe RepairCheck.Running

        coVerify(exactly = 0) { mangaDao.getMangaList() }
    }

    @Test
    fun `given a running restore when check then Blocked with RESTORE`() = runTest {
        coEvery { gate.blockingWork() } returns listOf(BlockingWork.RESTORE)

        repairer.check(gate) shouldBe RepairCheck.Blocked(listOf(BlockingWork.RESTORE))

        coVerify(exactly = 0) { mangaDao.getMangaList() }
    }

    @Test
    fun `given a healthy library when check then NothingToRepair`() = runTest {
        manga += manga(1, "/title/$U")

        repairer.check(gate) shouldBe RepairCheck.NothingToRepair
    }

    @Test
    fun `given a doubled title when check then Confirm with one duplicate title`() = runTest {
        seedDoubledTitle()

        (repairer.check(gate) as RepairCheck.Confirm).inventory.duplicateTitles shouldBe 1
    }

    @Test
    fun `given only a row that is not a MangaDex title when check then Confirm lists it as skipped`() =
        runTest {
            manga += manga(5, "/title/$U2", source = 123L, title = "Other")

            (repairer.check(gate) as RepairCheck.Confirm).inventory.skipped shouldBe
                listOf(SkippedEntry("Other", SkipReason.NotMangaDex))
        }

    private fun verifyNothingWritten() {
        coVerify(exactly = 0) {
            mangaDao.updateManga(any())
            mangaDao.deleteMangaList(any())
            chapterDao.updateChapters(any())
            historyDao.upsertHistoryList(any())
            trackDao.insertTracks(any())
            mergeMangaDao.insertMergeManga(any<List<MergeMangaEntity>>())
            mangaCategoryDao.insertMangaListCategories(any())
            artworkDao.insertArtworks(any())
            mangaAggregateDao.insertMangaAggregate(any())
        }
    }

    /**
     * Row 1 lost its source; row 2 is the copy Pull inserted. Chapter "/chapter/a" is on both, read
     * on the copy, and "/chapter/b" only on the copy. The copy also has artwork and an aggregate.
     */
    private fun seedDoubledTitle() {
        manga += manga(1, "/title/$U", source = -1L, title = "Old")
        manga += manga(2, "/manga/$U", title = "Real")
        chapters += chapter(10L, mangaId = 1L, url = "/chapter/a")
        chapters += chapter(20L, mangaId = 2L, url = "/chapter/a").copy(read = true)
        chapters += chapter(21L, mangaId = 2L, url = "/chapter/b")
        history += HistoryEntity(id = 200L, chapterId = 20L, lastRead = 2000L, timeRead = 30L)
        history += HistoryEntity(id = 201L, chapterId = 21L, lastRead = 3000L, timeRead = 40L)
        tracks += track(8L, mangaId = 2L, mediaId = 12L)
        merges +=
            MergeMangaEntity(
                id = 4L,
                mangaId = 2L,
                coverUrl = "",
                title = "Merge",
                url = "b",
                mergeType = MergeType.Komga,
            )
        categories += MangaCategoryEntity(id = 40L, mangaId = 1L, categoryId = 3)
        categories += MangaCategoryEntity(id = 41L, mangaId = 2L, categoryId = 5)
        artwork +=
            ArtworkEntity(
                id = 60L,
                mangaId = 2L,
                fileName = "60.jpg",
                volume = "1",
                locale = "en",
                description = "",
            )
        aggregates += MangaAggregateEntity(mangaId = 2L, volumes = "{}")
    }

    /** Title [U] on rows 1 and 2, title [U2] on rows 3 and 4. */
    private fun seedTwoDoubledTitles() {
        seedDoubledTitle()
        manga += manga(3, "/title/$U2", source = -1L, title = "Old two")
        manga += manga(4, "/manga/$U2", title = "Real two")
        chapters += chapter(41L, mangaId = 4L, url = "/chapter/c")
    }

    private fun snapshot(): List<List<Any>> =
        listOf(
            manga.toList(),
            chapters.toList(),
            history.toList(),
            tracks.toList(),
            merges.toList(),
            categories.toList(),
            artwork.toList(),
            aggregates.toList(),
        )

    @Suppress("UNCHECKED_CAST")
    private fun restore(snapshot: List<List<Any>>) {
        val tables =
            listOf(manga, chapters, history, tracks, merges, categories, artwork, aggregates)
        tables.forEachIndexed { index, table ->
            table as MutableList<Any>
            table.clear()
            table.addAll(snapshot[index])
        }
    }

    private fun manga(
        id: Long,
        url: String,
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

    private fun chapter(id: Long, mangaId: Long, url: String) =
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
            dateFetch = 500L,
            dateUpload = 0L,
            mangadexChapterId = null,
            language = null,
        )

    private fun track(id: Long, mangaId: Long, mediaId: Long) =
        TrackEntity(
            id = id,
            mangaId = mangaId,
            trackServiceId = 1,
            mediaId = mediaId,
            libraryId = null,
            title = "Track $id",
            lastChapterRead = 0f,
            totalChapters = 0,
            status = 0,
            score = 0f,
            trackingUrl = "",
            startedReadingDate = 0L,
            finishedReadingDate = 0L,
        )

    private companion object {
        const val MANGADEX = 2499283573021220255L
        const val U = "f9c33607-9180-4ba6-b85c-e4b5faee7192"
        const val U2 = "a1b2c3d4-0000-4abc-8def-1234567890ab"
    }
}
