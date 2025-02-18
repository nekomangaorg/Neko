/*
package org.nekomanga.presentation.screens.settings.screens

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.isInNightMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.theme.ThemeItem
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.Themes

fun appearanceSettingItems(
    preferencesHelper: PreferencesHelper,
    lightThemeContent: @Composable () -> Unit,
    darkThemeContent: @Composable () -> Unit,
): ImmutableList<Preference> {
    return persistentListOf(
        Preference.PreferenceGroup(
            title = UiText.StringResource(R.string.app_theme),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.CustomPreference(
                        title = UiText.StringResource(R.string.light_theme),
                        content = lightThemeContent,
                    ),
                    Preference.PreferenceItem.CustomPreference(
                        title = UiText.StringResource(R.string.dark_theme),
                        content = darkThemeContent,
                    ),
                ),
        )
    )
}

@Composable
fun lightThemeContent(
    preferences: PreferencesHelper,
    darkAppTheme: Themes,
    lightAppTheme: Themes,
    nightMode: Int,
) {
    val supportsDynamic = DynamicColors.isDynamicColorAvailable()
    val lightThemes by remember {
        derivedStateOf {
            Themes.entries
                .filter {
                    (!it.isDarkTheme() || it.followsSystem()) &&
                        (it.styleRes() != R.style.Theme_Tachiyomi_Monet || supportsDynamic)
                }
                .toSet()
        }
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Size.medium),
    ) {
        lightThemes.forEach { theme ->
            val isSelected =
                remember(darkAppTheme, lightAppTheme, nightMode) {
                    isSelected(theme, false, darkAppTheme, lightAppTheme, nightMode)
                }
            ThemeItem(
                theme = theme,
                isDarkTheme = false,
                selected = isSelected,
                onClick = {
                    themeClicked(
                        theme,
                        context,
                        isSelected = isSelected,
                        followingSystemTheme = followingSystemTheme,
                        isDarkTheme = false,
                    )
                },
            )
        }
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
        else -> (darkAppTheme == theme && isDarkTheme) || (lightAppTheme == theme && !isDarkTheme)
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
*/
