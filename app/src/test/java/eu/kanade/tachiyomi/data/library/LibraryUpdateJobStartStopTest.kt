package eu.kanade.tachiyomi.data.library

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.UUID
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
 * Pull-to-refresh, the category refresh button and the Cancel action on the update notification end
 * up in [LibraryUpdateJob.startNow] and [LibraryUpdateJob.stop]. Both must leave the calling thread
 * free while WorkManager looks up the update's state.
 */
class LibraryUpdateJobStartStopTest {

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val workInfoReleased = CountDownLatch(1)
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
    }

    @After
    fun tearDown() {
        workInfoReleased.countDown()
        unmockkAll()
    }

    @Test(timeout = 10_000)
    fun `startNow leaves the calling thread free while WorkManager is busy`() {
        // WorkManager answers state queries on its task executor, which can be busy
        every { workManager.getWorkInfosByTag(LibraryUpdateJob.TAG) } returns
            workInfos {
                workInfoReleased.await()
                emptyList()
            }

        assertCallingThreadStaysFree { LibraryUpdateJob.startNow(context) }
    }

    @Test(timeout = 10_000)
    fun `stop leaves the calling thread free while WorkManager is busy`() {
        every { workManager.getWorkInfos(any<WorkQuery>()) } returns
            workInfos {
                workInfoReleased.await()
                emptyList()
            }

        assertCallingThreadStaysFree { LibraryUpdateJob.stop(context) }
    }

    @Test
    fun `startNow enqueues the manual update when no update is running`() = runBlocking {
        every { workManager.getWorkInfosByTag(LibraryUpdateJob.TAG) } returns
            workInfos { emptyList() }

        LibraryUpdateJob.startNow(context)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                MANUAL_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `startNow does not enqueue another update while one is running`() = runBlocking {
        val running = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }
        every { workManager.getWorkInfosByTag(LibraryUpdateJob.TAG) } returns
            workInfos { listOf(running) }

        LibraryUpdateJob.startNow(context)

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any<String>(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `stop cancels the running update`() = runBlocking {
        val workId = UUID.randomUUID()
        val running =
            mockk<WorkInfo> {
                every { id } returns workId
                every { tags } returns setOf(LibraryUpdateJob.TAG, MANUAL_WORK_NAME)
            }
        every { workManager.getWorkInfos(any<WorkQuery>()) } returns workInfos { listOf(running) }

        LibraryUpdateJob.stop(context)

        verify(exactly = 1) { workManager.cancelWorkById(workId) }
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

    private fun workInfos(result: () -> List<WorkInfo>) =
        mockk<ListenableFuture<List<WorkInfo>>> { every { get() } answers { result() } }

    private companion object {
        const val MANUAL_WORK_NAME = "LibraryUpdate-manual"
    }
}
