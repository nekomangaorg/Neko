package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.library.filter.FilterUnread
import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.presentation.components.UiText

data class LibraryScreenState(
    val libraryDisplayMode: LibraryDisplayMode = LibraryDisplayMode.ComfortableGrid,
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
    val currentGroupBy: Int = 0,
    val items: ImmutableList<LibraryCategoryItem> = persistentListOf(),
)

data class LibraryScreenActions(
    val mangaClick: (Long) -> Unit,
    val search: (String?) -> Unit,
    val updateLibrary: (Boolean) -> Unit,
    val collapseExpandAllCategories: () -> Unit,
    val filterUnreadToggled: (FilterUnread) -> Unit,
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

@Immutable data class LibraryFilters(val filterUnread: FilterUnread = FilterUnread.Inactive)

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
