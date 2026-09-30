package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.reader.ReaderPreferences
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@OptIn(ExperimentalCoroutinesApi::class)
class PagerViewerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockActivity: ReaderActivity
    private lateinit var mockViewModel: ReaderViewModel
    private lateinit var mockPreferences: PreferencesHelper
    private lateinit var mockReaderPreferences: ReaderPreferences
    private lateinit var mockDownloadManager: DownloadManager

    private fun <T> mockPref(value: T): Preference<T> {
        val pref = mockk<Preference<T>>(relaxed = true)
        every { pref.get() } returns value
        every { pref.changes() } returns flowOf(value)
        return pref
    }

    private fun createChapter(id: Long, pageCount: Int = 5, lastPageRead: Int = 0): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = id.toFloat()
                this.last_page_read = lastPageRead
            }
        val readerChapter = ReaderChapter(dbChapter)
        readerChapter.requestedPage = lastPageRead
        val pages =
            (0 until pageCount).map { index ->
                ReaderPage(index = index, url = "url_$index", imageUrl = "img_$index").apply {
                    this.chapter = readerChapter
                }
            }
        readerChapter.state = ReaderChapter.State.Loaded(pages)
        return readerChapter
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        Injekt = InjektScope(DefaultRegistrar())

        mockActivity = mockk(relaxed = true)
        mockViewModel = mockk(relaxed = true)
        every { mockActivity.viewModel } returns mockViewModel

        mockPreferences = mockk(relaxed = true)
        mockReaderPreferences = mockk(relaxed = true)
        mockDownloadManager = mockk(relaxed = true)

        every { mockPreferences.showNavigationOverlayNewUser() } returns mockPref(false)

        every { mockReaderPreferences.readWithLongTap() } returns mockPref(true)
        every { mockReaderPreferences.doubleTapAnimSpeed() } returns mockPref(500)
        every { mockReaderPreferences.readWithVolumeKeys() } returns mockPref(false)
        every { mockReaderPreferences.readWithVolumeKeysInverted() } returns mockPref(false)
        every { mockReaderPreferences.alwaysShowChapterTransition() } returns mockPref(false)
        every { mockReaderPreferences.preloadPageAmount() } returns mockPref(4)

        every { mockReaderPreferences.animatedPageTransitions() } returns mockPref(false)
        every { mockReaderPreferences.fullscreen() } returns mockPref(true)
        every { mockReaderPreferences.imageScaleType() } returns mockPref(1)
        every { mockReaderPreferences.navigationModePager() } returns mockPref(0)
        every { mockReaderPreferences.pagerNavInverted() } returns
            mockPref(ViewerNavigation.TappingInvertMode.NONE)
        every { mockReaderPreferences.pagerCutoutBehavior() } returns mockPref(0)
        every { mockReaderPreferences.zoomStart() } returns mockPref(0)
        every { mockReaderPreferences.cropBorders() } returns mockPref(false)
        every { mockReaderPreferences.navigateToPan() } returns mockPref(false)
        every { mockReaderPreferences.landscapeZoom() } returns mockPref(false)
        every { mockReaderPreferences.readerTheme() } returns mockPref(0)
        every { mockReaderPreferences.invertDoublePages() } returns mockPref(false)
        every { mockReaderPreferences.doublePageGap() } returns mockPref(0)
        every { mockReaderPreferences.doublePageRotate() } returns mockPref(false)
        every { mockReaderPreferences.doublePageRotateReverse() } returns mockPref(false)
        every { mockReaderPreferences.pageLayout() } returns mockPref(0)
        every { mockReaderPreferences.automaticSplitsPage() } returns mockPref(false)

        Injekt.addSingleton(mockPreferences)
        Injekt.addSingleton(mockReaderPreferences)
        Injekt.addSingleton(mockDownloadManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `setChapters on initial load scrolls to requestedPage`() {
        val viewer = L2RPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 10, lastPageRead = 3)
        val viewerChapters = ViewerChapters(chapter1, null, null)

        viewer.setChapters(viewerChapters)

        val req = viewer.requestedPagePosition
        assertNotNull(req)
        val expectedItemIndex = viewer.controller.findPageIndex(viewer.items, chapter1.pages!![3])
        assertEquals(expectedItemIndex, req?.first)
    }

    @Test
    fun `setChapters on background preload reload does not reset user position`() {
        val viewer = L2RPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 10, lastPageRead = 0)
        val viewerChaptersInitial = ViewerChapters(chapter1, null, null)

        // Initial load
        viewer.setChapters(viewerChaptersInitial)
        assertNotNull(viewer.requestedPagePosition)

        // Clear requested position (as Compose does after consuming scroll command)
        viewer.requestedPagePosition = null
        viewer.currentPagePosition = 4 // User has read to page 4

        // Simulate background preload of adjacent chapter completing -> ReloadViewerChapters
        val chapter2 = createChapter(2L, pageCount = 8)
        val viewerChaptersReloaded = ViewerChapters(chapter1, null, chapter2)
        viewer.setChapters(viewerChaptersReloaded)

        // Assert: requestedPagePosition must NOT be re-set to 0
        assertNull(
            "requestedPagePosition should remain null on background reload to avoid jumping back to page 0",
            viewer.requestedPagePosition,
        )
    }

    @Test
    fun `setChapters when active chapter changes navigates to new chapter requestedPage`() {
        val viewer = L2RPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, null, null))
        viewer.requestedPagePosition = null

        // Navigate to chapter 2
        val chapter2 = createChapter(2L, pageCount = 10, lastPageRead = 7)
        viewer.setChapters(ViewerChapters(chapter2, chapter1, null))

        val req = viewer.requestedPagePosition
        assertNotNull(req)
        val expectedItemIndex = viewer.controller.findPageIndex(viewer.items, chapter2.pages!![7])
        assertEquals(expectedItemIndex, req?.first)
    }

    @Test
    fun `moveLeft at first page with transitions disabled triggers navigateAdjacentChapter backward`() {
        val viewer = L2RPagerViewer(mockActivity)
        viewer.config.alwaysShowChapterTransition = false

        val chapter0 = createChapter(0L, pageCount = 5)
        val chapter1 = createChapter(1L, pageCount = 5)
        val chapter2 = createChapter(2L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, chapter0, chapter2))

        // Ensure transitions are omitted when transitions are disabled and adjacent chapters exist
        assertTrue(viewer.items.none { it is ReaderUiItem.Transition })

        viewer.requestedPagePosition = null
        viewer.currentPagePosition = 0

        viewer.moveLeft()

        verify(exactly = 1) { mockViewModel.navigateAdjacentChapter(forward = false) }
    }

    @Test
    fun `moveRight at last page with transitions disabled triggers navigateAdjacentChapter forward`() {
        val viewer = L2RPagerViewer(mockActivity)
        viewer.config.alwaysShowChapterTransition = false

        val chapter0 = createChapter(0L, pageCount = 5)
        val chapter1 = createChapter(1L, pageCount = 5)
        val chapter2 = createChapter(2L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, chapter0, chapter2))

        assertTrue(viewer.items.none { it is ReaderUiItem.Transition })

        viewer.requestedPagePosition = null
        viewer.currentPagePosition = viewer.items.lastIndex

        viewer.moveRight()

        verify(exactly = 1) { mockViewModel.navigateAdjacentChapter(forward = true) }
    }

    @Test
    fun `R2LPagerViewer moveLeft at forward boundary with transitions disabled triggers navigateAdjacentChapter forward`() {
        val viewer = R2LPagerViewer(mockActivity)
        viewer.config.alwaysShowChapterTransition = false

        val chapter0 = createChapter(0L, pageCount = 5)
        val chapter1 = createChapter(1L, pageCount = 5)
        val chapter2 = createChapter(2L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, chapter0, chapter2))

        assertTrue(viewer.items.none { it is ReaderUiItem.Transition })

        viewer.requestedPagePosition = null
        viewer.currentPagePosition = 0 // In RTL, index 0 is forward boundary

        viewer.moveLeft()

        verify(exactly = 1) { mockViewModel.navigateAdjacentChapter(forward = true) }
    }

    @Test
    fun `R2LPagerViewer moveRight at backward boundary with transitions disabled triggers navigateAdjacentChapter backward`() {
        val viewer = R2LPagerViewer(mockActivity)
        viewer.config.alwaysShowChapterTransition = false

        val chapter0 = createChapter(0L, pageCount = 5)
        val chapter1 = createChapter(1L, pageCount = 5)
        val chapter2 = createChapter(2L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, chapter0, chapter2))

        assertTrue(viewer.items.none { it is ReaderUiItem.Transition })

        viewer.requestedPagePosition = null
        viewer.currentPagePosition =
            viewer.items.lastIndex // In RTL, lastIndex is backward boundary

        viewer.moveRight()

        verify(exactly = 1) { mockViewModel.navigateAdjacentChapter(forward = false) }
    }

    @Test
    fun `pendingPageMove queues move if page is not yet in items and executes upon setChapters`() {
        val viewer = L2RPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, null, null))
        viewer.requestedPagePosition = null

        // Try to move to page 4 of chapter 2 before chapter 2 is in items
        val chapter2 = createChapter(2L, pageCount = 10)
        val targetPage = chapter2.pages!![4]

        viewer.moveToPage(targetPage, false)
        assertNull(
            "Page not yet in items should not set requestedPagePosition immediately",
            viewer.requestedPagePosition,
        )

        // Now chapter 2 becomes active
        viewer.setChapters(ViewerChapters(chapter2, chapter1, null))
        assertNotNull(viewer.requestedPagePosition)
        val expectedItemIndex = viewer.controller.findPageIndex(viewer.items, targetPage)
        assertEquals(expectedItemIndex, viewer.requestedPagePosition?.first)
    }

    @Test
    fun `setChapters does not prematurely consume chapterChanged if pages are null on first invocation`() {
        val viewer = L2RPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, null, null))
        viewer.requestedPagePosition = null

        // New chapter 2 is received, but its pages are still null (loading)
        val chapter2Loading = createChapter(2L, pageCount = 0)
        chapter2Loading.state = ReaderChapter.State.Loading
        val viewerChaptersLoading = ViewerChapters(chapter2Loading, chapter1, null)
        viewer.setChapters(viewerChaptersLoading)
        assertNull(viewer.requestedPagePosition)

        // Now chapter 2 finishes loading its pages
        val chapter2Loaded = createChapter(2L, pageCount = 10, lastPageRead = 0)
        val viewerChaptersLoaded = ViewerChapters(chapter2Loaded, chapter1, null)
        viewer.setChapters(viewerChaptersLoaded)

        // It MUST detect chapterChanged and move to requestedPage (page 0)
        assertNotNull(viewer.requestedPagePosition)
        val expectedItemIndex =
            viewer.controller.findPageIndex(viewer.items, chapter2Loaded.pages!![0])
        assertEquals(expectedItemIndex, viewer.requestedPagePosition?.first)
    }

    @Test
    fun `setChapters in R2LPagerViewer navigates to new chapter requestedPage in RTL item order`() {
        val viewer = R2LPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, null, null))
        viewer.requestedPagePosition = null

        // Navigate to chapter 2 in RTL
        val chapter2 = createChapter(2L, pageCount = 10, lastPageRead = 3)
        viewer.setChapters(ViewerChapters(chapter2, chapter1, null))

        val req = viewer.requestedPagePosition
        assertNotNull(req)
        val expectedItemIndex = viewer.controller.findPageIndex(viewer.items, chapter2.pages!![3])
        assertEquals(expectedItemIndex, req?.first)
    }

    @Test
    fun `setChapters in R2LPagerViewer does not prematurely consume chapterChanged if pages are null on first invocation`() {
        val viewer = R2LPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, null, null))
        viewer.requestedPagePosition = null

        // New chapter 2 is received, but its pages are still null (loading)
        val chapter2Loading = createChapter(2L, pageCount = 0)
        chapter2Loading.state = ReaderChapter.State.Loading
        val viewerChaptersLoading = ViewerChapters(chapter2Loading, chapter1, null)
        viewer.setChapters(viewerChaptersLoading)
        assertNull(viewer.requestedPagePosition)

        // Now chapter 2 finishes loading its pages
        val chapter2Loaded = createChapter(2L, pageCount = 10, lastPageRead = 0)
        val viewerChaptersLoaded = ViewerChapters(chapter2Loaded, chapter1, null)
        viewer.setChapters(viewerChaptersLoaded)

        // It MUST detect chapterChanged and move to requestedPage (page 0)
        assertNotNull(viewer.requestedPagePosition)
        val expectedItemIndex =
            viewer.controller.findPageIndex(viewer.items, chapter2Loaded.pages!![0])
        assertEquals(expectedItemIndex, viewer.requestedPagePosition?.first)
    }

    @Test
    fun `moveToPage in R2LPagerViewer sets requestedPagePosition to page 0 when jumping back to page one`() {
        val viewer = R2LPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 10)
        viewer.setChapters(ViewerChapters(chapter1, null, null))

        // Simulate reading at page 5
        val page5 = chapter1.pages!![5]
        viewer.moveToPage(page5, false)
        viewer.requestedPagePosition = null

        // Now jump back to page 0 (page 1 on slider)
        val page0 = chapter1.pages!![0]
        viewer.moveToPage(page0, false)

        assertNotNull(viewer.requestedPagePosition)
        val expectedItemIndex = viewer.controller.findPageIndex(viewer.items, page0)
        assertEquals(expectedItemIndex, viewer.requestedPagePosition?.first)
    }

    @Test
    fun `moveToPage in L2RPagerViewer sets requestedPagePosition to page 0 when jumping back to page one`() {
        val viewer = L2RPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 10)
        viewer.setChapters(ViewerChapters(chapter1, null, null))

        // Simulate reading at page 5
        val page5 = chapter1.pages!![5]
        viewer.moveToPage(page5, false)
        viewer.requestedPagePosition = null

        // Now jump back to page 0 (page 1 on slider)
        val page0 = chapter1.pages!![0]
        viewer.moveToPage(page0, false)

        assertNotNull(viewer.requestedPagePosition)
        val expectedItemIndex = viewer.controller.findPageIndex(viewer.items, page0)
        assertEquals(expectedItemIndex, viewer.requestedPagePosition?.first)
    }

    @Test
    fun `setChapters correctly recovers and detects chapterChanged when returning to previous chapter after loading failure`() {
        val viewer = L2RPagerViewer(mockActivity)
        val chapter1 = createChapter(1L, pageCount = 5)
        viewer.setChapters(ViewerChapters(chapter1, null, null))
        viewer.requestedPagePosition = null

        // User navigates to chapter 2, but it has no pages yet (loading)
        val chapter2Loading = createChapter(2L, pageCount = 0)
        chapter2Loading.state = ReaderChapter.State.Loading
        viewer.setChapters(ViewerChapters(chapter2Loading, chapter1, null))
        assertNull(viewer.requestedPagePosition)

        // Loading chapter 2 fails or user cancels and navigates back to chapter 1
        viewer.setChapters(ViewerChapters(chapter1, null, null))

        // It MUST detect chapter change back to chapter 1 and move to requestedPage
        assertNotNull(viewer.requestedPagePosition)
        val expectedItemIndex = viewer.controller.findPageIndex(viewer.items, chapter1.pages!![0])
        assertEquals(expectedItemIndex, viewer.requestedPagePosition?.first)
    }
}
