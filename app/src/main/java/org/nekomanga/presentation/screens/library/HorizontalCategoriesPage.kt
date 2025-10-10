package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryDisplayMode
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.presentation.components.MangaGridItem
import org.nekomanga.presentation.components.MangaRow
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.theme.Size

@Composable
fun HorizontalCategoriesPage(
    contentPadding: PaddingValues,
    selectionMode: Boolean,
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
    libraryCategoryActions: LibraryCategoryActions,
    categorySortClick: (CategoryItem) -> Unit,
) {
    val pagerState =
        rememberPagerState(initialPage = 0, initialPageOffsetFraction = 0f) {
            libraryScreenState.items.size
        }
    val scope = rememberCoroutineScope()
    val columns = numberOfColumns(rawValue = libraryScreenState.rawColumnCount)
    val selectedIds =
        remember(libraryScreenState.selectedItems) {
            libraryScreenState.selectedItems.map { it.displayManga.mangaId }
        }

    val indicatorColor =
        if (libraryScreenState.useVividColorHeaders) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = Size.small,
            divider = {},
        ) {
            libraryScreenState.items.forEachIndexed { index, item ->
                val isSelected = pagerState.currentPage == index

                Tab(
                    text = {
                        Text(
                           item.categoryItem.name,
                            color =
                                if (isSelected) indicatorColor
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    selected = isSelected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            val item = libraryScreenState.items[page]

            val allSelected by
                remember(selectionMode, selectedIds) {
                    mutableStateOf(
                        item.libraryItems.isNotEmpty() &&
                            item.libraryItems
                                .map { it.displayManga.mangaId }
                                .all { id -> id in selectedIds }
                    )
                }
            when (libraryScreenState.libraryDisplayMode) {
                is LibraryDisplayMode.ComfortableGrid,
                is LibraryDisplayMode.CompactGrid -> {
                    Column {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable(
                                        enabled = selectionMode,
                                        onClick = {
                                            libraryScreenActions.selectAllLibraryMangaItems(
                                                item.libraryItems
                                            )
                                        },
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AnimatedVisibility(selectionMode) {
                                Icon(
                                    imageVector =
                                        if (allSelected) Icons.Default.CheckCircleOutline
                                        else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = indicatorColor,
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))

                            CategorySortButtons(
                                textColor = indicatorColor,
                                enabled = true,
                                categorySortClick = { categorySortClick(item.categoryItem) },
                                sortString =
                                    stringResource(
                                        item.categoryItem.sortOrder.stringRes(
                                            item.categoryItem.isDynamic
                                        )
                                    ),
                                isAscending = item.categoryItem.isAscending,
                                categoryIsRefreshing = item.isRefreshing,
                                ascendingClick = {
                                    libraryCategoryActions.categoryAscendingClick(item.categoryItem)
                                },
                                categoryRefreshClick = {
                                    libraryCategoryActions.categoryRefreshClick(item.categoryItem)
                                },
                            )
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize().padding(horizontal = Size.small),
                            horizontalArrangement = Arrangement.spacedBy(Size.small),
                            contentPadding =
                                PaddingValues(
                                    bottom = contentPadding.calculateBottomPadding(),
                                    top = Size.small,
                                ),
                        ) {
                            items(items = item.libraryItems, key = { it.displayManga.mangaId }) {
                                libraryItem ->
                                MangaGridItem(
                                    displayManga = libraryItem.displayManga,
                                    showUnreadBadge = libraryScreenState.showUnreadBadges,
                                    unreadCount = libraryItem.unreadCount,
                                    showDownloadBadge = libraryScreenState.showDownloadBadges,
                                    downloadCount = libraryItem.downloadCount,
                                    shouldOutlineCover = libraryScreenState.outlineCovers,
                                    isComfortable =
                                        libraryScreenState.libraryDisplayMode
                                            is LibraryDisplayMode.ComfortableGrid,
                                    isSelected =
                                        selectedIds.contains(libraryItem.displayManga.mangaId),
                                    showStartReadingButton =
                                        libraryScreenState.showStartReadingButton &&
                                            libraryItem.unreadCount > 0,
                                    onStartReadingClick = {
                                        libraryScreenActions.mangaStartReadingClick(
                                            libraryItem.displayManga.mangaId
                                        )
                                    },
                                    onClick = {
                                        if (libraryScreenState.selectedItems.isNotEmpty()) {
                                            libraryScreenActions.mangaLongClick(libraryItem)
                                        } else {
                                            libraryScreenActions.mangaClick(
                                                libraryItem.displayManga.mangaId
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        libraryScreenActions.mangaLongClick(libraryItem)
                                    },
                                )
                            }
                        }
                    }
                }
                is LibraryDisplayMode.List -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable(
                                        enabled = selectionMode,
                                        onClick = {
                                            libraryScreenActions.selectAllLibraryMangaItems(
                                                item.libraryItems
                                            )
                                        },
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Gap(Size.medium)
                            AnimatedVisibility(selectionMode) {
                                Icon(
                                    imageVector =
                                        if (allSelected) Icons.Default.CheckCircleOutline
                                        else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))

                            CategorySortButtons(
                                enabled = true,
                                categorySortClick = { categorySortClick(item.categoryItem) },
                                sortString =
                                    stringResource(
                                        item.categoryItem.sortOrder.stringRes(
                                            item.categoryItem.isDynamic
                                        )
                                    ),
                                isAscending = item.categoryItem.isAscending,
                                categoryIsRefreshing = item.isRefreshing,
                                ascendingClick = {
                                    libraryCategoryActions.categoryAscendingClick(item.categoryItem)
                                },
                                categoryRefreshClick = {
                                    libraryCategoryActions.categoryRefreshClick(item.categoryItem)
                                },
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                        ) {
                            itemsIndexed(
                                items = item.libraryItems,
                                key = { _, libraryItem -> libraryItem.displayManga.mangaId },
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
            showStartReadingButton =
                libraryScreenState.showStartReadingButton && libraryItem.unreadCount > 0,
            onStartReadingClick = {
                libraryScreenActions.mangaStartReadingClick(libraryItem.displayManga.mangaId)
            },
            unreadCount = libraryItem.unreadCount,
            showDownloadBadge = libraryScreenState.showDownloadBadges,
            downloadCount = libraryItem.downloadCount,
            shouldOutlineCover = libraryScreenState.outlineCovers,
        )
    }
}
