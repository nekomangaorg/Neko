package org.nekomanga.presentation.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import eu.kanade.tachiyomi.ui.setting.DataStorageSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.LibrarySettingsViewModel
import eu.kanade.tachiyomi.ui.setting.MangaDexSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.MergeSettingsViewModel
import eu.kanade.tachiyomi.ui.setting.SettingsViewModel
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.screens.settings.SettingsMainScreen
import org.nekomanga.presentation.screens.settings.editCategoryscreens.AddEditCategoriesScreen
import org.nekomanga.presentation.screens.settings.screens.AppearanceSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.DataStorageSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.GeneralSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.LibrarySettingsScreen
import org.nekomanga.presentation.screens.settings.screens.MangaDexSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.MergeSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsSearchScreen

@Composable
fun SettingsScreen(windowSizeClass: WindowSizeClass, onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val sdkMinimumO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val backStack = rememberNavBackStack(Screens.Settings.Main)

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
                            )
                        },
                    )
                }
                entry<Screens.Settings.Search> {
                    SettingsSearchScreen(
                        onNavigationIconClicked = { reset(backStack) },
                        navigate = { route ->
                            backStack.clear()
                            backStack.addAll(listOf(Screens.Settings.Main, route))
                        },
                    )
                }
                entry<Screens.Settings.General> {
                    val vm: SettingsViewModel = viewModel()

                    GeneralSettingsScreen(
                            onNavigationIconClick = { reset(backStack) },
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
                            onNavigationIconClick = { reset(backStack) },
                            preferences = vm.preferences,
                            mangaDetailsPreferences = vm.mangaDetailsPreferences,
                        )
                        .Content()
                }
                entry<Screens.Settings.Library> {
                    val vm: LibrarySettingsViewModel = viewModel()
                    LibrarySettingsScreen(
                            onNavigationIconClick = { reset(backStack) },
                            libraryPreferences = vm.libraryPreferences,
                            setLibrarySearchSuggestion = vm::setLibrarySearchSuggestion,
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
                            onNavigationIconClick = { reset(backStack) },
                            storagePreferences = vm.storagePreferences,
                            cacheData = vm.cacheData.collectAsState().value,
                        )
                        .Content()
                }
                entry<Screens.Settings.MangaDex> {
                    val vm: MangaDexSettingsViewModel = viewModel()
                    MangaDexSettingsScreen(
                            onNavigationIconClick = { reset(backStack) },
                            mangaDexPreferences = vm.mangaDexPreference,
                            mangaDexSettingsState = vm.state.collectAsState().value,
                            logout = vm::logout,
                        )
                        .Content()
                }
                entry<Screens.Settings.MergeSource> {
                    val vm: MergeSettingsViewModel = viewModel()
                    MergeSettingsScreen(
                            login = vm::login,
                            logout = vm::logout,
                            onNavigationIconClick = { reset(backStack) },
                            loginEvent = vm.loginEvent,
                            komgaState = vm.komgaMergeScreenState.collectAsState().value,
                            suwayomiState = vm.suwayomiMergeScreenState.collectAsState().value,
                        )
                        .Content()
                }
            },
    )
}

private fun reset(backstack: NavBackStack) {
    backstack.clear()
    backstack.add(Screens.Settings.Main)
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
