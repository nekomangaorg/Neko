package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.library.filter.FilterBookmarked
import eu.kanade.tachiyomi.ui.library.filter.FilterCompleted
import eu.kanade.tachiyomi.ui.library.filter.FilterDownloaded
import eu.kanade.tachiyomi.ui.library.filter.FilterMangaType
import eu.kanade.tachiyomi.ui.library.filter.FilterMerged
import eu.kanade.tachiyomi.ui.library.filter.FilterMissingChapters
import eu.kanade.tachiyomi.ui.library.filter.FilterTracked
import eu.kanade.tachiyomi.ui.library.filter.FilterUnavailable
import eu.kanade.tachiyomi.ui.library.filter.FilterUnread
import eu.kanade.tachiyomi.ui.library.filter.LibraryFilterType
import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.presentation.components.UiText

data class LibraryScreenState(
    val searchQuery: String? = null,
    val libraryDisplayMode: LibraryDisplayMode = LibraryDisplayMode.ComfortableGrid,
    val hasActiveFilters: Boolean = false,
    val libraryFilters: LibraryFilters = LibraryFilters(),
    val rawColumnCount: Float = 3f,
    val isFirstLoad: Boolean = true,
    val allCollapsed: Boolean = false,
    val isRefreshing: Boolean = false,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val outlineCovers: Boolean = false,
    val showUnreadBadges: Boolean = false,
    val showDownloadBadges: Boolean = false,
    val incognitoMode: Boolean = false,
    val groupByOptions: ImmutableList<Int> = persistentListOf(),
    val trackMap: PersistentMap<Long, List<String>> = persistentMapOf(),
    val showUnavailableFilter: Boolean = false,
    val currentGroupBy: Int = 0,
    val items: ImmutableList<LibraryCategoryItem> = persistentListOf(),
    val selectedItems: ImmutableList<LibraryMangaItem> = persistentListOf(),
)

data class LibraryScreenActions(
    val mangaClick: (Long) -> Unit,
    val mangaLongClick: (LibraryMangaItem) -> Unit,
    val selectAllLibraryMangaItems: (List<LibraryMangaItem>) -> Unit,
    val clearSelectedManga: () -> Unit,
    val search: (String?) -> Unit,
    val updateLibrary: (Boolean) -> Unit,
    val collapseExpandAllCategories: () -> Unit,
    val clearActiveFilters: () -> Unit,
    val filterToggled: (LibraryFilterType) -> Unit,
)

data class LibrarySheetActions(
    val groupByClick: (Int) -> Unit,
    val categoryItemLibrarySortClick: (CategoryItem, LibrarySort) -> Unit,
    val libraryDisplayModeClick: (LibraryDisplayMode) -> Unit,
    val rawColumnCountChanged: (Float) -> Unit,
    val outlineCoversToggled: () -> Unit,
    val unreadBadgesToggled: () -> Unit,
    val downloadBadgesToggled: () -> Unit,
)

data class LibraryCategoryActions(
    val categoryItemClick: (CategoryItem) -> Unit,
    val categoryRefreshClick: (CategoryItem) -> Unit,
    val dragAndDropManga: (Int, Int, CategoryItem, LibraryMangaItem) -> Unit,
)

data class LibraryViewItem(
    val libraryDisplayMode: LibraryDisplayMode,
    val rawColumnCount: Float = 3f,
    val libraryCategoryItems: PersistentList<LibraryCategoryItem>,
)

data class LibraryCategoryItem(
    val categoryItem: CategoryItem,
    val isRefreshing: Boolean = false,
    val libraryItems: ImmutableList<LibraryMangaItem> = persistentListOf(),
)

@Immutable
data class LibraryViewPreferences(
    val collapsedCategories: Set<String>,
    val collapsedDynamicCategories: Set<String>,
    val sortingMode: LibrarySort,
    val sortAscending: Boolean,
    val groupBy: Int,
    val showDownloadBadges: Boolean,
)

@Immutable
data class LibraryFilters(
    val filterBookmarked: FilterBookmarked = FilterBookmarked.Inactive,
    val filterCompleted: FilterCompleted = FilterCompleted.Inactive,
    val filterDownloaded: FilterDownloaded = FilterDownloaded.Inactive,
    val filterMangaType: FilterMangaType = FilterMangaType.Inactive,
    val filterMerged: FilterMerged = FilterMerged.Inactive,
    val filterMissingChapters: FilterMissingChapters = FilterMissingChapters.Inactive,
    val filterTracked: FilterTracked = FilterTracked.Inactive,
    val filterUnavailable: FilterUnavailable = FilterUnavailable.Inactive,
    val filterUnread: FilterUnread = FilterUnread.Inactive,
) {
    fun hasActiveFilter(): Boolean {
        return filterBookmarked !is FilterBookmarked.Inactive ||
            filterCompleted !is FilterCompleted.Inactive ||
            filterDownloaded !is FilterDownloaded.Inactive ||
            filterMangaType !is FilterMangaType.Inactive ||
            filterMerged !is FilterMerged.Inactive ||
            filterMissingChapters !is FilterMissingChapters.Inactive ||
            filterTracked !is FilterTracked.Inactive ||
            filterUnavailable !is FilterUnavailable.Inactive ||
            filterUnread !is FilterUnread.Inactive
    }
}

sealed interface LibraryDisplayMode {

    data object ComfortableGrid : LibraryDisplayMode

    data object CompactGrid : LibraryDisplayMode

    data object List : LibraryDisplayMode

    fun toInt(): Int {
        return when (this) {
            is List -> 0
            is CompactGrid -> 1
            else -> 2
        }
    }

    fun toUiText(): UiText {
        return when (this) {
            is List -> UiText.StringResource(R.string.list)
            is CompactGrid -> UiText.StringResource(R.string.compact_grid)
            else -> UiText.StringResource(R.string.comfortable_grid)
        }
    }

    companion object {

        fun entries() = listOf(List, CompactGrid, ComfortableGrid)

        fun fromInt(value: Int): LibraryDisplayMode {
            return when (value) {
                0 -> List
                1 -> CompactGrid
                else -> ComfortableGrid
            }
        }
    }
}
