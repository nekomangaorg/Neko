package eu.kanade.tachiyomi.ui.reader.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageTest {

    @Test
    fun `a page that was never retried is at generation 0`() {
        assertEquals(0, ReaderPage(0).retryGeneration)
    }

    @Test
    fun `each retry is higher than every earlier retry of any page`() {
        val page = ReaderPage(0)
        val otherPage = ReaderPage(1)

        page.retry()
        val first = page.retryGeneration
        otherPage.retry()
        page.retry()

        assertTrue(first > 0)
        assertTrue(otherPage.retryGeneration > first)
        assertTrue(page.retryGeneration > otherPage.retryGeneration)
        assertEquals(page.retryGeneration, page.retryGenerationFlow.value)
    }

    @Test
    fun `a page created again starts below its earlier retries`() {
        val page = ReaderPage(0)
        page.retry()

        assertTrue(ReaderPage(0).retryGeneration < page.retryGeneration)
    }
}
