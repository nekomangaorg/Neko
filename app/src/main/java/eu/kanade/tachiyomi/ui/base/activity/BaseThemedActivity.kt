package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.setThemeAndNight
import eu.kanade.tachiyomi.util.system.getThemeWithExtras
import uy.kohesive.injekt.injectLazy

abstract class BaseThemedActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeAndNight(preferences)
        super.onCreate(savedInstanceState)
    }

    override fun getTheme(): Resources.Theme {
        return getThemeWithExtras(super.getTheme(), preferences)
    }
}
