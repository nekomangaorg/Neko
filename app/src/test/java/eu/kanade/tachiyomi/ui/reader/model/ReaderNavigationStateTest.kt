package eu.kanade.tachiyomi.ui.reader.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderNavigationStateTest {

    @Test
    fun `ReaderChapterTransitionState Idle represents idle state`() {
        val state: ReaderChapterTransitionState = ReaderChapterTransitionState.Idle
        assertTrue(state is ReaderChapterTransitionState.Idle)
    }

    @Test
    fun `ReaderChapterTransitionState Loading holds target chapter and nav target`() {
        val state: ReaderChapterTransitionState =
            ReaderChapterTransitionState.Loading(
                targetChapterId = 42L,
                navTarget = ChapterNavTarget.Start,
            )
        assertTrue(state is ReaderChapterTransitionState.Loading)
        val loading = state as ReaderChapterTransitionState.Loading
        assertEquals(42L, loading.targetChapterId)
        assertEquals(ChapterNavTarget.Start, loading.navTarget)
    }

    @Test
    fun `ReaderChapterTransitionState Settling holds target chapter and target page`() {
        val state: ReaderChapterTransitionState =
            ReaderChapterTransitionState.Settling(
                targetChapterId = 42L,
                targetPage = 15,
            )
        assertTrue(state is ReaderChapterTransitionState.Settling)
        val settling = state as ReaderChapterTransitionState.Settling
        assertEquals(42L, settling.targetChapterId)
        assertEquals(15, settling.targetPage)
    }

    @Test
    fun `ReaderChapterTransitionState Error holds target chapter and throwable`() {
        val exception = IllegalStateException("Failed to load")
        val state: ReaderChapterTransitionState =
            ReaderChapterTransitionState.Error(
                targetChapterId = 42L,
                throwable = exception,
            )
        assertTrue(state is ReaderChapterTransitionState.Error)
        val error = state as ReaderChapterTransitionState.Error
        assertEquals(42L, error.targetChapterId)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun `ReaderNavCommand ScrollToPage holds page index and animation flag`() {
        val cmdAnimated = ReaderNavCommand.ScrollToPage(pageIndex = 5, animated = true)
        assertEquals(5, cmdAnimated.pageIndex)
        assertTrue(cmdAnimated.animated)

        val cmdNonAnimated = ReaderNavCommand.ScrollToPage(pageIndex = 5, animated = false)
        assertFalse(cmdNonAnimated.animated)

        val cmdWithChapter =
            ReaderNavCommand.ScrollToPage(pageIndex = 5, animated = true, chapterId = 100L)
        assertEquals(100L, cmdWithChapter.chapterId)
    }

    @Test
    fun `ReaderNavCommand SnapToPage holds target page index and chapter id`() {
        val cmd = ReaderNavCommand.SnapToPage(pageIndex = 12)
        assertEquals(12, cmd.pageIndex)
        assertEquals(null, cmd.chapterId)

        val cmdWithChapter = ReaderNavCommand.SnapToPage(pageIndex = 0, chapterId = 200L)
        assertEquals(0, cmdWithChapter.pageIndex)
        assertEquals(200L, cmdWithChapter.chapterId)
    }

    @Test
    fun `ReaderNavCommand ScrollToItem holds item index and animation flag`() {
        val cmd = ReaderNavCommand.ScrollToItem(itemIndex = 7, animated = false)
        assertEquals(7, cmd.itemIndex)
        assertFalse(cmd.animated)
    }

    @Test
    fun `ReaderNavCommand StepPage holds forward direction`() {
        val cmdForward = ReaderNavCommand.StepPage(forward = true)
        assertTrue(cmdForward.forward)

        val cmdBackward = ReaderNavCommand.StepPage(forward = false)
        assertFalse(cmdBackward.forward)
    }

    @Test
    fun `ReaderNavCommand ScrollByDelta holds delta amount`() {
        val cmd = ReaderNavCommand.ScrollByDelta(delta = 150.5f)
        assertEquals(150.5f, cmd.delta, 0.001f)
    }
}
