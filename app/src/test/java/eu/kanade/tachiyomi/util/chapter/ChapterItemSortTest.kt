package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.details.MangaDetailsPreferences

class ChapterItemSortTest {

    private lateinit var chapterFilter: ChapterItemFilter
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var mangaDetailsPreferences: MangaDetailsPreferences
    private lateinit var chapterItemSort: ChapterItemSort

    @Before
    fun setup() {
        chapterFilter = mockk()
        preferencesHelper = mockk()
        mangaDetailsPreferences = mockk()

        // Arrange
        chapterItemSort = ChapterItemSort(chapterFilter, preferencesHelper, mangaDetailsPreferences)
    }

    @Test
    fun `given unread chapters when getNextUnreadChapter then returns the first available unread chapter ignoring unsupported groups`() {
        // Arrange
        val manga = MangaImpl().apply { chapter_flags = Manga.CHAPTER_SORTING_SOURCE }

        every { mangaDetailsPreferences.chaptersDescAsDefault().get() } returns true
        every { mangaDetailsPreferences.sortChapterOrder().get() } returns
            Manga.CHAPTER_SORTING_SOURCE

        val chapter1 =
            ChapterItem(
                chapter =
                    SimpleChapter(
                        id = 1,
                        mangaId = 1,
                        read = true, // Read, should be skipped
                        bookmark = false,
                        lastPageRead = 0,
                        dateFetch = 0,
                        sourceOrder = 1,
                        url = "",
                        name = "Chapter 1",
                        dateUpload = 0,
                        chapterNumber = 1f,
                        pagesLeft = 0,
                        volume = "1",
                        chapterText = "1",
                        chapterTitle = "Title",
                        language = "en",
                        mangaDexChapterId = "1",
                        oldMangaDexChapterId = null,
                        scanlator = "ValidGroup",
                        smartOrder = 1,
                        uploader = "1",
                        isUnavailable = false,
                    ),
                downloadState = mockk(),
                downloadProgress = 0,
            )

        val chapter2 =
            ChapterItem(
                chapter =
                    SimpleChapter(
                        id = 2,
                        mangaId = 1,
                        read = false, // Unread, but unsupported group
                        bookmark = false,
                        lastPageRead = 0,
                        dateFetch = 0,
                        sourceOrder = 2,
                        url = "",
                        name = "Chapter 2",
                        dateUpload = 0,
                        chapterNumber = 2f,
                        pagesLeft = 0,
                        volume = "1",
                        chapterText = "2",
                        chapterTitle = "Title",
                        language = "en",
                        mangaDexChapterId = "2",
                        oldMangaDexChapterId = null,
                        scanlator =
                            MdConstants.UnsupportedOfficialGroupList.first(), // Unsupported group
                        smartOrder = 2,
                        uploader = "1",
                        isUnavailable = false,
                    ),
                downloadState = mockk(),
                downloadProgress = 0,
            )

        val chapter3 =
            ChapterItem(
                chapter =
                    SimpleChapter(
                        id = 3,
                        mangaId = 1,
                        read = false, // Unread, valid group, unavailable
                        bookmark = false,
                        lastPageRead = 0,
                        dateFetch = 0,
                        sourceOrder = 3,
                        url = "",
                        name = "Chapter 3",
                        dateUpload = 0,
                        chapterNumber = 3f,
                        pagesLeft = 0,
                        volume = "1",
                        chapterText = "3",
                        chapterTitle = "Title",
                        language = "en",
                        mangaDexChapterId = "3",
                        oldMangaDexChapterId = null,
                        scanlator = "ValidGroup",
                        smartOrder = 3,
                        uploader = "1",
                        isUnavailable = true,
                    ),
                downloadState = mockk(),
                downloadProgress = 0,
            )

        val chapter4 =
            ChapterItem(
                chapter =
                    SimpleChapter(
                        id = 4,
                        mangaId = 1,
                        read = false, // Unread, valid group, available - SHOULD BE CHOSEN
                        bookmark = false,
                        lastPageRead = 0,
                        dateFetch = 0,
                        sourceOrder = 4,
                        url = "",
                        name = "Chapter 4",
                        dateUpload = 0,
                        chapterNumber = 4f,
                        pagesLeft = 0,
                        volume = "1",
                        chapterText = "4",
                        chapterTitle = "Title",
                        language = "en",
                        mangaDexChapterId = "4",
                        oldMangaDexChapterId = null,
                        scanlator = "ValidGroup",
                        smartOrder = 4,
                        uploader = "1",
                        isUnavailable = false,
                    ),
                downloadState = mockk(),
                downloadProgress = 0,
            )

        val chapters = listOf(chapter1, chapter2, chapter3, chapter4)

        every { chapterFilter.filterChapters(chapters, manga) } returns chapters

        // Act
        val result = chapterItemSort.getNextUnreadChapter(manga, chapters)

        // Assert
        assertEquals(4L, result?.chapter?.id)
    }
}
