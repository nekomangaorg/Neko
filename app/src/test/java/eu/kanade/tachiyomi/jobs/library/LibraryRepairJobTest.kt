package eu.kanade.tachiyomi.jobs.library

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.tracking.DelayedTrackingUpdateJob
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.nekomanga.domain.track.store.DelayedTrackingStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class LibraryRepairJobTest {

    @get:Rule val cacheDir = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val gate = LibraryRepairGate(mockk(relaxed = true), showToast = {})
    private val repairer = mockk<LibraryRepairer>()
    private val store = mockk<DelayedTrackingStore>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val trackManager =
        mockk<TrackManager> {
            every { getService(any()) } returns null
            every { getService(1) } returns mockk { every { nameRes() } returns MAL_NAME }
        }

    /** Tags whose work WorkManager reports as running. */
    private val runningTags = mutableSetOf<String>()

    @Before
    fun setUp() {
        Injekt = InjektScope(DefaultRegistrar())
        every { workManager.getWorkInfosByTag(any()) } answers
            {
                val infos =
                    if (firstArg<String>() in runningTags) listOf(running()) else emptyList()
                mockk<ListenableFuture<List<WorkInfo>>> { every { get() } returns infos }
            }
        Injekt.addSingleton(workManager)
        Injekt.addSingleton(
            mockk<DownloadManager> {
                every { queueState } returns MutableStateFlow<List<Download>>(emptyList())
            },
        )
        Injekt.addSingleton(gate)
        Injekt.addSingleton(repairer)
        Injekt.addSingleton(store)
        Injekt.addSingleton(trackManager)
        every { store.getItems() } returns emptyList()
        every { context.externalCacheDir } returns cacheDir.root
        every { context.getString(MAL_NAME) } returns "MyAnimeList"
        mockkObject(DelayedTrackingUpdateJob.Companion)
        justRun { DelayedTrackingUpdateJob.setupTask(any()) }

        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        every { any<Context>().notificationBuilder(any(), any()) } returns mockk(relaxed = true)
        every { any<Context>().notificationManager } returns mockk(relaxed = true)
        coEvery { any<CoroutineWorker>().tryToSetForeground() } just Runs

        mockkConstructor(LibraryRepairNotifier::class)
        every { anyConstructed<LibraryRepairNotifier>().showProgress(any(), any()) } returns
            mockk(relaxed = true)
        justRun { anyConstructed<LibraryRepairNotifier>().showBlocked(any()) }
        justRun { anyConstructed<LibraryRepairNotifier>().showComplete(any(), any()) }
        justRun { anyConstructed<LibraryRepairNotifier>().showStopped(any(), any()) }
        justRun { anyConstructed<LibraryRepairNotifier>().showError(any()) }
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given no other work when doWork then the repair runs and the gate is free`() = runTest {
        coEvery { repairer.repair(any(), any()) } just Runs

        val result = job().doWork()

        coVerify(exactly = 1) { repairer.repair(any(), any()) }
        verify { anyConstructed<LibraryRepairNotifier>().showComplete(any(), null) }
        result shouldBe Result.success()
        gate.isRunning shouldBe false
    }

    @Test
    fun `given a library update running when doWork then nothing is repaired and the gate is free`() =
        runTest {
            runningTags += BlockingWork.LIBRARY_UPDATE.tag

            val result = job().doWork()

            coVerify(exactly = 0) { repairer.repair(any(), any()) }
            verify {
                anyConstructed<LibraryRepairNotifier>()
                    .showBlocked(listOf(BlockingWork.LIBRARY_UPDATE))
            }
            result shouldBe Result.success()
            gate.isRunning shouldBe false
        }

    @Test
    fun `given the repair is cancelled when doWork then the gate is free`() = runTest {
        coEvery { repairer.repair(any(), any()) } throws CancellationException("stopped")

        val error = runCatching { job().doWork() }.exceptionOrNull()

        error.shouldBeInstanceOf<CancellationException>()
        verify { anyConstructed<LibraryRepairNotifier>().showStopped(any(), null) }
        gate.isRunning shouldBe false
    }

    @Test
    fun `given the gate is already held when doWork then the repair does not run`() = runTest {
        gate.tryAcquire()

        val result = job().doWork()

        coVerify(exactly = 0) { repairer.repair(any(), any()) }
        result shouldBe Result.success()
        gate.isRunning shouldBe true
    }

    @Test
    fun `given the repair fails when doWork then the error is shown and the gate is free`() =
        runTest {
            coEvery { repairer.repair(any(), any()) } throws IllegalStateException("disk full")

            val result = job().doWork()

            verify { anyConstructed<LibraryRepairNotifier>().showError("disk full") }
            result shouldBe Result.failure()
            gate.isRunning shouldBe false
        }

    @Test
    fun `given delayed tracking items when the repair ends then their job is queued after the gate is free`() =
        runTest {
            every { store.getItems() } returns
                listOf(DelayedTrackingStore.DelayedTrackingItem(trackId = 8L, lastChapterRead = 9f))
            coEvery { repairer.repair(any(), any()) } just Runs
            val gateHeld = mutableListOf<Boolean>()
            every { DelayedTrackingUpdateJob.setupTask(any()) } answers
                {
                    gateHeld += gate.isRunning
                }

            job().doWork()

            gateHeld shouldBe listOf(false)
        }

    @Test
    fun `given no delayed tracking items when the repair ends then no tracking job is queued`() =
        runTest {
            coEvery { repairer.repair(any(), any()) } just Runs

            job().doWork()

            verify(exactly = 0) { DelayedTrackingUpdateJob.setupTask(any()) }
        }

    @Test
    fun `given delayed tracking running when doWork then no tracking job is queued`() = runTest {
        runningTags += BlockingWork.DELAYED_TRACKING.tag
        every { store.getItems() } returns
            listOf(DelayedTrackingStore.DelayedTrackingItem(trackId = 8L, lastChapterRead = 9f))

        job().doWork()

        verify(exactly = 0) { DelayedTrackingUpdateJob.setupTask(any()) }
    }

    @Test
    fun `given skipped, failed and conflicting titles when the repair ends then the log lists them by section`() =
        runTest {
            coEvery { repairer.repair(any(), any()) } answers
                {
                    val result = firstArg<RepairResult>()
                    result.skipped += SkippedEntry("Gone", SkipReason.NotMangaDex)
                    result.skipped +=
                        SkippedEntry("Split", SkipReason.DownloadFolders(listOf("A", "B")))
                    result.failed += "Broken: disk full"
                    result.trackingConflicts += TrackingConflict("Real", serviceId = 1)
                }

            job().doWork()

            val log = File(cacheDir.root, "neko_repair.log")
            log.readLines() shouldBe
                listOf(
                    "Skipped",
                    "Gone: not a MangaDex url or source",
                    "Split: more than one download folder exists (A, B)",
                    "",
                    "Failed",
                    "Broken: disk full",
                    "",
                    "Tracking conflicts",
                    "Real: MyAnimeList",
                )
            verify { anyConstructed<LibraryRepairNotifier>().showComplete(any(), log) }
        }

    @Test
    fun `given calls closer than the interval when tryPass then one call passes per interval`() {
        var now = 0L
        val throttle = ProgressThrottle(intervalMs = 1000L) { now }

        val passed =
            listOf(0L, 400L, 999L, 1000L, 1500L, 2100L).map {
                now = it
                throttle.tryPass()
            }

        passed shouldBe listOf(true, false, false, true, false, true)
    }

    private fun job() = LibraryRepairJob(context, mockk(relaxed = true))

    private fun running() = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }

    private companion object {
        const val MAL_NAME = 4242
    }
}
