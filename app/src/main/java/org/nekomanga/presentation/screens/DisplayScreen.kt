package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenState
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.browse.DisplayScreenContent
import org.nekomanga.presentation.screens.browse.DisplayScreenSheet
import org.nekomanga.presentation.screens.browse.DisplaySheetScreen
import org.nekomanga.presentation.theme.Size

@Composable
fun DisplayScreen(
    displayScreenState: State<DisplayScreenState>,
    displayScreenType: DisplayScreenType,
    switchDisplayClick: () -> Unit,
    onBackPress: () -> Unit,
    openManga: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>) -> Unit,
    loadNextPage: () -> Unit,
    retryClick: () -> Unit,
    onRefresh: () -> Unit,
    libraryEntryVisibilityClick: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentBottomSheet: DisplaySheetScreen? by remember { mutableStateOf(null) }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) { onRefresh() }
    }

    LaunchedEffect(displayScreenState.value.isRefreshing) {
        if (!displayScreenState.value.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(currentBottomSheet) {
        if (currentBottomSheet != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    val openSheet: (DisplaySheetScreen) -> Unit = { scope.launch { currentBottomSheet = it } }

    if (currentBottomSheet != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { currentBottomSheet = null },
            content = {
                Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                    currentBottomSheet?.let { currentSheet ->
                        DisplayScreenSheet(
                            currentScreen = currentSheet,
                            addNewCategory = addNewCategory,
                            contentPadding =
                                WindowInsets.navigationBars
                                    .only(WindowInsetsSides.Bottom)
                                    .asPaddingValues(),
                            closeSheet = { currentBottomSheet = null },
                            categories = displayScreenState.value.categories,
                            isList = displayScreenState.value.isList,
                            libraryEntryVisibility = displayScreenState.value.libraryEntryVisibility,
                        )
                    }
                }
            },
        )
    }
    NekoScaffold(
        onNavigationIconClicked = onBackPress,
        incognitoMode = displayScreenState.value.incognitoMode,
        title =
            when (displayScreenType) {
                is DisplayScreenType.List -> displayScreenType.title
                is DisplayScreenType.Similar -> stringResource(id = R.string.similar)
                else -> {
                    stringResource(
                        when (displayScreenType) {
                            is DisplayScreenType.FeedUpdates -> R.string.feed_updates
                            is DisplayScreenType.LatestChapters -> R.string.latest
                            is DisplayScreenType.PopularNewTitles -> R.string.popular_new_titles
                            is DisplayScreenType.RecentlyAdded -> R.string.recently_added
                            else -> R.string.missing
                        }
                    )
                }
            },
        actions = {
            AppBarActions(
                actions =
                    listOf(
                        AppBar.Action(
                            title = UiText.StringResource(R.string.settings),
                            icon = Icons.Outlined.Tune,
                            onClick = {
                                scope.launch {
                                    openSheet(
                                        DisplaySheetScreen.BrowseDisplayOptionsSheet(
                                            showIsList = true,
                                            switchDisplayClick = switchDisplayClick,
                                            libraryEntryVisibilityClick =
                                                libraryEntryVisibilityClick,
                                        )
                                    )
                                }
                            },
                        )
                    )
            )
        },
        content = { incomingContentPadding ->
            val contentPadding =
                PaddingValues(
                    bottom =
                        WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues()
                            .calculateBottomPadding(),
                    top = incomingContentPadding.calculateTopPadding(),
                )

            Box(
                modifier =
                    Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection).fillMaxSize()
            ) {
                if (
                    (displayScreenState.value.isLoading || displayScreenState.value.isRefreshing) &&
                        displayScreenState.value.filteredDisplayManga.isEmpty()
                ) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    val haptic = LocalHapticFeedback.current
                    fun mangaLongClick(displayManga: DisplayManga) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!displayManga.inLibrary && displayScreenState.value.promptForCategories
                        ) {
                            scope.launch {
                                openSheet(
                                    DisplaySheetScreen.CategoriesSheet(
                                        setCategories = { selectedCategories ->
                                            scope.launch { sheetState.hide() }
                                            toggleFavorite(displayManga.mangaId, selectedCategories)
                                        }
                                    )
                                )
                            }
                        } else {
                            toggleFavorite(displayManga.mangaId, emptyList())
                        }
                    }

                    DisplayScreenContent(
                        displayScreenType = displayScreenType,
                        groupedManga = displayScreenState.value.filteredDisplayManga,
                        isList = displayScreenState.value.isList,
                        isComfortable = displayScreenState.value.isComfortableGrid,
                        rawColumns = displayScreenState.value.rawColumnCount,
                        shouldOutlineCover = displayScreenState.value.outlineCovers,
                        contentPadding = contentPadding,
                        mangaClick = openManga,
                        mangaLongClick = ::mangaLongClick,
                        loadNextPage = loadNextPage,
                        endReached = displayScreenState.value.endReached,
                    )
                }

                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        },
    )
}
