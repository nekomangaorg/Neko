package eu.kanade.tachiyomi.ui.source.browse

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import org.nekomanga.R
import org.nekomanga.domain.DisplayResult
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.Filter
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.UiText

@Immutable
data class BrowseScreenState(
    val initialLoading: Boolean = true,
    val useVividColorHeaders: Boolean = true,
    val title: UiText = UiText.StringResource(R.string.browse),
    val hideFooterButton: Boolean = false,
    val pageLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val incognitoMode: Boolean = false,
    val screenType: BrowseScreenType = BrowseScreenType.Homepage,
    val displayMangaHolder: DisplayMangaHolder = DisplayMangaHolder(),
    val homePageManga: List<HomePageManga> = listOf(),
    val otherResults: List<DisplayResult> = listOf(),
    val error: UiText? = null,
    val endReached: Boolean = false,
    val page: Int = 1,
    val deepLinkHandled: Boolean = false,
    val isList: Boolean,
    val libraryEntryVisibility: Int,
    val outlineCovers: Boolean,
    val dynamicCovers: Boolean,
    val isComfortableGrid: Boolean,
    val rawColumnCount: Float,
    val promptForCategories: Boolean = false,
    val filters: DexFilters,
    val defaultContentRatings: Set<String>,
    val firstLoad: Boolean = true,
    val savedFilters: List<BrowseFilterImpl> = listOf(),
    val categories: List<CategoryItem> = listOf(),
)

@Immutable
data class HomePageManga(
    val displayScreenType: DisplayScreenType,
    val displayManga: List<DisplayManga> = listOf(),
)

@Immutable
data class DisplayMangaHolder(
    val resultType: BrowseScreenType = BrowseScreenType.None,
    val allDisplayManga: List<DisplayManga> = listOf(),
    val filteredDisplayManga: List<DisplayManga> = listOf(),
    val groupedDisplayManga: Map<Int, List<DisplayManga>> = mapOf(),
)

object LibraryEntryVisibility {
    const val SHOW_ALL = 0
    const val SHOW_NOT_IN_LIBRARY = 1
    const val SHOW_IN_LIBRARY = 2
}

enum class BrowseScreenType {
    Homepage,
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
    val filterChanged: (Filter) -> Unit,
)

sealed class NavigationEvent {
    data class NavigateToDisplay(val displayScreenType: DisplayScreenType) : NavigationEvent()
}
