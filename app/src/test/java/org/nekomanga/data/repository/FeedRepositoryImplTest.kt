package org.nekomanga.data.repository

import com.github.michaelbull.result.get
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.util.manga.toDisplayManga
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import org.nekomanga.domain.chapter.toChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.feed.FeedHistoryGroup
import org.nekomanga.domain.feed.FeedItem
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.usecases.chapters.ChapterUseCases
import tachiyomi.core.preference.Preference

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

    @Test
    fun `deleteChapter deletes downloaded chapter when manga exists`() = runTest {
        val manga =
            Manga.create(1L).apply {
                id = 1L
                title = "Manga 1"
                url = "/title/1"
            }
        val chapter =
            Chapter.create().apply {
                id = 101L
                manga_id = 1L
                name = "Ch 1"
                url = "/ch/101"
            }
        val chapterItem = chapter.toSimpleChapter()!!.toChapterItem()

        coEvery { mangaRepository.getMangaById(1L) } returns manga
        coEvery { downloadManager.deleteChapters(manga, any()) } returns Unit

        repository.deleteChapter(chapterItem)

        coVerify(exactly = 1) { downloadManager.deleteChapters(manga, any()) }
    }

    @Test
    fun `deleteChapter does nothing when manga does not exist`() = runTest {
        val chapter =
            Chapter.create().apply {
                id = 101L
                manga_id = 1L
                name = "Ch 1"
                url = "/ch/101"
            }
        val chapterItem = chapter.toSimpleChapter()!!.toChapterItem()

        coEvery { mangaRepository.getMangaById(1L) } returns null

        repository.deleteChapter(chapterItem)

        coVerify(exactly = 0) { downloadManager.deleteChapters(any(), any()) }
    }

    @Test
    fun `deleteAllHistoryForManga resets last_read and time_read and upserts list`() = runTest {
        val chapter1 = Chapter.create().apply { id = 101L }
        val chapter2 = Chapter.create().apply { id = 102L }
        val history1 =
            History.create(chapter1).apply {
                last_read = 5000L
                time_read = 1000L
            }
        val history2 =
            History.create(chapter2).apply {
                last_read = 6000L
                time_read = 2000L
            }
        coEvery { historyRepository.getHistoryByMangaId(1L) } returns listOf(history1, history2)
        coEvery { historyRepository.upsertHistoryList(any()) } returns Unit

        repository.deleteAllHistoryForManga(1L)

        history1.last_read shouldBe 0L
        history1.time_read shouldBe 0L
        history2.last_read shouldBe 0L
        history2.time_read shouldBe 0L
        coVerify(exactly = 1) { historyRepository.upsertHistoryList(listOf(history1, history2)) }
    }

    @Test
    fun `deleteHistoryForChapter resets last_read and time_read and upserts`() = runTest {
        val chapter = Chapter.create().apply { id = 101L }
        val history =
            History.create(chapter).apply {
                last_read = 5000L
                time_read = 1000L
            }
        coEvery { historyRepository.getHistoryByChapterUrl("/ch/101") } returns history
        coEvery { historyRepository.upsertHistory(any()) } returns Unit

        repository.deleteHistoryForChapter("/ch/101")

        history.last_read shouldBe 0L
        history.time_read shouldBe 0L
        coVerify(exactly = 1) { historyRepository.upsertHistory(history) }
    }

    @Test
    fun `deleteHistoryForChapter does nothing when history not found`() = runTest {
        coEvery { historyRepository.getHistoryByChapterUrl("/ch/unknown") } returns null

        repository.deleteHistoryForChapter("/ch/unknown")

        coVerify(exactly = 0) { historyRepository.upsertHistory(any()) }
    }

    @Test
    fun `getUpdatesPage returns mapped items and filters out blocked groups and uploaders`() =
        runTest {
            val blockedGroupsPref =
                mockk<Preference<Set<String>>> { every { get() } returns setOf("Blocked Group") }
            val blockedUploadersPref =
                mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
            every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
            every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

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
                    name = "Ch 1"
                    url = "/ch/101"
                    scanlator = "Allowed Group"
                    date_upload = 1000L
                }
            val chapter2 =
                Chapter.create().apply {
                    id = 102L
                    manga_id = 2L
                    name = "Ch 2"
                    url = "/ch/102"
                    scanlator = "Blocked Group"
                    date_upload = 2000L
                }

            every {
                chapterUseCases.validateChapterNotBlocked(
                    listOf("Allowed Group"),
                    any(),
                    setOf("Blocked Group"),
                    emptySet(),
                )
            } returns true

            every {
                chapterUseCases.validateChapterNotBlocked(
                    listOf("Blocked Group"),
                    any(),
                    setOf("Blocked Group"),
                    emptySet(),
                )
            } returns false

            every { downloadManager.isChapterDownloaded(any(), any()) } returns false
            every { downloadManager.getQueuedDownloadOrNull(any()) } returns null

            coEvery {
                chapterRepository.getRecentChapters(
                    search = "",
                    offset = 0,
                    limit = 25,
                    sortByFetched = false,
                )
            } returns listOf(MangaChapter(manga1, chapter1), MangaChapter(manga2, chapter2))

            val result =
                repository.getUpdatesPage(
                    searchQuery = "",
                    offset = 0,
                    limit = 25,
                    uploadsFetchSort = false,
                )

            val page = result.get()
            page shouldNotBe null
            page!!.items.size shouldBe 1
            page.items.first().mangaId shouldBe 1L
        }

    @Test(expected = CancellationException::class)
    fun `getUpdatesPage rethrows CancellationException`() = runTest {
        val blockedGroupsPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        val blockedUploadersPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

        coEvery { chapterRepository.getRecentChapters(any(), any(), any(), any()) } throws
            CancellationException("Cancelled")

        repository.getUpdatesPage(offset = 0, limit = 25, uploadsFetchSort = false)
    }

    @Test(expected = CancellationException::class)
    fun `getSummaryUpdatesList rethrows CancellationException`() = runTest {
        val blockedGroupsPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        val blockedUploadersPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

        coEvery { chapterRepository.getRecentChapters(any(), any(), any(), any()) } throws
            CancellationException("Cancelled")

        repository.getSummaryUpdatesList()
    }

    @Test(expected = CancellationException::class)
    fun `getSummaryContinueReadingList rethrows CancellationException`() = runTest {
        val blockedGroupsPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        val blockedUploadersPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

        coEvery { historyRepository.getRecentMangaLimit(any(), any(), any()) } throws
            CancellationException("Cancelled")

        repository.getSummaryContinueReadingList()
    }

    @Test(expected = CancellationException::class)
    fun `getSummaryNewlyAddedList rethrows CancellationException`() = runTest {
        val blockedGroupsPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        val blockedUploadersPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

        coEvery { mangaRepository.getFavoriteMangaList() } throws CancellationException("Cancelled")

        repository.getSummaryNewlyAddedList()
    }

    @Test
    fun `getSummaryNewlyAddedList returns top 6 unread favorited manga in descending date_added order`() =
        runTest {
            val blockedGroupsPref =
                mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
            val blockedUploadersPref =
                mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
            every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
            every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

            val manga1 =
                Manga.create(1L).apply {
                    id = 1L
                    title = "Manga 1"
                    url = "/title/1"
                    date_added = 100L
                }
            val manga2 =
                Manga.create(1L).apply {
                    id = 2L
                    title = "Manga 2"
                    url = "/title/2"
                    date_added = 200L
                }

            val chapter1 =
                Chapter.create().apply {
                    id = 101L
                    manga_id = 1L
                    name = "Ch 1"
                    url = "/ch/101"
                    read = false
                }
            val chapter2 =
                Chapter.create().apply {
                    id = 102L
                    manga_id = 2L
                    name = "Ch 2"
                    url = "/ch/102"
                    read = false
                }

            coEvery { mangaRepository.getFavoriteMangaList() } returns listOf(manga1, manga2)
            coEvery { chapterRepository.getChaptersForMangaIds(listOf(2L, 1L)) } returns
                listOf(chapter2, chapter1)
            every { downloadManager.isChapterDownloaded(any(), any()) } returns false
            every { downloadManager.getQueuedDownloadOrNull(any()) } returns null

            val result = repository.getSummaryNewlyAddedList()

            val list = result.get()
            list shouldNotBe null
            list!!.size shouldBe 2
            // Manga 2 has date_added = 200L, so it should be first
            list[0].mangaId shouldBe 2L
            list[1].mangaId shouldBe 1L
        }

    @Test
    fun `getSummaryNewlyAddedList filters out manga with read chapters`() = runTest {
        val blockedGroupsPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        val blockedUploadersPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

        val manga1 =
            Manga.create(1L).apply {
                id = 1L
                title = "Manga 1"
                url = "/title/1"
                date_added = 100L
            }
        val chapter1 =
            Chapter.create().apply {
                id = 101L
                manga_id = 1L
                name = "Ch 1"
                url = "/ch/101"
                read = true
            }

        coEvery { mangaRepository.getFavoriteMangaList() } returns listOf(manga1)
        coEvery { chapterRepository.getChaptersForMangaIds(listOf(1L)) } returns listOf(chapter1)

        val result = repository.getSummaryNewlyAddedList()

        val list = result.get()
        list shouldNotBe null
        list!!.shouldBe(emptyList())
    }

    @Test
    fun `getUpdatedFeedItemForHistoryBySeries filters out blocked scanlators`() = runTest {
        val blockedGroupsPref =
            mockk<Preference<Set<String>>> { every { get() } returns setOf("Blocked Group") }
        val blockedUploadersPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

        val manga =
            Manga.create(1L).apply {
                id = 1L
                title = "Manga 1"
                url = "/title/1"
            }
        val chapter1 =
            Chapter.create().apply {
                id = 101L
                manga_id = 1L
                name = "Ch 1"
                url = "/ch/101"
                scanlator = "Allowed Group"
            }
        val chapter2 =
            Chapter.create().apply {
                id = 102L
                manga_id = 1L
                name = "Ch 2"
                url = "/ch/102"
                scanlator = "Blocked Group"
            }
        val history1 = History.create(chapter1).apply { last_read = 1000L }
        val history2 = History.create(chapter2).apply { last_read = 2000L }

        val ch1 = MangaChapterHistory(manga, chapter1, history1)
        val ch2 = MangaChapterHistory(manga, chapter2, history2)

        coEvery { historyRepository.getChapterHistoryByMangaId(1L) } returns listOf(ch1, ch2)
        every {
            chapterUseCases.validateChapterNotBlocked(
                listOf("Allowed Group"),
                any(),
                setOf("Blocked Group"),
                emptySet(),
            )
        } returns true
        every {
            chapterUseCases.validateChapterNotBlocked(
                listOf("Blocked Group"),
                any(),
                setOf("Blocked Group"),
                emptySet(),
            )
        } returns false

        val feedItem =
            FeedItem(
                mangaId = 1L,
                mangaTitle = "Manga 1",
                date = 1000L,
                artwork = manga.toDisplayManga().currentArtwork,
                chapters = emptyList(),
            )

        val updated = repository.getUpdatedFeedItemForHistoryBySeries(feedItem)

        updated.chapters.size shouldBe 1
        updated.chapters.first().chapter.id shouldBe 101L
    }
}
