package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.R
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class GeneralSettingsScreen(
    incognitoMode: Boolean,
    val preferencesHelper: PreferencesHelper,
    val showNotificationSetting: Boolean,
    onNavigationIconClick: (() -> Unit)?,
    val manageNotificationsClicked: () -> Unit,
) : SearchableSettings(onNavigationIconClick, incognitoMode) {

    override fun getTitleRes(): Int = R.string.general

    @Composable
    override fun getPreferences(): List<Preference> {
        return listOf(
            Preference.PreferenceItem.ListPreference(
                pref = preferencesHelper.startingTab(),
                title = stringResource(R.string.starting_screen),
                entries =
                    mapOf(
                        1 to stringResource(R.string.last_used_library_recents),
                        -1 to stringResource(R.string.library),
                        -2 to stringResource(R.string.feed),
                        -3 to stringResource(R.string.browse),
                    ),
                subtitleProvider = { value, options ->
                    options[value] ?: stringResource(R.string.last_used_library_recents)
                },
            ),
            Preference.PreferenceItem.ListPreference(
                pref = preferencesHelper.dateFormatPreference(),
                title = stringResource(R.string.date_format),
                entries =
                    mapOf(
                        "" to stringResource(R.string.system_default),
                        "MM/dd/yy" to "MM/dd/yy",
                        "dd/MM/yy" to "dd/MM/yy",
                        "yyyy-MM-dd" to "yyyy-MM-dd",
                    ),
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.manage_notifications),
                enabled = showNotificationSetting,
                onClick = manageNotificationsClicked,
            ),
            appShortcutsGroup(),
            autoUpdatesGroup(),
        )
    }

    @Composable
    fun appShortcutsGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.app_shortcuts),
            preferenceItems =
                listOf(
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
        )
    }

    @Composable
    fun autoUpdatesGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.auto_updates),
            preferenceItems =
                listOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = preferencesHelper.appShouldAutoUpdate(),
                        title = stringResource(R.string.auto_update_app),
                        entries =
                            mapOf(
                                0 to stringResource(R.string.over_any_network),
                                1 to stringResource(R.string.over_wifi_only),
                                2 to stringResource(R.string.dont_auto_update),
                            ),
                    )
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): List<SearchTerm> {
            return listOf(
                SearchTerm(title = stringResource(R.string.starting_screen)),
                SearchTerm(title = stringResource(R.string.date_format)),
                SearchTerm(title = stringResource(R.string.manage_notifications)),
                SearchTerm(
                    title = stringResource(R.string.show_recent_series),
                    subtitle = stringResource(R.string.includes_recently_read_updated_added),
                    group = stringResource(R.string.app_shortcuts),
                ),
                SearchTerm(
                    title = stringResource(R.string.series_opens_new_chapters),
                    subtitle = stringResource(R.string.no_new_chapters_open_details),
                    group = stringResource(R.string.app_shortcuts),
                ),
                SearchTerm(
                    title = stringResource(R.string.auto_update_app),
                    group = stringResource(R.string.auto_updates),
                ),
            )
        }
    }
}
