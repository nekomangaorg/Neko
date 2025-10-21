package eu.kanade.tachiyomi.ui.main

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

data class NavigationItem(
    val screen: NavKey,
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
)
