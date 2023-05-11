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
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val groupChaptersUpdates: Boolean = false,
    val historyGrouping: FeedHistoryGroup,
    val incognitoMode: Boolean = false,
    val allFeedManga: ImmutableList<FeedManga> = persistentListOf(),

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

data class FeedManga(
    val mangaTitle: String,
    val date: Long,
    val mangaId: Long,
    val artwork: Artwork,
    val chapters: ImmutableList<SimpleChapter>,
)


