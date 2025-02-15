package org.nekomanga.presentation.screens.settings.screens

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.icons.MergeIcon
import org.nekomanga.presentation.screens.settings.Preference

fun generalSettingItems(preferencesHelper: PreferencesHelper): ImmutableList<Preference> {
    return persistentListOf(
        Preference.PreferenceItem.SwitchPreference(
            pref = preferencesHelper.backReturnsToStart(),
            title = UiText.StringResource(R.string.back_to_start),
            subtitle = UiText.StringResource(R.string.pressing_back_to_start),
        ),
        Preference.PreferenceItem.TextPreference(
            title = UiText.StringResource(R.string.general),
            icon = MergeIcon,
        ),
    )
}
