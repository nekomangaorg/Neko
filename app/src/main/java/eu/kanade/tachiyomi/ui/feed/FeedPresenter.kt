package eu.kanade.tachiyomi.ui.feed

import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.download.DownloadItem
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedPresenter(
    val preferences: PreferencesHelper = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    val securityPreferences: SecurityPreferences = Injekt.get(),
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get(),
    private val feedRepository: FeedRepository = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
) : BaseCoroutinePresenter<FeedController>() {

    private val _feedScreenState =
        MutableStateFlow(
            FeedScreenState(
                feedScreenType = preferences.feedViewType().get(),
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                outlineCards = preferences.feedViewOutlineCards().get(),
                incognitoMode = securityPreferences.incognitoMode().get(),
                groupUpdateChapters = preferences.groupChaptersUpdates().get(),
                downloadOnlyOnWifi = preferences.downloadOnlyOverWifi().get(),
                swipeRefreshEnabled = preferences.swipeRefreshFeedScreen().get(),
            )
        )

    private val _historyScreenPagingState =
        MutableStateFlow(
            HistoryScreenPagingState(historyGrouping = preferences.historyChapterGrouping().get())
        )

    private val _updatesScreenPagingState = MutableStateFlow(UpdatesScreenPagingState())

    private val _summaryScreenPagingState = MutableStateFlow(SummaryScreenPagingState())

    val feedScreenState: StateFlow<FeedScreenState> = _feedScreenState.asStateFlow()

    val updatesScreenPagingState: StateFlow<UpdatesScreenPagingState> =
        _updatesScreenPagingState.asStateFlow()

    val historyScreenPagingState: StateFlow<HistoryScreenPagingState> =
        _historyScreenPagingState.asStateFlow()

    val summaryScreenPagingState: StateFlow<SummaryScreenPagingState> =
        _summaryScreenPagingState.asStateFlow()

    private var searchJob: Job? = null

    private val updatesPaginator =
        DefaultPaginator(
            initialKey = _updatesScreenPagingState.value.offset,
            onLoadUpdated = {
                _updatesScreenPagingState.update { state -> state.copy(pageLoading = it) }
            },
            onRequest = {
                feedRepository.getUpdatesPage(
                    offset = _updatesScreenPagingState.value.offset,
                    limit = UPDATES_ENDLESS_LIMIT,
                    uploadsFetchSort = _updatesScreenPagingState.value.updatesSortedByFetch,
                )
            },
            getNextKey = { _updatesScreenPagingState.value.offset + UPDATES_ENDLESS_LIMIT },
            onError = {
                _updatesScreenPagingState.update { state -> state.copy(pageLoading = false) }
            },
            onSuccess = { hasNextPage, items, newKey ->
                _updatesScreenPagingState.update { state ->
                    state.copy(
                        offset = newKey,
                        pageLoading = false,
                        hasMoreResults = hasNextPage,
                        // empty the list before adding the first page in case it was loaded from
                        // cache
                        updatesFeedMangaList =
                            if (newKey == 1) {
                                items.toPersistentList()
                            } else {
                                (state.updatesFeedMangaList + items).toPersistentList()
                            },
                    )
                }
            },
        )

    private val historyPaginator =
        DefaultPaginator(
            initialKey = _historyScreenPagingState.value.offset,
            onLoadUpdated = {
                _historyScreenPagingState.update { state -> state.copy(pageLoading = it) }
            },
            onRequest = {
                feedRepository.getHistoryPage(
                    offset = _historyScreenPagingState.value.offset,
                    group = _historyScreenPagingState.value.historyGrouping,
                )
            },
            getNextKey = { _historyScreenPagingState.value.offset + HISTORY_ENDLESS_LIMIT },
            onError = {
                _historyScreenPagingState.update { state -> state.copy(pageLoading = false) }
            },
            onSuccess = { hasNextPage, items, newKey ->
                // empty the list before adding teh first
                if (newKey == 1) {
                    _historyScreenPagingState.update { state ->
                        state.copy(historyFeedMangaList = persistentListOf())
                    }
                }
                _historyScreenPagingState.update { state ->
                    state.copy(
                        offset = newKey,
                        pageLoading = false,
                        hasMoreResults = hasNextPage,
                        // empty the list before adding the first page in case it was loaded from
                        // cache
                        historyFeedMangaList =
                            if (newKey == 1) {
                                items.toPersistentList()
                            } else {
                                (state.historyFeedMangaList + items).toPersistentList()
                            },
                    )
                }
            },
        )

    override fun onDestroy() {
        super.onDestroy()
        saveItems()
    }

    fun saveItems() {
        lastUpdatesFeedMangaList = _updatesScreenPagingState.value.updatesFeedMangaList
        lastHistoryFeedMangaList = _historyScreenPagingState.value.historyFeedMangaList
        lastSummaryUpdatesFeedMangaList = _summaryScreenPagingState.value.updatesFeedMangaList
        lastContinueReadingList = _summaryScreenPagingState.value.continueReadingList
        lastSummaryNewlyAddedFeedMangaList = _summaryScreenPagingState.value.newlyAddedFeedMangaList
    }

    override fun onPause() {
        super.onPause()
        saveItems()
    }

    override fun onCreate() {
        super.onCreate()

        populateItems()
        observeDownloads()

        LibraryUpdateJob.updateFlow.onEach(::onUpdateManga).launchIn(presenterScope)
        presenterScope.launchIO {
            val downloads =
                downloadManager.queueState.value
                    .map { download ->
                        DownloadItem(
                            mangaItem = download.mangaItem,
                            chapterItem =
                                download.chapterItem.toChapterItem(
                                    download.status,
                                    download.progress,
                                ),
                        )
                    }
                    .toPersistentList()

            _feedScreenState.update { it.copy(downloads = downloads) }
        }

        if (_feedScreenState.value.firstLoad) {
            _feedScreenState.update { state -> state.copy(firstLoad = false) }
            presenterScope.launchIO { loadNextPage() }
        }

        presenterScope.launch {
            _feedScreenState.update {
                it.copy(sideNavMode = SideNavMode.findByPrefValue(preferences.sideNavMode().get()))
            }
        }

        presenterScope.launchIO {
            preferences.useVividColorHeaders().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _feedScreenState.update { it.copy(useVividColorHeaders = enabled) }
            }
        }

        presenterScope.launch {
            securityPreferences.incognitoMode().changes().collectLatest {
                _feedScreenState.update { state -> state.copy(incognitoMode = it) }
            }
        }

        presenterScope.launch {
            preferences.downloadOnlyOverWifi().changes().collectLatest {
                _feedScreenState.update { state -> state.copy(downloadOnlyOnWifi = it) }
            }
        }

        presenterScope.launch {
            preferences.feedViewOutlineCards().changes().collectLatest {
                _feedScreenState.update { state -> state.copy(outlineCards = it) }
            }
        }

        presenterScope.launch {
            libraryPreferences.outlineOnCovers().changes().collectLatest {
                _feedScreenState.update { state -> state.copy(outlineCovers = it) }
            }
        }

        presenterScope.launch {
            preferences.groupChaptersUpdates().changes().collectLatest {
                _feedScreenState.update { state -> state.copy(groupUpdateChapters = it) }
            }
        }

        presenterScope.launch {
            preferences.historyChapterGrouping().changes().collectLatest {
                _historyScreenPagingState.update { state ->
                    state.copy(
                        historyGrouping = it,
                        offset = 0,
                        historyFeedMangaList = persistentListOf(),
                    )
                }
                historyPaginator.reset()
                loadNextPage()
            }
        }

        presenterScope.launch {
            preferences.feedViewType().changes().collectLatest { type ->
                when (type) {
                    FeedScreenType.Summary -> Unit
                    FeedScreenType.History -> {
                        _historyScreenPagingState.update { state ->
                            state.copy(
                                offset = 0,
                                searchHistoryFeedMangaList = persistentListOf(),
                                searchQuery = "",
                            )
                        }
                        historyPaginator.reset()
                    }
                    FeedScreenType.Updates -> {
                        _updatesScreenPagingState.update { state ->
                            state.copy(
                                offset = 0,
                                searchUpdatesFeedMangaList = persistentListOf(),
                                searchQuery = "",
                            )
                        }
                        updatesPaginator.reset()
                        loadNextPage()
                    }
                }

                _feedScreenState.update { state -> state.copy(feedScreenType = type) }
                loadNextPage()
            }
        }

        presenterScope.launch {
            preferences.swipeRefreshFeedScreen().changes().collectLatest {
                _feedScreenState.update { state -> state.copy(swipeRefreshEnabled = it) }
            }
        }

        presenterScope.launch {
            preferences.sortFetchedTime().changes().collectLatest {
                _updatesScreenPagingState.update { state ->
                    state.copy(
                        updatesSortedByFetch = it,
                        offset = 0,
                        updatesFeedMangaList = persistentListOf(),
                        searchQuery = "",
                        searchUpdatesFeedMangaList = persistentListOf(),
                    )
                }
                updatesPaginator.reset()
                loadNextPage()
            }
        }
    }

    fun loadNextPage() {
        presenterScope.launchIO {
            when (_feedScreenState.value.feedScreenType) {
                FeedScreenType.Summary -> loadSummaryPage()
                FeedScreenType.History -> historyPaginator.loadNextItems()
                FeedScreenType.Updates -> updatesPaginator.loadNextItems()
            }
        }
    }

    fun switchViewType(feedScreenType: FeedScreenType) {
        presenterScope.launchIO { preferences.feedViewType().set(feedScreenType) }
    }

    fun toggleSwipeRefresh() {
        presenterScope.launchIO { preferences.swipeRefreshFeedScreen().toggle() }
    }

    fun toggleChapterRead(chapterItem: ChapterItem) {
        presenterScope.launchIO {
            val updatedChapterItem = feedRepository.toggleChapterRead(chapterItem)
            updateReadOnFeed(updatedChapterItem)
            if (updatedChapterItem.chapter.read) {
                if (
                    preferences.removeAfterMarkedAsRead().get() &&
                        updatedChapterItem.chapter.canDeleteChapter()
                ) {
                    feedRepository.deleteChapter(updatedChapterItem)
                }
            }
        }
    }

    fun togglerGroupUpdateChapters() {
        presenterScope.launchIO { preferences.groupChaptersUpdates().toggle() }
    }

    fun toggleGroupHistoryType(historyGrouping: FeedHistoryGroup) {
        presenterScope.launchIO { preferences.historyChapterGrouping().set(historyGrouping) }
    }

    fun toggleOutlineCards() {
        presenterScope.launchIO { preferences.feedViewOutlineCards().toggle() }
    }

    fun toggleOutlineCovers() {
        presenterScope.launchIO { libraryPreferences.outlineOnCovers().toggle() }
    }

    fun toggleIncognitoMode() {
        presenterScope.launchIO { securityPreferences.incognitoMode().toggle() }
    }

    fun toggleShowingDownloads() {
        presenterScope.launchIO {
            _feedScreenState.update { it.copy(showingDownloads = !it.showingDownloads) }
        }
    }

    fun toggleDownloader() {
        presenterScope.launch {
            when (downloadManager.isRunning) {
                true -> downloadManager.pauseDownloads()
                false -> downloadManager.startDownloads()
            }
        }
    }

    fun clearDownloadQueue() {
        presenterScope.launch { downloadManager.clearQueue() }
    }

    fun toggleUploadsSortOrder() {
        presenterScope.launchIO { preferences.sortFetchedTime().toggle() }
    }

    fun deleteAllHistoryForAllManga() {
        presenterScope.launchIO {
            _historyScreenPagingState.update {
                it.copy(
                    offset = 0,
                    historyFeedMangaList = persistentListOf(),
                    searchHistoryFeedMangaList = persistentListOf(),
                    searchQuery = "",
                )
            }
            feedRepository.deleteAllHistory()
            historyPaginator.reset()
            historyPaginator.loadNextItems()
            loadSummaryPage()
        }
    }

    fun deleteAllHistory(feedManga: FeedManga) {
        presenterScope.launchIO {
            feedRepository.deleteAllHistoryForManga(feedManga.mangaId)
            _historyScreenPagingState.update {
                it.copy(
                    historyFeedMangaList =
                        it.historyFeedMangaList
                            .filter { fm -> fm.mangaId != feedManga.mangaId }
                            .toPersistentList()
                )
            }
            loadSummaryPage()
        }
    }

    fun deleteHistory(feedManga: FeedManga, simpleChapter: SimpleChapter) {
        presenterScope.launchIO {
            feedRepository.deleteHistoryForChapter(simpleChapter.url)
            val index =
                _historyScreenPagingState.value.historyFeedMangaList.indexOfFirst {
                    it.mangaId == feedManga.mangaId
                }
            val mutableFeedManga =
                _historyScreenPagingState.value.historyFeedMangaList.toMutableList()

            if (feedManga.chapters.size == 1) {
                mutableFeedManga.removeAt(index)
            } else if (_historyScreenPagingState.value.historyGrouping == FeedHistoryGroup.Series) {
                val newFeedManga = feedRepository.getUpdatedFeedMangaForHistoryBySeries(feedManga)
                mutableFeedManga[index] = newFeedManga
            } else {
                val newFeedManga = _historyScreenPagingState.value.historyFeedMangaList[index]
                mutableFeedManga[index] =
                    newFeedManga.copy(
                        chapters =
                            newFeedManga.chapters
                                .filter { it.chapter.url != simpleChapter.url }
                                .toPersistentList()
                    )
            }
            _historyScreenPagingState.update {
                it.copy(historyFeedMangaList = mutableFeedManga.toPersistentList())
            }
        }
        loadSummaryPage()
    }

    fun loadSummaryPage() {
        presenterScope.launchIO {
            feedRepository.getSummaryUpdatesList().onSuccess { list ->
                _summaryScreenPagingState.update { state ->
                    state.copy(
                        updatesFeedMangaList = list.toPersistentList(),
                        updatingUpdates = false,
                    )
                }
            }
        }

        presenterScope.launchIO {
            feedRepository.getSummaryContinueReadingList().onSuccess { list ->
                _summaryScreenPagingState.update { state ->
                    state.copy(
                        continueReadingList = list.toPersistentList(),
                        updatingContinueReading = false,
                    )
                }
            }
        }

        presenterScope.launchIO {
            feedRepository.getSummaryNewlyAddedList().onSuccess { list ->
                _summaryScreenPagingState.update { state ->
                    state.copy(
                        newlyAddedFeedMangaList = list.toPersistentList(),
                        updatingNewlyAdded = false,
                    )
                }
            }
        }
    }

    fun search(searchQuery: String?) {
        searchJob?.cancel()
        searchJob =
            presenterScope.launchIO {
                if (searchQuery.isNullOrBlank()) {
                    when (_feedScreenState.value.feedScreenType) {
                        FeedScreenType.Summary -> TODO()
                        FeedScreenType.History ->
                            _historyScreenPagingState.update {
                                it.copy(
                                    searchHistoryFeedMangaList = persistentListOf(),
                                    searchQuery = "",
                                )
                            }
                        FeedScreenType.Updates ->
                            _updatesScreenPagingState.update {
                                it.copy(
                                    searchUpdatesFeedMangaList = persistentListOf(),
                                    searchQuery = "",
                                )
                            }
                    }
                } else {
                    when (_feedScreenState.value.feedScreenType) {
                        FeedScreenType.Summary -> Unit
                        FeedScreenType.History -> {
                            feedRepository
                                .getHistoryPage(
                                    searchQuery,
                                    offset = 0,
                                    limit = 100,
                                    _historyScreenPagingState.value.historyGrouping,
                                )
                                .onSuccess { results ->
                                    _historyScreenPagingState.update {
                                        it.copy(
                                            searchQuery = searchQuery,
                                            searchHistoryFeedMangaList =
                                                (results.second).toPersistentList(),
                                        )
                                    }
                                }
                        }
                        FeedScreenType.Updates -> {
                            feedRepository
                                .getUpdatesPage(
                                    searchQuery,
                                    offset = 0,
                                    limit = 100,
                                    _updatesScreenPagingState.value.updatesSortedByFetch,
                                )
                                .onSuccess { results ->
                                    _updatesScreenPagingState.update {
                                        it.copy(
                                            searchQuery = searchQuery,
                                            searchUpdatesFeedMangaList =
                                                (results.second).toPersistentList(),
                                        )
                                    }
                                }
                        }
                    }
                }
            }
    }

    fun downloadChapter(
        chapterItem: ChapterItem,
        feedManga: FeedManga,
        downloadAction: MangaConstants.DownloadAction,
    ) {
        presenterScope.launchIO {
            feedRepository.downloadChapter(feedManga, chapterItem, downloadAction)
            if (
                downloadAction == MangaConstants.DownloadAction.Cancel ||
                    downloadAction == MangaConstants.DownloadAction.Remove
            ) {
                updateDownloadOnFeed(chapterItem.chapter.id, feedManga.mangaId, null)
            }
        }
    }

    fun removeDownload(download: DownloadItem) {
        presenterScope.launchIO { downloadManager.deletePendingDownloadsItems(listOf(download)) }
    }

    fun moveDownload(downloadItem: DownloadItem, direction: MoveDownloadDirection) {
        presenterScope.launchIO {
            val index =
                downloadManager.queueState.value.indexOfFirst { download ->
                    download.chapterItem.id == downloadItem.chapterItem.chapter.id
                }
            val mutableDownloads = downloadManager.queueState.value.toMutableList()
            val downloadList = listOf(mutableDownloads.removeAt(index))
            val list =
                when (direction) {
                    MoveDownloadDirection.Top -> downloadList + mutableDownloads
                    MoveDownloadDirection.Bottom -> mutableDownloads + downloadList
                }

            downloadManager.reorderQueue(list)
        }
    }

    fun moveDownloadSeries(downloadItem: DownloadItem, direction: MoveDownloadDirection) {
        presenterScope.launchIO {
            val partitionedPair =
                downloadManager.queueState.value.partition { download ->
                    download.mangaItem.id == downloadItem.mangaItem.id
                }

            val list =
                when (direction) {
                    MoveDownloadDirection.Top -> partitionedPair.first + partitionedPair.second
                    MoveDownloadDirection.Bottom -> partitionedPair.second + partitionedPair.first
                }
            downloadManager.reorderQueue(list)
        }
    }

    fun cancelDownloadSeries(downloadItem: DownloadItem) {
        presenterScope.launchIO {
            val downloadsToDelete =
                downloadManager.queueState.value.filter { download ->
                    download.mangaItem.id == downloadItem.mangaItem.id
                }
            downloadManager.deletePendingDownloads(downloadsToDelete)
        }
    }

    fun toggleDownloadOnlyOnWifi() {
        presenterScope.launchIO { preferences.downloadOnlyOverWifi().toggle() }
    }

    /**
     * Finds the manga in the given list, finds the matching chapters and updates the chapter and
     * the list. Returning the updated list or false if the chapter didnt exist
     */
    /**
     * Finds the manga in the given list, finds the matching chapter, and updates its download
     * status. Returns a Pair containing a success flag and the updated list.
     */
    private fun updateChapterDownloadForManga(
        chapterId: Long,
        mangaId: Long,
        download: Download?,
        feedManga: List<FeedManga>,
    ): Pair<Boolean, List<FeedManga>> {
        val mangaIndex = feedManga.indexOfFirst { it.mangaId == mangaId }

        if (mangaIndex == -1) {
            return false to feedManga
        }

        val mangaToUpdate = feedManga[mangaIndex]

        val chapterIndex = mangaToUpdate.chapters.indexOfFirst { it.chapter.id == chapterId }

        if (chapterIndex == -1) {
            return false to feedManga
        }

        val updatedChapters =
            mangaToUpdate.chapters.mapIndexed { index, feedItem ->
                if (index == chapterIndex) {
                    feedItem.copy(
                        downloadState = download?.status ?: Download.State.NOT_DOWNLOADED,
                        downloadProgress = download?.progress ?: 0,
                    )
                } else {
                    feedItem
                }
            }

        val updatedFeedManga =
            feedManga.toMutableList().apply {
                this[mangaIndex] = mangaToUpdate.copy(chapters = updatedChapters.toPersistentList())
            }

        return true to updatedFeedManga
    }

    private fun updateChapterReadStatus(
        updatedChapterItem: ChapterItem,
        feedManga: List<FeedManga>,
    ): Pair<Boolean, List<FeedManga>> {
        var wasUpdated: Boolean = false
        val updatedFeedManga =
            feedManga.mapIndexed { index, manga ->
                if (
                    manga.mangaId == updatedChapterItem.chapter.mangaId &&
                        manga.chapters.firstOrNull()?.chapter?.id == updatedChapterItem.chapter.id
                ) {
                    wasUpdated = true
                    manga.copy(chapters = persistentListOf(updatedChapterItem))
                } else {
                    manga
                }
            }
        return wasUpdated to updatedFeedManga
    }

    private fun onUpdateManga(mangaId: Long?) {
        presenterScope.launchIO {
            when (mangaId) {
                null -> {
                    _feedScreenState.update { it.copy(isRefreshing = false) }
                }
                LibraryUpdateJob.STARTING_UPDATE_SOURCE -> {
                    _feedScreenState.update { it.copy(isRefreshing = true) }
                }
                else -> {
                    updatesPaginator.reset()
                    updatesPaginator.loadNextItems()
                }
            }
        }
    }

    private fun refreshUpdatesFeed() {
        presenterScope.launchIO {
            if (
                _updatesScreenPagingState.value.updatesFeedMangaList.isEmpty() &&
                    _feedScreenState.value.firstLoad
            )
                return@launchIO

            val currentOffset = _updatesScreenPagingState.value.offset
            var mutableFeedManga = mutableListOf<FeedManga>()
            val limit = UPDATES_ENDLESS_LIMIT
            for (i in 0..currentOffset step limit) {
                feedRepository
                    .getUpdatesPage(
                        offset = i,
                        limit = limit,
                        uploadsFetchSort = _updatesScreenPagingState.value.updatesSortedByFetch,
                    )
                    .onSuccess { results ->
                        mutableFeedManga = (mutableFeedManga + results.second).toMutableList()
                    }
            }
            _updatesScreenPagingState.update { state ->
                state.copy(updatesFeedMangaList = mutableFeedManga.toPersistentList())
            }
        }
    }

    private fun refreshHistoryFeed() {
        presenterScope.launchIO {
            if (
                _historyScreenPagingState.value.historyFeedMangaList.isEmpty() &&
                    _feedScreenState.value.firstLoad
            )
                return@launchIO

            val currentOffset = _historyScreenPagingState.value.offset
            var mutableFeedManga = mutableListOf<FeedManga>()
            val limit = HISTORY_ENDLESS_LIMIT
            for (i in 0..currentOffset step limit) {
                feedRepository
                    .getHistoryPage(
                        offset = i,
                        group = _historyScreenPagingState.value.historyGrouping,
                    )
                    .onSuccess { results ->
                        mutableFeedManga = (mutableFeedManga + results.second).toMutableList()
                    }
            }
            _historyScreenPagingState.update { state ->
                state.copy(historyFeedMangaList = mutableFeedManga.toPersistentList())
            }
        }
    }

    fun updateMangaForChanges() {
        if (!_feedScreenState.value.firstLoad) {
            presenterScope.launchIO {
                if (_updatesScreenPagingState.value.searchUpdatesFeedMangaList.isNotEmpty()) {
                    launch {
                        getUpdatedSearchFeedMangaList(FeedScreenType.Updates) { feedMangaList ->
                            _updatesScreenPagingState.update { state ->
                                state.copy(searchUpdatesFeedMangaList = feedMangaList)
                            }
                        }
                    }
                }
                if (_historyScreenPagingState.value.searchHistoryFeedMangaList.isNotEmpty()) {
                    launch {
                        getUpdatedSearchFeedMangaList(FeedScreenType.History) { feedMangaList ->
                            _historyScreenPagingState.update { state ->
                                state.copy(searchHistoryFeedMangaList = feedMangaList)
                            }
                        }
                    }
                }
            }
            when (feedScreenState.value.feedScreenType) {
                FeedScreenType.Summary -> loadSummaryPage()
                FeedScreenType.History -> refreshHistoryFeed()
                FeedScreenType.Updates -> refreshUpdatesFeed()
            }
        }
    }

    private suspend fun getUpdatedSearchFeedMangaList(
        feedScreenType: FeedScreenType,
        update: (PersistentList<FeedManga>) -> Unit,
    ) {
        if (feedScreenType == FeedScreenType.Updates) {
                feedRepository.getUpdatesPage(
                    searchQuery = _updatesScreenPagingState.value.searchQuery,
                    offset = 0,
                    limit = 100,
                    uploadsFetchSort = _updatesScreenPagingState.value.updatesSortedByFetch,
                )
            } else {
                feedRepository.getHistoryPage(
                    searchQuery = _historyScreenPagingState.value.searchQuery,
                    offset = 0,
                    group = _historyScreenPagingState.value.historyGrouping,
                )
            }
            .onSuccess { results -> update(results.second.toPersistentList()) }
    }

    private fun updateReadOnFeed(chapterItem: ChapterItem) {

        presenterScope.launchIO {
            val (searchHistoryUpdated, searchHistoryFeedMangaList) =
                updateChapterReadStatus(
                    chapterItem,
                    _historyScreenPagingState.value.searchHistoryFeedMangaList.map { it }.toList(),
                )
            if (searchHistoryUpdated) {
                _historyScreenPagingState.update {
                    it.copy(
                        searchHistoryFeedMangaList = searchHistoryFeedMangaList.toPersistentList()
                    )
                }
            }
        }
        presenterScope.launchIO {
            val (searchUpdatesUpdated, searchUpdatesFeedMangaList) =
                updateChapterReadStatus(
                    chapterItem,
                    _updatesScreenPagingState.value.searchUpdatesFeedMangaList.map { it }.toList(),
                )
            if (searchUpdatesUpdated) {
                _updatesScreenPagingState.update {
                    it.copy(
                        searchUpdatesFeedMangaList = searchUpdatesFeedMangaList.toPersistentList()
                    )
                }
            }
        }

        presenterScope.launchIO {
            val (historyFeedUpdated, historyFeedMangaList) =
                updateChapterReadStatus(
                    chapterItem,
                    _historyScreenPagingState.value.historyFeedMangaList.map { it }.toList(),
                )
            if (historyFeedUpdated) {
                _historyScreenPagingState.update {
                    it.copy(historyFeedMangaList = historyFeedMangaList.toPersistentList())
                }
            }
        }

        presenterScope.launchIO {
            val (updatesFeedUpdated, updatesFeedMangaList) =
                updateChapterReadStatus(
                    chapterItem,
                    _updatesScreenPagingState.value.updatesFeedMangaList.map { it }.toList(),
                )
            if (updatesFeedUpdated) {
                _updatesScreenPagingState.update {
                    it.copy(updatesFeedMangaList = updatesFeedMangaList.toPersistentList())
                }
            }
        }
        presenterScope.launchIO {
            val (updatesFeedUpdated, updatesFeedMangaList) =
                updateChapterReadStatus(
                    chapterItem,
                    _summaryScreenPagingState.value.updatesFeedMangaList.map { it }.toList(),
                )
            if (updatesFeedUpdated) {
                _summaryScreenPagingState.update {
                    it.copy(updatesFeedMangaList = updatesFeedMangaList.toPersistentList())
                }
            }
            val (newlyAddedFeedUpdated, newlyAddedFeedMangaList) =
                updateChapterReadStatus(
                    chapterItem,
                    _summaryScreenPagingState.value.newlyAddedFeedMangaList.map { it }.toList(),
                )
            if (newlyAddedFeedUpdated) {
                _summaryScreenPagingState.update {
                    it.copy(newlyAddedFeedMangaList = newlyAddedFeedMangaList.toPersistentList())
                }
            }

            val (continueReadingFeedUpdated, continueReadingList) =
                updateChapterReadStatus(
                    chapterItem,
                    _summaryScreenPagingState.value.continueReadingList.map { it }.toList(),
                )
            if (continueReadingFeedUpdated) {
                _summaryScreenPagingState.update {
                    it.copy(continueReadingList = continueReadingList.toPersistentList())
                }
            }

            if (updatesFeedUpdated || newlyAddedFeedUpdated || continueReadingFeedUpdated) {
                loadSummaryPage()
            }
        }
    }

    private fun updateDownloadOnFeed(chapterId: Long, mangaId: Long, download: Download?) {

        presenterScope.launchIO {
            val (searchHistoryFeedUpdated, searchHistoryFeedMangaList) =
                updateChapterDownloadForManga(
                    chapterId,
                    mangaId,
                    download,
                    _historyScreenPagingState.value.searchHistoryFeedMangaList.map { it }.toList(),
                )
            if (searchHistoryFeedUpdated) {
                _historyScreenPagingState.update {
                    it.copy(
                        searchHistoryFeedMangaList = searchHistoryFeedMangaList.toPersistentList()
                    )
                }
            }
        }
        presenterScope.launchIO {
            val (searchUpdatesFeedUpdated, searchUpdatesFeedMangaList) =
                updateChapterDownloadForManga(
                    chapterId,
                    mangaId,
                    download,
                    _updatesScreenPagingState.value.searchUpdatesFeedMangaList.map { it }.toList(),
                )
            if (searchUpdatesFeedUpdated) {
                _updatesScreenPagingState.update {
                    it.copy(
                        searchUpdatesFeedMangaList = searchUpdatesFeedMangaList.toPersistentList()
                    )
                }
            }
        }

        presenterScope.launchIO {
            val (historyFeedUpdated, historyFeedMangaList) =
                updateChapterDownloadForManga(
                    chapterId,
                    mangaId,
                    download,
                    _historyScreenPagingState.value.historyFeedMangaList.map { it }.toList(),
                )
            if (historyFeedUpdated) {
                _historyScreenPagingState.update {
                    it.copy(historyFeedMangaList = historyFeedMangaList.toPersistentList())
                }
            }
        }

        presenterScope.launchIO {
            val (updatesFeedUpdated, updatesFeedMangaList) =
                updateChapterDownloadForManga(
                    chapterId,
                    mangaId,
                    download,
                    _updatesScreenPagingState.value.updatesFeedMangaList.map { it }.toList(),
                )
            if (updatesFeedUpdated) {
                _updatesScreenPagingState.update {
                    it.copy(updatesFeedMangaList = updatesFeedMangaList.toPersistentList())
                }
            }
        }

        presenterScope.launchIO {
            val (summaryUpdatesFeedUpdated, summaryUpdatesFeedMangaList) =
                updateChapterDownloadForManga(
                    chapterId,
                    mangaId,
                    download,
                    _summaryScreenPagingState.value.updatesFeedMangaList.map { it }.toList(),
                )
            if (summaryUpdatesFeedUpdated) {
                _summaryScreenPagingState.update {
                    it.copy(updatesFeedMangaList = summaryUpdatesFeedMangaList.toPersistentList())
                }
            }
        }

        presenterScope.launchIO {
            val (newlyAddedFeedUpdated, newlyAddedFeedMangaList) =
                updateChapterDownloadForManga(
                    chapterId,
                    mangaId,
                    download,
                    _summaryScreenPagingState.value.newlyAddedFeedMangaList.map { it }.toList(),
                )
            if (newlyAddedFeedUpdated) {
                _summaryScreenPagingState.update {
                    it.copy(newlyAddedFeedMangaList = newlyAddedFeedMangaList.toPersistentList())
                }
            }
        }
    }

    private fun updateDownloadQueue(download: Download) {
        val mutableList = _feedScreenState.value.downloads.toMutableList()
        val indexOfDownload =
            mutableList.indexOfFirst { it.chapterItem.chapter.id == download.chapterItem.id }
        if (indexOfDownload >= 0) {
            if (download.status == Download.State.DOWNLOADED) {
                mutableList.removeAt(indexOfDownload)
            } else {
                mutableList[indexOfDownload] =
                    mutableList[indexOfDownload].copy(
                        chapterItem =
                            download.chapterItem.toChapterItem(download.status, download.progress)
                    )
            }

            _feedScreenState.update { it.copy(downloads = mutableList.toPersistentList()) }
        }
    }

    private fun observeDownloads() {

        pausablePresenterScope.launchIO {
            downloadManager.queueState.debounce(100).collectLatest { queueDownloads ->
                val downloads =
                    queueDownloads
                        .map { download ->
                            DownloadItem(
                                mangaItem = download.mangaItem,
                                chapterItem =
                                    download.chapterItem.toChapterItem(
                                        download.status,
                                        download.progress,
                                    ),
                            )
                        }
                        .toPersistentList()

                _feedScreenState.update { it.copy(downloads = downloads) }
            }
        }

        pausablePresenterScope.launchIO {
            combine(
                    preferences.downloadOnlyOverWifi().changes(),
                    downloadManager.isDownloaderRunning,
                    downloadManager.networkStateFlow(),
                ) { downloadOnlyOverWifi, downloadRunning, networkStateFlow ->
                    Triple(downloadOnlyOverWifi, downloadRunning, networkStateFlow)
                }
                .collectLatest { results ->
                    val result =
                        if (!results.third.isWifi && results.first) {
                            DownloaderStatus.NetworkPaused
                        } else if (results.second) {
                            DownloaderStatus.Running
                        } else {
                            DownloaderStatus.Paused
                        }
                    _feedScreenState.update { it.copy(downloaderStatus = result) }
                }
        }

        pausablePresenterScope.launchIO {
            downloadManager
                .statusFlow()
                .catch { error -> TimberKt.e(error) }
                .collect { download ->
                    updateDownloadOnFeed(download.chapterItem.id, download.mangaItem.id, download)
                    updateDownloadQueue(download)
                }
        }

        pausablePresenterScope.launchIO {
            downloadManager
                .progressFlow()
                .catch { error -> TimberKt.e(error) }
                .collect { download ->
                    updateDownloadOnFeed(download.chapterItem.id, download.mangaItem.id, download)
                    updateDownloadQueue(download)
                }
        }
    }

    fun populateItems() {
        presenterScope.launchIO {
            _updatesScreenPagingState.update {
                it.copy(updatesFeedMangaList = lastUpdatesFeedMangaList ?: it.updatesFeedMangaList)
            }
            _historyScreenPagingState.update {
                it.copy(historyFeedMangaList = lastHistoryFeedMangaList ?: it.historyFeedMangaList)
            }
            _summaryScreenPagingState.update {
                it.copy(
                    updatesFeedMangaList =
                        lastSummaryUpdatesFeedMangaList ?: it.updatesFeedMangaList,
                    continueReadingList = lastContinueReadingList ?: it.continueReadingList,
                    newlyAddedFeedMangaList =
                        lastSummaryNewlyAddedFeedMangaList ?: it.newlyAddedFeedMangaList,
                )
            }
            lastUpdatesFeedMangaList = null
            lastHistoryFeedMangaList = null
            lastSummaryUpdatesFeedMangaList = null
            lastContinueReadingList = null
            lastSummaryNewlyAddedFeedMangaList = null
        }
        presenterScope.launchIO { loadSummaryPage() }
        presenterScope.launchIO { refreshUpdatesFeed() }
        presenterScope.launchIO { refreshHistoryFeed() }
    }

    override fun onResume() {
        super.onResume()
        populateItems()
        observeDownloads()
    }

    companion object {

        private var lastUpdatesFeedMangaList: PersistentList<FeedManga>? = null
        private var lastHistoryFeedMangaList: PersistentList<FeedManga>? = null
        private var lastSummaryUpdatesFeedMangaList: PersistentList<FeedManga>? = null
        private var lastContinueReadingList: PersistentList<FeedManga>? = null
        private var lastSummaryNewlyAddedFeedMangaList: PersistentList<FeedManga>? = null

        fun onLowMemory() {
            lastUpdatesFeedMangaList = null
            lastHistoryFeedMangaList = null
            lastSummaryUpdatesFeedMangaList = null
            lastContinueReadingList = null
            lastSummaryNewlyAddedFeedMangaList = null
        }

        const val HISTORY_ENDLESS_LIMIT = 15
        const val UPDATES_ENDLESS_LIMIT = 200
    }
}
