package eu.kanade.tachiyomi.util.chapter

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Category
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
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.store.DelayedTrackingStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class ChapterTrackSyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockTrackRepository: TrackRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var trackManager: TrackManager
    private lateinit var preferences: PreferencesHelper
    private lateinit var delayedTrackingStore: DelayedTrackingStore
    private lateinit var context: Context

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(testDispatcher)
        mockTrackRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        trackManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        delayedTrackingStore = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { preferences.context } returns context
        every { preferences.excludeCategoriesFromTrackingUpdates().get() } returns emptySet()

        // Injekt
        Injekt.addSingleton(mockTrackRepository)
        Injekt.addSingleton(categoryRepository)
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
            coEvery { service.update(track, true) } returns updatedTrack

            updateTrackChapterRead(mangaId, 5f)

            coVerify(exactly = 1) { service.update(track, true) }
            coVerify(exactly = 1) { mockTrackRepository.insertTrack(updatedTrack) }
            coVerify(exactly = 1) { delayedTrackingStore.remove(100L) }
        }

    @Test
    fun `given manga in an excluded category when updateTrackChapterRead is called then no service is updated`() =
        runTest {
            val mangaId = 1L
            val track =
                mockk<Track>(relaxed = true) {
                    every { id } returns 100L
                    every { sync_id } returns 2
                    every { last_chapter_read } returns 3f
                }
            coEvery { mockTrackRepository.getTracksForManga(mangaId) } returns listOf(track)
            every { context.isOnline() } returns true
            val service = mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            every { trackManager.getService(2) } returns service

            every { preferences.excludeCategoriesFromTrackingUpdates().get() } returns setOf("7")
            coEvery { categoryRepository.getCategoriesForManga(mangaId) } returns
                listOf(
                    mockk<Category>(relaxed = true) { every { id } returns 3 },
                    mockk<Category>(relaxed = true) { every { id } returns 7 },
                )

            updateTrackChapterRead(mangaId, 5f)

            coVerify(exactly = 0) { service.update(any(), any()) }
            coVerify(exactly = 0) { mockTrackRepository.insertTrack(any()) }
        }

    @Test
    fun `given manga with no category when the default category is excluded then no service is updated`() =
        runTest {
            val mangaId = 1L
            val track =
                mockk<Track>(relaxed = true) {
                    every { id } returns 100L
                    every { sync_id } returns 2
                    every { last_chapter_read } returns 3f
                }
            coEvery { mockTrackRepository.getTracksForManga(mangaId) } returns listOf(track)
            every { context.isOnline() } returns true
            val service = mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            every { trackManager.getService(2) } returns service

            every { preferences.excludeCategoriesFromTrackingUpdates().get() } returns setOf("0")
            coEvery { categoryRepository.getCategoriesForManga(mangaId) } returns emptyList()

            updateTrackChapterRead(mangaId, 5f)

            coVerify(exactly = 0) { service.update(any(), any()) }
        }

    @Test
    fun `given manga outside the excluded categories when updateTrackChapterRead is called then the service is updated`() =
        runTest {
            val mangaId = 1L
            val track =
                mockk<Track>(relaxed = true) {
                    every { id } returns 100L
                    every { sync_id } returns 2
                    every { last_chapter_read } returns 3f
                }
            coEvery { mockTrackRepository.getTracksForManga(mangaId) } returns listOf(track)
            every { context.isOnline() } returns true
            val service = mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            every { trackManager.getService(2) } returns service
            coEvery { service.update(track, true) } returns mockk(relaxed = true)

            every { preferences.excludeCategoriesFromTrackingUpdates().get() } returns setOf("7")
            coEvery { categoryRepository.getCategoriesForManga(mangaId) } returns
                listOf(mockk<Category>(relaxed = true) { every { id } returns 3 })

            updateTrackChapterRead(mangaId, 5f)

            coVerify(exactly = 1) { service.update(track, true) }
        }
}
