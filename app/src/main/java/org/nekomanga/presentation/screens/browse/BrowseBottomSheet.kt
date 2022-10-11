package org.nekomanga.presentation.screens.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenState
import eu.kanade.tachiyomi.ui.source.browse.FilterActions
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.components.sheets.FilterBrowseSheet

/**
 * Sealed class that holds the types of bottom sheets the details screen can show
 */
sealed class BrowseBottomSheetScreen {
    data class CategoriesSheet(
        val addingToLibrary: Boolean = true,
        val setCategories: (List<CategoryItem>) -> Unit,
    ) : BrowseBottomSheetScreen()

    data class FilterSheet(val nothing: String = "") : BrowseBottomSheetScreen()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BrowseBottomSheet(
    currentScreen: BrowseBottomSheetScreen,
    browseScreenState: State<BrowseScreenState>,
    addNewCategory: (String) -> Unit,
    bottomPadding: Dp,
    filterActions: FilterActions,
    closeSheet: () -> Unit,
) {

    val keyboardController = LocalSoftwareKeyboardController.current


    when (currentScreen) {
        is BrowseBottomSheetScreen.CategoriesSheet -> EditCategorySheet(
            addingToLibrary = currentScreen.addingToLibrary,
            categories = browseScreenState.value.categories,
            bottomPadding = bottomPadding,
            cancelClick = closeSheet,
            addNewCategory = addNewCategory,
            confirmClicked = currentScreen.setCategories,
        )
        is BrowseBottomSheetScreen.FilterSheet -> FilterBrowseSheet(
            filters = browseScreenState.value.filters,
            bottomPadding = bottomPadding,
            filterClick = {
                keyboardController?.hide()
                closeSheet()
                filterActions.filterClick()
            },
            resetClick = {
                keyboardController?.hide()
                filterActions.resetClick
            },
            filterChanged = filterActions.filterChanged,
        )
    }
}
