package eu.kanade.tachiyomi.ui.setting.logins

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import org.nekomanga.R

class MangaDexLoginActivity : BaseOAuthLoginActivity() {
    override fun handleResult(data: Uri?) {
        val code = data?.getQueryParameter("code")
        if (code != null) {
            lifecycleScope.launchIO {
                if (!dexLoginHelper.login(code)) {
                    lifecycleScope.launchUI {
                        Toast.makeText(
                                this@MangaDexLoginActivity.applicationContext,
                                R.string.could_not_sign_in,
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }
                }
                returnToSettings()
            }
        } else {
            dexLoginHelper.invalidate()
            returnToSettings()
        }
    }
}
