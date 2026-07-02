package org.nekomanga.presentation.screens.feed

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.download.DownloadItem
import org.nekomanga.domain.manga.Artwork

@Immutable
data class FeedScreenState(
    val firstLoad: Boolean = true,
    val isRefreshing: Boolean = false,
    val feedScreenType: FeedScreenType = FeedScreenType.Updates,
    val showingDownloads: Boolean = false,
    val outlineCovers: Boolean,
    val dynamicCovers: Boolean,
    val outlineCards: Boolean,
    val groupUpdateChapters: Boolean = false,
    val incognitoMode: Boolean = false,
    val useVividColorHeaders: Boolean = true,
    val swipeRefreshEnabled: Boolean = true,
    val downloads: List<DownloadItem> = listOf(),
    val downloaderStatus: DownloaderStatus = DownloaderStatus.Paused,
    val downloadOnlyOnUnmetered: Boolean,
)

@Immutable
data class HistoryScreenPagingState(
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val pageLoading: Boolean = false,
    val historyGrouping: FeedHistoryGroup,
    val historyFeedMangaList: List<FeedManga> = listOf(),
    val searchHistoryFeedMangaList: List<FeedManga> = listOf(),
    val searchQuery: String = "",
)

@Immutable
data class UpdatesScreenPagingState(
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val pageLoading: Boolean = false,
    val updatesSortedByFetch: Boolean = true,
    val updatesFeedMangaList: List<FeedManga> = listOf(),
    val searchUpdatesFeedMangaList: List<FeedManga> = listOf(),
    val searchQuery: String = "",
)

@Immutable
data class SummaryScreenPagingState(
    val updatingUpdates: Boolean = true,
    val updatesFeedMangaList: List<FeedManga> = listOf(),
    val updatingContinueReading: Boolean = true,
    val continueReadingList: List<FeedManga> = listOf(),
    val updatingNewlyAdded: Boolean = true,
    val newlyAddedFeedMangaList: List<FeedManga> = listOf(),
)

enum class FeedScreenType {
    Summary,
    History,
    Updates,
}

enum class FeedHistoryGroup {
    No,
    Series,
    Day,
    Week,
}

enum class DownloaderStatus {
    Running,
    Paused,
    NetworkPaused,
}

@Immutable
data class FeedSettingActions(
    val groupHistoryClick: (FeedHistoryGroup) -> Unit,
    val clearHistoryClick: () -> Unit,
    val switchUploadsSortOrder: () -> Unit,
    val outlineCoversClick: () -> Unit,
    val outlineCardsClick: () -> Unit,
    val clearDownloadQueueClick: () -> Unit,
    val toggleDownloadOnUnmetered: () -> Unit,
    val toggleGroupUpdateChapters: () -> Unit,
    val toggleSwipeRefresh: () -> Unit,
)

@Immutable
data class FeedScreenActions(
    val mangaClick: (Long) -> Unit,
    val chapterClick: (Long, Long) -> Unit,
    val chapterSwipe: (ChapterItem) -> Unit,
    val switchViewType: (FeedScreenType) -> Unit,
    val toggleShowingDownloads: () -> Unit,
    val deleteHistoryClick: (FeedManga, SimpleChapter) -> Unit,
    val deleteAllHistoryClick: (FeedManga) -> Unit,
    val search: (String?) -> Unit,
    val downloadClick: (ChapterItem, FeedManga, MangaConstants.DownloadAction) -> Unit,
    val updateLibrary: (Boolean) -> Unit,
)

@Immutable
data class DownloadScreenActions(
    val downloadSwiped: (DownloadItem) -> Unit,
    val fabClick: () -> Unit,
    val moveDownloadClick: (DownloadItem, MoveDownloadDirection) -> Unit,
    val moveSeriesClick: (DownloadItem, MoveDownloadDirection) -> Unit,
    val cancelSeriesClick: (DownloadItem) -> Unit,
    val cancelSourceClick: (String) -> Unit,
)

enum class MoveDownloadDirection {
    Top,
    Bottom,
}

@Immutable
data class FeedManga(
    val mangaTitle: String,
    val date: Long,
    val mangaId: Long,
    val artwork: Artwork,
    val chapters: List<ChapterItem>,
    val lastReadChapter: String = "",
)
