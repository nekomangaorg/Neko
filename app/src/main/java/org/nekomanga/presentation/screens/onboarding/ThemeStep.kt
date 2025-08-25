package org.nekomanga.presentation.screens.onboarding

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.isInNightMode
import jp.wasabeef.gap.Gap
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.theme.ThemeFollowSystemSwitch
import org.nekomanga.presentation.components.theme.ThemeSelector
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.Themes
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ThemeStep : OnboardingStep {
    override val isComplete: Boolean = true

    private val preferences: PreferencesHelper = Injekt.get()

    @Composable
    override fun Content() {

        val nightMode by preferences.nightMode().collectAsState()

        Column(modifier = Modifier.padding(Size.medium)) {
            ThemeSelector(preferences = preferences, darkThemeSelector = false)
            Gap(Size.small)
            ThemeSelector(preferences = preferences, darkThemeSelector = true)
            ThemeFollowSystemSwitch(
                modifier = Modifier.padding(vertical = Size.small),
                nightMode = nightMode,
                nightModePreference = preferences.nightMode(),
            )
        }
    }

    fun isSelected(
        theme: Themes,
        isDarkTheme: Boolean,
        darkAppTheme: Themes,
        lightAppTheme: Themes,
        nightMode: Int,
    ): Boolean {
        return when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> darkAppTheme == theme && isDarkTheme
            AppCompatDelegate.MODE_NIGHT_NO -> lightAppTheme == theme && !isDarkTheme
            else ->
                (darkAppTheme == theme && isDarkTheme) || (lightAppTheme == theme && !isDarkTheme)
        }
    }

    private fun themeClicked(
        theme: Themes,
        context: Context,
        isSelected: Boolean,
        followingSystemTheme: Boolean,
        isDarkTheme: Boolean,
    ) {
        TimberKt.d {
            """
                isSelected: $isSelected
                isDarkTheme() : $isDarkTheme
                followingSystemTheme: $followingSystemTheme
                isInNightMode: ${context.isInNightMode()}
            """
                .trimIndent()
        }

        val nightMode =
            when (isDarkTheme) {
                true -> {
                    preferences.darkTheme().set(theme)
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                false -> {
                    preferences.lightTheme().set(theme)
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            }

        if (followingSystemTheme && isSelected) {
            preferences.nightMode().set(nightMode)
        } else if (!followingSystemTheme) {
            preferences.nightMode().set(nightMode)
        }

        (context as? Activity)?.let { activity -> ActivityCompat.recreate(activity) }
    }
}
