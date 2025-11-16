package eu.kanade.tachiyomi.ui.source.latest

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.UiText

data class DisplayScreenState(
    val isLoading: Boolean = false,
    val incognitoMode: Boolean = false,
    val title: UiText = UiText.String(""),
    val allDisplayManga: PersistentList<DisplayManga> = persistentListOf(),
    val filteredDisplayManga: PersistentList<DisplayManga> = persistentListOf(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 1,
    val isList: Boolean,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean = false,
    val libraryEntryVisibility: Int,
    val categories: PersistentList<CategoryItem> = persistentListOf(),
)

@Serializable
sealed interface DisplayScreenType {
    // All types now have a common `title` property
    val title: UiText

    @Serializable
    object LatestChapters : DisplayScreenType {
        override val title = UiText.StringResource(R.string.latest)
    }

    @Serializable
    object FeedUpdates : DisplayScreenType {
        override val title = UiText.StringResource(R.string.feed_updates)
    }

    @Serializable
    object RecentlyAdded : DisplayScreenType {
        override val title = UiText.StringResource(R.string.recently_added)
    }

    @Serializable
    object PopularNewTitles : DisplayScreenType {
        override val title = UiText.StringResource(R.string.popular_new_titles)
    }

    // The 'List' type now just implements the interface
    @Serializable
    data class List(override val title: UiText.String, val listUUID: String) : DisplayScreenType
}
