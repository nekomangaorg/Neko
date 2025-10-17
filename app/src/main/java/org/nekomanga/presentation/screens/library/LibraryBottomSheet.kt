package org.nekomanga.presentation.screens.library

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.LibrarySheetActions
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.sheets.DisplayOptionsSheet
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.components.sheets.GroupBySheet
import org.nekomanga.presentation.components.sheets.LibrarySortSheet

sealed class LibraryBottomSheetScreen {
    data class SortSheet(val categoryItem: CategoryItem) : LibraryBottomSheetScreen()

    object GroupBySheet : LibraryBottomSheetScreen()

    object DisplayOptionsSheet : LibraryBottomSheetScreen()

    object CategorySheet : LibraryBottomSheetScreen()
}

@Composable
fun LibraryBottomSheet(
    libraryScreenState: LibraryScreenState,
    librarySheetActions: LibrarySheetActions,
    currentScreen: LibraryBottomSheetScreen,
    closeSheet: () -> Unit,
) {

    when (currentScreen) {
        is LibraryBottomSheetScreen.SortSheet ->
            LibrarySortSheet(
                currentLibrarySort = currentScreen.categoryItem.sortOrder,
                librarySortClicked = { librarySort ->
                    librarySheetActions.categoryItemLibrarySortClick(
                        currentScreen.categoryItem,
                        librarySort,
                    )
                    closeSheet()
                },
            )
        is LibraryBottomSheetScreen.GroupBySheet ->
            GroupBySheet(
                groupByOptions = libraryScreenState.groupByOptions,
                currentGroupBy = libraryScreenState.currentGroupBy,
                groupByClick = {
                    librarySheetActions.groupByClick(it)
                    closeSheet()
                },
            )
        is LibraryBottomSheetScreen.DisplayOptionsSheet ->
            DisplayOptionsSheet(
                currentLibraryDisplayMode = libraryScreenState.libraryDisplayMode,
                libraryDisplayModeClick = { librarySheetActions.libraryDisplayModeClick(it) },
                rawColumnCount = libraryScreenState.rawColumnCount,
                rawColumnCountChanged = { librarySheetActions.rawColumnCountChanged(it) },
                outlineCoversEnabled = libraryScreenState.outlineCovers,
                outlineCoversToggled = { librarySheetActions.outlineCoversToggled() },
                unreadBadgesEnabled = libraryScreenState.showUnreadBadges,
                unreadBadgesToggled = { librarySheetActions.unreadBadgesToggled() },
                downloadBadgesEnabled = libraryScreenState.showDownloadBadges,
                downloadBadgesToggled = { librarySheetActions.downloadBadgesToggled() },
                showStartReadingButtonEnabled = libraryScreenState.showStartReadingButton,
                startReadingButtonToggled = { librarySheetActions.startReadingButtonToggled() },
                horizontalCategoriesEnabled = libraryScreenState.horizontalCategories,
                horizontalCategoriesToggled = { librarySheetActions.horizontalCategoriesToggled() },
                showLibraryButtonBarEnabled = libraryScreenState.showLibraryButtonBar,
                showLibraryButtonBarToggled = { librarySheetActions.showLibraryButtonBarToggled() },
            )
        is LibraryBottomSheetScreen.CategorySheet -> {

            val selectedDisplayItems = libraryScreenState.selectedItems.map { it.displayManga }

            val mangaCategories =
                libraryScreenState.selectedItems
                    .map { it.allCategories }
                    .flatten()
                    .distinct()
                    .toPersistentList()

            EditCategorySheet(
                addingToLibrary = false,
                categories = libraryScreenState.userCategories,
                mangaCategories = mangaCategories,
                cancelClick = { closeSheet() },
                addNewCategory = librarySheetActions.addNewCategory,
                confirmClicked = { newCategoryItems ->
                    librarySheetActions.editCategories(selectedDisplayItems, newCategoryItems)
                },
            )
        }
    }
}
