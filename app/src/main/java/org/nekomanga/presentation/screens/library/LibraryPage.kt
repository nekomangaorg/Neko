package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.LibrarySort
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.MangaRow
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryPage(
    contentPadding: PaddingValues,
    libraryScreenState: LibraryScreenState,
    libraryCategoryActions: LibraryCategoryActions,
    categorySortClick: (CategoryItem) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.wrapContentWidth(align = Alignment.CenterHorizontally),
        contentPadding = contentPadding,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item { Gap(Size.small) }
        libraryScreenState.items.forEach { item ->
            item(item.categoryItem.id) {
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
            item(key = "animated-content-${item.categoryItem.id}") {
                AnimatedVisibility(
                    visible = !item.categoryItem.isHidden,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                start = Size.small,
                                end = Size.small,
                                bottom = Size.small,
                            ),
                        verticalArrangement = Arrangement.spacedBy(Size.tiny),
                    ) {
                        item.libraryItems.forEachIndexed { index, libraryItem ->
                            val listCardType =
                                when {
                                    index == 0 && item.libraryItems.size > 1 -> ListCardType.Top
                                    index == item.libraryItems.size - 1 -> ListCardType.Bottom
                                    else -> ListCardType.Center
                                }
                            ExpressiveListCard(listCardType = listCardType) {
                                MangaRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    displayManga = libraryItem.displayManga,
                                    showUnreadBadge = true,
                                    unreadCount = libraryItem.unreadCount,
                                    showDownloadBadge = true,
                                    downloadCount = libraryItem.downloadCount,
                                    shouldOutlineCover = libraryScreenState.outlineCovers,
                                )
                            }
                        }
                    }
                }
            }
        }
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
