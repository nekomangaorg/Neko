package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebtoonScrollGatingPolicyTest {

    @Test
    fun `when list is idle and candidate is from adjacent chapter, transition is rejected`() {
        val result =
            WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
                activeChapterId = 2L,
                candidateChapterId = 1L,
                isScrollInProgress = false,
            )
        assertFalse(result)
    }

    @Test
    fun `when list is actively scrolling and candidate is from adjacent chapter, transition is accepted`() {
        val result =
            WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
                activeChapterId = 2L,
                candidateChapterId = 1L,
                isScrollInProgress = true,
            )
        assertTrue(result)
    }

    @Test
    fun `when candidate is from current chapter, selection is always accepted even if idle`() {
        val result =
            WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
                activeChapterId = 2L,
                candidateChapterId = 2L,
                isScrollInProgress = false,
            )
        assertTrue(result)
    }

    @Test
    fun `when activeChapterId is null, selection is accepted even if idle`() {
        val result =
            WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
                activeChapterId = null,
                candidateChapterId = 1L,
                isScrollInProgress = false,
            )
        assertTrue(result)
    }
}
