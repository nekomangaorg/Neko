package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryDisplayMode
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.LibrarySort
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.ui.theme.ThemePreviews
import org.nekomanga.ui.theme.ThemedPreviews

@ThemePreviews
@Composable
private fun VerticalCategoriesPagePreview() {
    val categories =
        listOf(
            CategoryItem(
                id = 1,
                name = "Currently Reading",
                order = 0,
                mangaCount = 2,
                isDynamic = false,
                isHidden = false,
                sortOrder = LibrarySort.LastRead(1),
                isAscending = true,
                isSystemCategory = true,
            ),
            CategoryItem(
                id = 2,
                name = "Completed",
                order = 1,
                mangaCount = 1,
                isDynamic = false,
                isHidden = false,
                sortOrder = LibrarySort.LastRead(2),
                isAscending = false,
                isSystemCategory = true,
            ),
        )

    val manga =
        listOf(
            LibraryMangaItem(
                displayManga =
                    DisplayManga(
                        mangaId = 1L,
                        url = "",
                        title = "Manga 1",
                        artwork = "",
                        inLibrary = true,
                    ),
                downloadCount = 5,
                unreadCount = 2,
                isLocal = false,
                sourceId = 123L,
            ),
            LibraryMangaItem(
                displayManga =
                    DisplayManga(
                        mangaId = 2L,
                        url = "",
                        title = "Another really long manga title to see how it wraps",
                        artwork = "",
                        inLibrary = true,
                    ),
                downloadCount = 0,
                unreadCount = 10,
                isLocal = false,
                sourceId = 123L,
            ),
            LibraryMangaItem(
                displayManga =
                    DisplayManga(
                        mangaId = 3L,
                        url = "",
                        title = "Manga 3",
                        artwork = "",
                        inLibrary = true,
                    ),
                downloadCount = 1,
                unreadCount = 0,
                isLocal = true,
                sourceId = 123L,
            ),
        )

    val items =
        listOf(
            LibraryScreenState.LibraryCategory(
                categoryItem = categories[0],
                libraryItems = manga.subList(0, 2),
                isRefreshing = false,
            ),
            LibraryScreenState.LibraryCategory(
                categoryItem = categories[1],
                libraryItems = manga.subList(2, 3),
                isRefreshing = true,
            ),
        )

    val libraryScreenState =
        LibraryScreenState(
            items = items,
            libraryDisplayMode = LibraryDisplayMode.ComfortableGrid,
            showUnreadBadges = true,
            showDownloadBadges = true,
            outlineCovers = true,
            showStartReadingButton = false,
            selectedItems = persistentListOf(),
            searchQuery = null,
            sort = LibrarySort.LastRead(0),
            rawColumnCount = 2,
            useVividColorHeaders = false,
        )

    val libraryScreenActions =
        LibraryScreenActions(
            mangaClick = {},
            mangaLongClick = {},
            selectAllLibraryMangaItems = {},
            mangaStartReadingClick = {},
        )

    val libraryCategoryActions =
        LibraryCategoryActions(
            categoryItemClick = {},
            categoryAscendingClick = {},
            categoryRefreshClick = {},
        )

    ThemedPreviews {
        VerticalCategoriesPage(
            contentPadding = PaddingValues(0.dp),
            selectionMode = false,
            libraryScreenState = libraryScreenState,
            libraryScreenActions = libraryScreenActions,
            libraryCategoryActions = libraryCategoryActions,
            categorySortClick = {},
        )
    }
}

@ThemePreviews
@Composable
private fun LibraryCategoryHeaderPreview() {
    val categoryItem =
        CategoryItem(
            id = 1,
            name = "Currently Reading",
            order = 0,
            mangaCount = 2,
            isDynamic = false,
            isHidden = false,
            sortOrder = LibrarySort.LastRead(1),
            isAscending = true,
            isSystemCategory = true,
        )

    ThemedPreviews {
        LibraryCategoryHeader(
            categoryItem = categoryItem,
            useVividColorHeaders = false,
            isRefreshing = false,
            isCollapsible = true,
            selectionMode = false,
            allSelected = false,
            categoryItemClick = {},
            categorySortClick = {},
            categoryAscendingClick = {},
            categoryRefreshClick = {},
            enabled = true,
        )
    }
}
