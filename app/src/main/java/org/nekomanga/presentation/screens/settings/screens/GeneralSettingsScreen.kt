package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.R
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class GeneralSettingsScreen(
    incognitoMode: Boolean,
    val preferencesHelper: PreferencesHelper,
    val showNotificationSetting: Boolean,
    onBackPressed: () -> Unit,
    val manageNotificationsClicked: () -> Unit,
) : SearchableSettings(onBackPressed, incognitoMode) {

    override fun getTitleRes(): Int = R.string.general

    @Composable
    override fun getPreferences(): PersistentList<Preference> {
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
                subtitleProvider = { value, options ->
                    options[value] ?: stringResource(R.string.last_used_library_recents)
                },
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
            appShortcutsGroup(),
            autoUpdatesGroup(),
        )
    }

    @Composable
    fun appShortcutsGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
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
        )
    }

    @Composable
    fun autoUpdatesGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
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
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(title = stringResource(R.string.starting_screen)),
                SearchTerm(title = stringResource(R.string.date_format)),
                SearchTerm(
                    title = stringResource(R.string.back_to_start),
                    subtitle = stringResource(R.string.pressing_back_to_start),
                ),
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
