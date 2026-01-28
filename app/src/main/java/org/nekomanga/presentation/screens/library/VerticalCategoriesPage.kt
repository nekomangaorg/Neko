package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryDisplayMode
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.LibrarySort
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
import org.nekomanga.ui.theme.ThemePreviews
import org.nekomanga.ui.theme.ThemedPreviews

@Composable
fun VerticalCategoriesPage(
    contentPadding: PaddingValues,
    selectionMode: Boolean,
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
    libraryCategoryActions: LibraryCategoryActions,
    categorySortClick: (CategoryItem) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    val columns = numberOfColumns(rawValue = libraryScreenState.rawColumnCount)

    val collapsible by
        remember(libraryScreenState.items.size) {
            mutableStateOf(libraryScreenState.items.size > 1)
        }

    val selectedIds by
        remember(libraryScreenState.selectedItems) {
            mutableStateOf(libraryScreenState.selectedItems.map { it.displayManga.mangaId })
        }

    // Optimize: Cache chunked items to avoid re-chunking on every scroll/recomposition
    // caused by frequent updates to LibraryScreenState (e.g. scroll positions).
    val chunkedCategories =
        remember(libraryScreenState.items, columns) {
            libraryScreenState.items.associateWith { item -> item.libraryItems.chunked(columns) }
        }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        libraryScreenState.items.forEach { item ->
            item(key = "header-${item.categoryItem.name}-${item.categoryItem.id}") {
                LibraryCategoryHeader(
                    categoryItem = item.categoryItem,
                    useVividColorHeaders = libraryScreenState.useVividColorHeaders,
                    enabled = !item.libraryItems.isEmpty(),
                    total = item.libraryItems.size,
                    isRefreshing = item.isRefreshing,
                    selectionMode = selectionMode,
                    allSelected =
                        item.libraryItems.isNotEmpty() &&
                            item.libraryItems
                                .map { it.displayManga.mangaId }
                                .all { id -> id in selectedIds },
                    isCollapsible = collapsible,
                    categoryItemClick = {
                        if (selectionMode) {
                            libraryScreenActions.selectAllLibraryMangaItems(item.libraryItems)
                        } else {
                            libraryCategoryActions.categoryItemClick(item.categoryItem)
                        }
                    },
                    categorySortClick = { categorySortClick(item.categoryItem) },
                    categoryAscendingClick = {
                        libraryCategoryActions.categoryAscendingClick(item.categoryItem)
                    },
                    categoryRefreshClick = {
                        libraryCategoryActions.categoryRefreshClick(item.categoryItem)
                    },
                )
                Gap(Size.tiny)
            }

            if (!item.categoryItem.isHidden || !collapsible) {
                when (libraryScreenState.libraryDisplayMode) {
                    is LibraryDisplayMode.ComfortableGrid,
                    is LibraryDisplayMode.CompactGrid -> {
                        itemsIndexed(
                            items = chunkedCategories[item] ?: emptyList(),
                            key = { _, row ->
                                "grid-row-${item.categoryItem.name}-${row.joinToString { it.displayManga.mangaId.toString() }}"
                            },
                        ) { _, rowItems ->
                            RowGrid(
                                modifier = Modifier.animateItem(),
                                rowItems = rowItems,
                                selectedIds = selectedIds,
                                libraryScreenState = libraryScreenState,
                                columns = columns,
                                isComfortableGrid =
                                    libraryScreenState.libraryDisplayMode
                                        is LibraryDisplayMode.ComfortableGrid,
                                libraryScreenActions = libraryScreenActions,
                            )
                        }
                    }
                    LibraryDisplayMode.List -> {
                        itemsIndexed(
                            item.libraryItems,
                            key = { _, libraryItem ->
                                "list-${item.categoryItem.name}-${libraryItem.displayManga.mangaId}"
                            },
                        ) { index, libraryItem ->
                            ListItem(
                                index = index,
                                totalSize = item.libraryItems.size,
                                selectedIds = selectedIds,
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
    modifier: Modifier = Modifier,
    rowItems: List<LibraryMangaItem>,
    selectedIds: List<Long>,
    libraryScreenState: LibraryScreenState,
    columns: Int,
    isComfortableGrid: Boolean,
    libraryScreenActions: LibraryScreenActions,
) {
    VerticalGrid(
        columns = SimpleGridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth().padding(horizontal = Size.small),
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
                isSelected = selectedIds.contains(libraryItem.displayManga.mangaId),
                showStartReadingButton =
                    libraryScreenState.showStartReadingButton && libraryItem.unreadCount > 0,
                onStartReadingClick = {
                    libraryScreenActions.mangaStartReadingClick(libraryItem.displayManga.mangaId)
                },
                onClick = {
                    if (libraryScreenState.selectedItems.isNotEmpty()) {
                        libraryScreenActions.mangaLongClick(libraryItem)
                    } else {
                        libraryScreenActions.mangaClick(libraryItem.displayManga.mangaId)
                    }
                },
                onLongClick = { libraryScreenActions.mangaLongClick(libraryItem) },
            )
        }
    }
}

@Composable
private fun ListItem(
    modifier: Modifier = Modifier,
    index: Int,
    totalSize: Int,
    selectedIds: List<Long>,
    libraryScreenState: LibraryScreenState,
    libraryItem: LibraryMangaItem,
    libraryScreenActions: LibraryScreenActions,
) {
    val listCardType =
        when {
            index == 0 && totalSize > 1 -> ListCardType.Top
            index == totalSize - 1 && totalSize > 1 -> ListCardType.Bottom
            totalSize == 1 -> ListCardType.Single
            else -> ListCardType.Center
        }
    ExpressiveListCard(
        modifier = modifier.padding(horizontal = Size.small),
        listCardType = listCardType,
    ) {
        MangaRow(
            modifier =
                Modifier.fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (libraryScreenState.selectedItems.isNotEmpty()) {
                                libraryScreenActions.mangaLongClick(libraryItem)
                            } else {
                                libraryScreenActions.mangaClick(libraryItem.displayManga.mangaId)
                            }
                        },
                        onLongClick = { libraryScreenActions.mangaLongClick(libraryItem) },
                    ),
            displayManga = libraryItem.displayManga,
            isSelected = selectedIds.contains(libraryItem.displayManga.mangaId),
            showUnreadBadge = libraryScreenState.showUnreadBadges,
            unreadCount = libraryItem.unreadCount,
            showDownloadBadge = libraryScreenState.showDownloadBadges,
            downloadCount = libraryItem.downloadCount,
            showStartReadingButton =
                libraryScreenState.showStartReadingButton && libraryItem.unreadCount > 0,
            onStartReadingClick = {
                libraryScreenActions.mangaStartReadingClick(libraryItem.displayManga.mangaId)
            },
            shouldOutlineCover = libraryScreenState.outlineCovers,
        )
    }
}

@Composable
fun LibraryCategoryHeader(
    categoryItem: CategoryItem,
    useVividColorHeaders: Boolean,
    total: Int,
    isRefreshing: Boolean,
    isCollapsible: Boolean,
    selectionMode: Boolean,
    allSelected: Boolean,
    categoryItemClick: () -> Unit,
    categorySortClick: () -> Unit,
    categoryAscendingClick: () -> Unit,
    categoryRefreshClick: () -> Unit,
    enabled: Boolean,
) {

    val textColor by
        animateColorAsState(
            targetValue =
                if (enabled) {
                    if (useVividColorHeaders) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = NekoColors.disabledAlphaLowContrast
                    )
                }
        )

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(
                    enabled = enabled && (isCollapsible || selectionMode),
                    onClick = categoryItemClick,
                )
                .padding(vertical = Size.extraTiny, horizontal = Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(isCollapsible && !selectionMode) {
            Icon(
                imageVector =
                    if (categoryItem.isHidden) Icons.Default.ArrowDropDown
                    else Icons.Default.ArrowDropUp,
                contentDescription = null,
                tint = textColor,
            )
        }
        AnimatedVisibility(selectionMode) {
            Icon(
                imageVector =
                    if (allSelected) Icons.Default.CheckCircleOutline else Icons.Outlined.Circle,
                contentDescription = null,
                tint = textColor,
            )
        }
        Gap(Size.small)

        val text =
            when {
                !isCollapsible && categoryItem.isSystemCategory -> ""
                else -> categoryItem.name
            }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.titleLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
            )

            if (total > 0) {
                Text(
                    text = stringResource(org.nekomanga.R.string.total_items, total),
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }

        CategorySortButtons(
            enabled = enabled,
            categorySortClick = categorySortClick,
            sortString = stringResource(categoryItem.sortOrder.stringRes(categoryItem.isDynamic)),
            isAscending = categoryItem.isAscending,
            textColor = textColor,
            categoryIsRefreshing = isRefreshing,
            ascendingClick = categoryAscendingClick,
            categoryRefreshClick = categoryRefreshClick,
        )
    }
}

@ThemePreviews
@Composable
private fun LibraryCategoryHeaderPreview() {
    val categoryItem =
        CategoryItem(
            id = 1,
            name = "This is a very long category",
            sortOrder = LibrarySort.DateAdded,
            order = 0,
            isDynamic = false,
            isHidden = false,
            isAscending = true,
            isSystemCategory = true,
        )

    ThemedPreviews {
        LibraryCategoryHeader(
            categoryItem = categoryItem,
            useVividColorHeaders = false,
            total = 10,
            isRefreshing = false,
            isCollapsible = true,
            selectionMode = false,
            allSelected = false,
            categoryItemClick = {},
            categorySortClick = {},
            categoryAscendingClick = {},
            categoryRefreshClick = {},
            enabled = true,
        )
    }
}
