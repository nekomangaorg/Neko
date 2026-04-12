package org.nekomanga.presentation.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
fun SettingsScreen(windowSizeClass: WindowSizeClass, onBackPressed: () -> Unit, deepLink: NavKey?) {
    val context = LocalContext.current

    val backStack = rememberNavBackStack(deepLink ?: Screens.Settings.Main())
    val wasDeepLink = remember(deepLink) { deepLink != null }

    val settingsVm: SettingsViewModel = viewModel()

    val animationSpec = tween<IntOffset>(durationMillis = 300)
    val fadeSpec = tween<Float>(durationMillis = 300)

    val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val detailBackStack = rememberNavBackStack(deepLink ?: Screens.Settings.General)

    remember(isTablet) {
        if (isTablet) {
            // If switching from Phone to Tablet
            if (backStack.size > 1) {
                val dropped = backStack.toList().drop(1)
                backStack.clear()
                backStack.add(Screens.Settings.Main())

                detailBackStack.clear()
                for (screen in dropped) {
                    detailBackStack.add(screen)
                }
            }
        } else {
            // If switching from Tablet to Phone
            if (detailBackStack.size > 0 && detailBackStack.last() !is Screens.Settings.General) {
                val items = detailBackStack.toList()
                backStack.clear()
                backStack.add(Screens.Settings.Main())
                for (screen in items) {
                    backStack.add(screen)
                }
            }
        }
        true
    }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<Screens.Settings.Main> {
            val selectedScreen =
                if (isTablet && detailBackStack.size > 0) detailBackStack.last() else null

            SettingsMainScreen(
                onNavigateClick = { screen ->
                    if (isTablet) {
                        detailBackStack.clear()
                        detailBackStack.add(screen)
                    } else {
                        backStack.add(screen)
                    }
                },
                incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                developerMode = settingsVm.preferences.developerMode().get(),
                onNavigationIconClick = onBackPressed,
                selectedScreen = selectedScreen,
            )
        }

        entry<Screens.Settings.General> {
            GeneralSettingsScreen(
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    preferencesHelper = settingsVm.preferences,
                    showNotificationSetting = true,
                    manageNotificationsClicked = { manageNotificationClick(context) },
                )
                .Content()
        }
        entry<Screens.Settings.Appearance> {
            AppearanceSettingsScreen(
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    preferences = settingsVm.preferences,
                    libraryPreferences = settingsVm.libraryPreferences,
                    mangaDetailsPreferences = settingsVm.mangaDetailsPreferences,
                )
                .Content()
        }
        entry<Screens.Settings.Library> {
            val vm: LibrarySettingsViewModel = viewModel()
            LibrarySettingsScreen(
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    libraryPreferences = vm.libraryPreferences,
                    categories = vm.allCategories.collectAsState().value,
                    viewModelScope = vm.viewModelScope,
                    onAddEditCategoryClick = {
                        if (isTablet) {
                            detailBackStack.add(Screens.Settings.Categories)
                        } else {
                            backStack.add(Screens.Settings.Categories)
                        }
                    },
                )
                .Content()
        }
        entry<Screens.Settings.Categories> {
            val vm: LibrarySettingsViewModel = viewModel()
            AddEditCategoriesScreen(
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
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
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
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
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
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
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    loginEvent = vm.loginEvent,
                    komgaState = vm.komgaMergeScreenState.collectAsState().value,
                    suwayomiState = vm.suwayomiMergeScreenState.collectAsState().value,
                    preferences = vm.preferences,
                )
                .Content()
        }
        entry<Screens.Settings.Reader> {
            val vm: ReaderSettingsViewModel = viewModel()
            ReaderSettingsScreen(
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    readerPreferences = vm.readerPreferences,
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                )
                .Content()
        }

        entry<Screens.Settings.Downloads> {
            val vm: DownloadSettingsViewModel = viewModel()

            DownloadSettingsScreen(
                    preferences = vm.preferences,
                    readerPreferences = vm.readerPreferences,
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    allCategories = vm.allCategories.collectAsState().value,
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                )
                .Content()
        }

        entry<Screens.Settings.Tracking> {
            val vm: TrackingSettingsViewModel = viewModel()

            TrackingSettingsScreen(
                    preferences = vm.preferences,
                    trackingScreenState = vm.state.collectAsState().value,
                    updateAutoAddTrack = vm::updateAutoAddTrack,
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    loginEvent = vm.loginEvent,
                    login = vm::login,
                    logout = vm::logout,
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                )
                .Content()
        }
        entry<Screens.Settings.Security> {
            SecuritySettingsScreen(
                    securityPreferences = settingsVm.securityPreferences,
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                )
                .Content()
        }
        entry<Screens.Settings.Advanced> {
            val vm: AdvancedSettingsViewModel = viewModel()

            AdvancedSettingsScreen(
                    preferences = vm.preferences,
                    readerPreferences = vm.readerPreferences,
                    networkPreferences = vm.networkPreference,
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    toastEvent = vm.toastEvent,
                    clearNetworkCookies = vm::clearNetworkCookies,
                    clearDatabase = vm::clearDatabase,
                    cleanupDownloads = vm::cleanupDownloads,
                    reindexDownloads = vm::reindexDownloads,
                    dedupeCategories = vm::dedupeCategories,
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                )
                .Content()
        }

        entry<Screens.Settings.Debug> {
            val vm: DebugSettingsViewModel = viewModel()

            DebugSettingsScreen(
                    toastEvent = vm.toastEvent,
                    incognitoMode = settingsVm.securityPreferences.incognitoMode().get(),
                    unfollowAllLibraryManga = vm::unfollowAllLibraryManga,
                    removeAllMangaWithStatusOnMangaDex = vm::removeAllMangaWithStatusOnMangaDex,
                    clearAllManga = vm::clearAllManga,
                    clearAllTrackers = vm::clearAllTrackers,
                    clearAllCategories = vm::clearAllCategories,
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
                )
                .Content()
        }
    }

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.weight(1f),
                onBack = {
                    reset(
                        backstack = backStack,
                        wasDeepLink = deepLink != null,
                        onBackPressed = onBackPressed,
                    )
                },
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = animationSpec,
                        initialOffsetX = { it / 4 },
                    ) + fadeIn(animationSpec = fadeSpec) togetherWith
                        fadeOut(animationSpec = fadeSpec)
                },
                popTransitionSpec = {
                    fadeIn(animationSpec = fadeSpec) togetherWith
                        slideOutHorizontally(
                            animationSpec = animationSpec,
                            targetOffsetX = { it / 4 },
                        ) + fadeOut(animationSpec = fadeSpec)
                },
                predictivePopTransitionSpec = {
                    fadeIn(animationSpec = fadeSpec) togetherWith
                        slideOutHorizontally(
                            animationSpec = animationSpec,
                            targetOffsetX = { it / 4 },
                        ) + fadeOut(animationSpec = fadeSpec)
                },
                entryProvider = entryProvider,
            )

            NavDisplay(
                backStack = detailBackStack,
                modifier = Modifier.weight(2f),
                onBack = {
                    if (detailBackStack.size > 1) {
                        detailBackStack.removeLastOrNull()
                    } else {
                        onBackPressed()
                    }
                },
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                transitionSpec = {
                    fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
                },
                popTransitionSpec = {
                    fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
                },
                predictivePopTransitionSpec = {
                    fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
                },
                entryProvider = entryProvider,
            )
        }
    } else {
        NavDisplay(
            backStack = backStack,
            onBack = {
                reset(
                    backstack = backStack,
                    wasDeepLink = deepLink != null,
                    onBackPressed = onBackPressed,
                )
            },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            transitionSpec = {
                slideInHorizontally(animationSpec = animationSpec, initialOffsetX = { it / 4 }) +
                    fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)
            },
            popTransitionSpec = {
                fadeIn(animationSpec = fadeSpec) togetherWith
                    slideOutHorizontally(
                        animationSpec = animationSpec,
                        targetOffsetX = { it / 4 },
                    ) + fadeOut(animationSpec = fadeSpec)
            },
            predictivePopTransitionSpec = {
                fadeIn(animationSpec = fadeSpec) togetherWith
                    slideOutHorizontally(
                        animationSpec = animationSpec,
                        targetOffsetX = { it / 4 },
                    ) + fadeOut(animationSpec = fadeSpec)
            },
            entryProvider = entryProvider,
        )
    }
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
        backstack.add(Screens.Settings.Main())
    }
}

@SuppressLint("InlinedApi")
fun manageNotificationClick(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    context.startActivity(intent)
}
