package eu.kanade.tachiyomi.ui.source.latest

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem

data class LatestScreenState(
    val isList: Boolean,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean,
    val categories: ImmutableList<CategoryItem> = persistentListOf(),
)
