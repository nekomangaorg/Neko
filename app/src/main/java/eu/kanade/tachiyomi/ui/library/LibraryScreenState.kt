package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.LibraryMangaItem

data class LibraryScreenState(
    val libraryViewType: LibraryViewType = LibraryViewType.List,
    val firstLoad: Boolean = true,
    val isRefreshing: Boolean = false,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val outlineCovers: Boolean,
    val incognitoMode: Boolean = false,
    val items: ImmutableList<LibraryCategoryItem> = persistentListOf(),
)

data class LibraryScreenActions(
    /* val mangaClick: (Long) -> Unit,*/
    val categoryItemClick: (CategoryItem) -> Unit,
    val search: (String?) -> Unit,
    val updateLibrary: (Boolean) -> Unit,
)

data class LibraryViewItem(
    val libraryViewType: LibraryViewType,
    val libraryCategoryItems: PersistentList<LibraryCategoryItem>,
)

data class LibraryCategoryItem(
    val categoryItem: CategoryItem,
    val libraryItems: ImmutableList<LibraryMangaItem> = persistentListOf(),
)

sealed class LibraryViewType {
    data class ComfortableGrid(val rawColumnCount: Float) : LibraryViewType()

    data class CompactGrid(val rawColumnCount: Float) : LibraryViewType()

    object List : LibraryViewType()
}
