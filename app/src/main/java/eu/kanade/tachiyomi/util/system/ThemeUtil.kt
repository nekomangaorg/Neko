package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.presentation.theme.Themes

object ThemeUtil {

    fun readerBackgroundColor(theme: Int, context: Context? = null): Int {
        val isDark = context?.isInNightMode() ?: true
        return when (theme) {
            0 -> Color.WHITE
            1 -> Color.BLACK
            3 -> if (isDark) Color.BLACK else Color.WHITE
            4 -> Color.BLACK
            else -> if (isDark) Color.BLACK else Color.WHITE
        }
    }
}

fun AppCompatActivity.setThemeByPref(preferences: PreferencesHelper) {
    setTheme(getPrefTheme(preferences).styleRes())
}

fun Context.getPrefTheme(preferences: PreferencesHelper): Themes {
    // Using a try catch in case I start to remove themes
    return try {
        (if (
                (applicationContext.isInNightMode() ||
                    preferences.nightMode().get() == AppCompatDelegate.MODE_NIGHT_YES) &&
                    preferences.nightMode().get() != AppCompatDelegate.MODE_NIGHT_NO
            ) {
                preferences.darkTheme()
            } else {
                preferences.lightTheme()
            })
            .get()
    } catch (e: Exception) {
        preferences.nightMode().set(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        preferences.lightTheme().set(Themes.Neko)
        preferences.darkTheme().set(Themes.Neko)
        getPrefTheme(preferences)
    }
}
