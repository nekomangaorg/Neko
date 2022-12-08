package eu.kanade.tachiyomi.ui.setting.logins

import android.net.Uri
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.util.system.launchIO

class AnilistLoginActivity : BaseOAuthLoginActivity() {

    override fun handleResult(data: Uri?) {
        val regex = "(?:access_token=)(.*?)(?:&)".toRegex()
        val matchResult = regex.find(data?.fragment.toString())
        if (matchResult?.groups?.get(1) != null) {
            lifecycleScope.launchIO {
                trackManager.aniList.login(matchResult.groups[1]!!.value)
                returnToSettings()
            }
        } else {
            trackManager.aniList.logout()
            returnToSettings()
        }
    }
}
