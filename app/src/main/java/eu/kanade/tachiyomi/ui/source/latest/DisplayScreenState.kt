package eu.kanade.tachiyomi.ui.source.latest

import android.os.Parcelable
import eu.kanade.tachiyomi.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga

data class DisplayScreenState(
    val isLoading: Boolean = false,
    val title: String = "",
    val titleRes: Int? = null,
    val allDisplayManga: ImmutableList<DisplayManga> = persistentListOf(),
    val filteredDisplayManga: ImmutableList<DisplayManga> = persistentListOf(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 1,
    val isList: Boolean,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean,
    val showLibraryEntries: Boolean,
    val categories: ImmutableList<CategoryItem> = persistentListOf(),
)

@Parcelize
sealed interface DisplayScreenType : Parcelable {
    @Parcelize
    data class LatestChapters(val titleRes: Int = R.string.latest) : DisplayScreenType

    @Parcelize
    data class RecentlyAdded(val titleRes: Int = R.string.recently_added) : DisplayScreenType

    @Parcelize
    data class List(val title: String, val listUUID: String) : DisplayScreenType
}


