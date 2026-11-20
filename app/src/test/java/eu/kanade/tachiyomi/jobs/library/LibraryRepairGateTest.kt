package eu.kanade.tachiyomi.jobs.library

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class LibraryRepairGateTest {

    private val toasts = mutableListOf<UiText>()
    private val gate = LibraryRepairGate(mockk(relaxed = true), showToast = { toasts += it })

    /** The states WorkManager reports for each tag. A tag missing here has no work. */
    private val workByTag = mutableMapOf<String, List<WorkInfo.State>>()
    private val queue = MutableStateFlow<List<Download>>(emptyList())

    @Before
    fun setUp() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(
            mockk<WorkManager> {
                every { getWorkInfosByTag(any()) } answers
                    {
                        val infos = workByTag[firstArg()].orEmpty().map { workInfo(it) }
                        mockk<ListenableFuture<List<WorkInfo>>> { every { get() } returns infos }
                    }
            },
        )
        Injekt.addSingleton(mockk<DownloadManager> { every { queueState } returns queue })
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given a free gate when tryAcquire twice then only the first succeeds`() {
        gate.tryAcquire() shouldBe true
        gate.tryAcquire() shouldBe false
        gate.isRunning shouldBe true

        gate.release()

        gate.isRunning shouldBe false
    }

    @Test
    fun `given a held gate when refuseStart then it refuses and shows the running toast`() {
        gate.tryAcquire()

        gate.refuseStart() shouldBe true

        (toasts.single() as UiText.StringResource).resourceId shouldBe
            R.string.library_repair_running
    }

    @Test
    fun `given a free gate when refuseStart then it allows and shows nothing`() {
        gate.refuseStart() shouldBe false

        toasts shouldBe emptyList()
    }

    @Test
    fun `given a running library update when blockingWork then it lists LIBRARY_UPDATE only`() =
        runTest {
            workByTag["LibraryUpdate"] = listOf(WorkInfo.State.RUNNING)
            workByTag["Downloader"] = listOf(WorkInfo.State.SUCCEEDED)

            gate.blockingWork() shouldBe listOf(BlockingWork.LIBRARY_UPDATE)
        }

    @Test
    fun `given an enqueued download job and an empty queue when blockingWork then it is empty`() =
        runTest {
            workByTag["Downloader"] = listOf(WorkInfo.State.ENQUEUED)

            gate.blockingWork() shouldBe emptyList()
        }

    @Test
    fun `given a paused download queue when blockingWork then it lists DOWNLOADS`() = runTest {
        queue.value = listOf(mockk())

        gate.blockingWork() shouldBe listOf(BlockingWork.DOWNLOADS)
    }

    private fun workInfo(workState: WorkInfo.State) =
        mockk<WorkInfo> { every { state } returns workState }
}
