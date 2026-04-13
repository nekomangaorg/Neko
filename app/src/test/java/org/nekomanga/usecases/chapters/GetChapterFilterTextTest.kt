package org.nekomanga.usecases.chapters

import android.content.Context
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.R

class GetChapterFilterTextTest {

    private lateinit var context: Context
    private lateinit var getChapterFilterText: GetChapterFilterText

    @Before
    fun setUp() {
        context = mockk()
        getChapterFilterText = GetChapterFilterText(context)

        every { context.getString(R.string.read) } returns "Read"
        every { context.getString(R.string.unread) } returns "Unread"
        every { context.getString(R.string.downloaded) } returns "Downloaded"
        every { context.getString(R.string.not_downloaded) } returns "Not downloaded"
        every { context.getString(R.string.bookmarked) } returns "Bookmarked"
        every { context.getString(R.string.not_bookmarked) } returns "Not bookmarked"
        every { context.getString(R.string.available) } returns "Available"
        every { context.getString(R.string.unavailable) } returns "Unavailable"
        every { context.getString(R.string.language) } returns "Language"
        every { context.getString(R.string.scanlators) } returns "Scanlators"
        every { context.getString(R.string.sources) } returns "Sources"
    }

    @Test
    fun `test no filters applied`() {
        val display = MangaConstants.ChapterDisplay()
        val source = MangaConstants.ScanlatorFilter()
        val scanlator = MangaConstants.ScanlatorFilter()
        val language = MangaConstants.LanguageFilter()

        val result = getChapterFilterText(display, source, scanlator, language)

        assertEquals("", result)
    }

    @Test
    fun `test single display filter applied`() {
        val display = MangaConstants.ChapterDisplay(unread = ToggleableState.On)
        val source = MangaConstants.ScanlatorFilter()
        val scanlator = MangaConstants.ScanlatorFilter()
        val language = MangaConstants.LanguageFilter()

        val result = getChapterFilterText(display, source, scanlator, language)

        assertEquals("Unread", result)
    }

    @Test
    fun `test multiple filters applied`() {
        val display =
            MangaConstants.ChapterDisplay(
                unread = ToggleableState.Indeterminate,
                downloaded = ToggleableState.On,
            )
        val source = MangaConstants.ScanlatorFilter()
        val scanlator =
            MangaConstants.ScanlatorFilter(
                scanlators =
                    persistentListOf(MangaConstants.ScanlatorOption("test", disabled = true))
            )
        val language = MangaConstants.LanguageFilter()

        val result = getChapterFilterText(display, source, scanlator, language)

        assertEquals("Read, Downloaded, Scanlators", result)
    }
}
