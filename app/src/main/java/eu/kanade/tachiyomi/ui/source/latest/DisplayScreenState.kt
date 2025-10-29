package eu.kanade.tachiyomi.ui.source.latest

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem

data class DisplayScreenState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val incognitoMode: Boolean = false,
    val title: String = "",
    val titleRes: Int? = null,
    val allDisplayManga: DisplayScreenContent = DisplayScreenContent.List(persistentListOf()),
    val filteredDisplayManga: DisplayScreenContent = DisplayScreenContent.List(persistentListOf()),
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
