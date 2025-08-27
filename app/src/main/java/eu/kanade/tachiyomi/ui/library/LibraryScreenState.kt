package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.util.system.SideNavMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.LibraryManga

data class LibraryScreenState(
    val firstLoad: Boolean = true,
    val isRefreshing: Boolean = false,
    val sideNavMode: SideNavMode = SideNavMode.DEFAULT,
    val outlineCovers: Boolean,
    val incognitoMode: Boolean = false,
    val items: ImmutableList<LibraryCategoryItem> = persistentListOf(),
)

data class LibraryScreenActions(
    /* val mangaClick: (Long) -> Unit,*/
    val search: (String?) -> Unit,
    val updateLibrary: (Boolean) -> Unit,
)

data class LibraryCategoryItem(
    val categoryItem: CategoryItem,
    val libraryItems: ImmutableList<LibraryManga> = persistentListOf(),
)
