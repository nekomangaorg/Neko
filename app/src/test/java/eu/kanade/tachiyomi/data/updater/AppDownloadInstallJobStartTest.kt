package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The Download button in both update dialogs (main screen and About), the update notification's
 * action and the automatic update check call [AppDownloadInstallJob.start]. It must leave the
 * calling thread free while WorkManager looks up whether a download is running, and still skip a
 * second download of the same version.
 */
class AppDownloadInstallJobStartTest {

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val workInfoReleased = CountDownLatch(1)
    private val context = mockk<Context>(relaxed = true)
    private val preferences = mockk<SharedPreferences>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(any()) } returns preferences
    }

    @After
    fun tearDown() {
        workInfoReleased.countDown()
        unmockkAll()
    }

    @Test(timeout = 10_000)
    fun `start leaves the calling thread free while WorkManager is busy`() {
        // WorkManager answers state queries on its task executor, which can be busy
        every { workManager.getWorkInfosForUniqueWork(WORK_NAME) } returns
            workInfos {
                workInfoReleased.await()
                emptyList()
            }

        assertCallingThreadStaysFree {
            AppDownloadInstallJob.start(context, URL, true, version = VERSION)
        }
    }

    @Test
    fun `start enqueues the download when none is running`() = runBlocking {
        every { workManager.getWorkInfosForUniqueWork(WORK_NAME) } returns workInfos { emptyList() }

        AppDownloadInstallJob.start(context, URL, true, version = VERSION)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `start skips a second download of the version already downloading`() = runBlocking {
        every { workManager.getWorkInfosForUniqueWork(WORK_NAME) } returns
            workInfos { listOf(running()) }
        every { preferences.getString(AppDownloadInstallJob.DOWNLOADING_VERSION_KEY, null) } returns
            VERSION

        AppDownloadInstallJob.start(context, URL, true, version = VERSION)

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any<String>(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `start replaces a running download of another version`() = runBlocking {
        every { workManager.getWorkInfosForUniqueWork(WORK_NAME) } returns
            workInfos { listOf(running()) }
        every { preferences.getString(AppDownloadInstallJob.DOWNLOADING_VERSION_KEY, null) } returns
            OLDER_VERSION

        AppDownloadInstallJob.start(context, URL, true, version = VERSION)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    /**
     * Runs [call] on a single thread that stands in for the main thread, then checks that the
     * thread can run other work while WorkManager has not answered.
     */
    private fun assertCallingThreadStaysFree(call: suspend () -> Unit) {
        val callingThread = Executors.newSingleThreadExecutor()
        val job = CoroutineScope(callingThread.asCoroutineDispatcher()).launch { call() }
        try {
            val otherWorkRan = CountDownLatch(1)
            callingThread.execute { otherWorkRan.countDown() }
            assertTrue(
                "the calling thread waited for WorkManager",
                otherWorkRan.await(5, TimeUnit.SECONDS),
            )
        } finally {
            workInfoReleased.countDown()
            runBlocking { withTimeout(5_000) { job.join() } }
            callingThread.shutdown()
        }
    }

    private fun running() = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }

    private fun workInfos(result: () -> List<WorkInfo>) =
        mockk<ListenableFuture<List<WorkInfo>>> { every { get() } answers { result() } }

    private companion object {
        const val WORK_NAME = "AppDownloadInstaller"
        const val URL = "https://example.org/neko.apk"
        const val VERSION = "v3.8.5"
        const val OLDER_VERSION = "v3.8.4"
    }
}
