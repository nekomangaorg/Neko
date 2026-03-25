package org.nekomanga.usecases.chapters

import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateChapterFilterUseCaseTest {

    private val useCase = CalculateChapterFilterUseCase()

    @Test
    fun `when option is All, returns a filter with showAll true`() {
        val current = MangaConstants.ChapterDisplay()
        val option =
            MangaConstants.ChapterDisplayOptions(
                displayType = MangaConstants.ChapterDisplayType.All,
                displayState = ToggleableState.On,
            )

        val result = useCase(current, option)

        assertTrue(result.showAll)
    }

    @Test
    fun `when option is Unread, updates unread state and sets showAll to false`() {
        val current = MangaConstants.ChapterDisplay(showAll = true)
        val option =
            MangaConstants.ChapterDisplayOptions(
                displayType = MangaConstants.ChapterDisplayType.Unread,
                displayState = ToggleableState.On,
            )

        val result = useCase(current, option)

        assertEquals(false, result.showAll)
        assertEquals(ToggleableState.On, result.unread)
    }

    @Test
    fun `when option is Bookmarked, updates bookmarked state and sets showAll to false`() {
        val current = MangaConstants.ChapterDisplay(showAll = true)
        val option =
            MangaConstants.ChapterDisplayOptions(
                displayType = MangaConstants.ChapterDisplayType.Bookmarked,
                displayState = ToggleableState.Indeterminate,
            )

        val result = useCase(current, option)

        assertEquals(false, result.showAll)
        assertEquals(ToggleableState.Indeterminate, result.bookmarked)
    }

    @Test
    fun `when option is Downloaded, updates downloaded state and sets showAll to false`() {
        val current = MangaConstants.ChapterDisplay(showAll = true)
        val option =
            MangaConstants.ChapterDisplayOptions(
                displayType = MangaConstants.ChapterDisplayType.Downloaded,
                displayState = ToggleableState.On,
            )

        val result = useCase(current, option)

        assertEquals(false, result.showAll)
        assertEquals(ToggleableState.On, result.downloaded)
    }

    @Test
    fun `when option is HideTitles, updates hideChapterTitles state only`() {
        val current = MangaConstants.ChapterDisplay(showAll = true, unread = ToggleableState.On)
        val option =
            MangaConstants.ChapterDisplayOptions(
                displayType = MangaConstants.ChapterDisplayType.HideTitles,
                displayState = ToggleableState.On,
            )

        val result = useCase(current, option)

        assertEquals(true, result.showAll) // Should remain unchanged
        assertEquals(ToggleableState.On, result.unread) // Should remain unchanged
        assertEquals(ToggleableState.On, result.hideChapterTitles)
    }

    @Test
    fun `when option is Available, updates available state and sets showAll to false`() {
        val current = MangaConstants.ChapterDisplay(showAll = true)
        val option =
            MangaConstants.ChapterDisplayOptions(
                displayType = MangaConstants.ChapterDisplayType.Available,
                displayState = ToggleableState.Indeterminate,
            )

        val result = useCase(current, option)

        assertEquals(false, result.showAll)
        assertEquals(ToggleableState.Indeterminate, result.available)
    }
}
