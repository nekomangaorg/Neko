package eu.kanade.tachiyomi.ui.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlowIn
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import org.nekomanga.presentation.screens.WebViewScreen
import org.nekomanga.presentation.theme.NekoTheme
import uy.kohesive.injekt.injectLazy

open class WebViewActivity : AppCompatActivity() {

    private val sourceManager by injectLazy<SourceManager>()
    private val preferences: PreferencesHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!WebViewUtil.supportsWebView(this)) {
            toast(R.string.webview_is_required, Toast.LENGTH_LONG)
            finish()
            return
        }

        preferences.incognitoMode()
            .asImmediateFlowIn(lifecycleScope) {
                SecureActivityDelegate.setSecure(this)
            }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        title = intent.extras?.getString(TITLE_KEY) ?: ""

        val url = intent.extras!!.getString(URL_KEY) ?: return

        val source = sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? HttpSource ?: return
        val headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }
        setContent {
            NekoTheme {
                WebViewScreen(
                    title = title.toString(),
                    url = url,
                    headers = headers,
                    onShare = this::shareWebpage,
                    onOpenInBrowser = this::openInBrowser,
                    canOpenInApp = this::canOpenInApp,
                    onOpenInApp = this::openInApp,
                    onClose = { finish() },
                )
            }
        }
    }

    private fun shareWebpage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            toast(e.message)
        }
    }

    private fun openInBrowser(url: String) {
        openInBrowser(url, forceDefaultBrowser = true)
    }

    private fun openInApp(url: String) {
        openInBrowser(url, forceDefaultBrowser = false)
    }

    private fun canOpenInApp(url: String): Boolean {
        return url.contains("mangadex.org/manga/", true) || url.contains("mangadex.org/title/", true) || url.contains("mangadex.org/group", true)
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
    }
}
