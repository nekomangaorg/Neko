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

    override val client =
        network.cloudFlareClient
            .newBuilder()
            .addInterceptor(ComixDescrambler.interceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                if (response.code != 404) return@addInterceptor response
                val url = request.url.toString()
                val fallbacks = listOf("/i5/", "/si/", "/i/", "/sii/", "/ii/")
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
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("browse")
            .addQueryParameter("q", query.trim())
            .addQueryParameter("sort", "relevance:desc")
            .addQueryParameter("page", "1")
            .build()

        val response = client.newCall(GET(url.toString(), headers)).await()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }
        return fetchMangaListFromBrowse(Request.Builder().url(url).headers(headers).build(), query.trim())
    }

    private suspend fun fetchMangaListFromBrowse(request: Request, expectedKeyword: String = ""): List<SManga> {
        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        val document = response.asJsoup()
        response.close()

        // 1. Try static extraction first via script#initial-data
        val searchResponse = extractBrowseResponse(document) ?: run {
            // 2. Fall back to WebView interception if static parsing fails
            val encodedKeyword = org.json.JSONObject.quote(expectedKeyword)
            val payload = runInWebView(
                document = document,
                buildScript = { interfaceName ->
                    """
                (function () {
                    const payloadKey = '__comixBrowsePayload';
                    const expectedKeyword = $encodedKeyword;
                    const capture = (parsed, allowEmpty = false) => {
                        try {
                            if (parsed && Array.isArray(parsed.items)) {
                                parsed = { result: parsed };
                            }
                            if (
                                parsed &&
                                parsed.result &&
                                Array.isArray(parsed.result.items) &&
                                (allowEmpty || parsed.result.items.length > 0)
                            ) {
                                window[payloadKey] = JSON.stringify(parsed);
                                window.$interfaceName.passPayload(window[payloadKey]);
                                return true;
                            }
                        } catch (e) {}
                        return false;
                    };

                    if (window[payloadKey]) return window[payloadKey];

                    try {
                        const raw = document.querySelector('script#initial-data')?.textContent;
                        const queries = raw && JSON.parse(raw).queries;
                        if (queries) Object.values(queries).some(capture);
                    } catch (e) {}

                    if (window[payloadKey]) return window[payloadKey];
                    if (window.__comixBrowseCaptureInstalled) return null;
                    window.__comixBrowseCaptureInstalled = true;

                    const captureText = text => {
                        try {
                            if (text) capture(JSON.parse(text), true);
                        } catch (e) {}
                    };

                    const shouldCaptureUrl = rawUrl => {
                        try {
                            const url = new URL(rawUrl || '', window.location.origin);
                            if (!url.pathname.includes('/api/v1/manga')) return false;
                            if (!expectedKeyword) return true;
                            return url.searchParams.get('keyword') === expectedKeyword || url.searchParams.get('q') === expectedKeyword;
                        } catch (e) {
                            return false;
                        }
                    };

                    const originalFetch = window.fetch;
                    if (typeof originalFetch === 'function') {
                        window.fetch = function () {
                            return originalFetch.apply(this, arguments).then(response => {
                                try {
                                    const url = response && response.url || '';
                                    if (shouldCaptureUrl(url)) {
                                        response.clone().text().then(captureText).catch(() => {});
                                    }
                                } catch (e) {}
                                return response;
                            });
                        };
                    }

                    const originalOpen = XMLHttpRequest.prototype.open;
                    const originalSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.open = function (method, url) {
                        this.__comixBrowseUrl = String(url || '');
                        return originalOpen.apply(this, arguments);
                    };
                    XMLHttpRequest.prototype.send = function () {
                        this.addEventListener('load', function () {
                            try {
                                if (shouldCaptureUrl(this.__comixBrowseUrl)) {
                                    captureText(this.responseText);
                                }
                            } catch (e) {}
                        });
                        return originalSend.apply(this, arguments);
                    };

                    const originalParse = JSON.parse;
                    const proxiedParse = new Proxy(originalParse, {
                        apply(target, thisArg, args) {
                            const parsed = Reflect.apply(target, thisArg, args);
                            if (!expectedKeyword) capture(parsed);
                            return parsed;
                        }
                    });
                    JSON.parse = proxiedParse;
                    return window[payloadKey] || null;
                })();
                """.trimIndent()
                }
            )
            json.decodeFromString<SearchResponse>(payload)
        }

        return searchResponse.result?.items?.map { it.toSManga() } ?: emptyList()
    }

    /**
     * Extracts pre-rendered SSR query data directly from static HTML DOM
     */
    private fun extractBrowseResponse(document: org.jsoup.nodes.Document): SearchResponse? {
        val initialData = document.selectFirst("script#initial-data")?.data() ?: return null
        return runCatching {
            val root = json.parseToJsonElement(initialData) as? kotlinx.serialization.json.JsonObject ?: return null
            val queries = root["queries"] as? kotlinx.serialization.json.JsonObject ?: return null

            queries.values.firstNotNullOfOrNull { value ->
                runCatching { json.decodeFromJsonElement(SearchResponse.serializer(), value) }
                    .getOrNull()
                    .takeIf { (it?.result?.items?.size ?: 0) > 0 }
            }
        }.getOrNull()
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
                """
            (function () {
                const payloadKey = '__comixChapterPayload';
                if (window[payloadKey]?.installed) return null;

                const state = window[payloadKey] = {
                    installed: true,
                    submitted: false,
                    seen: new Set(),
                    nextClicks: new Set(),
                    items: []
                };

                const submit = () => {
                    if (state.submitted) return;
                    state.submitted = true;
                    window.$interfaceName.passPayload(JSON.stringify(state.items));
                };

                const findNextButton = page => {
                    const buttons = [...document.querySelectorAll('.mchap-foot button')]
                        .filter(button => !button.disabled);
                    return buttons.find(button => {
                        const label = [
                            button.getAttribute('aria-label'),
                            button.getAttribute('title'),
                            button.textContent
                        ].filter(Boolean).join(' ');
                        return /\bnext\b/i.test(label);
                    }) || buttons.find(button => Number(button.textContent?.trim()) === page + 1);
                };

                const capture = parsed => {
                    try {
                        const items = parsed?.result?.items;
                        const first = items?.[0];
                        if (
                            state.submitted ||
                            !Array.isArray(items) ||
                            items.length === 0 ||
                            first?.id === undefined ||
                            first?.number === undefined
                        ) return false;

                        const meta = parsed.result.meta || parsed.result.pagination || {};
                        const page = meta.page || 1;
                        const lastPage = meta.lastPage || meta.last_page || page;
                        const hasNext = meta.hasNext || page < lastPage;

                        if (state.seen.has(page)) return true;

                        state.seen.add(page);
                        state.items.push(...items);

                        if (hasNext && !state.nextClicks.has(page)) {
                            state.nextClicks.add(page);
                            window.$interfaceName.resetTimer();
                            let tries = 0;
                            const interval = setInterval(() => {
                                const button = findNextButton(page);
                                if (button) {
                                    button.click();
                                    clearInterval(interval);
                                } else if (++tries > 50) {
                                    clearInterval(interval);
                                    submit();
                                }
                            }, 100);
                        } else {
                            submit();
                        }
                        return true;
                    } catch (e) {
                        return false;
                    }
                };

                const originalParse = JSON.parse;
                const proxiedParse = new Proxy(originalParse, {
                    apply(target, thisArg, args) {
                        const parsed = Reflect.apply(target, thisArg, args);
                        capture(parsed);
                        return parsed;
                    }
                });
                proxiedParse.__comixChapterCaptureInstalled = true;
                JSON.parse = proxiedParse;

                try {
                    const raw = document.querySelector('script#initial-data')?.textContent;
                    const queries = raw && originalParse(raw).queries;
                    if (queries) Object.values(queries).some(capture);
                } catch (e) {}

                return null;
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
            const payloadKey = '__comixPagePayload';
            const capture = parsed => {
                try {
                    if (parsed && parsed.result && parsed.result.pages) {
                        window[payloadKey] = JSON.stringify(parsed);
                        window.$interfaceName.passPayload(window[payloadKey]);
                        return true;
                    }
                } catch (e) {}
                return false;
            };

            if (window[payloadKey]) return window[payloadKey];

            try {
                const raw = document.querySelector('script#initial-data')?.textContent;
                const queries = raw && JSON.parse(raw).queries;
                if (queries) Object.values(queries).some(capture);
            } catch (e) {}

            if (window[payloadKey]) return window[payloadKey];
            if (JSON.parse.__comixPageCaptureInstalled) return null;

            const originalParse = JSON.parse;
            const proxiedParse = new Proxy(originalParse, {
                apply(target, thisArg, args) {
                    const parsed = Reflect.apply(target, thisArg, args);
                    capture(parsed);
                    return parsed;
                }
            });
            proxiedParse.__comixPageCaptureInstalled = true;
            JSON.parse = proxiedParse;
            return window[payloadKey] || null;
        })();
        """.trimIndent()
        }

        val pages = json.decodeFromString<ChapterResponse>(payload).result?.pages
        val base = pages?.baseUrl?.trimEnd('/') ?: return emptyList()

        return pages.items.mapIndexed { index, img ->
            val full = if (img.url.startsWith("http")) img.url else "$base/${img.url.trimStart('/')}"

            // V3 grid-scramble vs. Legacy byte-XOR detection
            val isV3 = img.s == 1 || full.contains("?v3")
            val isLegacyScramble = !isV3 && (index + 1) % 4 == 0

            val url = when {
                isV3 -> full.toHttpUrl().newBuilder().apply {
                    if (!full.toHttpUrl().queryParameterNames.contains("v3")) {
                        addQueryParameter("v3", null)
                    }
                }.build().toString()
                isLegacyScramble -> "$full#scrambled"
                else -> full
            }

            Page(index, imageUrl = url)
        }
    }

    override fun imageRequest(page: Page): Request {
        val imageUrl = page.imageUrl ?: return super.imageRequest(page)
        val urlWithoutFragment = imageUrl.substringBefore('#')
        val imageHost = urlWithoutFragment.toHttpUrlOrNull()?.host.orEmpty()
        val isScrambled = imageUrl.contains("#scrambled")
        val isV3 = urlWithoutFragment.toHttpUrlOrNull()?.queryParameterNames?.contains("v3") == true
        val isLegacyScramble = isScrambled && !isV3
        val baseUrlHost = baseUrl.toHttpUrl().host

        val requestHeaders = if (
            imageHost.isNotEmpty() &&
            !imageHost.endsWith(baseUrlHost) &&
            !isLegacyScramble
        ) {
            headers.newBuilder()
                .removeAll("Origin")
                .build()
        } else {
            headers
        }

        return GET(urlWithoutFragment, requestHeaders)
    }

    override fun getMangaUrl(url: String): String = "$baseUrl/title$url"

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return "$baseUrl/${simpleChapter.url}"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun runInWebView(
        document: org.jsoup.nodes.Document,
        initializationScript: String? = null,
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
        val active = java.util.concurrent.atomic.AtomicBoolean(true)
        val started = Semaphore(0)
        val startupError = java.util.concurrent.atomic.AtomicReference<Throwable?>()

        var webView: WebView? = null
        var injectScript: Runnable? = null
        var lastUrl = document.location()

        handler.post {
            try {
                if (!active.get()) return@post

                val context = Injekt.get<Application>()
                val view = WebView(context)
                webView = view

                with(view.settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    blockNetworkImage = false
                    userAgentString = headers["User-Agent"]
                }

                android.webkit.CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(view, true)
                }

                view.addJavascriptInterface(jsInterface, interfaceName)

                view.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val requestUrl = request.url?.toString()?.toHttpUrlOrNull()
                            ?: return super.shouldInterceptRequest(view, request)

                        val baseUrlHost = baseUrl.toHttpUrl().host
                        val allowedHost = requestUrl.host.endsWith(baseUrlHost) ||
                            requestUrl.host.endsWith(".comix.to") ||
                            requestUrl.host == "comix.to" ||
                            requestUrl.host == "comix.ws" ||
                            requestUrl.host == "challenges.cloudflare.com"

                        if (!allowedHost) return emptyResponse
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (url != null) lastUrl = url
                        if (active.get() && jsInterface.payload == null) {
                            runCatching { view.evaluateJavascript(script, null) }
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        if (url != null) lastUrl = url
                        if (active.get() && jsInterface.payload == null) {
                            runCatching { view.evaluateJavascript(script, null) }
                        }
                    }
                }

                val retry = object : Runnable {
                    override fun run() {
                        if (!active.get() || jsInterface.payload != null) return
                        runCatching { view.evaluateJavascript(script, null) }
                        if (active.get() && jsInterface.payload == null) {
                            handler.postDelayed(this, 100L)
                        }
                    }
                }
                injectScript = retry

                val html = document.clone().apply {
                    initializationScript?.let {
                        head().prependElement("script").append(it)
                    }
                }.outerHtml()

                view.loadDataWithBaseURL(
                    document.location(),
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
                handler.post(retry)
            } catch (error: Throwable) {
                startupError.set(error)
            } finally {
                started.release()
            }
        }

        val completed = try {
            if (!started.tryAcquire(120L, TimeUnit.SECONDS)) {
                throw Exception("Timed out starting WebView (url=$lastUrl)")
            }
            startupError.get()?.let {
                throw Exception("Failed to start WebView (url=$lastUrl)", it)
            }
            jsInterface.await(90L, TimeUnit.SECONDS)
        } finally {
            active.set(false)
            handler.post {
                injectScript?.let(handler::removeCallbacks)
                val view = webView
                webView = null
                runCatching { view?.stopLoading() }
                runCatching { view?.destroy() }
            }
        }

        if (!completed) {
            throw Exception("Timed out waiting for WebView payload (url=$lastUrl)")
        }
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

        private val SCRAMBLE_PATH_FALLBACK_REGEX = Regex("/(?:i5|s?i+)/")
    }
}
