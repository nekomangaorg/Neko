package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ContainedLoadingIndicator
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenState
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenType
import eu.kanade.tachiyomi.ui.source.browse.BrowseViewModel
import eu.kanade.tachiyomi.ui.source.browse.FilterActions
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.ButtonGroup
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.scaffold.RootScaffold
import org.nekomanga.presentation.screens.browse.BrowseBottomSheet
import org.nekomanga.presentation.screens.browse.BrowseBottomSheetScreen
import org.nekomanga.presentation.screens.browse.BrowseFilterPage
import org.nekomanga.presentation.screens.browse.BrowseFollowsPage
import org.nekomanga.presentation.screens.browse.BrowseHomePage
import org.nekomanga.presentation.screens.browse.BrowseOtherPage
import org.nekomanga.presentation.screens.browse.BrowseScreenTopBar
import org.nekomanga.presentation.theme.Size

@Composable
fun BrowseScreen(
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    browseViewModel: BrowseViewModel,
    mainDropdown: AppBar.MainDropdown,
    mainDropdownShowing: Boolean,
    onNavigateTo: (NavKey) -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    val deepLinkManga by browseViewModel.deepLinkMangaFlow.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUpdateMangaForChanges by rememberUpdatedState(browseViewModel::updateMangaForChanges)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentUpdateMangaForChanges()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(deepLinkManga) {
        deepLinkManga?.let { mangaId ->
            browseViewModel.onDeepLinkMangaHandled()
            onNavigateTo(Screens.Manga(mangaId))
        }
    }

    BrowseWrapper(
        navigationRail = navigationRail,
        bottomBar = bottomBar,
        browseScreenFlow = browseViewModel.browseScreenState,
        mainDropdown = mainDropdown,
        mainDropdownShowing = mainDropdownShowing,
        switchDisplayClick = browseViewModel::switchDisplayMode,
        libraryEntryVisibilityClick = browseViewModel::switchLibraryEntryVisibility,
        windowSizeClass = windowSizeClass,
        homeScreenTitleClick = { type -> onNavigateTo(Screens.Display(type)) },
        openManga = { mangaId -> onNavigateTo(Screens.Manga(mangaId)) },
        filterActions =
            FilterActions(
                filterClick = browseViewModel::getSearchPage,
                filterChanged = browseViewModel::filterChanged,
                resetClick = browseViewModel::resetFilter,
                saveFilterClick = browseViewModel::saveFilter,
                deleteFilterClick = browseViewModel::deleteFilter,
                filterDefaultClick = browseViewModel::markFilterAsDefault,
                loadFilter = browseViewModel::loadFilter,
            ),
        addNewCategory = browseViewModel::addNewCategory,
        toggleFavorite = browseViewModel::toggleFavorite,
        loadNextPage = browseViewModel::loadNextItems,
        retryClick = browseViewModel::retry,
        otherClick = browseViewModel::otherClick,
        changeScreenType = browseViewModel::changeScreenType,
        randomClick = browseViewModel::randomManga,
    )
}

