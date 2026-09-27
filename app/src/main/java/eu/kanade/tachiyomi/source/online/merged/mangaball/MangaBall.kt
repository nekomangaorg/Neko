package eu.kanade.tachiyomi.source.online.merged.mangaball

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.util.lang.toDisplayMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.closeQuietly
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.HttpException
import tachiyomi.core.network.await
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.injectLazy

class MangaBall : ReducedHttpSource() {

    override val name = MangaBall.name
    override val baseUrl = MangaBall.baseUrl

    private val json: Json by injectLazy()
    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

    /** Neko's enabled chapter languages mapped onto MangaBall site language codes. */
    private fun siteLangs(): List<String> {
        val langs =
            mangaDexPreferences
                .enabledChapterLanguages()
                .get()
                .flatMap { MangaBallLang.fromMangadexLang(it) }
                .distinct()
        return langs.ifEmpty { listOf("en") }
    }

    override val headers: Headers = Headers.Builder().apply { add("Referer", "$baseUrl/") }.build()

    override val client = network.cloudFlareClient

    override suspend fun searchManga(query: String): List<SManga> {
        val response =
            client.newCall(GET(searchUrl(query, siteLangs()).toString(), headers)).await()
        if (!response.isSuccessful) {
            response.closeQuietly()
            throw HttpException(response.code)
        }
        val search = with(json) { response.parseAs<SearchResponse>() }
        return search.data.map { it.toSManga() }
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        return try {
            val id = mangaUrl.substringAfterLast("-")
            val response =
                client
                    .newCall(
                        POST(
                            "$baseUrl/api/v1/chapter/chapter-listing-by-title-id",
                            headers,
                            titleIdBody(id),
                        )
                    )
                    .await()
            if (!response.isSuccessful) {
                response.closeQuietly()
                return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
            }

            val chapterList = with(json) { response.parseAs<ChapterListResponse>() }
            val enabledSiteLangs = siteLangs()
            Ok(chapterList.data.mapNotNull { it.toSChapter(enabledSiteLangs) }.map { it to false })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TimberKt.e(e) { "Error fetching chapters for MangaBall" }
            Err(ResultError.Generic(e.toDisplayMessage()))
        }
    }

    override fun getMangaUrl(url: String): String {
        return "$baseUrl/title-detail/$url"
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return "$baseUrl/chapter-detail/${simpleChapter.url}"
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val url =
            "$baseUrl/api/v1/chapter-detail"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("chapter_id", chapter.url)
                .build()
        val response = client.newCall(GET(url.toString(), headers)).await()
        if (!response.isSuccessful) {
            response.closeQuietly()
            throw HttpException(response.code)
        }
        return with(json) { response.parseAs<ChapterDetailResponse>() }.toPageList()
    }

    companion object {
        const val name = "Manga Ball"
        const val baseUrl = "https://mangaball.com"

        fun searchUrl(query: String, siteLangs: List<String>): HttpUrl =
            "$baseUrl/api/v1/title/search-advanced"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("limit", "24")
                .addQueryParameter("keyword", query.trim())
                .addQueryParameter("translated_language", siteLangs.joinToString(","))
                .build()

        /** The API answers 400 to a charset in Content-Type, which String.toRequestBody adds. */
        fun titleIdBody(titleId: String): RequestBody {
            val body = buildJsonObject { put("title_id", titleId) }.toString()
            return body.toByteArray().toRequestBody("application/json".toMediaType())
        }
    }
}
