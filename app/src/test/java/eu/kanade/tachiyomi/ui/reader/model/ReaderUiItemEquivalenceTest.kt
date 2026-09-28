package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.data.database.models.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderUiItemEquivalenceTest {

    private fun createPage(index: Int, chapterId: Long): ReaderPage {
        val dbChapter =
            Chapter.create().apply {
                this.id = chapterId
                this.url = "/chapter/$chapterId"
                this.name = "Chapter $chapterId"
            }
        val readerChapter = ReaderChapter(dbChapter)
        return ReaderPage(index = index, url = "url_$index", imageUrl = "img_$index").apply {
            this.chapter = readerChapter
        }
    }

    @Test
    fun `items with identical page and chapter are equivalent`() {
        val pageA = ReaderUiItem.Page(createPage(index = 5, chapterId = 10L))
        val pageB = ReaderUiItem.Page(createPage(index = 5, chapterId = 10L))

        assertTrue(pageA.isEquivalentTo(pageB))
    }

    @Test
    fun `items with different chapters are not equivalent`() {
        val pageA = ReaderUiItem.Page(createPage(index = 5, chapterId = 10L))
        val pageB = ReaderUiItem.Page(createPage(index = 5, chapterId = 11L))

        assertFalse(pageA.isEquivalentTo(pageB))
    }

    @Test
    fun `re-anchoring finds correct index when previous chapter is prepended`() {
        val initialItems =
            listOf(
                ReaderUiItem.Page(createPage(index = 0, chapterId = 2L)),
                ReaderUiItem.Page(createPage(index = 1, chapterId = 2L)),
            )
        val anchorItem = initialItems[1]

        // Prepend 50 pages from previous chapter
        val prependedPages =
            (0 until 50).map { ReaderUiItem.Page(createPage(index = it, chapterId = 1L)) }
        val updatedItems = prependedPages + initialItems

        val reanchoredIndex = updatedItems.indexOfFirst { it.isEquivalentTo(anchorItem) }
        assertEquals(51, reanchoredIndex)
    }

    @Test
    fun `re-anchoring preserves position when dual page pairs are shifted`() {
        val p4 = createPage(index = 4, chapterId = 1L)
        val p5 = createPage(index = 5, chapterId = 1L)
        val p6 = createPage(index = 6, chapterId = 1L)

        // Before shift: [p4, p5]
        val activeItem = ReaderUiItem.Page(p4, p5)

        // After shift: [p3, p4], [p5, p6]
        val shiftedItems =
            listOf(
                ReaderUiItem.Page(createPage(index = 3, chapterId = 1L), p4),
                ReaderUiItem.Page(p5, p6),
            )

        // User was looking at the spread containing p5; equivalence finds the pair containing p5
        // (or p4)
        val reanchoredIndex = shiftedItems.indexOfFirst { it.isEquivalentTo(activeItem) }
        assertEquals(0, reanchoredIndex) // p4 matches first pair
    }

    @Test
    fun `Page and SplitPage are equivalent when topOffset is 0`() {
        val page = createPage(index = 2, chapterId = 10L)
        val pageItem = ReaderUiItem.Page(page)
        val splitItem =
            ReaderUiItem.SplitPage(ReaderPageSplit(page = page, topOffset = 0, splitHeight = 1000))

        assertTrue(pageItem.isEquivalentTo(splitItem))
        assertTrue(splitItem.isEquivalentTo(pageItem))
    }

    @Test
    fun `Page and SplitPage are not equivalent when topOffset is greater than 0`() {
        val page = createPage(index = 2, chapterId = 10L)
        val pageItem = ReaderUiItem.Page(page)
        val splitItem =
            ReaderUiItem.SplitPage(
                ReaderPageSplit(page = page, topOffset = 1000, splitHeight = 1000)
            )

        assertFalse(pageItem.isEquivalentTo(splitItem))
        assertFalse(splitItem.isEquivalentTo(pageItem))
    }

    @Test
    fun `SplitPage items are equivalent only when chapter, page, and topOffset match`() {
        val page = createPage(index = 2, chapterId = 10L)
        val splitA =
            ReaderUiItem.SplitPage(
                ReaderPageSplit(page = page, topOffset = 500, splitHeight = 1000)
            )
        val splitB =
            ReaderUiItem.SplitPage(
                ReaderPageSplit(page = page, topOffset = 500, splitHeight = 1000)
            )
        val splitDifferentOffset =
            ReaderUiItem.SplitPage(
                ReaderPageSplit(page = page, topOffset = 1500, splitHeight = 1000)
            )

        assertTrue(splitA.isEquivalentTo(splitB))
        assertFalse(splitA.isEquivalentTo(splitDifferentOffset))
    }

    @Test
    fun `Transitions connecting same chapters in opposite directions are equivalent`() {
        val ch1 = createPage(0, 1L).chapter
        val ch2 = createPage(0, 2L).chapter

        val nextTransition = ReaderUiItem.Transition(ChapterTransition.Next(from = ch1, to = ch2))
        val prevTransition = ReaderUiItem.Transition(ChapterTransition.Prev(from = ch2, to = ch1))

        assertTrue(nextTransition.isEquivalentTo(prevTransition))
        assertTrue(prevTransition.isEquivalentTo(nextTransition))
    }

    @Test
    fun `Transitions connecting different chapters are not equivalent`() {
        val ch1 = createPage(0, 1L).chapter
        val ch2 = createPage(0, 2L).chapter
        val ch3 = createPage(0, 3L).chapter

        val trans12 = ReaderUiItem.Transition(ChapterTransition.Next(from = ch1, to = ch2))
        val trans23 = ReaderUiItem.Transition(ChapterTransition.Next(from = ch2, to = ch3))

        assertFalse(trans12.isEquivalentTo(trans23))
    }

    @Test
    fun `split wide page halves are not equivalent when firstHalf differs`() {
        val p1 = createPage(index = 2, chapterId = 10L).apply { firstHalf = true }
        val p2 = createPage(index = 2, chapterId = 10L).apply { firstHalf = false }

        val half1 = ReaderUiItem.Page(p1)
        val half2 = ReaderUiItem.Page(p2)

        assertFalse(half1.isEquivalentTo(half2))
    }
}
