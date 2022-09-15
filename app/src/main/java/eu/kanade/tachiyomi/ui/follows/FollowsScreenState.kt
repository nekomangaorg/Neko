package eu.kanade.tachiyomi.ui.follows

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError

data class FollowsScreenState(
    val isLoading: Boolean,
    val displayManga: ImmutableMap<Int, ImmutableList<DisplayManga>> = persistentMapOf(),
    val error: ResultError? = null,
    val isList: Boolean,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean,
    val categories: ImmutableList<CategoryItem> = persistentListOf(),
)
