package org.nekomanga.presentation.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
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
import org.nekomanga.presentation.screens.settings.screens.SettingsSearchScreen
import org.nekomanga.presentation.screens.settings.screens.TrackingSettingsScreen

@Composable
fun SettingsScreen(windowSizeClass: WindowSizeClass, onBackPressed: () -> Unit, deepLink: NavKey?) {
    val context = LocalContext.current
    val sdkMinimumO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val backStack = rememberNavBackStack(deepLink ?: Screens.Settings.Main)
    val wasDeepLink = remember(deepLink) { deepLink != null }

    NavDisplay(
        backStack = backStack,
        entryDecorators =
            listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Screens.Settings.Main> {
                    NekoScaffold(
                        type = NekoScaffoldType.SearchOutlineDummy,
                        onNavigationIconClicked = onBackPressed,
                        title = stringResource(R.string.settings),
                        searchPlaceHolder = stringResource(R.string.search_settings),
                        searchNavigationEnabled = true,
                        onSearchEnabled = { backStack.add(Screens.Settings.Search) },
                        content = { contentPadding ->
                            SettingsMainScreen(
                                contentPadding = contentPadding,
                                onGeneralClick = { backStack.add(Screens.Settings.General) },
                                onAppearanceClick = { backStack.add(Screens.Settings.Appearance) },
                                onLibraryClick = { backStack.add(Screens.Settings.Library) },
                                onDataStorageClick = {
                                    backStack.add(Screens.Settings.DataStorage)
                                },
                                onSiteSpecificClick = { backStack.add(Screens.Settings.MangaDex) },
                                onMergeSourceClick = {
                                    backStack.add(Screens.Settings.MergeSource)
                                },
                                onReaderClick = { backStack.add(Screens.Settings.Reader) },
                                onDownloadsClick = { backStack.add(Screens.Settings.Downloads) },
                                onTrackingClick = { backStack.add(Screens.Settings.Tracking) },
                                onSecurityClick = { backStack.add(Screens.Settings.Security) },
                                onAdvancedClick = { backStack.add(Screens.Settings.Advanced) },
                                onDebugClick = { backStack.add(Screens.Settings.Debug) },
                            )
                        },
                    )
                }
                entry<Screens.Settings.Search> {
                    SettingsSearchScreen(
                        onNavigationIconClicked = { reset(backStack, wasDeepLink, onBackPressed) },
                        navigate = { route ->
                            backStack.clear()
                            backStack.addAll(listOf(Screens.Settings.Main, route))
                        },
                    )
                }
                entry<Screens.Settings.General> {
                    val vm: SettingsViewModel = viewModel()

                    GeneralSettingsScreen(
                            onNavigationIconClick = {
                                reset(backStack, wasDeepLink, onBackPressed)
                            },
                            preferencesHelper = vm.preferences,
                            showNotificationSetting = sdkMinimumO,
                            manageNotificationsClicked = {
                                manageNotificationClick(context, sdkMinimumO)
                            },
                        )
                        .Content()
                }
                entry<Screens.Settings.Appearance> {
                    val vm: SettingsViewModel = viewModel()
                    AppearanceSettingsScreen(
                            onNavigationIconClick = {
                                reset(backStack, wasDeepLink, onBackPressed)
                            },
                            preferences = vm.preferences,
                            mangaDetailsPreferences = vm.mangaDetailsPreferences,
                        )
                        .Content()
                }
                entry<Screens.Settings.Library> {
                    val vm: LibrarySettingsViewModel = viewModel()
                    LibrarySettingsScreen(
                            onNavigationIconClick = {
                                reset(backStack, wasDeepLink, onBackPressed)
                            },
                            libraryPreferences = vm.libraryPreferences,
                            categories = vm.allCategories.collectAsState().value,
                            viewModelScope = vm.viewModelScope,
                            onAddEditCategoryClick = { backStack.add(Screens.Settings.Categories) },
                        )
                        .Content()
                }
                entry<Screens.Settings.Categories> {
                    val vm: LibrarySettingsViewModel = viewModel()
                    AddEditCategoriesScreen(
                            onNavigationIconClick = { backStack.removeLastOrNull() },
                            categories = vm.allCategories.collectAsState().value,
                            addUpdateCategory = vm::addUpdateCategory,
                            deleteCategory = vm::deleteCategory,
                            onChangeOrder = vm::onChangeOrder,
                        )
                        .Content()
                }
                entry<Screens.Settings.DataStorage> {
                    val vm: DataStorageSettingsViewModel = viewModel()

                    DataStorageSettingsScreen(
                            onNavigationIconClick = {
                                reset(backStack, wasDeepLink, onBackPressed)
                            },
                            storagePreferences = vm.storagePreferences,
                            clearCache = vm::clearParentCache,
                            toastEvent = vm.toastEvent,
                            cacheData = vm.cacheData.collectAsState().value,
                        )
                        .Content()
                }
                entry<Screens.Settings.MangaDex> {
                    val vm: MangaDexSettingsViewModel = viewModel()
                    MangaDexSettingsScreen(
                            onNavigationIconClick = {
                                reset(backStack, wasDeepLink, onBackPressed)
                            },
                            mangaDexPreferences = vm.mangaDexPreference,
                            mangaDexSettingsState = vm.state.collectAsState().value,
                            deleteSavedFilters = vm::deleteAllBrowseFilters,
                            logout = vm::logout,
                        )
                        .Content()
                }
                entry<Screens.Settings.MergeSource> {
                    val vm: MergeSettingsViewModel = viewModel()
                    MergeSettingsScreen(
                            login = vm::login,
                            logout = vm::logout,
                            onNavigationIconClick = {
                                reset(backStack, wasDeepLink, onBackPressed)
                            },
                            loginEvent = vm.loginEvent,
                            komgaState = vm.komgaMergeScreenState.collectAsState().value,
                            suwayomiState = vm.suwayomiMergeScreenState.collectAsState().value,
                        )
                        .Content()
                }
                entry<Screens.Settings.Reader> {
                    val vm: ReaderSettingsViewModel = viewModel()
                    ReaderSettingsScreen(
                            readerPreferences = vm.readerPreferences,
                            onNavigationIconClick = { reset(backStack, wasDeepLink, onBackPressed) },
                        )
                        .Content()
                }

                entry<Screens.Settings.Downloads> {
                    val vm: DownloadSettingsViewModel = viewModel()

                    DownloadSettingsScreen(
                            preferences = vm.preferences,
                            readerPreferences = vm.readerPreferences,
                            allCategories = vm.allCategories.collectAsState().value,
                            onNavigationIconClick = { reset(backStack, wasDeepLink, onBackPressed) },
                        )
                        .Content()
                }

                entry<Screens.Settings.Tracking> {
                    val vm: TrackingSettingsViewModel = viewModel()

                    TrackingSettingsScreen(
                            preferences = vm.preferences,
                            trackingScreenState = vm.state.collectAsState().value,
                            updateAutoAddTrack = vm::updateAutoAddTrack,
                            loginEvent = vm.loginEvent,
                            login = vm::login,
                            logout = vm::logout,
                            onNavigationIconClick = { reset(backStack, wasDeepLink, onBackPressed) },
                        )
                        .Content()
                }
                entry<Screens.Settings.Security> {
                    val vm: SettingsViewModel = viewModel()

                    SecuritySettingsScreen(
                            securityPreferences = vm.securityPreferences,
                            onNavigationIconClick = { reset(backStack, wasDeepLink, onBackPressed) },
                        )
                        .Content()
                }
                entry<Screens.Settings.Advanced> {
                    val vm: AdvancedSettingsViewModel = viewModel()

                    AdvancedSettingsScreen(
                            preferences = vm.preferences,
                            networkPreferences = vm.networkPreference,
                            toastEvent = vm.toastEvent,
                            clearNetworkCookies = vm::clearNetworkCookies,
                            clearDatabase = vm::clearDatabase,
                            cleanupDownloads = vm::cleanupDownloads,
                            reindexDownloads = vm::reindexDownloads,
                            onNavigationIconClick = { reset(backStack, wasDeepLink, onBackPressed) },
                        )
                        .Content()
                }

                entry<Screens.Settings.Debug> {
                    val vm: DebugSettingsViewModel = viewModel()

                    DebugSettingsScreen(
                            toastEvent = vm.toastEvent,
                            unfollowAllLibraryManga = vm::unfollowAllLibraryManga,
                            removeAllMangaWithStatusOnMangaDex =
                                vm::removeAllMangaWithStatusOnMangaDex,
                            clearAllManga = vm::clearAllManga,
                            clearAllTrackers = vm::clearAllTrackers,
                            clearAllCategories = vm::clearAllCategories,
                            onNavigationIconClick = { reset(backStack, wasDeepLink, onBackPressed) },
                        )
                        .Content()
                }
            },
    )
}

private fun reset(
    backstack: NavBackStack<NavKey>,
    wasDeepLink: Boolean,
    onBackPressed: () -> Unit,
) {
    if (wasDeepLink) {
        onBackPressed()
    } else {
        backstack.clear()
        backstack.add(Screens.Settings.Main)
    }
}

@SuppressLint("InlinedApi")
fun manageNotificationClick(context: Context, sdkMinimumO: Boolean) {
    if (sdkMinimumO) {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        context.startActivity(intent)
    }
}
