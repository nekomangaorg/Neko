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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/**
 * Reordering the queue builds the new queue from the current one. Downloads that change while the
 * reorder runs must come out the way they would without it.
 */
class DownloaderUpdateQueueTest {

    private lateinit var downloader: Downloader
    private lateinit var mangaRepository: MangaRepository

    /** Runs on every queue store write, on the thread that makes it. */
    @Volatile private var onStoreWrite: () -> Unit = {}

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
        val activeDownloads =
            mockk<SharedPreferences>(relaxed = true) {
                every { all } returns emptyMap<String, Any>()
                every { edit() } answers
                    {
                        onStoreWrite()
                        editor
                    }
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
    fun `keeps a chapter queued while a reorder waits for the queue`() {
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        downloader.start()

        val adding = CountDownLatch(1)
        val finishAdding = CountDownLatch(1)
        onStoreWrite = {
            if (Thread.currentThread().name == QUEUEING_THREAD) {
                adding.countDown()
                finishAdding.await(10, TimeUnit.SECONDS)
            }
        }
        var error: Throwable? = null
        val queueing =
            thread(name = QUEUEING_THREAD) {
                try {
                    downloader.queueChapters(manga, listOf(chapter(2)), autoStart = true)
                } catch (e: Throwable) {
                    error = e
                }
            }
        // queueChapters holds the queue lock and has not added the chapter to the queue yet.
        check(adding.await(10, TimeUnit.SECONDS)) { "the chapter was not queued" }

        val reordering = thread { downloader.updateQueue { it.reversed() } }
        waitUntil { !reordering.isAlive || reordering.state == Thread.State.BLOCKED }
        finishAdding.countDown()
        queueing.join(10_000)
        reordering.join(10_000)
        error?.let { throw it }

        assertEquals(listOf(2L, 1L), downloader.queueState.value.map { it.chapterItem.id })
    }

    @Test
    fun `does not queue again a download that finishes during a reorder`() {
        val finishDownload = CompletableDeferred<Unit>()
        val downloadCalls = AtomicInteger()
        coEvery { mangaRepository.getMangaById(1L) } coAnswers
            {
                // Only the first download of the chapter finishes. A second one never ends, so a
                // chapter queued again stays in the queue.
                if (downloadCalls.incrementAndGet() > 1) awaitCancellation()
                finishDownload.await()
                downloader.queueState.value.first { it.chapterItem.id == 1L }.status =
                    Download.State.DOWNLOADED
                null
            }
        downloader.queueChapters(manga, listOf(chapter(1), chapter(2)), autoStart = true)
        downloader.start()
        val download = downloader.queueState.value.first { it.chapterItem.id == 1L }

        downloader.updateQueue { queue ->
            finishDownload.complete(Unit)
            waitUntil { download.status == Download.State.DOWNLOADED }
            queue.reversed()
        }

        assertEquals(listOf(2L), downloader.queueState.value.map { it.chapterItem.id })
        assertEquals(Download.State.DOWNLOADED, download.status)
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

    private companion object {
        const val QUEUEING_THREAD = "queueing"
    }
}
