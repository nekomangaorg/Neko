package eu.kanade.tachiyomi.ui.source.latest

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.domain.DisplayResult
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.UiText

data class DisplayScreenState(
    val isLoading: Boolean = false,
    val incognitoMode: Boolean = false,
    val title: UiText = UiText.String(""),
    val alternativeDisplay: PersistentList<DisplayResult> = persistentListOf(),
    val allDisplayManga: PersistentList<DisplayManga> = persistentListOf(),
    val filteredDisplayManga: PersistentList<DisplayManga> = persistentListOf(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 1,
    val isList: Boolean,
    val isDisplayResult: Boolean = false,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean = false,
    val libraryEntryVisibility: Int,
    val categories: PersistentList<CategoryItem> = persistentListOf(),
)

data class DisplayPageResult(
    val hasNextPage: Boolean = false,
    val displayResult: PersistentList<DisplayResult> = persistentListOf(),
    val displayManga: PersistentList<DisplayManga> = persistentListOf(),
)

sealed interface DisplayScreenType {
    // All types now have a common `title` property
    val title: UiText

    object LatestChapters : DisplayScreenType {
        override val title = UiText.StringResource(R.string.latest)
    }

    object FeedUpdates : DisplayScreenType {
        override val title = UiText.StringResource(R.string.feed_updates)
    }

    object RecentlyAdded : DisplayScreenType {
        override val title = UiText.StringResource(R.string.recently_added)
    }

    object PopularNewTitles : DisplayScreenType {
        override val title = UiText.StringResource(R.string.popular_new_titles)
    }

    data class List(override val title: UiText.String, val listUUID: String) : DisplayScreenType

    data class AuthorByName(override val title: UiText.String) : DisplayScreenType

    data class AuthorWithUuid(override val title: UiText.String, val authorUUID: String) :
        DisplayScreenType

    data class GroupByName(override val title: UiText.String) : DisplayScreenType

    data class GroupByUuid(override val title: UiText.String, val groupUUID: String) :
        DisplayScreenType
}
