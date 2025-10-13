package eu.kanade.tachiyomi.ui.similar

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga

data class SimilarScreenState(
    val isRefreshing: Boolean = false,
    val incognitoMode: Boolean = false,
    val allDisplayManga: ImmutableMap<Int, PersistentList<DisplayManga>> = persistentMapOf(),
    val filteredDisplayManga: ImmutableMap<Int, PersistentList<DisplayManga>> = persistentMapOf(),
    val error: String? = null,
    val isList: Boolean,
    val libraryEntryVisibility: Int,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean = false,
    val categories: PersistentList<CategoryItem> = persistentListOf(),
)
