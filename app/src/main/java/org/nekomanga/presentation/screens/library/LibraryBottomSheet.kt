package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.library.LibrarySort
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.sheets.LibrarySortSheet

sealed class LibraryBottomSheetScreen {
    data class SortSheet(val categoryItem: CategoryItem) : LibraryBottomSheetScreen()
}

@Composable
fun LibraryBottomSheet(
    currentScreen: LibraryBottomSheetScreen,
    librarySortClicked: (CategoryItem, LibrarySort) -> Unit,
    contentPadding: PaddingValues,
    closeSheet: () -> Unit,
) {

    when (currentScreen) {
        is LibraryBottomSheetScreen.SortSheet ->
            LibrarySortSheet(
                currentLibrarySort = currentScreen.categoryItem.sortOrder,
                isCurrentLibrarySortAscending = currentScreen.categoryItem.isAscending,
                librarySortClicked = { librarySort ->
                    librarySortClicked(currentScreen.categoryItem, librarySort)
                    closeSheet()
                },
                bottomContentPadding = contentPadding.calculateBottomPadding(),
            )
    }
}
