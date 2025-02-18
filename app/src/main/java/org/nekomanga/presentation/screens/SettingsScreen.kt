package org.nekomanga.presentation.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.PreferenceScreen
import org.nekomanga.presentation.screens.settings.SettingsAdvancedRoute
import org.nekomanga.presentation.screens.settings.SettingsAppearanceRoute
import org.nekomanga.presentation.screens.settings.SettingsDataStorageRoute
import org.nekomanga.presentation.screens.settings.SettingsDownloadsRoute
import org.nekomanga.presentation.screens.settings.SettingsGeneralRoute
import org.nekomanga.presentation.screens.settings.SettingsLibraryRoute
import org.nekomanga.presentation.screens.settings.SettingsMainRoute
import org.nekomanga.presentation.screens.settings.SettingsMainScreen
import org.nekomanga.presentation.screens.settings.SettingsMangaDexRoute
import org.nekomanga.presentation.screens.settings.SettingsMergeSourceRoute
import org.nekomanga.presentation.screens.settings.SettingsReaderRoute
import org.nekomanga.presentation.screens.settings.SettingsSecurityRoute
import org.nekomanga.presentation.screens.settings.SettingsTrackingRoute
import org.nekomanga.presentation.screens.settings.screens.SettingsAppearanceScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsDataStorageScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsDownloadsScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsLibraryScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsMangaDexScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsMergeSourceScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsReaderScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsSecurityScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsTrackingScreen
import org.nekomanga.presentation.screens.settings.screens.generalSettingItems

@Composable
fun SettingsScreen(
    preferencesHelper: PreferencesHelper,
    windowSizeClass: WindowSizeClass,
    onBackPressed: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sdkMinimumO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    NavHost(navController = navController, startDestination = SettingsMainRoute) {
        composable<SettingsMainRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.settings),
                onNavigationIconClicked = onBackPressed,
            ) { contentPadding ->
                SettingsMainScreen(
                    contentPadding = contentPadding,
                    onGeneralClick = { navController.navigate(SettingsGeneralRoute) },
                    onAppearanceClick = { navController.navigate(SettingsAppearanceRoute) },
                    onLibraryClick = { navController.navigate(SettingsLibraryRoute) },
                    onDataStorageClick = { navController.navigate(SettingsDataStorageRoute) },
                    onSiteSpecificClick = { navController.navigate(SettingsMangaDexRoute) },
                    onMergeSourceClick = { navController.navigate(SettingsMergeSourceRoute) },
                    onReaderClick = { navController.navigate(SettingsReaderRoute) },
                    onDownloadsClick = { navController.navigate(SettingsDownloadsRoute) },
                    onTrackingClick = { navController.navigate(SettingsTrackingRoute) },
                    onSecurityClick = { navController.navigate(SettingsSecurityRoute) },
                    onAdvancedClick = { navController.navigate(SettingsAdvancedRoute) },
                )
            }
        }
        composable<SettingsGeneralRoute> {
            PreferenceScaffold(
                title = stringResource(R.string.general),
                onNavigationIconClicked = { navController.popBackStack() },
                preferenceList =
                    generalSettingItems(
                        preferencesHelper,
                        showNotificationSetting = sdkMinimumO,
                        manageNotificationsClicked = {
                            manageNotificationClick(context, sdkMinimumO)
                        },
                    ),
            )
        }

        composable<SettingsAppearanceRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.appearance),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsAppearanceScreen(contentPadding = contentPadding)
            }
        }

        composable<SettingsLibraryRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.library),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsLibraryScreen(contentPadding = contentPadding)
            }
        }

        composable<SettingsDataStorageRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.data_storage),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsDataStorageScreen(contentPadding = contentPadding)
            }
        }

        composable<SettingsMangaDexRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.site_specific_settings),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsMangaDexScreen(contentPadding = contentPadding)
            }
        }

        composable<SettingsMergeSourceRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.merge_source_settings),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsMergeSourceScreen(contentPadding = contentPadding)
            }
        }
        composable<SettingsReaderRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.reader_settings),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsReaderScreen(contentPadding = contentPadding)
            }
        }
        composable<SettingsDownloadsRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.downloads),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsDownloadsScreen(contentPadding = contentPadding)
            }
        }
        composable<SettingsTrackingRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.tracking),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsTrackingScreen(contentPadding = contentPadding)
            }
        }
        composable<SettingsSecurityRoute> {
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.security),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsSecurityScreen(contentPadding = contentPadding)
            }
        }
        composable<SettingsAdvancedRoute> {
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
