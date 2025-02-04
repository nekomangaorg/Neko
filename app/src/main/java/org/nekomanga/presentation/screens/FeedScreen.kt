package org.nekomanga.presentation.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.LinearProgressIndicator
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
import eu.kanade.tachiyomi.ui.feed.DownloadScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenState
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import eu.kanade.tachiyomi.ui.feed.FeedSettingActions
import eu.kanade.tachiyomi.ui.feed.HistoryScreenPagingState
import eu.kanade.tachiyomi.ui.feed.UpdatesScreenPagingState
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.FooterFilterChip
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dialog.ClearDownloadQueueDialog
import org.nekomanga.presentation.components.dialog.DeleteAllHistoryDialog
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
                refreshing = feedScreenState.value.isRefreshing,
                onRefresh = { feedScreenActions.updateLibrary(true) },
                indicatorOffset =
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding() +
                        Size.extraLarge,
            ) {
                NekoScaffold(
                    type =
                        if (feedScreenState.value.showingDownloads) NekoScaffoldType.Title
                        else NekoScaffoldType.SearchOutline,
                    incognitoMode = feedScreenState.value.incognitoMode,
                    searchPlaceHolder = searchHint,
                    isRoot = true,
                    onSearch = feedScreenActions.search,
                    actions = {
                        AppBarActions(
                            actions =
                                listOf(
                                    AppBar.Action(
                                        title = UiText.StringResource(R.string.settings),
                                        icon = Icons.Outlined.Tune,
                                        onClick = { scope.launch { sheetState.show() } },
                                    ),
                                    AppBar.MainDropdown(
                                        incognitoMode = feedScreenState.value.incognitoMode,
                                        incognitoModeClick = incognitoClick,
                                        settingsClick = settingsClick,
                                        statsClick = statsClick,
                                        aboutClick = aboutClick,
                                        helpClick = helpClick,
                                        menuShowing = { visible -> mainDropdownShowing = visible },
                                    ),
                                )
                        )
                    },
                ) { incomingContentPadding ->
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
                        val (feedManga, hasMoreResults) =
                            when (feedScreenType) {
                                FeedScreenType.Summary -> {
                                    if (
                                        historyPagingScreenState.value.searchHistoryFeedMangaList
                                            .isNotEmpty()
                                    ) {
                                        historyPagingScreenState.value.searchHistoryFeedMangaList to
                                            false
                                    } else {
                                        historyPagingScreenState.value.historyFeedMangaList to
                                            historyPagingScreenState.value.hasMoreResults
                                    }
                                }
                                FeedScreenType.History -> {
                                    if (
                                        historyPagingScreenState.value.searchHistoryFeedMangaList
                                            .isNotEmpty()
                                    ) {
                                        historyPagingScreenState.value.searchHistoryFeedMangaList to
                                            false
                                    } else {
                                        historyPagingScreenState.value.historyFeedMangaList to
                                            historyPagingScreenState.value.hasMoreResults
                                    }
                                }
                                FeedScreenType.Updates -> {
                                    if (
                                        updatesPagingScreenState.value.searchUpdatesFeedMangaList
                                            .isNotEmpty()
                                    ) {
                                        updatesPagingScreenState.value.searchUpdatesFeedMangaList to
                                            false
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
                            ScreenFooter(
                                screenType = feedScreenType,
                                modifier = Modifier.align(Alignment.BottomStart),
                                loadingMore =
                                    if (
                                        feedScreenState.value.feedScreenType ==
                                            FeedScreenType.History
                                    )
                                        historyPagingScreenState.value.pageLoading
                                    else updatesPagingScreenState.value.pageLoading,
                                showDownloads = feedScreenState.value.downloads.isNotEmpty(),
                                downloadsSelected = feedScreenState.value.showingDownloads,
                                downloadsClicked = feedScreenActions.toggleShowingDownloads,
                                screenTypeClick = { newScreenType: FeedScreenType ->
                                    scope.launch { sheetState.hide() }
                                    if (feedScreenState.value.showingDownloads) {
                                        feedScreenActions.toggleShowingDownloads()
                                    }
                                    if (feedScreenType != newScreenType) {
                                        feedScreenActions.switchViewType(newScreenType)
                                    }
                                },
                            )
                        }
                    }
                }
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
        DeleteAllHistoryDialog(
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

@Composable
private fun ScreenFooter(
    screenType: FeedScreenType,
    modifier: Modifier = Modifier,
    loadingMore: Boolean,
    showDownloads: Boolean,
    downloadsSelected: Boolean,
    downloadsClicked: () -> Unit,
    screenTypeClick: (FeedScreenType) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Size.none),
    ) {
        if (loadingMore) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Size.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Gap(Size.tiny)
            /* FooterFilterChip(
                selected = screenType == FeedScreenType.Summary && downloadsSelected == false,
                onClick = { screenTypeClick(FeedScreenType.Summary) },
                name = stringResource(R.string.summary),
            )*/
            FooterFilterChip(
                selected = screenType == FeedScreenType.History && downloadsSelected == false,
                onClick = { screenTypeClick(FeedScreenType.History) },
                name = stringResource(R.string.history),
            )

            FooterFilterChip(
                selected = screenType == FeedScreenType.Updates && downloadsSelected == false,
                onClick = { screenTypeClick(FeedScreenType.Updates) },
                name = stringResource(R.string.updates),
            )

            if (showDownloads) {
                FooterFilterChip(
                    selected = downloadsSelected,
                    onClick = downloadsClicked,
                    name = "",
                    icon = Icons.Default.Downloading,
                )
            }
        }
    }
}
