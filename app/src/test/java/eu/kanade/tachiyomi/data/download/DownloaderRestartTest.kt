package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
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
import io.mockk.verify
import java.util.Collections
import kotlin.concurrent.thread
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

/**
 * DownloadJob ends once [Downloader.isRunning] reads false, so a downloader that restarts itself
 * must never read as stopped from another thread halfway through.
 */
class DownloaderRestartTest {

    private lateinit var activeDownloads: SharedPreferences
    private lateinit var downloader: Downloader

    private val manga = Manga.create("/title/1", "Manga").apply { id = 1L }

    /**
     * Runs on every queue store write, from whichever thread makes it. Tests set this instead of
     * stubbing edit() again: the downloader writes the store from its own threads, and a call that
     * lands while MockK is still stubbing it throws "no answer provided" there.
     */
    @Volatile private var onStoreWrite: () -> Unit = {}

    /** isRunning as read by other threads while the queue store is written. */
    private val readsDuringRestart = Collections.synchronizedList(mutableListOf<Boolean>())
    private val readers = mutableListOf<Thread>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton<Json>(Json)
        Injekt.addSingleton(mockk<ChapterItemFilter>())
        Injekt.addSingleton(mockk<PreferencesHelper>())
        // Downloads never get past their first step, so a started downloader stays running.
        Injekt.addSingleton(
            mockk<MangaRepository> {
                coEvery { getMangaById(any()) } coAnswers { awaitCancellation() }
            }
        )
        Injekt.addSingleton(
            mockk<MangaDetailsPreferences> {
                every { sortChapterOrder().get() } returns Manga.CHAPTER_SORTING_SOURCE
            }
        )

        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        activeDownloads =
            mockk(relaxed = true) {
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

        downloader.queueChapters(manga, listOf(chapter(1), chapter(2)), autoStart = true)
        downloader.start()
        check(downloader.isRunning)
    }

    @After
    fun tearDown() {
        downloader.pause()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `reordering the queue never reads as stopped`() {
        readIsRunningOnQueueStoreWrites()

        downloader.updateQueue(downloader.queueState.value.reversed())

        assertReadsSawRunning()
    }

    @Test
    fun `removing a chapter from the queue never reads as stopped`() {
        readIsRunningOnQueueStoreWrites()

        downloader.removeFromQueueAndRestart(listOf(chapter(1)))

        assertReadsSawRunning()
        assertEquals(listOf(2L), downloader.queueState.value.map { it.chapterItem.id })
    }

    @Test
    fun `removing the only manga in the queue stops the downloader`() {
        downloader.removeFromQueueAndRestart(manga)

        assertTrue(downloader.queueState.value.isEmpty())
        assertFalse(downloader.isRunning)
        verify { DownloadJob.stop(any()) }
    }

    @Test
    fun `removing a manga with nothing queued leaves the downloader alone`() {
        // A restart always writes the queue store from this thread, start() sweeps the queue
        val testThread = Thread.currentThread()
        var storeWrites = 0
        onStoreWrite = { if (Thread.currentThread() == testThread) storeWrites++ }

        downloader.removeFromQueueAndRestart(Manga.create("/title/2", "Other").apply { id = 2L })

        assertEquals(0, storeWrites)
        assertTrue(downloader.isRunning)
        assertEquals(listOf(1L, 2L), downloader.queueState.value.map { it.chapterItem.id })
    }

    @Test
    fun `removing a manga from the queue never reads as stopped`() {
        val other = Manga.create("/title/2", "Other").apply { id = 2L }
        downloader.queueChapters(other, listOf(chapter(3, other)), autoStart = false)
        readIsRunningOnQueueStoreWrites()

        downloader.removeFromQueueAndRestart(manga)

        assertReadsSawRunning()
        assertEquals(listOf(3L), downloader.queueState.value.map { it.chapterItem.id })
    }

    /**
     * Every queue store write the test thread makes happens between the pause and the start of a
     * restart. Each one starts a thread that reads isRunning and waits until that read has either
     * finished or is blocked waiting for the restart to finish. The restarted downloader writes the
     * store from its own threads too; those writes are left alone.
     */
    private fun readIsRunningOnQueueStoreWrites() {
        val testThread = Thread.currentThread()
        onStoreWrite = {
            if (Thread.currentThread() == testThread) {
                val reader = thread { readsDuringRestart += downloader.isRunning }
                readers += reader
                waitUntil { !reader.isAlive || reader.state == Thread.State.BLOCKED }
            }
        }
    }

    private fun assertReadsSawRunning() {
        readers.forEach { it.join(10_000) }
        assertTrue("no isRunning read happened during the restart", readsDuringRestart.isNotEmpty())
        assertEquals(List(readsDuringRestart.size) { true }, readsDuringRestart.toList())
        assertTrue(downloader.isRunning)
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
