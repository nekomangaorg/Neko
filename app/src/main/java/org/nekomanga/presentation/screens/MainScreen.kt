package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import eu.kanade.tachiyomi.ui.source.browse.BrowseViewModel
import org.nekomanga.presentation.components.AppBar

@Composable
fun MainScreen(
    contentPadding: PaddingValues,
    backStack: NavBackStack<NavKey>,
    windowSizeClass: WindowSizeClass,
    incognitoMode: Boolean,
    incognitoClick: () -> Unit,
    onMenuShowing: (Boolean) -> Unit,
) {

    val mainDropDown =
        AppBar.MainDropdown(
            incognitoMode = incognitoMode,
            incognitoModeClick = incognitoClick,
            settingsClick = { backStack.add(Screens.Settings.Main) },
            statsClick = { backStack.add(Screens.Stats) },
            aboutClick = { backStack.add(Screens.About) },
            helpClick = {},
            menuShowing = onMenuShowing,
        )

    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<Screens.Library> { screen ->
                        val libraryViewModel: LibraryViewModel = viewModel()
                        if (screen.initialSearch.isNotEmpty()) {
                            libraryViewModel.deepLinkSearch(screen.initialSearch)
                        }
                        LibraryScreen(
                            libraryViewModel = libraryViewModel,
                            mainDropDown = mainDropDown,
                            openManga = { mangaId -> backStack.add(Screens.Manga(mangaId)) },
                            searchMangaDex = { title ->
                                backStack.clear()
                                backStack.add(Screens.Browse(title))
                            },
                            windowSizeClass = windowSizeClass,
                        )
                    }
                    entry<Screens.Feed> {
                        val feedViewModel: FeedViewModel = viewModel()

                        FeedScreen(
                            feedViewModel = feedViewModel,
                            mainDropDown = mainDropDown,
                            openManga = { mangaId -> backStack.add(Screens.Manga(mangaId)) },
                            windowSizeClass = windowSizeClass,
                        )
                    }
                    entry<Screens.Browse> { screen ->
                        val browseViewModel: BrowseViewModel = viewModel()
                        if (screen.initialSearch.isNotEmpty()) {
                            browseViewModel.deepLinkQuery(screen.initialSearch)
                        }
                        BrowseScreen(
                            browseViewModel = browseViewModel,
                            mainDropDown = mainDropDown,
                            openManga = { mangaId -> backStack.add(Screens.Manga(mangaId)) },
                            windowSizeClass = windowSizeClass,
                        )
                    }

                    entry<Screens.Manga> { screen ->
                        val mangaViewModel: MangaViewModel =
                            viewModel(factory = MangaViewModel.Factory(screen.mangaId))

                        MangaScreen(
                            mangaViewModel = mangaViewModel,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = { backStack.removeLastOrNull() },
                            onNavigate = { screen -> backStack.add(screen) },
                        )
                    }
                    entry<Screens.Settings.Main> {
                        SettingsScreen(
                            windowSizeClass = windowSizeClass,
                            onBackPressed = { backStack.removeLastOrNull() },
                            deepLink = null,
                        )
                    }
                    entry<Screens.Stats> {
                        val statsViewModel: StatsViewModel = viewModel()

                        StatsScreen(
                            statsViewModel = statsViewModel,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screens.About> {
                        val aboutView: AboutViewModel = viewModel()
                        AboutScreen(
                            aboutViewModel = aboutView,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = { backStack.removeLastOrNull() },
                            onNavigateTo = { backStack.add(Screens.License) },
                        )
                    }

                    entry<Screens.License> {
                        LicenseScreen(onBackPressed = { backStack.removeLastOrNull() })
                    }
                },
        )
    }
}
