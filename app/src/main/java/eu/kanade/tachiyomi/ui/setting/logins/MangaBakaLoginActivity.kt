package eu.kanade.tachiyomi.ui.setting.logins

import android.net.Uri
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.util.system.launchIO

class MangaBakaLoginActivity : BaseOAuthLoginActivity() {

    override fun handleResult(data: Uri?) {
        val code = data?.getQueryParameter("code")
        if (code != null) {
            lifecycleScope.launchIO {
                trackManager.mangaBaka.login(code)
                returnToSettings()
            }
        } else {
            trackManager.mangaBaka.logout()
            returnToSettings()
        }
    }
}
