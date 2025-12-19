package org.nekomanga.presentation.screens.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenState
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenType
import eu.kanade.tachiyomi.ui.source.browse.FilterActions
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.sheets.BrowseDisplayOptionsSheet
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.components.sheets.FilterBrowseSheet

/** Sealed class that holds the types of bottom sheets the details screen can show */
sealed class BrowseBottomSheetScreen {
    data class CategoriesSheet(
        val addingToLibrary: Boolean = true,
        val setCategories: (List<CategoryItem>) -> Unit,
    ) : BrowseBottomSheetScreen()

    data class FilterSheet(val nothing: String = "") : BrowseBottomSheetScreen()

    data class BrowseDisplayOptionsSheet(
        val switchDisplayClick: () -> Unit,
        val libraryEntryVisibilityClick: (Int) -> Unit,
    ) : BrowseBottomSheetScreen()
}

@Composable
fun BrowseBottomSheet(
    currentScreen: BrowseBottomSheetScreen,
    browseScreenState: BrowseScreenState,
    addNewCategory: (String) -> Unit,
    filterActions: FilterActions,
    closeSheet: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    when (currentScreen) {
        is BrowseBottomSheetScreen.BrowseDisplayOptionsSheet -> {

            BrowseDisplayOptionsSheet(
                showIsList = browseScreenState.screenType != BrowseScreenType.Homepage,
                isList = browseScreenState.isList,
                switchDisplayClick = currentScreen.switchDisplayClick,
                currentLibraryEntryVisibility = browseScreenState.libraryEntryVisibility,
                libraryEntryVisibilityClick = currentScreen.libraryEntryVisibilityClick,
            )
        }
        is BrowseBottomSheetScreen.CategoriesSheet ->
            EditCategorySheet(
                addingToLibrary = currentScreen.addingToLibrary,
                categories = browseScreenState.categories,
                cancelClick = closeSheet,
                addNewCategory = addNewCategory,
                confirmClicked = currentScreen.setCategories,
            )
        is BrowseBottomSheetScreen.FilterSheet ->
            FilterBrowseSheet(
                filters = browseScreenState.filters,
                savedFilters = browseScreenState.savedFilters,
                defaultContentRatings = browseScreenState.defaultContentRatings,
                filterClick = {
                    keyboardController?.hide()
                    closeSheet()
                    filterActions.filterClick()
                },
                resetClick = {
                    keyboardController?.hide()
                    filterActions.resetClick()
                },
                filterChanged = filterActions.filterChanged,
                saveClick = filterActions.saveFilterClick,
                deleteFilterClick = filterActions.deleteFilterClick,
                filterDefaultClick = filterActions.filterDefaultClick,
                loadFilter = filterActions.loadFilter,
            )
    }
}
