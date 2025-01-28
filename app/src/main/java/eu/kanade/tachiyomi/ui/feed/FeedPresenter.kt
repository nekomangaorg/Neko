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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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
                historyGrouping = preferences.historyChapterGrouping().get(),
                downloadOnlyOnWifi = preferences.downloadOnlyOverWifi().get(),
                swipeRefreshEnabled = preferences.swipeRefreshFeedScreen().get(),
            )
        )

    val feedScreenState: StateFlow<FeedScreenState> = _feedScreenState.asStateFlow()

    private var searchJob: Job? = null

    private val paginator =
        DefaultPaginator(
            initialKey = _feedScreenState.value.offset,
            onLoadUpdated = {},
            onRequest = {
                feedRepository.getPage(
                    offset = _feedScreenState.value.offset,
                    limit = ENDLESS_LIMIT,
                    type = _feedScreenState.value.feedScreenType,
                    uploadsFetchSort = _feedScreenState.value.updatesSortedByFetch,
                    group = _feedScreenState.value.historyGrouping,
                )
            },
            getNextKey = { _feedScreenState.value.offset + ENDLESS_LIMIT },
            onError = {
                // TODO
            },
            onSuccess = { hasNextPage, items, newKey ->
                _feedScreenState.update { state ->
                    state.copy(
                        allFeedManga = (state.allFeedManga + items).toImmutableList(),
                        offset = newKey,
                        hasMoreResults = hasNextPage,
                    )
                }
            },
        )

    override fun onCreate() {
        super.onCreate()
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
                    .toImmutableList()

            _feedScreenState.update { it.copy(downloads = downloads) }
        }
        observeDownloads()

        if (_feedScreenState.value.firstLoad) {
            _feedScreenState.update { state -> state.copy(firstLoad = false) }
            presenterScope.launchIO { loadNextPage() }
        }

        presenterScope.launch {
            _feedScreenState.update {
                it.copy(sideNavMode = SideNavMode.findByPrefValue(preferences.sideNavMode().get()))
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
                _feedScreenState.update { state ->
                    state.copy(historyGrouping = it, offset = 0, allFeedManga = persistentListOf())
                }
                paginator.reset()
                loadNextPage()
            }
        }

        presenterScope.launch {
            preferences.feedViewType().changes().collectLatest {
                _feedScreenState.update { state ->
                    state.copy(feedScreenType = it, offset = 0, allFeedManga = persistentListOf())
                }
                paginator.reset()
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
                _feedScreenState.update { state ->
                    state.copy(
                        updatesSortedByFetch = it,
                        offset = 0,
                        allFeedManga = persistentListOf(),
                    )
                }
                paginator.reset()
                loadNextPage()
            }
        }
    }

    fun loadNextPage() {
        presenterScope.launchIO { paginator.loadNextItems() }
    }

    fun switchViewType(feedScreenType: FeedScreenType) {
        presenterScope.launchIO {
            _feedScreenState.update { it.copy(allFeedManga = persistentListOf()) }
            preferences.feedViewType().set(feedScreenType)
        }
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
                        !updatedChapterItem.chapter.bookmark
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
            _feedScreenState.update {
                it.copy(
                    allFeedManga = persistentListOf(),
                    searchFeedManga = persistentListOf(),
                    searchQuery = "",
                )
            }
            feedRepository.deleteAllHistory()
            paginator.reset()
            paginator.loadNextItems()
        }
    }

    fun deleteAllHistory(feedManga: FeedManga) {
        presenterScope.launchIO {
            TimberKt.d { "Delete all history click" }
            feedRepository.deleteAllHistoryForManga(feedManga.mangaId)
            _feedScreenState.update {
                it.copy(
                    allFeedManga =
                        it.allFeedManga
                            .filter { fm -> (fm as FeedManga).mangaId != feedManga.mangaId }
                            .toImmutableList()
                )
            }
        }
    }

    fun deleteHistory(feedManga: FeedManga, simpleChapter: SimpleChapter) {
        presenterScope.launchIO {
            if (feedManga.chapters.size == 1) {
                deleteAllHistory(feedManga)
            } else {

                feedRepository.deleteHistoryForChapter(simpleChapter.url)
                val index =
                    _feedScreenState.value.allFeedManga.indexOfFirst {
                        it.mangaId == feedManga.mangaId
                    }
                val mutableFeedManga = _feedScreenState.value.allFeedManga.toMutableList()

                if (_feedScreenState.value.historyGrouping == FeedHistoryGroup.Series) {
                    val newFeedManga =
                        feedRepository.getUpdatedFeedMangaForHistoryBySeries(feedManga)
                    mutableFeedManga[index] = newFeedManga
                } else {
                    val newFeedManga = _feedScreenState.value.allFeedManga[index]
                    mutableFeedManga[index] =
                        newFeedManga.copy(
                            chapters =
                                newFeedManga.chapters
                                    .filter { it.chapter.url != simpleChapter.url }
                                    .toImmutableList()
                        )
                }
                _feedScreenState.update {
                    it.copy(allFeedManga = mutableFeedManga.toImmutableList())
                }
            }
        }
    }

    fun search(searchQuery: String?) {
        searchJob?.cancel()
        searchJob =
            presenterScope.launchIO {
                _feedScreenState.update { it.copy(searchQuery = "") }
                if (searchQuery.isNullOrBlank()) {
                    _feedScreenState.update { it.copy(searchFeedManga = persistentListOf()) }
                } else {
                    feedRepository
                        .getPage(
                            searchQuery,
                            0,
                            100,
                            _feedScreenState.value.feedScreenType,
                            _feedScreenState.value.updatesSortedByFetch,
                            _feedScreenState.value.historyGrouping,
                        )
                        .onSuccess { results ->
                            _feedScreenState.update { state ->
                                state.copy(searchFeedManga = (results.second).toImmutableList())
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
    private fun updateChapterDownloadForManga(
        chapterId: Long,
        mangaId: Long,
        download: Download?,
        feedManga: List<FeedManga>,
    ): Pair<Boolean, List<FeedManga>> {
        val mutableFeedManga = feedManga.toMutableList()
        val indexOfFeedManga = mutableFeedManga.indexOfFirst { it.mangaId == mangaId }
        if (indexOfFeedManga >= 0) {
            val mutableChapters = mutableFeedManga[indexOfFeedManga].chapters.toMutableList()
            val indexOfChapter = mutableChapters.indexOfFirst { it.chapter.id == chapterId }
            if (indexOfChapter >= 0) {
                mutableChapters[indexOfChapter] =
                    when (download == null) {
                        true ->
                            mutableChapters[indexOfChapter].copy(
                                downloadState = Download.State.NOT_DOWNLOADED,
                                downloadProgress = 0,
                            )
                        false ->
                            mutableChapters[indexOfChapter].copy(
                                downloadState = download.status,
                                downloadProgress = download.progress,
                            )
                    }

                mutableFeedManga[indexOfFeedManga] =
                    mutableFeedManga[indexOfFeedManga].copy(
                        chapters = mutableChapters.toImmutableList()
                    )
                return true to mutableFeedManga
            }
        }
        return false to emptyList()
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
                    paginator.reset()
                    paginator.loadNextItems()
                }
            }
        }
    }

    fun updateMangaForChanges() {
        TimberKt.d {
            "update manga for changes processing=${!_feedScreenState.value.firstLoad && (_feedScreenState.value.allFeedManga.isNotEmpty() || _feedScreenState.value.searchFeedManga.isNotEmpty())}"
        }
        if (
            !_feedScreenState.value.firstLoad &&
                (_feedScreenState.value.allFeedManga.isNotEmpty() ||
                    _feedScreenState.value.searchFeedManga.isNotEmpty())
        ) {
            presenterScope.launchIO {
                launch {
                    if (_feedScreenState.value.searchFeedManga.isNotEmpty()) {
                        feedRepository
                            .getPage(
                                searchQuery = _feedScreenState.value.searchQuery,
                                offset = 0,
                                limit = 100,
                                type = _feedScreenState.value.feedScreenType,
                                uploadsFetchSort = _feedScreenState.value.updatesSortedByFetch,
                                group = _feedScreenState.value.historyGrouping,
                            )
                            .onSuccess { results ->
                                _feedScreenState.update { state ->
                                    state.copy(searchFeedManga = (results.second).toImmutableList())
                                }
                            }
                    }
                }

                launch {
                    val currentOffset = _feedScreenState.value.offset
                    var mutableFeedManga = mutableListOf<FeedManga>()
                    for (i in 0..currentOffset step ENDLESS_LIMIT) {
                        feedRepository
                            .getPage(
                                offset = i,
                                limit = ENDLESS_LIMIT,
                                type = _feedScreenState.value.feedScreenType,
                                uploadsFetchSort = _feedScreenState.value.updatesSortedByFetch,
                                group = _feedScreenState.value.historyGrouping,
                            )
                            .onSuccess { results ->
                                mutableFeedManga =
                                    (mutableFeedManga + results.second).toMutableList()
                            }
                    }
                    _feedScreenState.update { state ->
                        state.copy(allFeedManga = mutableFeedManga.toImmutableList())
                    }
                }
            }
        }
    }

    private fun updateReadOnFeed(chapterItem: ChapterItem) {

        val (searchFeedUpdated, searchFeedMangaList) =
            updateChapterReadStatus(
                chapterItem,
                _feedScreenState.value.searchFeedManga.map { it }.toList(),
            )
        if (searchFeedUpdated) {
            _feedScreenState.update {
                it.copy(searchFeedManga = searchFeedMangaList.toImmutableList())
            }
        }

        val (feedUpdated, feedMangaList) =
            updateChapterReadStatus(
                chapterItem,
                _feedScreenState.value.allFeedManga.map { it }.toList(),
            )
        if (feedUpdated) {
            _feedScreenState.update { it.copy(allFeedManga = feedMangaList.toImmutableList()) }
        }
    }

    private fun updateDownloadOnFeed(chapterId: Long, mangaId: Long, download: Download?) {

        val (searchFeedUpdated, searchFeedMangaList) =
            updateChapterDownloadForManga(
                chapterId,
                mangaId,
                download,
                _feedScreenState.value.searchFeedManga.map { it }.toList(),
            )
        if (searchFeedUpdated) {
            _feedScreenState.update {
                it.copy(searchFeedManga = searchFeedMangaList.toImmutableList())
            }
        }

        val (feedUpdated, feedMangaList) =
            updateChapterDownloadForManga(
                chapterId,
                mangaId,
                download,
                _feedScreenState.value.allFeedManga.map { it }.toList(),
            )
        if (feedUpdated) {
            _feedScreenState.update { it.copy(allFeedManga = feedMangaList.toImmutableList()) }
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

            _feedScreenState.update { it.copy(downloads = mutableList.toImmutableList()) }
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
                        .toImmutableList()

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

    fun refreshing(start: Boolean) {
        presenterScope.launchIO { _feedScreenState.update { it.copy(isRefreshing = start) } }
    }

    override fun onResume() {
        super.onResume()
        observeDownloads()
    }

    companion object {
        const val ENDLESS_LIMIT = 10
    }
}
