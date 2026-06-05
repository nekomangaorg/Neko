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
import org.nekomanga.data.database.repository.TrackRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton

class ChapterTrackSyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockTrackRepository: TrackRepository
    private lateinit var trackManager: TrackManager
    private lateinit var preferences: PreferencesHelper
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockTrackRepository = mockk(relaxed = true)
        trackManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { preferences.context } returns context

        // Injekt
        Injekt.addSingleton(mockTrackRepository)
        Injekt.addSingleton(trackManager)
        Injekt.addSingleton(preferences)

        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        val fields = Injekt::class.java.declaredFields
        for (field in fields) {
            if (field.name == "registrars") {
                field.isAccessible = true
                val map = field.get(Injekt) as MutableMap<*, *>
                map.clear()
            }
        }
    }

    @Test
    fun `given null mangaId when updateTrackChapterRead is called then returns early`() = runTest {
        updateTrackChapterRead(null, 5f)
        coVerify(exactly = 0) { mockTrackRepository.getTracksForManga(any()) }
    }

    @Test
    fun `given track and online when new chapter read is higher then updates service and inserts returned track`() =
        runTest {
            val mangaId = 1L
            val track =
                mockk<Track>(relaxed = true) {
                    every { sync_id } returns 2
                    every { last_chapter_read } returns 3f
                }
            val updatedTrack = mockk<Track>(relaxed = true)

            coEvery { mockTrackRepository.getTracksForManga(mangaId) } returns listOf(track)
            every { context.isOnline() } returns true

            val service = mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            every { trackManager.getService(2) } returns service
            coEvery { service.update(track, true) } returns updatedTrack

            updateTrackChapterRead(mangaId, 5f)

            coVerify(exactly = 1) { service.update(track, true) }
            coVerify(exactly = 1) { mockTrackRepository.insertTrack(updatedTrack) }
        }
}