@Composable
private fun BrowseWrapper(
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    browseScreenFlow: StateFlow<BrowseScreenState>,
    mainDropdown: AppBar.MainDropdown,
    mainDropdownShowing: Boolean,
    switchDisplayClick: () -> Unit,
    libraryEntryVisibilityClick: (Int) -> Unit,
    windowSizeClass: WindowSizeClass,
    openManga: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>) -> Unit,
    loadNextPage: () -> Unit,
    retryClick: () -> Unit,
    otherClick: (String) -> Unit,
    filterActions: FilterActions,
    changeScreenType: (BrowseScreenType) -> Unit,
    homeScreenTitleClick: (DisplayScreenType) -> Unit,
    randomClick: () -> Unit,
) {

    val browseScreenState by browseScreenFlow.collectAsState()

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentBottomSheet: BrowseBottomSheetScreen? by remember { mutableStateOf(null) }

    val browseScreenType = browseScreenState.screenType

    LaunchedEffect(currentBottomSheet) {
        if (currentBottomSheet != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    val openSheet: (BrowseBottomSheetScreen) -> Unit = { scope.launch { currentBottomSheet = it } }

    if (currentBottomSheet != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { currentBottomSheet = null },
            content = {
                Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                    currentBottomSheet?.let { currentSheet ->
                        BrowseBottomSheet(
                            currentScreen = currentSheet,
                            browseScreenState = browseScreenState,
                            addNewCategory = addNewCategory,
                            closeSheet = {
                                scope.launch {
                                    sheetState.hide()
                                    currentBottomSheet = null
                                }
                            },
                            filterActions = filterActions,
                        )
                    }
                }
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    RootScaffold(
        scrollBehavior = scrollBehavior,
        mainSettingsExpanded = mainDropdownShowing,
        navigationRail = navigationRail,
        bottomBar = bottomBar,
        topBar = {
            BrowseScreenTopBar(
                browseScreenState = browseScreenState,
                scrollBehavior = scrollBehavior,
                mainDropDown = mainDropdown,
                openSheetClick = {
                    scope.launch {
                        openSheet(
                            BrowseBottomSheetScreen.BrowseDisplayOptionsSheet(
                                switchDisplayClick = switchDisplayClick,
                                libraryEntryVisibilityClick = libraryEntryVisibilityClick,
                            )
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // Create new padding that ignores the top bar's height
        val contentPadding =
            PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding(),
                top = 0.dp,
            )

        val recyclerContentPadding =
            PaddingValues(top = innerPadding.calculateTopPadding(), bottom = Size.huge)

        val haptic = LocalHapticFeedback.current
        fun mangaLongClick(displayManga: DisplayManga) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!displayManga.inLibrary && browseScreenState.promptForCategories) {
                scope.launch {
                    openSheet(
                        BrowseBottomSheetScreen.CategoriesSheet(
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

        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (browseScreenState.initialLoading) {
                ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (browseScreenState.error != null) {
                EmptyScreen(
                    message = browseScreenState.error!!,
                    actions =
                        persistentListOf(
                            Action(
                                text = UiText.StringResource(R.string.retry),
                                onClick = retryClick,
                            )
                        ),
                    contentPadding = recyclerContentPadding,
                )
            } else {
                Crossfade(targetState = browseScreenType) { type ->
                    when (type) {
                        BrowseScreenType.Homepage ->
                            BrowseHomePage(
                                browseHomePageManga = browseScreenState.homePageManga,
                                shouldOutlineCover = browseScreenState.outlineCovers,
                                useVividColorHeaders = browseScreenState.useVividColorHeaders,
                                titleClick = homeScreenTitleClick,
                                randomClick = randomClick,
                                onClick = { id -> openManga(id) },
                                onLongClick = ::mangaLongClick,
                                contentPadding = recyclerContentPadding,
                            )

                        BrowseScreenType.Follows -> {
                            BrowseFollowsPage(
                                displayMangaHolder = browseScreenState.displayMangaHolder,
                                isList = browseScreenState.isList,
                                isComfortableGrid = browseScreenState.isComfortableGrid,
                                outlineCovers = browseScreenState.outlineCovers,
                                rawColumnCount = browseScreenState.rawColumnCount,
                                contentPadding = recyclerContentPadding,
                                onClick = openManga,
                                onLongClick = ::mangaLongClick,
                            )
                        }

                        BrowseScreenType.Other -> {
                            BrowseOtherPage(
                                results = browseScreenState.otherResults,
                                contentPadding = recyclerContentPadding,
                                onClick = otherClick,
                            )
                        }

                        BrowseScreenType.Filter -> {
                            BrowseFilterPage(
                                displayMangaHolder = browseScreenState.displayMangaHolder,
                                isList = browseScreenState.isList,
                                isComfortableGrid = browseScreenState.isComfortableGrid,
                                outlineCovers = browseScreenState.outlineCovers,
                                rawColumnCount = browseScreenState.rawColumnCount,
                                pageLoading = browseScreenState.pageLoading,
                                lastPage = browseScreenState.endReached,
                                contentPadding = recyclerContentPadding,
                                onClick = openManga,
                                onLongClick = ::mangaLongClick,
                                loadNextPage = loadNextPage,
                            )
                        }

                        BrowseScreenType.None -> Unit
                    }
                }
            }

            // hide these on initial load
            if (!browseScreenState.hideFooterButton) {
                val items =
                    remember(browseScreenState.isLoggedIn) {
                        listOf(BrowseScreenType.Homepage) +
                            if (browseScreenState.isLoggedIn) {
                                listOf(BrowseScreenType.Follows)
                            } else {
                                emptyList()
                            } +
                            listOf(BrowseScreenType.Filter)
                    }

                val selectedItem =
                    when (browseScreenType) {
                        BrowseScreenType.Homepage -> BrowseScreenType.Homepage
                        BrowseScreenType.Follows -> BrowseScreenType.Follows
                        else -> BrowseScreenType.Filter
                    }

                ButtonGroup(
                    modifier =
                        Modifier.align(Alignment.BottomCenter).padding(horizontal = Size.tiny),
                    items = items,
                    selectedItem = selectedItem,
                    onItemClick = { item ->
                        scope.launch { sheetState.hide() }
                        val sameScreen = browseScreenType == item
                        val newIsFilterScreen = item == BrowseScreenType.Filter

                        if (sameScreen && !newIsFilterScreen) {
                            // do nothing
                        } else if (newIsFilterScreen) {
                            openSheet(BrowseBottomSheetScreen.FilterSheet())
                        } else {
                            changeScreenType(item)
                        }
                    },
                ) { item ->
                    val name =
                        when (item) {
                            BrowseScreenType.Homepage -> stringResource(id = R.string.home_page)
                            BrowseScreenType.Follows -> stringResource(R.string.follows)
                            else -> stringResource(R.string.search)
                        }
                    Text(
                        text = name,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
