package eu.kanade.tachiyomi.ui.source.browse

import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga

data class BrowseScreenState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val screenType: BrowseScreenType = BrowseScreenType.Homepage,
    val displayMangaHolder: DisplayMangaHolder = DisplayMangaHolder(),
    val homePageManga: ImmutableList<HomePageManga> = persistentListOf(),
    val error: String? = null,
    val endReached: Boolean = false,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val page: Int = 1,
    val isList: Boolean,
    val showLibraryEntries: Boolean,
    val outlineCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean,
    val categories: ImmutableList<CategoryItem> = persistentListOf(),
)

data class HomePageManga(val displayScreenType: DisplayScreenType, val displayManga: ImmutableList<DisplayManga> = persistentListOf())

data class DisplayMangaHolder(
    val browseScreenType: BrowseScreenType = BrowseScreenType.None,
    val allDisplayManga: ImmutableList<DisplayManga> = persistentListOf(),
    val filteredDisplayManga: ImmutableList<DisplayManga> = persistentListOf(),
)

enum class BrowseScreenType {
    Homepage,
    Search,
    Follows,
    None,
}
