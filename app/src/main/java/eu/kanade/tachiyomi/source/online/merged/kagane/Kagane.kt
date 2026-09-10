package eu.kanade.tachiyomi.source.online.merged.kagane

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.closeQuietly
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import tachiyomi.core.network.await
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.injectLazy

class Kagane : ReducedHttpSource() {
    override val name = Kagane.name
    override val baseUrl = Kagane.baseUrl

    private val apiUrl = "$baseUrl/api/v2"
    private val json: Json by injectLazy()
    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

    override val client =
        network.cloudFlareClient
            .newBuilder()
            .addInterceptor(::refreshTokenInterceptor)
            .rateLimit(3)
            .build()

    override val headers: Headers = Headers.Builder().add("Referer", "$baseUrl/").build()

    private var integrityToken: String = ""
    private var integrityExp = 0L
    private val integrityMutex = Mutex()

    private fun refreshTokenInterceptor(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val url = request.url
        if (!url.queryParameterNames.contains("token")) {
            return chain.proceed(request)
        }

        val segments = url.pathSegments
        val chapterId = segments.getOrNull(4)?.let {
            if (it == "datasaver") segments.getOrNull(5) else it
        } ?: return chain.proceed(request)

        var response = chain.proceed(request)
        if (response.code == 401 || response.code == 403 || response.code == 507) {
            response.close()
            val token =
                runBlocking { runCatching { getChallengeResponse(chapterId).accessToken } }
                    .getOrNull() ?: throw IOException("Failed to retrieve token")
            response =
                chain.proceed(
                    request
                        .newBuilder()
                        .url(url.newBuilder().setQueryParameter("token", token).build())
                        .build()
                )
        }
        return response
    }

    private fun invalidateIntegrityToken() {
        integrityToken = ""
        integrityExp = 0L
    }

    private fun jsonBody(value: String) =
        value.toRequestBody("application/json;charset=UTF-8".toMediaType())

    /** Neko's enabled chapter languages mapped onto Kagane's content_lang codes. */
    private fun contentLangs(): List<String> {
        val langs =
            mangaDexPreferences
                .enabledChapterLanguages()
                .get()
                .map { KaganeLang.fromMangadexLang(it) }
                .distinct()
        return langs.ifEmpty { listOf("en") }
    }

    private var sourceNamesCache: Map<String, String>? = null

    private suspend fun sourceNames(): Map<String, String> {
        sourceNamesCache?.let {
            return it
        }
        return runCatching {
                val res =
                    client
                        .newCall(
                            POST(
                                "$apiUrl/sources/list",
                                headers,
                                jsonBody("{\"source_types\":null}"),
                            )
                        )
                        .await()
                if (!res.isSuccessful) {
                    res.closeQuietly()
                    return emptyMap()
                }
                with(json) { res.parseAs<SourcesDto>() }
                    .sources
                    .associate { it.sourceId to it.title }
            }
            .getOrDefault(emptyMap())
            .also { if (it.isNotEmpty()) sourceNamesCache = it }
    }

    override suspend fun searchManga(query: String): List<SManga> {
        val body = buildJsonObject {
            if (query.isNotBlank()) put("title", query)
            putJsonArray("content_rating") {
                add("Safe")
                add("Suggestive")
                add("Erotica")
                add("Pornographic")
            }
            putJsonArray("content_lang") { contentLangs().forEach { add(it) } }
        }
        val url =
            "$apiUrl/search/series"
                .toHttpUrl()
                .newBuilder()
                .apply {
                    addQueryParameter("page", "0")
                    addQueryParameter("size", "35")
                }
                .build()
                .toString()

        val response =
            client
                .newCall(
                    POST(
                        url,
                        headers,
                        jsonBody(
                            json.encodeToString(
                                kotlinx.serialization.json.JsonObject.serializer(),
                                body,
                            )
                        ),
                    )
                )
                .await()
        if (!response.isSuccessful) {
            response.closeQuietly()
            throw Exception("HTTP error ${response.code}")
        }
        val dto = with(json) { response.parseAs<SearchDto>() }
        val names = sourceNames()
        return dto.content.map { it.toSManga(apiUrl, names[it.sourceId]) }
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        val response = client.newCall(GET("$apiUrl/series/$mangaUrl", headers)).await()
        if (!response.isSuccessful) {
            response.closeQuietly()
            return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
        }
        val dto = with(json) { response.parseAs<DetailsDto>() }
        val language = dto.translatedLanguage?.let { KaganeLang.fromKaganeLang(it) }
        val scanlatorSource = dto.sourceId?.let { sourceNames()[it] }
        val chapters =
            dto.seriesBooks.map {
                it.toSChapter(mangaUrl, Kagane.name, scanlatorSource, language) to false
            }
        return Ok(chapters.reversed())
    }

    private val dataSaver: Boolean
        get() = mangaDexPreferences.dataSaver().get()

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        if (";" in chapter.url) error("Outdated chapter URL. Please refresh the chapter list")
        val chapterId = chapter.url.trimEnd('/').substringAfterLast('/')
        val challenge = getChallengeResponse(chapterId)
        val token = challenge.accessToken
        val cacheUrl = challenge.cacheUrl
        return (challenge.manifest?.pages ?: emptyList()).map { page ->
            val pageUrl =
                "$cacheUrl/api/v2/books/page"
                    .toHttpUrl()
                    .newBuilder()
                    .apply {
                        if (dataSaver) {
                            addPathSegment("datasaver")
                        }
                        addPathSegment(chapterId)
                        addPathSegment("${page.pageUuid}.${page.ext ?: "jxl"}")
                        addQueryParameter("token", token)
                    }
                    .build()
                    .toString()
            Page(page.pageNumber, url = pageUrl, imageUrl = pageUrl)
        }
    }

    private suspend fun getIntegrityToken(): String {
        if (System.currentTimeMillis() < integrityExp && integrityToken.isNotBlank()) {
            return integrityToken
        }
        return integrityMutex.withLock {
            if (System.currentTimeMillis() < integrityExp && integrityToken.isNotBlank()) {
                return@withLock integrityToken
            }
            client.newCall(GET("$baseUrl/", headers)).await().closeQuietly()
            val response =
                client.newCall(POST("$baseUrl/api/integrity", headers, jsonBody(""))).await()
            if (!response.isSuccessful) {
                response.closeQuietly()
                invalidateIntegrityToken()
                throw IOException("Failed to obtain integrity token: HTTP ${response.code}")
            }
            val dto = with(json) { response.parseAs<IntegrityDto>() }
            integrityToken = dto.token
            integrityExp = dto.exp * 1000
            integrityToken
        }
    }

    private suspend fun getChallengeResponse(chapterId: String): ChallengeDto {
        val token = getIntegrityToken()
        val url =
            "$apiUrl/books/$chapterId"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("is_datasaver", dataSaver.toString())
                .build()
                .toString()
        val reqHeaders = headers.newBuilder().add("x-integrity-token", token).build()
        val response = client.newCall(POST(url, reqHeaders, jsonBody("{}"))).await()
        if (!response.isSuccessful) {
            response.closeQuietly()
            invalidateIntegrityToken()
            throw IOException("Failed to retrieve book challenge: HTTP ${response.code}")
        }
        return with(json) { response.parseAs<ChallengeDto>() }
    }

    override fun getMangaUrl(url: String): String {
        return "$baseUrl/series/$url"
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String = baseUrl + simpleChapter.url

    override fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headers)

    companion object {
        const val name = "Kagane"
        const val baseUrl = "https://kagane.to"
    }
}
