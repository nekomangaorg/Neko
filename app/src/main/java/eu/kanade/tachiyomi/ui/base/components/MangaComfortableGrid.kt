package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces

@Composable
fun MangaComfortableGrid(
    mangaList: List<DisplayManga>,
    shouldOutlineCover: Boolean,
    columns: Int,
    onClickManga: (Manga) -> Unit = {},
    onLongClickManga: (Manga) -> Unit = {},
) {
    val cells = if (columns > 1) {
        GridCells.Fixed(columns)
    } else {
        GridCells.Adaptive(160.dp)
    }
    LazyVerticalGrid(
        /* contentPadding = rememberNavigationBarsInsetsPaddingValues(
             additionalBottom = 64.dp
         ),*/
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
                onClick = { onClickManga(displayManga.manga) },
                onLongClick = { onLongClickManga(displayManga.manga) }
            )
        }
    }
}

@Composable
private fun MangaComfortableGridItem(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {

    val outlineModifier = when (shouldOutlineCover) {
        true -> Modifier.border(.75.dp, NekoColors.outline,
            RoundedCornerShape(12.dp))
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
    ) {
        Column {
            Image(painter = rememberImagePainter(
                data = displayManga,
                builder = {
                    placeholder(Pastel.getColorLight())
                    transformations(RoundedCornersTransformation(0f))
                }),
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(outlineModifier),
                contentScale = ContentScale.Crop
            )
            val shouldShowDisplayText = displayManga.displayText.isNullOrEmpty().not()

            val bottomPadding = if (shouldShowDisplayText) 2.dp else 4.dp

            Text(
                text = displayManga.manga.title,
                style = TextStyle(fontFamily = Typefaces.montserrat, fontSize = Typefaces.body),
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
                        fontSize = Typefaces.body,
                        color = MaterialTheme.colors.onSurface.copy(.6f)),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 0.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }

    }
}

@Preview
@Composable
private fun MangaGridPreview() {
    val onClick = {}
    val onLongClick = {}
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
    val onClick = {}
    val onLongClick = {}
    val manga = Manga.create("test",
        "Really Really Long Title Really Really Long Title Really Really Long Title Really Really Long TitleReally Really Long Title",
        1L)
    val mangaList = listOf(DisplayManga(manga),
        DisplayManga(manga),
        DisplayManga(manga),
        DisplayManga(manga),
        DisplayManga(manga))
    MangaComfortableGrid(mangaList = mangaList, shouldOutlineCover = true, columns = 4)
}

@Preview
@Composable
private fun MangaGridItemPreviewLongTitleWithOtherText() {
    MangaComfortableGridItem(displayManga = DisplayManga(Manga.create("test",
        "Really Really Long Title Really Really Long Title Really Really Long Title Really Really Long TitleReally Really Long Title",
        1L), "Sequel"), true,
        onClick = { /*TODO*/ }) {}
}