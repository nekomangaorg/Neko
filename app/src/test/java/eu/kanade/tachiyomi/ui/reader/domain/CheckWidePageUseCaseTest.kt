package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckWidePageUseCaseTest {

    private val useCase = CheckWidePageUseCase()

    private fun createPage(index: Int = 0): ReaderPage {
        val dbChapter =
            Chapter.create().apply {
                id = 1L
                url = "/chapter/1"
                name = "Chapter 1"
            }
        val chapter = ReaderChapter(dbChapter)
        return ReaderPage(index = index, url = "url", imageUrl = "img").apply {
            this.chapter = chapter
        }
    }

    @Test
    fun `isWidePage returns true when width is greater than height`() {
        assertTrue(useCase.isWidePage(1920, 1080))
        assertTrue(useCase.isWidePage(1200, 800))
    }

    @Test
    fun `isWidePage returns false when height is greater than or equal to width`() {
        assertFalse(useCase.isWidePage(1080, 1920))
        assertFalse(useCase.isWidePage(1000, 1000))
    }

    @Test
    fun `isWidePage returns false for non-positive dimensions`() {
        assertFalse(useCase.isWidePage(0, 1080))
        assertFalse(useCase.isWidePage(1920, 0))
        assertFalse(useCase.isWidePage(-1, -1))
    }

    @Test
    fun `invoking with wide dimensions sets fullPage and longPage to true`() {
        val page = createPage()
        assertFalse(page.fullPage == true)
        assertFalse(page.longPage == true)

        val result = useCase(page, 2000, 1000)

        assertTrue(result)
        assertTrue(page.fullPage == true)
        assertTrue(page.longPage == true)
    }

    @Test
    fun `invoking with portrait dimensions does not set fullPage or longPage`() {
        val page = createPage()

        val result = useCase(page, 1000, 2000)

        assertFalse(result)
        assertFalse(page.fullPage == true)
        assertFalse(page.longPage == true)
    }
}
