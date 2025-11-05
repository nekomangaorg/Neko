package org.nekomanga.presentation.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import eu.kanade.tachiyomi.ui.feed.FeedViewModel
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.main.SplashScreen
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.ui.more.about.AboutViewModel
import eu.kanade.tachiyomi.ui.more.stats.StatsViewModel
import eu.kanade.tachiyomi.ui.setting.AdvancedSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.DataStorageSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.DebugSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.DownloadSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.LibrarySettingsViewModel
import eu.kanade.tachiyomi.ui.setting.MangaDexSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.MergeSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.ReaderSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.SettingsViewModel
import eu.kanade.tachiyomi.ui.setting.TrackingSettingsViewModel
import eu.kanade.tachiyomi.ui.similar.SimilarViewModel
import eu.kanade.tachiyomi.ui.source.browse.BrowseViewModel
import eu.kanade.tachiyomi.ui.source.latest.DisplayViewModel
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.screens.settings.SettingsMainScreen
import org.nekomanga.presentation.screens.settings.editCategoryscreens.AddEditCategoriesScreen
import org.nekomanga.presentation.screens.settings.screens.AdvancedSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.AppearanceSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.DataStorageSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.DebugSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.DownloadSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.GeneralSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.LibrarySettingsScreen
import org.nekomanga.presentation.screens.settings.screens.MangaDexSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.MergeSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.ReaderSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.SecuritySettingsScreen
import org.nekomanga.presentation.screens.settings.screens.TrackingSettingsScreen

