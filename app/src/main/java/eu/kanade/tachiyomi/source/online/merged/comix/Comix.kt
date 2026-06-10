package eu.kanade.tachiyomi.source.online.merged.comix

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.lang.toDisplayMessage
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okio.Buffer
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class Comix : ReducedHttpSource() {

    override val name = Comix.name
    override val baseUrl = Comix.baseUrl
    private val descrambler = ComixDescrambler()
    private val apiUrl = "$baseUrl/api/v1"

    override val client =
        network.cloudFlareClient
            .newBuilder()
            .addNetworkInterceptor(descrambler.interceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                if (response.code != 404) return@addInterceptor response
                val url = request.url.toString()
                val fallbacks = listOf("/si/", "/i/", "/sii/", "/ii/")
                    .map { url.replaceFirst(SCRAMBLE_PATH_FALLBACK_REGEX, it) }
                    .filter { it != url }
                if (fallbacks.isEmpty()) return@addInterceptor response
                var lastResponse = response
                for (fallbackUrl in fallbacks) {
                    lastResponse.close()
                    lastResponse = chain.proceed(request.newBuilder().url(fallbackUrl).build())
                    if (lastResponse.code != 404) break
                }
                lastResponse
            }
            .rateLimit(5)
            .build()

    override val headers = Headers.Builder()
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)
        .add("Accept", "*/*")
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun searchManga(query: String): List<SManga> {
        val url = apiUrl.toHttpUrl().newBuilder()
            .addPathSegment("manga")
            .addQueryParameter("keyword", query)
            .addQueryParameter("order[relevance]", "desc")
            .addQueryParameter("limit", "50")
            .addQueryParameter("page", "1")
            .build()

        val response = client.newCall(GET(url.toString(), headers)).await()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        val res = json.decodeFromString<SearchResponse>(response.body.string())
        return res.result.items.map { it.toSManga() }
    }

    override suspend fun fetchChapters(
        mangaUrl: String,
    ): Result<List<SChapterStatusPair>, ResultError> {
        val mangaSlug = mangaUrl.removePrefix("/")

        try {
            val mangaPageUrl = getMangaUrl(mangaUrl)
            val response = client.newCall(GET(mangaPageUrl, headers)).await()
            if (!response.isSuccessful) {
                response.close()
                return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
            }
            val document = response.asJsoup()
            response.close()

            val payload = runInWebView(document) { interfaceName ->
                $$"""
                (function () {
                    const rewriteUrl = function (url) {
                        if (typeof url === 'string' && url.indexOf('/chapters') !== -1 && /[?&]limit=\d+/.test(url)) {
                            return url.replace(/([?&]limit=)\d+/, '$1100');
                        }
                        return url;
                    };
                    const originalOpen = XMLHttpRequest.prototype.open;
                    XMLHttpRequest.prototype.open = function (method, url) {
                        arguments[1] = rewriteUrl(url);
                        return originalOpen.apply(this, arguments);
                    };

                    if (JSON.parse.__comixChapterCaptureInstalled) return;
                    const originalParse = JSON.parse;
                    const seen = new Set();
                    const nextClicks = new Set();
                    const items = [];
                    let submitted = false;
                    const submit = function () {
                        if (submitted) return;
                        submitted = true;
                        window.$${interfaceName}.passPayload(JSON.stringify(items));
                    };

                    const proxiedParse = new Proxy(originalParse, {
                        apply(target, thisArg, args) {
                            const parsed = Reflect.apply(target, thisArg, args);
                            try {
                                if (
                                    !submitted &&
                                    parsed && parsed.result &&
                                    Array.isArray(parsed.result.items) &&
                                    parsed.result.items.length > 0 &&
                                    parsed.result.items[0] &&
                                    parsed.result.items[0].id !== undefined &&
                                    parsed.result.items[0].mangaId !== undefined
                                ) {
                                    const meta = parsed.result.meta || parsed.result.pagination;
                                    const page = (meta && meta.page) || 1;
                                    if (!seen.has(page)) {
                                        seen.add(page);
                                        for (const it of parsed.result.items) items.push(it);
                                        if (meta && meta.hasNext && !nextClicks.has(page)) {
                                            nextClicks.add(page);
                                            window.$${interfaceName}.resetTimer();
                                            let tries = 0;
                                            const iv = setInterval(function () {
                                                const btn = document.querySelector('.mchap-foot button[aria-label*=Next]');
                                                if (btn && !btn.disabled) {
                                                    btn.click();
                                                    clearInterval(iv);
                                                } else if (++tries > 50) {
                                                    clearInterval(iv);
                                                    submit();
                                                }
                                            }, 100);
                                        } else {
                                            submit();
                                        }
                                    }
                                }
                            } catch (e) {}
                            return parsed;
                        }
                    });
                    proxiedParse.__comixChapterCaptureInstalled = true;
                    JSON.parse = proxiedParse;
                })();
                """.trimIndent()
            }

            val allChapters = json.decodeFromString<List<Chapter>>(payload)
            return Ok(allChapters.map { it.toSChapter(mangaSlug) to false })
        } catch (e: Exception) {
            TimberKt.e(e) { "Error fetching chapters for Comix" }
            return Err(ResultError.Generic(e.toDisplayMessage()))
        }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val chapterUrl = "$baseUrl/${chapter.url}"

        val response = client.newCall(GET(chapterUrl, headers)).await()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        val document = response.asJsoup()
        response.close()

        val payload = runInWebView(document) { interfaceName ->
            """
            (function () {
                if (JSON.parse.__comixPageCaptureInstalled) return;
                const originalParse = JSON.parse;
                const proxiedParse = new Proxy(originalParse, {
                    apply(target, thisArg, args) {
                        const parsed = Reflect.apply(target, thisArg, args);
                        try {
                            if (parsed && parsed.result && parsed.result.pages) {
                                window.${interfaceName}.passPayload(args[0]);
                            }
                        } catch (e) {}
                        return parsed;
                    }
                });
                proxiedParse.__comixPageCaptureInstalled = true;
                JSON.parse = proxiedParse;
            })();
            """.trimIndent()
        }

        val pages = json.decodeFromString<ChapterResponse>(payload).result?.pages
        val base = pages?.baseUrl?.trimEnd('/')

        return pages?.items?.mapIndexed { index, img ->
            val isScrambled = img.s == 1 || (index + 1) % 4 == 0
            val full =
                if (img.url.startsWith("http")) img.url else "$base/${img.url.trimStart('/')}"
            val url = if (isScrambled) "$full#scrambled" else full
            Page(index, imageUrl = url)
        } ?: emptyList()
    }

    override fun imageRequest(page: Page): Request {
        val imageUrl = page.imageUrl ?: return super.imageRequest(page)
        val imageHost = imageUrl.substringBefore('#').toHttpUrlOrNull()?.host.orEmpty()
        val isScrambled = imageUrl.contains("#scrambled")
        val requestHeaders =
            if (imageHost.isNotEmpty() && !imageHost.contains("comix.to") && !isScrambled) {
                headers.newBuilder()
                    .removeAll("Origin")
                    .build()
            } else {
                headers
            }
        return GET(imageUrl, requestHeaders)
    }

    override fun getMangaUrl(url: String): String = "$baseUrl/title$url"

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return "$baseUrl/${simpleChapter.url}"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun runInWebView(
        document: org.jsoup.nodes.Document,
        buildScript: (interfaceName: String) -> String,
    ): String {
        val handler = Handler(Looper.getMainLooper())
        val jsInterface = WebViewPayloadInterface()
        val pool = ('a'..'z') + ('A'..'Z')
        val interfaceName = (1..(10..20).random())
            .map { pool.random() }
            .joinToString("")
        val script = buildScript(interfaceName)
        val emptyResponse = WebResourceResponse("text/plain", "utf-8", Buffer().inputStream())

        var webView: WebView? = null
        handler.post {
            val context = Injekt.get<Application>()
            val view = WebView(context)
            webView = view

            with(view.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                blockNetworkImage = true
                userAgentString = headers["User-Agent"]
            }
            view.addJavascriptInterface(jsInterface, interfaceName)

            view.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    val httpUrl = request.url?.toString()?.toHttpUrlOrNull()
                        ?: return super.shouldInterceptRequest(view, request)

                    return if (httpUrl.host.contains("comix.to") &&
                        (
                            httpUrl.encodedPath.contains(".js") ||
                                httpUrl.encodedPath.startsWith("/api/") ||
                                httpUrl.encodedPath.startsWith("/title/")
                            )
                    ) {
                        super.shouldInterceptRequest(view, request)
                    } else {
                        emptyResponse
                    }
                }

                override fun onPageStarted(
                    view: WebView,
                    url: String?,
                    favicon: android.graphics.Bitmap?,
                ) {
                    super.onPageStarted(view, url, favicon)
                    view.evaluateJavascript(script) {}
                }
            }

            view.loadDataWithBaseURL(
                document.location(),
                document.outerHtml(),
                "text/html",
                "utf-8",
                null,
            )
        }

        val completed = try {
            jsInterface.await(30, TimeUnit.SECONDS)
        } finally {
            handler.post { webView?.destroy() }
        }

        if (!completed) throw Exception("Timed out waiting for WebView payload")
        return jsInterface.payload ?: throw Exception("Failed to capture WebView payload")
    }

    private class WebViewPayloadInterface {
        private val signal = Semaphore(0)

        @Volatile
        var payload: String? = null
            private set

        @JavascriptInterface
        @Suppress("UNUSED")
        fun passPayload(data: String) {
            if (payload == null) {
                payload = data
                signal.release()
            }
        }

        @JavascriptInterface
        @Suppress("UNUSED")
        fun resetTimer() {
            signal.release()
        }

        fun await(timeout: Long, unit: TimeUnit): Boolean {
            while (payload == null) {
                if (!signal.tryAcquire(timeout, unit)) return false
            }
            return true
        }
    }

    companion object {
        const val name = "Comix"
        const val baseUrl = "https://comix.to"

        private val SCRAMBLE_PATH_FALLBACK_REGEX = Regex("/s?i+/")
    }
}
