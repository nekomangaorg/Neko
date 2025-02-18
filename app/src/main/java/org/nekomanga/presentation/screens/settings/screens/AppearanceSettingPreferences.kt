package org.nekomanga.presentation.screens.settings.screens

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.settings.Preference

fun appearanceSettingItems(preferencesHelper: PreferencesHelper): ImmutableList<Preference> {
    return persistentListOf(
        Preference.PreferenceGroup(
            title = UiText.StringResource(R.string.appearance),
            preferenceItems = persistentListOf(),
        )
    )
}
