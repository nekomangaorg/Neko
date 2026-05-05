package eu.kanade.tachiyomi.source.online.merged.atsumaru

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.source.online.merged.atsumaru.dto.AllChaptersDto
import eu.kanade.tachiyomi.source.online.merged.atsumaru.dto.MangaObjectDto
import eu.kanade.tachiyomi.source.online.merged.atsumaru.dto.PageObjectDto
import eu.kanade.tachiyomi.source.online.merged.atsumaru.dto.SearchResultsDto
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.internal.closeQuietly
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await
import uy.kohesive.injekt.injectLazy

class Atsumaru : ReducedHttpSource() {
    override val name = Atsumaru.name
    override val baseUrl = Atsumaru.baseUrl

    override val client = network.cloudFlareClient.newBuilder().rateLimit(2).build()

    override val headers =
        Headers.Builder()
            .apply {
                add("Accept", "*/*")
                add("Referer", baseUrl)
                add("Content-Type", "application/json")
            }
            .build()

    private val json: Json by injectLazy()

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        val (slug, name) = simpleChapter.url.split("/")
        return "$baseUrl/read/$slug/$name"
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val (slug, name) = chapter.url.split("/")
        val url =
            "$baseUrl/api/read/chapter"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("mangaId", slug)
                .addQueryParameter("chapterId", name)

        val response = client.newCall(GET(url.build().toString(), headers)).await()
        if (!response.isSuccessful) {
            response.closeQuietly()
            throw Exception("HTTP error ${response.code}")
        }
        val dto = json.decodeFromString<PageObjectDto>(response.body.string())

        return dto.readChapter.pages.mapIndexed { index, page ->
            val imageUrl =
                when {
                    page.image.startsWith("http") -> page.image
                    page.image.startsWith("//") -> "https:${page.image}"
                    else ->
                        "$baseUrl/static/${page.image.removePrefix("/").removePrefix("static/")}"
                }

            Page(index, imageUrl = imageUrl.replaceFirst(Regex("^https?:?//"), "https://"))
        }
    }

    override fun imageRequest(page: Page): Request {
        val imgHeaders =
            headers
                .newBuilder()
                .apply {
                    add("Accept", "image/avif,image/webp,*/*")
                    add("Referer", baseUrl)
                }
                .build()
        return GET(page.imageUrl!!, imgHeaders)
    }

    override suspend fun searchManga(query: String): List<SManga> {

        val url =
            "$baseUrl/collections/manga/documents/search"
                .toHttpUrl()
                .newBuilder()
                .apply {
                    addQueryParameter("q", query.ifEmpty { "*" })
                    addQueryParameter("query_by", "title,englishTitle,otherNames,authors")
                    addQueryParameter("query_by_weights", "4,3,2,1")
                    addQueryParameter("num_typos", "4,3,2,1")
                    addQueryParameter(
                        "include_fields",
                        "id,title,englishTitle,poster,posterSmall,posterMedium,type,isAdult,status,year,synopsis,otherNames,mbRating,avgRating,authors",
                    )
                    addQueryParameter("page", "1")
                    addQueryParameter("per_page", "40")
                }
                .build()
                .toString()

        val response = client.newCall(GET(url, headers)).await()

        if (!response.isSuccessful) {
            response.closeQuietly()
            throw Exception("HTTP error ${response.code}")
        }

        val body = response.body.string()

        return runCatching {
                json.decodeFromString<SearchResultsDto>(body).hits.map {
                    it.document.toSManga(baseUrl)
                }
            }
            .getOrThrow()
        /*  .getOrElse {
            json.decodeFromString<BrowseMangaDto>(body).items.map { it.toSManga(baseUrl) }
        }*/
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        val response =
            client.newCall(GET("$baseUrl/api/manga/allChapters?mangaId=$mangaUrl", headers)).await()

        if (!response.isSuccessful) {
            response.closeQuietly()
            return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
        }

        return response.use { res ->
            val mangaId = mangaUrl
            val scanlatorMap =
                try {
                    val detailsRequest = GET("$baseUrl/api/manga/page?id=$mangaId", headers)
                    client
                        .newCall(detailsRequest)
                        .await()
                        .use {
                            json
                                .decodeFromString<MangaObjectDto>(it.body.string())
                                .mangaPage
                                .scanlators
                                ?.associate { it.id to it.name }
                        }
                        .orEmpty()
                } catch (_: Exception) {
                    emptyMap()
                }

            val data = json.decodeFromString<AllChaptersDto>(res.body.string())

            val chapters =
                data.chapters.map {
                    it.toSChapter(mangaId, it.scanlationMangaId?.let { id -> scanlatorMap[id] }) to
                        false
                }
            Ok(chapters)
        }
    }

    companion object {
        const val name = "Atsumaru"
        const val baseUrl = "https://atsu.moe"
        private val PROTOCOL_REGEX = Regex("^https?://")
    }
}
