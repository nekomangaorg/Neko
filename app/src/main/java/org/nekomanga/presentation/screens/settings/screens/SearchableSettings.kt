package org.nekomanga.presentation.screens.settings.screens

import androidx.annotation.StringRes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.PreferenceScreen
import org.nekomanga.presentation.screens.settings.SettingsTopBar

internal abstract class SearchableSettings(
    val onNavigationBackClick: () -> Unit,
    val incognitoMode: Boolean,
) {

    @StringRes abstract fun getTitleRes(): Int

    @Composable abstract fun getPreferences(): PersistentList<Preference>

    @Composable
    fun Content() {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        ChildScreenScaffold(
            scrollBehavior = scrollBehavior,
            topBar = {
                SettingsTopBar(
                    scrollBehavior = scrollBehavior,
                    incognitoMode = incognitoMode,
                    onNavigationIconClicked = onNavigationBackClick,
                    title = stringResource(getTitleRes()),
                )
            },
        ) { contentPadding ->
            PreferenceScreen(contentPadding = contentPadding, items = getPreferences())
        }
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See BasePreferenceWidget.highlightBackground
        var highlightKey: String? = null
    }
}
