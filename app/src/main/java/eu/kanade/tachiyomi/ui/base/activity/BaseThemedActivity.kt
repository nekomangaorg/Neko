package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.ThemeUtil
import uy.kohesive.injekt.injectLazy

abstract class BaseThemedActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (preferences.theme().isNotSet()) {
            ThemeUtil.convertTheme(preferences, preferences.oldTheme())
        }
        val theme = preferences.theme().get()
        AppCompatDelegate.setDefaultNightMode(theme.nightMode)
        setTheme(theme.styleRes)

        super.onCreate(savedInstanceState)
    }
}
