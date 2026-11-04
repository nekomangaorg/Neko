package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SwapViewerChaptersTest {

    private fun createChapter(id: Long): ReaderChapter =
        ReaderChapter(
            Chapter.create().apply {
                this.id = id
                url = "/chapter/$id"
                name = "Chapter $id"
            }
        )

    @Test
    fun `swap counts each reference once when releasing a chapter writes the state`() = runTest {
        // Given the reader on chapter 2 while chapter 3 is still loading, and a collector that
        // writes the state when chapter 3 is recycled, as the transition card collector does
        val chapter0 = createChapter(0)
        val chapter1 = createChapter(1)
        val chapter2 = createChapter(2)
        val chapter3 = createChapter(3)
        val oldChapters = ViewerChapters(chapter2, chapter1, chapter3).apply { ref() }
        chapter3.state = ReaderChapter.State.Loading
        val state = MutableStateFlow(ReaderViewModel.State(viewerChapters = oldChapters))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            chapter3.stateFlow
                .filter { it == ReaderChapter.State.Wait }
                .collect { state.update { it.copy(lastPage = (it.lastPage ?: 0) + 1) } }
        }

        // When the reader goes back to chapter 1
        val newChapters = ViewerChapters(chapter1, chapter0, chapter2)
        state.swapViewerChapters(newChapters)

        // Then the collector wrote once, each chapter in the new set holds one reference and the
        // dropped chapter 3 holds none
        assertSame(newChapters, state.value.viewerChapters)
        assertEquals(1, state.value.lastPage)
        assertEquals(
            listOf(1, 1, 1, 0),
            listOf(chapter0, chapter1, chapter2, chapter3).map { it.references },
        )
    }
}
