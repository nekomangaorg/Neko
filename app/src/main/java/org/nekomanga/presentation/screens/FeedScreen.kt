package org.nekomanga.presentation.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.ui.feed.DownloadScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenState
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import eu.kanade.tachiyomi.ui.feed.FeedSettingActions
import eu.kanade.tachiyomi.ui.feed.HistoryScreenPagingState
import eu.kanade.tachiyomi.ui.feed.SummaryScreenPagingState
import eu.kanade.tachiyomi.ui.feed.UpdatesScreenPagingState
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.ButtonGroup
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dialog.ClearDownloadQueueDialog
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.components.rememberNavBarPadding
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.download.DownloadScreen
import org.nekomanga.presentation.screens.feed.FeedBottomSheet
import org.nekomanga.presentation.screens.feed.FeedPage
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedScreen(
    feedScreenState: State<FeedScreenState>,
    updatesPagingScreenState: State<UpdatesScreenPagingState>,
    historyPagingScreenState: State<HistoryScreenPagingState>,
    summaryScreenPagingState: State<SummaryScreenPagingState>,
    windowSizeClass: WindowSizeClass,
    loadNextPage: () -> Unit,
    legacySideNav: Boolean,
    feedSettingActions: FeedSettingActions,
    feedScreenActions: FeedScreenActions,
    downloadScreenActions: DownloadScreenActions,
    incognitoClick: () -> Unit,
    settingsClick: () -> Unit,
    statsClick: () -> Unit,
    helpClick: () -> Unit,
    aboutClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState =
        rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
            animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        )

    var mainDropdownShowing by remember { mutableStateOf(false) }

    val feedScreenType = feedScreenState.value.feedScreenType

    val searchHint =
        when (feedScreenType) {
            FeedScreenType.History -> stringResource(R.string.search_history)
            FeedScreenType.Updates -> stringResource(R.string.search_updates)
            else -> ""
        }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    // val sideNav = rememberSideBarVisible(windowSizeClass, feedScreenState.value.sideNavMode)
    val actualSideNav = legacySideNav
    val navBarPadding = rememberNavBarPadding(actualSideNav)

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    var downloadScreenVisible by
        remember(feedScreenState.value.showingDownloads, feedScreenState.value.downloads.size) {
            mutableStateOf(
                feedScreenState.value.showingDownloads &&
                    feedScreenState.value.downloads.isNotEmpty()
            )
        }

    Box(
        modifier =
            Modifier.fillMaxSize().conditional(mainDropdownShowing) {
                this.blur(Size.medium).clickable(enabled = false) {}
            }
    ) {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetShape = RoundedCornerShape(Shapes.sheetRadius),
            sheetContent = {
                Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                    FeedBottomSheet(
                        contentPadding = navBarPadding,
                        feedScreenType = feedScreenState.value.feedScreenType,
                        downloadScreenVisible = downloadScreenVisible,
                        downloadOnlyOnWifi = feedScreenState.value.downloadOnlyOnWifi,
                        historyGrouping = historyPagingScreenState.value.historyGrouping,
                        sortByFetched = updatesPagingScreenState.value.updatesSortedByFetch,
                        outlineCovers = feedScreenState.value.outlineCovers,
                        outlineCards = feedScreenState.value.outlineCards,
                        swipeRefreshEnabled = feedScreenState.value.swipeRefreshEnabled,
                        groupUpdateChapters = feedScreenState.value.groupUpdateChapters,
                        groupHistoryClick = { feedHistoryGroup ->
                            feedSettingActions.groupHistoryClick(feedHistoryGroup)
                        },
                        clearHistoryClick = { showClearHistoryDialog = true },
                        clearDownloadsClick = { showClearDownloadsDialog = true },
                        sortClick = { feedSettingActions.switchUploadsSortOrder() },
                        outlineCoversClick = { feedSettingActions.outlineCoversClick() },
                        outlineCardsClick = { feedSettingActions.outlineCardsClick() },
                        toggleDownloadOnWifi = { feedSettingActions.toggleDownloadOnlyOnWifi() },
                        toggleGroupUpdateChapters = {
                            feedSettingActions.toggleGroupUpdateChapters()
                        },
                        toggleSwipeRefresh = { feedSettingActions.toggleSwipeRefresh() },
                    )
                }
            },
        ) {
            PullRefresh(
                enabled = feedScreenState.value.swipeRefreshEnabled,
                isRefreshing = feedScreenState.value.isRefreshing,
                onRefresh = { feedScreenActions.updateLibrary(true) },
            ) {
                NekoScaffold(
                    type =
                        if (
                            feedScreenState.value.showingDownloads ||
                                feedScreenType == FeedScreenType.Summary
                        )
                            NekoScaffoldType.Title
                        else NekoScaffoldType.SearchOutline,
                    title =
                        if (feedScreenType == FeedScreenType.Summary)
                            stringResource(R.string.summary)
                        else "",
                    searchPlaceHolder = searchHint,
                    incognitoMode = feedScreenState.value.incognitoMode,
                    isRoot = true,
                    onSearch = feedScreenActions.search,
                    actions = {
                        AppBarActions(
                            actions =
                                if (
                                    feedScreenState.value.feedScreenType != FeedScreenType.Summary
                                ) {
                                    listOf(
                                        AppBar.Action(
                                            title = UiText.StringResource(R.string.settings),
                                            icon = Icons.Outlined.Tune,
                                            onClick = { scope.launch { sheetState.show() } },
                                        )
                                    )
                                } else {
                                    listOf()
                                } +
                                    listOf(
                                        AppBar.MainDropdown(
                                            incognitoMode = feedScreenState.value.incognitoMode,
                                            incognitoModeClick = incognitoClick,
                                            settingsClick = settingsClick,
                                            statsClick = statsClick,
                                            aboutClick = aboutClick,
                                            helpClick = helpClick,
                                            menuShowing = { visible ->
                                                mainDropdownShowing = visible
                                            },
                                        )
                                    )
                        )
                    },
                    content = { incomingContentPadding ->
                        val recyclerContentPadding =
                            PaddingValues(
                                top = incomingContentPadding.calculateTopPadding(),
                                bottom =
                                    if (actualSideNav) {
                                        Size.navBarSize
                                    } else {
                                        Size.navBarSize
                                    } +
                                        WindowInsets.navigationBars
                                            .asPaddingValues()
                                            .calculateBottomPadding(),
                            )

                        Box(
                            modifier =
                                Modifier.padding(bottom = navBarPadding.calculateBottomPadding())
                                    .fillMaxSize()
                        ) {
                            if (
                                (feedScreenState.value.feedScreenType == FeedScreenType.History &&
                                    historyPagingScreenState.value.pageLoading &&
                                    historyPagingScreenState.value.offset == 0) ||
                                    (feedScreenState.value.feedScreenType ==
                                        FeedScreenType.Updates &&
                                        updatesPagingScreenState.value.pageLoading &&
                                        updatesPagingScreenState.value.offset == 0)
                            ) {
                                ContainedLoadingIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            val (feedManga, hasMoreResults) =
                                when (feedScreenType) {
                                    FeedScreenType.Summary -> {
                                        if (
                                            historyPagingScreenState.value
                                                .searchHistoryFeedMangaList
                                                .isNotEmpty()
                                        ) {
                                            historyPagingScreenState.value
                                                .searchHistoryFeedMangaList to false
                                        } else {
                                            historyPagingScreenState.value.historyFeedMangaList to
                                                historyPagingScreenState.value.hasMoreResults
                                        }
                                    }

                                    FeedScreenType.History -> {
                                        if (
                                            historyPagingScreenState.value
                                                .searchHistoryFeedMangaList
                                                .isNotEmpty()
                                        ) {
                                            historyPagingScreenState.value
                                                .searchHistoryFeedMangaList to false
                                        } else {
                                            historyPagingScreenState.value.historyFeedMangaList to
                                                historyPagingScreenState.value.hasMoreResults
                                        }
                                    }

                                    FeedScreenType.Updates -> {
                                        if (
                                            updatesPagingScreenState.value
                                                .searchUpdatesFeedMangaList
                                                .isNotEmpty()
                                        ) {
                                            updatesPagingScreenState.value
                                                .searchUpdatesFeedMangaList to false
                                        } else {
                                            updatesPagingScreenState.value.updatesFeedMangaList to
                                                updatesPagingScreenState.value.hasMoreResults
                                        }
                                    }
                                }

                            if (
                                feedScreenState.value.showingDownloads &&
                                    feedScreenState.value.downloads.isEmpty()
                            ) {
                                feedScreenActions.toggleShowingDownloads()
                            }

                            when (downloadScreenVisible) {
                                true ->
                                    DownloadScreen(
                                        contentPadding = recyclerContentPadding,
                                        downloads = feedScreenState.value.downloads,
                                        downloaderStatus = feedScreenState.value.downloaderStatus,
                                        downloadScreenActions = downloadScreenActions,
                                    )

                                false -> {
                                    FeedPage(
                                        contentPadding = recyclerContentPadding,
                                        summaryScreenPagingState = summaryScreenPagingState,
                                        feedMangaList = feedManga,
                                        hasMoreResults = hasMoreResults,
                                        loadingResults =
                                            if (
                                                feedScreenState.value.feedScreenType ==
                                                    FeedScreenType.History
                                            )
                                                historyPagingScreenState.value.pageLoading
                                            else updatesPagingScreenState.value.pageLoading,
                                        groupedBySeries = feedScreenState.value.groupUpdateChapters,
                                        feedScreenType = feedScreenState.value.feedScreenType,
                                        historyGrouping =
                                            historyPagingScreenState.value.historyGrouping,
                                        outlineCovers = feedScreenState.value.outlineCovers,
                                        outlineCards = feedScreenState.value.outlineCards,
                                        updatesFetchSort =
                                            updatesPagingScreenState.value.updatesSortedByFetch,
                                        feedScreenActions = feedScreenActions,
                                        loadNextPage = loadNextPage,
                                    )
                                }
                            }

                            if (!feedScreenState.value.firstLoad) {
                                val buttonItems = remember {
                                    listOf(
                                        FeedScreenType.Summary,
                                        FeedScreenType.History,
                                        FeedScreenType.Updates,
                                    )
                                }

                                val downloadButton = "downloads"
                                val items: List<Any> =
                                    if (feedScreenState.value.downloads.isNotEmpty()) {
                                        buttonItems + downloadButton
                                    } else {
                                        buttonItems
                                    }

                                val selectedItem: Any =
                                    when (feedScreenState.value.showingDownloads) {
                                        true -> downloadButton
                                        false -> feedScreenType
                                    }

                                ButtonGroup(
                                    modifier =
                                        Modifier.align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .padding(horizontal = Size.tiny),
                                    items = items,
                                    selectedItem = selectedItem,
                                    onItemClick = { item ->
                                        scope.launch { sheetState.hide() }
                                        if (item is FeedScreenType) {
                                            if (feedScreenState.value.showingDownloads) {
                                                feedScreenActions.toggleShowingDownloads()
                                            }
                                            if (feedScreenType != item) {
                                                feedScreenActions.switchViewType(item)
                                            }
                                        } else {
                                            feedScreenActions.toggleShowingDownloads()
                                        }
                                    },
                                ) { item ->
                                    when (item) {
                                        is FeedScreenType -> {
                                            val name =
                                                when (item) {
                                                    FeedScreenType.History ->
                                                        stringResource(R.string.history)
                                                    FeedScreenType.Updates ->
                                                        stringResource(R.string.updates)
                                                    FeedScreenType.Summary ->
                                                        stringResource(R.string.summary)
                                                }
                                            Text(
                                                text = name,
                                                style =
                                                    MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.Downloading,
                                                contentDescription =
                                                    stringResource(id = R.string.downloads),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
        // this is needed for Android SDK where blur isn't available
        if (mainDropdownShowing && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = NekoColors.mediumAlphaLowContrast))
            )
        }
    }
    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_history_confirmation_1),
            body = stringResource(R.string.clear_history_confirmation_2),
            confirmButton = stringResource(id = R.string.clear),
            onDismiss = { showClearHistoryDialog = false },
            onConfirm = { feedSettingActions.clearHistoryClick() },
        )
    }

    if (showClearDownloadsDialog) {
        ClearDownloadQueueDialog(
            onDismiss = { showClearDownloadsDialog = false },
            onConfirm = {
                feedSettingActions.clearDownloadQueueClick()
                scope.launch { sheetState.hide() }
            },
        )
    }
}
