package eu.kanade.tachiyomi.jobs.follows

import com.github.michaelbull.result.Ok
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.dao.LibraryDao
import org.nekomanga.data.database.dao.MangaDao
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class FollowsSyncProcessorTest {

    /** The manga table. The dao answers from it the way its queries would. */
    private val table = mutableListOf<MangaEntity>()

    private val mangaDao: MangaDao = mockk {
        coEvery { getMangaByUrlAndSourceSync(any(), any()) } answers
            {
                table
                    .filter { it.url == firstArg<String>() && it.source == secondArg<Long>() }
                    .minByOrNull { it.id }
            }
        // Newest first, so a test fails if the code relies on the order of this list
        coEvery { getMangaByUrls(any()) } answers
            {
                table.filter { it.url in firstArg<List<String>>() }.sortedByDescending { it.id }
            }
        coEvery { getMangaByIds(any()) } answers
            {
                table.filter { it.id in firstArg<List<Long>>() }
            }
        coEvery { insertManga(any()) } answers
            {
                table += firstArg<MangaEntity>().copy(id = NEW_ROW_ID)
                NEW_ROW_ID
            }
        coEvery { updateManga(any()) } answers
            {
                val row = firstArg<MangaEntity>()
                table[table.indexOfFirst { it.id == row.id }] = row
            }
    }

    private val categoryRepository: CategoryRepository = mockk(relaxed = true)

    private val source: MangaDex = mockk {
        every { id } returns MANGADEX_SOURCE
        coEvery { fetchAllFollows() } returns Ok(listOf(follow(TITLE_URL)))
    }

    private var idsSentToUpdate: List<Long>? = null

    /** The MangaDex tracker status of each manga in the track table. */
    private val trackTable = mutableMapOf<Long, Int>()

    private val trackRepository: TrackRepository = mockk {
        coEvery { getTracksForMangaByIds(any()) } answers
            {
                firstArg<List<Long>>().mapNotNull { mangaId ->
                    trackTable[mangaId]?.let { status -> mdListTrack(mangaId, status) }
                }
            }
        coEvery { insertTrack(any()) } answers { saveTrack(firstArg()) }
    }

    private val mdListService: MdList = mockk {
        every { createInitialTracker(any()) } answers { callOriginal() }
        // MdList.update saves the track it is given
        coEvery { update(any(), any(), any(), any()) } answers
            {
                firstArg<Track>().also { saveTrack(it) }
            }
    }

    private val followsHandler: FollowsHandler = mockk()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())

        coEvery { categoryRepository.getCategories() } returns
            listOf(Category.create("Default").apply { id = DEFAULT_CATEGORY })

        Injekt.addSingleton(
            mockk<SourceManager> {
                every { mangaDex } returns source
                every { isMangadex(any()) } answers { callOriginal() }
            }
        )
        Injekt.addSingleton(categoryRepository)
        Injekt.addSingleton(
            mockk<MangaDexPreferences> {
                every { mangaDexPullToLibraryIndices().get() } returns
                    setOf(FollowStatus.READING.int.toString())
                every { readingSync().get() } returns false
            }
        )
        Injekt.addSingleton(trackRepository)
        Injekt.addSingleton(mockk<TrackManager> { every { mdList } returns mdListService })
        Injekt.addSingleton(followsHandler)
        Injekt.addSingleton(
            mockk<LibraryPreferences> { every { defaultCategory().get() } returns DEFAULT_CATEGORY }
        )
        Injekt.addSingleton<MangaRepository>(
            MangaRepositoryImpl(
                libraryDao = mockk<LibraryDao>(),
                mangaDao = mangaDao,
                mangaDexPreferences = mockk<MangaDexPreferences>(),
                libraryPreferences = mockk<LibraryPreferences>(),
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        )
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `pull adds no copy of a library title whose row lost its source`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)

        val added = pull()

        assertEquals(listOf(Triple(1L, UNSET_SOURCE, true)), rows())
        assertEquals(0, added)
        assertEquals(emptyList<Long>(), idsSentToUpdate)
        coVerify(exactly = 0) { categoryRepository.setMangaCategories(any(), any()) }
    }

    @Test
    fun `pull does not bring back a removed copy of a title still in the library`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = false)

        val added = pull()

        assertEquals(
            listOf(Triple(1L, UNSET_SOURCE, true), Triple(2L, MANGADEX_SOURCE, false)),
            rows(),
        )
        assertEquals(0, added)
        assertEquals(emptyList<Long>(), idsSentToUpdate)
    }

    @Test
    fun `pull leaves a title alone when its library row has the other url form`() = runTest {
        table += row(id = 1, source = MANGADEX_SOURCE, favorite = false)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = true, url = MANGA_URL)

        val added = pull()

        assertEquals(
            listOf(Triple(1L, MANGADEX_SOURCE, false), Triple(2L, MANGADEX_SOURCE, true)),
            rows(),
        )
        assertEquals(0, added)
        assertEquals(emptyList<Long>(), idsSentToUpdate)
    }

    @Test
    fun `pull puts a row that lost its source back in the library instead of adding a copy`() =
        runTest {
            table += row(id = 1, source = UNSET_SOURCE, favorite = false)

            val added = pull()

            assertEquals(listOf(Triple(1L, MANGADEX_SOURCE, true)), rows())
            assertEquals(1, added)
            assertEquals(listOf(1L), idsSentToUpdate)
            coVerify(exactly = 1) {
                categoryRepository.setMangaCategories(
                    match {
                        it.single().manga_id == 1L && it.single().category_id == DEFAULT_CATEGORY
                    },
                    listOf(1L),
                )
            }
        }

    @Test
    fun `pull puts the oldest row back when no row of the title is in the library`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = false)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = false)

        val added = pull()

        assertEquals(
            listOf(Triple(1L, MANGADEX_SOURCE, true), Triple(2L, MANGADEX_SOURCE, false)),
            rows(),
        )
        assertEquals(1, added)
        assertEquals(listOf(1L), idsSentToUpdate)
    }

    @Test
    fun `pull leaves a normal library row alone`() = runTest {
        table += row(id = 1, source = MANGADEX_SOURCE, favorite = true)

        val added = pull()

        assertEquals(listOf(Triple(1L, MANGADEX_SOURCE, true)), rows())
        assertEquals(0, added)
        assertEquals(emptyList<Long>(), idsSentToUpdate)
    }

    @Test
    fun `pull adds a followed title with no row to the library`() = runTest {
        val added = pull()

        assertEquals(listOf(Triple(NEW_ROW_ID, MANGADEX_SOURCE, true)), rows())
        assertEquals(TITLE_URL, table.single().url)
        assertTrue((table.single().dateAdded ?: 0L) > 0L)
        assertEquals(1, added)
        assertEquals(listOf(NEW_ROW_ID), idsSentToUpdate)
        coVerify(exactly = 1) {
            categoryRepository.setMangaCategories(
                match { it.single().manga_id == NEW_ROW_ID },
                listOf(NEW_ROW_ID),
            )
        }
    }

    @Test
    fun `pull matches each followed title to its own rows`() = runTest {
        table += row(id = 1, source = MANGADEX_SOURCE, favorite = true, url = OTHER_URL)
        coEvery { source.fetchAllFollows() } returns
            Ok(listOf(follow(TITLE_URL), follow(OTHER_URL)))

        val added = pull()

        assertEquals(
            listOf(1L to OTHER_URL, NEW_ROW_ID to TITLE_URL),
            table.sortedBy { it.id }.map { it.id to it.url },
        )
        assertEquals(1, added)
    }

    @Test
    fun `pull reads the rows of every followed title in one query`() = runTest {
        coEvery { source.fetchAllFollows() } returns
            Ok(listOf(follow(TITLE_URL), follow(OTHER_URL)))

        pull()

        coVerify(exactly = 1) { mangaDao.getMangaByUrls(any()) }
    }

    @Test
    fun `pull adds a title once when the follows list repeats it`() = runTest {
        coEvery { source.fetchAllFollows() } returns
            Ok(listOf(follow(TITLE_URL), follow(TITLE_URL)))

        val added = pull()

        assertEquals(listOf(Triple(NEW_ROW_ID, MANGADEX_SOURCE, true)), rows())
        assertEquals(1, added)
    }

    @Test
    fun `push sends a title again after MangaDex refused it`() = runTest {
        table += row(id = 1, source = MANGADEX_SOURCE, favorite = true)
        coEvery { followsHandler.updateFollowStatus(any(), any()) } returnsMany listOf(false, true)

        val firstPush = push("1")
        val secondPush = push("1")

        assertEquals(0, firstPush)
        assertEquals(1, secondPush)
        coVerify(exactly = 2) {
            followsHandler.updateFollowStatus(TITLE_UUID, FollowStatus.READING)
        }
        assertEquals(mapOf(1L to FollowStatus.READING.int), trackTable)
    }

    @Test
    fun `push counts and marks as reading only the titles MangaDex followed`() = runTest {
        table += row(id = 1, source = MANGADEX_SOURCE, favorite = true)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = true, url = OTHER_URL)
        trackTable[1] = FollowStatus.UNFOLLOWED.int
        trackTable[2] = FollowStatus.UNFOLLOWED.int
        coEvery { followsHandler.updateFollowStatus(TITLE_UUID, any()) } returns false
        coEvery { followsHandler.updateFollowStatus(OTHER_UUID, any()) } returns true

        val pushed = push("1,2")

        assertEquals(1, pushed)
        assertEquals(
            mapOf(1L to FollowStatus.UNFOLLOWED.int, 2L to FollowStatus.READING.int),
            trackTable,
        )
    }

    private fun follow(url: String) =
        SourceManga(
            currentThumbnail = "",
            url = url,
            title = "Title",
            displayTextRes = FollowStatus.READING.stringRes,
        )

    private suspend fun pull(): Int =
        FollowsSyncProcessor()
            .fromMangaDex(
                errorNotification = {},
                updateNotification = { _, _, _ -> },
                completeNotification = {},
                updateManga = { idsSentToUpdate = it },
            )

    /** Runs a push of the given manga ids and returns the count it reports. */
    private suspend fun push(ids: String): Int {
        var pushed = -1
        FollowsSyncProcessor()
            .toMangaDex(
                updateNotification = { _, _, _ -> },
                completeNotification = { pushed = it },
                ids = ids,
            )
        return pushed
    }

    private fun mdListTrack(mangaId: Long, status: Int) =
        Track.create(TrackManager.MDLIST).apply {
            id = mangaId
            manga_id = mangaId
            this.status = status
        }

    private fun saveTrack(track: Track): Long {
        trackTable[track.manga_id] = track.status
        return track.manga_id
    }

    private fun rows() = table.sortedBy { it.id }.map { Triple(it.id, it.source, it.favorite) }

    private fun row(id: Long, source: Long, favorite: Boolean, url: String = TITLE_URL) =
        MangaImpl()
            .apply {
                this.id = id
                this.source = source
                this.url = url
                this.title = "Title"
                this.favorite = favorite
            }
            .toEntity()

    private companion object {
        const val MANGADEX_SOURCE = 2499283573021220255L
        const val UNSET_SOURCE = -1L
        const val DEFAULT_CATEGORY = 5
        const val NEW_ROW_ID = 1000L
        const val TITLE_URL = "/title/00000000-0000-4000-8000-000000000001"
        const val MANGA_URL = "/manga/00000000-0000-4000-8000-000000000001"
        const val OTHER_URL = "/title/00000000-0000-4000-8000-000000000002"
        const val TITLE_UUID = "00000000-0000-4000-8000-000000000001"
        const val OTHER_UUID = "00000000-0000-4000-8000-000000000002"
    }
}
