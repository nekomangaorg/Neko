package eu.kanade.tachiyomi.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.compose.HeaderCard
import eu.kanade.tachiyomi.ui.base.compose.MangaCover

val montserrat = FontFamily(
    Font(R.font.montserrat_thin, FontWeight.Thin),
    Font(R.font.montserrat_black, FontWeight.Black),
    Font(R.font.montserrat_bold, FontWeight.Bold),
    Font(R.font.montserrat_extra_bold, FontWeight.ExtraBold),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semi_bold, FontWeight.SemiBold),
    Font(R.font.montserrat_regular, FontWeight.Normal),

    )

@Composable
fun MangaTitle(text: String, modifier: Modifier) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontFamily = montserrat,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp)
    )
}

@Composable
fun MangaRelationship(text: String, modifier: Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontFamily = montserrat,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(all = 4.dp)
    )
}

@Composable
fun MangaRow(
    manga: Manga,
    shouldOutlineCover: Boolean,
    modifier: Modifier,
) {
    Row(modifier = modifier.padding(4.dp)) {
        MangaCover(manga,
            Modifier.align(alignment = Alignment.CenterVertically),
            shouldOutlineCover)
        if (manga.relationship.isNullOrBlank()) {
            MangaTitle(manga.title, Modifier.align(alignment = Alignment.CenterVertically))
        } else {
            Column {
                MangaTitle(manga.title, Modifier)
                MangaRelationship(text = manga.relationship!!, Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun MangaList(
    mangaList: List<Manga>,
    shouldOutlineCover: Boolean,
    onClick: (manga: Manga) -> Unit = {},
) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
    ) {
        itemsIndexed(mangaList) { index, manga ->
            MangaRow(manga = manga,
                shouldOutlineCover = shouldOutlineCover,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable {
                        onClick(manga)
                    })
            if (index + 1 < mangaList.size) {
                Divider()
            }
        }
    }
}

@Composable
fun MangaListWithHeader(
    groupedManga: Map<String, List<Manga>>,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
    onClick: (manga: Manga) -> Unit = {},
) {
    LazyColumn(modifier
        .wrapContentWidth(align = Alignment.CenterHorizontally)
        .padding(bottom = 48.dp)) {
        groupedManga.forEach { (text, mangaList) ->
            stickyHeader {
                HeaderCard(text)
            }
            itemsIndexed(mangaList) { index, manga ->
                MangaRow(manga = manga,
                    shouldOutlineCover,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable {
                            onClick(manga)
                        })
                if (index + 1 < mangaList.size) {
                    Divider()
                }
            }

        }
    }
}

@Preview
@Composable
fun MangaListPreview() {
    MangaList(listOf(Manga.create(0L).apply {
        url = ""
        title = "test 1"
        relationship = "doujinshi"
    }, Manga.create(0L).apply {
        url = ""
        title =
            "This is a very very very very very very very very long text that ellipses because its too long"
    }), true, onClick = { })
}

@Preview
@Composable
fun MangaHeaderPreview() {
    MangaListWithHeader(mapOf("abc" to listOf(Manga.create(0L).apply {
        url = ""
        title = "test 1"
    }, Manga.create(0L).apply {
        url = ""
        title =
            "This is a very very very very very very very very long text that ellipses because its too long"
        relationship = "doujinshi"
    })), true, onClick = { })
}
