package eu.kanade.tachiyomi.jobs.tracking

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.store.DelayedTrackingStore
import org.nekomanga.domain.track.store.DelayedTrackingStore.DelayedTrackingItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton

class DelayedTrackingUpdateJobTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockTrackRepository: TrackRepository
    private lateinit var trackManager: TrackManager
    private lateinit var delayedTrackingStore: DelayedTrackingStore
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockTrackRepository = mockk(relaxed = true)
        trackManager = mockk(relaxed = true)
        delayedTrackingStore = mockk(relaxed = true)
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true) { every { runAttemptCount } returns 0 }

        Injekt.addSingleton(mockTrackRepository)
        Injekt.addSingleton(trackManager)
        Injekt.addSingleton(delayedTrackingStore)
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
    fun `given runAttemptCount greater than 3 when doWork then returns failure`() = runTest {
        every { workerParams.runAttemptCount } returns 4
        val job = DelayedTrackingUpdateJob(context, workerParams)

        val result = job.doWork()

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `given empty store when doWork then returns success without actions`() = runTest {
        every { delayedTrackingStore.getItems() } returns emptyList()
        val job = DelayedTrackingUpdateJob(context, workerParams)

        val result = job.doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { mockTrackRepository.getTrackById(any()) }
    }

    @Test
    fun `given track not in repository when doWork then removes from store and does not update service`() =
        runTest {
            val item = DelayedTrackingItem(trackId = 1L, lastChapterRead = 5f)
            every { delayedTrackingStore.getItems() } returns listOf(item)
            coEvery { mockTrackRepository.getTrackById(1L) } returns null

            val job = DelayedTrackingUpdateJob(context, workerParams)
            val result = job.doWork()

            assertEquals(Result.success(), result)
            coVerify(exactly = 1) { delayedTrackingStore.remove(1L) }
            coVerify(exactly = 0) { trackManager.getService(any()) }
        }

    @Test
    fun `given queued chapter less than or equal to local track when doWork then removes from store and skips update`() =
        runTest {
            val item = DelayedTrackingItem(trackId = 1L, lastChapterRead = 2f)
            val track =
                mockk<Track>(relaxed = true) {
                    every { id } returns 1L
                    every { sync_id } returns 2
                    every { last_chapter_read } returns 3f
                }
            every { delayedTrackingStore.getItems() } returns listOf(item)
            coEvery { mockTrackRepository.getTrackById(1L) } returns track

            val job = DelayedTrackingUpdateJob(context, workerParams)
            val result = job.doWork()

            assertEquals(Result.success(), result)
            coVerify(exactly = 1) { delayedTrackingStore.remove(1L) }
            coVerify(exactly = 0) { trackManager.getService(any()) }
        }

    @Test
    fun `given higher queued chapter and valid service when doWork then updates service, inserts track, and removes from store`() =
        runTest {
            val item = DelayedTrackingItem(trackId = 1L, lastChapterRead = 5f)
            val track =
                mockk<Track>(relaxed = true) {
                    every { id } returns 1L
                    every { manga_id } returns 10L
                    every { sync_id } returns 2
                    every { last_chapter_read } returns 3f
                }
            val updatedTrack = mockk<Track>(relaxed = true)
            val service = mockk<TrackService>(relaxed = true)

            every { delayedTrackingStore.getItems() } returns listOf(item)
            coEvery { mockTrackRepository.getTrackById(1L) } returns track
            every { trackManager.getService(2) } returns service
            coEvery { service.update(track, true) } returns updatedTrack

            val job = DelayedTrackingUpdateJob(context, workerParams)
            val result = job.doWork()

            assertEquals(Result.success(), result)
            coVerify(exactly = 1) { service.update(track, true) }
            coVerify(exactly = 1) { mockTrackRepository.insertTrack(updatedTrack) }
            coVerify(exactly = 1) { delayedTrackingStore.remove(1L) }
        }

    @Test
    fun `given missing service when doWork then removes from store`() = runTest {
        val item = DelayedTrackingItem(trackId = 1L, lastChapterRead = 5f)
        val track =
            mockk<Track>(relaxed = true) {
                every { id } returns 1L
                every { sync_id } returns 2
                every { last_chapter_read } returns 3f
            }

        every { delayedTrackingStore.getItems() } returns listOf(item)
        coEvery { mockTrackRepository.getTrackById(1L) } returns track
        every { trackManager.getService(2) } returns null

        val job = DelayedTrackingUpdateJob(context, workerParams)
        val result = job.doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 1) { delayedTrackingStore.remove(1L) }
    }

    @Test
    fun `given service update throws exception when doWork then retains item in store`() = runTest {
        val item = DelayedTrackingItem(trackId = 1L, lastChapterRead = 5f)
        val track =
            mockk<Track>(relaxed = true) {
                every { id } returns 1L
                every { manga_id } returns 10L
                every { sync_id } returns 2
                every { last_chapter_read } returns 3f
            }
        val service = mockk<TrackService>(relaxed = true)

        every { delayedTrackingStore.getItems() } returns listOf(item)
        coEvery { mockTrackRepository.getTrackById(1L) } returns track
        every { trackManager.getService(2) } returns service
        coEvery { service.update(track, true) } throws RuntimeException("Network error")

        val job = DelayedTrackingUpdateJob(context, workerParams)
        val result = job.doWork()

        assertEquals(Result.retry(), result)
        coVerify(exactly = 1) { service.update(track, true) }
        coVerify(exactly = 0) { mockTrackRepository.insertTrack(any()) }
        coVerify(exactly = 0) { delayedTrackingStore.remove(1L) }
    }
}
