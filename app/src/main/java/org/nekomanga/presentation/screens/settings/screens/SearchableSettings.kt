package org.nekomanga.presentation.screens.settings.screens

import androidx.annotation.StringRes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.main.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.ScreenBars
import kotlinx.collections.immutable.PersistentList
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
        val updateTopBar = LocalBarUpdater.current

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        val screenBars = remember {
            ScreenBars(
                topBar = {
                    SettingsTopBar(
                        scrollBehavior = scrollBehavior,
                        incognitoMode = incognitoMode,
                        onNavigationIconClicked = onNavigationBackClick,
                        title = stringResource(getTitleRes()),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        }

        DisposableEffect(Unit) {
            updateTopBar(screenBars)
            onDispose { updateTopBar(ScreenBars(id = screenBars.id, topBar = null)) }
        }

        PreferenceScreen(items = getPreferences())
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See BasePreferenceWidget.highlightBackground
        var highlightKey: String? = null
    }
}
