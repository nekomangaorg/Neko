package tachiyomi.core.util.system

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.net.InetAddress
import tachiyomi.core.network.interceptors.isPrivate

open class SecureWebViewClient : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? = secureShouldInterceptRequest(view, request)
}

fun secureShouldInterceptRequest(
    view: WebView?,
    request: WebResourceRequest?,
): WebResourceResponse? {
    val url = request?.url ?: return null
    val host = url.host ?: return null

    if (url.scheme == "http") {
        val addresses = InetAddress.getAllByName(host)
        val allPrivate = addresses.all { it.isPrivate() }

        if (!allPrivate) {
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                403,
                "Forbidden",
                mapOf(),
                ByteArrayInputStream("Blocked by policy".toByteArray()),
            )
        }
    }

    return null
}
