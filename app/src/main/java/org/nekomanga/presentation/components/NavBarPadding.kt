package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.tachiyomi.util.system.SideNavMode
import org.nekomanga.presentation.theme.Size

@Composable
fun rememberSideBarVisible(windowSizeClass: WindowSizeClass, sideNavMode: SideNavMode): Boolean {
    return remember(windowSizeClass, sideNavMode) {
        when (sideNavMode) {
            SideNavMode.NEVER -> false
            SideNavMode.ALWAYS -> true
            SideNavMode.DEFAULT -> {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Expanded -> true
                    WindowWidthSizeClass.Medium -> {
                        windowSizeClass.heightSizeClass != WindowHeightSizeClass.Medium
                    }
                    else -> false
                }
            }
        }
    }
}

@Composable
fun rememberNavBarPadding(
    isSideBarShowing: Boolean,
    shouldIgnoreBottomNavPadding: Boolean = false,
): PaddingValues {
    val bottomNav =
        PaddingValues(
            bottom =
                when {
                    shouldIgnoreBottomNavPadding -> Size.none
                    else ->
                        Size.navBarSize +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                }
        )
    val sideNav =
        PaddingValues(
            start = Size.navBarSize,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        )
    return remember(
        isSideBarShowing,
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    ) {
        when (isSideBarShowing) {
            true -> sideNav
            false -> bottomNav
        }
    }
}
