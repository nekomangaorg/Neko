package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.MangaRow
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryPage(
    contentPadding: PaddingValues,
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
) {
    LazyColumn(
        modifier = Modifier.wrapContentWidth(align = Alignment.CenterHorizontally),
        contentPadding = contentPadding,
    ) {
        libraryScreenState.items.forEach { item ->
            item(item.categoryItem.id) {
                LibraryCategoryHeader(
                    categoryItem = item.categoryItem,
                    categoryItemClick = {
                        libraryScreenActions.categoryItemClick(item.categoryItem)
                    },
                )
            }
            if (!item.categoryItem.isHidden) {
                items(
                    item.libraryItems,
                    key = { libraryItem -> libraryItem.displayManga.title + item.categoryItem.name },
                ) { libraryItem ->
                    MangaRow(
                        displayManga = libraryItem.displayManga,
                        shouldOutlineCover = libraryScreenState.outlineCovers,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCategoryHeader(categoryItem: CategoryItem, categoryItemClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = Size.medium)
                .clickable(onClick = categoryItemClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
                if (categoryItem.isHidden) Icons.Default.ArrowDropDown
                else Icons.Default.ArrowDropUp,
            contentDescription = null,
        )
        Text(text = categoryItem.name, maxLines = 1, style = MaterialTheme.typography.titleMedium)
    }
}
