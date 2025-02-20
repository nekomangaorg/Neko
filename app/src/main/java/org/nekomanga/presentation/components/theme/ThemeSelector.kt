package org.nekomanga.presentation.components.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.Themes

@Composable
fun ThemeSelector(
    modifier: Modifier = Modifier,
    preferences: PreferencesHelper,
    darkThemeSelector: Boolean,
) {
    val nightMode by preferences.nightMode().collectAsState()

    val followingSystemTheme by
        remember(nightMode) {
            derivedStateOf { nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
        }

    val darkAppTheme by preferences.darkTheme().collectAsState()
    val lightAppTheme by preferences.lightTheme().collectAsState()
    val context = LocalContext.current

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

    val darkThemes by remember {
        derivedStateOf {
            Themes.entries
                .filter {
                    (it.isDarkTheme() || it.followsSystem()) &&
                        (it.styleRes() != R.style.Theme_Tachiyomi_Monet || supportsDynamic)
                }
                .toSet()
        }
    }

    ThemeContent(
        modifier = modifier,
        context = context,
        themeSet = if (darkThemeSelector) darkThemes else lightThemes,
        preferences = preferences,
        darkAppTheme = darkAppTheme,
        lightAppTheme = lightAppTheme,
        nightMode = nightMode,
        followingSystemTheme = followingSystemTheme,
        isDarkThemeContent = darkThemeSelector,
    )
}

@Composable
private fun ThemeContent(
    modifier: Modifier = Modifier,
    context: Context,
    themeSet: Set<Themes>,
    preferences: PreferencesHelper,
    darkAppTheme: Themes,
    lightAppTheme: Themes,
    nightMode: Int,
    followingSystemTheme: Boolean,
    isDarkThemeContent: Boolean,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Size.medium),
    ) {
        Gap(Size.small)

        themeSet.forEach { theme ->
            val isSelected =
                remember(darkAppTheme, lightAppTheme, nightMode) {
                    isSelected(
                        theme = theme,
                        isDarkTheme = isDarkThemeContent,
                        darkAppTheme = darkAppTheme,
                        lightAppTheme = lightAppTheme,
                        nightMode = nightMode,
                    )
                }
            ThemeItem(
                theme = theme,
                isDarkTheme = isDarkThemeContent,
                selected = isSelected,
                onClick = {
                    themeClicked(
                        theme = theme,
                        context = context,
                        isSelected = isSelected,
                        followingSystemTheme = followingSystemTheme,
                        isDarkTheme = isDarkThemeContent,
                        preferences = preferences,
                    )
                },
            )
        }
        Gap(Size.small)
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
    preferences: PreferencesHelper,
    isDarkTheme: Boolean,
) {

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
