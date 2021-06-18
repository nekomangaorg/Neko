package eu.kanade.tachiyomi.util.system

import android.app.Activity
import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.data.preference.PreferencesHelper

object ThemeUtil {

    /** Migration method */
    fun convertTheme(preferences: PreferencesHelper, theme: Int) {
        preferences.nightMode().set(
            when (theme) {
                0, 1 -> AppCompatDelegate.MODE_NIGHT_NO
                2, 3, 4, 9 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        preferences.lightTheme().set(Themes.PURE_WHITE)
        preferences.darkTheme().set(
            when (theme) {
                3 -> Themes.AMOLED
                else -> Themes.DARK
            }
        )
    }

    fun isColoredTheme(theme: Themes): Boolean {
        return false
    }

    fun isPitchBlack(context: Context, theme: Themes): Boolean {
        return context.isInNightMode() && theme.darkBackground == Color.BLACK
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
}

fun Activity.setThemeAndNight(preferences: PreferencesHelper) {
    if (preferences.nightMode().isNotSet()) {
        ThemeUtil.convertTheme(preferences, preferences.oldTheme())
    }
    AppCompatDelegate.setDefaultNightMode(preferences.nightMode().get())
    val theme = getPrefTheme(preferences)
    setTheme(theme.styleRes)
}

fun Context.getPrefTheme(preferences: PreferencesHelper): Themes {
    // Using a try catch in case I start to remove themes
    return try {
        (
            if ((applicationContext.isInNightMode() || preferences.nightMode()
                    .get() == AppCompatDelegate.MODE_NIGHT_YES) &&
                preferences.nightMode().get() != AppCompatDelegate.MODE_NIGHT_NO
            ) preferences.darkTheme() else preferences.lightTheme()
            ).get()
    } catch (e: Exception) {
        preferences.lightTheme().set(Themes.PURE_WHITE)
        preferences.darkTheme().set(Themes.DARK)
        Themes.PURE_WHITE
    }
}
