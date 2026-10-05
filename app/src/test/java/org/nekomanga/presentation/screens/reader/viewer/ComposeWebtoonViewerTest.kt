package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderNavCommand
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonScrollGatingPolicy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.presentation.theme.Size

class ComposeWebtoonViewerTest {

    private fun createChapter(id: Long, pageCount: Int = 0): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = id.toFloat()
            }
        val readerChapter = ReaderChapter(dbChapter)
        if (pageCount > 0) {
            val pages =
                (0 until pageCount).map { index ->
                    ReaderPage(index = index, url = "url_$index", imageUrl = "img_$index").apply {
                        this.chapter = readerChapter
                    }
                }
            readerChapter.state = ReaderChapter.State.Loaded(pages)
        }
        return readerChapter
    }

    private fun createConfig(
        activeChapterId: Long = 1L,
        animatedTransitions: Boolean = true,
    ): WebtoonViewerConfigUiModel {
        return WebtoonViewerConfigUiModel(
            activeChapterId = activeChapterId,
            animatedTransitions = animatedTransitions,
            navigator = mockk<ViewerNavigation>(relaxed = true),
        )
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

    @Test
    fun `page dispatch gating does not prematurely consume adjacent chapter page when initially stationary`() {
        var lastDispatchedPage: ReaderPage? = null
        var dispatchCount = 0

        val ch1 = createChapter(1L)
        val ch2 = createChapter(2L)

        val page1 = ReaderPage(10, "url_10", "img_10").apply { chapter = ch1 }
        val page2 = ReaderPage(0, "url_0", "img_0").apply { chapter = ch2 }

        // Initial state: page 1 of ch 1 was dispatched
        lastDispatchedPage = page1

        var activeChapterId = 1L
        var isScrollInProgress = false

        fun evaluateDispatch(candidate: ReaderPage) {
            val shouldDispatch =
                WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
                    activeChapterId = activeChapterId,
                    candidateChapterId = candidate.chapter.chapter.id,
                    isScrollInProgress = isScrollInProgress,
                )
            if (shouldDispatch && candidate != lastDispatchedPage) {
                lastDispatchedPage = candidate
                dispatchCount++
            }
        }

        // 1. Candidate is adjacent chapter (page2 of ch2), but scrolling stopped (settling / idle)
        evaluateDispatch(page2)
        assertEquals(0, dispatchCount)
        assertEquals(page1, lastDispatchedPage) // Not prematurely consumed!

        // 2. User starts scrolling into adjacent chapter
        isScrollInProgress = true
        evaluateDispatch(page2)
        assertEquals(1, dispatchCount)
        assertEquals(page2, lastDispatchedPage) // Dispatched!

        // 3. Next split slice of same page2 while scrolling
        evaluateDispatch(page2)
        assertEquals(1, dispatchCount) // Not redundantly dispatched for same page!
    }

    @Test
    fun `webtoon list entries put a gap below every item except the last`() {
        val ch1 = createChapter(1L)
        val page0 = ReaderPage(index = 0, url = "url_0", imageUrl = "img_0").apply { chapter = ch1 }
        val page1 = ReaderPage(index = 1, url = "url_1", imageUrl = "img_1").apply { chapter = ch1 }
        val items =
            listOf(
                ReaderUiItem.Page(page0),
                ReaderUiItem.Page(page1),
                ReaderUiItem.Transition(ChapterTransition.Next(ch1, null)),
            )

        val entries = items.toWebtoonListEntries()

        assertEquals(items, entries.map { it.item })
        assertEquals(listOf(true, true, false), entries.map { it.hasGapBelow })
    }

    @Test
    fun `webtoon list entries put no gap between slices of one tall page`() {
        val ch1 = createChapter(1L)
        val tallPage =
            ReaderPage(index = 0, url = "url_0", imageUrl = "img_0").apply { chapter = ch1 }
        val nextPage =
            ReaderPage(index = 1, url = "url_1", imageUrl = "img_1").apply { chapter = ch1 }
        val items =
            listOf(
                ReaderUiItem.SplitPage(
                    ReaderPageSplit(tallPage, topOffset = 0, splitHeight = 1000)
                ),
                ReaderUiItem.SplitPage(
                    ReaderPageSplit(tallPage, topOffset = 1000, splitHeight = 1000)
                ),
                ReaderUiItem.SplitPage(
                    ReaderPageSplit(tallPage, topOffset = 2000, splitHeight = 500)
                ),
                ReaderUiItem.Page(nextPage),
            )

        val entries = items.toWebtoonListEntries()

        assertEquals(listOf(false, false, true, false), entries.map { it.hasGapBelow })
    }

    @Test
    fun `webtoon list entries of an empty list are empty`() {
        assertTrue(emptyList<ReaderUiItem>().toWebtoonListEntries().isEmpty())
    }

    @Test
    fun `executeWebtoonNavCommand with ScrollToItem animates to clamped index and updates scroll anchor state`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L, animatedTransitions = true)
            val mockLazyListState = mockk<LazyListState>(relaxed = true)
            val scrollAnchorState = ScrollAnchorState()

            executeWebtoonNavCommand(
                command = ReaderNavCommand.ScrollToItem(itemIndex = 2, animated = true),
                lazyListState = mockLazyListState,
                items = items,
                config = config,
                scrollAnchorState = scrollAnchorState,
            )

            coVerify { mockLazyListState.animateScrollToItem(2) }
            assertEquals(items[2], scrollAnchorState.item)
            assertEquals(0, scrollAnchorState.offset)
        }

    @Test
    fun `executeWebtoonNavCommand with ScrollToItem non-animated calls scrollToItem`() = runTest {
        val ch1 = createChapter(1L, pageCount = 5)
        val items = (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
        val config = createConfig(activeChapterId = 1L, animatedTransitions = true)
        val mockLazyListState = mockk<LazyListState>(relaxed = true)
        val scrollAnchorState = ScrollAnchorState()

        executeWebtoonNavCommand(
            command = ReaderNavCommand.ScrollToItem(itemIndex = 3, animated = false),
            lazyListState = mockLazyListState,
            items = items,
            config = config,
            scrollAnchorState = scrollAnchorState,
        )

        coVerify { mockLazyListState.scrollToItem(3) }
        coVerify(exactly = 0) { mockLazyListState.animateScrollToItem(any()) }
        assertEquals(items[3], scrollAnchorState.item)
    }

    @Test
    fun `executeWebtoonNavCommand with ScrollToItem clamps target index when out of bounds`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 4)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L, animatedTransitions = false)
            val mockLazyListState = mockk<LazyListState>(relaxed = true)
            val scrollAnchorState = ScrollAnchorState()

            executeWebtoonNavCommand(
                command = ReaderNavCommand.ScrollToItem(itemIndex = 99, animated = false),
                lazyListState = mockLazyListState,
                items = items,
                config = config,
                scrollAnchorState = scrollAnchorState,
            )

            coVerify { mockLazyListState.scrollToItem(3) } // lastIndex
            assertEquals(items[3], scrollAnchorState.item)
        }

    @Test
    fun `executeWebtoonNavCommand with ScrollToPage resolves target item index and animates`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 3)
            val ch2 = createChapter(2L, pageCount = 3)
            val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
            val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages
            val items =
                listOf(
                    ReaderUiItem.Page(ch1Pages[0]),
                    ReaderUiItem.Page(ch1Pages[1]),
                    ReaderUiItem.Page(ch1Pages[2]),
                    ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2)),
                    ReaderUiItem.Page(ch2Pages[0]),
                    ReaderUiItem.Page(ch2Pages[1]),
                    ReaderUiItem.Page(ch2Pages[2]),
                )
            val config = createConfig(activeChapterId = 1L, animatedTransitions = true)
            val mockLazyListState = mockk<LazyListState>(relaxed = true)
            val scrollAnchorState = ScrollAnchorState()

            executeWebtoonNavCommand(
                command =
                    ReaderNavCommand.ScrollToPage(
                        pageIndex = 1,
                        chapterId = 2L,
                        animated = true,
                    ),
                lazyListState = mockLazyListState,
                items = items,
                config = config,
                scrollAnchorState = scrollAnchorState,
            )

            coVerify { mockLazyListState.animateScrollToItem(5) }
            assertEquals(items[5], scrollAnchorState.item)
        }

    @Test
    fun `executeWebtoonNavCommand with SnapToPage calls scrollToItem directly`() = runTest {
        val ch1 = createChapter(1L, pageCount = 5)
        val items = (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
        val config = createConfig(activeChapterId = 1L)
        val mockLazyListState = mockk<LazyListState>(relaxed = true)
        val scrollAnchorState = ScrollAnchorState()

        executeWebtoonNavCommand(
            command = ReaderNavCommand.SnapToPage(pageIndex = 2, chapterId = 1L),
            lazyListState = mockLazyListState,
            items = items,
            config = config,
            scrollAnchorState = scrollAnchorState,
        )

        coVerify { mockLazyListState.scrollToItem(2) }
        assertEquals(items[2], scrollAnchorState.item)
    }

    @Test
    fun `calculateWebtoonStepDelta returns 90 percent of viewport height when positive`() {
        assertEquals(900f, calculateWebtoonStepDelta(1000), 0.001f)
        assertEquals(1800f, calculateWebtoonStepDelta(2000), 0.001f)
    }

    @Test
    fun `calculateWebtoonStepDelta falls back to 500 when viewport height is zero or negative`() {
        assertEquals(500f, calculateWebtoonStepDelta(0), 0.001f)
        assertEquals(500f, calculateWebtoonStepDelta(-100), 0.001f)
    }

    @Test
    fun `calculateEffectiveScrollAmount divides scroll amount by zoom scale when zoomed`() {
        assertEquals(450f, calculateEffectiveScrollAmount(900f, 2f), 0.001f)
        assertEquals(200f, calculateEffectiveScrollAmount(300f, 1.5f), 0.001f)
        assertEquals(-450f, calculateEffectiveScrollAmount(-900f, 2f), 0.001f)
    }

    @Test
    fun `calculateEffectiveScrollAmount returns unscaled amount when zoom scale is zero or negative`() {
        assertEquals(900f, calculateEffectiveScrollAmount(900f, 0f), 0.001f)
        assertEquals(900f, calculateEffectiveScrollAmount(900f, -1f), 0.001f)
    }

    @Test
    fun `executeWebtoonNavCommand catches CancellationException and does not throw`() = runTest {
        val ch1 = createChapter(1L, pageCount = 3)
        val items = (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
        val config = createConfig(activeChapterId = 1L, animatedTransitions = true)
        val mockLazyListState = mockk<LazyListState>(relaxed = true)
        val scrollAnchorState = ScrollAnchorState()
        coEvery { mockLazyListState.animateScrollToItem(any()) } throws
            CancellationException("Interrupted by touch")

        executeWebtoonNavCommand(
            command = ReaderNavCommand.ScrollToItem(itemIndex = 1, animated = true),
            lazyListState = mockLazyListState,
            items = items,
            config = config,
            scrollAnchorState = scrollAnchorState,
        )
        // Passes if no exception is thrown
    }

    @Test
    fun `calculateDefaultWebtoonIndex resolves requested page in active chapter`() {
        val ch1 = createChapter(1L, pageCount = 3)
        val ch2 = createChapter(2L, pageCount = 3)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

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

        val index =
            calculateDefaultWebtoonIndex(items = items, currentChapterId = 2L, requestedPage = 1)
        assertEquals(5, index)
    }

    @Test
    fun `calculateDefaultWebtoonIndex defaults to first page of active chapter when requested page is null`() {
        val ch1 = createChapter(1L, pageCount = 2)
        val ch2 = createChapter(2L, pageCount = 3)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

        val items =
            listOf(
                ReaderUiItem.Page(ch1Pages[0]),
                ReaderUiItem.Page(ch1Pages[1]),
                ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)),
                ReaderUiItem.Page(ch2Pages[0]),
                ReaderUiItem.Page(ch2Pages[1]),
            )

        val index =
            calculateDefaultWebtoonIndex(items = items, currentChapterId = 2L, requestedPage = null)
        assertEquals(3, index)
    }

    @Test
    fun `calculateDefaultWebtoonIndex returns 0 for empty items`() {
        val index =
            calculateDefaultWebtoonIndex(
                items = emptyList(),
                currentChapterId = 1L,
                requestedPage = null,
            )
        assertEquals(0, index)
    }
}
