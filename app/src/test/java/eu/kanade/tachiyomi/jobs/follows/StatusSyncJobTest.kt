package eu.kanade.tachiyomi.jobs.follows

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.michaelbull.result.Ok
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(FollowsSyncProcessor())
        Injekt.addSingleton(mockk<MangaDexLoginHelper> { every { isLoggedIn() } returns true })
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
    fun `an empty selection queues nothing`() {
        StatusSyncJob.startSelectedNow(workManager, emptyList())

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `blank selected ids push nothing and not the whole library`() = runTest {
        val blankIds = Data.Builder().putString("selected_manga_ids", "").build()

        assertEquals(Result.success(), runJob(blankIds))

        coVerify(exactly = 0) { mangaRepository.getMangaByIds(any()) }
        coVerify(exactly = 0) { mangaRepository.getLibraryList() }
    }

    @Test
    fun `a job with no input pushes and pulls nothing`() = runTest {
        assertEquals(Result.success(), runJob(Data.EMPTY))

        coVerify(exactly = 0) { mangaRepository.getMangaByIds(any()) }
        coVerify(exactly = 0) { mangaRepository.getLibraryList() }
        coVerify(exactly = 0) { mangaDex.fetchAllFollows() }
    }

    private suspend fun runJob(input: Data): Result {
        val params = mockk<WorkerParameters>(relaxed = true) { every { inputData } returns input }
        return StatusSyncJob(context, params).doWork()
    }
}
