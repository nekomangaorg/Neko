package eu.kanade.tachiyomi.source.online.merged.weebdex

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.source.online.merged.weebdex.dto.ChapterDto
import eu.kanade.tachiyomi.source.online.merged.weebdex.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.merged.weebdex.dto.MangaListDto
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await

class WeebDex : ReducedHttpSource() {
    override val name = WeebDex.name
    override val baseUrl = WeebDex.baseUrl

    override val client =
        network.cloudFlareClient.newBuilder().rateLimit(WeebDexConstants.RATE_LIMIT).build()

    override val headers = Headers.Builder().add("Referer", "$baseUrl/").build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun searchManga(query: String): List<SManga> {
        val urlBuilder =
            WeebDexConstants.API_MANGA_URL.toHttpUrl()
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("availableTranslatedLang", "en")

        if (query.isNotBlank()) {
            urlBuilder.addQueryParameter("title", query)
        }

        val request = GET(urlBuilder.build(), headers)
        val response = client.newCall(request).await()

        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        val mangaListDto = json.decodeFromString<MangaListDto>(response.body!!.string())
        return mangaListDto.toSMangaList("")
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        val chapters = mutableListOf<SChapter>()
        var page = 1

        try {
            while (true) {
                val url =
                    "${WeebDexConstants.API_URL}$mangaUrl/chapters"
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("order", "desc")
                        .addQueryParameter("tlang", "en")
                        .addQueryParameter("page", page.toString())
                        .build()

                val response = client.newCall(GET(url, headers)).await()
                if (!response.isSuccessful) {
                    response.close()
                    return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
                }

                val chapterListDto = json.decodeFromString<ChapterListDto>(response.body!!.string())
                chapters.addAll(chapterListDto.toSChapterList())

                if (chapterListDto.hasNextPage) {
                    page++
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            return Err(ResultError.GenericError(e.message ?: "Unknown error"))
        }

        return Ok(chapters.map { it to false })
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val url = "${WeebDexConstants.API_URL}${chapter.url}"
        val response = client.newCall(GET(url, headers)).await()

        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        val chapterDto = json.decodeFromString<ChapterDto>(response.body!!.string())
        return chapterDto.toPageList(false)
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return "$baseUrl${simpleChapter.url}"
    }

    companion object {
        const val name = "WeebDex"
        const val baseUrl = "https://weebdex.org"
    }
}
