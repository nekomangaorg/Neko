package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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

@Composable
fun LibraryPage(
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
                        items(items = item.libraryItems.chunked(columns)) { rowItems ->
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
                            key = { index, libraryItem ->
                                libraryItem.displayManga.title + item.categoryItem.name
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
            index == 0 && totalSize >= 1 -> ListCardType.Top
            index == totalSize - 1 -> ListCardType.Bottom
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
            shouldOutlineCover = libraryScreenState.outlineCovers,
        )
    }
}

@Composable
private fun LibraryCategoryHeader(
    categoryItem: CategoryItem,
    isRefreshing: Boolean,
    isCollapsible: Boolean,
    selectionMode: Boolean,
    allSelected: Boolean,
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

    val iconColor by
        animateColorAsState(
            targetValue =
                if (enabled) MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = NekoColors.disabledAlphaLowContrast
                    )
        )

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = isCollapsible || selectionMode, onClick = categoryItemClick)
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
                tint = iconColor,
            )
        }
        Gap(Size.small)

        val text =
            when {
                !isCollapsible && categoryItem.isSystemCategory -> ""
                else -> categoryItem.name
            }

        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.titleLarge,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            enabled = enabled,
            shapes =
                ButtonShapes(
                    shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
                ),
            onClick = categorySortClick,
        ) {
            Text(
                text = stringResource(categoryItem.sortOrder.stringRes(categoryItem.isDynamic)),
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
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
                modifier = Modifier.size(Size.large),
            )
        }
        Gap(ButtonGroupDefaults.ConnectedSpaceBetween)

        AnimatedContent(targetState = isRefreshing) { targetState ->
            when (targetState) {
                true -> {
                    val strokeWidth = with(LocalDensity.current) { Size.tiny.toPx() }
                    val stroke =
                        remember(strokeWidth) { Stroke(width = strokeWidth, cap = StrokeCap.Round) }
                    IconButton(
                        enabled = false,
                        shapes =
                            IconButtonShapes(
                                shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                                pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                            ),
                        colors =
                            IconButtonDefaults.iconButtonColors(disabledContentColor = textColor),
                        onClick = {},
                    ) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(Size.large),
                            trackStroke = stroke,
                            stroke = stroke,
                        )
                    }
                }

                false -> {

                    IconButton(
                        enabled = enabled,
                        shapes =
                            IconButtonShapes(
                                shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                                pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                            ),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = textColor),
                        onClick = categoryRefreshClick,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(Size.large),
                        )
                    }
                }
            }
        }
    }
}
