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
}
