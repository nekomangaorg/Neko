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
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.R
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedUpdatesPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedUpdatesMangaList: PersistentList<FeedManga> = persistentListOf(),
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
    feedUpdatesMangaList: PersistentList<FeedManga> = persistentListOf(),
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
    val groupedBySeries =
        remember(feedUpdatesMangaList) {
            feedUpdatesMangaList
                .groupBy { getDateString(it.date, now) }
                .map {
                    it.value
                        .groupBy { it.mangaId }
                        .map {
                            val (read, unread) =
                                it.value.flatMap { it.chapters }.partition { it.chapter.read }
                            val chapters = (unread.reversed() + read).toPersistentList()
                            it.value.first().copy(chapters = chapters)
                        }
                }
                .flatten()
        }

    LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {

        // 1. Group the already processed list by date string for rendering
        val renderedGroups = groupedBySeries.groupBy { getDateString(it.date, now) }

        // 2. Iterate through the date groups
        renderedGroups.forEach { (dateString, seriesListForDate) ->

            // 3. Date Header Logic
            if (dateString.isNotEmpty()) {
                // Find the global index of the first item in this date group
                // to correctly trigger the header display once per date.
                val firstItem = seriesListForDate.first()
                val globalIndex = groupedBySeries.indexOf(firstItem)

                // The 'timeSpan' state needs to track the date string globally to prevent duplicate
                // headers
                if (globalIndex == 0) timeSpan = "" // Reset tracker at the start

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
            }

            // 4. Iterate through the series within the current date group
            seriesListForDate.forEachIndexed { groupIndex, feedManga ->
                val latestChapter = feedManga.chapters.first()

                // 5. Determine the ListCardType based on position *within the date group*
                val listCardType =
                    when {
                        // First card after the header
                        groupIndex == 0 && seriesListForDate.size > 1 -> ListCardType.Top
                        // Last card of the group
                        groupIndex == seriesListForDate.size - 1 && seriesListForDate.size > 1 ->
                            ListCardType.Bottom
                        // Only one item in the group
                        seriesListForDate.size == 1 ->
                            ListCardType.Single // Use Single if available
                        // Middle card
                        else -> ListCardType.Center
                    }

                // 6. Pagination Logic (needs the global index)
                val globalIndex = groupedBySeries.indexOf(feedManga)

                item {
                    LaunchedEffect(scrollState, loadingResults) {
                        if (
                            globalIndex >= groupedBySeries.size - 5 &&
                                hasMoreResults &&
                                !loadingResults
                        ) {
                            loadNextPage()
                        }
                    }

                    // 7. Wrap UpdatesCard with ExpressiveListCard
                    ExpressiveListCard(
                        modifier = Modifier.padding(horizontal = Size.small),
                        listCardType = listCardType, // Pass the calculated shape
                    ) {
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

                    // Add Gap only if it's not the last card in the date group
                    if (groupIndex != seriesListForDate.size - 1) {
                        Gap(Size.tiny)
                    }
                }
            }
        }
    }
}

@Composable
private fun Ungrouped(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedUpdatesMangaList: PersistentList<FeedManga> = persistentListOf(),
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
        val groupedManga = feedUpdatesMangaList.groupBy { getDateString(it.date, now) }

        groupedManga.forEach { (dateString, mangaListForDate) ->
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

            val chaptersForDate =
                mangaListForDate.flatMap { feedManga ->
                    feedManga.chapters.map { chapter -> Pair(feedManga, chapter) }
                }

            chaptersForDate.forEachIndexed { chapterIndex, (feedManga, chapterItem) ->
                val listCardType =
                    when {
                        chapterIndex == 0 && chaptersForDate.size > 1 -> ListCardType.Top
                        chapterIndex == chaptersForDate.size - 1 && chaptersForDate.size > 1 ->
                            ListCardType.Bottom
                        chaptersForDate.size == 1 -> ListCardType.Single
                        else -> ListCardType.Center
                    }

                val globalIndex =
                    feedUpdatesMangaList.indexOf(feedManga) // Used for pagination only

                item {
                    LaunchedEffect(scrollState, loadingResults) {
                        if (
                            globalIndex >= feedUpdatesMangaList.size - 5 &&
                                hasMoreResults &&
                                !loadingResults
                        ) {
                            loadNextPage()
                        }
                    }
                    ExpressiveListCard(
                        modifier = Modifier.padding(horizontal = Size.small),
                        listCardType = listCardType, // Pass the correct type
                    ) {
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
                    if (chapterIndex != chaptersForDate.size - 1) {
                        Gap(Size.tiny)
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
