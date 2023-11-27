package eu.kanade.tachiyomi.ui.recents

import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.details.MangaDetailsPreferences
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

    ) : BaseCoroutinePresenter<FeedController>(), DownloadQueue.DownloadListener, LibraryServiceListener {

    private val _feedScreenState = MutableStateFlow(
        FeedScreenState(
            feedScreenType = preferences.feedViewType().get(),
            outlineCovers = libraryPreferences.outlineOnCovers().get(),
            incognitoMode = securityPreferences.incognitoMode().get(),
            groupChaptersUpdates = preferences.groupChaptersUpdates().get(),
            historyGrouping = preferences.historyChapterGrouping().get(),
            hideChapterTitles = mangaDetailsPreferences.hideChapterTitlesByDefault().get(),
        ),
    )
    val feedScreenState: StateFlow<FeedScreenState> = _feedScreenState.asStateFlow()

    private var searchJob: Job? = null

    private val paginator = DefaultPaginator(
        initialKey = _feedScreenState.value.offset,
        onLoadUpdated = { },
        onRequest = {
            feedRepository.getPage(offset = _feedScreenState.value.offset, limit = ENDLESS_LIMIT, type = _feedScreenState.value.feedScreenType, group = _feedScreenState.value.historyGrouping)
        },
        getNextKey = {
            _feedScreenState.value.offset + ENDLESS_LIMIT
        },
        onError = {
            //TODO
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
        downloadManager.addListener(this)
        LibraryUpdateService.setListener(this)

        if (_feedScreenState.value.initialLoad) {
            _feedScreenState.update { state ->
                state.copy(initialLoad = false)
            }
            presenterScope.launchIO { loadNextPage() }
        }

        presenterScope.launch {
            _feedScreenState.update {
                it.copy(sideNavMode = SideNavMode.findByPrefValue(preferences.sideNavMode().get()))
            }
        }


        presenterScope.launch {
            securityPreferences.incognitoMode().changes().collectLatest {
                _feedScreenState.update { state ->
                    state.copy(incognitoMode = it)
                }
            }
        }
        presenterScope.launch {
            preferences.groupChaptersUpdates().changes().collectLatest {
                _feedScreenState.update { state ->
                    state.copy(groupChaptersUpdates = it)
                }
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
    }

    fun loadNextPage() {
        presenterScope.launchIO {
            paginator.loadNextItems()
        }
    }

    fun switchViewType(feedScreenType: FeedScreenType) {
        presenterScope.launch {
            preferences.feedViewType().set(feedScreenType)
        }
    }

    fun toggleGroupHistoryType(historyGrouping: FeedHistoryGroup) {
        presenterScope.launch {
            preferences.historyChapterGrouping().set(historyGrouping)
        }
    }

    fun toggleIncognitoMode() {
        presenterScope.launch {
            securityPreferences.incognitoMode().toggle()
        }
    }

    fun deleteAllHistoryForAllManga() {
    }

    fun deleteAllHistory(feedManga: FeedManga) {
        presenterScope.launchIO {
            TimberKt.d { "Delete all history click" }
            feedRepository.deleteAllHistoryForManga(feedManga.mangaId)
            _feedScreenState.update {
                it.copy(
                    allFeedManga = it.allFeedManga.filter { fm -> fm.mangaId != feedManga.mangaId }.toImmutableList(),
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
                val index = _feedScreenState.value.allFeedManga.indexOfFirst { it.mangaId == feedManga.mangaId }
                val mutableFeedManga = _feedScreenState.value.allFeedManga.toMutableList()

                if (_feedScreenState.value.historyGrouping == FeedHistoryGroup.Series) {
                    val newFeedManga = feedRepository.getUpdatedFeedMangaForHistoryBySeries(feedManga)
                    mutableFeedManga[index] = newFeedManga
                } else {
                    val newFeedManga = _feedScreenState.value.allFeedManga[index]
                    mutableFeedManga[index] = newFeedManga.copy(chapters = newFeedManga.chapters.filter { it.chapter.url != simpleChapter.url }.toImmutableList())
                }
                _feedScreenState.update {
                    it.copy(
                        allFeedManga = mutableFeedManga.toImmutableList(),
                    )
                }
            }
        }
    }

    fun search(searchQuery: String?) {
        searchJob?.cancel()
        searchJob = presenterScope.launchIO {
            _feedScreenState.update { it.copy(searchQuery = "") }
            if (searchQuery.isNullOrBlank()) {
                _feedScreenState.update { it.copy(searchFeedManga = persistentListOf()) }
            } else {
                feedRepository.getPage(searchQuery, 0, 100, _feedScreenState.value.feedScreenType, _feedScreenState.value.historyGrouping)
                    .onSuccess { results ->
                        _feedScreenState.update { state ->
                            state.copy(
                                searchFeedManga = (results.second).toImmutableList(),
                            )
                        }
                    }
            }
        }
    }

    fun downloadChapter(chapterItem: ChapterItem, feedManga: FeedManga) {
        presenterScope.launchIO {
            if (chapterItem.isNotDownloaded) {
                feedRepository.downloadChapter(feedManga, chapterItem)
            }
        }
    }

    override fun updateDownload(download: Download) {
        presenterScope.launchIO {
            TimberKt.d { "Updating download downloadState: ${download.status}" }
            launch {
                val (searchFeedUpdated, searchFeedMangaList) = updateChapterDownloadForManga(download, _feedScreenState.value.searchFeedManga.toMutableList())
                if (searchFeedUpdated) {
                    _feedScreenState.update {
                        it.copy(searchFeedManga = searchFeedMangaList.toImmutableList())
                    }
                }
            }
            launch {
                val (feedUpdated, feedMangaList) = updateChapterDownloadForManga(download, _feedScreenState.value.allFeedManga.toMutableList())
                if (feedUpdated) {
                    _feedScreenState.update {
                        it.copy(allFeedManga = feedMangaList.toImmutableList())
                    }
                }
            }

        }
    }

    /**
     * Finds the manga in the given list, finds the matching chapters and updates the chapter and the list.  Returning the updated list or false if the chapter didnt exist
     */
    private fun updateChapterDownloadForManga(download: Download, feedManga: List<FeedManga>): Pair<Boolean, List<FeedManga>> {
        val mutableFeedManga = feedManga.toMutableList()
        val indexOfFeedManga = mutableFeedManga.indexOfFirst { it.mangaId == download.manga.id }
        if (indexOfFeedManga >= 0) {
            val mutableChapters = mutableFeedManga[indexOfFeedManga].chapters.toMutableList()
            val indexOfChapter = mutableChapters.indexOfFirst { it.chapter.id == download.chapter.id }
            if (indexOfChapter >= 0) {
                mutableChapters[indexOfChapter] = mutableChapters[indexOfChapter].copy(downloadState = download.status, downloadProgress = download.progressFloat)
                mutableFeedManga[indexOfFeedManga] = mutableFeedManga[indexOfFeedManga].copy(chapters = mutableChapters.toImmutableList())
                return true to mutableFeedManga
            }
        }
        return false to emptyList()
    }

    override fun updateDownloads() {
        presenterScope.launchIO {
            val allFeedManga = _feedScreenState.value.allFeedManga
            allFeedManga.forEach { feedManga ->

                val updatedChapters = feedRepository.getUpdateChapters(feedManga)

                val mutableFeedManga = _feedScreenState.value.allFeedManga.toMutableList()
                val index = mutableFeedManga.indexOfFirst { it.mangaId == feedManga.mangaId }
                if (index >= 0) {
                    mutableFeedManga[index] = feedManga.copy(chapters = updatedChapters)
                    _feedScreenState.update { it.copy(allFeedManga = mutableFeedManga.toImmutableList()) }
                }

            }
        }
    }

    override fun onUpdateManga(manga: Manga?) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadManager.removeListener(this)
        LibraryUpdateService.removeListener(this)
    }

    companion object {
        const val ENDLESS_LIMIT = 20
    }
}
