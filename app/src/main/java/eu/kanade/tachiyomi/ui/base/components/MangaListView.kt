package eu.kanade.tachiyomi.ui.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.ui.base.components.CoverRippleTheme
import eu.kanade.tachiyomi.ui.base.components.DisplayText
import eu.kanade.tachiyomi.ui.base.components.Favorited
import eu.kanade.tachiyomi.ui.base.components.HeaderCard
import eu.kanade.tachiyomi.ui.base.components.MangaTitle
import eu.kanade.tachiyomi.ui.base.components.NekoColors
import eu.kanade.tachiyomi.ui.base.components.theme.Shapes

@Composable
fun MangaRow(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(4.dp)) {
        MangaCover(displayManga.manga,
            shouldOutlineCover,
            Modifier.align(alignment = Alignment.CenterVertically))
        if (displayManga.displayText.isBlank()) {
            MangaTitle(
                title = displayManga.manga.title,
                modifier = Modifier.align(
                    alignment = Alignment.CenterVertically),
                fontSize = MaterialTheme.typography.body1.fontSize)
        } else {
            Column(Modifier
                .padding(4.dp)
                .align(
                    alignment = Alignment.CenterVertically)) {
                MangaTitle(
                    title = displayManga.manga.title,
                    maxLines = 1,
                    fontSize = MaterialTheme.typography.body1.fontSize)
                DisplayText(
                    displayText = displayManga.displayText,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun MangaList(
    mangaList: List<DisplayManga>,
    shouldOutlineCover: Boolean,
    onClick: (manga: Manga) -> Unit = {},
) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
    ) {
        itemsIndexed(mangaList) { index, displayManga ->
            MangaRow(displayManga = displayManga,
                shouldOutlineCover = shouldOutlineCover,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable {
                        onClick(displayManga.manga)
                    })
            if (index + 1 < mangaList.size) {
                Divider()
            }
        }
    }
}

@Composable
fun MangaListWithHeader(
    groupedManga: Map<String, List<DisplayManga>>,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (manga: Manga) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .wrapContentWidth(align = Alignment.CenterHorizontally),
        contentPadding = contentPadding) {
        groupedManga.forEach { (text, mangaList) ->
            stickyHeader {
                HeaderCard(text)
            }
            itemsIndexed(mangaList) { index, displayManga ->
                CompositionLocalProvider(LocalRippleTheme provides CoverRippleTheme) {
                    MangaRow(displayManga = displayManga,
                        shouldOutlineCover,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable {
                                onClick(displayManga.manga)
                            })
                    if (index + 1 < mangaList.size) {
                        Divider()
                    }
                }
            }

        }
    }
}

@Composable
private fun MangaCover(manga: Manga, shouldOutlineCover: Boolean, modifier: Modifier = Modifier) {
    Box {

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
            modifier = modifier
                .size(48.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(Shapes.coverRadius))
                .then(outlineModifier)

        )
        if (manga.favorite) {
            val offset = (-4).dp
            Favorited(offset)
        }
    }
}

@Preview
@Composable
fun MangaListPreview() {
    MangaList(listOf(DisplayManga(Manga.create(0L).apply {
        url = ""
        title = "test 1"
        relationship = "doujinshi"
    }), DisplayManga(Manga.create(0L).apply {
        url = ""
        title =
            "This is a very very very very very very very very long text that ellipses because its too long"
    })), true, onClick = { })
}

@Preview
@Composable
fun MangaHeaderPreview() {
    MangaListWithHeader(mapOf("abc" to listOf(DisplayManga(Manga.create(0L).apply {
        url = ""
        title = "test 1"
    }), DisplayManga(Manga.create(0L).apply {
        url = ""
        title =
            "This is a very very very very very very very very long text that ellipses because its too long"
    }, "doujinshi")
    )), true)
}

@Preview
@Composable
private fun MangaCoverPreview() {
    MangaCover(manga = Manga.create("test",
        "Title",
        1L).apply { favorite = true }, true)
}
