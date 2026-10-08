package eu.kanade.tachiyomi.ui.reader.model

import coil3.request.Options
import eu.kanade.tachiyomi.data.coil.ReaderPageKeyer
import eu.kanade.tachiyomi.data.coil.ReaderPageSplitKeyer
import eu.kanade.tachiyomi.data.database.models.Chapter
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.presentation.screens.reader.viewer.ChapterTransitionUiModel
import org.nekomanga.presentation.screens.reader.viewer.ChapterTransitionUiModel.PreloadState

class ReaderUiItemTest {

    private fun createReaderChapter(chapterId: Long): ReaderChapter {
        val chapter =
            Chapter.create().apply {
                id = chapterId
                url = "/chapter/$chapterId"
                name = "Chapter $chapterId"
            }
        return ReaderChapter(chapter)
    }

    private fun createReaderPage(
        chapterId: Long,
        index: Int,
        firstHalf: Boolean? = null,
    ): ReaderPage {
        val readerChapter = createReaderChapter(chapterId)
        return ReaderPage(index = index, url = "url_$index", imageUrl = "img_$index").apply {
            chapter = readerChapter
            this.firstHalf = firstHalf
        }
    }

    @Test
    fun `single page generates correct key`() {
        val page = createReaderPage(chapterId = 102992L, index = 0)
        val item = ReaderUiItem.Page(page)

        assertEquals("pager_page_102992_0", item.key("pager"))
        assertEquals("webtoon_page_102992_0", item.key("webtoon"))
    }

    @Test
    fun `split wide pages generate unique keys for first and second halves`() {
        val firstHalf = createReaderPage(chapterId = 102992L, index = 0, firstHalf = true)
        val secondHalf = createReaderPage(chapterId = 102992L, index = 0, firstHalf = false)

        val firstItem = ReaderUiItem.Page(firstHalf)
        val secondItem = ReaderUiItem.Page(secondHalf)

        assertEquals("pager_page_102992_0_half_true", firstItem.key("pager"))
        assertEquals("pager_page_102992_0_half_false", secondItem.key("pager"))
        assertNotEquals(firstItem.key("pager"), secondItem.key("pager"))
    }

    @Test
    fun `paired double page spread generates combined key`() {
        val page1 = createReaderPage(chapterId = 102992L, index = 0)
        val page2 = createReaderPage(chapterId = 102992L, index = 1)

        val item = ReaderUiItem.Page(page1, page2)

        assertEquals("pager_page_102992_0_102992_1", item.key("pager"))
    }

    @Test
    fun `transitions between same chapters generate symmetric keys across directions`() {
        val chapter1 = createReaderChapter(100L)
        val chapter2 = createReaderChapter(200L)

        val prevTransition = ChapterTransition.Prev(from = chapter2, to = chapter1)
        val nextTransition = ChapterTransition.Next(from = chapter1, to = chapter2)

        val prevItem = ReaderUiItem.Transition(prevTransition)
        val nextItem = ReaderUiItem.Transition(nextTransition)

        assertEquals("pager_transition_100_200", prevItem.key("pager"))
        assertEquals("pager_transition_100_200", nextItem.key("pager"))
        assertEquals(prevItem.key("pager"), nextItem.key("pager"))
    }

    @Test
    fun `reader ui items expose polymorphic chapterId and pageIndex`() {
        val page = createReaderPage(chapterId = 102992L, index = 3)
        val split = ReaderPageSplit(page = page, topOffset = 0, splitHeight = 1000)
        val chapter1 = createReaderChapter(100L)
        val chapter2 = createReaderChapter(200L)
        val transition = ChapterTransition.Next(from = chapter1, to = chapter2)

        val pageItem = ReaderUiItem.Page(page)
        val splitItem = ReaderUiItem.SplitPage(split)
        val transitionItem = ReaderUiItem.Transition(transition)

        assertEquals(102992L, pageItem.chapterId)
        assertEquals(3, pageItem.pageIndex)

        assertEquals(102992L, splitItem.chapterId)
        assertEquals(3, splitItem.pageIndex)

        assertNull(transitionItem.chapterId)
        assertNull(transitionItem.pageIndex)
    }

    @Test
    fun `split pages generate distinct keys for different offsets`() {
        val page = createReaderPage(chapterId = 102992L, index = 0)
        val split1 = ReaderPageSplit(page = page, topOffset = 0, splitHeight = 1000)
        val split2 = ReaderPageSplit(page = page, topOffset = 1000, splitHeight = 1000)

        val item1 = ReaderUiItem.SplitPage(split1)
        val item2 = ReaderUiItem.SplitPage(split2)

        assertEquals("webtoon_split_102992_0_0", item1.key("webtoon"))
        assertEquals("webtoon_split_102992_0_1000", item2.key("webtoon"))
        assertNotEquals(item1.key("webtoon"), item2.key("webtoon"))
    }

