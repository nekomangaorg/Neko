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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.PreferenceScreen
import org.nekomanga.presentation.screens.settings.SettingsMainScreen
import org.nekomanga.presentation.screens.settings.screens.GeneralSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsAppearanceScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsDataStorageScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsDownloadsScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsLibraryScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsMangaDexScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsMergeSourceScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsReaderScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsSearchScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsSecurityScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsTrackingScreen

@Composable
fun SettingsScreen(
    preferencesHelper: PreferencesHelper,
    windowSizeClass: WindowSizeClass,
    onBackPressed: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sdkMinimumO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    NavHost(navController = navController, startDestination = Screens.Settings.Main) {
        composable<Screens.Settings.Main> {
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
                                    onClick = { navController.navigate(Screens.Settings.Search) },
                                )
                            )
                    )
                },
            ) { contentPadding ->
                SettingsMainScreen(
                    contentPadding = contentPadding,
                    onGeneralClick = { navController.navigate(Screens.Settings.General) },
                    onAppearanceClick = { navController.navigate(Screens.Settings.Appearance) },
                    onLibraryClick = { navController.navigate(Screens.Settings.Library) },
                    onDataStorageClick = { navController.navigate(Screens.Settings.DataStorage) },
                    onSiteSpecificClick = { navController.navigate(Screens.Settings.MangaDex) },
                    onMergeSourceClick = { navController.navigate(Screens.Settings.MergeSource) },
                    onReaderClick = { navController.navigate(Screens.Settings.Reader) },
                    onDownloadsClick = { navController.navigate(Screens.Settings.Downloads) },
                    onTrackingClick = { navController.navigate(Screens.Settings.Tracking) },
                    onSecurityClick = { navController.navigate(Screens.Settings.Security) },
                    onAdvancedClick = { navController.navigate(Screens.Settings.Advanced) },
                )
            }
        }

        composable<Screens.Settings.Search> {
            SettingsSearchScreen(
                onBackPressed = { navController.popBackStack() },
                navigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screens.Settings.Main) { inclusive = false }
                    }
                },
            )
        }

        composable<Screens.Settings.General> {
            GeneralSettingsScreen(
                    onNavigationIconClick = { navController.popBackStack() },
                    preferencesHelper = preferencesHelper,
                    showNotificationSetting = sdkMinimumO,
                    manageNotificationsClicked = { manageNotificationClick(context, sdkMinimumO) },
                )
                .Content()
        }

        composable<Screens.Settings.Appearance> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.appearance),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsAppearanceScreen(contentPadding = contentPadding)
            }
        }

        composable<Screens.Settings.Library> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.library),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsLibraryScreen(contentPadding = contentPadding)
            }
        }

        composable<Screens.Settings.DataStorage> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.data_storage),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsDataStorageScreen(contentPadding = contentPadding)
            }
        }

        composable<Screens.Settings.MangaDex> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.site_specific_settings),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsMangaDexScreen(contentPadding = contentPadding)
            }
        }

        composable<Screens.Settings.MergeSource> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.merge_source_settings),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsMergeSourceScreen(contentPadding = contentPadding)
            }
        }
        composable<Screens.Settings.Reader> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.reader_settings),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsReaderScreen(contentPadding = contentPadding)
            }
        }
        composable<Screens.Settings.Downloads> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.downloads),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsDownloadsScreen(contentPadding = contentPadding)
            }
        }
        composable<Screens.Settings.Tracking> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.tracking),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsTrackingScreen(contentPadding = contentPadding)
            }
        }
        composable<Screens.Settings.Security> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.security),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsSecurityScreen(contentPadding = contentPadding)
            }
        }
        composable<Screens.Settings.Advanced> {
            PreferenceScaffold(
                title = stringResource(R.string.advanced),
                onNavigationIconClicked = { navController.popBackStack() },
                preferenceList = persistentListOf(),
            )
        }
    }
}

@Composable
private fun PreferenceScaffold(
    title: String,
    preferenceList: ImmutableList<Preference>,
    onNavigationIconClicked: () -> Unit,
) {
    NekoScaffold(
        type = NekoScaffoldType.Title,
        title = title,
        onNavigationIconClicked = onNavigationIconClicked,
    ) { contentPadding ->
        PreferenceScreen(contentPadding = contentPadding, items = preferenceList)
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
