package eu.kanade.tachiyomi.ui.source.latest

import android.os.Parcelable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.parcelize.Parcelize
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga

data class DisplayScreenState(
    val isPageLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val incognitoMode: Boolean = false,
    val title: String = "",
    val titleRes: Int? = null,
    val allDisplayManga: ImmutableMap<Int, PersistentList<DisplayManga>> = persistentMapOf(),
    val filteredDisplayManga: ImmutableMap<Int, PersistentList<DisplayManga>> = persistentMapOf(),
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
    val titleRes: Int?

    @Parcelize
    data class LatestChapters(override val titleRes: Int = R.string.latest) : DisplayScreenType

    @Parcelize
    data class FeedUpdates(override val titleRes: Int = R.string.feed_updates) : DisplayScreenType

    @Parcelize
    data class RecentlyAdded(override val titleRes: Int = R.string.recently_added) :
        DisplayScreenType

    @Parcelize
    data class PopularNewTitles(override val titleRes: Int = R.string.popular_new_titles) :
        DisplayScreenType

    @Parcelize
    data class List(val title: String, val listUUID: String, override val titleRes: Int? = null) :
        DisplayScreenType

    @Parcelize
    data class Similar(val mangaId: Long, override val titleRes: Int = R.string.similar) :
        DisplayScreenType
}
