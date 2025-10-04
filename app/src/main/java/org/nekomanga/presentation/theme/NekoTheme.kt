package org.nekomanga.presentation.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.presentation.theme.Typefaces.appTypography
import org.nekomanga.presentation.theme.colorschemes.BlueColorScheme
import org.nekomanga.presentation.theme.colorschemes.BrownColorScheme
import org.nekomanga.presentation.theme.colorschemes.GreenColorScheme
import org.nekomanga.presentation.theme.colorschemes.MonetColorScheme
import org.nekomanga.presentation.theme.colorschemes.MonochromeColorScheme
import org.nekomanga.presentation.theme.colorschemes.NekoColorScheme
import org.nekomanga.presentation.theme.colorschemes.NordColorScheme
import org.nekomanga.presentation.theme.colorschemes.OrangeColorScheme
import org.nekomanga.presentation.theme.colorschemes.PinkColorScheme
import org.nekomanga.presentation.theme.colorschemes.PurpleColorScheme
import org.nekomanga.presentation.theme.colorschemes.RetroColorScheme
import org.nekomanga.presentation.theme.colorschemes.TakoColorScheme
import org.nekomanga.presentation.theme.colorschemes.TealColorScheme
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun NekoTheme(colorScheme: ColorScheme? = null, content: @Composable () -> Unit) {
    val finalColorScheme = colorScheme ?: nekoThemeColorScheme()

    MaterialExpressiveTheme(
        colorScheme = finalColorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = appTypography,
        content = content,
    )
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

    return colorSchemeFromTheme(LocalContext.current, theme, isSystemInDarkTheme())
}

fun colorSchemeFromTheme(
    context: Context,
    theme: Themes,
    isSystemInDarkTheme: Boolean,
): ColorScheme {
    return when (theme) {
        Themes.Blue -> BlueColorScheme
        Themes.Teal -> TealColorScheme
        Themes.Green -> GreenColorScheme
        Themes.Monet -> MonetColorScheme(context)
        Themes.Monochrome -> MonochromeColorScheme
        Themes.Nord -> NordColorScheme
        Themes.Orange -> OrangeColorScheme
        Themes.Pink -> PinkColorScheme
        Themes.Purple -> PurpleColorScheme
        Themes.Retro -> RetroColorScheme
        Themes.Brown -> BrownColorScheme
        Themes.Tako -> TakoColorScheme
        else -> NekoColorScheme
    }.getColorScheme(isSystemInDarkTheme)
}
