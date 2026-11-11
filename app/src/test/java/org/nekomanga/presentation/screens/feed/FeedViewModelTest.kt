package org.nekomanga.presentation.screens.feed

import android.app.Application
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.NetworkState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.repository.FeedRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.feed.FeedHistoryGroup
import org.nekomanga.domain.feed.FeedItem
import org.nekomanga.domain.feed.FeedPage
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockApplication: Application
    private lateinit var mockPreferences: PreferencesHelper
    private lateinit var mockLibraryPreferences: LibraryPreferences
    private lateinit var mockSecurityPreferences: SecurityPreferences
    private lateinit var mockMangaDetailsPreferences: MangaDetailsPreferences
    private lateinit var mockFeedRepository: FeedRepository
    private lateinit var mockDownloadManager: DownloadManager

    private val feedViewTypeFlow = MutableStateFlow(FeedScreenType.History)
    private val downloadQueueState = MutableStateFlow<List<Download>>(emptyList())
    private val downloadNetworkState = MutableStateFlow<NetworkState>(mockk(relaxed = true))
    private val downloaderRunning = MutableStateFlow(false)

    private fun <T> mockPreference(defaultValue: T): Preference<T> {
        val pref = mockk<Preference<T>>()
        every { pref.get() } returns defaultValue
        every { pref.changes() } returns flowOf(defaultValue)
        return pref
    }

    private fun createFeedItem(mangaId: Long, title: String): FeedItem {
        return FeedItem(
            mangaId = mangaId,
            mangaTitle = title,
            date = 1000L,
            artwork = mockk(relaxed = true),
            chapters = emptyList(),
        )
    }

    @Before
    fun setup() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(testDispatcher)
        FeedViewModel.onLowMemory()

        feedViewTypeFlow.value = FeedScreenType.History

        mockApplication = mockk(relaxed = true)
        mockPreferences = mockk()
        mockLibraryPreferences = mockk()
        mockSecurityPreferences = mockk()
        mockMangaDetailsPreferences = mockk()
        mockFeedRepository = mockk()
        mockDownloadManager = mockk()

        // Feed view type preference with reactive changes flow
        val feedViewTypePref = mockk<Preference<FeedScreenType>>()
        every { feedViewTypePref.get() } answers { feedViewTypeFlow.value }
        every { feedViewTypePref.changes() } returns feedViewTypeFlow
        every { feedViewTypePref.set(any()) } answers
            {
                val newType = firstArg<FeedScreenType>()
                feedViewTypeFlow.value = newType
            }
        every { mockPreferences.feedViewType() } returns feedViewTypePref

        // Other preferences
        every { mockLibraryPreferences.outlineOnCovers() } returns mockPreference(false)
        every { mockMangaDetailsPreferences.dynamicCovers() } returns mockPreference(false)
        every { mockPreferences.feedViewOutlineCards() } returns mockPreference(false)
        every { mockSecurityPreferences.incognitoMode() } returns mockPreference(false)
        every { mockPreferences.groupChaptersUpdates() } returns mockPreference(false)
        every { mockPreferences.downloadOnlyOverUnmetered() } returns mockPreference(false)
        every { mockPreferences.swipeRefreshFeedScreen() } returns mockPreference(false)
        every { mockPreferences.historyChapterGrouping() } returns
            mockPreference(FeedHistoryGroup.Series)
        every { mockPreferences.useVividColorHeaders() } returns mockPreference(false)
        every { mockPreferences.sortFetchedTime() } returns mockPreference(false)
        every { mockPreferences.removeAfterMarkedAsRead() } returns mockPreference(false)

        // DownloadManager flows
        every { mockDownloadManager.queueState } returns downloadQueueState
        every { mockDownloadManager.isDownloaderRunning } returns downloaderRunning
        every { mockDownloadManager.networkStateFlow() } returns downloadNetworkState
        every { mockDownloadManager.statusFlow() } returns emptyFlow()
        every { mockDownloadManager.progressFlow() } returns emptyFlow()

        // FeedRepository defaults
        coEvery { mockFeedRepository.getSummaryUpdatesList() } returns Ok(emptyList())
        coEvery { mockFeedRepository.getSummaryContinueReadingList() } returns Ok(emptyList())
        coEvery { mockFeedRepository.getSummaryNewlyAddedList() } returns Ok(emptyList())
        coEvery {
            mockFeedRepository.getUpdatesPage(
                offset = any(),
                limit = any(),
                uploadsFetchSort = any(),
            )
        } returns Ok(FeedPage(hasNextPage = false, items = emptyList()))
        coEvery {
            mockFeedRepository.getHistoryPage(
                offset = any(),
                limit = any(),
                group = any(),
                seenMangaIds = any(),
            )
        } returns Ok(FeedPage(hasNextPage = false, items = emptyList()))

        Injekt.addSingleton(mockApplication)
        Injekt.addSingleton(mockPreferences)
        Injekt.addSingleton(mockLibraryPreferences)
        Injekt.addSingleton(mockSecurityPreferences)
        Injekt.addSingleton(mockMangaDetailsPreferences)
        Injekt.addSingleton(mockFeedRepository)
        Injekt.addSingleton(mockDownloadManager)
    }

    @After
    fun tearDown() {
        FeedViewModel.onLowMemory()
        Dispatchers.resetMain()
        unmockkAll()
        unmockkStatic(Dispatchers::class)
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given history page 0 when requested then seenIds is empty even if list contains items`() =
        runTest {
            val seenIdsSlot = slot<Set<Long>>()
            val feedItem = createFeedItem(1L, "Manga 1")

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 0,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = capture(seenIdsSlot),
                )
            } returns Ok(FeedPage(hasNextPage = true, items = listOf(feedItem)))

            val viewModel = FeedViewModel()
            advanceUntilIdle()

            // Verify initial load passed empty seenIds
            seenIdsSlot.captured shouldBe emptySet()
            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 1
            viewModel.historyScreenPagingState.value.historyFeedMangaList.first().mangaId shouldBe
                1L
        }

    @Test
    fun `given history loaded when switching to summary and back to history then history is preserved`() =
        runTest {
            val feedItem = createFeedItem(1L, "Manga 1")

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 0,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Ok(FeedPage(hasNextPage = true, items = listOf(feedItem)))

            val viewModel = FeedViewModel()
            advanceUntilIdle()

            // Verify history has items loaded
            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 1

            // Switch to Summary
            viewModel.switchViewType(FeedScreenType.Summary)
            advanceUntilIdle()
            viewModel.feedScreenState.value.feedScreenType shouldBe FeedScreenType.Summary

            // Switch back to History
            viewModel.switchViewType(FeedScreenType.History)
            advanceUntilIdle()
            viewModel.feedScreenState.value.feedScreenType shouldBe FeedScreenType.History

            // Assert: history list is not wiped out / shown as empty
            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 1
            viewModel.historyScreenPagingState.value.historyFeedMangaList.first().mangaId shouldBe
                1L
        }

    @Test
    fun `given page offset greater than 0 when requesting next page then seenIds contains previously loaded manga IDs`() =
        runTest {
            val item1 = createFeedItem(1L, "Manga 1")
            val item2 = createFeedItem(2L, "Manga 2")
            val seenIdsPage0 = slot<Set<Long>>()
            val seenIdsPage1 = slot<Set<Long>>()

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 0,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = capture(seenIdsPage0),
                )
            } returns Ok(FeedPage(hasNextPage = true, items = listOf(item1)))

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 15,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = capture(seenIdsPage1),
                )
            } returns Ok(FeedPage(hasNextPage = false, items = listOf(item2)))

            val viewModel = FeedViewModel()
            advanceUntilIdle()

            // First page should have empty seenIds
            seenIdsPage0.captured shouldBe emptySet()
            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 1

            // Load next page
            viewModel.loadNextPage()
            advanceUntilIdle()

            // Next page (offset = 15) must pass seenIds containing manga ID 1
            seenIdsPage1.captured shouldContainExactly setOf(1L)
            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 2
        }

    @Test
    fun `given populated history with offset zero when refreshHistoryFeed runs then items are not wiped out`() =
        runTest {
            val item = createFeedItem(1L, "Manga 1")

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 0,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Ok(FeedPage(hasNextPage = false, items = listOf(item)))

            val viewModel = FeedViewModel()
            advanceUntilIdle()

            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 1

            // Switch to Summary and back to History:
            // Since history already has items, switching back calls refreshHistoryFeed()
            viewModel.switchViewType(FeedScreenType.Summary)
            advanceUntilIdle()

            viewModel.switchViewType(FeedScreenType.History)
            advanceUntilIdle()

            // Feed must not be erased
            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 1
            viewModel.historyScreenPagingState.value.historyFeedMangaList.first().mangaId shouldBe
                1L
        }

    @Test
    fun `given partial failure during history refresh then existing history is preserved and not corrupted`() =
        runTest {
            val item1 = createFeedItem(1L, "Manga 1")
            val item2 = createFeedItem(2L, "Manga 2")

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 0,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Ok(FeedPage(hasNextPage = true, items = listOf(item1)))

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 15,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Ok(FeedPage(hasNextPage = false, items = listOf(item2)))

            val viewModel = FeedViewModel()
            advanceUntilIdle()

            viewModel.loadNextPage()
            advanceUntilIdle()

            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 2

            // Simulate failure on refreshing page at offset 15
            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 15,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Err(ResultError.Generic("Network failure"))

            // Trigger tab switch to run refreshHistoryFeed()
            viewModel.switchViewType(FeedScreenType.Summary)
            advanceUntilIdle()

            viewModel.switchViewType(FeedScreenType.History)
            advanceUntilIdle()

            // Assert: Since page 15 failed, the entire list was not corrupted with partial page 0
            // data
            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 2
        }

    @Test
    fun `given saved items and offsets when viewModel recreated then state and offsets are restored`() =
        runTest {
            val item1 = createFeedItem(1L, "Manga 1")
            val item2 = createFeedItem(2L, "Manga 2")

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 0,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Ok(FeedPage(hasNextPage = true, items = listOf(item1)))

            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 15,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Ok(FeedPage(hasNextPage = false, items = listOf(item2)))

            val viewModel1 = FeedViewModel()
            advanceUntilIdle()

            viewModel1.loadNextPage()
            advanceUntilIdle()

            viewModel1.historyScreenPagingState.value.offset shouldBe 30
            viewModel1.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 2

            // Save items on clearing
            viewModel1.saveItems()

            // Create new ViewModel instance (simulating Activity / ViewModel recreation)
            val viewModel2 = FeedViewModel()
            advanceUntilIdle()

            // Assert: Items and offset restored from cache
            viewModel2.historyScreenPagingState.value.offset shouldBe 30
            viewModel2.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 2
        }

    @Test
    fun `given 1 page loaded when refreshHistoryFeed runs then only offset 0 is queried and not offset 15`() =
        runTest {
            val item1 = createFeedItem(1L, "Manga 1")
            val capturedOffsets = mutableListOf<Int>()

            coEvery {
                mockFeedRepository.getHistoryPage(
                    searchQuery = any(),
                    offset = capture(capturedOffsets),
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } answers
                {
                    val offset = secondArg<Int>()
                    if (offset == 0) {
                        Ok(FeedPage(hasNextPage = true, items = listOf(item1)))
                    } else {
                        Ok(FeedPage(hasNextPage = false, items = emptyList()))
                    }
                }

            val viewModel = FeedViewModel()
            advanceUntilIdle()

            // Page 0 loaded, offset is 15
            viewModel.historyScreenPagingState.value.offset shouldBe 15
            viewModel.historyScreenPagingState.value.historyFeedMangaList.size shouldBe 1

            // Switch to Summary and back to History to trigger refreshHistoryFeed
            viewModel.switchViewType(FeedScreenType.Summary)
            advanceUntilIdle()

            viewModel.switchViewType(FeedScreenType.History)
            advanceUntilIdle()

            // Verify offset 0 was queried for initial load and refresh
            capturedOffsets.filter { it == 0 }.size shouldBe 2

            // Verify offset 15 was NEVER queried during refresh
            capturedOffsets shouldNotContain 15
        }

    @Test
    fun `given history refreshed when hasNextPage changes then hasMoreResults is updated`() =
        runTest {
            val item1 = createFeedItem(1L, "Manga 1")

            // Initial load has next page
            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 0,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Ok(FeedPage(hasNextPage = true, items = listOf(item1)))

            val viewModel = FeedViewModel()
            advanceUntilIdle()

            viewModel.historyScreenPagingState.value.hasMoreResults shouldBe true

            // During refresh, hasNextPage becomes false (e.g. no more history)
            coEvery {
                mockFeedRepository.getHistoryPage(
                    offset = 0,
                    limit = any(),
                    group = FeedHistoryGroup.Series,
                    seenMangaIds = any(),
                )
            } returns Ok(FeedPage(hasNextPage = false, items = listOf(item1)))

            viewModel.switchViewType(FeedScreenType.Summary)
            advanceUntilIdle()

            viewModel.switchViewType(FeedScreenType.History)
            advanceUntilIdle()

            // hasMoreResults must be updated to false
            viewModel.historyScreenPagingState.value.hasMoreResults shouldBe false
        }
}
