package eu.kanade.tachiyomi.data.backup

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRestoreJobTest {

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>()

    @Before
    fun setup() {
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
    }

    @After
    fun tearDown() {
        unmockkAll()
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

    private fun workInfo(workState: WorkInfo.State) =
        mockk<WorkInfo> { every { state } returns workState }

    private companion object {
        const val WORK_NAME = "BackupRestorer"
    }
}
