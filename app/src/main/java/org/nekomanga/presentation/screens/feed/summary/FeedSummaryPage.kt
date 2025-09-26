package org.nekomanga.presentation.screens.feed.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.screens.feed.updates.UpdatesCard
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedSummaryPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    updatingUpdates: Boolean = false,
    updatingContinueReading: Boolean = false,
    updatingNewlyAdded: Boolean = false,
    updatesFeedMangaList: ImmutableList<FeedManga> = persistentListOf(),
    continueReadingFeedMangaList: ImmutableList<FeedManga> = persistentListOf(),
    newlyAddedFeedMangaList: ImmutableList<FeedManga> = persistentListOf(),
    outlineCovers: Boolean,
    feedScreenActions: FeedScreenActions,
) {
    val scrollState = rememberLazyListState()
    LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {
        item {
            SummaryHeader(
                text = stringResource(R.string.feed_continue_reading),
                isRefreshing = updatingContinueReading,
            )
        }

        if (!updatingContinueReading && continueReadingFeedMangaList.isEmpty()) {
            item { NoResults() }
        } else {

            items(continueReadingFeedMangaList) { feedManga ->
                if (feedManga.chapters.isNotEmpty()) {
                    val chapter = feedManga.chapters.first()
                    ContinueReadingCard(
                        feedManga = feedManga,
                        outlineCover = outlineCovers,
                        mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                        chapterClick = {
                            feedScreenActions.chapterClick(feedManga.mangaId, chapter.chapter.id)
                        },
                        deleteAllHistoryClick = {
                            feedScreenActions.deleteAllHistoryClick(feedManga)
                        },
                        deleteHistoryClick = { chp ->
                            feedScreenActions.deleteHistoryClick(feedManga, chp)
                        },
                    )
                }
            }
        }

        item {
            SummaryHeader(
                text = stringResource(R.string.recently_updated_manga),
                isRefreshing = updatingUpdates,
            )
        }

        if (!updatingUpdates && updatesFeedMangaList.isEmpty()) {
            item { NoResults() }
        } else {
            items(updatesFeedMangaList) { feedManga ->
                val chapter = feedManga.chapters.first()
                UpdatesCard(
                    chapterItem = chapter,
                    updateDate = feedManga.date,
                    isGrouped = false,
                    mangaTitle = feedManga.mangaTitle,
                    artwork = feedManga.artwork,
                    outlineCovers = outlineCovers,
                    mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                    chapterClick = {
                        feedScreenActions.chapterClick(feedManga.mangaId, chapter.chapter.id)
                    },
                    chapterSwipe = { _ -> feedScreenActions.chapterSwipe(chapter) },
                    downloadClick = { action ->
                        feedScreenActions.downloadClick(chapter, feedManga, action)
                    },
                )
            }
        }

        item {
            SummaryHeader(
                text = stringResource(R.string.newly_added),
                isRefreshing = updatingNewlyAdded,
            )
        }

        if (!updatingNewlyAdded && newlyAddedFeedMangaList.isEmpty()) {
            item { NoResults() }
        } else {
            items(newlyAddedFeedMangaList) { feedManga ->
                val chapter = feedManga.chapters.first()
                NewlyAddedCard(
                    chapterItem = chapter,
                    mangaTitle = feedManga.mangaTitle,
                    dateAdded = feedManga.date,
                    artwork = feedManga.artwork,
                    outlineCovers = outlineCovers,
                    mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                    chapterClick = {
                        feedScreenActions.chapterClick(feedManga.mangaId, chapter.chapter.id)
                    },
                    chapterSwipe = { _ -> feedScreenActions.chapterSwipe(chapter) },
                    downloadClick = { action ->
                        feedScreenActions.downloadClick(chapter, feedManga, action)
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryHeader(text: String, isRefreshing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small, vertical = Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.secondary
                ),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(),
        )
        AnimatedVisibility(isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.size(Size.large))
        }
    }
}

@Composable
private fun NoResults() {
    Text(
        text = stringResource(R.string.no_results_found),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(),
    )
}
