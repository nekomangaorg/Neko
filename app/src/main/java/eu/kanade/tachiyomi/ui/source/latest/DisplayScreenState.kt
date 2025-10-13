package eu.kanade.tachiyomi.ui.source.latest

import android.os.Parcelable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga

data class DisplayScreenState(
    val isLoading: Boolean = false,
    val incognitoMode: Boolean = false,
    val title: String = "",
    val titleRes: Int? = null,
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

@Parcelize
sealed interface DisplayScreenType : Parcelable {
    @Parcelize data class LatestChapters(val titleRes: Int = R.string.latest) : DisplayScreenType

    @Parcelize
    data class FeedUpdates(val titleRes: Int = R.string.feed_updates) : DisplayScreenType

    @Parcelize
    data class RecentlyAdded(val titleRes: Int = R.string.recently_added) : DisplayScreenType

    @Parcelize
    data class PopularNewTitles(val titleRes: Int = R.string.popular_new_titles) :
        DisplayScreenType

    @Parcelize data class List(val title: String, val listUUID: String) : DisplayScreenType
}
