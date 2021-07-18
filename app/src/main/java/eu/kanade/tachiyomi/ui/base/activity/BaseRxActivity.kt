package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.getThemeWithExtras
import nucleus.view.NucleusAppCompatActivity
import uy.kohesive.injekt.injectLazy

abstract class BaseRxActivity<P : BasePresenter<*>> : NucleusAppCompatActivity<P>() {

    val scope = lifecycleScope
    private val preferences by injectLazy<PreferencesHelper>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureActivityDelegate.setSecure(this)
    }

    override fun onResume() {
        super.onResume()
        SecureActivityDelegate.promptLockIfNeeded(this)
    }

    override fun getTheme(): Resources.Theme {
        return getThemeWithExtras(super.getTheme(), preferences)
    }
}
