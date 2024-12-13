package org.nekomanga.presentation.screens.feed

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.recents.FeedManga
import eu.kanade.tachiyomi.ui.recents.FeedScreenActions
import eu.kanade.tachiyomi.ui.recents.FeedScreenType
import java.util.Date
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.R
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.decimalFormat
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedPage(
    feedMangaList: ImmutableList<FeedManga>,
    outlineCovers: Boolean,
    outlineCards: Boolean,
    hasMoreResults: Boolean,
    hideChapterTitles: Boolean,
    groupedBySeries: Boolean,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
    feedScreenType: FeedScreenType,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val scrollState = rememberLazyListState()

    val themeColorState = defaultThemeColorState()

    val now = Date().time

    var timeSpan by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
    ) {
        when (feedScreenType) {
            FeedScreenType.History -> {
                items(feedMangaList) { feedManga ->
                    HistoryCard(
                        feedManga = feedManga,
                        themeColorState = themeColorState,
                        outlineCover = outlineCovers,
                        outlineCard = outlineCards,
                        hideChapterTitles = hideChapterTitles,
                        groupedBySeries = groupedBySeries,
                        downloadClick = { chp, action ->
                            feedScreenActions.downloadClick(chp, feedManga, action)
                        },
                        mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                        chapterClick = { chapterId ->
                            feedScreenActions.chapterClick(feedManga.mangaId, chapterId)
                        },
                        deleteAllHistoryClick = {
                            feedScreenActions.deleteAllHistoryClick(feedManga)
                        },
                        deleteHistoryClick = { chp ->
                            feedScreenActions.deleteHistoryClick(feedManga, chp)
                        },
                    )
                    LaunchedEffect(scrollState) {
                        if (
                            hasMoreResults &&
                                feedMangaList.indexOf(feedManga) >= feedMangaList.size - 4
                        ) {
                            loadNextPage()
                        }
                    }
                }
            }
            FeedScreenType.Updates -> {

                feedMangaList.forEach { feedManga ->
                    val dateString =
                        DateUtils.getRelativeTimeSpanString(
                                feedManga.date,
                                now,
                                DateUtils.DAY_IN_MILLIS,
                            )
                            .toString()
                    // there should only ever be 1
                    feedManga.chapters.forEach { chapterItem ->
                        if (timeSpan != dateString) {
                            timeSpan = dateString

                            val prefix =
                                when (updatesFetchSort) {
                                    true -> R.string.fetched_
                                    false -> R.string.updated_
                                }

                            item {
                                Text(
                                    text = stringResource(id = prefix, dateString),
                                    style =
                                        MaterialTheme.typography.labelLarge.copy(
                                            color = themeColorState.buttonColor
                                        ),
                                    modifier =
                                        Modifier.padding(
                                            start = Size.small,
                                            top = Size.small,
                                            end = Size.small,
                                        ),
                                )
                            }
                        }
                        item {
                            UpdatesCard(
                                chapterItem = chapterItem,
                                themeColorState = themeColorState,
                                mangaTitle = feedManga.mangaTitle,
                                artwork = feedManga.artwork,
                                outlineCovers = outlineCovers,
                                hideChapterTitles = hideChapterTitles,
                                mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                                chapterClick = { chapterId ->
                                    feedScreenActions.chapterClick(feedManga.mangaId, chapterId)
                                },
                                downloadClick = { action ->
                                    feedScreenActions.downloadClick(chapterItem, feedManga, action)
                                },
                            )
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun getReadTextColor(
    isRead: Boolean,
    defaultColor: Color = MaterialTheme.colorScheme.onSurface,
): Color {
    return when (isRead) {
        true ->
            MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)
        false -> defaultColor
    }
}

@Composable
fun FeedCover(
    artwork: Artwork,
    outlined: Boolean,
    coverSize: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(Shapes.coverRadius)).clickable { onClick() }) {
        MangaCover.Square.invoke(
            artwork = artwork,
            shouldOutlineCover = outlined,
            modifier = Modifier.size(coverSize),
        )
    }
}

@Composable
fun FeedChapterTitleLine(
    hideChapterTitles: Boolean,
    isBookmarked: Boolean,
    chapterNumber: Float,
    title: String,
    style: TextStyle,
    textColor: Color,
) {
    val titleText =
        when (hideChapterTitles) {
            true -> stringResource(id = R.string.chapter_, decimalFormat.format(chapterNumber))
            false -> title
        }
    Row {
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(16.dp).align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.primary,
            )
            Gap(4.dp)
        }
        Text(
            text = titleText,
            style =
                style.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-.6).sp,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
