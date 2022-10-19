package eu.kanade.tachiyomi.ui.source.browse

import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.DisplayResult
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.NewFilter
import org.nekomanga.domain.manga.DisplayManga

data class BrowseScreenState(
    val initialLoading: Boolean = true,
    val pageLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val incognitoMode: Boolean = false,
    val screenType: BrowseScreenType = BrowseScreenType.Homepage,
    val displayMangaHolder: DisplayMangaHolder = DisplayMangaHolder(),
    val homePageManga: ImmutableList<HomePageManga> = persistentListOf(),
    val otherResults: ImmutableList<DisplayResult> = persistentListOf(),
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
    val filters: DexFilters,
    val defaultContentRatings: ImmutableSet<String>,
    val handledIncomingQuery: Boolean = false,
    val savedFilters: ImmutableList<BrowseFilterImpl> = persistentListOf(),
    val categories: ImmutableList<CategoryItem> = persistentListOf(),
)

data class HomePageManga(val displayScreenType: DisplayScreenType, val displayManga: ImmutableList<DisplayManga> = persistentListOf())

data class DisplayMangaHolder(
    val resultType: BrowseScreenType = BrowseScreenType.None,
    val allDisplayManga: ImmutableList<DisplayManga> = persistentListOf(),
    val filteredDisplayManga: ImmutableList<DisplayManga> = persistentListOf(),
)

enum class BrowseScreenType {
    Homepage,
    Other,
    Filter,
    Follows,
    None,
}

data class FilterActions(
    val filterClick: () -> Unit,
    val saveFilterClick: (String) -> Unit,
    val deleteFilterClick: (String) -> Unit,
    val filterDefaultClick: (String, Boolean) -> Unit,
    val loadFilter: (BrowseFilterImpl) -> Unit,
    val resetClick: () -> Unit,
    val filterChanged: (NewFilter) -> Unit,
)





