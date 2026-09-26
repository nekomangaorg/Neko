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

    @Test
    fun `resolveTransitionIndexForChapter finds transition item leading to targetChapterId`() {
        val ch1 = createChapter(1L, pageCount = 3)
        val ch2 = createChapter(2L, pageCount = 3)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages

        val items =
            listOf(
                ReaderUiItem.Page(ch1Pages[0]),
                ReaderUiItem.Page(ch1Pages[1]),
                ReaderUiItem.Page(ch1Pages[2]),
                ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2)),
            )

        val transitionIndex = resolveTransitionIndexForChapter(items = items, targetChapterId = 2L)
        assertEquals(3, transitionIndex)
    }

    @Test
    fun `resolveTransitionIndexForChapter returns null when transition does not lead to targetChapterId`() {
        val ch1 = createChapter(1L, pageCount = 3)
        val ch2 = createChapter(2L, pageCount = 3)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages

        val items =
            listOf(
                ReaderUiItem.Page(ch1Pages[0]),
                ReaderUiItem.Page(ch1Pages[1]),
                ReaderUiItem.Page(ch1Pages[2]),
                ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2)),
            )

        // Target chapter 1 is 'from', not 'to'
        assertNull(resolveTransitionIndexForChapter(items = items, targetChapterId = 1L))
        // Target chapter 3 has no transition
        assertNull(resolveTransitionIndexForChapter(items = items, targetChapterId = 3L))
    }

    @Test
    fun `resolveTransitionIndexForChapter returns null when transition destination is null`() {
        val ch1 = createChapter(1L, pageCount = 2)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages

        val items =
            listOf(
                ReaderUiItem.Page(ch1Pages[0]),
                ReaderUiItem.Page(ch1Pages[1]),
                ReaderUiItem.Transition(ChapterTransition.Next(ch1, null)),
            )

        assertNull(resolveTransitionIndexForChapter(items = items, targetChapterId = 2L))
    }

    @Test
    fun `resolveTransitionIndexForChapter returns null for empty items or non-positive target ids`() {
        assertNull(resolveTransitionIndexForChapter(items = emptyList(), targetChapterId = 2L))
        assertNull(resolveTransitionIndexForChapter(items = emptyList(), targetChapterId = null))
        assertNull(resolveTransitionIndexForChapter(items = emptyList(), targetChapterId = 0L))
        assertNull(resolveTransitionIndexForChapter(items = emptyList(), targetChapterId = -1L))
    }

    @Test
    fun `resolveItemIndexForPage returns null when target chapter exists but page index is not in chapter`() {
        val ch1 = createChapter(1L, pageCount = 15)
        val ch2 = createChapter(2L, pageCount = 3)

        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

        val items =
            listOf(
                ReaderUiItem.Page(ch1Pages[10]), // Ch1 has page 10
                ReaderUiItem.Page(ch2Pages[0]), // Ch2 has page 0
                ReaderUiItem.Page(ch2Pages[1]), // Ch2 has page 1
                ReaderUiItem.Page(ch2Pages[2]), // Ch2 has page 2
            )

        // Target Chapter 2 Page 10 does not exist in Chapter 2, even though Chapter 1 has page 10.
        // It MUST return null, NOT navigate to Chapter 1 Page 10!
        val resolved = resolveItemIndexForPage(items = items, targetChapterId = 2L, pageIndex = 10)
        assertNull(resolved)
    }

    @Test
    fun `resolveItemIndexForPage falls back to page index match and clamping when targetChapterId is null`() {
        val ch1 = createChapter(1L, pageCount = 5)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages

        val items = ch1Pages.map { ReaderUiItem.Page(it) }

        // Matches page index across items when targetChapterId is null
        assertEquals(
            3,
            resolveItemIndexForPage(items = items, targetChapterId = null, pageIndex = 3),
        )
        // Clamps within bounds when out of range and targetChapterId is null
        assertEquals(
            4,
            resolveItemIndexForPage(items = items, targetChapterId = null, pageIndex = 99),
        )
    }
}
