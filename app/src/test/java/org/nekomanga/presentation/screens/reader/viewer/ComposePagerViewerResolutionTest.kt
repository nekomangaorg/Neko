package org.nekomanga.presentation.screens.reader.viewer

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComposePagerViewerResolutionTest {

    private fun createChapter(id: Long, pageCount: Int = 5): ReaderChapter {
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
    fun `resolveItemIndexForPage resolves target chapter page 0 correctly when previous chapter is prepended`() {
        val ch1 = createChapter(1L, pageCount = 3)
        val ch2 = createChapter(2L, pageCount = 3)

        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

        // Items containing ch1 (prev chapter) + transition + ch2 (current chapter)
        val items =
            listOf(
                ReaderUiItem.Page(ch1Pages[0]),
                ReaderUiItem.Page(ch1Pages[1]),
                ReaderUiItem.Page(ch1Pages[2]),
                ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)),
                ReaderUiItem.Page(ch2Pages[0]),
                ReaderUiItem.Page(ch2Pages[1]),
                ReaderUiItem.Page(ch2Pages[2]),
            )

        // When navigating to Chapter 2 Page 0, it MUST resolve to index 4, NOT index 0!
        val resolved = resolveItemIndexForPage(items = items, targetChapterId = 2L, pageIndex = 0)
        assertEquals(4, resolved)
    }

    @Test
    fun `resolveItemIndexForPage resolves paired extraPage correctly`() {
        val ch2 = createChapter(2L, pageCount = 4)
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

        val items =
            listOf(
                ReaderUiItem.Page(page = ch2Pages[0], extraPage = ch2Pages[1]),
                ReaderUiItem.Page(page = ch2Pages[2], extraPage = ch2Pages[3]),
            )

        // Page 1 is the extraPage of item 0
        val resolvedPage1 =
            resolveItemIndexForPage(items = items, targetChapterId = 2L, pageIndex = 1)
        assertEquals(0, resolvedPage1)

        // Page 3 is the extraPage of item 1
        val resolvedPage3 =
            resolveItemIndexForPage(items = items, targetChapterId = 2L, pageIndex = 3)
        assertEquals(1, resolvedPage3)
    }

    @Test
    fun `resolveItemIndexForPage resolves SplitPage items correctly`() {
        val ch2 = createChapter(2L, pageCount = 2)
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

        val split = ReaderPageSplit(page = ch2Pages[1], topOffset = 0, splitHeight = 500)
        val items =
            listOf(
                ReaderUiItem.Page(page = ch2Pages[0]),
                ReaderUiItem.SplitPage(split = split),
            )

        val resolved = resolveItemIndexForPage(items = items, targetChapterId = 2L, pageIndex = 1)
        assertEquals(1, resolved)
    }

    @Test
    fun `resolveItemIndexForPage returns null when target chapter is not yet present in items`() {
        val ch1 = createChapter(1L, pageCount = 3)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages

        // Items only contain ch1, but ViewModel requests Chapter 2 Page 0
        val items = ch1Pages.map { ReaderUiItem.Page(it) }

        val resolved = resolveItemIndexForPage(items = items, targetChapterId = 2L, pageIndex = 0)
        // Must return null to defer navigation instead of mistakenly jumping to ch1 page 0
        assertNull(resolved)
    }

    @Test
    fun `resolveItemIndexForPage returns null for empty items`() {
        val resolved =
            resolveItemIndexForPage(items = emptyList(), targetChapterId = 2L, pageIndex = 0)
        assertNull(resolved)
    }
}
