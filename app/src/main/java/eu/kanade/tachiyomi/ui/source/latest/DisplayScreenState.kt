package eu.kanade.tachiyomi.ui.source.latest

import eu.kanade.tachiyomi.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga

data class DisplayScreenState(
    val isLoading: Boolean = false,
    val title: String = "",
    val titleRes: Int? = null,
    val displayManga: ImmutableList<DisplayManga> = persistentListOf(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 1,
    val isList: Boolean,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean,
    val categories: ImmutableList<CategoryItem> = persistentListOf(),
)

enum class DisplayScreenType(val titleRes: Int? = null) {
    LatestChapters(R.string.latest),
    RecentlyAdded(R.string.recently_added),
    List,
}
