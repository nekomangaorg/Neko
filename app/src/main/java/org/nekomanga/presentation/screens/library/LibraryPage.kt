package org.nekomanga.presentation.screens.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.MangaRow
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryPage(
    contentPadding: PaddingValues,
    libraryScreenState: LibraryScreenState,
    libraryCategoryActions: LibraryCategoryActions,
    categorySortClick: (CategoryItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.wrapContentWidth(align = Alignment.CenterHorizontally),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item { Gap(Size.small) }
        libraryScreenState.items.forEach { item ->
            item(item.categoryItem.id) {
                LibraryCategoryHeader(
                    categoryItem = item.categoryItem,
                    enabled = !item.libraryItems.isEmpty(),
                    categoryItemClick = {
                        libraryCategoryActions.categoryItemClick(item.categoryItem)
                    },
                    categorySortClick = { categorySortClick(item.categoryItem) },
                    categoryRefreshClick = {
                        libraryCategoryActions.categoryRefreshClick(item.categoryItem)
                    },
                )
            }
            if (!item.categoryItem.isHidden) {
                items(
                    item.libraryItems,
                    key = { libraryItem -> libraryItem.displayManga.title + item.categoryItem.name },
                ) { libraryItem ->
                    MangaRow(
                        modifier = Modifier.fillMaxWidth(),
                        displayManga = libraryItem.displayManga,
                        shouldOutlineCover = libraryScreenState.outlineCovers,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCategoryHeader(
    categoryItem: CategoryItem,
    categoryItemClick: () -> Unit,
    categorySortClick: () -> Unit,
    categoryRefreshClick: () -> Unit,
    enabled: Boolean,
) {

    // Animate the background color to provide a smooth transition for the disabled state
    val textColor by
        animateColorAsState(
            targetValue =
                if (enabled) MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = NekoColors.disabledAlphaLowContrast
                    )
        )

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled, onClick = categoryItemClick)
                .padding(vertical = Size.extraTiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
                if (categoryItem.isHidden) Icons.Default.ArrowDropDown
                else Icons.Default.ArrowDropUp,
            contentDescription = null,
            tint = textColor,
        )
        Text(
            text = categoryItem.name,
            color = textColor,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(enabled = enabled, onClick = categorySortClick) {
            Text(
                text = stringResource(categoryItem.sortOrder.stringRes(categoryItem.isDynamic)),
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
            )
            Gap(Size.extraTiny)
            Icon(
                imageVector =
                    if (categoryItem.isAscending) Icons.Default.ArrowDownward
                    else Icons.Default.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.size(Size.mediumLarge),
            )
        }
        IconButton(
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(contentColor = textColor),
            onClick = categoryRefreshClick,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(Size.mediumLarge),
            )
        }
    }
}
