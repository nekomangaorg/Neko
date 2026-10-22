package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import eu.kanade.tachiyomi.source.SourceManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/**
 * The download queue's Resume button calls [DownloadManager.startDownloads] on the main thread. A
 * downloader started outside DownloadJob runs with no worker holding the foreground service.
 */
class DownloadManagerStartDownloadsTest {

    private val workManager = mockk<WorkManager>()
    private val workInfoReleased = CountDownLatch(1)

    private lateinit var context: Context
    private lateinit var downloadManager: DownloadManager

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(mockk<SourceManager> { every { mangaDex } returns mockk() })
        Injekt.addSingleton(
            mockk<StorageManager> { every { baseDirChanges } returns MutableSharedFlow() }
        )
        Injekt.addSingleton(
            mockk<MangaRepository> { coEvery { getMangaList() } returns emptyList() }
        )

        val preferences =
            mockk<SharedPreferences>(relaxed = true) {
                every { all } returns emptyMap<String, Any>()
            }
        context =
            mockk<Context>(relaxed = true) {
                every { getSharedPreferences(any(), any()) } returns preferences
            }

        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        mockkObject(DownloadJob.Companion)
        every { DownloadJob.start(any()) } just runs
        mockkConstructor(Downloader::class)
        every { anyConstructed<Downloader>().isRunning } returns false
        every { anyConstructed<Downloader>().start() } returns true
        downloadManager = DownloadManager(context)
    }

    @After
    fun tearDown() {
        workInfoReleased.countDown()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test(timeout = 10_000)
    fun `startDownloads does not wait for WorkManager to report the worker state`() {
        // WorkManager answers state queries on its task executor, which can be busy
        every { workManager.getWorkInfosForUniqueWork(any()) } returns
            workInfos {
                workInfoReleased.await()
                emptyList()
            }

        downloadManager.startDownloads()

        verify(exactly = 1) { DownloadJob.start(context) }
    }

    @Test
    fun `startDownloads starts DownloadJob while the last worker still reads as running`() {
        // A worker reads as running until WorkManager records its result, a moment after the
        // downloader stops
        val running = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }
        every { workManager.getWorkInfosForUniqueWork(any()) } returns workInfos { listOf(running) }

        downloadManager.startDownloads()

        verify(exactly = 1) { DownloadJob.start(context) }
        verify(exactly = 0) { anyConstructed<Downloader>().start() }
    }

    @Test
    fun `startDownloads does nothing when downloader is already running`() {
        every { anyConstructed<Downloader>().isRunning } returns true

        downloadManager.startDownloads()

        verify(exactly = 0) { DownloadJob.start(any()) }
    }

    private fun workInfos(result: () -> List<WorkInfo>) =
        mockk<ListenableFuture<List<WorkInfo>>> { every { get() } answers { result() } }
}
