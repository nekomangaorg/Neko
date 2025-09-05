package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.LibraryMangaItem

data class LibraryScreenState(
    val libraryViewType: LibraryViewType = LibraryViewType.List,
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
)

data class LibrarySheetActions(
    val groupByClick: (Int) -> Unit,
    val categoryItemLibrarySortClick: (CategoryItem, LibrarySort) -> Unit,
)

data class LibraryCategoryActions(
    val categoryItemClick: (CategoryItem) -> Unit,
    val categoryRefreshClick: (CategoryItem) -> Unit,
    val dragAndDropManga: (Int, Int, CategoryItem, LibraryMangaItem) -> Unit,
)

data class LibraryViewItem(
    val libraryViewType: LibraryViewType,
    val libraryCategoryItems: PersistentList<LibraryCategoryItem>,
)

data class LibraryCategoryItem(
    val categoryItem: CategoryItem,
    val isRefreshing: Boolean = false,
    val libraryItems: ImmutableList<LibraryMangaItem> = persistentListOf(),
)

sealed class LibraryViewType() {

    data class Grid(val rawColumnCount: Float, val gridType: GridType) : LibraryViewType()

    object List : LibraryViewType()

    enum class GridType {
        Comfortable,
        Compact,
    }
}

