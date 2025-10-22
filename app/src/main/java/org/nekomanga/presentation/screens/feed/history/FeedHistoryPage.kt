package org.nekomanga.presentation.screens.feed.history

import android.text.format.DateUtils
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import eu.kanade.tachiyomi.ui.feed.FeedHistoryGroup
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import java.util.Date
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedHistoryPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedHistoryMangaList: PersistentList<FeedManga> = persistentListOf(),
    outlineCovers: Boolean,
    outlineCards: Boolean,
    hasMoreResults: Boolean,
    loadingResults: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
    historyGrouping: FeedHistoryGroup,
) {
    val scrollState = rememberLazyListState()

    val now = Date().time
    var timeSpan by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = scrollState,
        contentPadding = contentPadding,
    ) {
        feedHistoryMangaList.forEachIndexed { index, feedManga ->
            val dateString =
                getDateString(feedManga.chapters.first().chapter.lastRead, now, historyGrouping)
            if (dateString.isNotEmpty() && timeSpan != dateString) {
                timeSpan = dateString

                item(key = "header-$dateString") {
                    Text(
                        text = dateString,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth().padding(Size.small),
                    )
                }
            } else {
                item(key = "gap-$index") { Gap(Size.small) }
            }

            item(key = feedManga.mangaId) {
                LaunchedEffect(scrollState, loadingResults) {
                    if (
                        index >= feedHistoryMangaList.size - 5 && hasMoreResults && !loadingResults
                    ) {
                        loadNextPage()
                    }
                }
                HistoryCard(
                    feedManga = feedManga,
                    outlineCover = outlineCovers,
                    outlineCard = outlineCards,
                    groupedBySeries = historyGrouping == FeedHistoryGroup.Series,
                    mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                    chapterClick = { chapterId ->
                        feedScreenActions.chapterClick(feedManga.mangaId, chapterId)
                    },
                    deleteAllHistoryClick = { feedScreenActions.deleteAllHistoryClick(feedManga) },
                    deleteHistoryClick = { chp ->
                        feedScreenActions.deleteHistoryClick(feedManga, chp)
                    },
                )
                Gap(Size.tiny)
            }
        }
    }
}

private fun getDateString(
    date: Long,
    currentDate: Long,
    historyGroupType: FeedHistoryGroup = FeedHistoryGroup.Day,
): String {
    if (historyGroupType == FeedHistoryGroup.No || historyGroupType == FeedHistoryGroup.Series) {
        return ""
    }

    val dateType =
        if (historyGroupType == FeedHistoryGroup.Day) {
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