    @Test
    fun `reader page keyer uses composite fallback when chapter id is null`() {
        val chapter =
            Chapter.create().apply {
                id = null
                url = "/chapter/test-url"
                name = "Test Chapter"
            }
        val readerChapter = ReaderChapter(chapter)
        val page = ReaderPage(index = 2, url = "url_2").apply { this.chapter = readerChapter }
        val split = ReaderPageSplit(page = page, topOffset = 500, splitHeight = 1000)

        val options = mockk<Options>(relaxed = true)
        val pageKey = ReaderPageKeyer().key(page, options)
        val splitKey = ReaderPageSplitKeyer().key(split, options)

        val expectedHash = "/chapter/test-url".hashCode()
        assertEquals("reader_page_${expectedHash}_2", pageKey)
        assertEquals("reader_split_${expectedHash}_2_500", splitKey)
    }

    @Test
    fun `reader page keyer uses chapter id when non-null`() {
        val page = createReaderPage(chapterId = 12345L, index = 1)
        val split = ReaderPageSplit(page = page, topOffset = 250, splitHeight = 1000)

        val options = mockk<Options>(relaxed = true)
        val pageKey = ReaderPageKeyer().key(page, options)
        val splitKey = ReaderPageSplitKeyer().key(split, options)

        assertEquals("reader_page_12345_1", pageKey)
        assertEquals("reader_split_12345_1_250", splitKey)
    }

