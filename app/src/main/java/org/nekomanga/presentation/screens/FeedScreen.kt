package org.nekomanga.presentation.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.recents.FeedScreenState
import eu.kanade.tachiyomi.ui.recents.FeedScreenType
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.launch
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.FooterFilterChip
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.rememberNavBarPadding
import org.nekomanga.presentation.components.rememberSideBarVisible
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.browse.BrowseBottomSheetScreen
import org.nekomanga.presentation.screens.feed.FeedUpdatePage
import org.nekomanga.presentation.theme.Padding
import org.nekomanga.presentation.theme.Shapes

@Composable
fun FeedScreen(
    feedScreenState: State<FeedScreenState>,
    windowSizeClass: WindowSizeClass,
    loadNextPage: () -> Unit,
    toggleGroupChaptersUpdates: () -> Unit,
    mangaClick: (Long) -> Unit,
    onBackPress: () -> Unit,
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

    var currentBottomSheet: BrowseBottomSheetScreen? by remember {
        mutableStateOf(null)
    }

    var mainDropdownShowing by remember {
        mutableStateOf(false)
    }

    val feedScreenType = feedScreenState.value.feedScreenType

    /**
     * Close the bottom sheet on back if its open
     */
    BackHandler(enabled = sheetState.isVisible) {
        scope.launch { sheetState.hide() }
    }

    val sideNav = rememberSideBarVisible(windowSizeClass, feedScreenState.value.sideNavMode)
    val navBarPadding = rememberNavBarPadding(sideNav)

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
        modifier = Modifier
            .fillMaxSize()
            .conditional(mainDropdownShowing) {
                this
                    .blur(16.dp)
                    .clickable(enabled = false) { }
            },
    ) {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetShape = RoundedCornerShape(Shapes.sheetRadius),
            sheetContent = {
                Box(modifier = Modifier.defaultMinSize(minHeight = 1.dp)) {
                    /* currentBottomSheet?.let { currentSheet ->
                         BrowseBottomSheet(
                             currentScreen = currentSheet,
                             browseScreenState = browseScreenState,
                             addNewCategory = addNewCategory,
                             contentPadding = navBarPadding,
                             closeSheet = { scope.launch { sheetState.hide() } },
                             filterActions = filterActions,
                         )
                     }*/
                }
            },
        ) {
            NekoScaffold(
                incognitoMode = feedScreenState.value.incognitoMode,
                isRoot = true,
                title = "",
                onNavigationIconClicked = onBackPress,
                actions = {
                    AppBarActions(
                        actions =

                        listOf(
                            AppBar.MainDropdown(
                                incognitoMode = feedScreenState.value.incognitoMode,
                                incognitoModeClick = incognitoClick,
                                settingsClick = settingsClick,
                                statsClick = statsClick,
                                aboutClick = aboutClick,
                                helpClick = helpClick,
                                menuShowing = { visible -> mainDropdownShowing = visible },
                            ),
                        ),
                    )
                },
            ) { incomingContentPadding ->

                val recyclerContentPadding =
                    PaddingValues(
                        top = incomingContentPadding.calculateTopPadding(),
                        bottom = if (sideNav) {
                            Padding.navBarSize
                        } else {
                            Padding.navBarSize
                        } + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    )

                Box(
                    modifier = Modifier
                        .padding(bottom = navBarPadding.calculateBottomPadding())
                        .fillMaxSize(),
                ) {

                    FeedUpdatePage(
                        contentPadding = recyclerContentPadding,
                        feedChapters = feedScreenState.value.allFeedChapters,
                        hasMoreResults = feedScreenState.value.hasMoreResults,
                        groupChaptersUpdates = feedScreenState.value.groupChaptersUpdates,
                        toggleGroupChaptersUpdates = toggleGroupChaptersUpdates,
                        mangaClick = mangaClick,
                        outlineCovers = feedScreenState.value.outlineCovers,
                        loadNextPage = loadNextPage,
                    )


                    ScreenTypeFooter(
                        screenType = feedScreenType,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .conditional(sideNav) {
                                this.navigationBarsPadding()
                            },
                        screenTypeClick = { newScreenType: FeedScreenType ->
                            scope.launch { sheetState.hide() }
                            val sameScreen = feedScreenType == newScreenType
                            if (!sameScreen) {
                                //changeScreenType(newScreenType)
                            }

                        },
                    )
                }
            }
        }
        // this is needed for Android SDK where blur isn't available
        if (mainDropdownShowing && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = NekoColors.mediumAlphaLowContrast)),
            )
        }
    }
}

@Composable
private fun ScreenTypeFooter(screenType: FeedScreenType, modifier: Modifier = Modifier, screenTypeClick: (FeedScreenType) -> Unit) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            Gap(8.dp)
        }

        item {
            FooterFilterChip(
                selected = screenType == FeedScreenType.History,
                onClick = { screenTypeClick(FeedScreenType.History) },
                name = stringResource(R.string.history),
            )
        }

        item {
            FooterFilterChip(
                selected = screenType == FeedScreenType.Updates,
                onClick = { screenTypeClick(FeedScreenType.Updates) },
                name = stringResource(R.string.updates),
            )
        }
    }
}

