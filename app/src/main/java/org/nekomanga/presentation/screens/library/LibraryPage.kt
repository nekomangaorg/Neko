package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.LibrarySort
import eu.kanade.tachiyomi.ui.library.LibraryViewType
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.presentation.components.MangaGridItem
import org.nekomanga.presentation.components.MangaRow
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryPage(
    contentPadding: PaddingValues,
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
    libraryCategoryActions: LibraryCategoryActions,
    categorySortClick: (CategoryItem) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    val columns =
        numberOfColumns(
            rawValue =
                (libraryScreenState.libraryViewType as? LibraryViewType.Grid)?.rawColumnCount ?: 0f
        )

    // val width = (LocalConfiguration.current.screenWidthDp / columns).dp - Size.smedium

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        libraryScreenState.items.forEach { item ->
            item(item.categoryItem.name) {
                LibraryCategoryHeader(
                    categoryItem = item.categoryItem,
                    enabled = !item.libraryItems.isEmpty(),
                    isRefreshing = item.isRefreshing,
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
                when (libraryScreenState.libraryViewType) {
                    is LibraryViewType.Grid -> {
                        items(items = item.libraryItems.chunked(columns)) { rowItems ->
                            RowGrid(
                                rowItems = rowItems,
                                libraryScreenState = libraryScreenState,
                                columns = columns,
                                isComfortableGrid =
                                    libraryScreenState.libraryViewType.gridType ==
                                        LibraryViewType.GridType.Comfortable,
                                libraryScreenActions = libraryScreenActions,
                            )
                        }
                    }

                    LibraryViewType.List -> {
                        itemsIndexed(
                            item.libraryItems,
                            key = { index, libraryItem ->
                                libraryItem.displayManga.title + item.categoryItem.name
                            },
                        ) { index, libraryItem ->
                            ListItem(
                                index = index,
                                totalSize = item.libraryItems.size,
                                libraryScreenState = libraryScreenState,
                                libraryItem = libraryItem,
                                libraryScreenActions = libraryScreenActions,
                            )
                            Gap(Size.tiny)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowGrid(
    rowItems: List<LibraryMangaItem>,
    libraryScreenState: LibraryScreenState,
    columns: Int,
    isComfortableGrid: Boolean,
    libraryScreenActions: LibraryScreenActions,
) {
    VerticalGrid(
        columns = SimpleGridCells.Fixed(columns),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
        horizontalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        rowItems.forEach { libraryItem ->
            MangaGridItem(
                displayManga = libraryItem.displayManga,
                showUnreadBadge = libraryScreenState.showUnreadBadges,
                unreadCount = libraryItem.unreadCount,
                showDownloadBadge = libraryScreenState.showDownloadBadges,
                downloadCount = libraryItem.downloadCount,
                shouldOutlineCover = libraryScreenState.outlineCovers,
                isComfortable = isComfortableGrid,
                onClick = { libraryScreenActions.mangaClick(libraryItem.displayManga.mangaId) },
                onLongClick = {},
            )
        }
    }
}

@Composable
private fun ListItem(
    index: Int,
    totalSize: Int,
    libraryScreenState: LibraryScreenState,
    libraryItem: LibraryMangaItem,
    libraryScreenActions: LibraryScreenActions,
) {
    val listCardType =
        when {
            index == 0 && totalSize > 1 -> ListCardType.Top
            index == totalSize - 1 -> ListCardType.Bottom
            else -> ListCardType.Center
        }
    ExpressiveListCard(
        modifier = Modifier.padding(horizontal = Size.small),
        listCardType = listCardType,
    ) {
        MangaRow(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(
                        onClick = {
                            libraryScreenActions.mangaClick(libraryItem.displayManga.mangaId)
                        }
                    ),
            displayManga = libraryItem.displayManga,
            showUnreadBadge = libraryScreenState.showUnreadBadges,
            unreadCount = libraryItem.unreadCount,
            showDownloadBadge = libraryScreenState.showDownloadBadges,
            downloadCount = libraryItem.downloadCount,
            shouldOutlineCover = libraryScreenState.outlineCovers,
        )
    }
}

@Composable
private fun LibraryCategoryHeader(
    categoryItem: CategoryItem,
    isRefreshing: Boolean,
    categoryItemClick: () -> Unit,
    categorySortClick: () -> Unit,
    categoryRefreshClick: () -> Unit,
    enabled: Boolean,
) {

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
                .padding(vertical = Size.extraTiny, horizontal = Size.small),
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
            style = MaterialTheme.typography.titleMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
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
                    when {
                        categoryItem.sortOrder == LibrarySort.DragAndDrop ->
                            LibrarySort.DragAndDrop.composeIcon()

                        categoryItem.isAscending -> Icons.Default.ArrowDownward
                        else -> Icons.Default.ArrowUpward
                    },
                contentDescription = null,
                modifier = Modifier.size(Size.mediumLarge),
            )
        }

        AnimatedContent(targetState = isRefreshing) { targetState ->
            when (targetState) {
                true -> {
                    IconButton(
                        enabled = false,
                        colors =
                            IconButtonDefaults.iconButtonColors(disabledContentColor = textColor),
                        onClick = {},
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Size.medium),
                            strokeWidth = Size.extraTiny,
                        )
                    }
                }

                false -> {
                    IconButton(
                        modifier = Modifier.size(Size.mediumLarge),
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
        }
    }
}
