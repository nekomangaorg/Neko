package org.nekomanga.presentation.screens.settings

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType

@Composable
internal fun PreferenceScaffold(
    title: String,
    onNavigationIconClicked: () -> Unit,
    itemsProvider: @Composable () -> PersistentList<Preference>,
) {
    NekoScaffold(
        type = NekoScaffoldType.Title,
        onNavigationIconClicked = onNavigationIconClicked,
        title = title,
        content = { contentPadding ->
            PreferenceScreen(contentPadding = contentPadding, items = itemsProvider())
        },
    )
}
