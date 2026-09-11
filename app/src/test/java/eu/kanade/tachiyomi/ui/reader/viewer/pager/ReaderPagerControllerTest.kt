package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPagerControllerTest {

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
                }
            }
        readerChapter.state = ReaderChapter.State.Loaded(pages)
        return readerChapter
    }

    @Test
    fun `single page mode generates single item pages`() {
        val controller = ReaderPagerController()
        val currChapter = createChapter(1L, pageCount = 4)
        val viewerChapters = ViewerChapters(currChapter, null, null)

        val items =
            controller.buildItems(
                chapters = viewerChapters,
                forceTransition = false,
                doublePages = false,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = false,
            )

        // Prev transition (since prev is null) + 4 pages + next transition (since next is null) = 6
        // items
        assertEquals(6, items.size)
        assertTrue(items[0] is ReaderUiItem.Transition)
        assertTrue(items[1] is ReaderUiItem.Page)
        assertNull((items[1] as ReaderUiItem.Page).extraPage)
        assertEquals(0, items[1].pageIndex)
    }

    @Test
    fun `double page mode pairs consecutive pages`() {
        val controller = ReaderPagerController()
        val currChapter = createChapter(1L, pageCount = 4)
        val viewerChapters = ViewerChapters(currChapter, null, null)

        val items =
            controller.buildItems(
                chapters = viewerChapters,
                forceTransition = false,
                doublePages = true,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = false,
            )

        // In double page mode:
        // Pages are chunked into 2 pairs: (0, 1), (2, 3)
        // Transitions are also included
        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        assertEquals(2, pageItems.size)

        assertEquals(0, pageItems[0].page.index)
        assertEquals(1, pageItems[0].extraPage?.index)

        assertEquals(2, pageItems[1].page.index)
        assertEquals(3, pageItems[1].extraPage?.index)
    }

    @Test
    fun `double page mode with shifted pages isolates first page`() {
        val controller = ReaderPagerController()
        val currChapter = createChapter(1L, pageCount = 4)
        val viewerChapters = ViewerChapters(currChapter, null, null)

        val items =
            controller.buildItems(
                chapters = viewerChapters,
                forceTransition = false,
                doublePages = true,
                splitPages = false,
                shiftDoublePage = true,
                isRtl = false,
            )

        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        // Shifted double page adds blank after shifted page:
        // Pair 1: (0, null)
        // Pair 2: (1, 2)
        // Pair 3: (3, null)
        assertEquals(3, pageItems.size)
        assertEquals(0, pageItems[0].page.index)
        assertNull(pageItems[0].extraPage)

        assertEquals(1, pageItems[1].page.index)
        assertEquals(2, pageItems[1].extraPage?.index)

        assertEquals(3, pageItems[2].page.index)
        assertNull(pageItems[2].extraPage)
    }

    @Test
    fun `isRtl reverses item list in single and double page mode`() {
        val controller = ReaderPagerController()
        val currChapter = createChapter(1L, pageCount = 3)
        val viewerChapters = ViewerChapters(currChapter, null, null)

        val itemsLtr =
            controller.buildItems(
                chapters = viewerChapters,
                forceTransition = false,
                doublePages = false,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = false,
            )

        val itemsRtl =
            controller.buildItems(
                chapters = viewerChapters,
                forceTransition = false,
                doublePages = false,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = true,
            )

        assertEquals(itemsLtr.size, itemsRtl.size)
        assertEquals(itemsLtr.first().key("pager"), itemsRtl.last().key("pager"))
        assertEquals(itemsLtr.last().key("pager"), itemsRtl.first().key("pager"))
    }

    @Test
    fun `getDoublePageOrder in RTL mode without invert places extraPage on the left and page on the right`() {
        val controller = ReaderPagerController()
        val currChapter = createChapter(1L, pageCount = 2)
        val pages = currChapter.pages!!
        val page1 = pages[0]
        val page2 = pages[1]

        val (first, second) =
            controller.getDoublePageOrder(
                page = page1,
                extraPage = page2,
                isRtl = true,
                invertDoublePages = false,
            )

        // In RTL mode, reading is right-to-left:
        // page1 (first read) is on the right, page2 is on the left -> [2][1]
        assertEquals(page2, first)
        assertEquals(page1, second)
    }

    @Test
    fun `getDoublePageOrder in RTL mode with invert places page on the left and extraPage on the right`() {
        val controller = ReaderPagerController()
        val currChapter = createChapter(1L, pageCount = 2)
        val pages = currChapter.pages!!
        val page1 = pages[0]
        val page2 = pages[1]

        val (first, second) =
            controller.getDoublePageOrder(
                page = page1,
                extraPage = page2,
                isRtl = true,
                invertDoublePages = true,
            )

        // Inverted RTL mode -> [1][2]
        assertEquals(page1, first)
        assertEquals(page2, second)
    }

    @Test
    fun `getDoublePageOrder in LTR mode without invert places page on the left and extraPage on the right`() {
        val controller = ReaderPagerController()
        val currChapter = createChapter(1L, pageCount = 2)
        val pages = currChapter.pages!!
        val page1 = pages[0]
        val page2 = pages[1]

        val (first, second) =
            controller.getDoublePageOrder(
                page = page1,
                extraPage = page2,
                isRtl = false,
                invertDoublePages = false,
            )

        // In LTR mode, reading is left-to-right:
        // page1 is on the left, page2 is on the right -> [1][2]
        assertEquals(page1, first)
        assertEquals(page2, second)
    }

    @Test
    fun `getDoublePageOrder in LTR mode with invert places extraPage on the left and page on the right`() {
        val controller = ReaderPagerController()
        val currChapter = createChapter(1L, pageCount = 2)
        val pages = currChapter.pages!!
        val page1 = pages[0]
        val page2 = pages[1]

        val (first, second) =
            controller.getDoublePageOrder(
                page = page1,
                extraPage = page2,
                isRtl = false,
                invertDoublePages = true,
            )

        // Inverted LTR mode -> [2][1]
        assertEquals(page2, first)
        assertEquals(page1, second)
    }

    @Test
    fun `getPreloadIndices in LTR mode returns current page, forward pages up to preloadAmount, and up to 2 behind`() {
        // totalItems = 20, preloadAmount = 6, currentIndex = 5, isRtl = false
        val indices =
            ReaderPagerController.getPreloadIndices(
                currentIndex = 5,
                preloadAmount = 6,
                totalItems = 20,
                isRtl = false,
            )

        // Expected: 5 (current), 6, 7, 8, 9, 10, 11 (ahead), 4, 3 (behind)
        assertEquals(listOf(5, 6, 7, 8, 9, 10, 11, 4, 3), indices)
    }

    @Test
    fun `getPreloadIndices in RTL mode returns current page, decreasing ahead pages up to preloadAmount, and increasing behind pages`() {
        // totalItems = 20, preloadAmount = 6, currentIndex = 15, isRtl = true
        val indices =
            ReaderPagerController.getPreloadIndices(
                currentIndex = 15,
                preloadAmount = 6,
                totalItems = 20,
                isRtl = true,
            )

        // In RTL, reading ahead goes down toward 0:
        // Expected: 15 (current), 14, 13, 12, 11, 10, 9 (ahead), 16, 17 (behind)
        assertEquals(listOf(15, 14, 13, 12, 11, 10, 9, 16, 17), indices)
    }

    @Test
    fun `getPreloadIndices in RTL mode at chapter start clamps to bounds and preloads ahead correctly`() {
        // In RTL, chapter start is at the end of items list (e.g. index 19 of 20 items)
        val indices =
            ReaderPagerController.getPreloadIndices(
                currentIndex = 19,
                preloadAmount = 6,
                totalItems = 20,
                isRtl = true,
            )

        // Expected: 19 (current), 18, 17, 16, 15, 14, 13 (ahead), behind is out of bounds (20, 21
        // clamped)
        assertEquals(listOf(19, 18, 17, 16, 15, 14, 13), indices)
    }

    @Test
    fun `getPreloadIndices in LTR mode at chapter start clamps to bounds and preloads ahead correctly`() {
        // In LTR, chapter start is at index 0
        val indices =
            ReaderPagerController.getPreloadIndices(
                currentIndex = 0,
                preloadAmount = 4,
                totalItems = 20,
                isRtl = false,
            )

        // Expected: 0 (current), 1, 2, 3, 4 (ahead), behind is out of bounds (-1, -2 clamped)
        assertEquals(listOf(0, 1, 2, 3, 4), indices)
    }

    @Test
    fun `getPreloadIndices with invalid inputs returns empty list`() {
        assertTrue(ReaderPagerController.getPreloadIndices(0, 4, 0, false).isEmpty())
        assertTrue(ReaderPagerController.getPreloadIndices(-1, 4, 10, false).isEmpty())
        assertTrue(ReaderPagerController.getPreloadIndices(10, 4, 10, false).isEmpty())
    }
}
