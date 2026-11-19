package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.Closeable
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/**
 * Runs the update download against a local server that sends the APK slowly. The update APK is
 * about 40 MB, so on a slow connection it takes longer to download than the shared client's
 * one-minute call timeout. [sharedClient] stands in for that client with a shorter timeout.
 */
class AppDownloadInstallJobDownloadTest {

    @get:Rule val folder = TemporaryFolder()

    private val sharedClient =
        OkHttpClient.Builder().callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS).build()
    private lateinit var context: Context
    private var server: SlowApkServer? = null

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        val network = mockk<NetworkHelper> { every { client } returns sharedClient }
        Injekt.addSingleton(network)
        Injekt.addSingleton(mockk<PreferencesHelper>(relaxed = true))
        context = mockk(relaxed = true) { every { externalCacheDir } returns folder.root }

        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(any()) } returns
            mockk<SharedPreferences>(relaxed = true)
        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        coEvery { any<CoroutineWorker>().tryToSetForeground() } just runs
        mockkStatic("eu.kanade.tachiyomi.util.storage.FileExtensionsKt")
        every { any<File>().getUriCompat(any()) } returns mockk(relaxed = true)

        mockkConstructor(AppUpdateNotifier::class)
        every { anyConstructed<AppUpdateNotifier>().onProgressChange(any()) } just runs
        every { anyConstructed<AppUpdateNotifier>().onDownloadFinished(any()) } just runs
        every { anyConstructed<AppUpdateNotifier>().onDownloadError(any(), any()) } just runs
        every { anyConstructed<AppUpdateNotifier>().cancel() } just runs
    }

    @After
    fun tearDown() {
        server?.close()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given the apk takes longer than the call timeout when doWork then the download finishes`() =
        runBlocking {
            val server = SlowApkServer(APK_SIZE, BODY_DURATION_MS).also { server = it }
            val start = System.nanoTime()

            AppDownloadInstallJob(context, workerParams(server.url)).doWork()

            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            verify(exactly = 0) {
                anyConstructed<AppUpdateNotifier>().onDownloadError(any(), any())
            }
            verify(exactly = 1) { anyConstructed<AppUpdateNotifier>().onDownloadFinished(any()) }
            assertEquals(APK_SIZE.toLong(), File(folder.root, "update.apk").length())
            assertTrue("the download ended within the call timeout", elapsedMs > CALL_TIMEOUT_MS)
        }

    @Test
    fun `given stop cancels the download when doWork then no download error is shown`() =
        runBlocking {
            val server = SlowApkServer(APK_SIZE, BODY_DURATION_MS).also { server = it }
            val downloading = CountDownLatch(1)
            every { anyConstructed<AppUpdateNotifier>().onProgressChange(any()) } answers
                {
                    downloading.countDown()
                }
            mockkObject(WorkManager.Companion)
            every { WorkManager.getInstance(any<Context>()) } returns mockk(relaxed = true)

            val download =
                async(Dispatchers.IO) {
                    AppDownloadInstallJob(context, workerParams(server.url)).doWork()
                }
            assertTrue("the download did not start", downloading.await(5, TimeUnit.SECONDS))
            // The Cancel action on the download notification calls stop(), which cancels the call
            // before WorkManager stops the job
            AppDownloadInstallJob.stop(context)
            download.await()

            verify(exactly = 0) {
                anyConstructed<AppUpdateNotifier>().onDownloadError(any(), any())
            }
            verify { anyConstructed<AppUpdateNotifier>().cancel() }
        }

    private fun workerParams(url: String) =
        mockk<WorkerParameters>(relaxed = true) {
            every { inputData } returns
                Data.Builder()
                    .putString(AppDownloadInstallJob.EXTRA_DOWNLOAD_URL, url)
                    .putString(AppDownloadInstallJob.EXTRA_VERSION, VERSION)
                    .build()
        }

    /** Answers one request with [size] bytes, sent in [PIECES] pieces spread over [durationMs]. */
    private class SlowApkServer(size: Int, durationMs: Long) : Closeable {
        private val socket = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        val url = "http://127.0.0.1:${socket.localPort}/neko.apk"

        private val worker =
            thread(isDaemon = true) {
                runCatching {
                    socket.accept().use { connection ->
                        val request = connection.getInputStream().bufferedReader()
                        while (!request.readLine().isNullOrEmpty()) {
                            // Skip the request line and headers
                        }
                        val response = connection.getOutputStream()
                        response.write(
                            ("HTTP/1.1 200 OK\r\nContent-Length: $size\r\n" +
                                    "Connection: close\r\n\r\n")
                                .toByteArray(),
                        )
                        val piece = ByteArray(size / PIECES)
                        repeat(PIECES) {
                            Thread.sleep(durationMs / PIECES)
                            response.write(piece)
                            response.flush()
                        }
                    }
                }
            }

        override fun close() {
            socket.close()
            worker.join(5_000)
        }
    }

    private companion object {
        const val CALL_TIMEOUT_MS = 500L
        const val BODY_DURATION_MS = 1_600L
        const val PIECES = 8
        const val APK_SIZE = 64 * 1024
        const val VERSION = "v9.9.9"
    }
}
