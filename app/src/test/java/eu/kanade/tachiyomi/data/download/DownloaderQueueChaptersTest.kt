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
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import org.junit.After
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

    private fun chapter(id: Long, manga: Manga = this.manga): Chapter =
        Chapter.create().apply {
            this.id = id
            manga_id = manga.id
            url = "/chapter/$id"
            name = "Ch.$id"
        }
}
