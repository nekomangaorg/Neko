package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.feed.DownloadScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenState
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import eu.kanade.tachiyomi.ui.feed.HistoryScreenPagingState
import eu.kanade.tachiyomi.ui.feed.SummaryScreenPagingState
import eu.kanade.tachiyomi.ui.feed.UpdatesScreenPagingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.components.dialog.ClearDownloadQueueDialog
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.screens.download.DownloadScreen

@Composable
fun FeedScreenDialogs(
    showClearHistoryDialog: Boolean,
    showClearDownloadsDialog: Boolean,
    onClearHistoryDismiss: () -> Unit,
    onClearHistoryConfirm: () -> Unit,
    onClearDownloadsDismiss: () -> Unit,
    onClearDownloadsConfirm: () -> Unit,
    scope: CoroutineScope,
    sheetStateHide: suspend () -> Unit,
) {
    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_history_confirmation_1),
            body = stringResource(R.string.clear_history_confirmation_2),
            confirmButton = stringResource(id = R.string.clear),
            onDismiss = onClearHistoryDismiss,
            onConfirm = onClearHistoryConfirm,
        )
    }

    if (showClearDownloadsDialog) {
        ClearDownloadQueueDialog(
            onDismiss = onClearDownloadsDismiss,
            onConfirm = {
                onClearDownloadsConfirm()
                scope.launch { sheetStateHide() }
            },
        )
    }
}

@Composable
fun FeedScreenContent(
    downloadScreenVisible: Boolean,
    contentPadding: PaddingValues,
    feedScreenState: FeedScreenState,
    summaryScreenPagingState: SummaryScreenPagingState,
    historyPagingScreenState: HistoryScreenPagingState,
    updatesPagingScreenState: UpdatesScreenPagingState,
    downloadScreenActions: DownloadScreenActions,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (
            (feedScreenState.feedScreenType == FeedScreenType.History &&
                historyPagingScreenState.pageLoading &&
                historyPagingScreenState.offset == 0) ||
                (feedScreenState.feedScreenType == FeedScreenType.Updates &&
                    updatesPagingScreenState.pageLoading &&
                    updatesPagingScreenState.offset == 0)
        ) {
            ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        val (feedManga, hasMoreResults) =
            remember(
                feedScreenState.feedScreenType,
                historyPagingScreenState.searchHistoryFeedMangaList,
                historyPagingScreenState.historyFeedMangaList,
                updatesPagingScreenState.searchUpdatesFeedMangaList,
                updatesPagingScreenState.updatesFeedMangaList,
            ) {
                when (feedScreenState.feedScreenType) {
                    FeedScreenType.Summary -> {
                        if (historyPagingScreenState.searchHistoryFeedMangaList.isNotEmpty()) {
                            historyPagingScreenState.searchHistoryFeedMangaList to false
                        } else {
                            historyPagingScreenState.historyFeedMangaList to
                                historyPagingScreenState.hasMoreResults
                        }
                    }
                    FeedScreenType.History -> {
                        if (historyPagingScreenState.searchHistoryFeedMangaList.isNotEmpty()) {
                            historyPagingScreenState.searchHistoryFeedMangaList to false
                        } else {
                            historyPagingScreenState.historyFeedMangaList to
                                historyPagingScreenState.hasMoreResults
                        }
                    }
                    FeedScreenType.Updates -> {
                        if (updatesPagingScreenState.searchUpdatesFeedMangaList.isNotEmpty()) {
                            updatesPagingScreenState.searchUpdatesFeedMangaList to false
                        } else {
                            updatesPagingScreenState.updatesFeedMangaList to
                                updatesPagingScreenState.hasMoreResults
                        }
                    }
                }
            }

        if (feedScreenState.showingDownloads && feedScreenState.downloads.isEmpty()) {
            feedScreenActions.toggleShowingDownloads()
        }

        when (downloadScreenVisible) {
            true ->
                DownloadScreen(
                    contentPadding = contentPadding,
                    downloads = feedScreenState.downloads,
                    downloaderStatus = feedScreenState.downloaderStatus,
                    downloadScreenActions = downloadScreenActions,
                )
            false -> {
                FeedPage(
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                    summaryScreenPagingState = summaryScreenPagingState,
                    feedMangaList = feedManga,
                    hasMoreResults = hasMoreResults,
                    loadingResults =
                        if (feedScreenState.feedScreenType == FeedScreenType.History) {
                            historyPagingScreenState.pageLoading
                        } else {
                            updatesPagingScreenState.pageLoading
                        },
                    groupedBySeries = feedScreenState.groupUpdateChapters,
                    feedScreenType = feedScreenState.feedScreenType,
                    historyGrouping = historyPagingScreenState.historyGrouping,
                    outlineCovers = feedScreenState.outlineCovers,
                    outlineCards = feedScreenState.outlineCards,
                    useVividColorHeaders = feedScreenState.useVividColorHeaders,
                    updatesFetchSort = updatesPagingScreenState.updatesSortedByFetch,
                    feedScreenActions = feedScreenActions,
                    loadNextPage = loadNextPage,
                )
            }
        }
    }
}
