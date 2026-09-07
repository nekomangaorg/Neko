package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

        // Previous chapter: all 5 pages (pages 0..4)
        // No prev transition because prevChapter is loaded and no missing chapters
        // Current chapter: all 10 pages (pages 0..9)
        // No next transition because nextChapter is loaded and no missing chapters
        // Next chapter: all 5 pages (pages 0..4)
        // Total = 5 + 10 + 5 = 20 items
        assertEquals(20, items.size)
        assertTrue(items[0] is ReaderUiItem.Page)
        assertEquals(1L, items[0].chapterId)
        assertEquals(0, items[0].pageIndex)

        assertEquals(2L, items[5].chapterId)
        assertEquals(0, items[5].pageIndex)

        assertEquals(3L, items[15].chapterId)
        assertEquals(0, items[15].pageIndex)
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
        // prev pages (5) + prev transition (1) + curr pages (10) + next transition (1) = 17 items
        // (next is Wait so 0 pages)
        assertEquals(17, items.size)
        assertTrue(items[5] is ReaderUiItem.Transition)
        assertTrue((items[5] as ReaderUiItem.Transition).transition is ChapterTransition.Prev)
        assertTrue(items[16] is ReaderUiItem.Transition)
        assertTrue((items[16] as ReaderUiItem.Transition).transition is ChapterTransition.Next)
    }

    @Test
    fun `splitPage replaces monolithic page when slices start at offset 0`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 3)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        assertEquals(3, items.size)
        val targetPage = currChapter.pages!![1]
        val split1 = ReaderPageSplit(targetPage, 0, 1000)
        val split2 = ReaderPageSplit(targetPage, 1000, 1000)

        val updatedItems = controller.splitPage(items, targetPage, listOf(split1, split2))
        assertEquals(4, updatedItems.size)
        assertTrue(updatedItems[0] is ReaderUiItem.Page)
        assertTrue(updatedItems[1] is ReaderUiItem.SplitPage)
        assertEquals(0, (updatedItems[1] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(updatedItems[2] is ReaderUiItem.SplitPage)
        assertEquals(1000, (updatedItems[2] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(updatedItems[3] is ReaderUiItem.Page)
        assertTrue(controller.tallSplitPages.contains(targetPage))
    }

    @Test
    fun `splitPage inserts slices after original page when topOffset is greater than 0`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 3)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        assertEquals(3, items.size)
        val targetPage = currChapter.pages!![1]
        val split1 = ReaderPageSplit(targetPage, 1000, 1000)

        val updatedItems = controller.splitPage(items, targetPage, listOf(split1))
        assertEquals(4, updatedItems.size)
        assertTrue(updatedItems[0] is ReaderUiItem.Page)
        assertTrue(updatedItems[1] is ReaderUiItem.Page)
        assertTrue(updatedItems[2] is ReaderUiItem.SplitPage)
        assertTrue(updatedItems[3] is ReaderUiItem.Page)
    }

    @Test
    fun `computeSplits returns null for normal proportion images`() {
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        val splits = ReaderWebtoonController.computeSplits(page, 1000, 1500, 1000, 4096)
        assertNull(splits)
    }

    @Test
    fun `computeSplits calculates correct slices for tall images`() {
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        val splits = ReaderWebtoonController.computeSplits(page, 1000, 12000, 1000, 4096)
        assertNotNull(splits)
        assertEquals(3, splits!!.size)
        assertEquals(0, splits[0].topOffset)
        assertEquals(4000, splits[0].splitHeight)
        assertEquals(4000, splits[1].topOffset)
        assertEquals(4000, splits[1].splitHeight)
        assertEquals(8000, splits[2].topOffset)
        assertEquals(4000, splits[2].splitHeight)
    }

    @Test
    fun `computeSplits handles images that exceed maxTextureSize`() {
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        val splits = ReaderWebtoonController.computeSplits(page, 2000, 5000, 3000, 4096)
        assertNotNull(splits)
        assertEquals(2, splits!!.size)
        assertEquals(0, splits[0].topOffset)
        assertEquals(2500, splits[0].splitHeight)
        assertEquals(2500, splits[1].topOffset)
        assertEquals(2500, splits[1].splitHeight)
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
