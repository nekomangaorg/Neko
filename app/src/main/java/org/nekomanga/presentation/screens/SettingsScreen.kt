package org.nekomanga.presentation.screens

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
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
import org.nekomanga.presentation.screens.settings.screens.SettingsAdvancedScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsAppearanceScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsDataStorageScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsDownloadsScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsGeneralScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsLibraryScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsMangaDexScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsMergeSourceScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsReaderScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsSecurityScreen
import org.nekomanga.presentation.screens.settings.screens.SettingsTrackingScreen

@Composable
fun SettingsScreen(windowSizeClass: WindowSizeClass, onBackPressed: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SettingsMainRoute) {
        composable<SettingsMainRoute> {
            NekoScaffold(
                type = NekoScaffoldType.SearchOutline,
                searchNavigationEnabled = true,
                onNavigationIconClicked = onBackPressed,
                onSearch = {},
                searchPlaceHolder = stringResource(id = R.string.search_settings),
                actions = {},
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
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.general),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsGeneralScreen(contentPadding = contentPadding)
            }
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
            NekoScaffold(
                type = NekoScaffoldType.Title,
                title = stringResource(R.string.advanced),
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsAdvancedScreen(contentPadding = contentPadding)
            }
        }
    }
}
