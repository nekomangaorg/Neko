package eu.kanade.tachiyomi.util.system

import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.R

object ThemeUtil {
    fun isBlueTheme(theme: Int): Boolean {
        return theme == 4 || theme == 8 || theme == 7
    }

    fun isAMOLEDTheme(theme: Int): Boolean {
        return theme == 3 || theme == 6
    }

    fun theme(theme: Int): Int {
        return when {
            isAMOLEDTheme(theme) -> R.style.Theme_Tachiyomi_Amoled
            isBlueTheme(theme) -> R.style.Theme_Tachiyomi_AllBlue
            else -> R.style.Theme_Tachiyomi
        }
    }

    fun readerBackgroundColor(theme: Int): Int {
        return when (theme) {
            1 -> Color.BLACK
            else -> Color.WHITE
        }
    }

    fun nightMode(theme: Int): Int {
        return when (theme) {
            1, 8 -> AppCompatDelegate.MODE_NIGHT_NO
            2, 3, 4 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
