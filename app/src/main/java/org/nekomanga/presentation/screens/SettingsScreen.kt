package org.nekomanga.presentation.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.setting.SettingsDataStorageViewModel
import eu.kanade.tachiyomi.ui.setting.SettingsLibraryViewModel
import eu.kanade.tachiyomi.ui.setting.SettingsMangaDexViewModel
import org.nekomanga.R
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.settings.SettingsMainScreen
import org.nekomanga.presentation.screens.settings.screens.AddEditCategoriesScreen
import org.nekomanga.presentation.screens.settings.screens.AppearanceSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.DataStorageSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.GeneralSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.LibrarySettingsScreen
import org.nekomanga.presentation.screens.settings.screens.MangaDexSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsSearchScreen

@Composable
fun SettingsScreen(
    preferencesHelper: PreferencesHelper,
    mangaDetailsPreferences: MangaDetailsPreferences,
    storagePreferences: StoragePreferences,
    windowSizeClass: WindowSizeClass,
    onBackPressed: () -> Unit,
) {
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
                        type = NekoScaffoldType.Title,
                        title = stringResource(R.string.settings),
                        onNavigationIconClicked = onBackPressed,
                        actions = {
                            AppBarActions(
                                actions =
                                    listOf(
                                        AppBar.Action(
                                            title = UiText.StringResource(R.string.search_settings),
                                            icon = Icons.Outlined.Search,
                                            onClick = { backStack.add(Screens.Settings.Search) },
                                        )
                                    )
                            )
                        },
                    ) { contentPadding ->
                        SettingsMainScreen(
                            contentPadding = contentPadding,
                            onGeneralClick = { backStack.add(Screens.Settings.General) },
                            onAppearanceClick = { backStack.add(Screens.Settings.Appearance) },
                            onLibraryClick = { backStack.add(Screens.Settings.Library) },
                            onDataStorageClick = { backStack.add(Screens.Settings.DataStorage) },
                            onSiteSpecificClick = { backStack.add(Screens.Settings.MangaDex) },
                            onMergeSourceClick = { backStack.add(Screens.Settings.MergeSource) },
                            onReaderClick = { backStack.add(Screens.Settings.Reader) },
                            onDownloadsClick = { backStack.add(Screens.Settings.Downloads) },
                            onTrackingClick = { backStack.add(Screens.Settings.Tracking) },
                            onSecurityClick = { backStack.add(Screens.Settings.Security) },
                            onAdvancedClick = { backStack.add(Screens.Settings.Advanced) },
                        )
                    }
                }
                entry<Screens.Settings.Search> {
                    SettingsSearchScreen(
                        onNavigationIconClicked = { backStack.removeLastOrNull() },
                        navigate = { route ->
                            backStack.clear()
                            backStack.addAll(listOf(Screens.Settings.Main, route))
                        },
                    )
                }
                entry<Screens.Settings.General> {
                    GeneralSettingsScreen(
                            onNavigationIconClick = { backStack.removeLastOrNull() },
                            preferencesHelper = preferencesHelper,
                            showNotificationSetting = sdkMinimumO,
                            manageNotificationsClicked = {
                                manageNotificationClick(context, sdkMinimumO)
                            },
                        )
                        .Content()
                }
                entry<Screens.Settings.Appearance> {
                    AppearanceSettingsScreen(
                            onNavigationIconClick = { backStack.removeLastOrNull() },
                            preferences = preferencesHelper,
                            mangaDetailsPreferences = mangaDetailsPreferences,
                        )
                        .Content()
                }
                entry<Screens.Settings.Library> {
                    val vm: SettingsLibraryViewModel = viewModel()
                    LibrarySettingsScreen(
                            onNavigationIconClick = { backStack.removeLastOrNull() },
                            libraryPreferences = vm.libraryPreferences,
                            setLibrarySearchSuggestion = vm::setLibrarySearchSuggestion,
                            categories = vm.dbCategories.collectAsState().value,
                            viewModelScope = vm.viewModelScope,
                            onAddEditCategoryClick = { backStack.add(Screens.Settings.Categories) },
                        )
                        .Content()
                }
                entry<Screens.Settings.Categories> {
                    AddEditCategoriesScreen(
                            onNavigationIconClick = { backStack.removeLastOrNull() }
                        )
                        .Content()
                }
                entry<Screens.Settings.DataStorage> {
                    val vm: SettingsDataStorageViewModel = viewModel()

                    DataStorageSettingsScreen(
                            onNavigationIconClick = { backStack.removeLastOrNull() },
                            storagePreferences = storagePreferences,
                            cacheData = vm.cacheData.collectAsState().value,
                        )
                        .Content()
                }
                entry<Screens.Settings.MangaDex> {
                    val vm: SettingsMangaDexViewModel = viewModel()
                    MangaDexSettingsScreen(
                            onNavigationIconClick = { backStack.removeLastOrNull() },
                            mangaDexPreferences = vm.mangaDexPreference,
                            mangaDexSettingsState = vm.state.collectAsState().value,
                            logout = vm::logout,
                        )
                        .Content()
                }
            },
    )
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
