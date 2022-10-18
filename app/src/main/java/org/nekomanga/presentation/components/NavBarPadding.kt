package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.util.system.SideNavMode
import org.nekomanga.presentation.theme.Padding

@Composable
fun rememberNavBarPadding(windowSizeClass: WindowSizeClass, sideNavMode: SideNavMode): PaddingValues {
    val bottomNav = PaddingValues(bottom = Padding.navBarSize + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    val sideNav = PaddingValues(start = Padding.navBarSize, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    return when (sideNavMode) {
        SideNavMode.NEVER -> bottomNav
        SideNavMode.ALWAYS -> sideNav
        SideNavMode.DEFAULT -> {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Expanded -> sideNav
                else -> bottomNav
            }
        }
    }
}
