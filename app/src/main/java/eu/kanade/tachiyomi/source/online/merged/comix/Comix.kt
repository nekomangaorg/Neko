package eu.kanade.tachiyomi.source.online.merged.comix

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await

class Comix : ReducedHttpSource() {
    override val name = "Comix"
    override val baseUrl = "https://comix.to"
    private val apiUrl = "https://comix.to/api/v2/"

    override val client = network.cloudFlareClient.newBuilder().rateLimit(5).build()

    override val headers = Headers.Builder().add("Referer", "$baseUrl/").build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun searchManga(query: String): List<SManga> {
        val url =
            apiUrl
                .toHttpUrl()
                .newBuilder()
                .apply {
                    addPathSegment("manga")
                    addQueryParameter("keyword", query)
                    addQueryParameter("order[relevance]", "desc")
                    addQueryParameter("limit", "50")
                    addQueryParameter("page", "1")
                }
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
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        val mangaHash = mangaUrl.removePrefix("/").substringAfterLast("/")

        // Logic to support deduplication if enabled
        var chapterMap: LinkedHashMap<Number, Chapter>? = null
        var chapterList: ArrayList<Chapter>? = null

        chapterList = ArrayList()

        var page = 1
        var hasNext: Boolean

        try {
            do {
                val url =
                    apiUrl
                        .toHttpUrl()
                        .newBuilder()
                        .addPathSegment("manga")
                        .addPathSegment(mangaHash)
                        .addPathSegment("chapters")
                        .addQueryParameter("order[number]", "desc")
                        .addQueryParameter("limit", "100")
                        .addQueryParameter("page", page.toString())
                        .build()

                val response = client.newCall(GET(url.toString(), headers)).await()
                if (!response.isSuccessful) {
                    response.close()
                    return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
                }

                val resp = json.decodeFromString<ChapterDetailsResponse>(response.body.string())
                val items = resp.result.items
                hasNext = resp.result.pagination.lastPage > resp.result.pagination.page
                page++

                chapterList.addAll(items)
            } while (hasNext)

            return Ok(chapterList.map { it.toSChapter(mangaHash) to false })
        } catch (e: Exception) {
            return Err(ResultError.Generic(e.message ?: "Unknown error"))
        }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val chapterId = chapter.url.substringAfterLast("/")
        val url = "${apiUrl}chapters/$chapterId"

        val response = client.newCall(GET(url, headers)).await()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        val res = json.decodeFromString<ChapterResponse>(response.body!!.string())
        val result = res.result ?: throw Exception("Chapter not found")

        if (result.images.isEmpty()) {
            throw Exception("No images found for chapter ${result.chapterId}")
        }

        return result.images.mapIndexed { index, img -> Page(index, imageUrl = img.url) }
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    override fun getChapterUrl(simpleChapter: org.nekomanga.domain.chapter.SimpleChapter): String {
        return baseUrl + simpleChapter.url
    }

    companion object {
        const val name = "Comix"
        const val baseUrl = "https://comix.to"
    }
}
