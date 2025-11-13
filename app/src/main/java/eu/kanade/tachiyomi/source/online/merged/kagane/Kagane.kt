package eu.kanade.tachiyomi.source.online.merged.kagane

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.extension.en.kagane.ChallengeDto
import eu.kanade.tachiyomi.extension.en.kagane.ChapterDto
import eu.kanade.tachiyomi.extension.en.kagane.DetailsDto
import eu.kanade.tachiyomi.extension.en.kagane.ImageInterceptor
import eu.kanade.tachiyomi.extension.en.kagane.SearchDto
import eu.kanade.tachiyomi.extension.en.kagane.sha256
import eu.kanade.tachiyomi.extension.en.kagane.toBase64
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await
import tachiyomi.core.network.parseAs
import tachiyomi.core.network.toJsonString
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class Kagane : ReducedHttpSource() {

    override val name = Kagane.name
    override val baseUrl = Kagane.baseUrl

    override val headers =
        Headers.Builder()
            .apply {
                add("Origin", baseUrl)
                add("Referer", "$baseUrl/")
            }
            .build()

    private val json: Json by injectLazy()

    override val client =
        network.cloudFlareClient
            .newBuilder()
            .addInterceptor(ImageInterceptor())
            .addInterceptor(::refreshTokenInterceptor)
            .rateLimit(2)
            .build()

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        var (seriesId, chapterId, pageCount) = simpleChapter.url.split(";")
        return baseUrl
            .toHttpUrl()
            .newBuilder()
            .addPathSegment("series")
            .addPathSegment(seriesId)
            .addPathSegment("reader")
            .addPathSegment(chapterId)
            .build()
            .toString()
    }

    private fun refreshTokenInterceptor(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        if (!url.queryParameterNames.contains("token")) {
            return chain.proceed(request)
        }

        val seriesId = url.pathSegments[3]
        val chapterId = url.pathSegments[5]

        var response =
            chain.proceed(
                request
                    .newBuilder()
                    .url(url.newBuilder().setQueryParameter("token", accessToken).build())
                    .build()
            )
        if (response.code == 401) {
            response.close()
            val challenge =
                try {
                    getChallengeResponse(seriesId, chapterId)
                } catch (_: Exception) {
                    throw IOException("Failed to retrieve token")
                }
            accessToken = challenge.accessToken
            cacheUrl = challenge.cacheUrl
            response =
                chain.proceed(
                    request
                        .newBuilder()
                        .url(url.newBuilder().setQueryParameter("token", accessToken).build())
                        .build()
                )
        }

        return response
    }

    override suspend fun searchManga(query: String): List<SManga> {
        val url =
            "$apiUrl/api/v1/search"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("page", 0.toString())
                .addQueryParameter("size", 35.toString())
                .addQueryParameter("name", query)
                .build()
                .toString()

        val body = "{}".toRequestBody("application/json".toMediaType())

        val response = client.newCall(POST(url, headers, body = body)).await()

        return parseSearchManga(response)
    }

    private fun parseSearchManga(response: Response): List<SManga> {
        val dto = with(json) { response.parseAs<SearchDto>() }
        val mangaList = dto.content.map { it.toSManga(apiUrl) }
        return mangaList
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        val url =
            (apiUrl)
                .toHttpUrl()
                .newBuilder()
                .addPathSegment("api")
                .addPathSegment("v1")
                .addPathSegment("books")
                .addPathSegment(mangaUrl)
                .build()
                .toString()
        val response = client.newCall(GET(url, headers)).await()

        if (!response.isSuccessful) {
            response.close()
            return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
        }

        return parseChapters(response)
    }

    private fun mangaDetailsRequest(seriesId: String): Request {
        return GET("$apiUrl/api/v1/series/$seriesId", headers)
    }

    private fun parseChapters(response: Response): Result<List<SChapterStatusPair>, ResultError> {

        val seriesId = response.request.url.toString().substringAfterLast("/")

        val dto = with(json) { response.parseAs<ChapterDto>() }

        val source =
            runCatching {
                    with(json) {
                        client
                            .newCall(mangaDetailsRequest(seriesId))
                            .execute()
                            .parseAs<DetailsDto>()
                            .source
                    }
                }
                .getOrDefault("")
        val useSourceChapterNumber =
            source in setOf("Dark Horse Comics", "Flame Comics", "MangaDex", "Square Enix Manga")

        return Ok(
            dto.content
                .map { it ->
                    it.toSChapter(
                        useSourceChapterNumber = useSourceChapterNumber,
                        source = source,
                    ) to false
                }
                .reversed()
        )
    }

    private fun getCertificate(): String {
        return client
            .newCall(GET("$apiUrl/api/v1/static/bin.bin", headers))
            .execute()
            .body
            .bytes()
            .toBase64()
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        if (chapter.url.count { it == ';' } != 2)
            throw Exception("Chapter url error, please refresh chapter list.")
        var (seriesId, chapterId, pageCount) = chapter.url.split(";")

        val challengeResp = getChallengeResponse(seriesId, chapterId)
        accessToken = challengeResp.accessToken
        cacheUrl = challengeResp.cacheUrl

        val pages =
            (0 until pageCount.toInt()).map { page ->
                val pageUrl =
                    "$cacheUrl/api/v1/books"
                        .toHttpUrl()
                        .newBuilder()
                        .apply {
                            addPathSegment(seriesId)
                            addPathSegment("file")
                            addPathSegment(chapterId)
                            addPathSegment((page + 1).toString())
                            addQueryParameter("token", accessToken)
                        }
                        .build()
                        .toString()

                Page(page, imageUrl = pageUrl)
            }

        return pages
    }

    private var cacheUrl = "https://kazana.$domain"
    private var accessToken: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    private fun getChallengeResponse(seriesId: String, chapterId: String): ChallengeDto {
        val f = "$seriesId:$chapterId".sha256().sliceArray(0 until 16)

        val interfaceName = "jsInterface"
        val html =
            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Title</title>
            </head>
            <body>
                <script>
                    function base64ToArrayBuffer(base64) {
                        var binaryString = atob(base64);
                        var bytes = new Uint8Array(binaryString.length);
                        for (var i = 0; i < binaryString.length; i++) {
                            bytes[i] = binaryString.charCodeAt(i);
                        }
                        return bytes.buffer;
                    }

                    async function getData() {
                        const g = base64ToArrayBuffer("${getCertificate()}");
                        let t = await navigator.requestMediaKeySystemAccess("com.widevine.alpha", [{
                          initDataTypes: ["cenc"],
                          audioCapabilities: [],
                          videoCapabilities: [{
                            contentType: 'video/mp4; codecs="avc1.42E01E"'
                          }]
                        }]);

                        let e = await t.createMediaKeys();
                        await e.setServerCertificate(g);
                        let n = e.createSession();
                        let i = new Promise((resolve, reject) => {
                          function onMessage(event) {
                            n.removeEventListener("message", onMessage);
                            resolve(event.message);
                          }

                          function onError() {
                            n.removeEventListener("error", onError);
                            reject(new Error("Failed to generate license challenge"));
                          }

                          n.addEventListener("message", onMessage);
                          n.addEventListener("error", onError);
                        });

                        await n.generateRequest("cenc", base64ToArrayBuffer("${getPssh(f).toBase64()}"));
                        let o = await i;
                        let m = new Uint8Array(o);
                        let v = btoa(String.fromCharCode(...m));
                        window.$interfaceName.passPayload(v);
                    }
                    getData();
                </script>
            </body>
            </html>
        """
                .trimIndent()

        val handler = Handler(Looper.getMainLooper())
        val latch = CountDownLatch(1)
        val jsInterface = JsInterface(latch)
        var webView: WebView? = null

        handler.post {
            val innerWv = WebView(Injekt.get<Application>())

            webView = innerWv
            innerWv.settings.domStorageEnabled = true
            innerWv.settings.javaScriptEnabled = true
            innerWv.settings.blockNetworkImage = true
            innerWv.settings.userAgentString = headers["User-Agent"]
            innerWv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            innerWv.addJavascriptInterface(jsInterface, interfaceName)

            innerWv.webChromeClient =
                object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (
                            request
                                ?.resources
                                ?.contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID) == true
                        ) {
                            request.grant(request.resources)
                        } else {
                            super.onPermissionRequest(request)
                        }
                    }
                }

            innerWv.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }

        latch.await(10, TimeUnit.SECONDS)
        handler.post { webView?.destroy() }

        if (latch.count == 1L) {
            throw Exception("Timed out getting drm challenge")
        }

        if (jsInterface.challenge.isEmpty()) {
            throw Exception("Failed to get drm challenge")
        }

        val challengeUrl =
            "$apiUrl/api/v1/books/$seriesId/file/$chapterId".toHttpUrl().newBuilder().build()
        val challengeBody =
            with(json) {
                buildJsonObject { put("challenge", jsInterface.challenge) }
                    .toJsonString()
                    .toRequestBody("application/json".toMediaType())
            }

        return with(json) {
            client
                .newCall(POST(challengeUrl.toString(), headers, challengeBody))
                .execute()
                .parseAs<ChallengeDto>()
        }
    }

    private fun concat(vararg arrays: ByteArray): ByteArray =
        arrays.reduce { acc, bytes -> acc + bytes }

    private fun getPssh(t: ByteArray): ByteArray {
        val e = Base64.decode("7e+LqXnWSs6jyCfc1R0h7Q==", Base64.DEFAULT)
        val zeroes = ByteArray(4)

        val i = byteArrayOf(18, t.size.toByte()) + t
        val s = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i.size).array()

        val innerBox = concat(zeroes, e, s, i)
        val outerSize =
            ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(innerBox.size + 8).array()
        val psshHeader = "pssh".toByteArray(StandardCharsets.UTF_8)

        return concat(outerSize, psshHeader, innerBox)
    }

    internal class JsInterface(private val latch: CountDownLatch) {
        var challenge: String = ""

        @JavascriptInterface
        @Suppress("UNUSED")
        fun passPayload(rawData: String) {
            try {
                challenge = rawData
                latch.countDown()
            } catch (_: Exception) {
                return
            }
        }
    }

    // ============================= Utilities ==============================

    companion object {
        const val name = "Kagane"
        const val domain = "kagane.org"
        const val apiUrl = "https://api.$domain"
        const val baseUrl = "https://$domain"

        private const val CONTENT_RATING = "pref_content_rating"
        private const val CONTENT_RATING_DEFAULT = "pornographic"
        internal val CONTENT_RATINGS = arrayOf("safe", "suggestive", "erotica", "pornographic")

        private const val DATA_SAVER = "data_saver_default"
    }

    // ============================= Filters ==============================

    private val metadataClient =
        client
            .newBuilder()
            .addNetworkInterceptor { chain ->
                chain
                    .proceed(chain.request())
                    .newBuilder()
                    .header("Cache-Control", "max-age=${24 * 60 * 60}")
                    .removeHeader("Pragma")
                    .removeHeader("Expires")
                    .build()
            }
            .build()
}
