package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.security.BiometricActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.ThemeUtil
import uy.kohesive.injekt.injectLazy

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()
    lateinit var binding: VB

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (preferences.theme().isNotSet()) {
            ThemeUtil.convertTheme(preferences, preferences.oldTheme())
        }
        // Using a try catch in case I start to remove themes
        val theme = try {
            preferences.theme().get()
        } catch (e: Exception) {
            preferences.theme().set(ThemeUtil.Themes.DEFAULT)
            ThemeUtil.Themes.DEFAULT
        }
        AppCompatDelegate.setDefaultNightMode(theme.nightMode)
        setTheme(theme.styleRes)
        super.onCreate(savedInstanceState)
        SecureActivityDelegate.setSecure(this)
    }

    override fun onResume() {
        super.onResume()
        if (this !is BiometricActivity && this !is SearchActivity) {
            SecureActivityDelegate.promptLockIfNeeded(this)
        }
    }
}
