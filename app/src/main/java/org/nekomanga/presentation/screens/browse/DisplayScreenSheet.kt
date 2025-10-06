package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.sheets.BrowseDisplayOptionsSheet
import org.nekomanga.presentation.components.sheets.EditCategorySheet

sealed class DisplaySheetScreen {
    data class CategoriesSheet(
        val addingToLibrary: Boolean = true,
        val setCategories: (List<CategoryItem>) -> Unit,
    ) : DisplaySheetScreen()

    data class BrowseDisplayOptionsSheet(
        val showIsList: Boolean,
        val switchDisplayClick: () -> Unit,
        val libraryEntryVisibilityClick: (Int) -> Unit,
    ) : DisplaySheetScreen()
}

@Composable
fun DisplayScreenSheet(
    currentScreen: DisplaySheetScreen,
    categories: PersistentList<CategoryItem>,
    isList: Boolean,
    libraryEntryVisibility: Int,
    addNewCategory: (String) -> Unit,
    contentPadding: PaddingValues,
    closeSheet: () -> Unit,
) {

    when (currentScreen) {
        is DisplaySheetScreen.BrowseDisplayOptionsSheet -> {
            BrowseDisplayOptionsSheet(
                showIsList = currentScreen.showIsList,
                isList = isList,
                switchDisplayClick = currentScreen.switchDisplayClick,
                currentLibraryEntryVisibility = libraryEntryVisibility,
                libraryEntryVisibilityClick = currentScreen.libraryEntryVisibilityClick,
                bottomContentPadding = contentPadding.calculateBottomPadding(),
            )
        }
        is DisplaySheetScreen.CategoriesSheet ->
            EditCategorySheet(
                addingToLibrary = currentScreen.addingToLibrary,
                categories = categories,
                cancelClick = closeSheet,
                bottomContentPadding = contentPadding.calculateBottomPadding(),
                addNewCategory = addNewCategory,
                confirmClicked = currentScreen.setCategories,
            )
    }
}
