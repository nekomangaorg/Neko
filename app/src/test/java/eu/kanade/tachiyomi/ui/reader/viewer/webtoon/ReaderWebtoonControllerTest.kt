package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderWebtoonControllerTest {

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
    fun `buildItems with loaded chapters adds prevTransition, currentPages, and nextPages`() {
        val controller = ReaderWebtoonController()
        val prevChapter = createChapter(1L, pageCount = 5)
        val currChapter = createChapter(2L, pageCount = 10)
        val nextChapter = createChapter(3L, pageCount = 5)

        val viewerChapters = ViewerChapters(currChapter, prevChapter, nextChapter)
        val items =
            controller.buildItems(viewerChapters, forceTransition = false, screenHeight = 1000)

        // Webtoon mode is forward-continuous: previous chapter pages are never prepended.
        // Index 0: previous chapter transition
        // Current chapter: all 10 pages (pages 0..9 at indices 1..10)
        // Next chapter: all 5 pages (pages 0..4 at indices 11..15)
        // Total = 1 + 10 + 5 = 16 items
        assertEquals(16, items.size)
        assertTrue(items[0] is ReaderUiItem.Transition)
        assertTrue((items[0] as ReaderUiItem.Transition).transition is ChapterTransition.Prev)
        assertNull(items[0].chapterId)
        assertNull(items[0].pageIndex)

        assertEquals(2L, items[1].chapterId)
        assertEquals(0, items[1].pageIndex)

        assertEquals(3L, items[11].chapterId)
        assertEquals(0, items[11].pageIndex)
    }

    @Test
    fun `buildItems inserts transition when forced or unloaded`() {
        val controller = ReaderWebtoonController()
        val prevChapter = createChapter(1L, pageCount = 5)
        val currChapter = createChapter(2L, pageCount = 10)
        val nextChapter =
            createChapter(3L, pageCount = 5).apply { state = ReaderChapter.State.Wait }

        val viewerChapters = ViewerChapters(currChapter, prevChapter, nextChapter)
        val items = controller.buildItems(viewerChapters, forceTransition = true)

        // With forceTransition = true:
        // prev transition (1) + curr pages (10) + next transition (1) = 12 items
        // (next is Wait so 0 pages)
        assertEquals(12, items.size)
        assertTrue(items[0] is ReaderUiItem.Transition)
        assertTrue((items[0] as ReaderUiItem.Transition).transition is ChapterTransition.Prev)
        assertTrue(items[11] is ReaderUiItem.Transition)
        assertTrue((items[11] as ReaderUiItem.Transition).transition is ChapterTransition.Next)
    }

    @Test
    fun `splitPage replaces monolithic page when slices start at offset 0`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 3)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        assertEquals(5, items.size)
        val targetPage = currChapter.pages!![1]
        val split1 = ReaderPageSplit(targetPage, 0, 1000)
        val split2 = ReaderPageSplit(targetPage, 1000, 1000)

        val updatedItems = controller.splitPage(items, targetPage, listOf(split1, split2))
        assertEquals(6, updatedItems.size)
        assertTrue(updatedItems[0] is ReaderUiItem.Transition)
        assertTrue(updatedItems[1] is ReaderUiItem.Page)
        assertTrue(updatedItems[2] is ReaderUiItem.SplitPage)
        assertEquals(0, (updatedItems[2] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(updatedItems[3] is ReaderUiItem.SplitPage)
        assertEquals(1000, (updatedItems[3] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(updatedItems[4] is ReaderUiItem.Page)
        assertTrue(updatedItems[5] is ReaderUiItem.Transition)
        assertTrue(controller.tallSplitPages.contains(targetPage))
    }

    @Test
    fun `splitPage inserts slices after original page when topOffset is greater than 0`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 3)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        assertEquals(5, items.size)
        val targetPage = currChapter.pages!![1]
        val split1 = ReaderPageSplit(targetPage, 1000, 1000)

        val updatedItems = controller.splitPage(items, targetPage, listOf(split1))
        assertEquals(6, updatedItems.size)
        assertTrue(updatedItems[0] is ReaderUiItem.Transition)
        assertTrue(updatedItems[1] is ReaderUiItem.Page)
        assertTrue(updatedItems[2] is ReaderUiItem.Page)
        assertTrue(updatedItems[3] is ReaderUiItem.SplitPage)
        assertTrue(updatedItems[4] is ReaderUiItem.Page)
        assertTrue(updatedItems[5] is ReaderUiItem.Transition)
    }

    @Test
    fun `computeSplits returns null for normal proportion images`() {
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        val splits = ReaderWebtoonController.computeSplits(page, 1000, 1500, 1000, 4096)
        assertNull(splits)
    }

    @Test
    fun `computeSplits calculates correct slices for tall images`() {
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        val splits = ReaderWebtoonController.computeSplits(page, 1000, 12000, 1000, 4096)
        assertNotNull(splits)
        assertEquals(3, splits!!.size)
        assertEquals(0, splits[0].topOffset)
        assertEquals(4000, splits[0].splitHeight)
        assertEquals(4000, splits[1].topOffset)
        assertEquals(4000, splits[1].splitHeight)
        assertEquals(8000, splits[2].topOffset)
        assertEquals(4000, splits[2].splitHeight)
    }

    @Test
    fun `computeSplits handles images that exceed maxTextureSize`() {
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        val splits = ReaderWebtoonController.computeSplits(page, 2000, 5000, 3000, 4096)
        assertNotNull(splits)
        assertEquals(2, splits!!.size)
        assertEquals(0, splits[0].topOffset)
        assertEquals(2500, splits[0].splitHeight)
        assertEquals(2500, splits[1].topOffset)
        assertEquals(2500, splits[1].splitHeight)
    }

    @Test
    fun `computeSplits calculates optimal slices on modern high-res screens`() {
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        val splits = ReaderWebtoonController.computeSplits(page, 1080, 7000, 2400, 8192)
        assertNotNull(splits)
        assertEquals(2, splits!!.size)
        assertEquals(0, splits[0].topOffset)
        assertEquals(3500, splits[0].splitHeight)
        assertEquals(3500, splits[1].topOffset)
        assertEquals(3500, splits[1].splitHeight)
    }

    @Test
    fun `findPageIndex locates page in list`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 5)
        val viewerChapters = ViewerChapters(currChapter, null, null)
        val items = controller.buildItems(viewerChapters, forceTransition = false)

        val targetPage = currChapter.pages!![3]
        val index = controller.findPageIndex(items, targetPage)
        assertEquals(4, index)
    }

    @Test
    fun `checkAndTrackTallPage returns AlreadySplit when page was previously tracked`() {
        val controller = ReaderWebtoonController()
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]
        controller.tallSplitPages.add(page)

        val result = controller.checkAndTrackTallPage(page, screenHeight = 1000)
        assertEquals(ReaderWebtoonController.TallSplitResult.AlreadySplit, result)
    }

    @Test
    fun `checkAndTrackTallPage returns NotTall when page stream is null`() {
        val controller = ReaderWebtoonController()
        val chapter = createChapter(1L, pageCount = 1)
        val page = chapter.pages!![0]

        val result = controller.checkAndTrackTallPage(page, screenHeight = 1000)
        assertEquals(ReaderWebtoonController.TallSplitResult.NotTall, result)
        assertTrue(!controller.tallSplitPages.contains(page))
        assertTrue(controller.isNonTall(page))
    }

    @Test
    fun `Transition key is symmetrical across adjacent chapters when destination is resolved`() {
        val chapter1 = createChapter(45L, pageCount = 5)
        val chapter2 = createChapter(46L, pageCount = 5)

        val nextTrans = ChapterTransition.Next(chapter1, chapter2)
        val prevTrans = ChapterTransition.Prev(chapter2, chapter1)

        val nextItem = ReaderUiItem.Transition(nextTrans)
        val prevItem = ReaderUiItem.Transition(prevTrans)

        assertEquals("webtoon_transition_45_46", nextItem.key("webtoon"))
        assertEquals("webtoon_transition_45_46", prevItem.key("webtoon"))
        assertEquals(nextItem.key("webtoon"), prevItem.key("webtoon"))
        assertNull(nextItem.chapterId)
        assertNull(prevItem.chapterId)
    }

    @Test
    fun `Transition key falls back to directional type when destination is null`() {
        val currChapter = createChapter(46L, pageCount = 5)
        val transWithoutTo = ChapterTransition.Prev(currChapter, null)
        val itemWithoutTo = ReaderUiItem.Transition(transWithoutTo)

        assertEquals("webtoon_transition_prev_46", itemWithoutTo.key("webtoon"))
    }

    @Test
    fun `buildItems preserves next transition card when next chapter loads after being displayed`() {
        val controller = ReaderWebtoonController()
        val currChapter = createChapter(1L, pageCount = 5)
        val dbChapter2 =
            Chapter.create().apply {
                this.id = 2L
                this.url = "/chapter/2"
                this.name = "Chapter 2"
                this.chapter_number = 2f
            }
        val nextChapter = ReaderChapter(dbChapter2)
        nextChapter.state = ReaderChapter.State.Wait

        val viewerChaptersInitial = ViewerChapters(currChapter, null, nextChapter)

        // Initial build: next chapter is not loaded, so next transition card must be present
        val initialItems =
            controller.buildItems(
                viewerChaptersInitial,
                forceTransition = false,
                screenHeight = 1000,
            )

        // 1 prev transition + 5 curr pages + 1 next transition = 7 items
        assertEquals(7, initialItems.size)
        assertTrue(initialItems[6] is ReaderUiItem.Transition)
        assertTrue(
            (initialItems[6] as ReaderUiItem.Transition).transition is ChapterTransition.Next
        )

        // Next chapter finishes loading
        val pages2 =
            (0 until 5).map { index ->
                ReaderPage(index = index, url = "url2_$index", imageUrl = "img2_$index").apply {
                    this.chapter = nextChapter
                    this.renderedHeight = 1000
                }
            }
        nextChapter.state = ReaderChapter.State.Loaded(pages2)

        val viewerChaptersLoaded = ViewerChapters(currChapter, null, nextChapter)

        // Re-build with next chapter loaded: transition card must still be preserved
        val loadedItems =
            controller.buildItems(
                viewerChaptersLoaded,
                forceTransition = false,
                screenHeight = 1000,
            )

        // 1 prev transition + 5 curr pages + 1 next transition (preserved!) + 5 next pages = 12
        // items
        assertEquals(12, loadedItems.size)
        assertTrue(loadedItems[6] is ReaderUiItem.Transition)
        assertTrue((loadedItems[6] as ReaderUiItem.Transition).transition is ChapterTransition.Next)

        // Next chapter pages follow the transition card
        assertEquals(2L, loadedItems[7].chapterId)
        assertEquals(0, loadedItems[7].pageIndex)
    }

    @Test
    fun `buildItems resets hadTransitionForNext when current chapter changes`() {
        val controller = ReaderWebtoonController()
        val chapter1 = createChapter(1L, pageCount = 5)
        val dbChapter2 =
            Chapter.create().apply {
                this.id = 2L
                this.url = "/chapter/2"
                this.name = "Chapter 2"
                this.chapter_number = 2f
            }
        val chapter2 = ReaderChapter(dbChapter2)
        chapter2.state = ReaderChapter.State.Wait

        // Chapter 1 displayed with unloaded Chapter 2 -> sets hadTransitionForNext = true
        controller.buildItems(ViewerChapters(chapter1, null, chapter2), forceTransition = false)

        // Now user moves to Chapter 2 (currentChapter changes to 2) with loaded Chapter 3
        val chapter3 = createChapter(3L, pageCount = 5)
        val pages2 =
            (0 until 5).map { index ->
                ReaderPage(index = index, url = "url2_$index", imageUrl = "img2_$index").apply {
                    this.chapter = chapter2
                    this.renderedHeight = 1000
                }
            }
        chapter2.state = ReaderChapter.State.Loaded(pages2)

        val chapter2Items =
            controller.buildItems(
                ViewerChapters(chapter2, chapter1, chapter3),
                forceTransition = false,
            )

        // In Chapter 2: Chapter 3 is loaded and forceTransition is false.
        // hadTransitionForNext should have been reset, so no next transition is added.
        // 1 prev transition + 5 curr pages (ch 2) + 5 next pages (ch 3) = 11 items
        assertEquals(11, chapter2Items.size)
        assertTrue(chapter2Items[0] is ReaderUiItem.Transition)
        assertTrue(
            (chapter2Items[0] as ReaderUiItem.Transition).transition is ChapterTransition.Prev
        )
        assertEquals(2L, chapter2Items[1].chapterId)
        assertEquals(3L, chapter2Items[6].chapterId)
    }

    @Test
    fun `buildItems reuses existing SplitPage slices to maintain key and layout continuity`() {
        val controller = ReaderWebtoonController()
        val chapter1 = createChapter(1L, pageCount = 3)
        val page0 = chapter1.pages!![0]
        val split0 = ReaderPageSplit(page0, topOffset = 0, splitHeight = 1000)
        val split1 = ReaderPageSplit(page0, topOffset = 1000, splitHeight = 1000)

        val existingItems =
            listOf(
                ReaderUiItem.Transition(ChapterTransition.Prev(chapter1, null)),
                ReaderUiItem.SplitPage(split0),
                ReaderUiItem.SplitPage(split1),
                ReaderUiItem.Page(chapter1.pages!![1]),
                ReaderUiItem.Page(chapter1.pages!![2]),
            )

        val newItems =
            controller.buildItems(
                ViewerChapters(chapter1, null, null),
                forceTransition = false,
                existingItems = existingItems,
            )

        // 1 prev transition + 2 slices for page 0 + 2 pages + 1 next transition = 6 items
        assertEquals(6, newItems.size)
        assertTrue(newItems[1] is ReaderUiItem.SplitPage)
        assertTrue(newItems[2] is ReaderUiItem.SplitPage)
        assertEquals(0, (newItems[1] as ReaderUiItem.SplitPage).split.topOffset)
        assertEquals(1000, (newItems[2] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(controller.tallSplitPages.contains(page0))
    }

    @Test
    fun `findPageIndex matches pages across distinct ReaderPage instances for same chapter and index`() {
        val controller = ReaderWebtoonController()
        val chapter1 = createChapter(1L, pageCount = 2)
        val items =
            listOf(
                ReaderUiItem.Transition(ChapterTransition.Prev(chapter1, null)),
                ReaderUiItem.Page(chapter1.pages!![0]),
                ReaderUiItem.Page(chapter1.pages!![1]),
            )

        // Distinct ReaderPage instance with the same chapter ID and index
        val duplicateChapter = createChapter(1L, pageCount = 2)
        val queryPage = duplicateChapter.pages!![1]

        val foundIndex = controller.findPageIndex(items, queryPage)
        assertEquals(2, foundIndex)
    }
}
