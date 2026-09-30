package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PagerScrollAnchorResolverTest {

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
    fun `resolveReanchorTarget returns null when native key tracking already maintained equivalent item`() {
        val ch1 = createChapter(1L, pageCount = 10)
        val items = ch1.pages!!.map { ReaderUiItem.Page(it) }

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = items,
                lastActiveItem = items[4],
                currentVisibleIndex = 4,
            )

        assertNull(target)
    }

    @Test
    fun `resolveReanchorTarget returns null on unshifted append of next chapter pages`() {
        val ch1 = createChapter(1L, pageCount = 10)
        val ch2 = createChapter(2L, pageCount = 10)

        val initialItems = ch1.pages!!.map { ReaderUiItem.Page(it) }
        val updatedItems = initialItems + ch2.pages!!.map { ReaderUiItem.Page(it) }

        // User is reading page 5 of chapter 1
        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = updatedItems,
                lastActiveItem = initialItems[5],
                currentVisibleIndex = 5,
                previousItems = initialItems,
            )

        assertNull(target)
    }

    @Test
    fun `resolveReanchorTarget re-anchors correctly when previous chapter is prepended`() {
        val ch1 = createChapter(1L, pageCount = 20)
        val ch2 = createChapter(2L, pageCount = 20)

        val ch2Items = ch2.pages!!.map { ReaderUiItem.Page(it) }
        val activeItem = ch2Items[3] // User on Ch2 Page 3

        // Prepend Ch1 pages (20 pages) + Transition
        val prependedItems = mutableListOf<ReaderUiItem>()
        prependedItems.addAll(ch1.pages!!.map { ReaderUiItem.Page(it) })
        prependedItems.add(ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)))
        prependedItems.addAll(ch2Items)

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = prependedItems,
                lastActiveItem = activeItem,
                currentVisibleIndex = 3, // Old index
                previousItems = ch2Items,
            )

        assertNotNull(target)
        // 20 prev pages + 1 transition + index 3 = 24
        assertEquals(24, target!!.index)
        assertEquals(2L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(3, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget preserves position when dual page pairs are shifted`() {
        val ch1 = createChapter(1L, pageCount = 10)
        val pages = ch1.pages!!

        // Before shift: [p4, p5] at index 2
        val activeItem = ReaderUiItem.Page(pages[4], pages[5])

        // After shift: [p3, p4] at index 2, [p5, p6] at index 3
        val shiftedItems =
            listOf(
                ReaderUiItem.Page(pages[0], null),
                ReaderUiItem.Page(pages[1], pages[2]),
                ReaderUiItem.Page(pages[3], pages[4]),
                ReaderUiItem.Page(pages[5], pages[6]),
            )

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = shiftedItems,
                lastActiveItem = activeItem,
                currentVisibleIndex = 2,
            )

        // Item at index 2 has p4 which matches activeItem's p4 (first-path fast check matches index
        // 2)
        // If currentItem at index 2 is equivalent, it returns null (position preserved!)
        assertNull(target)
    }

    @Test
    fun `resolveReanchorTarget re-anchors to pair containing constituent page when shifted to different index`() {
        val ch1 = createChapter(1L, pageCount = 10)
        val pages = ch1.pages!!

        // User was viewing spread [p4, p5]
        val activeItem = ReaderUiItem.Page(pages[4], pages[5])

        // Assume prepending an extra item moved [p3, p4] to index 5 and [p5, p6] to index 6
        val items =
            listOf(
                ReaderUiItem.Page(pages[0], null),
                ReaderUiItem.Page(pages[1], null),
                ReaderUiItem.Page(pages[2], null),
                ReaderUiItem.Page(pages[3], null),
                ReaderUiItem.Page(pages[4], null),
                ReaderUiItem.Page(pages[4], pages[5]),
            )

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = items,
                lastActiveItem = activeItem,
                currentVisibleIndex = 0, // Old viewport was at 0
            )

        assertNotNull(target)
        assertEquals(4, target!!.index) // First item matching p4 or p5
    }

    @Test
    fun `resolveReanchorTarget returns null for empty items`() {
        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = emptyList(),
                lastActiveItem = null,
                currentVisibleIndex = 0,
            )

        assertNull(target)
    }

    @Test
    fun `resolveReanchorTarget falls back to first page of next chapter when transition is replaced`() {
        val ch1 = createChapter(1L, pageCount = 5)
        val ch2 = createChapter(2L, pageCount = 5)

        val nextTransition = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))

        // New items no longer have Transition.Next, but have Ch2 pages loaded
        val newItems = ch2.pages!!.map { ReaderUiItem.Page(it) }

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastActiveItem = nextTransition,
                currentVisibleIndex = 5,
            )

        assertNotNull(target)
        assertEquals(0, target!!.index)
        assertEquals(2L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(0, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget falls back to last page of prev chapter when transition is replaced`() {
        val ch1 = createChapter(1L, pageCount = 5)
        val ch2 = createChapter(2L, pageCount = 5)

        val prevTransition = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))

        // New items no longer have Transition.Prev, but have Ch1 pages loaded followed by Ch2 pages
        val newItems = (ch1.pages!! + ch2.pages!!).map { ReaderUiItem.Page(it) }

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastActiveItem = prevTransition,
                currentVisibleIndex = 0,
            )

        assertNotNull(target)
        // Last page of Ch1 is at index 4
        assertEquals(4, target!!.index)
        assertEquals(1L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(4, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget in RTL mode falls back to page 0 of next chapter at last index when Next transition is replaced`() {
        val ch1 = createChapter(1L, pageCount = 5)
        val ch2 = createChapter(2L, pageCount = 5)

        val nextTransition = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))

        // In RTL, items are reversed: [Ch2 Page 4, Ch2 Page 3, ..., Ch2 Page 0]
        val rtlNewItems = ch2.pages!!.reversed().map { ReaderUiItem.Page(it) }

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = rtlNewItems,
                lastActiveItem = nextTransition,
                currentVisibleIndex = 0,
                isRtl = true,
                activeChapterId = 2L,
            )

        assertNotNull(target)
        // Page 0 of Ch2 is at the last index (4) in RTL list
        assertEquals(4, target!!.index)
        val targetPage1 = target.item as ReaderUiItem.Page
        assertEquals(2L, targetPage1.page.chapter.chapter.id)
        assertEquals(0, targetPage1.page.index)
    }

    @Test
    fun `resolveReanchorTarget in RTL mode falls back to last page of prev chapter at first index when Prev transition is replaced`() {
        val ch1 = createChapter(1L, pageCount = 5)
        val ch2 = createChapter(2L, pageCount = 5)

        val prevTransition = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))

        // In RTL, items are reversed: [Ch2 pages reversed, Ch1 pages reversed]
        val rtlNewItems =
            (ch2.pages!!.reversed() + ch1.pages!!.reversed()).map { ReaderUiItem.Page(it) }

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = rtlNewItems,
                lastActiveItem = prevTransition,
                currentVisibleIndex = 0,
                isRtl = true,
                activeChapterId = 1L,
            )

        assertNotNull(target)
        // Last page of Ch1 (Page 4) is at index 5 in the combined reversed list
        assertEquals(5, target!!.index)
        val targetPage2 = target.item as ReaderUiItem.Page
        assertEquals(1L, targetPage2.page.chapter.chapter.id)
        assertEquals(4, targetPage2.page.index)
    }

    @Test
    fun `resolveReanchorTarget anchors to activeChapterId start when activeItem belongs to obsolete chapter`() {
        val ch1 = createChapter(1L, pageCount = 5)
        val ch2 = createChapter(2L, pageCount = 5)

        val ch1LastPage = ch1.pages!!.last()
        val ch1ActiveItem = ReaderUiItem.Page(ch1LastPage)

        // Prepended 2 pages of ch1 + transition + ch2 pages
        val items =
            listOf(
                ReaderUiItem.Page(ch1.pages!![3]),
                ReaderUiItem.Page(ch1.pages!![4]),
                ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)),
                ReaderUiItem.Page(ch2.pages!![0]),
                ReaderUiItem.Page(ch2.pages!![1]),
            )

        // Target should anchor to start of Chapter 2 (index 3), NOT index 1 (the prepended Ch1
        // page)!
        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = items,
                lastActiveItem = ch1ActiveItem,
                currentVisibleIndex = 1,
                isRtl = false,
                activeChapterId = 2L,
            )

        assertNotNull(target)
        assertEquals(3, target!!.index)
        val targetPage3 = target.item as ReaderUiItem.Page
        assertEquals(2L, targetPage3.page.chapter.chapter.id)
        assertEquals(0, targetPage3.page.index)
    }

    @Test
    fun `resolveReanchorTarget in RTL mode anchors to activeChapterId start at last index when activeItem belongs to obsolete chapter`() {
        val ch1 = createChapter(1L, pageCount = 5)
        val ch2 = createChapter(2L, pageCount = 5)

        val ch1LastPage = ch1.pages!!.last()
        val ch1ActiveItem = ReaderUiItem.Page(ch1LastPage)

        // In RTL, Chapter 2 pages reversed + Transition + Chapter 1 pages reversed
        val items =
            listOf(
                ReaderUiItem.Page(ch2.pages!![1]),
                ReaderUiItem.Page(ch2.pages!![0]),
                ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)),
                ReaderUiItem.Page(ch1.pages!![4]),
                ReaderUiItem.Page(ch1.pages!![3]),
            )

        // Target should anchor to start of Chapter 2 (Page 0, index 1 in RTL list)
        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = items,
                lastActiveItem = ch1ActiveItem,
                currentVisibleIndex = 3,
                isRtl = true,
                activeChapterId = 2L,
            )

        assertNotNull(target)
        assertEquals(1, target!!.index)
        val targetPage = target.item as ReaderUiItem.Page
        assertEquals(2L, targetPage.page.chapter.chapter.id)
        assertEquals(0, targetPage.page.index)
    }

    @Test
    fun `resolveReanchorTarget in RTL mode preserves reading position when next chapter is appended to front`() {
        val ch2 = createChapter(2L, pageCount = 3)
        val ch3 = createChapter(3L, pageCount = 3)

        // Initial RTL items for Ch2: [Ch2 P2, Ch2 P1, Ch2 P0]
        val initialItems = ch2.pages!!.reversed().map { ReaderUiItem.Page(it) }
        val activeItem = initialItems[2] // User reading Ch2 P0 (index 2)

        // Next chapter Ch3 appended: in RTL, next chapter is at the beginning of the list
        // [Ch3 P2, Ch3 P1, Ch3 P0, NextTrans, Ch2 P2, Ch2 P1, Ch2 P0]
        val updatedItems =
            ch3.pages!!.reversed().map { ReaderUiItem.Page(it) } +
                listOf(ReaderUiItem.Transition(ChapterTransition.Next(ch2, ch3))) +
                initialItems

        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = updatedItems,
                lastActiveItem = activeItem,
                currentVisibleIndex = 2,
                previousItems = initialItems,
                isRtl = true,
                activeChapterId = 2L,
            )

        assertNotNull(target)
        // Shifted by 3 Ch3 pages + 1 transition = index 2 + 4 = 6
        assertEquals(6, target!!.index)
        val targetPage = target.item as ReaderUiItem.Page
        assertEquals(2L, targetPage.page.chapter.chapter.id)
        assertEquals(0, targetPage.page.index)
    }
}
