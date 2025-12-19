package org.nekomanga.presentation.screens

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import eu.kanade.tachiyomi.ui.feed.FeedViewModel
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.ui.more.about.AboutViewModel
import eu.kanade.tachiyomi.ui.more.stats.StatsViewModel
import eu.kanade.tachiyomi.ui.similar.SimilarViewModel
import eu.kanade.tachiyomi.ui.source.browse.BrowseViewModel
import eu.kanade.tachiyomi.ui.source.latest.DisplayViewModel
import eu.kanade.tachiyomi.ui.source.latest.toSerializable
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.screens.deepLink.DeepLinkScreen
import org.nekomanga.presentation.screens.deepLink.DeepLinkViewModel

@Composable
fun MainScreen(
    backStack: NavBackStack<NavKey>,
    windowSizeClass: WindowSizeClass,
    incognitoMode: Boolean,
    incognitoClick: () -> Unit,
    onboardingCompleted: () -> Unit,
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
) {

    var mainDropdownShowing by remember { mutableStateOf(false) }

    val mainDropDown =
        AppBar.MainDropdown(
            incognitoMode = incognitoMode,
            incognitoModeClick = incognitoClick,
            settingsClick = { backStack.add(Screens.Settings.Main()) },
            statsClick = { backStack.add(Screens.Stats) },
            aboutClick = { backStack.add(Screens.About) },
            menuShowing = { mainDropdownShowing = it },
        )

    val animationSpec = tween<IntOffset>(durationMillis = 300)
    val fadeSpec = tween<Float>(durationMillis = 300)

    val slideInTransition =
        slideInHorizontally(animationSpec = animationSpec, initialOffsetX = { it / 4 }) +
            fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)

    val slideOutTransition =
        fadeIn(animationSpec = fadeSpec) togetherWith
            slideOutHorizontally(animationSpec = animationSpec, targetOffsetX = { it / 4 }) +
                fadeOut(animationSpec = fadeSpec)

    // The new fade-only animation for top-level screens
    val fadeTransition =
        fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val goBack = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            onBackPressedDispatcher?.onBackPressed()
        }
        Unit
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            transitionSpec = {
                val initialIsTop = isTopLevel(initialState.key)
                val targetIsTop = isTopLevel(targetState.key)

                if (initialIsTop && targetIsTop) {
                    fadeTransition
                } else {
                    slideInTransition
                }
            },
            popTransitionSpec = {
                val initialIsTop = isTopLevel(initialState.key)
                val targetIsTop = isTopLevel(targetState.key)

                if (initialIsTop && targetIsTop) {
                    fadeTransition
                } else {
                    slideOutTransition
                }
            },
            predictivePopTransitionSpec = {
                val initialIsTop = isTopLevel(initialState.key)
                val targetIsTop = isTopLevel(targetState.key)

                if (initialIsTop && targetIsTop) {
                    fadeTransition
                } else {
                    slideOutTransition
                }
            },
            entryProvider =
                entryProvider {
                    entry<Screens.Loading> { LoadingScreen(it.showLoadingIndicator) }
                    entry<Screens.DeepLink> {
                        val deepLinkViewModel: DeepLinkViewModel = viewModel()
                        DeepLinkScreen(
                            onNavigate = { screens ->
                                backStack.clear()
                                backStack.add(screens.last())
                                backStack.addAll(0, screens.dropLast(1))
                            },
                            host = it.host,
                            path = it.path,
                            id = it.id,
                            deepLinkViewModel = deepLinkViewModel,
                        )
                    }
                    entry<Screens.Onboarding> {
                        OnboardingScreen(
                            finishedOnBoarding = {
                                backStack.clear()
                                backStack.add(Screens.Library())
                                onboardingCompleted()
                            }
                        )
                    }

                    entry<Screens.Library> { screen ->
                        val libraryViewModel: LibraryViewModel = viewModel()
                        remember(screen.initialSearch) {
                            if (screen.initialSearch.isNotEmpty()) {
                                libraryViewModel.deepLinkSearch(screen.initialSearch)
                            }
                            true
                        }
                        LibraryScreen(
                            libraryViewModel = libraryViewModel,
                            mainDropdown = mainDropDown,
                            mainDropdownShowing = mainDropdownShowing,
                            openManga = { mangaId -> backStack.add(Screens.Manga(mangaId)) },
                            onSearchMangaDex = { search ->
                                backStack.clear()
                                backStack.add(Screens.Browse(search))
                            },
                            windowSizeClass = windowSizeClass,
                            navigationRail = navigationRail,
                            bottomBar = bottomBar,
                        )
                    }
                    entry<Screens.Feed> {
                        val feedViewModel: FeedViewModel = viewModel()
                        FeedScreen(
                            feedViewModel = feedViewModel,
                            mainDropdown = mainDropDown,
                            mainDropdownShowing = mainDropdownShowing,
                            openManga = { mangaId -> backStack.add(Screens.Manga(mangaId)) },
                            windowSizeClass = windowSizeClass,
                            navigationRail = navigationRail,
                            bottomBar = bottomBar,
                        )
                    }
                    entry<Screens.Browse> { screen ->
                        val browseViewModel: BrowseViewModel = viewModel()
                        if (screen.title != null) {
                            browseViewModel.initSearch(screen.title)
                        }
                        BrowseScreen(
                            browseViewModel = browseViewModel,
                            mainDropdown = mainDropDown,
                            mainDropdownShowing = mainDropdownShowing,
                            onNavigateTo = { screen -> backStack.add(screen) },
                            windowSizeClass = windowSizeClass,
                            navigationRail = navigationRail,
                            bottomBar = bottomBar,
                        )
                    }

                    entry<Screens.Manga> { screen ->
                        val mangaViewModel: MangaViewModel =
                            viewModel(factory = MangaViewModel.Factory(screen.mangaId))

                        MangaScreen(
                            mangaViewModel = mangaViewModel,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                            onNavigate = { screen -> backStack.add(screen) },
                            onSearchLibrary = { tag ->
                                backStack.clear()
                                backStack.add(Screens.Library(initialSearch = tag))
                            },
                            onSearchMangaDex = { displayType ->
                                backStack.add(Screens.Display(displayType.toSerializable()))
                            },
                        )
                    }

                    entry<Screens.WebView> { screen ->
                        WebViewScreen(
                            title = screen.title,
                            url = screen.url,
                            onBackPressed = goBack,
                        )
                    }

                    entry<Screens.Display> { screen ->
                        val displayViewModel: DisplayViewModel =
                            viewModel(factory = DisplayViewModel.Factory(screen.displayScreenType))

                        DisplayScreen(
                            viewModel = displayViewModel,
                            onBackPressed = goBack,
                            onNavigateTo = { screen -> backStack.add(screen) },
                        )
                    }

                    entry<Screens.Similar> { screen ->
                        val similarViewModel: SimilarViewModel =
                            viewModel(factory = SimilarViewModel.Factory(screen.mangaUUID))

                        SimilarScreen(
                            viewModel = similarViewModel,
                            onBackPressed = goBack,
                            onNavigateTo = { screen -> backStack.add(screen) },
                        )
                    }

                    entry<Screens.Settings.Main> { screen ->
                        SettingsScreen(
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                            deepLink = screen.deepLink,
                        )
                    }
                    entry<Screens.Stats> {
                        val statsViewModel: StatsViewModel = viewModel()

                        StatsScreen(
                            statsViewModel = statsViewModel,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                        )
                    }

                    entry<Screens.About> {
                        val aboutView: AboutViewModel = viewModel()
                        AboutScreen(
                            aboutViewModel = aboutView,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                            onNavigateTo = { backStack.add(Screens.License) },
                        )
                    }

                    entry<Screens.License> { LicenseScreen(onBackPressed = goBack) }
                },
        )
    }
}

private fun isTopLevel(key: Any?): Boolean {
    if (key == null) return false
    val keyString = key.toString()
    return keyString.contains("Library") ||
        keyString.contains("Feed") ||
        keyString.contains("Browse")
}
