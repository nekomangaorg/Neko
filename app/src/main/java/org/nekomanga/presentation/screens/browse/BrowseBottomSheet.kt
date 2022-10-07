package org.nekomanga.presentation.screens.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenState
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.sheets.EditCategorySheet

/**
 * Sealed class that holds the types of bottom sheets the details screen can show
 */
sealed class BrowseBottomSheetScreen {
    class CategoriesSheet(
        val addingToLibrary: Boolean = true,
        val setCategories: (List<CategoryItem>) -> Unit,
    ) : BrowseBottomSheetScreen()

    class FilterSheet : BrowseBottomSheetScreen()
}

@Composable
fun BrowseBottomSheet(
    currentScreen: BrowseBottomSheetScreen,
    browseScreenState: State<BrowseScreenState>,
    addNewCategory: (String) -> Unit,
    bottomPadding: Dp,
    closeSheet: () -> Unit,
) {
    when (currentScreen) {
        is BrowseBottomSheetScreen.CategoriesSheet -> EditCategorySheet(
            addingToLibrary = currentScreen.addingToLibrary,
            categories = browseScreenState.value.categories,
            bottomPadding = bottomPadding,
            cancelClick = closeSheet,
            addNewCategory = addNewCategory,
            confirmClicked = currentScreen.setCategories,
        )
        else -> Unit
    }
}
