package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.presentation.theme.Size

class ComposeWebtoonViewerTest {

    private fun createChapter(id: Long): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = id.toFloat()
            }
        return ReaderChapter(dbChapter)
    }

    @Test
    fun `areTransitionsEquivalent returns true for same type and same from chapter`() {
        val ch1 = createChapter(1L)
        val ch2 = createChapter(2L)

        val nextWithTo = ChapterTransition.Next(ch1, ch2)
        val nextWithoutTo = ChapterTransition.Next(ch1, null)

        assertTrue(areTransitionsEquivalent(nextWithTo, nextWithoutTo))
        assertTrue(areTransitionsEquivalent(nextWithoutTo, nextWithTo))
    }

    @Test
    fun `areTransitionsEquivalent returns true across forward and backward boundary between same chapters`() {
        val ch1 = createChapter(1L)
        val ch2 = createChapter(2L)

        val next = ChapterTransition.Next(ch1, ch2)
        val prev = ChapterTransition.Prev(ch2, ch1)

        assertTrue(areTransitionsEquivalent(next, prev))
        assertTrue(areTransitionsEquivalent(prev, next))
    }

    @Test
    fun `areTransitionsEquivalent returns false for transitions across different chapter boundaries`() {
        val ch1 = createChapter(1L)
        val ch2 = createChapter(2L)
        val ch3 = createChapter(3L)

        val next1to2 = ChapterTransition.Next(ch1, ch2)
        val next2to3 = ChapterTransition.Next(ch2, ch3)
        val prev3to2 = ChapterTransition.Prev(ch3, ch2)

        assertFalse(areTransitionsEquivalent(next1to2, next2to3))
        assertFalse(areTransitionsEquivalent(next1to2, prev3to2))
    }

    @Test
    fun `areItemsEquivalent matches equivalent boundary transitions`() {
        val ch1 = createChapter(1L)
        val ch2 = createChapter(2L)

        val itemNext = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))
        val itemPrev = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))

        assertTrue(areItemsEquivalent(itemNext, itemPrev))
        assertTrue(areItemsEquivalent(itemPrev, itemNext))
    }

    @Test
    fun `areItemsEquivalent matches pages and split pages appropriately`() {
        val ch1 = createChapter(1L)
        val page0 = ReaderPage(index = 0, url = "url_0", imageUrl = "img_0").apply { chapter = ch1 }
        val page1 = ReaderPage(index = 1, url = "url_1", imageUrl = "img_1").apply { chapter = ch1 }

        val itemPage0 = ReaderUiItem.Page(page0)
        val itemPage1 = ReaderUiItem.Page(page1)

        assertTrue(areItemsEquivalent(itemPage0, ReaderUiItem.Page(page0)))
        assertFalse(areItemsEquivalent(itemPage0, itemPage1))

        val splitPage0Top =
            ReaderUiItem.SplitPage(ReaderPageSplit(page = page0, topOffset = 0, splitHeight = 500))
        val splitPage0Bottom =
            ReaderUiItem.SplitPage(
                ReaderPageSplit(page = page0, topOffset = 500, splitHeight = 500)
            )

        assertTrue(areItemsEquivalent(itemPage0, splitPage0Top))
        assertFalse(areItemsEquivalent(itemPage0, splitPage0Bottom))
        assertTrue(areItemsEquivalent(splitPage0Top, splitPage0Top))
        assertFalse(areItemsEquivalent(splitPage0Top, splitPage0Bottom))
    }

    @Test
    fun `calculateEffectiveContentPadding applies sidePaddingPercent to start and end`() {
        val basePadding = PaddingValues(top = Size.small, bottom = Size.medium)
        val result =
            calculateEffectiveContentPadding(
                sidePadding = Size.none,
                sidePaddingPercent = 0.1f,
                maxWidth = 400.dp,
                contentPadding = basePadding,
                layoutDirection = LayoutDirection.Ltr,
            )

        assertEquals(40.dp, result.calculateStartPadding(LayoutDirection.Ltr))
        assertEquals(40.dp, result.calculateEndPadding(LayoutDirection.Ltr))
        assertEquals(Size.small, result.calculateTopPadding())
        assertEquals(Size.medium, result.calculateBottomPadding())
    }

    @Test
    fun `calculateEffectiveContentPadding prioritizes sidePadding Dp when greater than none`() {
        val basePadding = PaddingValues(top = Size.none, bottom = Size.none)
        val result =
            calculateEffectiveContentPadding(
                sidePadding = Size.large,
                sidePaddingPercent = 0.25f,
                maxWidth = 400.dp,
                contentPadding = basePadding,
                layoutDirection = LayoutDirection.Ltr,
            )

        assertEquals(Size.large, result.calculateStartPadding(LayoutDirection.Ltr))
        assertEquals(Size.large, result.calculateEndPadding(LayoutDirection.Ltr))
    }

    @Test
    fun `calculateEffectiveContentPadding combines with existing start and end contentPadding`() {
        val basePadding = PaddingValues(start = Size.small, end = Size.small)
        val result =
            calculateEffectiveContentPadding(
                sidePadding = Size.none,
                sidePaddingPercent = 0.1f,
                maxWidth = 200.dp,
                contentPadding = basePadding,
                layoutDirection = LayoutDirection.Ltr,
            )

        assertEquals(20.dp + Size.small, result.calculateStartPadding(LayoutDirection.Ltr))
        assertEquals(20.dp + Size.small, result.calculateEndPadding(LayoutDirection.Ltr))
    }
}
