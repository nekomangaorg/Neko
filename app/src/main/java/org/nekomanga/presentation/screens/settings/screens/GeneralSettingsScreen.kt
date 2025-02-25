package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.R
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class GeneralSettingsScreen(
    val preferencesHelper: PreferencesHelper,
    val showNotificationSetting: Boolean,
    onNavigationIconClick: () -> Unit,
    val manageNotificationsClicked: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.general

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {
        return persistentListOf(
            Preference.PreferenceItem.ListPreference(
                pref = preferencesHelper.startingTab(),
                title = stringResource(R.string.starting_screen),
                entries =
                    persistentMapOf(
                        1 to stringResource(R.string.last_used_library_recents),
                        -1 to stringResource(R.string.library),
                        -2 to stringResource(R.string.feed),
                        -3 to stringResource(R.string.browse),
                    ),
            ),
            Preference.PreferenceItem.ListPreference(
                pref = preferencesHelper.dateFormatPreference(),
                title = stringResource(R.string.date_format),
                entries =
                    persistentMapOf(
                        "" to stringResource(R.string.system_default),
                        "MM/dd/yy" to "MM/dd/yy",
                        "dd/MM/yy" to "dd/MM/yy",
                        "yyyy-MM-dd" to "yyyy-MM-dd",
                    ),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = preferencesHelper.backReturnsToStart(),
                title = stringResource(R.string.back_to_start),
                subtitle = stringResource(R.string.pressing_back_to_start),
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.manage_notifications),
                enabled = showNotificationSetting,
                onClick = manageNotificationsClicked,
            ),
            // AppShortcuts
            Preference.PreferenceGroup(
                title = stringResource(R.string.app_shortcuts),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.SwitchPreference(
                            pref = preferencesHelper.showSeriesInShortcuts(),
                            title = stringResource(R.string.show_recent_series),
                            subtitle = stringResource(R.string.includes_recently_read_updated_added),
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            pref = preferencesHelper.openChapterInShortcuts(),
                            title = stringResource(R.string.series_opens_new_chapters),
                            subtitle = stringResource(R.string.no_new_chapters_open_details),
                        ),
                    ),
            ),
            // Auto updates
            Preference.PreferenceGroup(
                title = stringResource(R.string.auto_updates),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.ListPreference(
                            pref = preferencesHelper.appShouldAutoUpdate(),
                            title = stringResource(R.string.auto_update_app),
                            entries =
                                persistentMapOf(
                                    0 to stringResource(R.string.over_any_network),
                                    1 to stringResource(R.string.over_wifi_only),
                                    2 to stringResource(R.string.dont_auto_update),
                                ),
                        )
                    ),
            ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf(
                SearchTerm(stringResource(R.string.starting_screen)),
                SearchTerm(stringResource(R.string.date_format)),
                SearchTerm(
                    stringResource(R.string.back_to_start),
                    stringResource(R.string.pressing_back_to_start),
                ),
                SearchTerm(stringResource(R.string.manage_notifications)),
                SearchTerm(stringResource(R.string.app_shortcuts)),
                SearchTerm(
                    stringResource(R.string.show_recent_series),
                    stringResource(R.string.includes_recently_read_updated_added),
                ),
                SearchTerm(
                    stringResource(R.string.series_opens_new_chapters),
                    stringResource(R.string.no_new_chapters_open_details),
                ),
                SearchTerm(stringResource(R.string.auto_update_app)),
            )
        }
    }
}
