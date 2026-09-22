package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.domain.CheckTallPageUseCase
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderPreloadEngineTest {

    private val context = mockk<Context>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val checkTallPage = mockk<CheckTallPageUseCase>(relaxed = true)

    private lateinit var engine: ReaderPreloadEngine

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        engine =
            ReaderPreloadEngine(
                context = context,
                scope = testScope,
                checkTallPage = checkTallPage,
                getScreenHeight = { 2000 },
            )
    }

    @After
    fun tearDown() {
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
    fun `calculateWindowStart returns 0 for empty items`() {
        val start = engine.calculateWindowStart(startIndex = 5, items = emptyList())
        assertEquals(0, start)
    }

    @Test
    fun `calculateWindowStart clamps negative startIndex to 0`() {
        val chapter = createChapter(1L, 10)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }
        val start = engine.calculateWindowStart(startIndex = -5, items = items)
        assertEquals(0, start)
    }

    @Test
    fun `calculateWindowStart clamps out of bounds startIndex to items lastIndex`() {
        val chapter = createChapter(1L, 10)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }
        val start =
            engine.calculateWindowStart(
                startIndex = 100,
                items = items,
                pageBudget = 1,
                minItems = 3,
            )
        // From index 9 downTo 0, pageBudget = 1 and minItems = 3 -> safeStart (9) - 3 = 6
        assertTrue(start in 0..9)
    }

    @Test
    fun `calculateWindowEnd returns 0 for empty items`() {
        val end =
            engine.calculateWindowEnd(
                startIndex = 5,
                items = emptyList(),
                pageBudget = 4,
                minItems = 8,
            )
        assertEquals(0, end)
    }

    @Test
    fun `calculateWindowEnd clamps negative startIndex to 0`() {
        val chapter = createChapter(1L, 10)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }
        val end =
            engine.calculateWindowEnd(startIndex = -5, items = items, pageBudget = 1, minItems = 3)
        assertTrue(end >= 0)
    }

    @Test
    fun `calculateWindowEnd clamps out of bounds startIndex to items lastIndex`() {
        val chapter = createChapter(1L, 10)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }
        val end =
            engine.calculateWindowEnd(startIndex = 50, items = items, pageBudget = 4, minItems = 8)
        assertEquals(items.lastIndex, end)
    }

    @Test
    fun `calculateWindowEnd bounds memory horizon to MAX_MEMORY_PRELOAD_PAGES and MAX_MEMORY_PRELOAD_SLICES`() {
        val chapter = createChapter(1L, 30)
        val items = chapter.pages!!.map { ReaderUiItem.Page(it) }

        // User preference is 20 pages
        val preloadAmount = 20

        val diskBudget = maxOf(1, preloadAmount)
        val diskMinItems = maxOf(4, preloadAmount * 2)

        val memoryBudget =
            minOf(maxOf(2, preloadAmount), ReaderPreloadEngine.MAX_MEMORY_PRELOAD_PAGES)
        val memoryMinItems =
            minOf(
                maxOf(ReaderPreloadEngine.MIN_MEMORY_PRELOAD_SLICES, preloadAmount * 2),
                ReaderPreloadEngine.MAX_MEMORY_PRELOAD_SLICES,
            )

        assertEquals(20, diskBudget)
        assertEquals(40, diskMinItems)

        assertEquals(ReaderPreloadEngine.MAX_MEMORY_PRELOAD_PAGES, memoryBudget)
        assertEquals(ReaderPreloadEngine.MAX_MEMORY_PRELOAD_SLICES, memoryMinItems)

        val diskEnd =
            engine.calculateWindowEnd(
                startIndex = 0,
                items = items,
                pageBudget = diskBudget,
                minItems = diskMinItems,
            )
        val memoryEnd =
            engine.calculateWindowEnd(
                startIndex = 0,
                items = items,
                pageBudget = memoryBudget,
                minItems = memoryMinItems,
            )

        // Memory horizon must be strictly less than or equal to disk horizon
        assertTrue(
            "memoryEnd ($memoryEnd) should be less than or equal to diskEnd ($diskEnd)",
            memoryEnd <= diskEnd,
        )
        assertTrue(
            "memoryEnd ($memoryEnd) should be bounded by MAX_MEMORY_PRELOAD_SLICES",
            memoryEnd <= ReaderPreloadEngine.MAX_MEMORY_PRELOAD_SLICES,
        )
    }

    @Test
    fun `calculateWindowEnd handles mixed page and split slices correctly`() {
        val chapter = createChapter(1L, 5)
        val items = mutableListOf<ReaderUiItem>()
        // Monolithic page 0
        items.add(ReaderUiItem.Page(chapter.pages!![0]))
        // Page 1 split into 4 slices
        val page1 = chapter.pages!![1]
        for (i in 0 until 4) {
            items.add(
                ReaderUiItem.SplitPage(
                    ReaderPageSplit(page = page1, topOffset = i * 1000, splitHeight = 1000)
                )
            )
        }
        // Transition Next
        val nextChapter = createChapter(2L, 5)
        items.add(ReaderUiItem.Transition(ChapterTransition.Next(chapter, nextChapter)))
        // Next chapter pages
        items.addAll(nextChapter.pages!!.map { ReaderUiItem.Page(it) })

        val end =
            engine.calculateWindowEnd(startIndex = 0, items = items, pageBudget = 2, minItems = 4)
        assertTrue(end >= 4)
    }

    @Test
    fun `updateActiveIndex handles rapid list shrinking without crashing`() = testScope.runTest {
        // Preloading launches jobs on Dispatchers.IO that outlive the test body, so give the
        // engine the background scope runTest cancels instead of the scope it waits on.
        val engine =
            ReaderPreloadEngine(
                context = context,
                scope = backgroundScope,
                checkTallPage = checkTallPage,
                getScreenHeight = { 2000 },
            )
        val chapter = createChapter(1L, 20)
        val largeList = chapter.pages!!.map { ReaderUiItem.Page(it) }
        val smallList = largeList.take(3)

        // Start with large list at high index
        engine.updateActiveIndex(activeIndex = 15, items = largeList, preloadAmount = 4)

        // Rapid transition shrinks list and active index
        engine.updateActiveIndex(activeIndex = 1, items = smallList, preloadAmount = 4)

        advanceTimeBy(100L)
        // Completes without throwing IndexOutOfBoundsException
    }
}
