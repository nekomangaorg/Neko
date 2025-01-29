package org.nekomanga.presentation.screens.feed.updates

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import java.util.Date
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.nekomanga.R
import org.nekomanga.presentation.components.Loading
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedUpdatesPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedUpdatesMangaList: ImmutableList<FeedManga> = persistentListOf(),
    outlineCovers: Boolean = false,
    hasMoreResults: Boolean = false,
    groupedBySeries: Boolean = false,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
) {
    when (groupedBySeries) {
        true -> {
            grouped(
                modifier = modifier,
                contentPadding = contentPadding,
                feedUpdatesMangaList = feedUpdatesMangaList,
                outlineCovers = outlineCovers,
                hasMoreResults = hasMoreResults,
                updatesFetchSort = updatesFetchSort,
                feedScreenActions = feedScreenActions,
                loadNextPage = loadNextPage,
            )
        }
        false -> {
            ungrouped(
                modifier = modifier,
                contentPadding = contentPadding,
                feedUpdatesMangaList = feedUpdatesMangaList,
                outlineCovers = outlineCovers,
                hasMoreResults = hasMoreResults,
                updatesFetchSort = updatesFetchSort,
                feedScreenActions = feedScreenActions,
                loadNextPage = loadNextPage,
            )
        }
    }
}

@Composable
private fun grouped(
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
    var groupedBySeries =
        remember(feedUpdatesMangaList.size) {
            feedUpdatesMangaList
                .groupBy { getDateString(it.date, now) }
                .map {
                    it.value
                        .groupBy { it.mangaId }
                        .map {
                            val chapters = it.value.flatMap { it.chapters }.toImmutableList()
                            it.value.first().copy(chapters = chapters)
                        }
                }
                .flatten()
        }
    var loadingMore by remember { mutableStateOf(false) }
    LaunchedEffect(groupedBySeries.size) { loadingMore = false }

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
                            Modifier.padding(
                                start = Size.small,
                                top = Size.small,
                                end = Size.small
                            ),
                    )
                }
            }
            item {
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
                    chapterSwipe = { chapterItem -> feedScreenActions.chapterSwipe(chapterItem) },
                    downloadClick = {},
                )

                LaunchedEffect(feedUpdatesMangaList.size) {
                    if (hasMoreResults && index >= groupedBySeries.size - 5) {
                        loadingMore = true
                        loadNextPage()
                    }
                }
            }
        }
        if (loadingMore) {
            item {
                Gap(Size.small)
                Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Loading()
                }
            }
        }
    }
}

@Composable
private fun ungrouped(
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
    var loadingMore by remember { mutableStateOf(false) }
    LaunchedEffect(feedUpdatesMangaList.size) { loadingMore = false }
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
                    LaunchedEffect(feedUpdatesMangaList.size) {
                        if (hasMoreResults && index >= feedUpdatesMangaList.size - 5) {
                            loadingMore = true
                            loadNextPage()
                        }
                    }
                }
            }
        }
    }
}

private fun getDateString(date: Long, currentDate: Long): String {
    return DateUtils.getRelativeTimeSpanString(date, currentDate, DateUtils.DAY_IN_MILLIS)
        .toString()
}
