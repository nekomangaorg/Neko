package org.nekomanga.presentation.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.NavigationItem
import eu.kanade.tachiyomi.ui.main.states.SideNavAlignment
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.theme.Size

@Composable
fun NavigationSideBar(
    items: List<NavigationItem>,
    sideNavAlignment: SideNavAlignment,
    libraryUpdating: Boolean,
    downloaderRunning: Boolean,
    selectedItemIndex: Int,
    onNavigate: (NavKey) -> Unit,
) {

    val arrangement =
        remember(sideNavAlignment) {
            when (sideNavAlignment) {
                SideNavAlignment.Bottom -> Arrangement.Bottom
                SideNavAlignment.Center -> Arrangement.Center
                SideNavAlignment.Top -> Arrangement.Top
            }
        }

    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        content = {
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = arrangement) {
                items.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = selectedItemIndex == index,
                        onClick = { onNavigate(item.screen) },
                        icon = {
                            PulsingIcon(
                                isPulsing =
                                    ((index == 0 && libraryUpdating) ||
                                        (index == 1 && downloaderRunning)),
                                imageVector =
                                    if (selectedItemIndex == index) item.selectedIcon
                                    else item.unselectedIcon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(text = item.title) },
                    )
                    Gap(Size.tiny)
                }
            }
        },
    )
}
