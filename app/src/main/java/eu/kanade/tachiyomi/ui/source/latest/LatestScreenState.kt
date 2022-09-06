package eu.kanade.tachiyomi.ui.source.latest

import eu.kanade.tachiyomi.data.models.DisplayManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem

data class LatestScreenState(
    val isLoading: Boolean = false,
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
