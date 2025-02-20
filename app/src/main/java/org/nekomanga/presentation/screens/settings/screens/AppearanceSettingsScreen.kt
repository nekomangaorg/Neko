package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.theme.ThemeFollowSystemSwitch
import org.nekomanga.presentation.components.theme.ThemeSelector
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.theme.Size

internal class AppearanceSettingsScreen(
    val preferences: PreferencesHelper,
    val onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {
    override fun getTitleRes(): Int = R.string.appearance

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {

        val nightMode by preferences.nightMode().collectAsState()

        return persistentListOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.app_theme),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.CustomPreference(
                            title = stringResource(R.string.light_theme),
                            content = {
                                ThemeSelector(
                                    modifier = Modifier.fillMaxWidth(),
                                    preferences = preferences,
                                    darkThemeSelector = false,
                                )
                            },
                        ),
                        Preference.PreferenceItem.CustomPreference(
                            title = stringResource(R.string.dark_theme),
                            content = {
                                ThemeSelector(
                                    modifier = Modifier.fillMaxWidth(),
                                    preferences = preferences,
                                    darkThemeSelector = true,
                                )
                            },
                        ),
                        Preference.PreferenceItem.CustomPreference(
                            title = "",
                            content = {
                                ThemeFollowSystemSwitch(
                                    modifier = Modifier.padding(Size.medium),
                                    nightMode = nightMode,
                                    nightModePreference = preferences.nightMode(),
                                )
                            },
                        ),
                    ),
            )
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf(
                SearchTerm(stringResource(R.string.app_theme)),
                SearchTerm(stringResource(R.string.light_theme)),
                SearchTerm(stringResource(R.string.dark_theme)),
            )
        }
    }
}
