package org.nekomanga.presentation.screens

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewNavigator
import com.kevinnzou.web.rememberWebViewState
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.screens.webview.WebviewTopBar
import tachiyomi.core.util.system.WebViewUtil
import tachiyomi.core.util.system.setDefaultSettings

@Composable
fun WebViewScreen(title: String, url: String, onBackPressed: () -> Unit) {

    val context = LocalContext.current

    val share = stringResource(R.string.share)

    if (!WebViewUtil.supportsWebView(context)) {
        context.toast(R.string.information_webview_required, Toast.LENGTH_LONG)
        onBackPressed()
    }

    WebViewWrapper(
        title = title,
        url = url,
        onShare = { url ->
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
            context.startActivity(Intent.createChooser(intent, share))
        },
        onOpenInBrowser = { urlToOpen ->
            context.openInBrowser(urlToOpen, forceDefaultBrowser = true)
        },
        canOpenInApp = { urlToCheck ->
            urlToCheck.contains("mangadex.org/manga/", true) ||
                urlToCheck.contains("mangadex.org/title/", true) ||
                urlToCheck.contains("mangadex.org/group", true)
        },
        onOpenInApp = { urlToOpen ->
            context.openInBrowser(urlToOpen, forceDefaultBrowser = false)
        },
        onClose = onBackPressed,
    )
}

@Composable
fun WebViewWrapper(
    title: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    onShare: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit,
    canOpenInApp: (String) -> Boolean,
    onOpenInApp: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberWebViewState(url = url)

    var currentUrl by remember { mutableStateOf(url) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(state = rememberTopAppBarState())

    val navigator = rememberWebViewNavigator()

    ChildScreenScaffold(
        scrollBehavior = scrollBehavior,
        topBar = {
            WebviewTopBar(
                state = state,
                navigator = navigator,
                title = title,
                subtitle = currentUrl,
                onNavigationIconClicked = onClose,
                scrollBehavior = scrollBehavior,
                onShare = onShare,
                onOpenInBrowser = onOpenInBrowser,
                canOpenInApp = canOpenInApp,
                onOpenInApp = onOpenInApp,
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            val loadingState = state.loadingState
            if (loadingState is LoadingState.Loading) {
                LinearProgressIndicator(
                    progress = { loadingState.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val webClient = remember {
                object : AccompanistWebViewClient() {

                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { currentUrl = it }
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String?,
                        isReload: Boolean,
                    ) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        url?.let { currentUrl = it }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        request?.let { view?.loadUrl(it.url.toString(), headers) }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }
            }
            WebView(
                state = state,
                modifier = Modifier.fillMaxSize(),
                navigator = navigator,
                onCreated = { webView ->
                    webView.setDefaultSettings()

                    // Debug mode (chrome://inspect/#devices)
                    if (
                        BuildConfig.DEBUG &&
                            0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
                    ) {
                        WebView.setWebContentsDebuggingEnabled(true)
                    }

                    headers["user-agent"]?.let { webView.settings.userAgentString = it }
                },
                client = webClient,
                onDispose = { webview -> webview.destroy() },
            )
        }
    }
}
