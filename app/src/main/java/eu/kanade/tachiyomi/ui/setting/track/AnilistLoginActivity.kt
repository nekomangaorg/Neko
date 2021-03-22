package eu.kanade.tachiyomi.ui.setting.track

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity.CENTER
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

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
