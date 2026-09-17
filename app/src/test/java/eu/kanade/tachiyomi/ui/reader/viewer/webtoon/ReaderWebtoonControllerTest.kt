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
    fun `buildItems with loaded chapters adds prevTransition, currentPages, and nextPages`() {
        val controller = ReaderWebtoonController()
        val prevChapter = createChapter(1L, pageCount = 5)
        val currChapter = createChapter(2L, pageCount = 10)
        val nextChapter = createChapter(3L, pageCount = 5)

        val viewerChapters = ViewerChapters(currChapter, prevChapter, nextChapter)
        val items =
            controller.buildItems(viewerChapters, forceTransition = false, screenHeight = 1000)

        // Webtoon mode is forward-continuous: previous chapter pages are never prepended.
        // Index 0: previous chapter transition
        // Current chapter: all 10 pages (pages 0..9 at indices 1..10)
        // Next chapter: all 5 pages (pages 0..4 at indices 11..15)
        // Total = 1 + 10 + 5 = 16 items
        assertEquals(16, items.size)
        assertTrue(items[0] is ReaderUiItem.Transition)
        assertTrue((items[0] as ReaderUiItem.Transition).transition is ChapterTransition.Prev)

        assertEquals(2L, items[1].chapterId)
        assertEquals(0, items[1].pageIndex)

        assertEquals(3L, items[11].chapterId)
        assertEquals(0, items[11].pageIndex)
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
        // prev transition (1) + curr pages (10) + next transition (1) = 12 items
        // (next is Wait so 0 pages)
        assertEquals(12, items.size)
        assertTrue(items[0] is ReaderUiItem.Transition)
        assertTrue((items[0] as ReaderUiItem.Transition).transition is ChapterTransition.Prev)
        assertTrue(items[11] is ReaderUiItem.Transition)
        assertTrue((items[11] as ReaderUiItem.Transition).transition is ChapterTransition.Next)
    }

    @Test
    fun `splitPage replaces monolithic page when slices start at offset 0`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 3)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        assertEquals(5, items.size)
        val targetPage = currChapter.pages!![1]
        val split1 = ReaderPageSplit(targetPage, 0, 1000)
        val split2 = ReaderPageSplit(targetPage, 1000, 1000)

        val updatedItems = controller.splitPage(items, targetPage, listOf(split1, split2))
        assertEquals(6, updatedItems.size)
        assertTrue(updatedItems[0] is ReaderUiItem.Transition)
        assertTrue(updatedItems[1] is ReaderUiItem.Page)
        assertTrue(updatedItems[2] is ReaderUiItem.SplitPage)
        assertEquals(0, (updatedItems[2] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(updatedItems[3] is ReaderUiItem.SplitPage)
        assertEquals(1000, (updatedItems[3] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(updatedItems[4] is ReaderUiItem.Page)
        assertTrue(updatedItems[5] is ReaderUiItem.Transition)
        assertTrue(controller.tallSplitPages.contains(targetPage))
    }

    @Test
    fun `splitPage inserts slices after original page when topOffset is greater than 0`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 3)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        assertEquals(5, items.size)
        val targetPage = currChapter.pages!![1]
        val split1 = ReaderPageSplit(targetPage, 1000, 1000)

        val updatedItems = controller.splitPage(items, targetPage, listOf(split1))
        assertEquals(6, updatedItems.size)
        assertTrue(updatedItems[0] is ReaderUiItem.Transition)
        assertTrue(updatedItems[1] is ReaderUiItem.Page)
        assertTrue(updatedItems[2] is ReaderUiItem.Page)
        assertTrue(updatedItems[3] is ReaderUiItem.SplitPage)
        assertTrue(updatedItems[4] is ReaderUiItem.Page)
        assertTrue(updatedItems[5] is ReaderUiItem.Transition)
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
    fun `computeSplits calculates optimal slices on modern high-res screens`() {
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        val splits = ReaderWebtoonController.computeSplits(page, 1080, 7000, 2400, 8192)
        assertNotNull(splits)
        assertEquals(2, splits!!.size)
        assertEquals(0, splits[0].topOffset)
        assertEquals(3500, splits[0].splitHeight)
        assertEquals(3500, splits[1].topOffset)
        assertEquals(3500, splits[1].splitHeight)
    }

    @Test
    fun `findPageIndex locates page in list`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 5)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        val targetPage = currChapter.pages!![3]
        val index = controller.findPageIndex(items, targetPage)
        assertEquals(4, index)
    }

    @Test
    fun `checkAndTrackTallPage returns AlreadySplit when page was previously tracked`() {
        val controller = ReaderWebtoonController()
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        controller.tallSplitPages.add(page)

        val result = controller.checkAndTrackTallPage(page, screenHeight = 1000)
        assertEquals(ReaderWebtoonController.TallSplitResult.AlreadySplit, result)
    }

    @Test
    fun `checkAndTrackTallPage returns NotTall when page stream is null`() {
        val controller = ReaderWebtoonController()
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]

        val result = controller.checkAndTrackTallPage(page, screenHeight = 1000)
        assertEquals(ReaderWebtoonController.TallSplitResult.NotTall, result)
        assertTrue(!controller.tallSplitPages.contains(page))
        assertTrue(controller.isNonTall(page))
    }
}
