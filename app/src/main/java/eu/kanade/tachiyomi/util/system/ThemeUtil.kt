package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.presentation.theme.Themes

object ThemeUtil {

    /** Migration method */
    fun convertNewThemes(preferences: PreferencesHelper, theme: Int) {
        preferences
            .nightMode()
            .set(
                when (theme) {
                    0,
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2,
                    3,
                    4,
                    9 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        preferences.lightTheme().set(Themes.Neko)
        preferences.darkTheme().set(Themes.Neko)
    }

    /** Migration method */
    fun convertNewThemes(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lightTheme = prefs.getString(PreferenceKeys.lightTheme, "DEFAULT")
        val darkTheme = prefs.getString(PreferenceKeys.darkTheme, "DEFAULT")

        prefs.edit {
            putString(
                PreferenceKeys.lightTheme,
                when (lightTheme) {
                    "SPRING" -> Themes.Pink
                    "STRAWBERRY_DAIQUIRI" -> Themes.Red
                    else -> Themes.Neko
                }.name,
            )
            putString(
                PreferenceKeys.darkTheme,
                when (darkTheme) {
                    "DUSK" -> Themes.Pink
                    "CHOCOLATE_STRAWBERRIES" -> Themes.Red
                    else -> Themes.Neko
                }.name,
            )
        }
    }

    fun readerBackgroundColor(theme: Int): Int {
        return when (theme) {
            1 -> Color.BLACK
            else -> Color.WHITE
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
        ThemeUtil.convertNewThemes(preferences.context)
        getPrefTheme(preferences)
    }
}
