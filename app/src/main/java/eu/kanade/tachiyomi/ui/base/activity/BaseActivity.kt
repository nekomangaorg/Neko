package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.security.BiometricActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.ThemeUtil
import uy.kohesive.injekt.injectLazy

abstract class BaseActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(ThemeUtil.nightMode(preferences.theme()))
        val theme = preferences.theme()
        setTheme(when {
            ThemeUtil.isAMOLEDTheme(theme) -> R.style.Theme_Tachiyomi_Amoled
            ThemeUtil.isBlueTheme(theme) -> R.style.Theme_Tachiyomi_AllBlue
            else -> R.style.Theme_Tachiyomi
        })
        super.onCreate(savedInstanceState)
        SecureActivityDelegate.setSecure(this)
    }

    override fun onResume() {
        super.onResume()
        if (this !is BiometricActivity && this !is SearchActivity)
            SecureActivityDelegate.promptLockIfNeeded(this)
    }
}
