package eu.kanade.tachiyomi.ui.setting.logins

import android.net.Uri
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.util.system.launchIO
import org.nekomanga.logging.TimberKt

class MangaBakaLoginActivity : BaseOAuthLoginActivity() {

    override fun handleResult(data: Uri?) {
        val code = data?.getQueryParameter("code")
        val state = data?.getQueryParameter("state")
        if (state == null) {
            TimberKt.e { "Did not receive state parameter from MangaBaka Oauth" }
            logoutExit()
        } else if (code != null) {
            if (!trackManager.mangaBaka.verifyOAuthState(state)) {
                TimberKt.w { "Received invalid state parameter from MangaBaka Oauth" }
                logoutExit()
            }
            lifecycleScope.launchIO {
                trackManager.mangaBaka.login(code)
                returnToSettings()
            }
        } else {
            logoutExit()
        }
    }

    fun logoutExit() {
        trackManager.mangaBaka.logout()
        returnToSettings()
    }
}
