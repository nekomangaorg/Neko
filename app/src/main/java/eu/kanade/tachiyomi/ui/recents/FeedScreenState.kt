package eu.kanade.tachiyomi.ui.recents

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
    val previousScreenType: FeedScreenType = FeedScreenType.Updates,
    val updatesSortedByFetch: Boolean = true,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val outlineCovers: Boolean,
    val hideChapterTitles: Boolean,
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val groupChaptersUpdates: Boolean = false,
    val historyGrouping: FeedHistoryGroup,
    val incognitoMode: Boolean = false,
    val allFeedManga: ImmutableList<FeedManga> = persistentListOf(),
    val searchFeedManga: ImmutableList<FeedManga> = persistentListOf(),
    val downloads: ImmutableList<DownloadItem> = persistentListOf(),
    val searchQuery: String = "",
)

enum class FeedScreenType {
    History,
    Updates,
    Downloads,
}

enum class FeedHistoryGroup {
    Never,
    Series,
    Day,
    Week,
}

data class FeedSettingActions(
    val groupHistoryClick: (FeedHistoryGroup) -> Unit,
    val clearHistoryClick: () -> Unit,
    val switchUploadsSortOrder: () -> Unit,
)

data class FeedScreenActions(
    val mangaClick: (Long) -> Unit,
    val chapterClick: (Long, Long) -> Unit,
    val switchViewType: (FeedScreenType) -> Unit,
    val deleteHistoryClick: (FeedManga, SimpleChapter) -> Unit,
    val deleteAllHistoryClick: (FeedManga) -> Unit,
    val search: (String?) -> Unit,
    val downloadClick: (ChapterItem, FeedManga, MangaConstants.DownloadAction) -> Unit,
    val updateLibrary: (Boolean) -> Unit,
)

data class DownloadScreenActions(val downloadSwiped: (DownloadItem) -> Unit)

data class FeedManga(
    val mangaTitle: String,
    val date: Long,
    val mangaId: Long,
    val artwork: Artwork,
    val chapters: ImmutableList<ChapterItem>,
)
