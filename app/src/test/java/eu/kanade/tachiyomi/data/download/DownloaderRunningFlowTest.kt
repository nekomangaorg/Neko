package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/** DownloadJob waits on [Downloader.isRunningFlow], so it has to follow [Downloader.isRunning]. */
class DownloaderRunningFlowTest {

    private lateinit var downloader: Downloader
    private lateinit var mangaRepository: MangaRepository

    private val manga = Manga.create("/title/1", "Manga").apply { id = 1L }

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton<Json>(Json)
        Injekt.addSingleton(mockk<ChapterItemFilter>())
        Injekt.addSingleton(mockk<PreferencesHelper>())
        // Downloads never get past their first step, so a started downloader stays running.
        mangaRepository = mockk {
            coEvery { getMangaById(any()) } coAnswers { awaitCancellation() }
        }
        Injekt.addSingleton(mangaRepository)
        Injekt.addSingleton(
            mockk<MangaDetailsPreferences> {
                every { sortChapterOrder().get() } returns Manga.CHAPTER_SORTING_SOURCE
            }
        )

        val activeDownloads =
            mockk<SharedPreferences>(relaxed = true) {
                every { all } returns emptyMap<String, Any>()
            }
        val context =
            mockk<Context>(relaxed = true) {
                every { getSharedPreferences(any(), any()) } returns activeDownloads
            }

        mockkObject(DownloadJob.Companion)
        every { DownloadJob.start(any()) } just runs
        every { DownloadJob.stop(any()) } just runs
        mockkObject(LibraryUpdateJob.Companion)
        every { LibraryUpdateJob.isRunning(any()) } returns false
        mockkConstructor(DownloadNotifier::class)
        every { anyConstructed<DownloadNotifier>().onPaused() } just runs
        every { anyConstructed<DownloadNotifier>().onComplete() } just runs
        every { anyConstructed<DownloadNotifier>().dismissProgress() } just runs

        val provider =
            mockk<DownloadProvider> {
                every { findMangaDir(any()) } returns null
                every { chapterDirDoesNotExist(any(), any()) } returns true
            }
        val sourceManager = mockk<SourceManager> { every { mangaDex } returns mockk() }
        downloader = Downloader(context, provider, mockk(), sourceManager)
    }

    @After
    fun tearDown() {
        downloader.pause()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `reads running once started`() {
        assertFalse(downloader.isRunningFlow.value)

        startDownloader()

        assertTrue(downloader.isRunningFlow.value)
    }

    @Test
    fun `reads stopped after a stop`() {
        startDownloader()

        downloader.stop()

        assertFalse(downloader.isRunningFlow.value)
    }

    @Test
    fun `reads stopped after a pause`() {
        startDownloader()

        downloader.pause()

        assertFalse(downloader.isRunningFlow.value)
    }

    @Test
    fun `reads stopped after the queue is cleared`() {
        startDownloader()

        downloader.clearQueue()

        assertFalse(downloader.isRunningFlow.value)
    }

    @Test
    fun `reads stopped once the last download finishes`() {
        val finishDownload = CompletableDeferred<Unit>()
        coEvery { mangaRepository.getMangaById(1L) } coAnswers
            {
                finishDownload.await()
                downloader.queueState.value.single().status = Download.State.DOWNLOADED
                null
            }
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        downloader.start()
        assertTrue(downloader.isRunningFlow.value)

        finishDownload.complete(Unit)

        waitUntil { !downloader.isRunning }
        assertFalse(downloader.isRunningFlow.value)
    }

    @Test
    fun `reads running after a restart`() {
        startDownloader()

        downloader.removeFromQueueAndRestart(listOf(chapter(1)))

        assertTrue(downloader.isRunning)
        assertTrue(downloader.isRunningFlow.value)
    }

    @Test
    fun `reads stopped when the downloader job ends without a stop`() {
        startDownloader()

        // Nothing in Downloader ends the job this way today; a failure escaping the job would.
        val field = Downloader::class.java.getDeclaredField("downloaderJob")
        field.isAccessible = true
        (field.get(downloader) as Job).cancel()

        waitUntil { !downloader.isRunningFlow.value }
    }

    private fun startDownloader() {
        downloader.queueChapters(manga, listOf(chapter(1), chapter(2)), autoStart = true)
        downloader.start()
        check(downloader.isRunning)
        assertTrue(downloader.isRunningFlow.value)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition not met within 10 s" }
            Thread.sleep(10)
        }
    }

    private fun chapter(id: Long): Chapter =
        Chapter.create().apply {
            this.id = id
            manga_id = manga.id
            url = "/chapter/$id"
            name = "Ch.$id"
        }
}
