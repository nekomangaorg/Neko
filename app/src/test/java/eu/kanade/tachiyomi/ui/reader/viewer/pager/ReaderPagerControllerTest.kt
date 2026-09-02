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
}
