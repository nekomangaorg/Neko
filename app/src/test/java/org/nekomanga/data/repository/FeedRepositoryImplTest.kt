package org.nekomanga.data.repository

import com.github.michaelbull.result.get
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.download.DownloadManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.feed.FeedHistoryGroup
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.usecases.chapters.ChapterUseCases

class FeedRepositoryImplTest {

    private val chapterRepository: ChapterRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()
    private val mangaRepository: MangaRepository = mockk()
    private val downloadManager: DownloadManager = mockk()
    private val chapterUseCases: ChapterUseCases = mockk()
    private val mangaDexPreferences: MangaDexPreferences = mockk()

    private val repository =
        FeedRepositoryImpl(
            chapterRepository = chapterRepository,
            historyRepository = historyRepository,
            mangaRepository = mangaRepository,
            downloadManager = downloadManager,
            chapterUseCases = chapterUseCases,
            mangaDexPreferences = mangaDexPreferences,
        )

    @Test
    fun `deleteAllHistory delegates to historyRepository`() = runTest {
        coEvery { historyRepository.deleteAllHistory() } returns Unit

        repository.deleteAllHistory()

        coVerify(exactly = 1) { historyRepository.deleteAllHistory() }
    }

    @Test
    fun `getHistoryPage with FeedHistoryGroup_Series filters out seenMangaIds`() = runTest {
        val manga1 =
            Manga.create(1L).apply {
                id = 1L
                title = "Manga 1"
                url = "/title/1"
            }
        val manga2 =
            Manga.create(1L).apply {
                id = 2L
                title = "Manga 2"
                url = "/title/2"
            }
        val chapter1 =
            Chapter.create().apply {
                id = 101L
                manga_id = 1L
                name = "Ch. 1"
                url = "/chapter/101"
            }
        val chapter2 =
            Chapter.create().apply {
                id = 102L
                manga_id = 2L
                name = "Ch. 2"
                url = "/chapter/102"
            }
        val history1 = History.create(chapter1).apply { last_read = 1000L }
        val history2 = History.create(chapter2).apply { last_read = 2000L }

        val ch1 = MangaChapterHistory(manga1, chapter1, history1)
        val ch2 = MangaChapterHistory(manga2, chapter2, history2)

        coEvery {
            historyRepository.getRecentMangaLimit(search = "", offset = 0, limit = 25)
        } returns listOf(ch1, ch2)
        coEvery { historyRepository.getChapterHistoryByMangaIds(listOf(1L, 2L)) } returns
            listOf(ch1, ch2)
        coEvery { historyRepository.getChapterHistoryByMangaIds(listOf(2L)) } returns listOf(ch2)
        every { downloadManager.isChapterDownloaded(any(), any()) } returns false
        every { downloadManager.getQueuedDownloadOrNull(any()) } returns null

        // If manga 1 is already in seenMangaIds, it should be filtered out
        val result =
            repository.getHistoryPage(
                searchQuery = "",
                offset = 0,
                limit = 25,
                group = FeedHistoryGroup.Series,
                seenMangaIds = setOf(1L),
            )

        val page = result.get()
        page!!.items.size shouldBe 1
        page.items.first().mangaId shouldBe 2L
    }

    @Test
    fun `getRecentlyReadManga returns empty list when history page is empty`() = runTest {
        coEvery {
            historyRepository.getRecentMangaLimit(search = "", offset = 0, limit = 25)
        } returns emptyList()
        coEvery { historyRepository.getChapterHistoryByMangaIds(emptyList()) } returns emptyList()
        coEvery { mangaRepository.getMangaByIds(any()) } returns emptyList()

        val result = repository.getRecentlyReadManga()

        result shouldBe emptyList()
    }

    @Test(expected = CancellationException::class)
    fun `getHistoryPage rethrows CancellationException`() = runTest {
        coEvery { historyRepository.getRecentMangaLimit(any(), any(), any()) } throws
            CancellationException("Job was cancelled")

        repository.getHistoryPage(offset = 0, limit = 25, group = FeedHistoryGroup.Series)
    }
}
