package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.NetworkState
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.networkStateFlow
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class DownloadJobTest {

    private val online = NetworkState(isConnected = true, isValidated = true, isUnmetered = true)
    private val offline =
        NetworkState(isConnected = false, isValidated = false, isUnmetered = false)

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var downloadManager: DownloadManager
    private val networkState = MutableStateFlow(online)

    @Volatile private var downloaderRunning = false

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)

        downloadManager = mockk {
            every { downloaderStart() } answers
                {
                    downloaderRunning = true
                    true
                }
            every { downloaderStop(any()) } answers { downloaderRunning = false }
            every { pauseDownloads() } answers { downloaderRunning = false }
            every { isRunning } answers { downloaderRunning }
        }
        Injekt.addSingleton(downloadManager)
        Injekt.addSingleton(
            mockk<PreferencesHelper> {
                every { downloadOnlyOverUnmetered().get() } returns false
                every { downloadOnlyOverUnmetered().changes() } returns flowOf(false)
            }
        )

        mockkStatic("eu.kanade.tachiyomi.util.system.NetworkStateTrackerKt")
        every { any<Context>().activeNetworkState() } answers { networkState.value }
        every { any<Context>().networkStateFlow() } returns networkState
        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        coEvery { any<CoroutineWorker>().tryToSetForeground() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given network lost while downloading when doWork then worker finishes`() = runTest {
        launch {
            delay(5.seconds)
            networkState.value = offline
        }

        val result = withTimeoutOrNull(1.minutes) { DownloadJob(context, workerParams).doWork() }

        assertEquals(Result.success(), result)
        verify(exactly = 1) { downloadManager.downloaderStop(any()) }
    }

    @Test
    fun `given downloads finish when doWork then worker finishes and stops watching network`() =
        runTest {
            launch {
                delay(5.seconds)
                downloaderRunning = false
            }

            val result =
                withTimeoutOrNull(1.minutes) { DownloadJob(context, workerParams).doWork() }
            networkState.value = offline
            advanceUntilIdle()

            assertEquals(Result.success(), result)
            verify(exactly = 0) { downloadManager.downloaderStop(any()) }
        }

    @Test
    fun `given system stops the worker when doWork then downloader is paused`() = runTest {
        val worker = DownloadJob(context, workerParams)
        val work = async { worker.doWork() }
        delay(5.seconds)

        // WorkManager sets the stop reason before it cancels doWork
        worker.stop(WorkInfo.STOP_REASON_QUOTA)
        work.cancel()
        advanceUntilIdle()

        verify(exactly = 1) { downloadManager.pauseDownloads() }
        assertEquals(false, downloaderRunning)
    }

    @Test
    fun `given worker is replaced when doWork then downloader keeps running`() = runTest {
        val worker = DownloadJob(context, workerParams)
        val work = async { worker.doWork() }
        delay(5.seconds)

        worker.stop(WorkInfo.STOP_REASON_CANCELLED_BY_APP)
        work.cancel()
        advanceUntilIdle()

        verify(exactly = 0) { downloadManager.pauseDownloads() }
        assertEquals(true, downloaderRunning)
    }
}
