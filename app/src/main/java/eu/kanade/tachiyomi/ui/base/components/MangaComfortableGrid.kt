package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.ui.base.components.theme.Shapes
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces

@Composable
fun MangaComfortableGridWithHeader(
    groupedManga: Map<String, List<DisplayManga>>,
    shouldOutlineCover: Boolean,
    columns: Int,
    modifier: Modifier = Modifier,
    onClick: (manga: Manga) -> Unit = {},
) {

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        groupedManga.forEach { (headerText, allGrids) ->
            stickyHeader {
                HeaderCard(headerText)
            }
            gridItems(items = allGrids,
                columns = columns,
                modifier = modifier
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) { displayManga ->
                MangaComfortableGridItem(
                    displayManga = displayManga,
                    shouldOutlineCover = shouldOutlineCover,
                    iconModifier = columns,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
fun MangaComfortableGrid(
    mangaList: List<DisplayManga>,
    shouldOutlineCover: Boolean,
    columns: Int,
    onClick: (Manga) -> Unit = {},
) {
    val cells = if (columns > 1) {
        GridCells.Fixed(columns)
    } else {
        GridCells.Adaptive(160.dp)
    }
    LazyVerticalGrid(
        cells = cells,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(mangaList) { displayManga ->
            MangaComfortableGridItem(
                displayManga = displayManga,
                shouldOutlineCover = shouldOutlineCover,
                iconModifier = columns,
                onClick = { onClick(displayManga.manga) },
            )
        }
    }
}

@Composable
private fun MangaComfortableGridItem(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    iconModifier: Int = 1,
    onClick: (Manga) -> Unit = {},
) {

    val outlineModifier = when (shouldOutlineCover) {
        true -> Modifier.border(.75.dp, NekoColors.outline,
            RoundedCornerShape(12.dp))
        else -> Modifier
    }
    Box {
        Box(
            modifier = Modifier
                .clickable { onClick(displayManga.manga) }
                .padding(2.dp)
                .background(color = Color.Transparent,
                    shape = RoundedCornerShape(Shapes.coverRadius))
        ) {
            Column {
                Image(painter = rememberImagePainter(
                    data = displayManga.manga,
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
                val shouldShowDisplayText = displayManga.displayText.isNullOrEmpty().not()

                val bottomPadding = if (shouldShowDisplayText) 0.dp else 4.dp

                Text(
                    text = displayManga.manga.title,
                    style = TextStyle(fontFamily = Typefaces.montserrat,
                        fontSize = Typefaces.bodySmall,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-.8).sp),
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp,
                        bottom = bottomPadding,
                        start = 4.dp,
                        end = 4.dp)
                )
                if (shouldShowDisplayText) {
                    Text(
                        text = displayManga.displayText!!,
                        style = TextStyle(fontFamily = Typefaces.montserrat,
                            fontSize = Typefaces.bodySmall,
                            letterSpacing = (-.5).sp,
                            color = MaterialTheme.colors.onSurface.copy(.6f)),
                        maxLines = 1,
                        modifier = Modifier
                            .padding(top = 0.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
                    )
                }
            }
        }
        if (displayManga.manga.favorite) {
            Image(
                asset = CommunityMaterial.Icon2.cmd_heart,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.secondary),
                modifier = Modifier
                    .size(24.dp)
                    .align(alignment = Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-6).dp)
            )
        }
    }
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
    MangaComfortableGridWithHeader(groupedManga = mangaMap, shouldOutlineCover = true, columns = 2)
}

@Preview
@Composable
private fun MangaGridPreview() {
    val manga = Manga.create("test", "One Piece", 1L)
    val mangaList = listOf(DisplayManga(manga, "related"),
        DisplayManga(manga, "Sequel"),
        DisplayManga(manga),
        DisplayManga(manga),
        DisplayManga(manga))
    MangaComfortableGrid(mangaList = mangaList, shouldOutlineCover = true, columns = 2)
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
    MangaComfortableGrid(mangaList = mangaList, shouldOutlineCover = true, columns = 4)
}

@Preview
@Composable
private fun MangaGridItemPreviewLongTitleWithOtherText() {
    MangaComfortableGridItem(displayManga = DisplayManga(Manga.create("test",
        "Really Really Long Title Really Really Long Title Really Really Long Title Really Really Long TitleReally Really Long Title",
        1L).apply { favorite = true }, "Sequel"), true)
}