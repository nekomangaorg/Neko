package org.nekomanga.presentation.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenState
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenType
import eu.kanade.tachiyomi.ui.source.browse.FilterActions
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.ButtonGroup
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.rememberNavBarPadding
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.browse.BrowseBottomSheet
import org.nekomanga.presentation.screens.browse.BrowseBottomSheetScreen
import org.nekomanga.presentation.screens.browse.BrowseFilterPage
import org.nekomanga.presentation.screens.browse.BrowseFollowsPage
import org.nekomanga.presentation.screens.browse.BrowseHomePage
import org.nekomanga.presentation.screens.browse.BrowseOtherPage
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun BrowseScreen(
    browseScreenState: State<BrowseScreenState>,
    switchDisplayClick: () -> Unit,
    libraryEntryVisibilityClick: (Int) -> Unit,
    windowSizeClass: WindowSizeClass,
    legacySideNav: Boolean,
    onBackPress: () -> Unit,
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

    var currentBottomSheet: BrowseBottomSheetScreen? by remember { mutableStateOf(null) }

    var mainDropdownShowing by remember { mutableStateOf(false) }

    val browseScreenType = browseScreenState.value.screenType

    // var sortType by remember { mutableStateOf(Sort.Entries) }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    // val sideNav = rememberSideBarVisible(windowSizeClass, browseScreenState.value.sideNavMode)
    val actualSideNav = legacySideNav
    val navBarPadding = rememberNavBarPadding(actualSideNav, browseScreenState.value.isDeepLink)

    // set the current sheet to null when bottom sheet is closed
    LaunchedEffect(key1 = sheetState.isVisible) {
        if (!sheetState.isVisible) {
            currentBottomSheet = null
        }
    }

    val openSheet: (BrowseBottomSheetScreen) -> Unit = {
        scope.launch {
            currentBottomSheet = it
            sheetState.show()
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize().conditional(mainDropdownShowing) {
                this.blur(16.dp).clickable(enabled = false) {}
            }
    ) {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetShape = RoundedCornerShape(Shapes.sheetRadius),
            sheetContent = {
                Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                    currentBottomSheet?.let { currentSheet ->
                        BrowseBottomSheet(
                            currentScreen = currentSheet,
                            browseScreenState = browseScreenState,
                            addNewCategory = addNewCategory,
                            contentPadding = navBarPadding,
                            closeSheet = { scope.launch { sheetState.hide() } },
                            filterActions = filterActions,
                        )
                    }
                }
            },
        ) {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                onNavigationIconClicked = onBackPress,
                title = browseScreenState.value.title.asString(),
                incognitoMode = browseScreenState.value.incognitoMode,
                isRoot = true,
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
                                                BrowseBottomSheetScreen.BrowseDisplayOptionsSheet(
                                                    showIsList =
                                                        browseScreenType !=
                                                            BrowseScreenType.Homepage &&
                                                            browseScreenType !=
                                                                BrowseScreenType.Other,
                                                    switchDisplayClick = switchDisplayClick,
                                                    libraryEntryVisibilityClick =
                                                        libraryEntryVisibilityClick,
                                                )
                                            )
                                        }
                                    },
                                )
                            ) +
                                if (browseScreenState.value.isDeepLink) {
                                    emptyList()
                                } else {
                                    listOf(
                                        AppBar.MainDropdown(
                                            incognitoMode = browseScreenState.value.incognitoMode,
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
                                }
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

                    val haptic = LocalHapticFeedback.current
                    fun mangaLongClick(displayManga: DisplayManga) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (
                            !displayManga.inLibrary && browseScreenState.value.promptForCategories
                        ) {
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

                    Box(
                        modifier =
                            Modifier.padding(bottom = navBarPadding.calculateBottomPadding())
                                .fillMaxSize()
                    ) {
                        if (browseScreenState.value.initialLoading) {
                            ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                        } else if (browseScreenState.value.error != null) {
                            EmptyScreen(
                                message = browseScreenState.value.error!!,
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
                                            browseHomePageManga =
                                                browseScreenState.value.homePageManga,
                                            shouldOutlineCover =
                                                browseScreenState.value.outlineCovers,
                                            titleClick = homeScreenTitleClick,
                                            randomClick = randomClick,
                                            onClick = { id -> openManga(id) },
                                            onLongClick = ::mangaLongClick,
                                            contentPadding = recyclerContentPadding,
                                        )

                                    BrowseScreenType.Follows -> {
                                        BrowseFollowsPage(
                                            displayMangaHolder =
                                                browseScreenState.value.displayMangaHolder,
                                            isList = browseScreenState.value.isList,
                                            isComfortableGrid =
                                                browseScreenState.value.isComfortableGrid,
                                            outlineCovers = browseScreenState.value.outlineCovers,
                                            rawColumnCount = browseScreenState.value.rawColumnCount,
                                            contentPadding = recyclerContentPadding,
                                            onClick = openManga,
                                            onLongClick = ::mangaLongClick,
                                        )
                                    }

                                    BrowseScreenType.Other -> {
                                        BrowseOtherPage(
                                            results = browseScreenState.value.otherResults,
                                            contentPadding = recyclerContentPadding,
                                            onClick = otherClick,
                                        )
                                    }

                                    BrowseScreenType.Filter -> {
                                        BrowseFilterPage(
                                            displayMangaHolder =
                                                browseScreenState.value.displayMangaHolder,
                                            isList = browseScreenState.value.isList,
                                            isComfortableGrid =
                                                browseScreenState.value.isComfortableGrid,
                                            outlineCovers = browseScreenState.value.outlineCovers,
                                            rawColumnCount = browseScreenState.value.rawColumnCount,
                                            pageLoading = browseScreenState.value.pageLoading,
                                            lastPage = browseScreenState.value.endReached,
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
                        if (!browseScreenState.value.hideFooterButton) {
                            val items =
                                remember(browseScreenState.value.isLoggedIn) {
                                    listOf(BrowseScreenType.Homepage) +
                                        if (browseScreenState.value.isLoggedIn) {
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
                                    Modifier.align(Alignment.BottomStart)
                                        .padding(horizontal = Size.tiny),
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
                                        BrowseScreenType.Homepage ->
                                            stringResource(id = R.string.home_page)
                                        BrowseScreenType.Follows -> stringResource(R.string.follows)
                                        else -> stringResource(R.string.search)
                                    }
                                Text(
                                    text = name,
                                    style =
                                        MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                )
                            }
                        }
                    }
                },
            )
        }
        // this is needed for Android SDK where blur isn't available
        if (mainDropdownShowing && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = NekoColors.mediumAlphaLowContrast))
            ) {}
        }
    }
}