    @Test
    fun `slice continues into the next slice of the same page`() {
        val page = createReaderPage(chapterId = 102992L, index = 0)
        val top = ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 0, splitHeight = 1000))
        val bottom =
            ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 1000, splitHeight = 1000))

        assertTrue(top.continuesInto(bottom))
    }

    @Test
    fun `last slice does not continue into the next page`() {
        val page = createReaderPage(chapterId = 102992L, index = 0)
        val lastSlice =
            ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 1000, splitHeight = 1000))
        val nextPage = ReaderUiItem.Page(createReaderPage(chapterId = 102992L, index = 1))

        assertFalse(lastSlice.continuesInto(nextPage))
    }

    @Test
    fun `slice does not continue into a slice of another page that starts where it ends`() {
        val page = createReaderPage(chapterId = 102992L, index = 0)
        val nextPage = createReaderPage(chapterId = 102992L, index = 1)
        val lastSlice =
            ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 1000, splitHeight = 1000))
        val nextPageSlice =
            ReaderUiItem.SplitPage(ReaderPageSplit(nextPage, topOffset = 2000, splitHeight = 1000))

        assertFalse(lastSlice.continuesInto(nextPageSlice))
    }

    @Test
    fun `slice does not continue into a slice with the same page index in another chapter`() {
        val lastSlice =
            ReaderUiItem.SplitPage(
                ReaderPageSplit(
                    createReaderPage(chapterId = 100L, index = 0),
                    topOffset = 1000,
                    splitHeight = 1000,
                )
            )
        val otherChapterSlice =
            ReaderUiItem.SplitPage(
                ReaderPageSplit(
                    createReaderPage(chapterId = 200L, index = 0),
                    topOffset = 2000,
                    splitHeight = 1000,
                )
            )

        assertFalse(lastSlice.continuesInto(otherChapterSlice))
    }

    @Test
    fun `pages, transitions and the end of the list never continue`() {
        val page = createReaderPage(chapterId = 100L, index = 0)
        val slice = ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 0, splitHeight = 1000))
        val transition =
            ReaderUiItem.Transition(
                ChapterTransition.Next(
                    from = createReaderChapter(100L),
                    to = createReaderChapter(200L),
                )
            )

        assertFalse(ReaderUiItem.Page(page).continuesInto(ReaderUiItem.Page(page)))
        assertFalse(slice.continuesInto(transition))
        assertFalse(transition.continuesInto(slice))
        assertFalse(slice.continuesInto(null))
    }

    @Test
    fun `slice does not continue into a duplicate slice`() {
        val page = createReaderPage(chapterId = 100L, index = 0)
        val slice = ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 0, splitHeight = 1000))
        val duplicate =
            ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 0, splitHeight = 1000))

        assertFalse(slice.continuesInto(duplicate))
    }

    @Test
    fun `slice does not continue into an out-of-order slice`() {
        val page = createReaderPage(chapterId = 100L, index = 0)
        val lower =
            ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 1000, splitHeight = 1000))
        val upper = ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 0, splitHeight = 1000))

        assertFalse(lower.continuesInto(upper))
    }

    @Test
    fun `slice does not continue into a non-contiguous slice`() {
        val page = createReaderPage(chapterId = 100L, index = 0)
        val top = ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 0, splitHeight = 1000))
        val afterMissingSlice =
            ReaderUiItem.SplitPage(ReaderPageSplit(page, topOffset = 2000, splitHeight = 1000))

        assertFalse(top.continuesInto(afterMissingSlice))
    }

    private fun transitionItem(
        from: ReaderChapter,
        to: ReaderChapter?,
        preloadState: PreloadState = PreloadState.Ready,
        isNext: Boolean = true,
    ): ReaderUiItem.Transition {
        val target = to?.let {
            ChapterTransitionUiModel.TargetChapterInfo(
                chapterId = it.chapter.id!!,
                name = it.chapter.name,
                preloadState = preloadState,
            )
        }
        return if (isNext) {
            ReaderUiItem.Transition(
                transition = ChapterTransition.Next(from = from, to = to),
                transitionUiModel =
                    ChapterTransitionUiModel.Next(
                        fromChapterName = from.chapter.name,
                        toChapter = target,
                    ),
            )
        } else {
            ReaderUiItem.Transition(
                transition = ChapterTransition.Prev(from = from, to = to),
                transitionUiModel =
                    ChapterTransitionUiModel.Prev(
                        fromChapterName = from.chapter.name,
                        toChapter = target,
                    ),
            )
        }
    }

    /** The state the card at [index] shows after the items take the live target states. */
    private fun List<ReaderUiItem>.shownPreloadState(index: Int): PreloadState? =
        when (
            val model =
                (withLivePreloadStates()[index] as ReaderUiItem.Transition).transitionUiModel
        ) {
            is ChapterTransitionUiModel.Next -> model.toChapter?.preloadState
            is ChapterTransitionUiModel.Prev -> model.toChapter?.preloadState
            null -> null
        }

    @Test
    fun `transition shows a preload error raised after the items were built`() {
        val next = createReaderChapter(2L)
        val items = listOf(transitionItem(from = createReaderChapter(1L), to = next))

        next.state = ReaderChapter.State.Error(Exception("Error chapter is region locked"))

        assertEquals(
            PreloadState.Error("Error chapter is region locked"),
            items.shownPreloadState(0),
        )
    }

    @Test
    fun `prev transition shows a retry in progress`() {
        val prev = createReaderChapter(1L)
        val items =
            listOf(
                transitionItem(
                    from = createReaderChapter(2L),
                    to = prev,
                    preloadState = PreloadState.Error("timeout"),
                    isNext = false,
                )
            )

        prev.state = ReaderChapter.State.Loading

        assertEquals(PreloadState.Loading, items.shownPreloadState(0))
    }

    @Test
    fun `transition drops the error once the chapter loads`() {
        val next = createReaderChapter(2L)
        val items =
            listOf(
                transitionItem(
                    from = createReaderChapter(1L),
                    to = next,
                    preloadState = PreloadState.Error("timeout"),
                )
            )

        next.state = ReaderChapter.State.Loaded(emptyList())

        assertEquals(PreloadState.Ready, items.shownPreloadState(0))
    }

    @Test
    fun `live states keep the same list when every card is current`() {
        val next = createReaderChapter(2L)
        next.state = ReaderChapter.State.Error(Exception("timeout"))
        val items =
            listOf(
                ReaderUiItem.Page(createReaderPage(chapterId = 1L, index = 0)),
                transitionItem(
                    from = createReaderChapter(1L),
                    to = next,
                    preloadState = PreloadState.Error("timeout"),
                ),
                transitionItem(from = createReaderChapter(2L), to = null),
            )

        assertSame(items, items.withLivePreloadStates())
    }

    @Test
    fun `live states replace only the transition that changed`() {
        val next = createReaderChapter(2L)
        val page = ReaderUiItem.Page(createReaderPage(chapterId = 1L, index = 0))
        val items = listOf(page, transitionItem(from = createReaderChapter(1L), to = next))

        next.state = ReaderChapter.State.Error(Exception("timeout"))
        val updated = items.withLivePreloadStates()

        assertSame(page, updated[0])
        assertSame(
            (items[1] as ReaderUiItem.Transition).transition,
            (updated[1] as ReaderUiItem.Transition).transition,
        )
        assertEquals(PreloadState.Error("timeout"), items.shownPreloadState(1))
    }

    @Test
    fun `a card state update is not a new active item`() {
        val next = createReaderChapter(2L)
        val before = transitionItem(from = createReaderChapter(1L), to = next)
        val after =
            transitionItem(
                from = createReaderChapter(1L),
                to = next,
                preloadState = PreloadState.Error("timeout"),
            )
        val page = ReaderUiItem.Page(createReaderPage(chapterId = 1L, index = 0))

        assertNotEquals(before, after)
        assertEquals(before.withoutCardModel(), after.withoutCardModel())
        assertSame(page, page.withoutCardModel())
    }

    @Test
    fun `automatic preloads skip a chapter that failed and Retry does not`() {
        val error = ReaderChapter.State.Error(Exception("timeout"))

        assertTrue(ReaderChapter.State.Wait.allowsPreload(isRetry = false))
        assertFalse(error.allowsPreload(isRetry = false))
        assertTrue(error.allowsPreload(isRetry = true))
        assertFalse(ReaderChapter.State.Loading.allowsPreload(isRetry = true))
        assertFalse(ReaderChapter.State.Loaded(emptyList()).allowsPreload(isRetry = true))
    }
}
