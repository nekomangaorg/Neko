package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import org.nekomanga.domain.category.CategoryItem

@Composable
fun LibraryPage(
    contentPadding: PaddingValues,
    selectionMode: Boolean,
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
    libraryCategoryActions: LibraryCategoryActions,
    categorySortClick: (CategoryItem) -> Unit,
) {
    if (libraryScreenState.horizontalCategories) {
        HorizontalCategoriesPage(
            contentPadding = contentPadding,
            selectionMode = selectionMode,
            libraryScreenState = libraryScreenState,
            libraryScreenActions = libraryScreenActions,
            libraryCategoryActions = libraryCategoryActions,
            categorySortClick = categorySortClick,
        )
    } else {
        VerticalCategoriesPage(
            contentPadding = contentPadding,
            selectionMode = selectionMode,
            libraryScreenState = libraryScreenState,
            libraryScreenActions = libraryScreenActions,
            libraryCategoryActions = libraryCategoryActions,
            categorySortClick = categorySortClick,
        )
    }
}
