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
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedUpdatesPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedUpdatesMangaList: ImmutableList<FeedManga> = persistentListOf(),
    outlineCovers: Boolean,
    hasMoreResults: Boolean,
    loadingResults: Boolean,
    groupedBySeries: Boolean,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
) {
    when (groupedBySeries) {
        true -> {
            Grouped(
                modifier = modifier,
                contentPadding = contentPadding,
                feedUpdatesMangaList = feedUpdatesMangaList,
                outlineCovers = outlineCovers,
                hasMoreResults = hasMoreResults,
                loadingResults = loadingResults,
                updatesFetchSort = updatesFetchSort,
                feedScreenActions = feedScreenActions,
                loadNextPage = loadNextPage,
            )
        }
        false -> {
            Ungrouped(
                modifier = modifier,
                contentPadding = contentPadding,
                feedUpdatesMangaList = feedUpdatesMangaList,
                outlineCovers = outlineCovers,
                hasMoreResults = hasMoreResults,
                loadingResults = loadingResults,
                updatesFetchSort = updatesFetchSort,
                feedScreenActions = feedScreenActions,
                loadNextPage = loadNextPage,
            )
        }
    }
}

@Composable
private fun Grouped(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedUpdatesMangaList: ImmutableList<FeedManga> = persistentListOf(),
    outlineCovers: Boolean = false,
    hasMoreResults: Boolean = false,
    loadingResults: Boolean = false,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
) {
    val scrollState = rememberLazyListState()
    val now = Date().time
    var timeSpan by remember { mutableStateOf("") }
    var groupedBySeries =
        remember(feedUpdatesMangaList) {
            feedUpdatesMangaList
                .groupBy { getDateString(it.date, now) }
                .map {
                    it.value
                        .groupBy { it.mangaId }
                        .map {
                            val chapters =
                                it.value.flatMap { it.chapters }.reversed().toImmutableList()
                            it.value.first().copy(chapters = chapters)
                        }
                }
                .flatten()
        }

    LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {
        groupedBySeries.forEachIndexed { index, feedManga ->
            if (index == 0) {
                timeSpan = ""
            }
            val dateString = getDateString(feedManga.date, now)
            // there should only ever be 1

            val latestChapter = feedManga.chapters.first()

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
                            Modifier.padding(start = Size.small, top = Size.small, end = Size.small),
                    )
                }
            }
            item {
                LaunchedEffect(scrollState, loadingResults) {
                    if (index >= groupedBySeries.size - 5 && hasMoreResults && !loadingResults) {
                        loadNextPage()
                    }
                }
                UpdatesCard(
                    chapterItem = latestChapter,
                    numberOfChapters = feedManga.chapters.size,
                    isGrouped = true,
                    mangaTitle = feedManga.mangaTitle,
                    artwork = feedManga.artwork,
                    outlineCovers = outlineCovers,
                    mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                    chapterClick = { chapterId ->
                        feedScreenActions.chapterClick(feedManga.mangaId, chapterId)
                    },
                    chapterSwipe = { _ -> feedScreenActions.chapterSwipe(latestChapter) },
                    downloadClick = { action ->
                        feedScreenActions.downloadClick(latestChapter, feedManga, action)
                    },
                )
            }
        }
    }
}

@Composable
private fun Ungrouped(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedUpdatesMangaList: ImmutableList<FeedManga> = persistentListOf(),
    outlineCovers: Boolean = false,
    hasMoreResults: Boolean = false,
    loadingResults: Boolean = false,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
) {
    val scrollState = rememberLazyListState()
    val now = Date().time
    var timeSpan by remember { mutableStateOf("") }
    LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {
        feedUpdatesMangaList.forEachIndexed { index, feedManga ->
            if (index == 0) {
                timeSpan = ""
            }
            val dateString = getDateString(feedManga.date, now)
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
                    LaunchedEffect(scrollState, loadingResults) {
                        if (
                            index >= feedUpdatesMangaList.size - 5 &&
                                hasMoreResults &&
                                !loadingResults
                        ) {
                            loadNextPage()
                        }
                    }
                    UpdatesCard(
                        chapterItem = chapterItem,
                        mangaTitle = feedManga.mangaTitle,
                        artwork = feedManga.artwork,
                        outlineCovers = outlineCovers,
                        mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                        chapterClick = { chapterId ->
                            feedScreenActions.chapterClick(feedManga.mangaId, chapterId)
                        },
                        chapterSwipe = { chapterItem ->
                            feedScreenActions.chapterSwipe(chapterItem)
                        },
                        downloadClick = { action ->
                            feedScreenActions.downloadClick(chapterItem, feedManga, action)
                        },
                    )
                }
            }
        }
    }
}

private fun getDateString(date: Long, currentDate: Long): String {
    return DateUtils.getRelativeTimeSpanString(date, currentDate, DateUtils.DAY_IN_MILLIS)
        .toString()
}
