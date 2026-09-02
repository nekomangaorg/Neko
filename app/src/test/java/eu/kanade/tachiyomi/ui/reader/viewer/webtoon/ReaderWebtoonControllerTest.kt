package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderWebtoonControllerTest {

    private fun createChapter(id: Long, pageCount: Int = 10): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = id.toFloat()
            }
        val readerChapter = ReaderChapter(dbChapter)
        val pages =
            (0 until pageCount).map { index ->
                ReaderPage(index = index, url = "url_$index", imageUrl = "img_$index").apply {
                    this.chapter = readerChapter
                    this.renderedHeight = 1000
                }
            }
        readerChapter.state = ReaderChapter.State.Loaded(pages)
        return readerChapter
    }

    @Test
    fun `buildItems with loaded chapters adds prevPages, currentPages, and nextPages`() {
        val controller = ReaderWebtoonController()
        val prevChapter = createChapter(1L, pageCount = 5)
        val currChapter = createChapter(2L, pageCount = 10)
        val nextChapter = createChapter(3L, pageCount = 5)

        val viewerChapters = ViewerChapters(currChapter, prevChapter, nextChapter)
        val items =
            controller.buildItems(viewerChapters, forceTransition = false, screenHeight = 1000)

        // Previous chapter: takes last 2 pages (pages 3, 4)
        // No prev transition because prevChapter is loaded and no missing chapters
        // Current chapter: all 10 pages (pages 0..9)
        // No next transition because nextChapter is loaded and no missing chapters
        // Next chapter: first 2 pages (pages 0, 1)
        // Total = 2 + 10 + 2 = 14 items
        assertEquals(14, items.size)
        assertTrue(items[0] is ReaderUiItem.Page)
        assertEquals(1L, items[0].chapterId)
        assertEquals(3, items[0].pageIndex)

        assertEquals(2L, items[2].chapterId)
        assertEquals(0, items[2].pageIndex)

        assertEquals(3L, items[12].chapterId)
        assertEquals(0, items[12].pageIndex)
    }

    @Test
    fun `buildItems inserts transition when forced or unloaded`() {
        val controller = ReaderWebtoonController()
        val prevChapter = createChapter(1L, pageCount = 5)
        val currChapter = createChapter(2L, pageCount = 10)
        val nextChapter =
            createChapter(3L, pageCount = 5).apply { state = ReaderChapter.State.Wait }

        val viewerChapters = ViewerChapters(currChapter, prevChapter, nextChapter)
        val items = controller.buildItems(viewerChapters, forceTransition = true)

        // With forceTransition = true:
        // prev pages (2) + prev transition (1) + curr pages (10) + next transition (1) + next pages
        // (2) = 16
        assertEquals(16, items.size)
        assertTrue(items[2] is ReaderUiItem.Transition)
        assertTrue((items[2] as ReaderUiItem.Transition).transition is ChapterTransition.Prev)
        assertTrue(items[13] is ReaderUiItem.Transition)
        assertTrue((items[13] as ReaderUiItem.Transition).transition is ChapterTransition.Next)
    }

    @Test
    fun `splitPage correctly inserts split slices into item list`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 3)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        assertEquals(3, items.size)
        val targetPage = currChapter.pages!![1]
        val split1 = ReaderPageSplit(targetPage, 0, 1000)
        val split2 = ReaderPageSplit(targetPage, 1000, 1000)

        val updatedItems = controller.splitPage(items, targetPage, listOf(split1, split2))
        assertEquals(5, updatedItems.size)
        assertTrue(updatedItems[0] is ReaderUiItem.Page)
        assertTrue(updatedItems[1] is ReaderUiItem.Page) // original page
        assertTrue(updatedItems[2] is ReaderUiItem.SplitPage)
        assertTrue(updatedItems[3] is ReaderUiItem.SplitPage)
        assertTrue(updatedItems[4] is ReaderUiItem.Page)
    }

    @Test
    fun `findPageIndex locates page in list`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 5)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        val targetPage = currChapter.pages!![3]
        val index = controller.findPageIndex(items, targetPage)
        assertEquals(3, index)
    }
}
