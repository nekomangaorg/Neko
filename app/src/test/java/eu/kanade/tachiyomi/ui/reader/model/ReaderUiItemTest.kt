package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.data.database.models.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReaderUiItemTest {

    private fun createReaderChapter(chapterId: Long): ReaderChapter {
        val chapter =
            Chapter.create().apply {
                id = chapterId
                url = "/chapter/$chapterId"
                name = "Chapter $chapterId"
            }
        return ReaderChapter(chapter)
    }

    private fun createReaderPage(
        chapterId: Long,
        index: Int,
        firstHalf: Boolean? = null,
    ): ReaderPage {
        val readerChapter = createReaderChapter(chapterId)
        return ReaderPage(index = index, url = "url_$index", imageUrl = "img_$index").apply {
            chapter = readerChapter
            this.firstHalf = firstHalf
        }
    }

    @Test
    fun `single page generates correct key`() {
        val page = createReaderPage(chapterId = 102992L, index = 0)
        val item = ReaderUiItem.Page(page)

        assertEquals("pager_page_102992_0", item.key("pager"))
        assertEquals("webtoon_page_102992_0", item.key("webtoon"))
    }

    @Test
    fun `split wide pages generate unique keys for first and second halves`() {
        val firstHalf = createReaderPage(chapterId = 102992L, index = 0, firstHalf = true)
        val secondHalf = createReaderPage(chapterId = 102992L, index = 0, firstHalf = false)

        val firstItem = ReaderUiItem.Page(firstHalf)
        val secondItem = ReaderUiItem.Page(secondHalf)

        assertEquals("pager_page_102992_0_half_true", firstItem.key("pager"))
        assertEquals("pager_page_102992_0_half_false", secondItem.key("pager"))
        assertNotEquals(firstItem.key("pager"), secondItem.key("pager"))
    }

    @Test
    fun `paired double page spread generates combined key`() {
        val page1 = createReaderPage(chapterId = 102992L, index = 0)
        val page2 = createReaderPage(chapterId = 102992L, index = 1)

        val item = ReaderUiItem.Page(page1, page2)

        assertEquals("pager_page_102992_0_102992_1", item.key("pager"))
    }

    @Test
    fun `transitions between same chapters generate distinct keys for prev and next`() {
        val chapter1 = createReaderChapter(100L)
        val chapter2 = createReaderChapter(200L)

        val prevTransition = ChapterTransition.Prev(from = chapter2, to = chapter1)
        val nextTransition = ChapterTransition.Next(from = chapter1, to = chapter2)

        val prevItem = ReaderUiItem.Transition(prevTransition)
        val nextItem = ReaderUiItem.Transition(nextTransition)

        assertEquals("pager_transition_prev_200_100", prevItem.key("pager"))
        assertEquals("pager_transition_next_100_200", nextItem.key("pager"))
        assertNotEquals(prevItem.key("pager"), nextItem.key("pager"))
    }

    @Test
    fun `split pages generate distinct keys for different offsets`() {
        val page = createReaderPage(chapterId = 102992L, index = 0)
        val split1 = ReaderPageSplit(page = page, topOffset = 0, splitHeight = 1000)
        val split2 = ReaderPageSplit(page = page, topOffset = 1000, splitHeight = 1000)

        val item1 = ReaderUiItem.SplitPage(split1)
        val item2 = ReaderUiItem.SplitPage(split2)

        assertEquals("webtoon_split_102992_0_0", item1.key("webtoon"))
        assertEquals("webtoon_split_102992_0_1000", item2.key("webtoon"))
        assertNotEquals(item1.key("webtoon"), item2.key("webtoon"))
    }
}
