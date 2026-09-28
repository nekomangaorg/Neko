package org.nekomanga.presentation.screens.reader.viewer

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PagerOverscrollNavigationTest {

    private fun createChapter(id: Long): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
            }
        val readerChapter = ReaderChapter(dbChapter)
        val pages =
            (0 until 3).map { index ->
                ReaderPage(index = index, url = "url_$index", imageUrl = "img_$index").apply {
                    this.chapter = readerChapter
                }
            }
        readerChapter.state = ReaderChapter.State.Loaded(pages)
        return readerChapter
    }

    @Test
    fun `LTR prev transition overscroll backward triggers navigation to prev chapter with End target`() {
        val prevChapter = createChapter(1L)
        val currChapter = createChapter(2L)
        val transition =
            ReaderUiItem.Transition(ChapterTransition.Prev(from = currChapter, to = prevChapter))
        val items = listOf(transition)

        var navigatedChapter: Chapter? = null
        var navigatedTarget: ChapterNavTarget? = null

        val handler =
            PagerOverscrollHandler(
                isRtl = false,
                thresholdPx = 100f,
                onNavigateToChapter = { ch, target ->
                    navigatedChapter = ch
                    navigatedTarget = target
                },
            )

        // Delta 50f does not trigger (< 100f)
        handler.handleScrollDelta(
            delta = 50f,
            currentIndex = 0,
            items = items,
            isNavigating = false,
        )
        assertNull(navigatedChapter)

        // Delta 60f accumulates to 110f (> 100f) -> triggers navigation
        handler.handleScrollDelta(
            delta = 60f,
            currentIndex = 0,
            items = items,
            isNavigating = false,
        )
        assertEquals(1L, navigatedChapter?.id)
        assertEquals(ChapterNavTarget.End, navigatedTarget)
        assertEquals(0f, handler.accumulatedOverscroll)
    }

    @Test
    fun `LTR next transition overscroll forward triggers navigation to next chapter with Start target`() {
        val currChapter = createChapter(1L)
        val nextChapter = createChapter(2L)
        val transition =
            ReaderUiItem.Transition(ChapterTransition.Next(from = currChapter, to = nextChapter))
        val items = listOf(transition)

        var navigatedChapter: Chapter? = null
        var navigatedTarget: ChapterNavTarget? = null

        val handler =
            PagerOverscrollHandler(
                isRtl = false,
                thresholdPx = 100f,
                onNavigateToChapter = { ch, target ->
                    navigatedChapter = ch
                    navigatedTarget = target
                },
            )

        // Forward swipe in LTR produces negative delta (delta < -thresholdPx)
        handler.handleScrollDelta(
            delta = -120f,
            currentIndex = 0,
            items = items,
            isNavigating = false,
        )
        assertEquals(2L, navigatedChapter?.id)
        assertEquals(ChapterNavTarget.Start, navigatedTarget)
    }

    @Test
    fun `RTL prev transition overscroll backward triggers navigation to prev chapter with End target`() {
        val prevChapter = createChapter(1L)
        val currChapter = createChapter(2L)
        val transition =
            ReaderUiItem.Transition(ChapterTransition.Prev(from = currChapter, to = prevChapter))
        val items = listOf(transition)

        var navigatedChapter: Chapter? = null
        var navigatedTarget: ChapterNavTarget? = null

        val handler =
            PagerOverscrollHandler(
                isRtl = true,
                thresholdPx = 100f,
                onNavigateToChapter = { ch, target ->
                    navigatedChapter = ch
                    navigatedTarget = target
                },
            )

        // In RTL, prev transition overscroll backward is delta < -thresholdPx
        handler.handleScrollDelta(
            delta = -150f,
            currentIndex = 0,
            items = items,
            isNavigating = false,
        )
        assertEquals(1L, navigatedChapter?.id)
        assertEquals(ChapterNavTarget.End, navigatedTarget)
    }

    @Test
    fun `RTL next transition overscroll forward triggers navigation to next chapter with Start target`() {
        val currChapter = createChapter(1L)
        val nextChapter = createChapter(2L)
        val transition =
            ReaderUiItem.Transition(ChapterTransition.Next(from = currChapter, to = nextChapter))
        val items = listOf(transition)

        var navigatedChapter: Chapter? = null
        var navigatedTarget: ChapterNavTarget? = null

        val handler =
            PagerOverscrollHandler(
                isRtl = true,
                thresholdPx = 100f,
                onNavigateToChapter = { ch, target ->
                    navigatedChapter = ch
                    navigatedTarget = target
                },
            )

        // In RTL, next transition overscroll forward is delta > thresholdPx
        handler.handleScrollDelta(
            delta = 150f,
            currentIndex = 0,
            items = items,
            isNavigating = false,
        )
        assertEquals(2L, navigatedChapter?.id)
        assertEquals(ChapterNavTarget.Start, navigatedTarget)
    }

    @Test
    fun `LTR page overscroll at start of content triggers prev chapter when prev transition exists`() {
        val prevChapter = createChapter(1L)
        val currChapter = createChapter(2L)
        val prevTransition =
            ReaderUiItem.Transition(ChapterTransition.Prev(from = currChapter, to = prevChapter))
        val pages = currChapter.pages!!.map { ReaderUiItem.Page(it) }
        val items = listOf(prevTransition) + pages

        var navigatedChapter: Chapter? = null
        var navigatedTarget: ChapterNavTarget? = null

        val handler =
            PagerOverscrollHandler(
                isRtl = false,
                thresholdPx = 100f,
                onNavigateToChapter = { ch, target ->
                    navigatedChapter = ch
                    navigatedTarget = target
                },
            )

        // At index 0 with delta > 0 (swiping backwards)
        handler.handleScrollDelta(
            delta = 120f,
            currentIndex = 0,
            items = items,
            isNavigating = false,
        )
        assertEquals(1L, navigatedChapter?.id)
        assertEquals(ChapterNavTarget.End, navigatedTarget)
    }

    @Test
    fun `LTR page overscroll at end of content triggers next chapter when next transition exists`() {
        val currChapter = createChapter(1L)
        val nextChapter = createChapter(2L)
        val nextTransition =
            ReaderUiItem.Transition(ChapterTransition.Next(from = currChapter, to = nextChapter))
        val pages = currChapter.pages!!.map { ReaderUiItem.Page(it) }
        val items = pages + listOf(nextTransition)

        var navigatedChapter: Chapter? = null
        var navigatedTarget: ChapterNavTarget? = null

        val handler =
            PagerOverscrollHandler(
                isRtl = false,
                thresholdPx = 100f,
                onNavigateToChapter = { ch, target ->
                    navigatedChapter = ch
                    navigatedTarget = target
                },
            )

        // At lastIndex with delta < 0 (swiping forward)
        handler.handleScrollDelta(
            delta = -120f,
            currentIndex = items.lastIndex,
            items = items,
            isNavigating = false,
        )
        assertEquals(2L, navigatedChapter?.id)
        assertEquals(ChapterNavTarget.Start, navigatedTarget)
    }

    @Test
    fun `isNavigating true suppresses overscroll trigger`() {
        val currChapter = createChapter(1L)
        val nextChapter = createChapter(2L)
        val transition =
            ReaderUiItem.Transition(ChapterTransition.Next(from = currChapter, to = nextChapter))
        val items = listOf(transition)

        var navigatedChapter: Chapter? = null

        val handler =
            PagerOverscrollHandler(
                isRtl = false,
                thresholdPx = 100f,
                onNavigateToChapter = { ch, _ -> navigatedChapter = ch },
            )

        handler.handleScrollDelta(
            delta = -200f,
            currentIndex = 0,
            items = items,
            isNavigating = true,
        )
        assertNull(navigatedChapter)
    }

    @Test
    fun `reversing scroll direction resets accumulated overscroll`() {
        val currChapter = createChapter(1L)
        val nextChapter = createChapter(2L)
        val transition =
            ReaderUiItem.Transition(ChapterTransition.Next(from = currChapter, to = nextChapter))
        val items = listOf(transition)

        val handler =
            PagerOverscrollHandler(
                isRtl = false,
                thresholdPx = 100f,
                onNavigateToChapter = { _, _ -> },
            )

        // Scroll forward: delta -60f
        handler.handleScrollDelta(
            delta = -60f,
            currentIndex = 0,
            items = items,
            isNavigating = false,
        )
        assertEquals(-60f, handler.accumulatedOverscroll)

        // Reversal: delta +10f -> resets prior negative accumulation and starts new accumulation at
        // +10f (instead of -50f)
        handler.handleScrollDelta(
            delta = 10f,
            currentIndex = 0,
            items = items,
            isNavigating = false,
        )
        assertEquals(10f, handler.accumulatedOverscroll)
    }

    @Test
    fun `reset clears accumulated overscroll`() {
        val handler =
            PagerOverscrollHandler(
                isRtl = false,
                thresholdPx = 100f,
                onNavigateToChapter = { _, _ -> },
            )
        handler.accumulatedOverscroll = 85f
        handler.reset()
        assertEquals(0f, handler.accumulatedOverscroll)
    }
}
