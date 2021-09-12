package eu.kanade.tachiyomi.ui.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga

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
fun MangaCover(manga: Manga, modifier: Modifier) {
    Box {
        Image(painter = rememberImagePainter(
            data = manga,
            builder = {
                transformations(RoundedCornersTransformation(12f))
                placeholder(Pastel.getColorLight())
            }), contentDescription = null,
            modifier
                .size(64.dp)
                .padding(4.dp))
        if (manga.favorite) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(shape = CircleShape)
                    .background(color = MaterialTheme.colors.secondary)
                    .align(alignment = Alignment.TopStart)
                    .padding(4.dp),
            )
        }
    }
}

@Composable
fun MangaTitle(text: String, modifier: Modifier) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontFamily = montserrat,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(all = 4.dp)
    )
}

@Composable
fun MangaRow(
    manga: Manga,
    modifier: Modifier,
) {
    Row(modifier = modifier) {
        MangaCover(manga, Modifier.align(alignment = Alignment.CenterVertically))
        MangaTitle(manga.title, Modifier.align(alignment = Alignment.CenterVertically))
    }
}

@Composable
fun MangaList(
    mangaList: List<Manga>,
    onClick: (manga: Manga) -> Unit = {},
) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
    ) {
        itemsIndexed(mangaList) { index, manga ->
            MangaRow(manga = manga,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable {
                        onClick(manga)
                    })
            if (index < mangaList.size) {
                Divider()
            }
        }
    }
}

@Composable
fun MangaListWithHeader(
    groupedManga: Map<String, List<Manga>>,
    modifier: Modifier = Modifier,
    onClick: (manga: Manga) -> Unit = {},
) {
    LazyColumn(modifier
        .wrapContentWidth(align = Alignment.CenterHorizontally)
        .padding(bottom = 48.dp)) {
        groupedManga.forEach { (text, mangaList) ->
            stickyHeader {
                Card(shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colors.secondary) {
                    Text(
                        text = text,
                        fontSize = 18.sp,
                        fontFamily = montserrat,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSecondary,
                        modifier = Modifier.padding(all = 8.dp)
                    )
                }
            }
            itemsIndexed(mangaList) { index, manga ->
                MangaRow(manga = manga,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable {
                            onClick(manga)
                        })
                if (index < mangaList.size) {
                    Divider()
                }
            }

        }
    }
}

@Preview
@Composable
fun MangaRowPreview() {
    MangaList(listOf(Manga.create(0L).apply {
        url = ""
        title = "test 1"
    }, Manga.create(0L).apply {
        url = ""
        title =
            "This is a very very very very very very very very long text that ellipses because its too long"
    }), onClick = { })
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
    })), onClick = { })
}
