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
import org.nekomanga.presentation.screens.settings.SettingsAppearanceScreen
import org.nekomanga.presentation.screens.settings.SettingsDataStorageScreen
import org.nekomanga.presentation.screens.settings.SettingsGeneralScreen
import org.nekomanga.presentation.screens.settings.SettingsLibraryScreen
import org.nekomanga.presentation.screens.settings.SettingsMainScreen

@Composable
fun SettingsScreen(windowSizeClass: WindowSizeClass, onBackPressed: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SettingsMainRoute) {
        composable<SettingsMainRoute> {
            NekoScaffold(
                type = NekoScaffoldType.SearchOutline,
                searchNavigationEnabled = true,
                onNavigationIconClicked = {},
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
                    onAdvancedClick = { navController.navigate(SettingsAppearanceRoute) },
                )
            }
        }
        composable<SettingsGeneralRoute> {
            NekoScaffold(
                type = NekoScaffoldType.NoTitle,
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsGeneralScreen(contentPadding = contentPadding)
            }
        }

        composable<SettingsAppearanceRoute> {
            NekoScaffold(
                type = NekoScaffoldType.NoTitle,
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsAppearanceScreen(contentPadding = contentPadding)
            }
        }

        composable<SettingsLibraryRoute> {
            NekoScaffold(
                type = NekoScaffoldType.NoTitle,
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsLibraryScreen(contentPadding = contentPadding)
            }
        }

        composable<SettingsDataStorageRoute> {
            NekoScaffold(
                type = NekoScaffoldType.NoTitle,
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsDataStorageScreen(contentPadding = contentPadding)
            }
        }
    }
}
