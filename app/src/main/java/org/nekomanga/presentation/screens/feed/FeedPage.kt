package org.nekomanga.presentation.screens.feed

import android.text.format.DateUtils
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.feed.FeedHistoryGroup
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import java.util.Date
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.R
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedPage(
    feedMangaList: ImmutableList<FeedManga>,
    outlineCovers: Boolean,
    outlineCards: Boolean,
    hasMoreResults: Boolean,
    groupedBySeries: Boolean,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
    feedScreenType: FeedScreenType,
    historyGrouping: FeedHistoryGroup,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val scrollState = rememberLazyListState()

    val now = Date().time

    var timeSpan by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
    ) {
        when (feedScreenType) {
            FeedScreenType.History -> {
                feedMangaList.forEach { feedManga ->
                    val dateString =
                        getDateString(
                            feedManga.chapters.first().chapter.lastRead,
                            now,
                            historyGrouping,
                        )
                    if (dateString.isNotEmpty() && timeSpan != dateString) {
                        timeSpan = dateString

                        item {
                            Text(
                                text = dateString,
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        color = MaterialTheme.colorScheme.tertiary
                                    ),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(
                                            start = Size.small,
                                            end = Size.small,
                                            top = Size.small,
                                            bottom = Size.small,
                                        ),
                            )
                        }
                    } else {
                        item { Gap(Size.small) }
                    }

                    item {
                        HistoryCard(
                            feedManga = feedManga,
                            outlineCover = outlineCovers,
                            outlineCard = outlineCards,
                            groupedBySeries = groupedBySeries,
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
                        Gap(Size.tiny)
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
            }
            FeedScreenType.Updates -> {
                feedMangaList.forEach { feedManga ->
                    val dateString = getDateString(feedManga.date, now, isRecent = true)
                    // there should only ever be 1
                    feedManga.chapters.forEach { chapterItem ->
                        if (dateString.isNotEmpty() && timeSpan != dateString) {
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
                                            color = MaterialTheme.colorScheme.primary
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
                                mangaTitle = feedManga.mangaTitle,
                                artwork = feedManga.artwork,
                                outlineCovers = outlineCovers,
                                mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                                chapterClick = { chapterId ->
                                    feedScreenActions.chapterClick(feedManga.mangaId, chapterId)
                                },
                                downloadClick = { action ->
                                    feedScreenActions.downloadClick(chapterItem, feedManga, action)
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
                }
            }
        }
    }
}

private fun getDateString(
    date: Long,
    currentDate: Long,
    historyGroupType: FeedHistoryGroup = FeedHistoryGroup.Day,
    isRecent: Boolean = false,
): String {
    if (historyGroupType == FeedHistoryGroup.No || historyGroupType == FeedHistoryGroup.Series) {
        return ""
    }

    val dateType =
        if (isRecent || historyGroupType == FeedHistoryGroup.Day) {
            DateUtils.DAY_IN_MILLIS
        } else {
            DateUtils.WEEK_IN_MILLIS
        }

    val dateString = DateUtils.getRelativeTimeSpanString(date, currentDate, dateType).toString()
    return if (dateString == "0 weeks ago") {
        "This week"
    } else {
        dateString
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
    language: String,
    isBookmarked: Boolean,
    chapterNumber: Float,
    title: String,
    style: TextStyle,
    textColor: Color,
) {
    Row {
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(Size.medium).align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.primary,
            )
            Gap(Size.extraTiny)
        }
        if (language.isNotEmpty() && !language.equals("en", true)) {
            val iconRes = MdLang.fromIsoCode(language)?.iconResId

            when (iconRes == null) {
                true -> {
                    TimberKt.e { "Missing flag for $language" }
                }
                false -> {
                    val painter =
                        rememberDrawablePainter(
                            drawable = AppCompatResources.getDrawable(LocalContext.current, iconRes)
                        )
                    Image(
                        painter = painter,
                        modifier =
                            Modifier.height(Size.medium)
                                .clip(RoundedCornerShape(Size.tiny))
                                .align(Alignment.CenterVertically),
                        contentDescription = "flag",
                    )
                    Gap(Size.extraTiny)
                }
            }
        }
        Text(
            text = title,
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
