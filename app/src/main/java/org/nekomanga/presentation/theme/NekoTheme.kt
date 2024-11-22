package org.nekomanga.presentation.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.presentation.theme.Typefaces.appTypography
import org.nekomanga.presentation.theme.colorschemes.LavenderColorScheme
import org.nekomanga.presentation.theme.colorschemes.LimeColorScheme
import org.nekomanga.presentation.theme.colorschemes.MonetColorScheme
import org.nekomanga.presentation.theme.colorschemes.NekoColorScheme
import org.nekomanga.presentation.theme.colorschemes.TakoColorScheme
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun NekoTheme(content: @Composable () -> Unit) {

    val colorScheme = nekoThemeColorScheme()

    MaterialTheme(colorScheme = colorScheme, typography = appTypography, content = content)
}

@Composable
@ReadOnlyComposable
fun nekoThemeColorScheme(): ColorScheme {
    val preferences = Injekt.get<PreferencesHelper>()

    val theme =
        if (
                (isSystemInDarkTheme() ||
                    preferences.nightMode().get() == AppCompatDelegate.MODE_NIGHT_YES) &&
                    preferences.nightMode().get() != AppCompatDelegate.MODE_NIGHT_NO
            ) {
                preferences.darkTheme()
            } else {
                preferences.lightTheme()
            }
            .get()

    return when (theme) {
        Themes.Monet -> MonetColorScheme(LocalContext.current)
        Themes.Lavender -> LavenderColorScheme
        Themes.Lime -> LimeColorScheme
        Themes.Tako -> TakoColorScheme
        else -> NekoColorScheme
    }.getColorScheme(isSystemInDarkTheme())
}
