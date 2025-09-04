package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.LibrarySheetActions
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.sheets.GroupBySheet
import org.nekomanga.presentation.components.sheets.LibrarySortSheet

sealed class LibraryBottomSheetScreen {
    data class SortSheet(val categoryItem: CategoryItem) : LibraryBottomSheetScreen()

    object GroupBySheet : LibraryBottomSheetScreen()
}

@Composable
fun LibraryBottomSheet(
    libraryScreenState: LibraryScreenState,
    librarySheetActions: LibrarySheetActions,
    currentScreen: LibraryBottomSheetScreen,
    contentPadding: PaddingValues,
    closeSheet: () -> Unit,
) {

    when (currentScreen) {
        is LibraryBottomSheetScreen.SortSheet ->
            LibrarySortSheet(
                currentLibrarySort = currentScreen.categoryItem.sortOrder,
                isCurrentLibrarySortAscending = currentScreen.categoryItem.isAscending,
                librarySortClicked = { librarySort ->
                    librarySheetActions.categoryItemLibrarySortClick(
                        currentScreen.categoryItem,
                        librarySort,
                    )
                    closeSheet()
                },
                bottomContentPadding = contentPadding.calculateBottomPadding(),
            )
        is LibraryBottomSheetScreen.GroupBySheet ->
            GroupBySheet(
                groupByOptions = libraryScreenState.groupByOptions,
                currentGroupBy = libraryScreenState.currentGroupBy,
                groupByClick = {
                    librarySheetActions.groupByClick(it)
                    closeSheet()
                },
                bottomContentPadding = contentPadding.calculateBottomPadding(),
            )
    }
}
