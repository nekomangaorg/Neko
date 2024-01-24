package eu.kanade.tachiyomi.ui.setting.logins

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.ui.base.activity.BaseThemedActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import org.nekomanga.presentation.screens.LoadingScreen
import org.nekomanga.presentation.theme.NekoTheme
import uy.kohesive.injekt.injectLazy

abstract class BaseOAuthLoginActivity : BaseThemedActivity() {

    internal val trackManager: TrackManager by injectLazy()
    internal val dexLoginHelper: MangaDexLoginHelper by injectLazy()

    abstract fun handleResult(data: Uri?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { NekoTheme { LoadingScreen() } }

        handleResult(intent.data)
    }

    internal fun returnToSettings() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finishAfterTransition()
    }
}
