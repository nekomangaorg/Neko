package eu.kanade.tachiyomi.ui.feed

import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
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
    val updatesSortedByFetch: Boolean = true,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val outlineCovers: Boolean,
    val outlineCards: Boolean,
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val groupChaptersUpdates: Boolean = false,
    val historyGrouping: FeedHistoryGroup,
    val incognitoMode: Boolean = false,
    val allFeedManga: ImmutableList<FeedManga> = persistentListOf(),
    val searchFeedManga: ImmutableList<FeedManga> = persistentListOf(),
    val downloads: ImmutableList<DownloadItem> = persistentListOf(),
    val downloaderStatus: DownloaderStatus = DownloaderStatus.Paused,
    val downloadOnlyOnWifi: Boolean,
    val searchQuery: String = "",
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
    val chapters: ImmutableList<ChapterItem>,
)
