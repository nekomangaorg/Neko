package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.image.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.util.system.toMangaCacheKey
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.theme.Shapes

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
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        groupedManga.forEach { (stringRes, allGrids) ->
            stickyHeader {
                HeaderCard(stringResource(id = stringRes))
            }
            gridItems(
                items = allGrids,
                columns = columns,
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    loadNextItems: () -> Unit = {},
) {
    val cells = GridCells.Fixed(columns)

    val scrollState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = cells,
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(mangaList, key = { _, display -> display.mangaId }) { index, displayManga ->

            LaunchedEffect(scrollState) {
                if (index >= mangaList.size - 1) {
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
private fun MangaGridItem(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    isComfortable: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    CompositionLocalProvider(LocalRippleTheme provides PrimaryColorRippleTheme) {
        Box {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Shapes.coverRadius))
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    .padding(2.dp),
            ) {
                if (isComfortable) {
                    ComfortableGridItem(
                        displayManga,
                        displayManga.displayText,
                        shouldOutlineCover,
                    )
                } else {
                    CompactGridItem(
                        displayManga,
                        displayManga.displayText,
                        shouldOutlineCover,
                    )
                }
            }

            if (displayManga.inLibrary) {
                val offset = (-2).dp
                InLibraryBadge(offset, shouldOutlineCover)
            }
        }
    }
}

@Composable
private fun ComfortableGridItem(
    manga: DisplayManga,
    displayText: String,
    shouldOutlineCover: Boolean,
) {
    Column {
        GridCover(manga, shouldOutlineCover)
        MangaTitle(
            title = manga.title,
            modifier = Modifier.padding(
                top = 0.dp,
                bottom = if (displayText.isNotBlank()) 0.dp else 4.dp,
                start = 4.dp,
                end = 4.dp,
            ),
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            fontWeight = FontWeight.Medium,
        )

        if (displayText.isNotBlank()) {
            DisplayText(
                displayText = displayText,
                modifier = Modifier.padding(
                    top = 0.dp,
                    bottom = 4.dp,
                    start = 4.dp,
                    end = 4.dp,
                ),
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            )
        }
    }
}

@Composable
private fun CompactGridItem(
    manga: DisplayManga,
    displayText: String,
    shouldOutlineCover: Boolean,
) {
    Box {
        GridCover(manga, shouldOutlineCover)
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = .95f)),
                    ),
                    shape = RoundedCornerShape(
                        bottomStart = Shapes.coverRadius,
                        bottomEnd = Shapes.coverRadius,
                    ),
                )
                .padding(top = 6.dp),
        ) {
            MangaTitle(
                title = manga.title,
                modifier = Modifier.padding(
                    top = 4.dp,
                    bottom = if (displayText.isNotBlank()) 0.dp else 4.dp,
                    start = 4.dp,
                    end = 4.dp,
                ),
                fontWeight = FontWeight.Medium,
            )

            if (displayText.isNotBlank()) {
                DisplayText(
                    displayText = displayText,
                    modifier = Modifier
                        .padding(
                            top = 0.dp,
                            bottom = 4.dp,
                            start = 4.dp,
                            end = 4.dp,
                        ),
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun GridCover(manga: DisplayManga, shouldOutlineCover: Boolean) {
    val color by remember { mutableStateOf(Pastel.getColorLight()) }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(manga.currentArtwork)
            .memoryCacheKey(manga.mangaId.toMangaCacheKey())
            .placeholder(color)
            .setParameter(MangaCoverFetcher.useCustomCover, false)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(Shapes.coverRadius))
            .conditional(shouldOutlineCover) {
                this.border(width = Outline.thickness, color = Outline.color, shape = RoundedCornerShape(Shapes.coverRadius))
            },
    )
}
