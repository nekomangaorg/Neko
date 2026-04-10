package eu.kanade.tachiyomi.ui.setting.logins

import android.net.Uri
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.launchIO
import uy.kohesive.injekt.injectLazy

class MangaBakaLoginActivity : BaseOAuthLoginActivity() {

    private val preferences: PreferencesHelper by injectLazy()

    override fun handleResult(data: Uri?) {
        val code = data?.getQueryParameter("code")
        if (code != null) {
            lifecycleScope.launchIO {
                val codeVerifier = preferences.mangabakaCodeVerifier.get()
                trackManager.mangaBaka.login(code, codeVerifier)
                returnToSettings()
            }
        } else {
            trackManager.mangaBaka.logout()
            returnToSettings()
        }
    }
}
