package org.nekomanga.presentation.screens.feed

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
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
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.recents.FeedManga
import eu.kanade.tachiyomi.ui.recents.FeedScreenType
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import java.util.Date
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.decimalFormat
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedPage(
    contentPadding: PaddingValues,
    feedMangaList: ImmutableList<FeedManga>,
    outlineCovers: Boolean,
    hasMoreResults: Boolean,
    hideChapterTitles: Boolean,
    mangaClick: (Long) -> Unit,
    loadNextPage: () -> Unit,
    feedScreenType: FeedScreenType,
) {
    val scrollState = rememberLazyListState()

    val themeColorState = defaultThemeColorState()

    val now = Date().time

    var timeSpan by remember { mutableStateOf("") }


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
    ) {
        when (feedScreenType) {
            FeedScreenType.History -> {
                items(feedMangaList) { feedManga ->
                    HistoryCard(
                        feedManga = feedManga,
                        themeColorState = themeColorState,
                        outlineCovers = outlineCovers,
                        hideChapterTitles = hideChapterTitles,
                        downloadClick = {},
                        mangaClick = { mangaClick(feedManga.mangaId) },
                    )
                    LaunchedEffect(scrollState) {
                        if (hasMoreResults && feedMangaList.indexOf(feedManga) >= feedMangaList.size - 4) {
                            loadNextPage()
                        }
                    }
                }
            }

            FeedScreenType.Updates -> {

                feedMangaList.forEach { feedManga ->
                    feedManga.chapters.forEach { chapter ->
                        chapter.dateFetch.timeSpanFromNow
                        val dateString = DateUtils.getRelativeTimeSpanString(chapter.dateFetch, now, DateUtils.DAY_IN_MILLIS).toString()
                        if (timeSpan != dateString) {
                            timeSpan = dateString
                            item {
                                Text(text = dateString, style = MaterialTheme.typography.labelLarge.copy(color = themeColorState.buttonColor), modifier = Modifier.padding(start = Size.small))
                            }
                        }
                        item {
                            UpdatesCard(
                                chapter,
                                buttonColor = themeColorState.buttonColor,
                                mangaTitle = feedManga.mangaTitle,
                                artwork = feedManga.artwork,
                                outlineCovers = outlineCovers,
                                hideChapterTitles = hideChapterTitles,
                                mangaClick = { mangaClick(feedManga.mangaId) },
                            )
                        }
                    }

                }
            }

        }

    }
}

@Composable
fun getReadTextColor(isRead: Boolean, defaultColor: Color = MaterialTheme.colorScheme.onSurface): Color {
    return when (isRead) {
        true -> MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)
        false -> defaultColor
    }
}

@Composable
fun FeedCover(artwork: Artwork, outlined: Boolean, coverSize: Dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Shapes.coverRadius))
            .clickable { onClick() },
    ) {
        MangaCover.Square.invoke(
            artwork = artwork,
            shouldOutlineCover = outlined,
            modifier = Modifier
                .size(coverSize),
        )
    }
}

@Composable
fun FeedChapterTitleLine(hideChapterTitles: Boolean, isBookmarked: Boolean, chapterNumber: Float, title: String, style: TextStyle, textColor: Color) {
    val titleText = when (hideChapterTitles) {
        true -> stringResource(id = R.string.chapter_, decimalFormat.format(chapterNumber))
        false -> title
    }
    Row {
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.primary,
            )
            Gap(4.dp)
        }
        Text(
            text = titleText,
            style = style.copy(color = textColor, fontWeight = FontWeight.Medium, letterSpacing = (-.6).sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
