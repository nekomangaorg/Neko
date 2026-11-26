package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.jobs.library.LibraryRepairGate
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class BackupRestoreJobTest {

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>()
    private val gate = LibraryRepairGate(context, showToast = {})

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(gate)
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        coEvery { any<CoroutineWorker>().tryToSetForeground() } just runs
        every { any<Context>().notificationBuilder(any(), any()) } returns mockk(relaxed = true)
        every { any<Context>().notificationManager } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given queued restore when isRunningFlow then emits true`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME) } returns
            flowOf(listOf(workInfo(WorkInfo.State.ENQUEUED)))

        assertTrue(BackupRestoreJob.isRunningFlow(context).first())
    }

    @Test
    fun `given finished restore when isRunningFlow then emits false`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME) } returns
            flowOf(listOf(workInfo(WorkInfo.State.SUCCEEDED)))

        assertFalse(BackupRestoreJob.isRunningFlow(context).first())
    }

    @Test
    fun `given running restore when isRunningFlow then emits true`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME) } returns
            flowOf(listOf(workInfo(WorkInfo.State.RUNNING)))

        assertTrue(BackupRestoreJob.isRunningFlow(context).first())
    }

    @Test
    fun `given active running and previous finished restore when isRunningFlow then emits true`() =
        runTest {
            every { workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME) } returns
                flowOf(listOf(workInfo(WorkInfo.State.SUCCEEDED), workInfo(WorkInfo.State.RUNNING)))

            assertTrue(BackupRestoreJob.isRunningFlow(context).first())
        }

    @Test
    fun `given no restores scheduled when isRunningFlow then emits false`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME) } returns flowOf(emptyList())

        assertFalse(BackupRestoreJob.isRunningFlow(context).first())
    }

    @Test
    fun `given the repair runs when doWork then it returns retry`() = runTest {
        gate.tryAcquire()
        val params =
            mockk<WorkerParameters>(relaxed = true) { every { inputData } returns Data.EMPTY }

        val result = runCatching { BackupRestoreJob(context, params).doWork() }.getOrNull()

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `given the repair runs when start then nothing is enqueued`() {
        gate.tryAcquire()
        every {
            workManager.enqueueUniqueWork(any<String>(), any(), any<OneTimeWorkRequest>())
        } returns mockk()

        BackupRestoreJob.start(context, mockk<Uri>(relaxed = true))

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any<String>(), any(), any<OneTimeWorkRequest>())
        }
    }

    private fun workInfo(workState: WorkInfo.State) =
        mockk<WorkInfo> { every { state } returns workState }

    private companion object {
        const val WORK_NAME = "BackupRestorer"
    }
}
