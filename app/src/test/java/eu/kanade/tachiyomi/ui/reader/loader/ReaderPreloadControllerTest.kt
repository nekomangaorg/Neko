package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.domain.CheckTallPageUseCase
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderPreloadControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val checkTallPage = mockk<CheckTallPageUseCase>(relaxed = true)
    private val memoryWarmManager = mockk<MemoryCacheWarmManager>(relaxed = true)
    private val onPreloadChapter = mockk<(ReaderChapter) -> Unit>(relaxed = true)

    private lateinit var controller: ReaderPreloadControllerImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        controller =
            ReaderPreloadControllerImpl(
                context = context,
                scope = testScope,
                memoryCacheWarmManager = memoryWarmManager,
                checkTallPage = checkTallPage,
                onRequestPreloadChapter = onPreloadChapter,
                getScreenHeight = { 2000 },
                ioDispatcher = testDispatcher,
            )
    }

    @After
    fun tearDown() {
        controller.release()
        Dispatchers.resetMain()
    }

    private fun createChapter(id: Long, pageCount: Int): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = id.toFloat()
            }
        val readerChapter = ReaderChapter(dbChapter)
        val mockLoader = mockk<PageLoader>(relaxed = true)
        val pages =
            (0 until pageCount).map { index ->
                ReaderPage(index = index, url = "url_$index", imageUrl = "img_$index").apply {
                    this.chapter = readerChapter
                    this.renderedHeight = 1000
                    this.status = Page.State.READY
                }
            }
        readerChapter.state = ReaderChapter.State.Loaded(pages)
        readerChapter.pageLoader = mockLoader
        return readerChapter
    }

    @Test
    fun `onPositionChanged on empty items resets state to idle and empty ranges`() {
        controller.onPositionChanged(
            currentIndex = 5,
            items = emptyList(),
            preloadAmount = 4,
            isRtl = false,
            isWebtoon = false,
        )

        val state = controller.state.value
        assertEquals(0, state.activeIndex)
        assertEquals(IntRange.EMPTY, state.windowRange)
        assertEquals(IntRange.EMPTY, state.memoryRange)
        assertTrue(state.isIdle)
    }

    @Test
    fun `resolvePagerIndices in LTR mode returns ahead window and bounds memory window to 2`() {
        val chapter = createChapter(1L, 10)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }

        val result =
            controller.resolvePagerIndices(
                safeStart = 2,
                items = items,
                preloadAmount = 5,
                isRtl = false,
            )

        // Preload indices should include current (2), ahead (3..7), and behind (1, 0)
        assertTrue(result.orderedIndices.contains(2))
        assertTrue(result.orderedIndices.contains(3))
        assertTrue(result.orderedIndices.contains(7))
        assertEquals(0..7, result.diskRange)

        // Memory window is strictly 2 ahead and 2 behind: 0..4
        assertTrue(result.memoryIndices.contains(2))
        assertTrue(result.memoryIndices.contains(3))
        assertTrue(result.memoryIndices.contains(4))
        assertFalse(result.memoryIndices.contains(5))
        assertEquals(0..4, result.memoryRange)
    }

    @Test
    fun `resolvePagerIndices in RTL mode returns ahead indices stepping backwards`() {
        val chapter = createChapter(1L, 10)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }

        val result =
            controller.resolvePagerIndices(
                safeStart = 5,
                items = items,
                preloadAmount = 3,
                isRtl = true,
            )

        // In RTL, ahead goes down (4, 3, 2), behind goes up (6, 7)
        assertTrue(result.orderedIndices.contains(5))
        assertTrue(result.orderedIndices.contains(4))
        assertTrue(result.orderedIndices.contains(3))
        assertTrue(result.orderedIndices.contains(2))
        assertEquals(2..7, result.diskRange)

        // Memory window: ahead 2 (4, 3), behind 2 (6, 7)
        assertTrue(result.memoryIndices.contains(5))
        assertTrue(result.memoryIndices.contains(4))
        assertTrue(result.memoryIndices.contains(3))
        assertFalse(result.memoryIndices.contains(2))
        assertEquals(3..7, result.memoryRange)
    }

    @Test
    fun `resolveWebtoonIndices bounds memory budget strictly to MAX_MEMORY_PRELOAD_PAGES`() {
        val chapter = createChapter(1L, 30)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }

        val result =
            controller.resolveWebtoonIndices(
                safeStart = 0,
                items = items,
                preloadAmount = 20, // user set 20
            )

        // Disk window expands to preloadAmount
        assertTrue(result.diskRange.last >= 20)

        // Memory window is capped to MAX_MEMORY_PRELOAD_PAGES (4)
        assertTrue(result.memoryRange.last <= ReaderPreloadControllerImpl.MAX_MEMORY_PRELOAD_SLICES)
        assertTrue(
            result.memoryIndices.size <= ReaderPreloadControllerImpl.MAX_MEMORY_PRELOAD_SLICES + 1
        )
    }

    @Test
    fun `onPositionChanged executes disk prefetch and warms memory cache`() = testScope.runTest {
        val chapter = createChapter(1L, 10)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }

        controller.onPositionChanged(
            currentIndex = 0,
            items = items,
            preloadAmount = 4,
            isRtl = false,
            isWebtoon = false,
        )

        advanceTimeBy(ReaderPreloadControllerImpl.DEBOUNCE_DELAY_MS + 10L)
        runCurrent()

        // Verify disk loader was called for items
        val loader = chapter.pageLoader!!
        coVerify(atLeast = 1) { loader.loadPage(any()) }

        // Verify memory cache was warmed for pages within memory window
        io.mockk.verify(atLeast = 1) {
            memoryWarmManager.warmMemoryCache(
                key = any(),
                data = any(),
                crossfade = any(),
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `rapid scrubs cancel previous jobs and debounce safely`() = testScope.runTest {
        val chapter = createChapter(1L, 20)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }

        controller.onPositionChanged(
            currentIndex = 2,
            items = items,
            preloadAmount = 4,
            isRtl = false,
            isWebtoon = false,
        )
        advanceTimeBy(20L) // Less than debounce delay

        // Scrub jumps to page 15
        controller.onPositionChanged(
            currentIndex = 15,
            items = items,
            preloadAmount = 4,
            isRtl = false,
            isWebtoon = false,
        )
        advanceTimeBy(ReaderPreloadControllerImpl.DEBOUNCE_DELAY_MS + 10L)
        runCurrent()

        assertEquals(15, controller.state.value.activeIndex)
    }

    @Test
    fun `failed download sets status to Error and retryPage triggers retry`() = testScope.runTest {
        val chapter = createChapter(1L, 5)
        val failingPage = chapter.pages!![0]
        failingPage.status = Page.State.ERROR

        val loader = chapter.pageLoader!!
        coEvery { loader.loadPage(failingPage) } answers { failingPage.status = Page.State.ERROR }

        val item = ReaderUiItem.Page(failingPage)
        val items = listOf(item)

        controller.onPositionChanged(
            currentIndex = 0,
            items = items,
            preloadAmount = 1,
            isRtl = false,
            isWebtoon = false,
        )
        advanceTimeBy(ReaderPreloadControllerImpl.DEBOUNCE_DELAY_MS + 10L)
        runCurrent()

        val errorKey = item.key("pager")
        val status = controller.state.value.pageStatuses[errorKey]
        assertTrue("Expected Error status but got $status", status is PreloadPageStatus.Error)

        // Now retry
        failingPage.status = Page.State.READY
        coEvery { loader.loadPage(failingPage) } answers { failingPage.status = Page.State.READY }

        controller.retryPage(item)
        runCurrent()

        coVerify(atLeast = 1) { loader.retryPage(failingPage) }
    }

    @Test
    fun `requestPreloadChapter invokes onRequestPreloadChapter callback`() {
        val chapter = createChapter(2L, 5)
        controller.requestPreloadChapter(chapter)
        io.mockk.verify(exactly = 1) { onPreloadChapter.invoke(chapter) }
    }

    @Test
    fun `tall page splitting invokes onPageSplit callback in webtoon mode`() = testScope.runTest {
        val chapter = createChapter(1L, 5)
        val page = chapter.pages!![0]
        val splits =
            listOf(
                ReaderPageSplit(page = page, topOffset = 0, splitHeight = 1000),
                ReaderPageSplit(page = page, topOffset = 1000, splitHeight = 1000),
            )
        every { checkTallPage.invoke(page, any(), any()) } returns splits

        var splitInvoked = false
        controller =
            ReaderPreloadControllerImpl(
                context = context,
                scope = testScope,
                memoryCacheWarmManager = memoryWarmManager,
                checkTallPage = checkTallPage,
                isSplitTallPagesEnabled = { true },
                onPageSplit = { _, _ -> splitInvoked = true },
                getScreenHeight = { 2000 },
                ioDispatcher = testDispatcher,
            )

        val items = listOf(ReaderUiItem.Page(page))
        controller.onPositionChanged(
            currentIndex = 0,
            items = items,
            preloadAmount = 1,
            isRtl = false,
            isWebtoon = true,
        )

        advanceTimeBy(ReaderPreloadControllerImpl.DEBOUNCE_DELAY_MS + 10L)
        runCurrent()

        assertTrue(splitInvoked)
    }

    @Test
    fun `release clears all state and resets to idle`() = testScope.runTest {
        val chapter = createChapter(1L, 5)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }

        controller.onPositionChanged(
            currentIndex = 0,
            items = items,
            preloadAmount = 2,
            isRtl = false,
            isWebtoon = false,
        )
        advanceTimeBy(ReaderPreloadControllerImpl.DEBOUNCE_DELAY_MS + 10L)
        runCurrent()

        controller.release()

        val state = controller.state.value
        assertTrue(state.isIdle)
        assertTrue(state.pageStatuses.isEmpty())
        io.mockk.verify(atLeast = 1) { memoryWarmManager.release() }
    }
}
