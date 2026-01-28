package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaGridWithHeader(
    groupedManga: ImmutableMap<Int, PersistentList<DisplayManga>>,
    shouldOutlineCover: Boolean,
    columns: Int,
    modifier: Modifier = Modifier,
    isComfortable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    onLongClick: (DisplayManga) -> Unit = {},
) {
    // Optimize: Memoize chunking to avoid re-allocating lists on every recomposition.
    val chunkedGroupedManga =
        remember(groupedManga, columns) {
            groupedManga.mapValues { (_, list) -> list.chunked(columns) }
        }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        groupedManga.forEach { (stringRes, allGrids) ->
            if (allGrids.isNotEmpty()) {
                item(key = "header-$stringRes") {
                    HeaderCard { DefaultHeaderText(stringResource(id = stringRes)) }
                }

                val chunks = chunkedGroupedManga[stringRes] ?: emptyList()
                itemsIndexed(items = chunks, key = { index, _ -> "grid-row-$stringRes-$index" }) {
                    gridIndex,
                    rowItems ->
                    VerticalGrid(
                        columns = SimpleGridCells.Fixed(columns),
                        modifier = modifier.fillMaxWidth().padding(horizontal = Size.small),
                        horizontalArrangement = Arrangement.spacedBy(Size.small),
                    ) {
                        rowItems.forEach { displayManga ->
                            MangaGridItem(
                                displayManga = displayManga,
                                shouldOutlineCover = shouldOutlineCover,
                                isComfortable = isComfortable,
                                onClick = { onClick(displayManga.mangaId) },
                                onLongClick = { onLongClick(displayManga) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaGrid(
    mangaList: List<DisplayManga>,
    shouldOutlineCover: Boolean,
    columns: Int,
    contentPadding: PaddingValues = PaddingValues(),
    isComfortable: Boolean = true,
    onClick: (Long) -> Unit = {},
    onLongClick: (DisplayManga) -> Unit = {},
    lastPage: Boolean = true,
    loadNextItems: () -> Unit = {},
) {
    val cells = GridCells.Fixed(columns)

    val scrollState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = cells,
        state = scrollState,
        modifier = Modifier.fillMaxSize().padding(Size.tiny),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(mangaList, key = { _, display -> display.mangaId }) { index, displayManga ->
            LaunchedEffect(scrollState) {
                if (!lastPage && index >= mangaList.size - 1) {
                    loadNextItems()
                }
            }

            MangaGridItem(
                displayManga = displayManga,
                shouldOutlineCover = shouldOutlineCover,
                isComfortable = isComfortable,
                onClick = { onClick(displayManga.mangaId) },
                onLongClick = { onLongClick(displayManga) },
            )
        }
    }
}

@Composable
fun MangaGridItem(
    modifier: Modifier = Modifier,
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    showUnreadBadge: Boolean = false,
    showDownloadBadge: Boolean = false,
    unreadCount: Int = 0,
    downloadCount: Int = 0,
    isComfortable: Boolean = true,
    isSelected: Boolean = false,
    showStartReadingButton: Boolean = false,
    onStartReadingClick: () -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier.padding(start = 2.dp, top = 2.dp)) {
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(Shapes.coverRadius))
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                        .padding(Size.extraTiny)
            ) {
                val subtitleText =
                    when (displayManga.displayTextRes) {
                        null -> displayManga.displayText
                        else -> stringResource(displayManga.displayTextRes)
                    }

                if (isComfortable) {
                    Column {
                        ComfortableGridItem(
                            manga = displayManga,
                            subtitleText = subtitleText,
                            shouldOutlineCover = shouldOutlineCover,
                        )
                    }
                } else {
                    Box { CompactGridItem(displayManga, subtitleText, shouldOutlineCover) }
                }
                if (showStartReadingButton) {
                    StartReadingButton(
                        modifier = Modifier.align(Alignment.TopEnd),
                        onStartReadingClick = onStartReadingClick,
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier =
                        Modifier.matchParentSize()
                            .clip(RoundedCornerShape(Shapes.coverRadius))
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            )
                )
            }
        }

        if (displayManga.inLibrary) {
            InLibraryBadge(shouldOutlineCover, offset = 0.dp)
        }
        if ((showUnreadBadge && unreadCount > 0) || (showDownloadBadge && downloadCount > 0)) {
            DownloadUnreadBadge(
                outline = shouldOutlineCover,
                showUnread = showUnreadBadge,
                unreadCount = unreadCount,
                showDownloads = showDownloadBadge,
                downloadCount = downloadCount,
                offset = 0.dp,
            )
        }
    }
}

@Composable
fun ComfortableGridItem(
    modifier: Modifier = Modifier,
    manga: DisplayManga,
    subtitleText: String,
    shouldOutlineCover: Boolean,
) {
    MangaCover.Book.invoke(
        artwork = manga.currentArtwork,
        shouldOutlineCover = shouldOutlineCover,
        modifier = modifier,
    )
    MangaGridTitle(title = manga.getTitle(), hasSubtitle = subtitleText.isNotBlank())

    MangaGridSubtitle(subtitleText = subtitleText)
}

@Composable
fun BoxScope.CompactGridItem(
    manga: DisplayManga,
    subtitleText: String,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
) {
    MangaCover.Book.invoke(
        artwork = manga.currentArtwork,
        shouldOutlineCover = shouldOutlineCover,
        modifier = modifier,
    )

    Box(
        modifier =
            Modifier.background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(NekoColors.veryLowContrast),
                                    Color.Black.copy(NekoColors.highAlphaLowContrast),
                                )
                        ),
                    shape =
                        RoundedCornerShape(
                            bottomStart = Shapes.coverRadius,
                            bottomEnd = Shapes.coverRadius,
                        ),
                )
                .matchParentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
            MangaGridTitle(
                title = manga.getTitle(),
                hasSubtitle = subtitleText.isNotBlank(),
                isComfortable = false,
            )
            MangaGridSubtitle(subtitleText = subtitleText, isComfortable = false)
        }
    }
}

@Composable
fun MangaGridTitle(
    title: String,
    maxLines: Int = 3,
    isComfortable: Boolean = true,
    hasSubtitle: Boolean = false,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isComfortable) MaterialTheme.colorScheme.onSurface else Color.White,
        fontWeight = FontWeight.Medium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.padding(
                top = Size.tiny,
                bottom = if (hasSubtitle) Size.none else Size.tiny,
                start = Size.tiny,
                end = Size.tiny,
            ),
    )
}

@Composable
fun MangaGridSubtitle(subtitleText: String, isComfortable: Boolean = true) {
    if (subtitleText.isNotBlank()) {
        Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color =
                if (isComfortable) MaterialTheme.colorScheme.onSurface
                else Color.White.copy(alpha = NekoColors.mediumAlphaLowContrast),
            fontWeight = if (isComfortable) FontWeight.Normal else FontWeight.SemiBold,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.padding(
                    top = Size.none,
                    bottom = Size.tiny,
                    start = Size.tiny,
                    end = Size.tiny,
                ),
        )
    }
}
