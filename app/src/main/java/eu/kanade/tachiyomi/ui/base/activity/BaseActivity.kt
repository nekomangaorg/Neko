package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.security.BiometricActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.getThemeWithExtras
import eu.kanade.tachiyomi.util.system.setThemeAndNight
import uy.kohesive.injekt.injectLazy

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()
    lateinit var binding: VB
    val isBindingInitialized get() = this::binding.isInitialized

    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeAndNight(preferences)
        super.onCreate(savedInstanceState)
        SecureActivityDelegate.setSecure(this)
    }

    override fun onResume() {
        super.onResume()
        if (this !is BiometricActivity && this !is SearchActivity) {
            SecureActivityDelegate.promptLockIfNeeded(this)
        }
    }

    override fun getTheme(): Resources.Theme {
        return getThemeWithExtras(super.getTheme(), preferences)
    }
}
