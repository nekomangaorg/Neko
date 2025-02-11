package org.nekomanga.presentation.screens.feed.summary

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        item { SummaryHeader(stringResource(R.string.feed_continue_reading)) }
        if (updatingContinueReading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small)
                )
            }
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

        item { SummaryHeader(stringResource(R.string.new_chapters_unread)) }
        if (updatingUpdates) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small)
                )
            }
        }
        if (!updatingUpdates && updatesFeedMangaList.isEmpty()) {
            item { NoResults() }
        } else {
            items(updatesFeedMangaList) { feedManga ->
                val chapter = feedManga.chapters.first()
                UpdatesCard(
                    chapterItem = chapter,
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

        item { SummaryHeader(stringResource(R.string.newly_added)) }
        if (updatingNewlyAdded) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small)
                )
            }
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
private fun SummaryHeader(text: String) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.tertiary),
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

@Composable
private fun NoResults() {
    Text(
        text = stringResource(R.string.no_results_found),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(),
    )
}
