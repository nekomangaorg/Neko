package org.nekomanga.presentation.screens

import android.content.pm.ApplicationInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.NekoScaffold

@Composable
fun WebViewScreen(
    title: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    onShare: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberWebViewState(url = url)
    val navigator = rememberWebViewNavigator()

    NekoScaffold(
        title = title,
        onNavigationIconClicked = onClose,
        navigationIcon = Icons.Filled.Close,
        navigationIconLabel = stringResource(id = R.string.close),
        subtitle = url,
        actions = {
            AppBarActions(
                actions = listOf(
                    if (navigator.canGoBack) {
                        AppBar.Action(
                            title = stringResource(id = R.string.back),
                            icon = Icons.Filled.ArrowBack,
                            onClick = {
                                navigator.navigateBack()
                            },
                        )
                    } else {
                        AppBar.Empty
                    },
                ) + listOf(
                    if (navigator.canGoForward) {
                        AppBar.Action(
                            title = stringResource(R.string.forward),
                            icon = Icons.Default.ArrowForward,
                            onClick = {
                                navigator.navigateForward()
                            },
                        )
                    } else {
                        AppBar.Empty
                    },
                ) + listOf(
                    AppBar.OverflowAction(
                        title = stringResource(R.string.refresh),
                        onClick = { navigator.reload() },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(R.string.share),
                        onClick = { onShare(state.content.getCurrentUrl()!!) },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(R.string.open_in_browser),
                        onClick = { onOpenInBrowser(state.content.getCurrentUrl()!!) },
                    ),
                ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            val loadingState = state.loadingState
            if (loadingState is LoadingState.Loading) {
                LinearProgressIndicator(
                    progress = loadingState.progress,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }

            val webClient = remember {
                object : AccompanistWebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        request?.let {
                            view?.loadUrl(it.url.toString(), headers)
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }
            }
            WebView(
                state = state,
                navigator = navigator,
                onCreated = { webView ->
                    webView.setDefaultSettings()

                    // Debug mode (chrome://inspect/#devices)
                    if (BuildConfig.DEBUG && 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                        WebView.setWebContentsDebuggingEnabled(true)
                    }

                    headers["User-Agent"]?.let {
                        webView.settings.userAgentString = it
                    }
                },
                client = webClient,
            )
        }
    }
}
