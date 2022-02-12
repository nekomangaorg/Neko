package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.ui.base.components.theme.Shapes

@Composable
fun MangaGridWithHeader(
    groupedManga: Map<String, List<DisplayManga>>,
    shouldOutlineCover: Boolean,
    columns: Int,
    modifier: Modifier = Modifier,
    isComfortable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Manga) -> Unit = {},
    onLongClick: (Manga) -> Unit = {},
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
            gridItems(items = allGrids,
                columns = columns,
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) { displayManga ->
                MangaGridItem(
                    displayManga = displayManga,
                    shouldOutlineCover = shouldOutlineCover,
                    isComfortable = isComfortable,
                    onClick = { onClick(displayManga.manga) },
                    onLongClick = { onLongClick(displayManga.manga) }
                )
            }
        }
    }
}

@Composable
fun PagingMangaGrid(
    mangaList: LazyPagingItems<DisplayManga>,
    shouldOutlineCover: Boolean,
    columns: Int,
    contentPadding: PaddingValues = PaddingValues(),
    isComfortable: Boolean = true,
    onClick: (Manga) -> Unit = {},
    onLongClick: (Manga) -> Unit = {},
) {
    val cells = GridCells.Fixed(columns)
    LazyVerticalGrid(
        cells = cells,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(mangaList) { displayManga ->
            displayManga?.let {
                MangaGridItem(
                    displayManga = displayManga,
                    shouldOutlineCover = shouldOutlineCover,
                    isComfortable = isComfortable,
                    onClick = { onClick(displayManga.manga) },
                    onLongClick = { onLongClick(displayManga.manga) }
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
    onClick: (Manga) -> Unit = {},
) {
    val cells = GridCells.Fixed(columns)

    LazyVerticalGrid(
        cells = cells,
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
                onClick = { onClick(displayManga.manga) },
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

    CompositionLocalProvider(LocalRippleTheme provides CoverRippleTheme) {
        Box {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Shapes.coverRadius))
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .padding(2.dp)
            ) {

                if (isComfortable) {
                    ComfortableGridItem(displayManga.manga,
                        displayManga.displayText,
                        shouldOutlineCover)
                } else {
                    CompactGridItem(
                        displayManga.manga,
                        displayManga.displayText,
                        shouldOutlineCover,
                    )
                }
            }

            if (displayManga.manga.favorite) {
                val offset = (-6).dp
                Favorited(offset)
            }
        }
    }
}

@Composable
private fun ComfortableGridItem(
    manga: Manga,
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
                end = 4.dp),
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            fontWeight = FontWeight.Medium)

        if (displayText.isNotBlank()) {
            DisplayText(
                displayText = displayText,
                modifier = Modifier.padding(
                    top = 0.dp,
                    bottom = 4.dp,
                    start = 4.dp,
                    end = 4.dp),
                fontSize = MaterialTheme.typography.bodySmall.fontSize)
        }
    }
}

@Composable
private fun CompactGridItem(
    manga: Manga,
    displayText: String,
    shouldOutlineCover: Boolean,
) {
    Box {
        GridCover(manga, shouldOutlineCover)
        Column(Modifier
            .fillMaxWidth()
            .align(Alignment.BottomStart)
            .background(Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = .95f))
            ),
                shape = RoundedCornerShape(bottomStart = Shapes.coverRadius,
                    bottomEnd = Shapes.coverRadius))
            .padding(top = 6.dp)) {
            MangaTitle(
                title = manga.title,
                modifier = Modifier.padding(
                    top = 4.dp,
                    bottom = if (displayText.isNotBlank()) 0.dp else 4.dp,
                    start = 4.dp,
                    end = 4.dp),
                fontWeight = FontWeight.Medium)

            if (displayText.isNotBlank()) {
                DisplayText(
                    displayText = displayText,
                    modifier = Modifier
                        .padding(
                            top = 0.dp,
                            bottom = 4.dp,
                            start = 4.dp,
                            end = 4.dp),
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun GridCover(manga: Manga, shouldOutlineCover: Boolean) {

    val outlineModifier = when (shouldOutlineCover) {
        true -> Modifier.border(.75.dp, NekoColors.outline,
            RoundedCornerShape(Shapes.coverRadius))
        else -> Modifier
    }

    Image(painter = rememberImagePainter(
        data = manga,
        builder = {
            placeholder(Pastel.getColorLight())
            transformations(RoundedCornersTransformation(0f))
        }),
        contentDescription = null,
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(Shapes.coverRadius))
            .then(outlineModifier),
        contentScale = ContentScale.Crop
    )
}

@Preview
@Composable
private fun MangaGridHeaderPreview() {
    val manga = Manga.create("test", "One Piece", 1L)
    val mangaList = listOf(DisplayManga(manga, "related"),
        DisplayManga(manga, "Sequel"),
        DisplayManga(manga),
        DisplayManga(manga),
        DisplayManga(manga))
    val mangaMap = mapOf("Test" to mangaList)
    MangaGridWithHeader(groupedManga = mangaMap, shouldOutlineCover = true, columns = 2)
}

@Preview
@Composable
private fun MangaComfotableGridPreview() {
    val manga = Manga.create("test", "One Piece", 1L)
    val mangaList = listOf(DisplayManga(manga, "related"),
        DisplayManga(manga, "Sequel"),
        DisplayManga(manga),
        DisplayManga(manga),
        DisplayManga(manga))
    MangaGrid(mangaList = mangaList, shouldOutlineCover = true, columns = 2, isComfortable = false)
}

@Preview
@Composable
private fun MangaCompactGridPreview() {
    val manga = Manga.create("test", "One Piece", 1L)
    val mangaList = listOf(DisplayManga(manga, "related"),
        DisplayManga(manga, "Sequel"),
        DisplayManga(manga),
        DisplayManga(manga),
        DisplayManga(manga))
    MangaGrid(mangaList = mangaList, shouldOutlineCover = true, columns = 2)
}

@Preview
@Composable
private fun MangaGridPreviewLongTitles() {
    val manga = Manga.create("test",
        "Really Really Long Title Really Really Long Title Really Really Long Title Really Really Long TitleReally Really Long Title",
        1L)
    val mangaList = listOf(DisplayManga(manga),
        DisplayManga(manga.apply { favorite = true }, "Sequel"),
        DisplayManga(manga, "Sequel"),
        DisplayManga(manga.apply { favorite = true }),
        DisplayManga(manga))
    MangaGrid(mangaList = mangaList, shouldOutlineCover = true, columns = 4)
}

@Preview
@Composable
private fun MangaGridItemPreviewLongTitleWithOtherText() {
    MangaGridItem(displayManga = DisplayManga(Manga.create("test",
        "Really Really Long Title Really Really Long Title Really Really Long Title Really Really Long TitleReally Really Long Title",
        1L).apply { favorite = true }, "Sequel"), true)
}