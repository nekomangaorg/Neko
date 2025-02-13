package org.nekomanga.presentation.screens

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.screens.settings.SettingsGeneralScreen
import org.nekomanga.presentation.screens.settings.SettingsMainScreen

@Composable
fun SettingsScreen(windowSizeClass: WindowSizeClass, onBackPressed: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SettingsMainRoute) {
        composable<SettingsMainRoute> {
            SettingsMainScreen(onGeneralClick = { navController.navigate(SettingsGeneralRoute) })
        }
        composable<SettingsGeneralRoute> {
            NekoScaffold(
                type = NekoScaffoldType.NoTitle,
                onNavigationIconClicked = { navController.popBackStack() },
            ) { contentPadding ->
                SettingsGeneralScreen(contentPadding = contentPadding)
            }
        }
    }
}

@Composable private fun SettingsScaffold(modifier: Modifier = Modifier) {}
