package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/**
 * Deleting a chapter or manga takes its download out of the queue and cancels it, but blocking work
 * such as zipping the pages keeps going and then fails on the deleted files.
 */
class DownloaderRemovedDownloadTest {

    private lateinit var downloader: Downloader

    private val manga = Manga.create("/title/1", "Manga").apply { id = 1L }

    private val blockingWorkStarted = CountDownLatch(1)
    private val blockingWorkFailed = CountDownLatch(1)
    private val errorReported = CountDownLatch(1)
    @Volatile private var sawCancel = false

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton<Json>(Json)
        Injekt.addSingleton(mockk<ChapterItemFilter>())
        Injekt.addSingleton(mockk<PreferencesHelper>())
        Injekt.addSingleton(mockk<MangaRepository> { coEvery { getMangaById(1L) } returns manga })
        Injekt.addSingleton(
            mockk<ChapterRepository> { coEvery { getChapterById(1L) } returns chapter(1) }
        )
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
        every { anyConstructed<DownloadNotifier>().onError(any(), any(), any()) } answers
            {
                errorReported.countDown()
            }

        val mangaDir =
            mockk<UniFile> {
                every { filePath } returns null
                every { createDirectory(any()) } returns mockk()
            }
        val provider =
            mockk<DownloadProvider> {
                every { findMangaDir(any()) } returns null
                every { chapterDirDoesNotExist(any(), any()) } returns true
                every { getMangaDir(any()) } returns mangaDir
                every { getChapterDirName(any()) } returns "Ch.1"
            }
        val source =
            mockk<MangaDex> {
                coEvery { getPageList(any()) } coAnswers
                    {
                        val job = currentCoroutineContext().job
                        blockingWorkStarted.countDown()
                        // Blocking work does not stop when its job is cancelled.
                        sawCancel = waitUntil { job.isCancelled }
                        blockingWorkFailed.countDown()
                        throw IllegalArgumentException("invalid entry size")
                    }
            }
        val sourceManager = mockk<SourceManager> { every { mangaDex } returns source }
        downloader = Downloader(context, provider, mockk(), sourceManager)
    }

    @After
    fun tearDown() {
        downloader.pause()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `a removed download that fails afterwards reports no error`() {
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = true)
        val download = downloader.queueState.value.single()
        downloader.start()
        check(blockingWorkStarted.await(10, TimeUnit.SECONDS)) { "the download did not start" }

        downloader.removeFromQueueAndRestart(listOf(chapter(1)))

        check(blockingWorkFailed.await(10, TimeUnit.SECONDS)) { "the blocking work did not end" }
        check(sawCancel) { "the download was not cancelled" }
        // An error is reported on the download's thread right after the failure.
        assertFalse(errorReported.await(1, TimeUnit.SECONDS))
        assertEquals(Download.State.NOT_DOWNLOADED, download.status)
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            if (System.currentTimeMillis() >= deadline) return false
            Thread.sleep(10)
        }
        return true
    }

    private fun chapter(id: Long): Chapter =
        Chapter.create().apply {
            this.id = id
            manga_id = manga.id
            url = "/chapter/$id"
            name = "Ch.$id"
        }
}
