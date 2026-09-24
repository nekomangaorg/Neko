package eu.kanade.tachiyomi.data.backup

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        context = mockk(relaxed = true)
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
}
