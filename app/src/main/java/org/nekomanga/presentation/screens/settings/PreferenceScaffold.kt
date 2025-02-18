package org.nekomanga.presentation.screens.settings

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType

@Composable
internal fun PreferenceScaffold(
    title: String,
    onNavigationIconClicked: () -> Unit,
    itemsProvider: @Composable () -> ImmutableList<Preference>,
) {
    NekoScaffold(
        type = NekoScaffoldType.Title,
        title = title,
        onNavigationIconClicked = onNavigationIconClicked,
    ) { contentPadding ->
        PreferenceScreen(contentPadding = contentPadding, items = itemsProvider())
    }
}
