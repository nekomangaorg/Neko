package eu.kanade.tachiyomi.ui.source.browse

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga

data class BrowseScreenState(
    val isLoading: Boolean = true,
    val initialScreen: Boolean = true,
    val displayManga: PersistentList<DisplayManga> = persistentListOf(),
    val homePageManga: PersistentList<HomePageManga> = persistentListOf(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 1,
    val isList: Boolean,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean,
    val categories: PersistentList<CategoryItem> = persistentListOf(),
)

data class HomePageManga(val title: String? = null, val titleRes: Int? = null, val displayManga: ImmutableList<DisplayManga> = persistentListOf())
