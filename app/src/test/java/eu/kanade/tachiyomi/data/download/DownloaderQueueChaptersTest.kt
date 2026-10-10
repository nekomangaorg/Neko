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
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
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

class DownloaderQueueChaptersTest {

    private lateinit var context: Context
    private lateinit var downloader: Downloader
    private lateinit var mangaRepository: MangaRepository
    private lateinit var provider: DownloadProvider
    private lateinit var sourceManager: SourceManager

    /** Keys in the saved queue, kept in step with the writes the downloader makes. */
    private val savedDownloads: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    /** Runs before each write to the saved queue. */
    @Volatile private var onSaveQueue: () -> Unit = {}

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

        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putString(any(), any()) } answers
            {
                savedDownloads += firstArg<String>()
                editor
            }
        every { editor.remove(any()) } answers
            {
                savedDownloads -= firstArg<String>()
                editor
            }
        every { editor.clear() } answers
            {
                savedDownloads.clear()
                editor
            }
        val activeDownloads =
            mockk<SharedPreferences>(relaxed = true) {
                every { all } returns emptyMap<String, Any>()
                every { edit() } answers
                    {
                        onSaveQueue()
                        editor
                    }
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
        sourceManager = mockk { every { mangaDex } returns mockk() }
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
        downloader.pause()
        downloader = Downloader(context, provider, mockk(), sourceManager, Dispatchers.Unconfined)
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

    @Test
    fun `keeps a downloader started while pause cancels the old one`() {
        downloader.pause()
        downloader = Downloader(context, provider, mockk(), sourceManager, Dispatchers.Unconfined)
        val activeDownloads = AtomicInteger()
        val startedDuringPause = AtomicBoolean()
        val starting = AtomicReference<Thread>()
        coEvery { mangaRepository.getMangaById(1L) } coAnswers
            {
                activeDownloads.incrementAndGet()
                try {
                    awaitCancellation()
                } finally {
                    activeDownloads.decrementAndGet()
                    // Unconfined runs this inside pause(), while it cancels the downloader job. A
                    // start() on another thread gets in before pause() is done with the job.
                    if (startedDuringPause.compareAndSet(false, true)) {
                        val thread = thread { downloader.start() }
                        starting.set(thread)
                        waitUntil { !thread.isAlive || thread.state == Thread.State.BLOCKED }
                    }
                }
            }
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        downloader.start()

        downloader.pause()
        starting.get().join(10_000)

        // The second start() comes after the pause, so its downloader is the one running, and
        // pausing again cancels its download.
        assertTrue(downloader.isRunning)
        downloader.pause()
        assertEquals(0, activeDownloads.get())
    }

    @Test
    fun `leaves a downloader started as pause finishes unpaused`() {
        every { DownloadJob.stop(any()) } just runs
        every { anyConstructed<DownloadNotifier>().onComplete() } just runs
        val testThread = Thread.currentThread()
        val armed = AtomicBoolean()
        val starting = AtomicReference<Thread>()
        mockkConstructor(Download::class)
        every { anyConstructed<Download>().status } answers
            {
                // The first status read on this thread once armed is pause() looking for running
                // downloads, after it cancelled the downloader job. A start() on another thread
                // gets in there.
                if (Thread.currentThread() === testThread && armed.compareAndSet(true, false)) {
                    starting.set(runUntilDoneOrBlocked { downloader.start() })
                }
                callOriginal()
            }
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        downloader.start()

        armed.set(true)
        downloader.pause()
        checkNotNull(starting.get()) { "pause() read no status" }.join(10_000)

        // The start() comes after the pause, so the downloader runs unpaused, and stopping it
        // reports the downloads as finished.
        assertTrue(downloader.isRunning)
        downloader.stop()
        verify(exactly = 0) { anyConstructed<DownloadNotifier>().onPaused() }
        verify(exactly = 1) { anyConstructed<DownloadNotifier>().onComplete() }
    }

    @Test
    fun `stops DownloadJob before a downloader started during stop runs`() {
        downloader.pause()
        downloader = Downloader(context, provider, mockk(), sourceManager, Dispatchers.Unconfined)
        val events = Collections.synchronizedList(mutableListOf<String>())
        every { DownloadJob.stop(any()) } answers { events += "job stop" }
        val starting = AtomicReference<Thread>()
        // stop() posts its notification after it cancelled the downloader job and before it stops
        // DownloadJob. A start() on another thread gets in there.
        every { anyConstructed<DownloadNotifier>().onComplete() } answers
            {
                if (starting.get() == null) {
                    starting.set(runUntilDoneOrBlocked { downloader.start() })
                }
            }
        coEvery { mangaRepository.getMangaById(1L) } coAnswers
            {
                events += "download"
                awaitCancellation()
            }
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        downloader.start()
        events.clear()

        downloader.stop()
        checkNotNull(starting.get()) { "stop() posted no notification" }.join(10_000)

        // Unconfined runs the new downloader's download inside start(). It has to come after
        // DownloadJob stopped, or it downloads with no job keeping the app alive.
        assertEquals(listOf("job stop", "download"), events.toList())
    }

    @Test
    fun `saves a chapter queued while the queue is being cleared`() {
        every { anyConstructed<DownloadNotifier>().dismissProgress() } just runs
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = false)
        val queueing = AtomicReference<Thread>()
        // clearQueue() empties the queue first and the saved queue after. A chapter queued on
        // another thread gets in between.
        onSaveQueue = {
            if (calledFrom("clearQueueState") && queueing.get() == null) {
                queueing.set(
                    runUntilDoneOrBlocked {
                        downloader.queueChapters(manga, listOf(chapter(2)), autoStart = false)
                    }
                )
            }
        }

        downloader.clearQueue()
        checkNotNull(queueing.get()) { "clearQueue() saved nothing" }.join(10_000)

        assertEquals(1, downloader.queueState.value.size)
        assertEquals(1, savedDownloads.size)
    }

    /** Runs [block] on a new thread and returns once it is done or waits for a lock. */
    private fun runUntilDoneOrBlocked(block: () -> Unit): Thread {
        val thread = thread { block() }
        waitUntil { !thread.isAlive || thread.state == Thread.State.BLOCKED }
        return thread
    }

    private fun calledFrom(method: String): Boolean =
        Thread.currentThread().stackTrace.any { it.methodName == method }

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
