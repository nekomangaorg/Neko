package org.nekomanga.presentation.screens.feed.updates

import android.text.format.DateUtils
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.feed.FeedHistoryGroup
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedUpdatesPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedUpdatesMangaList: ImmutableList<FeedManga> = persistentListOf(),
    outlineCovers: Boolean = false,
    hasMoreResults: Boolean = false,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
) {
    val scrollState = rememberLazyListState()
    val now = Date().time
    var timeSpan by remember { mutableStateOf("") }
    LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {
        feedUpdatesMangaList.forEachIndexed { index, feedManga ->
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
                        if (hasMoreResults && index >= feedUpdatesMangaList.size - 10) {
                            loadNextPage()
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
