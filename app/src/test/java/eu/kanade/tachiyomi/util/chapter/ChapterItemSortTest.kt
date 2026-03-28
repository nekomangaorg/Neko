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

        // Read, should be skipped
        val chapter1 = 1L.createChapterItem(read = true)
        // Unread, but unsupported group
        val chapter2 =
            2L.createChapterItem(scanlator = MdConstants.UnsupportedOfficialGroupList.first())
        // Unread, valid group, unavailable
        val chapter3 = 3L.createChapterItem(isUnavailable = true)
        // Unread, valid group, available - SHOULD BE CHOSEN
        val chapter4 = 4L.createChapterItem()

        val chapters = listOf(chapter1, chapter2, chapter3, chapter4)

        every { chapterFilter.filterChapters(chapters, manga) } returns chapters

        // Act
        val result = chapterItemSort.getNextUnreadChapter(manga, chapters)

        // Assert
        assertEquals(4L, result?.chapter?.id)
    }

    private fun Long.createChapterItem(
        read: Boolean = false,
        scanlator: String = "ValidGroup",
        isUnavailable: Boolean = false,
    ): ChapterItem {
        return ChapterItem(
            chapter =
                SimpleChapter(
                    id = this,
                    mangaId = 1,
                    read = read,
                    bookmark = false,
                    lastPageRead = 0,
                    dateFetch = 0,
                    sourceOrder = this.toInt(),
                    url = "",
                    name = "Chapter $this",
                    dateUpload = 0,
                    chapterNumber = this.toFloat(),
                    pagesLeft = 0,
                    volume = "1",
                    chapterText = "$this",
                    chapterTitle = "Title",
                    language = "en",
                    mangaDexChapterId = "$this",
                    oldMangaDexChapterId = null,
                    scanlator = scanlator,
                    smartOrder = this.toInt(),
                    uploader = "1",
                    isUnavailable = isUnavailable,
                ),
            downloadState = mockk(),
            downloadProgress = 0,
        )
    }
}
