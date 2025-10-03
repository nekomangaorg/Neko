package eu.kanade.tachiyomi.ui.feed

import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.download.DownloadItem
import org.nekomanga.domain.manga.Artwork

data class FeedScreenState(
    val firstLoad: Boolean = true,
    val isRefreshing: Boolean = false,
    val feedScreenType: FeedScreenType = FeedScreenType.Updates,
    val showingDownloads: Boolean = false,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val outlineCovers: Boolean,
    val outlineCards: Boolean,
    val groupUpdateChapters: Boolean = false,
    val incognitoMode: Boolean = false,
    val swipeRefreshEnabled: Boolean = true,
    val downloads: PersistentList<DownloadItem> = persistentListOf(),
    val downloaderStatus: DownloaderStatus = DownloaderStatus.Paused,
    val downloadOnlyOnWifi: Boolean,
)

data class HistoryScreenPagingState(
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val pageLoading: Boolean = false,
    val historyGrouping: FeedHistoryGroup,
    val historyFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
    val searchHistoryFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
    val searchQuery: String = "",
)

data class UpdatesScreenPagingState(
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val pageLoading: Boolean = false,
    val updatesSortedByFetch: Boolean = true,
    val updatesFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
    val searchUpdatesFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
    val searchQuery: String = "",
)

data class SummaryScreenPagingState(
    val updatingUpdates: Boolean = true,
    val updatesFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
    val updatingContinueReading: Boolean = true,
    val continueReadingList: PersistentList<FeedManga> = persistentListOf(),
    val updatingNewlyAdded: Boolean = true,
    val newlyAddedFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
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

data class FeedSettingActions(
    val groupHistoryClick: (FeedHistoryGroup) -> Unit,
    val clearHistoryClick: () -> Unit,
    val switchUploadsSortOrder: () -> Unit,
    val outlineCoversClick: () -> Unit,
    val outlineCardsClick: () -> Unit,
    val clearDownloadQueueClick: () -> Unit,
    val toggleDownloadOnlyOnWifi: () -> Unit,
    val toggleGroupUpdateChapters: () -> Unit,
    val toggleSwipeRefresh: () -> Unit,
)

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

data class DownloadScreenActions(
    val downloadSwiped: (DownloadItem) -> Unit,
    val fabClick: () -> Unit,
    val moveDownloadClick: (DownloadItem, MoveDownloadDirection) -> Unit,
    val moveSeriesClick: (DownloadItem, MoveDownloadDirection) -> Unit,
    val cancelSeriesClick: (DownloadItem) -> Unit,
)

enum class MoveDownloadDirection {
    Top,
    Bottom,
}

data class FeedManga(
    val mangaTitle: String,
    val date: Long,
    val mangaId: Long,
    val artwork: Artwork,
    val chapters: PersistentList<ChapterItem>,
    val lastReadChapter: String = "",
)
