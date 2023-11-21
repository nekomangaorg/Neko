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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.recents.FeedScreenState
import eu.kanade.tachiyomi.ui.recents.FeedScreenType
import eu.kanade.tachiyomi.ui.recents.FeedSettingActions
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.launch
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.FooterFilterChip
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.rememberNavBarPadding
import org.nekomanga.presentation.components.rememberSideBarVisible
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.feed.FeedBottomSheet
import org.nekomanga.presentation.screens.feed.FeedPage
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedScreen(
    feedScreenState: State<FeedScreenState>,
    windowSizeClass: WindowSizeClass,
    loadNextPage: () -> Unit,
    feedSettingActions: FeedSettingActions,
    viewTypeClick: (FeedScreenType) -> Unit,
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
                    FeedBottomSheet(
                        feedScreenState = feedScreenState,
                        contentPadding = navBarPadding,
                        closeSheet = { scope.launch { sheetState.hide() } },
                        feedActions = feedSettingActions,
                    )
                }
            },
        ) {
            NekoScaffold(
                type = NekoScaffoldType.Search,
                incognitoMode = feedScreenState.value.incognitoMode,
                isRoot = true,
                title = "",
                onNavigationIconClicked = onBackPress,
                actions = {
                    AppBarActions(
                        actions =
                        if (feedScreenType == FeedScreenType.History) {
                            listOf(
                                AppBar.Action(
                                    title = UiText.StringResource(R.string.settings),
                                    icon = Icons.Outlined.Tune,
                                    onClick = { scope.launch { sheetState.show() } },
                                ),
                            )
                        } else {
                            listOf()
                        }
                            +
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
                            Size.navBarSize
                        } else {
                            Size.navBarSize
                        } + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    )

                Box(
                    modifier = Modifier
                        .padding(bottom = navBarPadding.calculateBottomPadding())
                        .fillMaxSize(),
                ) {

                    FeedPage(
                        contentPadding = recyclerContentPadding,
                        feedMangaList = feedScreenState.value.allFeedManga,
                        hasMoreResults = feedScreenState.value.hasMoreResults,
                        feedScreenType = feedScreenState.value.feedScreenType,
                        mangaClick = mangaClick,
                        outlineCovers = feedScreenState.value.outlineCovers,
                        hideChapterTitles = feedScreenState.value.hideChapterTitles,
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
                            if (feedScreenType != newScreenType) {
                                viewTypeClick(newScreenType)
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
