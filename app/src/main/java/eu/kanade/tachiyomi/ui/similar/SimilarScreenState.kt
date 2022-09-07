package eu.kanade.tachiyomi.ui.similar

import eu.kanade.tachiyomi.data.models.DisplayManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.domain.category.CategoryItem

data class SimilarScreenState(
    val isRefreshing: Boolean = false,
    val displayManga: ImmutableMap<Int, ImmutableList<DisplayManga>> = persistentMapOf(),
    val error: String? = null,
    val isList: Boolean,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean,
    val categories: ImmutableList<CategoryItem> = persistentListOf(),
)
