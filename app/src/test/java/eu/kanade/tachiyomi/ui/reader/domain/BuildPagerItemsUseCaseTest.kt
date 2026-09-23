package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildPagerItemsUseCaseTest {

    private val useCase = BuildPagerItemsUseCase()

    private fun createChapter(id: Long, pageCount: Int = 5): ReaderChapter {
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
    fun `emits pages and boundary transitions for single current chapter`() {
        val curr = createChapter(1L, pageCount = 6)
        val chapters = ViewerChapters(currChapter = curr, prevChapter = null, nextChapter = null)

        val items =
            useCase(
                chapters = chapters,
                doublePages = false,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = false,
            )

        // Prev transition + 6 pages + Next transition = 8 items
        assertEquals(8, items.size)
        assertTrue(items.first() is ReaderUiItem.Transition)
        assertTrue(items.last() is ReaderUiItem.Transition)

        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        assertEquals(6, pageItems.size)
        assertEquals(0, pageItems.first().page.index)
        assertEquals(5, pageItems.last().page.index)
    }

    @Test
    fun `reverses items when isRtl is true`() {
        val curr = createChapter(1L, pageCount = 4)
        val chapters = ViewerChapters(currChapter = curr, prevChapter = null, nextChapter = null)

        val items =
            useCase(
                chapters = chapters,
                doublePages = false,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = true,
            )

        // Reversed: Next transition first, then pages 3 down to 0, then Prev transition
        assertEquals(6, items.size)
        assertTrue(items.first() is ReaderUiItem.Transition)
        assertTrue(items.last() is ReaderUiItem.Transition)

        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        assertEquals(4, pageItems.size)
        assertEquals(3, pageItems.first().page.index)
        assertEquals(0, pageItems.last().page.index)
    }

    @Test
    fun `prepends boundary pages when prevChapter is loaded`() {
        val prev = createChapter(1L, pageCount = 6)
        val curr = createChapter(2L, pageCount = 6)
        val chapters = ViewerChapters(currChapter = curr, prevChapter = prev, nextChapter = null)

        val items =
            useCase(
                chapters = chapters,
                doublePages = false,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = false,
            )

        // 2 boundary pages from prev (since size=6 is even) + 6 curr pages + Next transition = 9
        // items
        assertEquals(9, items.size)
        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        assertEquals(8, pageItems.size)
        assertEquals(1L, pageItems[0].page.chapter.chapter.id)
        assertEquals(4, pageItems[0].page.index)
        assertEquals(5, pageItems[1].page.index)
        assertEquals(2L, pageItems[2].page.chapter.chapter.id)
        assertEquals(0, pageItems[2].page.index)
        assertTrue(items.last() is ReaderUiItem.Transition)
    }

    @Test
    fun `appends boundary pages when nextChapter is loaded`() {
        val curr = createChapter(1L, pageCount = 6)
        val next = createChapter(2L, pageCount = 6)
        val chapters = ViewerChapters(currChapter = curr, prevChapter = null, nextChapter = next)

        val items =
            useCase(
                chapters = chapters,
                doublePages = false,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = false,
            )

        // Prev transition + 6 curr pages + 2 boundary pages from next = 9 items
        assertEquals(9, items.size)
        assertTrue(items.first() is ReaderUiItem.Transition)
        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        assertEquals(8, pageItems.size)
        assertEquals(1L, pageItems[0].page.chapter.chapter.id)
        assertEquals(0, pageItems[0].page.index)
        assertEquals(5, pageItems[5].page.index)
        assertEquals(2L, pageItems[6].page.chapter.chapter.id)
        assertEquals(0, pageItems[6].page.index)
        assertEquals(1, pageItems[7].page.index)
    }

    @Test
    fun `chunks pages into pairs in double-page mode`() {
        val curr = createChapter(1L, pageCount = 4)
        val chapters = ViewerChapters(currChapter = curr, prevChapter = null, nextChapter = null)

        val items =
            useCase(
                chapters = chapters,
                doublePages = true,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = false,
            )

        // 4 pages chunked into 2 pairs: [0, 1], [2, 3]
        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        assertEquals(2, pageItems.size)
        val firstPair = pageItems[0]
        val secondPair = pageItems[1]

        assertEquals(0, firstPair.page.index)
        assertEquals(1, firstPair.extraPage?.index)
        assertEquals(2, secondPair.page.index)
        assertEquals(3, secondPair.extraPage?.index)
    }

    @Test
    fun `isolates wide page spread into single item in double-page mode`() {
        val curr = createChapter(1L, pageCount = 4)
        // Mark page 1 as wide fullPage
        curr.pages!![1].fullPage = true

        val chapters = ViewerChapters(currChapter = curr, prevChapter = null, nextChapter = null)

        val items =
            useCase(
                chapters = chapters,
                doublePages = true,
                splitPages = false,
                shiftDoublePage = false,
                isRtl = false,
            )

        // Page 0 (single/isolated before full page), Page 1 (fullPage isolated), Pages 2 & 3
        // (paired)
        // Total 3 items:
        // Item 0: [p0, null]
        // Item 1: [p1, null]
        // Item 2: [p2, p3]
        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        assertEquals(3, pageItems.size)
        val item0 = pageItems[0]
        val item1 = pageItems[1]
        val item2 = pageItems[2]

        assertEquals(0, item0.page.index)
        assertNull(item0.extraPage)

        assertEquals(1, item1.page.index)
        assertNull(item1.extraPage)

        assertEquals(2, item2.page.index)
        assertEquals(3, item2.extraPage?.index)
    }

    @Test
    fun `splits wide page into two InsertPage halves in splitPages mode`() {
        val curr = createChapter(1L, pageCount = 3)
        // Mark page 1 as wide longPage
        curr.pages!![1].longPage = true

        val chapters = ViewerChapters(currChapter = curr, prevChapter = null, nextChapter = null)

        val items =
            useCase(
                chapters = chapters,
                doublePages = false,
                splitPages = true,
                shiftDoublePage = false,
                isRtl = false,
            )

        // 3 pages with middle one split into 2 halves = 4 items
        val pageItems = items.filterIsInstance<ReaderUiItem.Page>()
        assertEquals(4, pageItems.size)

        val p0 = pageItems[0]
        val p1Half1 = pageItems[1]
        val p1Half2 = pageItems[2]
        val p2 = pageItems[3]

        assertEquals(0, p0.page.index)

        assertEquals(1, p1Half1.page.index)
        assertTrue(p1Half1.page is InsertPage)
        assertEquals(true, p1Half1.page.firstHalf)

        assertEquals(1, p1Half2.page.index)
        assertTrue(p1Half2.page is InsertPage)
        assertEquals(false, p1Half2.page.firstHalf)

        assertEquals(2, p2.page.index)
    }
}
