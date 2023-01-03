package eu.kanade.tachiyomi.ui.recents

import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.chapter.FeedChapter

data class FeedScreenState(
    val feedScreenType: FeedScreenType = FeedScreenType.Updates,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val outlineCovers: Boolean,
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val groupChaptersUpdates: Boolean = false,
    val incognitoMode: Boolean = false,
    val allFeedChapters: ImmutableList<FeedChapter> = persistentListOf(),

    )

enum class FeedScreenType {
    History,
    Updates
}
