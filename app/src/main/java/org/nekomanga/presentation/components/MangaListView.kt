package org.nekomanga.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.models.DisplayManga
import org.nekomanga.presentation.theme.Shapes

@Composable
fun MangaRow(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(4.dp)) {
        MangaCover(
            displayManga.manga,
            shouldOutlineCover,
            Modifier.align(alignment = Alignment.CenterVertically),
        )
        if (displayManga.displayText.isBlank()) {
            MangaTitle(
                title = displayManga.manga.title,
                modifier = Modifier.align(
                    alignment = Alignment.CenterVertically,
                ),
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            )
        } else {
            Column(
                Modifier
                    .padding(4.dp)
                    .align(
                        alignment = Alignment.CenterVertically,
                    ),
            ) {
                MangaTitle(
                    title = displayManga.manga.title,
                    maxLines = 1,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                )
                DisplayText(
                    displayText = displayManga.displayText,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
fun PagingListManga(
    mangaListPagingItems: LazyPagingItems<DisplayManga>,
    shouldOutlineCover: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (manga: Manga) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        items(mangaListPagingItems) { displayManga ->
            displayManga?.let {
                MangaRow(
                    displayManga = displayManga,
                    shouldOutlineCover = shouldOutlineCover,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable {
                            onClick(displayManga.manga)
                        },
                )
            }
        }
        mangaListPagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Loading(
                                isLoading = true,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaList(
    mangaList: List<DisplayManga>,
    shouldOutlineCover: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (manga: Manga) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(mangaList) { index, displayManga ->
            MangaRow(
                displayManga = displayManga,
                shouldOutlineCover = shouldOutlineCover,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable {
                        onClick(displayManga.manga)
                    },
            )
        }
    }
}

@Composable
fun MangaListWithHeader(
    groupedManga: Map<String, List<DisplayManga>>,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Manga) -> Unit = {},
    onLongClick: (Manga) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .wrapContentWidth(align = Alignment.CenterHorizontally),
        contentPadding = contentPadding,
    ) {
        groupedManga.forEach { (text, mangaList) ->
            stickyHeader {
                HeaderCard(text)
            }
            itemsIndexed(mangaList) { index, displayManga ->
                CompositionLocalProvider(LocalRippleTheme provides PrimaryColorRippleTheme) {
                    MangaRow(
                        displayManga = displayManga,
                        shouldOutlineCover,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .combinedClickable(
                                onClick = { onClick(displayManga.manga) },
                                onLongClick = { onLongClick(displayManga.manga) },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaCover(manga: Manga, shouldOutlineCover: Boolean, modifier: Modifier = Modifier) {
    Box {
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
                .data(manga)
                .placeholder(color)
                .setParameter(MangaCoverFetcher.useCustomCover, false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(Shapes.coverRadius))
                .then(outlineModifier),
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
    MangaList(
        listOf(
            DisplayManga(
                Manga.create(0L).apply {
                    url = ""
                    title = "test 1"
                },
            ),
            DisplayManga(
                Manga.create(0L).apply {
                    url = ""
                    title =
                        "This is a very very very very very very very very long text that ellipses because its too long"
                },
            ),
        ),
        true, onClick = { },
    )
}

@Preview
@Composable
fun MangaHeaderPreview() {
    MangaListWithHeader(
        mapOf(
            "abc" to listOf(
                DisplayManga(
                    Manga.create(0L).apply {
                        url = ""
                        title = "test 1"
                    },
                ),
                DisplayManga(
                    Manga.create(0L).apply {
                        url = ""
                        title =
                            "This is a very very very very very very very very long text that ellipses because its too long"
                    },
                    "doujinshi",
                ),
            ),
        ),
        true,
    )
}

@Preview
@Composable
private fun MangaCoverPreview() {
    MangaCover(
        manga = Manga.create(
            "test",
            "Title",
            1L,
        ).apply { favorite = true },
        true,
    )
}
