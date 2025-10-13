package org.nekomanga.presentation.screens.feed.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.screens.feed.updates.UpdatesCard
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedSummaryPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    updatingUpdates: Boolean = false,
    updatingContinueReading: Boolean = false,
    updatingNewlyAdded: Boolean = false,
    updatesFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
    continueReadingFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
    newlyAddedFeedMangaList: PersistentList<FeedManga> = persistentListOf(),
    outlineCovers: Boolean,
    useVividColorHeaders: Boolean,
    feedScreenActions: FeedScreenActions,
) {
    val scrollState = rememberLazyListState()

    val headerColor =
        when (useVividColorHeaders) {
            true -> MaterialTheme.colorScheme.primary
            false -> MaterialTheme.colorScheme.onSurface
        }

    LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {
        item {
            SummaryHeader(
                text = stringResource(R.string.feed_continue_reading),
                color = headerColor,
                isRefreshing = updatingContinueReading,
            )
        }

        if (!updatingContinueReading && continueReadingFeedMangaList.isEmpty()) {
            item { NoResults() }
        } else {

            itemsIndexed(continueReadingFeedMangaList) { index, feedManga ->
                val listCardType =
                    when {
                        index == 0 && continueReadingFeedMangaList.size > 1 -> ListCardType.Top
                        index == continueReadingFeedMangaList.size - 1 &&
                            continueReadingFeedMangaList.size > 1 -> ListCardType.Bottom
                        continueReadingFeedMangaList.size == 1 -> ListCardType.Single
                        else -> ListCardType.Center
                    }
                if (feedManga.chapters.isNotEmpty()) {
                    val chapter = feedManga.chapters.first()
                    ExpressiveListCard(
                        modifier = Modifier.padding(horizontal = Size.small),
                        listCardType = listCardType,
                    ) {
                        ContinueReadingCard(
                            feedManga = feedManga,
                            outlineCover = outlineCovers,
                            mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                            chapterClick = {
                                feedScreenActions.chapterClick(
                                    feedManga.mangaId,
                                    chapter.chapter.id,
                                )
                            },
                            deleteAllHistoryClick = {
                                feedScreenActions.deleteAllHistoryClick(feedManga)
                            },
                            deleteHistoryClick = { chp ->
                                feedScreenActions.deleteHistoryClick(feedManga, chp)
                            },
                        )
                    }
                    Gap(Size.tiny)
                }
            }
        }

        item {
            SummaryHeader(
                text = stringResource(R.string.recently_updated_manga),
                color = headerColor,
                isRefreshing = updatingUpdates,
            )
        }

        if (!updatingUpdates && updatesFeedMangaList.isEmpty()) {
            item { NoResults() }
        } else {
            itemsIndexed(updatesFeedMangaList) { index, feedManga ->
                val listCardType =
                    when {
                        index == 0 && updatesFeedMangaList.size > 1 -> ListCardType.Top
                        index == updatesFeedMangaList.size - 1 && updatesFeedMangaList.size > 1 ->
                            ListCardType.Bottom
                        updatesFeedMangaList.size == 1 -> ListCardType.Single
                        else -> ListCardType.Center
                    }
                val chapter = feedManga.chapters.first()
                ExpressiveListCard(
                    modifier = Modifier.padding(horizontal = Size.small),
                    listCardType = listCardType,
                ) {
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
        }

        item {
            SummaryHeader(
                text = stringResource(R.string.newly_added),
                color = headerColor,
                isRefreshing = updatingNewlyAdded,
            )
        }

        if (!updatingNewlyAdded && newlyAddedFeedMangaList.isEmpty()) {
            item { NoResults() }
        } else {
            itemsIndexed(newlyAddedFeedMangaList) { index, feedManga ->
                val listCardType =
                    when {
                        index == 0 && newlyAddedFeedMangaList.size > 1 -> ListCardType.Top
                        index == newlyAddedFeedMangaList.size - 1 &&
                            newlyAddedFeedMangaList.size > 1 -> ListCardType.Bottom
                        newlyAddedFeedMangaList.size == 1 -> ListCardType.Single
                        else -> ListCardType.Center
                    }
                val chapter = feedManga.chapters.first()
                ExpressiveListCard(
                    modifier = Modifier.padding(horizontal = Size.small),
                    listCardType = listCardType,
                ) {
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
}

@Composable
private fun SummaryHeader(text: String, isRefreshing: Boolean, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small, vertical = Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(),
        )
        AnimatedVisibility(isRefreshing) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(Size.large),
                trackStroke =
                    Stroke(
                        width = with(LocalDensity.current) { Size.extraTiny.toPx() },
                        cap = StrokeCap.Round,
                    ),
                stroke =
                    Stroke(
                        width = with(LocalDensity.current) { Size.extraTiny.toPx() },
                        cap = StrokeCap.Round,
                    ),
            )
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
