package org.nekomanga.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaList(
    mangaList: PersistentList<DisplayManga>,
    shouldOutlineCover: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    onLongClick: (DisplayManga) -> Unit = {},
    lastPage: Boolean = true,
    loadNextItems: () -> Unit = {},
) {
    val scrollState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
    ) {
        itemsIndexed(mangaList, key = { _, display -> display.mangaId }) { index, displayManga ->
            LaunchedEffect(scrollState) {
                if (!lastPage && index >= mangaList.size - 1) {
                    loadNextItems()
                }
            }
            val listCardType =
                when {
                    index == 0 && mangaList.size > 1 -> ListCardType.Top
                    index == mangaList.size - 1 && mangaList.size > 1 -> ListCardType.Bottom
                    mangaList.size == 1 -> ListCardType.Single
                    else -> ListCardType.Center
                }
            ExpressiveListCard(
                modifier = Modifier.padding(horizontal = Size.small),
                listCardType = listCardType,
            ) {
                MangaRow(
                    displayManga = displayManga,
                    shouldOutlineCover = shouldOutlineCover,
                    modifier =
                        Modifier.fillMaxWidth()
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
fun MangaListWithHeader(
    groupedManga: ImmutableMap<Int, PersistentList<DisplayManga>>,
    shouldOutlineCover: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    onLongClick: (DisplayManga) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.wrapContentWidth(align = Alignment.CenterHorizontally),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
    ) {
        groupedManga.forEach { (stringRes, mangaList) ->
            if (mangaList.isNotEmpty()) {
                item(key = "header-$stringRes") {
                    HeaderCard { DefaultHeaderText(stringResource(id = stringRes)) }
                }
                itemsIndexed(
                    mangaList,
                    key = { _, displayManga ->
                        "${stringRes}-item-${displayManga.getTitle()}-${displayManga.mangaId}"
                    },
                ) { index, displayManga ->
                    val listCardType =
                        when {
                            index == 0 && mangaList.size > 1 -> ListCardType.Top
                            index == mangaList.size - 1 && mangaList.size > 1 -> ListCardType.Bottom
                            mangaList.size == 1 -> ListCardType.Single
                            else -> ListCardType.Center
                        }
                    ExpressiveListCard(
                        modifier = Modifier.padding(horizontal = Size.small),
                        listCardType = listCardType,
                    ) {
                        MangaRow(
                            displayManga = displayManga,
                            shouldOutlineCover = shouldOutlineCover,
                            modifier =
                                Modifier.fillMaxWidth()
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
    }
}

@Composable
fun MangaRow(
    modifier: Modifier = Modifier,
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    isSelected: Boolean = false,
    showUnreadBadge: Boolean = false,
    showDownloadBadge: Boolean = false,
    showStartReadingButton: Boolean = false,
    onStartReadingClick: () -> Unit = {},
    unreadCount: Int = 0,
    downloadCount: Int = 0,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.padding(Size.tiny),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaListCover(displayManga, shouldOutlineCover)

            Column(modifier = Modifier.weight(1f).padding(Size.tiny)) {
                val titleLineCount =
                    when (displayManga.displayText.isBlank()) {
                        true -> 2
                        false -> 1
                    }
                MangaListTitle(title = displayManga.getTitle(), maxLines = titleLineCount)
                MangaListSubtitle(
                    text = displayManga.displayText,
                    textRes = displayManga.displayTextRes,
                )
            }
            if ((showUnreadBadge && unreadCount > 0) || (showDownloadBadge && downloadCount > 0)) {
                Gap(Size.tiny)
                DownloadUnreadBadge(
                    offset = 0.dp,
                    outline = shouldOutlineCover,
                    showUnread = showUnreadBadge,
                    showDownloads = showDownloadBadge,
                    unreadCount = unreadCount,
                    downloadCount = downloadCount,
                )
            }
            if (showStartReadingButton) {
                Gap(Size.tiny)
                StartReadingButton(onStartReadingClick = onStartReadingClick)
            }
        }
        if (isSelected) {
            Box(
                modifier =
                    modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun MangaListTitle(title: String, maxLines: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MangaListSubtitle(text: String, @StringRes textRes: Int?) {
    val displayText =
        when (textRes) {
            null -> text
            else -> stringResource(textRes)
        }
    if (displayText.isNotBlank()) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RowScope.MangaListCover(displayManga: DisplayManga, shouldOutlineCover: Boolean) {
    Box(modifier = Modifier.align(alignment = Alignment.CenterVertically)) {
        MangaCover.Square.invoke(
            artwork = displayManga.currentArtwork,
            shouldOutlineCover = shouldOutlineCover,
            modifier = Modifier.size(Size.huge).padding(Size.tiny),
        )

        if (displayManga.inLibrary) {
            val offset = (-2).dp
            InLibraryIcon(offset, shouldOutlineCover)
        }
    }
}
