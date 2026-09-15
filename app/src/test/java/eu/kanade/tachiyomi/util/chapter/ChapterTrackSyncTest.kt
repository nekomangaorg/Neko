package eu.kanade.tachiyomi.util.chapter

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.system.isOnline
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.store.DelayedTrackingStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class ChapterTrackSyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockTrackRepository: TrackRepository
    private lateinit var mockMangaRepository: MangaRepository
    private lateinit var mockChapterRepository: ChapterRepository
    private lateinit var trackManager: TrackManager
    private lateinit var preferences: PreferencesHelper
    private lateinit var delayedTrackingStore: DelayedTrackingStore
    private lateinit var context: Context

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(testDispatcher)
        mockTrackRepository = mockk(relaxed = true)
        mockMangaRepository = mockk(relaxed = true)
        mockChapterRepository = mockk(relaxed = true)
        trackManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        delayedTrackingStore = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { preferences.context } returns context

        // Injekt
        Injekt.addSingleton(mockTrackRepository)
        Injekt.addSingleton(mockMangaRepository)
        Injekt.addSingleton(mockChapterRepository)
        Injekt.addSingleton(trackManager)
        Injekt.addSingleton(preferences)
        Injekt.addSingleton(delayedTrackingStore)

        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given null mangaId when updateTrackChapterRead is called then returns early`() = runTest {
        updateTrackChapterRead(null, 5f)
        coVerify(exactly = 0) { mockTrackRepository.getTracksForManga(any()) }
    }

    @Test
    fun `given track and online when new chapter read is higher then updates service, inserts returned track, and removes from delayed store`() =
        runTest {
            val mangaId = 1L
            val track =
                mockk<Track>(relaxed = true) {
                    every { id } returns 100L
                    every { sync_id } returns 2
                    every { last_chapter_read } returns 3f
                }
            val updatedTrack = mockk<Track>(relaxed = true)

            coEvery { mockTrackRepository.getTracksForManga(mangaId) } returns listOf(track)
            every { context.isOnline() } returns true

            val service = mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            every { trackManager.getService(2) } returns service
            coEvery { service.update(track, true, any(), any()) } returns updatedTrack

            updateTrackChapterRead(mangaId, 5f)

            coVerify(exactly = 1) { service.update(track, true, any(), any()) }
            coVerify(exactly = 1) { mockTrackRepository.insertTrack(updatedTrack) }
            coVerify(exactly = 1) { delayedTrackingStore.remove(100L) }
        }

    @Test
    fun `given multiple eligible tracks when updateTrackChapterRead is called then manga and chapters are fetched only once`() =
        runTest {
            val mangaId = 1L
            val track1 =
                mockk<Track>(relaxed = true) {
                    every { id } returns 101L
                    every { sync_id } returns 1
                    every { last_chapter_read } returns 3f
                }
            val track2 =
                mockk<Track>(relaxed = true) {
                    every { id } returns 102L
                    every { sync_id } returns 2
                    every { last_chapter_read } returns 3f
                }

            coEvery { mockTrackRepository.getTracksForManga(mangaId) } returns
                listOf(track1, track2)
            every { context.isOnline() } returns true

            val service1 = mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            val service2 = mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            every { trackManager.getService(1) } returns service1
            every { trackManager.getService(2) } returns service2
            coEvery { service1.update(track1, true, any(), any()) } returns track1
            coEvery { service2.update(track2, true, any(), any()) } returns track2

            updateTrackChapterRead(mangaId, 5f)

            coVerify(exactly = 1) { mockMangaRepository.getMangaById(mangaId) }
            coVerify(exactly = 1) { mockChapterRepository.getChaptersForManga(mangaId) }
            coVerify(exactly = 1) { service1.update(track1, true, any(), any()) }
            coVerify(exactly = 1) { service2.update(track2, true, any(), any()) }
        }
}
