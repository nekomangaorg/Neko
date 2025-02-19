package org.nekomanga.presentation.screens.settings.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.PreferenceScaffold

internal interface SearchableSetting {

    @StringRes fun getTitleRes(): Int

    fun onNavigationIconClick()

    @Composable fun getPreferences(): ImmutableList<Preference>

    @Composable
    fun Content() {
        PreferenceScaffold(
            title = stringResource(getTitleRes()),
            onNavigationIconClicked = { onNavigationIconClick() },
            itemsProvider = { getPreferences() },
        )
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See BasePreferenceWidget.highlightBackground
        var highlightKey: String? = null
    }
}
