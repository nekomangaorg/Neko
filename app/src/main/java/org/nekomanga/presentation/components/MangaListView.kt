package org.nekomanga.presentation.components

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.image.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.util.system.toMangaCacheKey
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.theme.Shapes

@Composable
fun MangaRow(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(4.dp)) {
        MangaCover(
            displayManga,
            shouldOutlineCover,
            Modifier.align(alignment = Alignment.CenterVertically),
        )
        if (displayManga.displayText.isBlank()) {
            MangaTitle(
                title = displayManga.title,
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
                    title = displayManga.title,
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
fun MangaList(
    mangaList: List<DisplayManga>,
    shouldOutlineCover: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    onLongClick: (DisplayManga) -> Unit = {},
    loadNextItems: () -> Unit = {},
) {

    val scrollState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
    ) {
        itemsIndexed(mangaList, key = { _, display -> display.mangaId }) { index, displayManga ->

            LaunchedEffect(scrollState) {
                if (index >= mangaList.size - 1) {
                    loadNextItems()
                }
            }
            MangaRow(
                displayManga = displayManga,
                shouldOutlineCover = shouldOutlineCover,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .combinedClickable(
                        onClick = { onClick(displayManga.mangaId) },
                        onLongClick = { onLongClick(displayManga) },
                    ),
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
    onClick: (Long) -> Unit = {},
    onLongClick: (Long) -> Unit = {},
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
            itemsIndexed(mangaList, key = { _, display -> display.mangaId }) { _, displayManga ->
                CompositionLocalProvider(LocalRippleTheme provides PrimaryColorRippleTheme) {
                    MangaRow(
                        displayManga = displayManga,
                        shouldOutlineCover,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .combinedClickable(
                                onClick = { onClick(displayManga.mangaId) },
                                onLongClick = { onLongClick(displayManga.mangaId) },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaCover(manga: DisplayManga, shouldOutlineCover: Boolean, modifier: Modifier = Modifier) {
    Box {
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
                .size(48.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(Shapes.coverRadius))
                .conditional(shouldOutlineCover) {
                    this.border(
                        width = Outline.thickness,
                        color = Outline.color,
                        shape = RoundedCornerShape(Shapes.coverRadius),
                    )
                },
        )

        if (manga.inLibrary) {
            val offset = (-2).dp
            InLibraryIcon(offset, shouldOutlineCover)
        }
    }
}
