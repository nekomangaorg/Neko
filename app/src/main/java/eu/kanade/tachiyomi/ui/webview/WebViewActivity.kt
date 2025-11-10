package eu.kanade.tachiyomi.ui.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.setThemeByPref
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.presentation.screens.WebViewScreen
import org.nekomanga.presentation.theme.NekoTheme
import tachiyomi.core.util.system.WebViewUtil
import uy.kohesive.injekt.injectLazy

open class WebViewActivity : AppCompatActivity() {

    private val sourceManager by injectLazy<SourceManager>()
    private val preferences: PreferencesHelper by injectLazy()
    private val securityPreferences: SecurityPreferences by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setThemeByPref(preferences)

        if (!WebViewUtil.supportsWebView(this)) {
            toast(R.string.information_webview_required, Toast.LENGTH_LONG)
            finish()
            return
        }

        securityPreferences
            .incognitoMode()
            .changes()
            .onEach { SecureActivityDelegate.setSecure(this) }
            .launchIn(lifecycleScope)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        title = intent.extras?.getString(TITLE_KEY) ?: ""

        val url = intent.extras!!.getString(URL_KEY) ?: return

        val source = sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? HttpSource ?: return
        val headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }
        setContent {
            NekoTheme {
                WebViewScreen(title = title.toString(), url = url, onBackPressed = { finish() })
            }
        }
    }

    companion object {
        const val SOURCE_KEY = "source_key"
        const val URL_KEY = "url_key"
        const val TITLE_KEY = "title_key"

        fun newIntent(context: Context, sourceId: Long, url: String, title: String?): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(SOURCE_KEY, sourceId)
            intent.putExtra(URL_KEY, url)
            intent.putExtra(TITLE_KEY, title)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }

        fun newIntent(context: Context, url: String, title: String?): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(URL_KEY, url)
            intent.putExtra(TITLE_KEY, title)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }
    }
}
