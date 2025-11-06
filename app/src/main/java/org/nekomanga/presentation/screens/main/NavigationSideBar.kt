package org.nekomanga.presentation.screens.main

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.NavigationItem

@Composable
fun NavigationSideBar(
    items: List<NavigationItem>,
    libraryUpdating: Boolean,
    downloaderRunning: Boolean,
    selectedItemIndex: Int,
    onNavigate: (NavKey) -> Unit,
) {

    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        content = {
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
            }
        },
    )
}
