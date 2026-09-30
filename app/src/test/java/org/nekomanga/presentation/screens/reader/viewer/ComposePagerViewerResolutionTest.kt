package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderNavCommand
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private fun createConfig(
        activeChapterId: Long = 1L,
        animatedTransitions: Boolean = true,
        isRtl: Boolean = false,
    ): PagerViewerConfigUiModel {
        return PagerViewerConfigUiModel(
            activeChapterId = activeChapterId,
            animatedTransitions = animatedTransitions,
            isRtl = isRtl,
            backgroundColor = Color.Black,
        )
    }

    @Test
    fun `executeNavCommand catches CancellationException when animateScrollToPage is interrupted and returns true`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L, animatedTransitions = true)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 0
            coEvery { mockPagerState.animateScrollToPage(any(), any()) } throws
                CancellationException("Interrupted by subsequent tap")

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.ScrollToItem(itemIndex = 2, animated = true),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue("Interrupted scroll animation must return true and not throw", result)
        }

    @Test
    fun `executeNavCommand returns false when target chapter is not yet loaded in items`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 3)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 0

            val result =
                executeNavCommand(
                    command =
                        ReaderNavCommand.ScrollToPage(
                            pageIndex = 0,
                            chapterId = 2L,
                            animated = true,
                        ),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertFalse(
                "Unresolved chapter target must return false to allow pending queueing",
                result,
            )
        }

    @Test
    fun `executeNavCommand navigates to target item successfully when chapter exists`() = runTest {
        val ch1 = createChapter(1L, pageCount = 5)
        val items = (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
        val config = createConfig(activeChapterId = 1L, animatedTransitions = false)

        val mockPagerState = mockk<PagerState>(relaxed = true)
        every { mockPagerState.currentPage } returns 0

        val result =
            executeNavCommand(
                command = ReaderNavCommand.ScrollToItem(itemIndex = 3, animated = false),
                pagerState = mockPagerState,
                items = items,
                config = config,
            )

        assertTrue(result)
        coVerify { mockPagerState.scrollToPage(3) }
    }

    @Test
    fun `executeNavCommand with StepPage forward in LTR animates scroll to currentPage + 1`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config =
                createConfig(activeChapterId = 1L, animatedTransitions = true, isRtl = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 1

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.StepPage(forward = true),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify { mockPagerState.animateScrollToPage(page = 2, animationSpec = any()) }
        }

    @Test
    fun `executeNavCommand with StepPage forward in RTL animates scroll to currentPage - 1`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config =
                createConfig(activeChapterId = 1L, animatedTransitions = true, isRtl = true)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 3

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.StepPage(forward = true),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify { mockPagerState.animateScrollToPage(page = 2, animationSpec = any()) }
        }

    @Test
    fun `executeNavCommand with StepPage backward in LTR animates scroll to currentPage - 1`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config =
                createConfig(activeChapterId = 1L, animatedTransitions = true, isRtl = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 3

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.StepPage(forward = false),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify { mockPagerState.animateScrollToPage(page = 2, animationSpec = any()) }
        }

    @Test
    fun `executeNavCommand with StepPage backward in RTL animates scroll to currentPage + 1`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config =
                createConfig(activeChapterId = 1L, animatedTransitions = true, isRtl = true)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 2

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.StepPage(forward = false),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify { mockPagerState.animateScrollToPage(page = 3, animationSpec = any()) }
        }

    @Test
    fun `executeNavCommand with StepPage backward at start boundary does not scroll and returns true`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config =
                createConfig(activeChapterId = 1L, animatedTransitions = true, isRtl = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 0

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.StepPage(forward = false),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify(exactly = 0) { mockPagerState.animateScrollToPage(any(), any()) }
            coVerify(exactly = 0) { mockPagerState.scrollToPage(any()) }
        }

    @Test
    fun `executeNavCommand with StepPage forward at end boundary does not scroll and returns true`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config =
                createConfig(activeChapterId = 1L, animatedTransitions = true, isRtl = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 4 // lastIndex

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.StepPage(forward = true),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify(exactly = 0) { mockPagerState.animateScrollToPage(any(), any()) }
            coVerify(exactly = 0) { mockPagerState.scrollToPage(any()) }
        }

    @Test
    fun `executeNavCommand with StepPage with animatedTransitions disabled calls scrollToPage`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config =
                createConfig(activeChapterId = 1L, animatedTransitions = false, isRtl = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 1

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.StepPage(forward = true),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify { mockPagerState.scrollToPage(2) }
            coVerify(exactly = 0) { mockPagerState.animateScrollToPage(any(), any()) }
        }

    @Test
    fun `executeNavCommand with StepPage catches CancellationException when rapid tapping interrupts animation`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config =
                createConfig(activeChapterId = 1L, animatedTransitions = true, isRtl = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 1
            coEvery { mockPagerState.animateScrollToPage(any(), any()) } throws
                CancellationException("Interrupted by rapid user tap")

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.StepPage(forward = true),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue("StepPage interruption must not escape or return false", result)
        }

    @Test
    fun `executeNavCommand with SnapToPage calls scrollToPage when target page is found and returns true`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 0

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.SnapToPage(pageIndex = 3, chapterId = 1L),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify { mockPagerState.scrollToPage(3) }
        }

    @Test
    fun `executeNavCommand with SnapToPage scrolls to transition when chapter is not loaded and returns false`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 2)
            val ch2 = createChapter(2L, pageCount = 2)
            val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
            val items =
                listOf(
                    ReaderUiItem.Page(ch1Pages[0]),
                    ReaderUiItem.Page(ch1Pages[1]),
                    ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2)),
                )
            val config = createConfig(activeChapterId = 1L)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 0

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.SnapToPage(pageIndex = 0, chapterId = 2L),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertFalse(
                "Must return false so pending command is preserved until chapter loads",
                result,
            )
            coVerify { mockPagerState.scrollToPage(2) }
        }

    @Test
    fun `executeNavCommand with SnapToPage returns false when neither target page nor transition exists`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 3)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 0

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.SnapToPage(pageIndex = 0, chapterId = 99L),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertFalse(result)
            coVerify(exactly = 0) { mockPagerState.scrollToPage(any()) }
        }

    @Test
    fun `executeNavCommand with ScrollToPage animates to transition when chapter is not loaded and returns false`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 2)
            val ch2 = createChapter(2L, pageCount = 2)
            val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
            val items =
                listOf(
                    ReaderUiItem.Page(ch1Pages[0]),
                    ReaderUiItem.Page(ch1Pages[1]),
                    ReaderUiItem.Transition(ChapterTransition.Next(ch1, ch2)),
                )
            val config = createConfig(activeChapterId = 1L, animatedTransitions = true)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 0

            val result =
                executeNavCommand(
                    command =
                        ReaderNavCommand.ScrollToPage(
                            pageIndex = 0,
                            chapterId = 2L,
                            animated = true,
                        ),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertFalse(result)
            coVerify { mockPagerState.animateScrollToPage(page = 2, animationSpec = any()) }
        }

    @Test
    fun `executeNavCommand with ScrollToPage calls scrollToPage when animatedTransitions is false and returns true`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 5)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L, animatedTransitions = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 0

            val result =
                executeNavCommand(
                    command =
                        ReaderNavCommand.ScrollToPage(
                            pageIndex = 3,
                            chapterId = 1L,
                            animated = true,
                        ),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify { mockPagerState.scrollToPage(3) }
            coVerify(exactly = 0) { mockPagerState.animateScrollToPage(any(), any()) }
        }

    @Test
    fun `executeNavCommand with ScrollToItem clamps target index when out of bounds`() = runTest {
        val ch1 = createChapter(1L, pageCount = 3)
        val items = (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
        val config = createConfig(activeChapterId = 1L, animatedTransitions = false)

        val mockPagerState = mockk<PagerState>(relaxed = true)
        every { mockPagerState.currentPage } returns 0

        val result =
            executeNavCommand(
                command = ReaderNavCommand.ScrollToItem(itemIndex = 10, animated = false),
                pagerState = mockPagerState,
                items = items,
                config = config,
            )

        assertTrue(result)
        coVerify { mockPagerState.scrollToPage(2) } // clamped to lastIndex 2
    }

    @Test
    fun `executeNavCommand with ScrollToItem does not scroll when already on target page`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 3)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L, animatedTransitions = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 2

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.ScrollToItem(itemIndex = 2, animated = false),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify(exactly = 0) { mockPagerState.scrollToPage(any()) }
            coVerify(exactly = 0) { mockPagerState.animateScrollToPage(any(), any()) }
        }

    @Test
    fun `executeNavCommand with ScrollByDelta returns true`() = runTest {
        val ch1 = createChapter(1L, pageCount = 3)
        val items = (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
        val config = createConfig(activeChapterId = 1L)
        val mockPagerState = mockk<PagerState>(relaxed = true)

        val result =
            executeNavCommand(
                command = ReaderNavCommand.ScrollByDelta(delta = 100f),
                pagerState = mockPagerState,
                items = items,
                config = config,
            )

        assertTrue(result)
    }

    @Test
    fun `executeNavCommand with ScrollToItem navigates to page 0 when jumping back to page one`() =
        runTest {
            val ch1 = createChapter(1L, pageCount = 20)
            val items =
                (ch1.state as ReaderChapter.State.Loaded).pages.map { ReaderUiItem.Page(it) }
            val config = createConfig(activeChapterId = 1L, animatedTransitions = false)

            val mockPagerState = mockk<PagerState>(relaxed = true)
            every { mockPagerState.currentPage } returns 5 // User was at page 5

            val result =
                executeNavCommand(
                    command = ReaderNavCommand.ScrollToItem(itemIndex = 0, animated = false),
                    pagerState = mockPagerState,
                    items = items,
                    config = config,
                )

            assertTrue(result)
            coVerify { mockPagerState.scrollToPage(0) }
        }

    @Test
    fun `resolveItemIndexForPage in RTL resolves target chapter page 0 correctly when previous chapter is prepended`() {
        val ch1 = createChapter(1L, pageCount = 3)
        val ch2 = createChapter(2L, pageCount = 3)

        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

        // In RTL, list is reversed: [Ch2 P2, Ch2 P1, Ch2 P0, PrevTrans, Ch1 P2, Ch1 P1, Ch1 P0]
        val rtlItems =
            listOf(
                ReaderUiItem.Page(ch2Pages[2]),
                ReaderUiItem.Page(ch2Pages[1]),
                ReaderUiItem.Page(ch2Pages[0]),
                ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)),
                ReaderUiItem.Page(ch1Pages[2]),
                ReaderUiItem.Page(ch1Pages[1]),
                ReaderUiItem.Page(ch1Pages[0]),
            )

        // Chapter 2 Page 0 is at index 2
        val resolvedCh2P0 =
            resolveItemIndexForPage(items = rtlItems, targetChapterId = 2L, pageIndex = 0)
        assertEquals(2, resolvedCh2P0)

        // Chapter 2 Page 2 is at index 0
        val resolvedCh2P2 =
            resolveItemIndexForPage(items = rtlItems, targetChapterId = 2L, pageIndex = 2)
        assertEquals(0, resolvedCh2P2)

        // Chapter 1 Page 0 is at index 6
        val resolvedCh1P0 =
            resolveItemIndexForPage(items = rtlItems, targetChapterId = 1L, pageIndex = 0)
        assertEquals(6, resolvedCh1P0)
    }

    @Test
    fun `executeNavCommand with SnapToPage in RTL scrolls to correct page index`() = runTest {
        val ch1 = createChapter(1L, pageCount = 3)
        val ch2 = createChapter(2L, pageCount = 3)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

        val rtlItems =
            listOf(
                ReaderUiItem.Page(ch2Pages[2]),
                ReaderUiItem.Page(ch2Pages[1]),
                ReaderUiItem.Page(ch2Pages[0]),
                ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)),
                ReaderUiItem.Page(ch1Pages[2]),
                ReaderUiItem.Page(ch1Pages[1]),
                ReaderUiItem.Page(ch1Pages[0]),
            )
        val config = createConfig(activeChapterId = 2L, isRtl = true)

        val mockPagerState = mockk<PagerState>(relaxed = true)
        every { mockPagerState.currentPage } returns 0

        // Snap to Chapter 2 Page 0 in RTL (should navigate to item index 2)
        val result =
            executeNavCommand(
                command = ReaderNavCommand.SnapToPage(pageIndex = 0, chapterId = 2L),
                pagerState = mockPagerState,
                items = rtlItems,
                config = config,
            )

        assertTrue(result)
        coVerify { mockPagerState.scrollToPage(2) }
    }

    @Test
    fun `executeNavCommand with ScrollToPage in RTL animates to correct page index`() = runTest {
        val ch1 = createChapter(1L, pageCount = 3)
        val ch2 = createChapter(2L, pageCount = 3)
        val ch1Pages = (ch1.state as ReaderChapter.State.Loaded).pages
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages

        val rtlItems =
            listOf(
                ReaderUiItem.Page(ch2Pages[2]),
                ReaderUiItem.Page(ch2Pages[1]),
                ReaderUiItem.Page(ch2Pages[0]),
                ReaderUiItem.Transition(ChapterTransition.Prev(ch2, ch1)),
                ReaderUiItem.Page(ch1Pages[2]),
                ReaderUiItem.Page(ch1Pages[1]),
                ReaderUiItem.Page(ch1Pages[0]),
            )
        val config = createConfig(activeChapterId = 2L, isRtl = true, animatedTransitions = true)

        val mockPagerState = mockk<PagerState>(relaxed = true)
        every { mockPagerState.currentPage } returns 0

        val result =
            executeNavCommand(
                command =
                    ReaderNavCommand.ScrollToPage(pageIndex = 0, chapterId = 2L, animated = true),
                pagerState = mockPagerState,
                items = rtlItems,
                config = config,
            )

        assertTrue(result)
        coVerify { mockPagerState.animateScrollToPage(page = 2, animationSpec = any()) }
    }

    @Test
    fun `executeNavCommand with ScrollToItem in RTL scrolls to exact item index`() = runTest {
        val ch2 = createChapter(2L, pageCount = 5)
        val ch2Pages = (ch2.state as ReaderChapter.State.Loaded).pages
        val rtlItems = ch2Pages.reversed().map { ReaderUiItem.Page(it) }
        val config = createConfig(activeChapterId = 2L, isRtl = true, animatedTransitions = false)

        val mockPagerState = mockk<PagerState>(relaxed = true)
        every { mockPagerState.currentPage } returns 0

        // User dragged slider to page 1 (which in RTL maps to item index 4)
        val result =
            executeNavCommand(
                command = ReaderNavCommand.ScrollToItem(itemIndex = 4, animated = false),
                pagerState = mockPagerState,
                items = rtlItems,
                config = config,
            )

        assertTrue(result)
        coVerify { mockPagerState.scrollToPage(4) }
    }
}
