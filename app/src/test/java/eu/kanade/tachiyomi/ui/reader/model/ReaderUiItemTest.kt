package eu.kanade.tachiyomi.ui.reader.model

import coil3.request.Options
import eu.kanade.tachiyomi.data.coil.ReaderPageKeyer
import eu.kanade.tachiyomi.data.coil.ReaderPageSplitKeyer
import eu.kanade.tachiyomi.data.database.models.Chapter
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
    fun `transitions between same chapters generate symmetric keys across directions`() {
        val chapter1 = createReaderChapter(100L)
        val chapter2 = createReaderChapter(200L)

        val prevTransition = ChapterTransition.Prev(from = chapter2, to = chapter1)
        val nextTransition = ChapterTransition.Next(from = chapter1, to = chapter2)

        val prevItem = ReaderUiItem.Transition(prevTransition)
        val nextItem = ReaderUiItem.Transition(nextTransition)

        assertEquals("pager_transition_100_200", prevItem.key("pager"))
        assertEquals("pager_transition_100_200", nextItem.key("pager"))
        assertEquals(prevItem.key("pager"), nextItem.key("pager"))
    }

    @Test
    fun `reader ui items expose polymorphic chapterId and pageIndex`() {
        val page = createReaderPage(chapterId = 102992L, index = 3)
        val split = ReaderPageSplit(page = page, topOffset = 0, splitHeight = 1000)
        val chapter1 = createReaderChapter(100L)
        val chapter2 = createReaderChapter(200L)
        val transition = ChapterTransition.Next(from = chapter1, to = chapter2)

        val pageItem = ReaderUiItem.Page(page)
        val splitItem = ReaderUiItem.SplitPage(split)
        val transitionItem = ReaderUiItem.Transition(transition)

        assertEquals(102992L, pageItem.chapterId)
        assertEquals(3, pageItem.pageIndex)

        assertEquals(102992L, splitItem.chapterId)
        assertEquals(3, splitItem.pageIndex)

        assertNull(transitionItem.chapterId)
        assertNull(transitionItem.pageIndex)
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

    @Test
    fun `reader page keyer uses composite fallback when chapter id is null`() {
        val chapter =
            Chapter.create().apply {
                id = null
                url = "/chapter/test-url"
                name = "Test Chapter"
            }
        val readerChapter = ReaderChapter(chapter)
        val page = ReaderPage(index = 2, url = "url_2").apply { this.chapter = readerChapter }
        val split = ReaderPageSplit(page = page, topOffset = 500, splitHeight = 1000)

        val options = mockk<Options>(relaxed = true)
        val pageKey = ReaderPageKeyer().key(page, options)
        val splitKey = ReaderPageSplitKeyer().key(split, options)

        val expectedHash = "/chapter/test-url".hashCode()
        assertEquals("reader_page_${expectedHash}_2", pageKey)
        assertEquals("reader_split_${expectedHash}_2_500", splitKey)
    }

    @Test
    fun `reader page keyer uses chapter id when non-null`() {
        val page = createReaderPage(chapterId = 12345L, index = 1)
        val split = ReaderPageSplit(page = page, topOffset = 250, splitHeight = 1000)

        val options = mockk<Options>(relaxed = true)
        val pageKey = ReaderPageKeyer().key(page, options)
        val splitKey = ReaderPageSplitKeyer().key(split, options)

        assertEquals("reader_page_12345_1", pageKey)
        assertEquals("reader_split_12345_1_250", splitKey)
    }
}
