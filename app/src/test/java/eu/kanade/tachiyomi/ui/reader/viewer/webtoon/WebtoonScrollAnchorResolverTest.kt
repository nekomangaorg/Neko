package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WebtoonScrollAnchorResolverTest {

    private fun createChapter(id: Long, pageCount: Int = 10): ReaderChapter {
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
                    this.renderedHeight = 1000
                }
            }
        readerChapter.state = ReaderChapter.State.Loaded(pages)
        return readerChapter
    }

    @Test
    fun `resolveReanchorTarget preserves scroll on chapter boundary forward transition pruning (preventing jump to end)`() {
        val ch1 = createChapter(1L, pageCount = 20)
        val ch2 = createChapter(2L, pageCount = 20)

        // Before transition: Chapter 1 (20 pages), Transition.Next, Chapter 2 (20 pages)
        // Total 41 items. User scrolls through transition to Chapter 2 Page 1 (index 21)
        val oldCh2Page1 = ReaderUiItem.Page(ch2.pages!![0])
        val lastFirstVisibleOffset = 180

        // When Chapter 2 becomes current, Chapter 1 is pruned down to its last 2 pages:
        // Indices 0..1: Ch 1 Pages 18..19
        // Index 2: Transition.Prev
        // Indices 3..22: Ch 2 Pages 0..19 (20 pages)
        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.takeLast(2).map { ReaderUiItem.Page(it) })
        newItems.add(ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)))
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        // LazyColumn's firstVisibleItemIndex would stay at 21 without re-anchoring,
        // which in newItems corresponds to Chapter 2 Page 19 (the end of the chapter)!
        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = oldCh2Page1,
                lastFirstVisibleOffset = lastFirstVisibleOffset,
                lastActiveItem = oldCh2Page1,
                activeChapterId = 2L,
                currentFirstVisibleIndex = 21,
            )

        assertNotNull(target)
        // Must resolve to Chapter 2 Page 1 at index 3 in newItems, NOT index 21!
        assertEquals(3, target!!.index)
        assertEquals(lastFirstVisibleOffset, target.offset)
        assertEquals(2L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(0, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget returns null when Compose native key tracking already maintained equivalent item`() {
        val ch1 = createChapter(1L, pageCount = 10)
        val items = ch1.pages!!.map { ReaderUiItem.Page(it) }

        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = items,
                lastFirstVisibleItem = items[4],
                lastFirstVisibleOffset = 100,
                lastActiveItem = items[4],
                activeChapterId = 1L,
                currentFirstVisibleIndex = 4,
            )

        assertNull(target)
    }

    @Test
    fun `resolveReanchorTarget calculates exact split slice and sub-pixel offset when tall page is split`() {
        val ch1 = createChapter(1L, pageCount = 3)
        val originalPage = ch1.pages!![1]
        val monolithicItem = ReaderUiItem.Page(originalPage)

        // Monolithic page was at scroll offset 1250 inside it
        val lastOffset = 1250

        // Page 1 is dynamically split into two 1000px slices
        val split0 = ReaderPageSplit(page = originalPage, topOffset = 0, splitHeight = 1000)
        val split1 = ReaderPageSplit(page = originalPage, topOffset = 1000, splitHeight = 1000)

        val newItems =
            listOf(
                ReaderUiItem.Page(ch1.pages!![0]), // index 0
                ReaderUiItem.SplitPage(split0), // index 1 (offset 0..1000)
                ReaderUiItem.SplitPage(split1), // index 2 (offset 1000..2000)
                ReaderUiItem.Page(ch1.pages!![2]), // index 3
            )

        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = monolithicItem,
                lastFirstVisibleOffset = lastOffset,
                lastActiveItem = monolithicItem,
                activeChapterId = 1L,
                currentFirstVisibleIndex = 1,
            )

        assertNotNull(target)
        // Offset 1250 falls inside split1 (1000..2000) at sliceOffset 250 (1250 - 1000)
        assertEquals(2, target!!.index)
        assertEquals(250, target.offset)
        assertEquals(split1, (target.item as ReaderUiItem.SplitPage).split)
    }

    @Test
    fun `resolveReanchorTarget restores monolithic page offset when split slice is converted back to monolithic`() {
        val ch1 = createChapter(1L, pageCount = 3)
        val page1 = ch1.pages!![1]
        val split1 = ReaderPageSplit(page = page1, topOffset = 1000, splitHeight = 1000)
        val splitItem = ReaderUiItem.SplitPage(split1)

        // User was at offset 250 in split1
        val lastOffset = 250

        val newItems =
            listOf(
                ReaderUiItem.Page(ch1.pages!![0]), // index 0
                ReaderUiItem.Page(page1), // index 1
                ReaderUiItem.Page(ch1.pages!![2]), // index 2
            )

        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = splitItem,
                lastFirstVisibleOffset = lastOffset,
                lastActiveItem = splitItem,
                activeChapterId = 1L,
                currentFirstVisibleIndex = 2,
            )

        assertNotNull(target)
        // Monolithic page target offset is split.topOffset (1000) + lastOffset (250) = 1250
        assertEquals(1, target!!.index)
        assertEquals(1250, target.offset)
    }

    @Test
    fun `resolveReanchorTarget anchors to first page of new chapter if Transition Next is replaced by loaded pages`() {
        val ch1 = createChapter(1L, pageCount = 5)
        val ch2 = createChapter(2L, pageCount = 5)
        val transitionNext = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))

        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.map { ReaderUiItem.Page(it) }) // 0..4
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) }) // 5..9

        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = transitionNext,
                lastFirstVisibleOffset = 50,
                lastActiveItem = transitionNext,
                activeChapterId = 1L,
                currentFirstVisibleIndex = 5,
            )

        assertNotNull(target)
        assertEquals(5, target!!.index)
        assertEquals(0, target.offset)
        assertEquals(2L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(0, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget preserves scroll on chapter transition seam when transitioning from Transition Next to Prev`() {
        val ch1 = createChapter(1L, pageCount = 171)
        val ch2 = createChapter(2L, pageCount = 159)

        // User was on ChapterTransition.Next at the end of Chapter 1
        val transitionNext = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))
        val transitionPrev = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))
        val lastOffset = 42

        // When Chapter 2 becomes active, items are:
        // Index 0: Ch1 Page 169
        // Index 1: Ch1 Page 170
        // Index 2: PrevTransition (ch2 -> ch1)
        // Indices 3..161: Ch2 Pages 0..158
        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.takeLast(2).map { ReaderUiItem.Page(it) })
        newItems.add(transitionPrev)
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        // Without re-anchoring, Compose LazyColumn clamps firstVisibleItemIndex to 158
        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = transitionNext,
                lastFirstVisibleOffset = lastOffset,
                lastActiveItem = transitionNext,
                activeChapterId = 2L,
                currentFirstVisibleIndex = 158,
            )

        assertNotNull(target)
        // Transition Next and Transition Prev are equivalent across chapter seams, so target must
        // be index 2!
        assertEquals(2, target!!.index)
        assertEquals(lastOffset, target.offset)
        assertEquals(transitionPrev, target.item)
    }

    @Test
    fun `resolveReanchorTarget preserves scroll on last page of chapter 1 when transitioning to chapter 2`() {
        val ch1 = createChapter(1L, pageCount = 171)
        val ch2 = createChapter(2L, pageCount = 159)

        // User was on Page 170 of Chapter 1
        val lastPageCh1 = ReaderUiItem.Page(ch1.pages!![170])
        val transitionPrev = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))
        val lastOffset = 350

        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.takeLast(2).map { ReaderUiItem.Page(it) }) // indices 0..1
        newItems.add(transitionPrev) // index 2
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) }) // indices 3..161

        // LazyColumn clamped to 158
        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = lastPageCh1,
                lastFirstVisibleOffset = lastOffset,
                lastActiveItem = lastPageCh1,
                activeChapterId = 2L,
                currentFirstVisibleIndex = 158,
            )

        assertNotNull(target)
        // Ch1 Page 170 is at index 1 in newItems, NOT index 158
        assertEquals(1, target!!.index)
        assertEquals(lastOffset, target.offset)
        assertEquals(1L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(170, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget falls back to start of new chapter if user was on pruned page of previous chapter`() {
        val ch1 = createChapter(1L, pageCount = 171)
        val ch2 = createChapter(2L, pageCount = 159)

        // User was on Page 50 of Chapter 1 (pruned when Chapter 2 is active)
        val prunedPageCh1 = ReaderUiItem.Page(ch1.pages!![50])
        val transitionPrev = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))

        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.takeLast(2).map { ReaderUiItem.Page(it) }) // indices 0..1
        newItems.add(transitionPrev) // index 2
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) }) // indices 3..161

        // LazyColumn clamped to 158
        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = prunedPageCh1,
                lastFirstVisibleOffset = 0,
                lastActiveItem = prunedPageCh1,
                activeChapterId = 2L,
                currentFirstVisibleIndex = 158,
            )

        assertNotNull(target)
        // Must fallback to the first page of active chapter (index 3), NEVER index 158!
        assertEquals(3, target!!.index)
        assertEquals(0, target.offset)
        assertEquals(2L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(0, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget returns null when next chapter loads after transition preserving position without jitter`() {
        val ch1 = createChapter(1L, pageCount = 171)
        val ch2 = createChapter(2L, pageCount = 159)

        val transitionNext = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))

        // Previous items: Chapter 1 (171 pages) + Transition.Next (index 171)
        val previousItems = mutableListOf<ReaderUiItem>()
        previousItems.addAll(ch1.pages!!.map { ReaderUiItem.Page(it) })
        previousItems.add(transitionNext)

        // User is at transition at index 171, live scroll offset 250
        val currentFirstVisibleIndex = 171
        val liveOffset = 250

        // When Chapter 2 finishes loading, its pages are appended after index 171:
        // Indices 0..170: Ch 1 Pages
        // Index 171: Transition.Next
        // Indices 172..330: Ch 2 Pages
        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(previousItems)
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        // Even if lastFirstVisibleItem was lagging (e.g. still Page 170 from prior frames)
        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = previousItems[170],
                lastFirstVisibleOffset = liveOffset,
                lastActiveItem = transitionNext,
                activeChapterId = 1L,
                currentFirstVisibleIndex = currentFirstVisibleIndex,
                previousItems = previousItems,
            )

        // Target MUST be null so no scrollToItem is called, eliminating jitter on next chapter
        // loading
        assertNull(target)
    }

    @Test
    fun `resolveReanchorTarget preserves scroll and prevents page jump when previous chapter loads on backward navigation`() {
        val ch1 = createChapter(1L, pageCount = 171)
        val ch2 = createChapter(2L, pageCount = 159)

        val transitionPrev = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))
        val transitionNext = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))

        // Previous items: Ch 1 Pages 169..170 (indices 0..1) + Transition.Prev (index 2) + Ch 2
        // Pages 0..158 (indices 3..161)
        val previousItems = mutableListOf<ReaderUiItem>()
        previousItems.addAll(ch1.pages!!.takeLast(2).map { ReaderUiItem.Page(it) })
        previousItems.add(transitionPrev)
        previousItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        // User scrolled backwards into Chapter 1 Page 170 (index 1 in previousItems)
        val currentFirstVisibleIndex = 1
        val liveOffset = 320
        val targetPageCh1 = ReaderUiItem.Page(ch1.pages!![170])

        // When Chapter 1 becomes active, it expands to all 171 pages:
        // Indices 0..170: Ch 1 Pages 0..170
        // Index 171: Transition.Next
        // Indices 172..330: Ch 2 Pages 0..158
        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.map { ReaderUiItem.Page(it) })
        newItems.add(transitionNext)
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = targetPageCh1,
                lastFirstVisibleOffset = liveOffset,
                lastActiveItem = targetPageCh1,
                activeChapterId = 1L,
                currentFirstVisibleIndex = currentFirstVisibleIndex,
                previousItems = previousItems,
            )

        assertNotNull(target)
        // Must resolve to Chapter 1 Page 170 at index 170 (NOT stay at index 1 which is Page 1!)
        assertEquals(170, target!!.index)
        // Must preserve the exact live scroll offset
        assertEquals(liveOffset, target.offset)
        assertEquals(1L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(170, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget preserves scroll on transition seam when backward chapter loads`() {
        val ch1 = createChapter(1L, pageCount = 171)
        val ch2 = createChapter(2L, pageCount = 159)

        val transitionPrev = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))
        val transitionNext = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))

        // User was at Transition.Prev at index 2 in previousItems
        val previousItems = mutableListOf<ReaderUiItem>()
        previousItems.addAll(ch1.pages!!.takeLast(2).map { ReaderUiItem.Page(it) })
        previousItems.add(transitionPrev)
        previousItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        val currentFirstVisibleIndex = 2
        val liveOffset = 145

        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.map { ReaderUiItem.Page(it) })
        newItems.add(transitionNext)
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = transitionPrev,
                lastFirstVisibleOffset = liveOffset,
                lastActiveItem = transitionPrev,
                activeChapterId = 1L,
                currentFirstVisibleIndex = currentFirstVisibleIndex,
                previousItems = previousItems,
            )

        assertNotNull(target)
        // Transition is now at index 171 in newItems
        assertEquals(171, target!!.index)
        assertEquals(liveOffset, target.offset)
        assertEquals(transitionNext, target.item)
    }

    @Test
    fun `resolveReanchorTarget returns null when target index matches current index and item is equivalent`() {
        val ch1 = createChapter(1L, pageCount = 20)
        val items = ch1.pages!!.map { ReaderUiItem.Page(it) }

        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = items,
                lastFirstVisibleItem = items[7],
                lastFirstVisibleOffset = 180,
                lastActiveItem = items[7],
                activeChapterId = 1L,
                currentFirstVisibleIndex = 7,
                previousItems = items,
            )

        assertNull(target)
    }

    @Test
    fun `resolveReanchorTarget correctly anchors to Chapter 2 Page 0 when list is pruned and index is clamped to 158`() {
        val ch1 = createChapter(1L, pageCount = 171)
        val ch2 = createChapter(2L, pageCount = 159)

        val transitionNext = ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2))
        val transitionPrev = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))

        // Before transition: Ch 1 (171 pages) + Transition.Next (171) + Ch 2 (172..330)
        val previousItems = mutableListOf<ReaderUiItem>()
        previousItems.addAll(ch1.pages!!.map { ReaderUiItem.Page(it) })
        previousItems.add(transitionNext)
        previousItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        // User was at Chapter 2 Page 0 at index 172 in previousItems
        val ch2Page0 = ReaderUiItem.Page(ch2.pages!![0])
        val liveOffset = 75

        // When Chapter 2 becomes active, items shrink to 162 items:
        // Indices 0..1: Ch 1 Pages 169..170
        // Index 2: Transition.Prev
        // Indices 3..161: Ch 2 Pages 0..158
        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.takeLast(2).map { ReaderUiItem.Page(it) })
        newItems.add(transitionPrev)
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        // LazyListState's firstVisibleItemIndex was clamped to 158 (end of pruned list)
        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = ch2Page0,
                lastFirstVisibleOffset = liveOffset,
                lastActiveItem = ch2Page0,
                activeChapterId = 2L,
                currentFirstVisibleIndex = 158,
                previousItems = previousItems,
            )

        assertNotNull(target)
        // Must resolve to Chapter 2 Page 0 at index 3 in newItems, NEVER staying clamped at index
        // 158!
        assertEquals(3, target!!.index)
        assertEquals(liveOffset, target.offset)
        assertEquals(2L, (target.item as ReaderUiItem.Page).page.chapter.chapter.id)
        assertEquals(0, (target.item as ReaderUiItem.Page).page.index)
    }

    @Test
    fun `resolveReanchorTarget handles out-of-bounds currentFirstVisibleIndex without crashing and anchors cleanly`() {
        val ch1 = createChapter(1L, pageCount = 171)
        val ch2 = createChapter(2L, pageCount = 159)

        val transitionPrev = ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1))

        val newItems = mutableListOf<ReaderUiItem>()
        newItems.addAll(ch1.pages!!.takeLast(2).map { ReaderUiItem.Page(it) })
        newItems.add(transitionPrev)
        newItems.addAll(ch2.pages!!.map { ReaderUiItem.Page(it) })

        val ch2Page0 = ReaderUiItem.Page(ch2.pages!![0])

        // currentFirstVisibleIndex is 172 (before Compose clamped it), which is >
        // newItems.lastIndex (161)
        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = newItems,
                lastFirstVisibleItem = ch2Page0,
                lastFirstVisibleOffset = 110,
                lastActiveItem = ch2Page0,
                activeChapterId = 2L,
                currentFirstVisibleIndex = 172,
            )

        assertNotNull(target)
        assertEquals(3, target!!.index)
        assertEquals(110, target.offset)
        assertEquals(ch2Page0, target.item)
    }
}
