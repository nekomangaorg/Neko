package org.nekomanga.presentation.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Objects
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.nekomanga.domain.manga.DisplayManga

@Composable
fun MangaList(
    mangaList: ImmutableList<DisplayManga>,
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
    groupedManga: ImmutableMap<Int, ImmutableList<DisplayManga>>,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    onLongClick: (DisplayManga) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .wrapContentWidth(align = Alignment.CenterHorizontally),
        contentPadding = contentPadding,
    ) {
        groupedManga.forEach { (stringRes, mangaList) ->
            stickyHeader {
                HeaderCard(stringResource(id = stringRes))
            }
            itemsIndexed(mangaList, key = { _, displayManga -> Objects.hash(displayManga.title, displayManga.mangaId, stringRes) }) { _, displayManga ->
                MangaRow(
                    displayManga = displayManga,
                    shouldOutlineCover,
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
}

@Composable
private fun MangaRow(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(4.dp)) {
        MangaListCover(displayManga, shouldOutlineCover)

        Column(
            modifier = Modifier
                .padding(4.dp)
                .align(alignment = Alignment.CenterVertically),
        ) {
            val titleLineCount = when (displayManga.displayText.isBlank()) {
                true -> 2
                false -> 1
            }

            MangaListTitle(title = displayManga.title, maxLines = titleLineCount)
            MangaListDisplay(displayText = displayManga.displayText)
        }
    }
}

@Composable
private fun MangaListTitle(title: String, maxLines: Int) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MangaListDisplay(displayText: String) {
    if (displayText.isNotBlank()) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RowScope.MangaListCover(displayManga: DisplayManga, shouldOutlineCover: Boolean) {
    Box(modifier = Modifier.Companion.align(alignment = Alignment.CenterVertically)) {
        MangaCover.Square.invoke(
            manga = displayManga,
            shouldOutlineCover = shouldOutlineCover,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp),
        )

        if (displayManga.inLibrary) {
            val offset = (-2).dp
            InLibraryIcon(offset, shouldOutlineCover)
        }
    }
}
