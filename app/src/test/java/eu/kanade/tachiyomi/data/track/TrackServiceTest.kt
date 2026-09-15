package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SManga
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class TrackServiceTest {

    private lateinit var chapterRepository: ChapterRepository
    private lateinit var mangaRepository: MangaRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var preferences: PreferencesHelper
    private lateinit var networkService: NetworkHelper
    private lateinit var trackRepository: TrackRepository
    private lateinit var testService: TestTrackService

    private class TestTrackService(id: Int = 1) : TrackService(id) {
        override val supportsReadingDates = true

        override fun nameRes(): Int = 0

        override fun getLogo(): Int = 0

        override fun getLogoColor(): Int = 0

        override fun getStatusList(): List<Int> = listOf(1, 2, 3)

        override fun isCompletedStatus(index: Int): Boolean = index == 1

        override fun completedStatus(): Int = 2

        override fun readingStatus(): Int = 1

        override fun planningStatus(): Int = 3

        override fun getStatus(status: Int): String = status.toString()

        override fun getGlobalStatus(status: Int): String = status.toString()

        override fun getScoreList(): List<String> = emptyList()

        override fun displayScore(track: Track): String = ""

        override suspend fun add(track: Track): Track = track

        override suspend fun update(track: Track, setToRead: Boolean): Track {
            updateTrackStatus(track, setToRead, mustReadToComplete = true)
            return track
        }

        override suspend fun bind(track: Track): Track = track

        override suspend fun search(
            query: String,
            manga: Manga,
            wasPreviouslyTracked: Boolean,
        ): List<TrackSearch> = emptyList()

        override suspend fun refresh(track: Track): Track = track

        override suspend fun login(username: String, password: String): Boolean = true
    }

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        chapterRepository = mockk(relaxed = true)
        mangaRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        networkService = mockk(relaxed = true)
        trackRepository = mockk(relaxed = true)

        Injekt.addSingleton(chapterRepository)
        Injekt.addSingleton(mangaRepository)
        Injekt.addSingleton(historyRepository)
        Injekt.addSingleton(preferences)
        Injekt.addSingleton(networkService)
        Injekt.addSingleton(trackRepository)

        testService = TestTrackService()
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given track when total chapters match last chapter read then status is completed and dates set`() =
        runTest {
            val track =
                Track.create(1).apply {
                    manga_id = 10L
                    status = testService.readingStatus()
                    last_chapter_read = 12f
                    total_chapters = 12
                    finished_reading_date = 0L
                    started_reading_date = 0L
                }

            testService.updateTrackStatus(track, setToReadStatus = true, mustReadToComplete = true)

            assertEquals(testService.completedStatus(), track.status)
            assertTrue(track.finished_reading_date > 0L)
            assertTrue(track.started_reading_date > 0L)
        }

    @Test
    fun `given track with zero total chapters when manga completed and all chapters read then status is completed`() =
        runTest {
            val mangaId = 20L
            val track =
                Track.create(1).apply {
                    manga_id = mangaId
                    status = testService.readingStatus()
                    last_chapter_read = 10f
                    total_chapters = 0
                    finished_reading_date = 0L
                    started_reading_date = 0L
                }

            val mockManga =
                mockk<Manga>(relaxed = true) {
                    coEvery { isOneShotOrCompleted(chapterRepository, any()) } returns true
                    every { last_chapter_number } returns 10
                }
            val mockChapters =
                (1..10).map { num ->
                    mockk<Chapter>(relaxed = true) {
                        every { chapter_number } returns num.toFloat()
                        every { read } returns true
                        every { isRecognizedNumber } returns true
                        every { name } returns "Chapter $num"
                    }
                }

            coEvery { mangaRepository.getMangaById(mangaId) } returns mockManga
            coEvery { chapterRepository.getChaptersForManga(mangaId) } returns mockChapters

            testService.updateTrackStatus(track, setToReadStatus = true, mustReadToComplete = true)

            assertEquals(testService.completedStatus(), track.status)
            assertEquals(10, track.total_chapters)
            assertTrue(track.finished_reading_date > 0L)
            assertTrue(track.started_reading_date > 0L)
        }

    @Test
    fun `given track with zero total chapters when chapter with END tag is read then status is completed`() =
        runTest {
            val mangaId = 30L
            val track =
                Track.create(1).apply {
                    manga_id = mangaId
                    status = testService.readingStatus()
                    last_chapter_read = 5f
                    total_chapters = 0
                    finished_reading_date = 0L
                    started_reading_date = 0L
                }

            val mockManga =
                mockk<Manga>(relaxed = true) {
                    every { status } returns SManga.COMPLETED
                    every { last_chapter_number } returns 5
                }
            val mockChapters =
                (1..5).map { num ->
                    mockk<Chapter>(relaxed = true) {
                        every { chapter_number } returns num.toFloat()
                        every { read } returns (num == 5)
                        every { isRecognizedNumber } returns true
                        every { name } returns if (num == 5) "Chapter 5 [END]" else "Chapter $num"
                    }
                }

            coEvery { mangaRepository.getMangaById(mangaId) } returns mockManga
            coEvery { chapterRepository.getChaptersForManga(mangaId) } returns mockChapters

            testService.updateTrackStatus(track, setToReadStatus = true, mustReadToComplete = true)

            assertEquals(testService.completedStatus(), track.status)
            assertEquals(5, track.total_chapters)
            assertTrue(track.finished_reading_date > 0L)
            assertTrue(track.started_reading_date > 0L)
        }

    @Test
    fun `given planning status and chapter read higher than 0 when updateTrackStatus then status is reading and started date set`() =
        runTest {
            val track =
                Track.create(1).apply {
                    manga_id = 40L
                    status = testService.planningStatus()
                    last_chapter_read = 1f
                    total_chapters = 20
                    started_reading_date = 0L
                    finished_reading_date = 0L
                }

            testService.updateTrackStatus(track, setToReadStatus = true, mustReadToComplete = true)

            assertEquals(testService.readingStatus(), track.status)
            assertTrue(track.started_reading_date > 0L)
            assertEquals(0L, track.finished_reading_date)
        }
}
