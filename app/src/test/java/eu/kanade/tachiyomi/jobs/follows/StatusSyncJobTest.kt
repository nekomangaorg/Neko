package eu.kanade.tachiyomi.jobs.follows

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.michaelbull.result.Ok
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.site.MangaDexPreferences
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/** Runs the job with the input its start functions queue, through a real FollowsSyncProcessor. */
@OptIn(ExperimentalCoroutinesApi::class)
class StatusSyncJobTest {

    private val context: Context = mockk(relaxed = true)
    private val workManager: WorkManager = mockk()
    private val queuedRequest = slot<OneTimeWorkRequest>()

    private val mangaRepository: MangaRepository = mockk {
        coEvery { getMangaByIds(any()) } returns emptyList()
        coEvery { getLibraryList() } returns emptyList()
        coEvery { getMangaByUrls(any()) } returns emptyList()
    }
    private val mangaDex: MangaDex = mockk { coEvery { fetchAllFollows() } returns Ok(emptyList()) }
    private val loginHelper: MangaDexLoginHelper = mockk { every { isLoggedIn() } returns true }

    private var storedPushIds = ""
    private val followsPushIds: Preference<String> = mockk {
        every { get() } answers { storedPushIds }
        every { set(any()) } answers { storedPushIds = firstArg() }
        every { delete() } answers { storedPushIds = "" }
    }

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(FollowsSyncProcessor())
        Injekt.addSingleton(loginHelper)
        Injekt.addSingleton(mangaRepository)
        Injekt.addSingleton(
            mockk<SourceManager> { every { mangaDex } returns this@StatusSyncJobTest.mangaDex }
        )
        Injekt.addSingleton(
            mockk<CategoryRepository> { coEvery { getCategories() } returns emptyList() }
        )
        Injekt.addSingleton(
            mockk<MangaDexPreferences> {
                every { mangaDexPullToLibraryIndices().get() } returns emptySet()
                every { followsPushIds() } returns this@StatusSyncJobTest.followsPushIds
            }
        )
        Injekt.addSingleton(
            mockk<LibraryPreferences> { every { defaultCategory().get() } returns 0 }
        )

        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        every { workManager.enqueueUniqueWork(any(), any(), capture(queuedRequest)) } returns
            mockk()

