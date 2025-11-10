package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.feed.DownloadScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenState
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import eu.kanade.tachiyomi.ui.feed.FeedSettingActions
import eu.kanade.tachiyomi.ui.feed.FeedViewModel
import eu.kanade.tachiyomi.ui.feed.HistoryScreenPagingState
import eu.kanade.tachiyomi.ui.feed.SummaryScreenPagingState
import eu.kanade.tachiyomi.ui.feed.UpdatesScreenPagingState
import eu.kanade.tachiyomi.ui.main.states.PullRefreshState
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.ButtonGroup
import org.nekomanga.presentation.components.scaffold.RootScaffold
import org.nekomanga.presentation.screens.feed.FeedBottomSheet
import org.nekomanga.presentation.screens.feed.FeedScreenContent
import org.nekomanga.presentation.screens.feed.FeedScreenDialogs
import org.nekomanga.presentation.screens.feed.FeedScreenTopBar
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedScreen(
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    feedViewModel: FeedViewModel,
    mainDropdown: AppBar.MainDropdown,
    mainDropdownShowing: Boolean,
    openManga: (Long) -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    val context = LocalContext.current

    FeedWrapper(
        navigationRail = navigationRail,
        bottomBar = bottomBar,
        feedScreenFlow = feedViewModel.feedScreenState,
        updateScreenFlow = feedViewModel.updatesScreenPagingState,
        historyScreenFlow = feedViewModel.historyScreenPagingState,
        summaryScreenFlow = feedViewModel.summaryScreenPagingState,
        windowSizeClass = windowSizeClass,
        mainDropdown = mainDropdown,
        mainDropdownShowing = mainDropdownShowing,
        loadNextPage = feedViewModel::loadNextPage,
        feedSettingActions =
            FeedSettingActions(
                groupHistoryClick = feedViewModel::toggleGroupHistoryType,
                clearHistoryClick = feedViewModel::deleteAllHistoryForAllManga,
                switchUploadsSortOrder = feedViewModel::toggleUploadsSortOrder,
                outlineCoversClick = feedViewModel::toggleOutlineCovers,
                outlineCardsClick = feedViewModel::toggleOutlineCards,
                clearDownloadQueueClick = feedViewModel::clearDownloadQueue,
                toggleDownloadOnlyOnWifi = feedViewModel::toggleDownloadOnlyOnWifi,
                toggleGroupUpdateChapters = feedViewModel::togglerGroupUpdateChapters,
                toggleSwipeRefresh = feedViewModel::toggleSwipeRefresh,
            ),
        feedScreenActions =
            FeedScreenActions(
                mangaClick = openManga,
                chapterClick = { mangaId, chapterId ->
                    context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
                },
                chapterSwipe = feedViewModel::toggleChapterRead,
                switchViewType = feedViewModel::switchViewType,
                deleteAllHistoryClick = feedViewModel::deleteAllHistory,
                deleteHistoryClick = feedViewModel::deleteHistory,
                search = feedViewModel::search,
                downloadClick = { chapterItem, feedManga, downloadAction ->
                    if (
                        MdConstants.UnsupportedOfficialGroupList.contains(
                            chapterItem.chapter.scanlator
                        )
                    ) {
                        context.toast("${chapterItem.chapter.scanlator} not supported, try WebView")
                    } else if (chapterItem.chapter.isUnavailable) {
                        context.toast("Chapter is not available")
                    } else {
                        feedViewModel.downloadChapter(chapterItem, feedManga, downloadAction)
                    }
                },
                toggleShowingDownloads = feedViewModel::toggleShowingDownloads,
                updateLibrary = { start ->
                    if (LibraryUpdateJob.isRunning(context) && !start) {
                        LibraryUpdateJob.stop(context)
                    } else if (!LibraryUpdateJob.isRunning(context) && start) {
                        LibraryUpdateJob.startNow(context)
                    }
                },
            ),
        downloadScreenActions =
            DownloadScreenActions(
                downloadSwiped = feedViewModel::removeDownload,
                fabClick = feedViewModel::toggleDownloader,
                moveDownloadClick = feedViewModel::moveDownload,
                moveSeriesClick = feedViewModel::moveDownloadSeries,
                cancelSeriesClick = feedViewModel::cancelDownloadSeries,
            ),
    )
}

