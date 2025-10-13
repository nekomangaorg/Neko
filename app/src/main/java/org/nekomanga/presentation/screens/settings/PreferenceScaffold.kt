package org.nekomanga.presentation.screens.settings

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType

@Composable
internal fun PreferenceScaffold(
    title: String,
    incognitoMode: Boolean,
    onNavigationIconClicked: () -> Unit,
    itemsProvider: @Composable () -> PersistentList<Preference>,
) {
    NekoScaffold(
        type = NekoScaffoldType.Title,
        onNavigationIconClicked = onNavigationIconClicked,
        incognitoMode = incognitoMode,
        title = title,
        content = { contentPadding ->
            PreferenceScreen(contentPadding = contentPadding, items = itemsProvider())
        },
    )
}
