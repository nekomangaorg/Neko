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
}
