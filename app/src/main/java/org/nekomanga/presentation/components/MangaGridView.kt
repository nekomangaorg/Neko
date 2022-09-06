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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.image.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.util.system.toMangaCacheKey
import org.nekomanga.presentation.screens.IconicsEmptyScreen
import org.nekomanga.presentation.theme.Shapes

@Composable
fun MangaGridWithHeader(
    groupedManga: Map<String, List<DisplayManga>>,
    shouldOutlineCover: Boolean,
    columns: Int,
    modifier: Modifier = Modifier,
    isComfortable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    onLongClick: (Long) -> Unit = {},
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        groupedManga.forEach { (headerText, allGrids) ->
            stickyHeader {
                HeaderCard(headerText)
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
                    onLongClick = { onLongClick(displayManga.mangaId) },
                )
            }
        }
    }
}

@Composable
fun PagingMangaGrid(
    mangaListPagingItems: LazyPagingItems<DisplayManga>,
    shouldOutlineCover: Boolean,
    columns: Int,
    contentPadding: PaddingValues = PaddingValues(),
    isComfortable: Boolean = true,
    onClick: (Long) -> Unit = {},
    onLongClick: (DisplayManga) -> Unit = {},
) {
    val cells = GridCells.Fixed(columns)

    var isLoading by remember { mutableStateOf(true) }
    var initialLoading = true

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            mangaListPagingItems.loadState.refresh == LoadState.Loading -> {
                initialLoading = true
                isLoading = true
            }
            mangaListPagingItems.loadState.append == LoadState.Loading -> {
                initialLoading = false
                isLoading = true
            }
            mangaListPagingItems.loadState.append is LoadState.Error && mangaListPagingItems.itemCount == 0 -> {
                isLoading = false
                IconicsEmptyScreen(
                    iconicImage = CommunityMaterial.Icon.cmd_compass_off,
                    iconSize = 176.dp,
                    message = stringResource(id = R.string.no_results_found),
                )
            }
            else -> {
                isLoading = false
            }
        }

        if (initialLoading) {
            Loading(
                isLoading,
                Modifier
                    .zIndex(1f)
                    .padding(8.dp)
                    .padding(top = contentPadding.calculateTopPadding())
                    .align(Alignment.TopCenter),
            )
        }

        LazyVerticalGrid(
            columns = cells,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(mangaListPagingItems) { displayManga ->
                displayManga?.let {
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
        if (initialLoading.not() && isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(8.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding())
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
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
) {
    val cells = GridCells.Fixed(columns)

    LazyVerticalGrid(
        columns = cells,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(mangaList) { displayManga ->
            MangaGridItem(
                displayManga = displayManga,
                shouldOutlineCover = shouldOutlineCover,
                isComfortable = isComfortable,
                onClick = { onClick(displayManga.mangaId) },
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
                val offset = (-6).dp
                Favorited(offset)
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
    val outlineModifier = when (shouldOutlineCover) {
        true -> Modifier.border(
            .75.dp, NekoColors.outline,
            RoundedCornerShape(Shapes.coverRadius),
        )
        else -> Modifier
    }
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
            .then(outlineModifier),
    )
}

