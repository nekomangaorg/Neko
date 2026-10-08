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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class DownloaderQueueChaptersTest {

    private lateinit var context: Context
    private lateinit var downloader: Downloader
    private lateinit var mangaRepository: MangaRepository
    private lateinit var provider: DownloadProvider

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
        context =
            mockk(relaxed = true) {
                every { getSharedPreferences(any(), any()) } returns activeDownloads
            }

        mockkObject(DownloadJob.Companion)
        every { DownloadJob.start(any()) } just runs
        mockkObject(LibraryUpdateJob.Companion)
        every { LibraryUpdateJob.isRunning(any()) } returns false
        mockkConstructor(DownloadNotifier::class)
        every { anyConstructed<DownloadNotifier>().onPaused() } just runs

        provider =
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
    fun `starts the downloader when the queue only holds errored downloads`() {
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        // The download failed and the downloader stopped, which leaves the entry in the queue.
        downloader.queueState.value.single().status = Download.State.ERROR

        downloader.queueChapters(manga, listOf(chapter(2)), autoStart = true)

        verify(exactly = 2) { DownloadJob.start(context) }
        verify(exactly = 0) { anyConstructed<DownloadNotifier>().onPaused() }
    }

    @Test
    fun `keeps a paused queue paused`() {
        // DownloadJob is mocked, so the first chapter stays queued with the downloader not
        // running, the same state pausing the downloads leaves behind.
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)

        downloader.queueChapters(manga, listOf(chapter(2)), autoStart = true)

        verify(exactly = 1) { DownloadJob.start(context) }
        verify(exactly = 1) { anyConstructed<DownloadNotifier>().onPaused() }
    }

    @Test
    fun `leaves a running downloader to pick up new chapters`() {
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        downloader.start()
        // Same state as deleting a manga's downloads while one of them downloads and another
        // download sits errored. Cancelling that download does not stop the downloader, so it keeps
        // running with nothing pending.
        downloader.queueState.value.single().status = Download.State.ERROR

        val otherManga = Manga.create("/title/2", "Other").apply { id = 2L }
        downloader.queueChapters(otherManga, listOf(chapter(2, otherManga)), autoStart = true)

        coVerify(timeout = 10_000) { mangaRepository.getMangaById(2L) }
        verify(exactly = 1) { DownloadJob.start(context) }
        verify(exactly = 0) { anyConstructed<DownloadNotifier>().onPaused() }
    }

    @Test
    fun `starts a chapter queued while the last download finishes`() {
        every { DownloadJob.stop(any()) } just runs
        every { anyConstructed<DownloadNotifier>().onComplete() } just runs
        val lastDownloadDone = CompletableDeferred<Unit>()
        coEvery { mangaRepository.getMangaById(1L) } coAnswers
            {
                lastDownloadDone.await()
                downloader.queueState.value.single().status = Download.State.DOWNLOADED
                null
            }
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        downloader.start()

        // Listing the manga folder takes a while on SAF storage. The running download finishes and
        // the downloader stops while the second chapter is still being looked up.
        val otherManga = Manga.create("/title/2", "Other").apply { id = 2L }
        every { provider.findMangaDir(match { it.id == 2L }) } answers
            {
                lastDownloadDone.complete(Unit)
                waitUntil { !downloader.isRunning }
                null
            }
        downloader.queueChapters(otherManga, listOf(chapter(2, otherManga)), autoStart = true)

        verify(exactly = 2) { DownloadJob.start(context) }
        verify(exactly = 0) { anyConstructed<DownloadNotifier>().onPaused() }
    }

    @Test
    fun `starts the job for a chapter queued during stop after the stop`() {
        val jobCalls = Collections.synchronizedList(mutableListOf<String>())
        every { DownloadJob.start(any()) } answers { jobCalls += "start" }
        every { DownloadJob.stop(any()) } answers { jobCalls += "stop" }
        val stopping = CountDownLatch(1)
        val finishStop = CountDownLatch(1)
        every { anyConstructed<DownloadNotifier>().onComplete() } answers
            {
                stopping.countDown()
                finishStop.await(10, TimeUnit.SECONDS)
            }
        coEvery { mangaRepository.getMangaById(1L) } coAnswers
            {
                downloader.queueState.value.single().status = Download.State.DOWNLOADED
                null
            }
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        downloader.start()
        // The last download finished, and its stop() has cancelled the downloader but not
        // DownloadJob yet.
        check(stopping.await(10, TimeUnit.SECONDS)) { "the downloader did not stop" }

        val otherManga = Manga.create("/title/2", "Other").apply { id = 2L }
        var error: Throwable? = null
        val queueing = thread {
            try {
                downloader.queueChapters(
                    otherManga,
                    listOf(chapter(2, otherManga)),
                    autoStart = true,
                )
            } catch (e: Throwable) {
                error = e
            }
        }
        // Either the chapter is queued already, or queueChapters waits for the stop to finish.
        waitUntil {
            !queueing.isAlive ||
                queueing.state == Thread.State.BLOCKED &&
                    queueing.stackTrace.firstOrNull()?.methodName == "queueChapters"
        }
        finishStop.countDown()
        queueing.join(10_000)
        error?.let { throw it }

        // A start before the stop would be cancelled by it, leaving the chapter with no job.
        assertEquals(listOf("start", "stop", "start"), jobCalls.toList())
    }

    @Test
    fun `stops a downloader whose last download finishes before start returns`() {
        every { DownloadJob.stop(any()) } just runs
        every { anyConstructed<DownloadNotifier>().onComplete() } just runs
        // Unconfined runs the downloader job inside launch, so its only download finishes and the
        // downloader stops before start() returns.
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns Dispatchers.Unconfined
        downloader.pause()
        downloader =
            Downloader(context, provider, mockk(), mockk { every { mangaDex } returns mockk() })
        coEvery { mangaRepository.getMangaById(1L) } coAnswers
            {
                downloader.queueState.value.single().status = Download.State.DOWNLOADED
                null
            }
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)

        downloader.start()

        assertFalse(downloader.isRunning)
        val otherManga = Manga.create("/title/2", "Other").apply { id = 2L }
        downloader.queueChapters(otherManga, listOf(chapter(2, otherManga)), autoStart = true)
        verify(exactly = 2) { DownloadJob.start(context) }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition not met within 10 s" }
            Thread.sleep(10)
        }
    }

    private fun chapter(id: Long, manga: Manga = this.manga): Chapter =
        Chapter.create().apply {
            this.id = id
            manga_id = manga.id
            url = "/chapter/$id"
            name = "Ch.$id"
        }
}
