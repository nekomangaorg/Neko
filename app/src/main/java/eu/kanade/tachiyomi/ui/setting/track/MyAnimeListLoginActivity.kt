package eu.kanade.tachiyomi.ui.setting.track

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.webview.BaseWebViewActivity
import eu.kanade.tachiyomi.util.system.WebViewClientCompat
import kotlinx.android.synthetic.main.webview_activity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

class MyAnimeListLoginActivity : BaseWebViewActivity() {

    private val trackManager: TrackManager by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        title = "MyAnimeList"

        webview.webViewClient = object : WebViewClientCompat() {
            override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Get CSRF token from HTML after post-login redirect
                if (url == MyAnimeListApi.baseUrl + "/") {
                    view?.evaluateJavascript(
                        "(function(){return document.querySelector('meta[name=csrf_token]').getAttribute('content')})();"
                    ) {
                        scope.launch {
                            withContext(Dispatchers.IO) { trackManager.myAnimeList.login(it.replace("\"", "")) }
                            returnToSettings()
                        }

                    }
                }
            }
        }
        webview.loadUrl(MyAnimeListApi.loginUrl().toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun returnToSettings() {
        finish()

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            val intent = Intent(context, MyAnimeListLoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }
    }
}
