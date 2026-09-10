package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WebtoonActiveItemResolverTest {

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
    fun `resolveActiveIndex returns firstCurrentItemIndex when anchored at start (prevents auto-completion bug)`() {
        val currChapter = createChapter(2L, pageCount = 3)
        val currentItems: List<ReaderUiItem> = currChapter.pages!!.map { ReaderUiItem.Page(it) }

        // 3 short pages of 50px each on a 2400px screen
        val visibleBounds =
            listOf(
                VisibleItemBounds(index = 0, offset = 0, size = 50),
                VisibleItemBounds(index = 1, offset = 50, size = 50),
                VisibleItemBounds(index = 2, offset = 100, size = 50),
            )

        val activeIndex =
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = visibleBounds,
                currentItems = currentItems,
                activeChapterId = 2L,
                viewportStartOffset = 0,
                viewportEndOffset = 2400,
                firstVisibleIndex = 0,
                firstVisibleScrollOffset = 0,
            )

        // Must resolve to Page 1 (index 0), NOT Page 3 (index 2) which is closer to viewportMiddle
        // (1200)
        assertEquals(0, activeIndex)
    }

    @Test
    fun `resolveActiveIndex resolves to current chapter page 1 when LazyColumn clamps backwards (Issue #3347)`() {
        val prevChapter = createChapter(1L, pageCount = 5)
        val currChapter = createChapter(2L, pageCount = 2)

        val currentItems = mutableListOf<ReaderUiItem>()
        currentItems.addAll(prevChapter.pages!!.map { ReaderUiItem.Page(it) }) // indices 0..4
        currentItems.addAll(currChapter.pages!!.map { ReaderUiItem.Page(it) }) // indices 5..6

        // Clamping pulls prevChapter onto the top 2200px of screen; currChapter is at 2200..2400
        val visibleBounds =
            listOf(
                VisibleItemBounds(index = 1, offset = 0, size = 600),
                VisibleItemBounds(
                    index = 2,
                    offset = 600,
                    size = 600,
                ), // Spans viewportMiddle (1200)
                VisibleItemBounds(index = 3, offset = 1200, size = 600),
                VisibleItemBounds(index = 4, offset = 1800, size = 400),
                VisibleItemBounds(index = 5, offset = 2200, size = 100), // Curr chapter Page 1
                VisibleItemBounds(index = 6, offset = 2300, size = 100), // Curr chapter Page 2
            )

        val activeIndex =
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = visibleBounds,
                currentItems = currentItems,
                activeChapterId = 2L,
                viewportStartOffset = 0,
                viewportEndOffset = 2400,
                firstVisibleIndex = 1,
                firstVisibleScrollOffset = 0,
            )

        // Must resolve to index 5 (currChapter Page 1), preventing jump back to prevChapter
        assertEquals(5, activeIndex)
    }

    @Test
    fun `resolveActiveIndex returns item spanning viewport middle during normal forward reading`() {
        val currChapter = createChapter(2L, pageCount = 10)
        val currentItems: List<ReaderUiItem> = currChapter.pages!!.map { ReaderUiItem.Page(it) }

        val visibleBounds =
            listOf(
                VisibleItemBounds(index = 2, offset = -400, size = 1000), // ends at 600
                VisibleItemBounds(
                    index = 3,
                    offset = 600,
                    size = 1000,
                ), // spans viewportMiddle (1200)
                VisibleItemBounds(index = 4, offset = 1600, size = 1000),
            )

        val activeIndex =
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = visibleBounds,
                currentItems = currentItems,
                activeChapterId = 2L,
                viewportStartOffset = 0,
                viewportEndOffset = 2400,
                firstVisibleIndex = 2,
                firstVisibleScrollOffset = 400,
            )

        assertEquals(3, activeIndex)
    }

    @Test
    fun `resolveActiveIndex resolves forward to Transition Next when scrolling past chapter end`() {
        val currChapter = createChapter(2L, pageCount = 5)
        val nextChapter = createChapter(3L, pageCount = 5)

        val currentItems = mutableListOf<ReaderUiItem>()
        currentItems.addAll(currChapter.pages!!.map { ReaderUiItem.Page(it) }) // indices 0..4
        val nextTrans = ChapterTransition.Next(currChapter, nextChapter)
        currentItems.add(ReaderUiItem.Transition(nextTrans)) // index 5
        currentItems.addAll(nextChapter.pages!!.map { ReaderUiItem.Page(it) }) // indices 6..10

        val visibleBounds =
            listOf(
                VisibleItemBounds(
                    index = 4,
                    offset = -500,
                    size = 1000,
                ), // Curr chapter Page 5 (ends at 500)
                VisibleItemBounds(
                    index = 5,
                    offset = 500,
                    size = 1200,
                ), // Transition.Next (spans 1200)
                VisibleItemBounds(index = 6, offset = 1700, size = 1000), // Next chapter Page 1
            )

        val activeIndex =
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = visibleBounds,
                currentItems = currentItems,
                activeChapterId = 2L,
                viewportStartOffset = 0,
                viewportEndOffset = 2400,
                firstVisibleIndex = 4,
                firstVisibleScrollOffset = 500,
            )

        // Must resolve to Transition.Next (index 5)
        assertEquals(5, activeIndex)
    }

    @Test
    fun `resolveActiveIndex allows backward chapter navigation when current chapter leaves viewport`() {
        val prevChapter = createChapter(1L, pageCount = 5)
        val currChapter = createChapter(2L, pageCount = 5)

        val currentItems = mutableListOf<ReaderUiItem>()
        currentItems.addAll(prevChapter.pages!!.map { ReaderUiItem.Page(it) }) // indices 0..4
        currentItems.addAll(currChapter.pages!!.map { ReaderUiItem.Page(it) }) // indices 5..9

        // User scrolled up so far that no currChapter pages are visible on screen
        val visibleBounds =
            listOf(
                VisibleItemBounds(index = 1, offset = -200, size = 1000),
                VisibleItemBounds(index = 2, offset = 800, size = 1000), // Spans 1200
                VisibleItemBounds(index = 3, offset = 1800, size = 800),
            )

        val activeIndex =
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = visibleBounds,
                currentItems = currentItems,
                activeChapterId = 2L,
                viewportStartOffset = 0,
                viewportEndOffset = 2400,
                firstVisibleIndex = 1,
                firstVisibleScrollOffset = 200,
            )

        // Chapter 2 is completely off-screen, so user intentionally navigated backwards to Chapter
        // 1
        assertEquals(2, activeIndex)
    }

    @Test
    fun `resolveActiveIndex returns fallback firstVisibleIndex when visibleItems is empty`() {
        val currChapter = createChapter(1L, pageCount = 5)
        val currentItems = currChapter.pages!!.map { ReaderUiItem.Page(it) }

        val activeIndex =
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = emptyList<VisibleItemBounds>(),
                currentItems = currentItems,
                activeChapterId = 1L,
                viewportStartOffset = 0,
                viewportEndOffset = 2400,
                firstVisibleIndex = 3,
                firstVisibleScrollOffset = 0,
            )

        assertEquals(3, activeIndex)
    }

    @Test
    fun `resolveActiveIndex picks closest item when viewportMiddle falls into spacing gap`() {
        val currChapter = createChapter(1L, pageCount = 3)
        val currentItems = currChapter.pages!!.map { ReaderUiItem.Page(it) }

        // Viewport 0..2400 -> middle is 1200.
        // Item 0: 0..1190
        // Gap: 1190..1210 (Middle 1200 falls in gap)
        // Item 1: 1210..2400
        val visibleBounds =
            listOf(
                VisibleItemBounds(index = 0, offset = 0, size = 1190), // middle = 595, dist = 605
                VisibleItemBounds(
                    index = 1,
                    offset = 1210,
                    size = 1190,
                ), // middle = 1805, dist = 605
            )

        val activeIndex =
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = visibleBounds,
                currentItems = currentItems,
                activeChapterId = 1L,
                viewportStartOffset = 0,
                viewportEndOffset = 2400,
                firstVisibleIndex = 0,
                firstVisibleScrollOffset = 50,
            )

        // Neither spans 1200, both are current chapter, picks closest
        assertEquals(0, activeIndex)
    }

    @Test
    fun `resolveActiveIndex supports ReaderUiItem SplitPage correctly`() {
        val currChapter = createChapter(1L, pageCount = 1)
        val page = currChapter.pages!![0]
        val split1 = ReaderPageSplit(page = page, topOffset = 0, splitHeight = 1500)
        val split2 = ReaderPageSplit(page = page, topOffset = 1500, splitHeight = 1500)

        val currentItems =
            listOf(
                ReaderUiItem.SplitPage(split1), // index 0
                ReaderUiItem.SplitPage(split2), // index 1
            )

        val visibleBounds =
            listOf(
                VisibleItemBounds(index = 0, offset = -500, size = 1500), // spans 0..1000
                VisibleItemBounds(
                    index = 1,
                    offset = 1000,
                    size = 1500,
                ), // spans 1000..2500 (spans 1200)
            )

        val activeIndex =
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = visibleBounds,
                currentItems = currentItems,
                activeChapterId = 1L,
                viewportStartOffset = 0,
                viewportEndOffset = 2400,
                firstVisibleIndex = 0,
                firstVisibleScrollOffset = 500,
            )

        assertEquals(1, activeIndex)
    }
}
