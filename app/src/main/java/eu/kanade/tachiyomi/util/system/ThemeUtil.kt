package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper

object ThemeUtil {

    /** Migration method */
    fun convertTheme(preferences: PreferencesHelper, theme: Int) {
        preferences.theme().set(
            when (theme) {
                0 -> Themes.PURE_WHITE
                1 -> Themes.LIGHT_BLUE
                2 -> Themes.DARK
                3 -> Themes.AMOLED
                4 -> Themes.DARK_BLUE
                5 -> Themes.DEFAULT
                6 -> Themes.DEFAULT_AMOLED
                7 -> Themes.ALL_BLUE
                else -> Themes.DEFAULT
            }
        )
    }

    fun isColoredTheme(theme: Themes): Boolean {
        return theme.styleRes == R.style.Theme_Tachiyomi_AllBlue
    }

    fun isPitchBlack(context: Context, theme: Themes): Boolean {
        return context.isInNightMode() && theme.styleRes == R.style.Theme_Tachiyomi_Amoled
    }

    fun hasDarkActionBarInLight(context: Context, theme: Themes): Boolean {
        return !context.isInNightMode() && isColoredTheme(theme)
    }

    fun readerBackgroundColor(theme: Int): Int {
        return when (theme) {
            1 -> Color.BLACK
            else -> Color.WHITE
        }
    }

    @Suppress("unused")
    enum class Themes(@StyleRes val styleRes: Int, val nightMode: Int, @StringRes val nameRes: Int) {
        PURE_WHITE(R.style.Theme_Tachiyomi, AppCompatDelegate.MODE_NIGHT_NO, R.string.white_theme),
        LIGHT_BLUE(R.style.Theme_Tachiyomi_AllBlue, AppCompatDelegate.MODE_NIGHT_NO, R.string.light_blue),
        DARK(R.style.Theme_Tachiyomi, AppCompatDelegate.MODE_NIGHT_YES, R.string.dark),
        AMOLED(R.style.Theme_Tachiyomi_Amoled, AppCompatDelegate.MODE_NIGHT_YES, R.string.amoled_black),
        DARK_BLUE(R.style.Theme_Tachiyomi_AllBlue, AppCompatDelegate.MODE_NIGHT_YES, R.string.dark_blue),
        DEFAULT(R.style.Theme_Tachiyomi, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.system_default),
        DEFAULT_AMOLED(R.style.Theme_Tachiyomi_Amoled, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.system_default_amoled),
        ALL_BLUE(R.style.Theme_Tachiyomi_AllBlue, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.system_default_all_blue),
    }
}