@Composable
private fun FeedWrapper(
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    feedScreenFlow: StateFlow<FeedScreenState>,
    updateScreenFlow: StateFlow<UpdatesScreenPagingState>,
    historyScreenFlow: StateFlow<HistoryScreenPagingState>,
    summaryScreenFlow: StateFlow<SummaryScreenPagingState>,
    windowSizeClass: WindowSizeClass,
    mainDropdown: AppBar.MainDropdown,
    mainDropdownShowing: Boolean,
    loadNextPage: () -> Unit,
    feedSettingActions: FeedSettingActions,
    feedScreenActions: FeedScreenActions,
    downloadScreenActions: DownloadScreenActions,
) {
    val feedScreenState by feedScreenFlow.collectAsState()
    val updatesPagingScreenState by updateScreenFlow.collectAsState()
    val historyPagingScreenState by historyScreenFlow.collectAsState()
    val summaryScreenPagingState by summaryScreenFlow.collectAsState()

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    val feedScreenType = feedScreenState.feedScreenType

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    var downloadScreenVisible by
        remember(feedScreenState.showingDownloads, feedScreenState.downloads.size) {
            mutableStateOf(
                feedScreenState.showingDownloads && feedScreenState.downloads.isNotEmpty()
            )
        }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showBottomSheet) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { showBottomSheet = false },
                content = {
                    Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                        FeedBottomSheet(
                            feedScreenType = feedScreenState.feedScreenType,
                            downloadScreenVisible = downloadScreenVisible,
                            downloadOnlyOnWifi = feedScreenState.downloadOnlyOnWifi,
                            historyGrouping = historyPagingScreenState.historyGrouping,
                            sortByFetched = updatesPagingScreenState.updatesSortedByFetch,
                            outlineCovers = feedScreenState.outlineCovers,
                            outlineCards = feedScreenState.outlineCards,
                            swipeRefreshEnabled = feedScreenState.swipeRefreshEnabled,
                            groupUpdateChapters = feedScreenState.groupUpdateChapters,
                            groupHistoryClick = { feedHistoryGroup ->
                                feedSettingActions.groupHistoryClick(feedHistoryGroup)
                            },
                            clearHistoryClick = { showClearHistoryDialog = true },
                            clearDownloadsClick = { showClearDownloadsDialog = true },
                            sortClick = { feedSettingActions.switchUploadsSortOrder() },
                            outlineCoversClick = { feedSettingActions.outlineCoversClick() },
                            outlineCardsClick = { feedSettingActions.outlineCardsClick() },
                            toggleDownloadOnWifi = {
                                feedSettingActions.toggleDownloadOnlyOnWifi()
                            },
                            toggleGroupUpdateChapters = {
                                feedSettingActions.toggleGroupUpdateChapters()
                            },
                            toggleSwipeRefresh = { feedSettingActions.toggleSwipeRefresh() },
                        )
                    }
                },
            )
        }
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        val pullRefreshState =
            remember(
                feedScreenState.swipeRefreshEnabled,
                feedScreenState.isRefreshing,
                feedScreenActions.updateLibrary,
            ) {
                PullRefreshState(
                    enabled = feedScreenState.swipeRefreshEnabled,
                    isRefreshing = feedScreenState.isRefreshing,
                    onRefresh = { feedScreenActions.updateLibrary(true) },
                )
            }

        RootScaffold(
            pullRefreshState = pullRefreshState,
            scrollBehavior = scrollBehavior,
            mainSettingsExpanded = mainDropdownShowing,
            navigationRail = navigationRail,
            bottomBar = bottomBar,
            topBar = {
                FeedScreenTopBar(
                    scrollBehavior = scrollBehavior,
                    mainDropDown = mainDropdown,
                    feedScreenState = feedScreenState,
                    feedScreenActions = feedScreenActions,
                    openSheetClick = { showBottomSheet = true },
                )
            },
        ) { contentPadding ->
            val recyclerContentPadding = PaddingValues(bottom = Size.huge)

            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                FeedScreenContent(
                    downloadScreenVisible = downloadScreenVisible,
                    contentPadding = recyclerContentPadding,
                    feedScreenState = feedScreenState,
                    summaryScreenPagingState = summaryScreenPagingState,
                    historyPagingScreenState = historyPagingScreenState,
                    updatesPagingScreenState = updatesPagingScreenState,
                    downloadScreenActions = downloadScreenActions,
                    feedScreenActions = feedScreenActions,
                    loadNextPage = loadNextPage,
                )

                if (!feedScreenState.firstLoad) {
                    val buttonItems = remember {
                        listOf(
                            FeedScreenType.Summary,
                            FeedScreenType.History,
                            FeedScreenType.Updates,
                        )
                    }

                    val downloadButton = "downloads"
                    val items: List<Any> =
                        if (feedScreenState.downloads.isNotEmpty()) {
                            buttonItems + downloadButton
                        } else {
                            buttonItems
                        }

                    val selectedItem: Any =
                        when (feedScreenState.showingDownloads) {
                            true -> downloadButton
                            false -> feedScreenType
                        }

                    ButtonGroup(
                        modifier =
                            Modifier.align(Alignment.BottomCenter).padding(horizontal = Size.tiny),
                        items = items,
                        selectedItem = selectedItem,
                        onItemClick = { item ->
                            scope.launch { sheetState.hide() }
                            if (item is FeedScreenType) {
                                if (feedScreenState.showingDownloads) {
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
                                        FeedScreenType.History -> stringResource(R.string.history)

                                        FeedScreenType.Updates -> stringResource(R.string.updates)

                                        FeedScreenType.Summary -> stringResource(R.string.summary)
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
                                    contentDescription = stringResource(id = R.string.downloads),
                                )
                            }
                        }
                    }
                }
            }
        }

        FeedScreenDialogs(
            showClearHistoryDialog = showClearHistoryDialog,
            showClearDownloadsDialog = showClearDownloadsDialog,
            onClearHistoryDismiss = { showClearHistoryDialog = false },
            onClearHistoryConfirm = { feedSettingActions.clearHistoryClick() },
            onClearDownloadsDismiss = { showClearDownloadsDialog = false },
            onClearDownloadsConfirm = { feedSettingActions.clearDownloadQueueClick() },
            scope = scope,
            sheetStateHide = { sheetState.hide() },
        )
    }
}
