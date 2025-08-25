package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.ActivityCompat
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.getActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.R
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.presentation.components.theme.ThemeFollowSystemSwitch
import org.nekomanga.presentation.components.theme.ThemeSelector
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.theme.Size

internal class AppearanceSettingsScreen(
    val mangaDetailsPreferences: MangaDetailsPreferences,
    val preferences: PreferencesHelper,
    onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {
    override fun getTitleRes(): Int = R.string.appearance

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {

        val context = LocalContext.current

        val nightMode by preferences.nightMode().collectAsState()

        return persistentListOf(appearanceGroup(nightMode), detailGroup(), navigationGroup(context))
    }

    @Composable
    fun appearanceGroup(nightMode: Int): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
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
                                modifier =
                                    Modifier.padding(
                                        start = Size.medium,
                                        end = Size.medium,
                                        top = Size.medium,
                                    ),
                                textStyle =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                nightMode = nightMode,
                                nightModePreference = preferences.nightMode(),
                            )
                        },
                    ),
                ),
        )
    }

    @Composable
    fun detailGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.details_page),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = mangaDetailsPreferences.forcePortrait(),
                        title = stringResource(R.string.force_portrait_details),
                        subtitle = stringResource(R.string.force_portrait_details_description),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = mangaDetailsPreferences.autoThemeByCover(),
                        title = stringResource(R.string.theme_buttons_based_on_cover),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = mangaDetailsPreferences.hideButtonText(),
                        title = stringResource(R.string.hide_button_text),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = mangaDetailsPreferences.extraLargeBackdrop(),
                        title = stringResource(R.string.extra_large_backdrop),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = mangaDetailsPreferences.wrapAltTitles(),
                        title = stringResource(R.string.wrap_alt_titles),
                    ),
                ),
        )
    }

    @Composable
    fun navigationGroup(context: Context): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.navigation),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = preferences.sideNavIconAlignment(),
                        title = stringResource(R.string.side_nav_icon_alignment),
                        entries =
                            persistentMapOf(
                                0 to stringResource(R.string.top),
                                1 to stringResource(R.string.center),
                                2 to stringResource(R.string.bottom),
                            ),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = preferences.sideNavMode(),
                        title = stringResource(R.string.use_side_navigation),
                        entries =
                            persistentMapOf(
                                SideNavMode.DEFAULT.prefValue to
                                    stringResource(SideNavMode.DEFAULT.stringRes),
                                SideNavMode.NEVER.prefValue to
                                    stringResource(SideNavMode.NEVER.stringRes),
                                SideNavMode.ALWAYS.prefValue to
                                    stringResource(SideNavMode.ALWAYS.stringRes),
                            ),
                        onValueChanged = {
                            context.getActivity()?.let { activity ->
                                ActivityCompat.recreate(activity)
                            }
                            true
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(R.string.by_default_side_nav_info)
                    ),
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf(
                SearchTerm(
                    title = stringResource(R.string.light_theme),
                    group = stringResource(R.string.app_theme),
                ),
                SearchTerm(
                    title = stringResource(R.string.dark_theme),
                    group = stringResource(R.string.app_theme),
                ),
                SearchTerm(
                    title = stringResource(R.string.force_portrait_details),
                    subtitle = stringResource(R.string.force_portrait_details_description),
                    group = stringResource(R.string.details_page),
                ),
                SearchTerm(
                    title = stringResource(R.string.details_page),
                    group = stringResource(R.string.details_page),
                ),
                SearchTerm(
                    title = stringResource(R.string.theme_buttons_based_on_cover),
                    group = stringResource(R.string.details_page),
                ),
                SearchTerm(
                    title = stringResource(R.string.hide_button_text),
                    group = stringResource(R.string.details_page),
                ),
                SearchTerm(
                    title = stringResource(R.string.extra_large_backdrop),
                    group = stringResource(R.string.details_page),
                ),
                SearchTerm(
                    title = stringResource(R.string.wrap_alt_titles),
                    group = stringResource(R.string.details_page),
                ),
                SearchTerm(
                    title = stringResource(R.string.side_nav_icon_alignment),
                    group = stringResource(R.string.navigation),
                ),
                SearchTerm(
                    title = stringResource(R.string.use_side_navigation),
                    group = stringResource(R.string.navigation),
                ),
                SearchTerm(
                    title = stringResource(R.string.by_default_side_nav_info),
                    group = stringResource(R.string.navigation),
                ),
            )
        }
    }
}
