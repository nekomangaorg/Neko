package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaGridWithHeader(
    groupedManga: ImmutableMap<Int, ImmutableList<DisplayManga>>,
    shouldOutlineCover: Boolean,
    columns: Int,
    modifier: Modifier = Modifier,
    isComfortable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    onLongClick: (DisplayManga) -> Unit = {},
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        groupedManga.forEach { (stringRes, allGrids) ->
            stickyHeader { HeaderCard(stringResource(id = stringRes)) }
            gridItems(
                items = allGrids,
                columns = columns,
                modifier = Modifier.padding(horizontal = Size.small),
                horizontalArrangement = Arrangement.spacedBy(Size.small),
            ) { displayManga ->
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
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    isComfortable: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier.clip(RoundedCornerShape(Shapes.coverRadius))
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    .padding(Size.extraTiny),
        ) {
            val subtitleText =
                when (displayManga.displayTextRes) {
                    null -> displayManga.displayText
                    else -> stringResource(displayManga.displayTextRes)
                }

            if (isComfortable) {
                Column {
                    ComfortableGridItem(
                        displayManga,
                        subtitleText,
                        shouldOutlineCover,
                    )
                }
            } else {
                Box {
                    CompactGridItem(
                        displayManga,
                        subtitleText,
                        shouldOutlineCover,
                    )
                }
            }
        }

        if (displayManga.inLibrary) {
            InLibraryBadge(shouldOutlineCover)
        }
    }
}

@Composable
fun ColumnScope.ComfortableGridItem(
    manga: DisplayManga,
    subtitleText: String,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
) {
    MangaCover.Book.invoke(
        manga = manga,
        shouldOutlineCover = shouldOutlineCover,
        modifier = modifier,
    )
    MangaGridTitle(
        title = manga.title,
        hasSubtitle = subtitleText.isNotBlank(),
    )

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
        manga = manga,
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
                                    Color.Black.copy(NekoColors.highAlphaLowContrast)),
                        ),
                    shape =
                        RoundedCornerShape(
                            bottomStart = Shapes.coverRadius,
                            bottomEnd = Shapes.coverRadius,
                        ),
                )
                .matchParentSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
        ) {
            MangaGridTitle(
                title = manga.title,
                hasSubtitle = subtitleText.isNotBlank(),
                isComfortable = false,
            )
            MangaGridSubtitle(
                subtitleText = subtitleText,
                isComfortable = false,
            )
        }
    }
}

@Composable
fun MangaGridTitle(
    title: String,
    maxLines: Int = 2,
    isComfortable: Boolean = true,
    hasSubtitle: Boolean = false,
) {
    Text(
        text = title,
        style =
            if (isComfortable) MaterialTheme.typography.bodySmall
            else MaterialTheme.typography.bodyMedium,
        color = if (isComfortable) MaterialTheme.colorScheme.onSurface else Color.White,
        fontWeight = FontWeight.Medium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.padding(
                top = Size.tiny,
                bottom = if (hasSubtitle) Size.none else Size.tiny,
                start = 6.dp,
                end = 6.dp,
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
                    start = 6.dp,
                    end = 6.dp,
                ),
        )
    }
}
