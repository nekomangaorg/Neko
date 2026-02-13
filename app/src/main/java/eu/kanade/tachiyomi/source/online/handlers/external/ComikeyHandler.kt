package eu.kanade.tachiyomi.source.online.handlers.external

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource.Companion.USER_AGENT
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.interceptor.rateLimit
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ComikeyHandler {
    val baseUrl = "https://comikey.com"
    val headers =
        Headers.Builder().add("User-Agent", USER_AGENT).add("Referer", "$baseUrl/").build()

    val client: OkHttpClient by lazy {
        Injekt.get<NetworkHelper>().cloudFlareClient.newBuilder().rateLimit(3).build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    fun fetchPageList(externalUrl: String): List<Page> {
        val chapterUrl = externalUrl.substringAfter(baseUrl).substringBefore("?utm_source")
        return pageListParse(chapterUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun pageListParse(chapterUrl: String): List<Page> {
        val interfaceName = randomString()

        val handler = Handler(Looper.getMainLooper())
        val latch = CountDownLatch(1)
        val jsInterface = JsInterface(latch, json)
        var webView: WebView? = null

        handler.post {
            val innerWv = WebView(Injekt.get<Application>())

            webView = innerWv
            innerWv.settings.domStorageEnabled = true
            innerWv.settings.javaScriptEnabled = true
            innerWv.settings.blockNetworkImage = true
            // Security: Disable file and content access to prevent potential local file
            // exfiltration
            innerWv.settings.allowFileAccess = false
            innerWv.settings.allowContentAccess = false
            innerWv.settings.userAgentString = headers["User-Agent"]
            innerWv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            innerWv.addJavascriptInterface(jsInterface, interfaceName)

            // Somewhat useful if you need to debug WebView issues. Don't delete.
            //
            /* innerWv.webChromeClient =
            object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    if (consoleMessage == null) {
                        return false
                    }
                    val logContent =
                        "wv: ${consoleMessage.message()} (${consoleMessage.sourceId()}, line ${consoleMessage.lineNumber()})"
                    when (consoleMessage.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR ->
                            TimberKt.e { "comikey $logContent" }
                        ConsoleMessage.MessageLevel.LOG -> TimberKt.i { "comikey $logContent" }
                        ConsoleMessage.MessageLevel.TIP -> TimberKt.i { "comikey $logContent" }
                        ConsoleMessage.MessageLevel.WARNING ->
                            TimberKt.w { "comikey $logContent" }
                        else -> TimberKt.d { "comikey $logContent" }
                    }

                    return true
                }
            }*/

            innerWv.webViewClient =
                object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.evaluateJavascript(
                            webviewScript.replace("__interface__", interfaceName)
                        ) {}
                    }

                    // If you're logged in, the manifest URL sent to the client is not a direct
                    // link;
                    // it only redirects to the real one when you call it.
                    //
                    // In order to avoid a later call and remove an avenue for sniffing out users,
                    // we intercept said request so we can grab the real manifest URL.
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url ?: return super.shouldInterceptRequest(view, request)

                        if (
                            url.host != "relay-us.epub.rocks" ||
                                url.path?.endsWith("/manifest") != true
                        ) {
                            return super.shouldInterceptRequest(view, request)
                        }

                        val requestHeaders =
                            headers
                                .newBuilder()
                                .apply {
                                    request.requestHeaders.entries.forEach { set(it.key, it.value) }

                                    removeAll("X-Requested-With")
                                }
                                .build()
                        val response = client.newCall(GET(url.toString(), requestHeaders)).execute()

                        jsInterface.manifestUrl = response.request.url

                        return WebResourceResponse(
                            response.headers["Content-Type"]
                                ?: "application/divina+json+vnd.e4p.drm",
                            null,
                            response.code,
                            response.message,
                            response.headers.toMap(),
                            response.body.byteStream(),
                        )
                    }
                }

            innerWv.loadUrl(
                "$baseUrl$chapterUrl",
                buildMap {
                    putAll(headers.toMap())
                    put("X-Requested-With", randomString())
                },
            )
        }

        latch.await(30, TimeUnit.SECONDS)
        handler.post { webView?.destroy() }

        if (latch.count == 1L) {
            throw Exception("Timed out decrypting image links")
        }

        if (jsInterface.error.isNotEmpty()) {
            throw Exception(jsInterface.error)
        }

        val manifestUrl = jsInterface.manifestUrl!!
        val manifest = jsInterface.manifest!!
        val webtoon = manifest.metadata.readingProgression == "ttb"

        return manifest.readingOrder.mapIndexed { i, it ->
            val url =
                manifestUrl
                    .newBuilder()
                    .apply {
                        removePathSegment(manifestUrl.pathSize - 1)

                        if (
                            it.alternate.isNotEmpty() &&
                                it.height == 2048 &&
                                it.type == "image/jpeg"
                        ) {
                            addPathSegments(
                                it.alternate
                                    .first {
                                        val dimension = if (webtoon) it.width else it.height

                                        dimension <= 1536 && it.type == "image/webp"
                                    }
                                    .href
                            )
                        } else {
                            addPathSegments(it.href)
                        }

                        addQueryParameter("act", jsInterface.act)
                    }
                    .toString()

            Page(i, imageUrl = url)
        }
    }

    private val webviewScript =
        """
            document.addEventListener("DOMContentLoaded", (e) => {
                // This is intentional. Simply binding `_` to `window.__interface__.gettext` will
                // throw an error: "Java bridge method can't be invoked on a non-injected object".
                const _ = (key) => window.__interface__.gettext(key);

                if (document.querySelector("#unlock-full")) {
                    window.__interface__.passError(_("error_locked_chapter_unlock_in_webview"));
                }
            });

            document.addEventListener(
                "you-right-now:reeeeeee",
                async (e) => {
                    const _ = (key) => window.__interface__.gettext(key);

                    try {
                        const db = await new Promise((resolve, reject) => {
                            const request = indexedDB.open("firebase-app-check-database");

                            request.onsuccess = (event) => resolve(event.target.result);
                            request.onerror = (event) => reject(event.target);
                        });

                        const act = await new Promise((resolve, reject) => {
                            db.onerror = (event) => reject(event.target);

                            const request = db.transaction("firebase-app-check-store").objectStore("firebase-app-check-store").getAll();

                            request.onsuccess = (event) => {
                                const entries = event.target.result;
                                db.close();

                                if (entries.length < 1) {
                                    window.__interface__.passError("Open chapter in WebView, then try again token not found.");
                                }

                                const value = entries[0].value;

                                if (value.expireTimeMillis < Date.now()) {
                                    window.__interface__.passError("Open chapter in WebView, then try again token expired.");
                                }

                                resolve(value.token)
                            }
                        });

                        const manifest = JSON.parse(document.querySelector("#lmao-init").textContent).manifest;
                        window.__interface__.passPayload(manifest, act, await e.detail);
                    } catch (e) {
                        window.__interface__.passError("Unknown error");
                    }
                },
                { once: true },
            );
        """

    private fun randomString(): String {
        val length = (10..20).random()

        return buildString(length) {
            val charPool = ('a'..'z') + ('A'..'Z')

            for (i in 0 until length) {
                append(charPool.random())
            }
        }
    }

    private class JsInterface(private val latch: CountDownLatch, private val json: Json) {
        var manifest: ComikeyEpisodeManifest? = null
            private set

        var manifestUrl: HttpUrl? = null

        var act: String = ""
            private set

        var error: String = ""
            private set

        @JavascriptInterface
        @Suppress("UNUSED")
        fun passError(msg: String) {
            error = msg
            latch.countDown()
        }

        @JavascriptInterface
        @Suppress("UNUSED")
        fun passPayload(manifestUrl: String, act: String, rawData: String) {
            if (this.manifestUrl == null) {
                this.manifestUrl = manifestUrl.toHttpUrl()
            }

            this.act = act
            manifest = json.decodeFromString<ComikeyEpisodeManifest>(rawData)

            latch.countDown()
        }
    }

    @Serializable
    data class ComikeyEpisodeManifest(
        val metadata: ComikeyEpisodeManifestMetadata,
        val readingOrder: List<ComikeyPage>,
    )

    @Serializable data class ComikeyEpisodeManifestMetadata(val readingProgression: String)

    @Serializable
    data class ComikeyPage(
        val href: String,
        val type: String,
        val height: Int,
        val width: Int,
        val alternate: List<ComikeyAlternatePage>,
    )

    @Serializable
    data class ComikeyAlternatePage(
        val href: String,
        val type: String,
        val height: Int,
        val width: Int,
    )
}
