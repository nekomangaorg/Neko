package eu.kanade.tachiyomi.jobs.tracking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.jobs.library.LibraryRepairGate
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class TrackingSyncJobTest {

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val trackSyncProcessor = mockk<TrackSyncProcessor>(relaxed = true)
    private val gate = LibraryRepairGate(context, showToast = {})

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(gate)
        Injekt.addSingleton(trackSyncProcessor)

        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        mockkObject(NotificationReceiver.Companion)
        every { NotificationReceiver.cancelTrackingSyncPendingIntent(any()) } returns mockk()
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
    fun `given the repair runs when doWork then it returns retry`() = runTest {
        gate.tryAcquire()

        val result = runCatching { TrackingSyncJob(context, mockk(relaxed = true)).doWork() }

        assertEquals(Result.retry(), result.getOrNull())
        coVerify(exactly = 0) { trackSyncProcessor.process(any(), any()) }
    }

    @Test
    fun `given the repair runs when doWorkNow then nothing is enqueued`() {
        gate.tryAcquire()

        TrackingSyncJob.doWorkNow(context)

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any<String>(), any(), any<OneTimeWorkRequest>())
        }
    }
}