@Composable
fun MainScreen(
    contentPadding: PaddingValues,
    startingScreen: NavKey,
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
                    entry<Screens.Splash> {
                        SplashScreen {
                            backStack.clear()
                            backStack.add(startingScreen)
                        }
                    }

                    entry<Screens.Library> { screen ->
                        val libraryViewModel: LibraryViewModel = viewModel()
                        if (screen.initialSearch.isNotEmpty()) {
                            libraryViewModel.deepLinkSearch(screen.initialSearch)
                        }
                        LibraryScreen(
                            libraryViewModel = libraryViewModel,
                            mainDropDown = mainDropDown,
                            openManga = { mangaId -> backStack.add(Screens.Manga(mangaId)) },
                            onSearchMangaDex = { search ->
                                backStack.clear()
                                backStack.add(Screens.Browse(search))
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
                        if (screen.searchBrowse != null) {
                            browseViewModel.deepLinkQuery(screen.searchBrowse)
                        }
                        BrowseScreen(
                            browseViewModel = browseViewModel,
                            mainDropDown = mainDropDown,
                            onNavigateTo = { screen -> backStack.add(screen) },
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
                            onSearchLibrary = { tag ->
                                backStack.clear()
                                backStack.add(Screens.Library(initialSearch = tag))
                            },
                            onSearchMangaDex = { searchBrowse ->
                                backStack.clear()
                                backStack.add(Screens.Browse(searchBrowse = searchBrowse))
                            },
                        )
                    }

                    entry<Screens.Display> { screen ->
                        val displayViewModel: DisplayViewModel =
                            viewModel(factory = DisplayViewModel.Factory(screen.displayScreenType))

                        DisplayScreen(
                            viewModel = displayViewModel,
                            onBackPressed = { backStack.removeLastOrNull() },
                            onNavigateTo = { screen -> backStack.add(screen) },
                        )
                    }

                    entry<Screens.Similar> { screen ->
                        val similarViewModel: SimilarViewModel =
                            viewModel(factory = SimilarViewModel.Factory(screen.mangaUUID))

                        SimilarScreen(
                            viewModel = similarViewModel,
                            onBackPressed = { backStack.removeLastOrNull() },
                            onNavigateTo = { screen -> backStack.add(screen) },
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

                    entry<Screens.Settings.Main> {
                        val settingsVm: SettingsViewModel = viewModel()
                        SettingsMainScreen(
                            onNavigateTo = { screen -> backStack.add(screen) },
                            incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                            onBackPressed = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screens.Settings.General> {
                        val settingsVm: SettingsViewModel = viewModel()

                        val sdkMinimumO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

                        val context = LocalContext.current

                        GeneralSettingsScreen(
                                onBackPressed = { backStack.removeLastOrNull() },
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                preferencesHelper = settingsVm.preferences,
                                showNotificationSetting = sdkMinimumO,
                                manageNotificationsClicked = {
                                    if (sdkMinimumO) {
                                        val intent =
                                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                                .apply {
                                                    putExtra(
                                                        Settings.EXTRA_APP_PACKAGE,
                                                        context.packageName,
                                                    )
                                                }
                                        context.startActivity(intent)
                                    }
                                },
                            )
                            .Content()
                    }
                    entry<Screens.Settings.Appearance> {
                        val settingsVm: SettingsViewModel = viewModel()

                        AppearanceSettingsScreen(
                                onBackPressed = { backStack.removeLastOrNull() },
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                preferences = settingsVm.preferences,
                                mangaDetailsPreferences = settingsVm.mangaDetailsPreferences,
                            )
                            .Content()
                    }
                    entry<Screens.Settings.Library> {
                        val vm: LibrarySettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        LibrarySettingsScreen(
                                onBackPressed = { backStack.removeLastOrNull() },
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                libraryPreferences = vm.libraryPreferences,
                                categories = vm.allCategories.collectAsState().value,
                                viewModelScope = vm.viewModelScope,
                                onAddEditCategoryClick = {
                                    backStack.add(Screens.Settings.Categories)
                                },
                            )
                            .Content()
                    }
                    entry<Screens.Settings.Categories> {
                        val vm: LibrarySettingsViewModel = viewModel()
                        AddEditCategoriesScreen(
                                onBackPressed = { backStack.removeLastOrNull() },
                                categories = vm.allCategories.collectAsState().value,
                                addUpdateCategory = vm::addUpdateCategory,
                                deleteCategory = vm::deleteCategory,
                                onChangeOrder = vm::onChangeOrder,
                            )
                            .Content()
                    }
                    entry<Screens.Settings.DataStorage> {
                        val vm: DataStorageSettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        DataStorageSettingsScreen(
                                onBackPressed = { backStack.removeLastOrNull() },
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                storagePreferences = vm.storagePreferences,
                                clearCache = vm::clearParentCache,
                                toastEvent = vm.toastEvent,
                                cacheData = vm.cacheData.collectAsState().value,
                            )
                            .Content()
                    }
                    entry<Screens.Settings.MangaDex> {
                        val vm: MangaDexSettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        MangaDexSettingsScreen(
                                onBackPressed = { backStack.removeLastOrNull() },
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                mangaDexPreferences = vm.mangaDexPreference,
                                mangaDexSettingsState = vm.state.collectAsState().value,
                                deleteSavedFilters = vm::deleteAllBrowseFilters,
                                logout = vm::logout,
                            )
                            .Content()
                    }
                    entry<Screens.Settings.MergeSource> {
                        val vm: MergeSettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        MergeSettingsScreen(
                                login = vm::login,
                                logout = vm::logout,
                                onBackPressed = { backStack.removeLastOrNull() },
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                loginEvent = vm.loginEvent,
                                komgaState = vm.komgaMergeScreenState.collectAsState().value,
                                suwayomiState = vm.suwayomiMergeScreenState.collectAsState().value,
                            )
                            .Content()
                    }
                    entry<Screens.Settings.Reader> {
                        val vm: ReaderSettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        ReaderSettingsScreen(
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                readerPreferences = vm.readerPreferences,
                                onBackPressed = { backStack.removeLastOrNull() },
                            )
                            .Content()
                    }

                    entry<Screens.Settings.Downloads> {
                        val vm: DownloadSettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        DownloadSettingsScreen(
                                preferences = vm.preferences,
                                readerPreferences = vm.readerPreferences,
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                allCategories = vm.allCategories.collectAsState().value,
                                onBackPressed = { backStack.removeLastOrNull() },
                            )
                            .Content()
                    }

                    entry<Screens.Settings.Tracking> {
                        val vm: TrackingSettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        TrackingSettingsScreen(
                                preferences = vm.preferences,
                                trackingScreenState = vm.state.collectAsState().value,
                                updateAutoAddTrack = vm::updateAutoAddTrack,
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                loginEvent = vm.loginEvent,
                                login = vm::login,
                                logout = vm::logout,
                                onBackPressed = { backStack.removeLastOrNull() },
                            )
                            .Content()
                    }
                    entry<Screens.Settings.Security> {
                        val settingsVm: SettingsViewModel = viewModel()

                        SecuritySettingsScreen(
                                securityPreferences = settingsVm.securityPreferences,
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                onBackPressed = { backStack.removeLastOrNull() },
                            )
                            .Content()
                    }
                    entry<Screens.Settings.Advanced> {
                        val vm: AdvancedSettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        AdvancedSettingsScreen(
                                preferences = vm.preferences,
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                networkPreferences = vm.networkPreference,
                                toastEvent = vm.toastEvent,
                                clearNetworkCookies = vm::clearNetworkCookies,
                                clearDatabase = vm::clearDatabase,
                                cleanupDownloads = vm::cleanupDownloads,
                                reindexDownloads = vm::reindexDownloads,
                                dedupeCategories = vm::dedupeCategories,
                                onBackPressed = { backStack.removeLastOrNull() },
                            )
                            .Content()
                    }

                    entry<Screens.Settings.Debug> {
                        val vm: DebugSettingsViewModel = viewModel()
                        val settingsVm: SettingsViewModel = viewModel()

                        DebugSettingsScreen(
                                toastEvent = vm.toastEvent,
                                incognitoMode =
                                    settingsVm.securityPreferences.incognitoMode().get(),
                                unfollowAllLibraryManga = vm::unfollowAllLibraryManga,
                                removeAllMangaWithStatusOnMangaDex =
                                    vm::removeAllMangaWithStatusOnMangaDex,
                                clearAllManga = vm::clearAllManga,
                                clearAllTrackers = vm::clearAllTrackers,
                                clearAllCategories = vm::clearAllCategories,
                                onBackPressed = { backStack.removeLastOrNull() },
                            )
                            .Content()
                    }
                },
        )
    }
}