        mockkObject(NotificationReceiver.Companion)
        every { NotificationReceiver.cancelFollowSyncPendingIntent(any()) } returns mockk()
        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        coEvery { any<CoroutineWorker>().tryToSetForeground() } just runs
        every { any<Context>().notificationBuilder(any(), any()) } returns
            mockk<NotificationCompat.Builder>(relaxed = true)
        every { any<Context>().notificationManager } returns mockk(relaxed = true)
        every { any<Context>().toast(any<String>(), any()) } just runs

        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `selecting only manga id 1 pushes that manga and does not pull follows`() = runTest {
        StatusSyncJob.startSelectedNow(workManager, listOf(1L))

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))

        coVerify(exactly = 1) { mangaRepository.getMangaByIds(listOf(1L)) }
        coVerify(exactly = 0) { mangaDex.fetchAllFollows() }
    }

    @Test
    fun `selected manga are pushed by id and the rest of the library is not`() = runTest {
        StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))

        coVerify(exactly = 1) { mangaRepository.getMangaByIds(listOf(5L, 7L)) }
        coVerify(exactly = 0) { mangaRepository.getLibraryList() }
        coVerify(exactly = 0) { mangaDex.fetchAllFollows() }
    }

    @Test
    fun `pull from settings pulls follows and pushes nothing`() = runTest {
        StatusSyncJob.startNow(context, StatusSyncJob.followsFromDex)

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))

        coVerify(exactly = 1) { mangaDex.fetchAllFollows() }
        coVerify(exactly = 0) { mangaRepository.getMangaByIds(any()) }
        coVerify(exactly = 0) { mangaRepository.getLibraryList() }
    }

    @Test
    fun `push from settings pushes the whole library`() = runTest {
        StatusSyncJob.startNow(context, StatusSyncJob.entireLibraryToDex)

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))

        coVerify(exactly = 1) { mangaRepository.getLibraryList() }
        coVerify(exactly = 0) { mangaRepository.getMangaByIds(any()) }
        coVerify(exactly = 0) { mangaDex.fetchAllFollows() }
    }

    @Test
    fun `ids queued as the sync type by the previous version are still pushed`() = runTest {
        val queuedByOldVersion = Data.Builder().putString("sync_type", "5, 7").build()

        assertEquals(Result.success(), runJob(queuedByOldVersion))

        coVerify(exactly = 1) { mangaRepository.getMangaByIds(listOf(5L, 7L)) }
        coVerify(exactly = 0) { mangaRepository.getLibraryList() }
        coVerify(exactly = 0) { mangaDex.fetchAllFollows() }
    }

    @Test
    fun `an empty selection queues nothing and keeps the ids of a queued push`() {
        storedPushIds = "5,7"

        StatusSyncJob.startSelectedNow(workManager, emptyList())

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
        assertEquals("5,7", storedPushIds)
    }

    @Test
    fun `a selected push with no stored ids pushes nothing and shows no notification`() = runTest {
        val noStoredIds = Data.Builder().putBoolean("selected_manga", true).build()

        assertEquals(Result.success(), runJob(noStoredIds))

        coVerify(exactly = 0) { mangaRepository.getMangaByIds(any()) }
        coVerify(exactly = 0) { mangaRepository.getLibraryList() }
        coVerify(exactly = 0) { any<CoroutineWorker>().tryToSetForeground() }
    }

    @Test
    fun `a job with no input pushes and pulls nothing`() = runTest {
        assertEquals(Result.success(), runJob(Data.EMPTY))

        coVerify(exactly = 0) { mangaRepository.getMangaByIds(any()) }
        coVerify(exactly = 0) { mangaRepository.getLibraryList() }
        coVerify(exactly = 0) { mangaDex.fetchAllFollows() }
    }

    @Test
    fun `a selection too big for the job input is queued and every id is pushed`() = runTest {
        val selection = (10_000_000L until 10_002_500L).toList()
        val pushed = mutableListOf<List<Long>>()
        coEvery { mangaRepository.getMangaByIds(capture(pushed)) } returns emptyList()

        StatusSyncJob.startSelectedNow(workManager, selection)

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))
        assertEquals(selection, pushed.flatten())
    }

    @Test
    fun `the stored ids are removed when the push finishes`() = runTest {
        StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))

        assertEquals("", storedPushIds)
    }

    @Test
    fun `a push stopped part way still has its ids when WorkManager runs it again`() = runTest {
        var pushes = 0
        val pushStarted = CompletableDeferred<Unit>()
        coEvery { mangaRepository.getMangaByIds(listOf(5L, 7L)) } coAnswers
            {
                pushes++
                if (pushes == 1) {
                    pushStarted.complete(Unit)
                    awaitCancellation()
                }
                emptyList()
            }
        StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))
        val input = queuedRequest.captured.workSpec.input

        val stopped = launch { runJob(input) }
        pushStarted.await()
        stopped.cancelAndJoin()

        assertEquals(Result.success(), runJob(input))
        assertEquals(2, pushes)
    }

    @Test
    fun `a selection made while a push waits is added to it`() = runTest {
        storedPushIds = "1,2,3"
        val pushed = mutableListOf<List<Long>>()
        coEvery { mangaRepository.getMangaByIds(capture(pushed)) } returns emptyList()

        StatusSyncJob.startSelectedNow(workManager, listOf(3L, 5L))

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))
        assertEquals(listOf(1L, 2L, 3L, 5L), pushed.flatten())
    }

    @Test
    fun `a selection made during a push is left for the push queued after it`() = runTest {
        val pushed = mutableListOf<List<Long>>()
        coEvery { mangaRepository.getMangaByIds(capture(pushed)) } answers
            {
                if (pushed.size == 1) StatusSyncJob.startSelectedNow(workManager, listOf(9L))
                emptyList()
            }
        StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))
        assertEquals("9", storedPushIds)

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))
        assertEquals(listOf(listOf(5L, 7L), listOf(9L)), pushed)
        assertEquals("", storedPushIds)
    }

    @Test
    fun `a selected push waits for a running sync and a sync from settings does not`() {
        val policies = mutableListOf<ExistingWorkPolicy>()
        every {
            workManager.enqueueUniqueWork(any(), capture(policies), any<OneTimeWorkRequest>())
        } returns mockk()

        StatusSyncJob.startSelectedNow(workManager, listOf(5L))
        StatusSyncJob.startNow(context, StatusSyncJob.followsFromDex)

        assertEquals(
            listOf(ExistingWorkPolicy.APPEND_OR_REPLACE, ExistingWorkPolicy.KEEP),
            policies,
        )
    }

    @Test
    fun `a pull from settings leaves the stored ids to the push queued after it`() = runTest {
        storedPushIds = "1,2,3"

        StatusSyncJob.startNow(context, StatusSyncJob.followsFromDex)

        assertEquals(Result.success(), runJob(queuedRequest.captured.workSpec.input))
        coVerify(exactly = 1) { mangaDex.fetchAllFollows() }
        coVerify(exactly = 0) { mangaRepository.getMangaByIds(any()) }
        assertEquals("1,2,3", storedPushIds)
    }

    @Test
    fun `a push that fails drops the stored ids`() = runTest {
        coEvery { mangaRepository.getMangaByIds(any()) } throws IllegalStateException("closed")
        StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))

        assertEquals(Result.failure(), runJob(queuedRequest.captured.workSpec.input))
        assertEquals("", storedPushIds)
    }

    @Test
    fun `a selection made before a failed push ends is dropped with it`() = runTest {
        coEvery { mangaRepository.getMangaByIds(any()) } throws IllegalStateException("closed")
        val notificationManager: NotificationManager = mockk(relaxed = true)
        every { any<Context>().notificationManager } returns notificationManager
        // The job's last call, made while WorkManager still has it running, so the push this
        // selection queues waits behind the failed one
        every { notificationManager.cancel(Notifications.Id.Status.Complete) } answers
            {
                StatusSyncJob.startSelectedNow(workManager, listOf(9L))
            }
        StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))

        assertEquals(Result.failure(), runJob(queuedRequest.captured.workSpec.input))
        assertEquals("", storedPushIds)
    }

    @Test
    fun `a push that throws CancellationException without being stopped drops the stored ids`() =
        runTest {
            coEvery { mangaRepository.getMangaByIds(any()) } throws CancellationException("timeout")
            StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))

            assertEquals(Result.failure(), runJob(queuedRequest.captured.workSpec.input))
            assertEquals("", storedPushIds)
        }

    @Test
    fun `a push that fails after WorkManager stopped it keeps its ids`() = runTest {
        StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))
        val job = newJob(queuedRequest.captured.workSpec.input)
        coEvery { mangaRepository.getMangaByIds(any()) } answers
            {
                job.stop(WorkInfo.STOP_REASON_PREEMPT)
                throw IOException("Canceled")
            }

        job.doWork()

        assertEquals("5,7", storedPushIds)
    }

    @Test
    fun `a push while logged out drops the stored ids`() = runTest {
        every { loginHelper.isLoggedIn() } returns false
        StatusSyncJob.startSelectedNow(workManager, listOf(5L, 7L))

        assertEquals(Result.failure(), runJob(queuedRequest.captured.workSpec.input))
        assertEquals("", storedPushIds)
    }

    @Test
    fun `cancelling the sync from its notification drops the stored ids`() {
        every { workManager.cancelUniqueWork(any()) } returns mockk()
        storedPushIds = "5,7"

        StatusSyncJob.stop(context)

        verify(exactly = 1) { workManager.cancelUniqueWork(StatusSyncJob.TAG) }
        assertEquals("", storedPushIds)
    }

    private suspend fun runJob(input: Data): Result = newJob(input).doWork()

    private fun newJob(input: Data): StatusSyncJob {
        val params = mockk<WorkerParameters>(relaxed = true) { every { inputData } returns input }
        return StatusSyncJob(context, params)
    }
}
