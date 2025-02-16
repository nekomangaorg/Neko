package org.nekomanga.presentation.screens.settings.screens

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.settings.Preference

fun generalSettingItems(
    preferencesHelper: PreferencesHelper,
    showNotificationSetting: Boolean,
    manageNotificationsClicked: () -> Unit,
): ImmutableList<Preference> {
    return persistentListOf(
        Preference.PreferenceItem.ListPreference(
            pref = preferencesHelper.startingTab(),
            title = UiText.StringResource(R.string.starting_screen),
            entries =
                persistentMapOf(
                    1 to UiText.StringResource(R.string.last_used_library_recents),
                    -1 to UiText.StringResource(R.string.library),
                    -2 to UiText.StringResource(R.string.feed),
                    -3 to UiText.StringResource(R.string.browse),
                ),
        ),
        Preference.PreferenceItem.ListPreference(
            pref = preferencesHelper.dateFormatPreference(),
            title = UiText.StringResource(R.string.date_format),
            entries =
                persistentMapOf(
                    "" to UiText.StringResource(R.string.system_default),
                    "MM/dd/yy" to UiText.String("MM/dd/yy"),
                    "dd/MM/yy" to UiText.String("dd/MM/yy"),
                    "yyyy-MM-dd" to UiText.String("yyyy-MM-dd"),
                ),
        ),
        Preference.PreferenceItem.SwitchPreference(
            pref = preferencesHelper.backReturnsToStart(),
            title = UiText.StringResource(R.string.back_to_start),
            subtitle = UiText.StringResource(R.string.pressing_back_to_start),
        ),
        Preference.PreferenceItem.TextPreference(
            title = UiText.StringResource(R.string.manage_notifications),
            enabled = showNotificationSetting,
            onClick = manageNotificationsClicked,
        ),
        // AppShortcuts
        Preference.PreferenceGroup(
            title = UiText.StringResource(R.string.app_shortcuts),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = preferencesHelper.showSeriesInShortcuts(),
                        title = UiText.StringResource(R.string.show_recent_series),
                        subtitle =
                            UiText.StringResource(R.string.includes_recently_read_updated_added),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = preferencesHelper.openChapterInShortcuts(),
                        title = UiText.StringResource(R.string.series_opens_new_chapters),
                        subtitle = UiText.StringResource(R.string.no_new_chapters_open_details),
                    ),
                ),
        ),
        // Auto updates
        Preference.PreferenceGroup(
            title = UiText.StringResource(R.string.auto_updates),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = preferencesHelper.appShouldAutoUpdate(),
                        title = UiText.StringResource(R.string.auto_update_app),
                        entries =
                            persistentMapOf(
                                0 to UiText.StringResource(R.string.over_any_network),
                                1 to UiText.StringResource(R.string.over_wifi_only),
                                2 to UiText.StringResource(R.string.dont_auto_update),
                            ),
                    )
                ),
        ),
    )
}
