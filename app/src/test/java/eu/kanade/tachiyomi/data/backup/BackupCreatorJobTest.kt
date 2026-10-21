package eu.kanade.tachiyomi.data.backup

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class BackupCreatorJobTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private val workManager = mockk<WorkManager>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        context = mockk(relaxed = true)
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        // The periodic backup request carries no location
        workerParams = mockk(relaxed = true) { every { inputData } returns Data.EMPTY }

        val storageManager =
            mockk<StorageManager>(relaxed = true) {
                every { getBackupDirectory() } returns null
                every { getAutomaticBackupsDirectory() } returns null
            }
        Injekt.addSingleton(storageManager)

        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        every { any<Context>().notificationBuilder(any(), any()) } returns mockk(relaxed = true)
        every { any<Context>().notificationManager } returns mockk(relaxed = true)

        mockkConstructor(BackupCreator::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given automatic backup and no backup folder when doWork then returns failure`() = runTest {
        coEvery { anyConstructed<BackupCreator>().createBackup(any(), any(), any()) } throws
            IllegalStateException("Couldn't create automatic backup folder")

        val result = BackupCreatorJob(context, workerParams).doWork()

        assertEquals(Result.failure(), result)
        coVerify(exactly = 1) { anyConstructed<BackupCreator>().createBackup(any(), any(), true) }
    }

    @Test
    fun `given queued manual backup when isManualJobRunningFlow then emits true`() = runTest {
        every { workManager.getWorkInfosByTagFlow(MANUAL_TAG) } returns
            flowOf(listOf(workInfo(WorkInfo.State.ENQUEUED)))

        assertTrue(BackupCreatorJob.isManualJobRunningFlow(context).first())
    }

    @Test
    fun `given finished manual backup when isManualJobRunningFlow then emits false`() = runTest {
        every { workManager.getWorkInfosByTagFlow(MANUAL_TAG) } returns
            flowOf(listOf(workInfo(WorkInfo.State.SUCCEEDED)))

        assertFalse(BackupCreatorJob.isManualJobRunningFlow(context).first())
    }

    @Test
    fun `given running manual backup when isManualJobRunningFlow then emits true`() = runTest {
        every { workManager.getWorkInfosByTagFlow(MANUAL_TAG) } returns
            flowOf(listOf(workInfo(WorkInfo.State.RUNNING)))

        assertTrue(BackupCreatorJob.isManualJobRunningFlow(context).first())
    }

    @Test
    fun `given active running and previous finished backup when isManualJobRunningFlow then emits true`() =
        runTest {
            every { workManager.getWorkInfosByTagFlow(MANUAL_TAG) } returns
                flowOf(listOf(workInfo(WorkInfo.State.SUCCEEDED), workInfo(WorkInfo.State.RUNNING)))

            assertTrue(BackupCreatorJob.isManualJobRunningFlow(context).first())
        }

    @Test
    fun `given no backups scheduled when isManualJobRunningFlow then emits false`() = runTest {
        every { workManager.getWorkInfosByTagFlow(MANUAL_TAG) } returns flowOf(emptyList())

        assertFalse(BackupCreatorJob.isManualJobRunningFlow(context).first())
    }

    private fun workInfo(workState: WorkInfo.State) =
        mockk<WorkInfo> { every { state } returns workState }

    private companion object {
        const val MANUAL_TAG = "BackupCreator:manual"
    }
}
