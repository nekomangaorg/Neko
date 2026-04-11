package tachiyomi.core.util.system

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.net.InetAddress
import tachiyomi.core.network.interceptors.isIpLiteral
import tachiyomi.core.network.interceptors.isPrivate

open class SecureWebViewClient : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? = secureShouldInterceptRequest(request)

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
        shouldOverrideUrlLoading(request)
}

fun secureShouldInterceptRequest(request: WebResourceRequest?): WebResourceResponse? {
    val url = request?.url ?: return null
    val host = url.host ?: return null

    if (url.scheme == "http") {
        val allPrivate =
            runCatching { InetAddress.getAllByName(host).all { it.isPrivate() } }.getOrNull()
                ?: return null

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

fun shouldOverrideUrlLoading(request: WebResourceRequest?): Boolean {
    val url = request?.url ?: return false
    val host = url.host ?: return true

    if (url.scheme == "http") {
        if (host.isIpLiteral()) {
            val address = InetAddress.getByName(host) // getAllByName blocks
            if (!address.isPrivate()) return true
        }
    }

    return false
}
