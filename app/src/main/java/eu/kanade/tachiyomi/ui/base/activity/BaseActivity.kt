package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.LocaleHelper
import uy.kohesive.injekt.injectLazy

abstract class BaseActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            when (preferences.theme()) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2, 3, 4 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        setTheme(when (preferences.theme()) {
            3, 6 -> R.style.Theme_Tachiyomi_Amoled
            4, 7 -> R.style.Theme_Tachiyomi_DarkBlue
            else -> R.style.Theme_Tachiyomi
        })
        super.onCreate(savedInstanceState)
    }

}
