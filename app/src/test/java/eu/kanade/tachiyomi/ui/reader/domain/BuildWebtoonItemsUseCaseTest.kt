package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildWebtoonItemsUseCaseTest {

    private val checkTallPageUseCase = CheckTallPageUseCase()
    private val useCase = BuildWebtoonItemsUseCase(checkTallPageUseCase)

    private fun createChapter(
        id: Long,
        pageCount: Int = 10,
        chapterNumber: Float = id.toFloat(),
    ): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = chapterNumber
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
    fun `invoke with fully loaded consecutive chapters adds padding, prevTransition, currentPages, and nextPages`() {
        val prevChapter = createChapter(1L, pageCount = 5, chapterNumber = 1f)
        val currChapter = createChapter(2L, pageCount = 10, chapterNumber = 2f)
        val nextChapter = createChapter(3L, pageCount = 5, chapterNumber = 3f)

        val viewerChapters = ViewerChapters(currChapter, prevChapter, nextChapter)
        val result = useCase(viewerChapters, forceTransition = false, screenHeight = 1000)

        // Previous chapter padding (last 2: pages 3 and 4) + prev transition + 10 curr pages + 5
        // next pages = 18 items
        val items = result.items
        assertEquals(18, items.size)

        // Padded items
        assertEquals(1L, items[0].chapterId)
        assertEquals(3, items[0].pageIndex)
        assertEquals(1L, items[1].chapterId)
        assertEquals(4, items[1].pageIndex)

        // Prev transition
        assertTrue(items[2] is ReaderUiItem.Transition)
        assertTrue((items[2] as ReaderUiItem.Transition).transition is ChapterTransition.Prev)
        assertNull(items[2].chapterId)

        // Current chapter pages
        assertEquals(2L, items[3].chapterId)
        assertEquals(0, items[3].pageIndex)
        assertEquals(2L, items[12].chapterId)
        assertEquals(9, items[12].pageIndex)

        // Next chapter pages (seamless because consecutive and loaded)
        assertEquals(3L, items[13].chapterId)
        assertEquals(0, items[13].pageIndex)
        assertEquals(3L, items[17].chapterId)
        assertEquals(4, items[17].pageIndex)
    }

    @Test
    fun `invoke adds next transition when next chapter is not loaded`() {
        val prevChapter = createChapter(1L, pageCount = 5, chapterNumber = 1f)
        val currChapter = createChapter(2L, pageCount = 10, chapterNumber = 2f)
        val nextChapter =
            createChapter(3L, pageCount = 5, chapterNumber = 3f).apply {
                state = ReaderChapter.State.Wait
            }

        val viewerChapters = ViewerChapters(currChapter, prevChapter, nextChapter)
        val result = useCase(viewerChapters, forceTransition = false)

        val items = result.items
        // 2 prev pages + 1 prev trans + 10 curr pages + 1 next trans = 14 items
        assertEquals(14, items.size)
        assertTrue(items[13] is ReaderUiItem.Transition)
        assertTrue((items[13] as ReaderUiItem.Transition).transition is ChapterTransition.Next)
        assertTrue(result.hadTransitionForNext)
    }

    @Test
    fun `invoke adds next transition when missing chapters exist between current and next`() {
        val currChapter = createChapter(1L, pageCount = 5, chapterNumber = 1f)
        // Chapter gap: 1 to 5
        val nextChapter = createChapter(5L, pageCount = 5, chapterNumber = 5f)

        val viewerChapters = ViewerChapters(currChapter, null, nextChapter)
        val result = useCase(viewerChapters, forceTransition = false)

        val items = result.items
        // 1 prev trans + 5 curr pages + 1 next trans + 5 next pages = 12 items
        assertEquals(12, items.size)
        assertTrue(items[0] is ReaderUiItem.Transition)
        assertTrue(items[6] is ReaderUiItem.Transition)
        assertTrue((items[6] as ReaderUiItem.Transition).transition is ChapterTransition.Next)
    }

    @Test
    fun `invoke preserves and reuses existing splits from existingItems`() {
        val currChapter = createChapter(1L, pageCount = 2, chapterNumber = 1f)
        val targetPage = currChapter.pages!![0]
        val split1 = ReaderPageSplit(targetPage, 0, 1000)
        val split2 = ReaderPageSplit(targetPage, 1000, 1000)
        val existingItems =
            listOf(
                ReaderUiItem.SplitPage(split1),
                ReaderUiItem.SplitPage(split2),
            )

        val viewerChapters = ViewerChapters(currChapter, null, null)
        val result = useCase(viewerChapters, existingItems = existingItems)

        val items = result.items
        // 1 prev trans + 2 splits for page 0 + 1 page for page 1 + 1 next trans (null next) = 5
        // items
        assertEquals(5, items.size)
        assertTrue(items[1] is ReaderUiItem.SplitPage)
        assertEquals(0, (items[1] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(items[2] is ReaderUiItem.SplitPage)
        assertEquals(1000, (items[2] as ReaderUiItem.SplitPage).split.topOffset)
        assertTrue(result.tallSplitPages.contains(targetPage))
    }

    @Test
    fun `invoke splits ready pages with valid stream upfront when screenHeight is greater than 0`() {
        val mockCheckTallPage = mockk<CheckTallPageUseCase>()
        val customUseCase = BuildWebtoonItemsUseCase(mockCheckTallPage)
        val currChapter = createChapter(1L, pageCount = 1, chapterNumber = 1f)
        val page =
            currChapter.pages!![0].apply {
                status = Page.State.READY
                stream = { ByteArrayInputStream(ByteArray(0)) }
            }
        val splits =
            listOf(
                ReaderPageSplit(page, 0, 500),
                ReaderPageSplit(page, 500, 500),
            )
        every { mockCheckTallPage(page, 1000) } returns splits

        val viewerChapters = ViewerChapters(currChapter, null, null)
        val result = customUseCase(viewerChapters, screenHeight = 1000)

        val items = result.items
        // 1 prev trans + 2 splits + 1 next trans (null next) = 4 items
        assertEquals(4, items.size)
        assertTrue(items[1] is ReaderUiItem.SplitPage)
        assertTrue(items[2] is ReaderUiItem.SplitPage)
        assertEquals(splits, page.precomputedSplits)
        assertTrue(result.tallSplitPages.contains(page))
    }

    @Test
    fun `invoke reuses precomputedSplits from ReaderPage`() {
        val currChapter = createChapter(1L, pageCount = 1, chapterNumber = 1f)
        val page = currChapter.pages!![0]
        val splits =
            listOf(
                ReaderPageSplit(page, 0, 500),
                ReaderPageSplit(page, 500, 500),
            )
        page.precomputedSplits = splits

        val viewerChapters = ViewerChapters(currChapter, null, null)
        val result = useCase(viewerChapters)

        val items = result.items
        // 1 prev trans + 2 splits + 1 next trans = 4 items
        assertEquals(4, items.size)
        assertTrue(items[1] is ReaderUiItem.SplitPage)
        assertTrue(items[2] is ReaderUiItem.SplitPage)
        assertTrue(result.tallSplitPages.contains(page))
    }

    @Test
    fun `invoke preserves non-tall page when precomputedSplits is empty list`() {
        val currChapter = createChapter(1L, pageCount = 1, chapterNumber = 1f)
        val page = currChapter.pages!![0]
        page.precomputedSplits = emptyList()

        val viewerChapters = ViewerChapters(currChapter, null, null)
        val result = useCase(viewerChapters, screenHeight = 1000)

        val items = result.items
        // 1 prev trans + 1 monolithic page + 1 next trans = 3 items
        assertEquals(3, items.size)
        assertTrue(items[1] is ReaderUiItem.Page)
        assertEquals(page, (items[1] as ReaderUiItem.Page).page)
        assertTrue(!result.tallSplitPages.contains(page))
    }
}
