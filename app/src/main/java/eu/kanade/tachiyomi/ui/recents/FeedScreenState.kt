package eu.kanade.tachiyomi.ui.recents

import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork

data class FeedScreenState(
    val initialLoad: Boolean = true,
    val feedScreenType: FeedScreenType = FeedScreenType.Updates,
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
    val searchQuery: String = "",
)

enum class FeedScreenType {
    History,
    Updates
}

enum class FeedHistoryGroup {
    Never,
    Series,
    Day,
    Week
}

data class FeedSettingActions(
    val groupHistoryClick: (FeedHistoryGroup) -> Unit,
)

data class FeedScreenActions(
    val mangaClick: (Long) -> Unit,
    val switchViewType: (FeedScreenType) -> Unit,
    val deleteHistoryClick: (FeedManga, SimpleChapter) -> Unit,
    val deleteAllHistoryClick: (FeedManga) -> Unit,
    val search: (String?) -> Unit,
)

data class FeedManga(
    val mangaTitle: String,
    val date: Long,
    val mangaId: Long,
    val artwork: Artwork,
    val chapters: ImmutableList<SimpleChapter>,
)


