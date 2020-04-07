package eu.kanade.tachiyomi.util.system

import androidx.appcompat.app.AppCompatDelegate

object ThemeUtil {
    fun isBlueTheme(theme: Int): Boolean {
        return theme == 4 || theme == 8 || theme == 7
    }

    fun isAMOLEDTheme(theme: Int): Boolean {
        return theme == 3 || theme == 6
    }

    fun isNekoTheme(theme: Int): Boolean {
        return theme == 9
    }

    fun nightMode(theme: Int): Int {
        return when (theme) {
            1, 8 -> AppCompatDelegate.MODE_NIGHT_NO
            2, 3, 4, 9 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
