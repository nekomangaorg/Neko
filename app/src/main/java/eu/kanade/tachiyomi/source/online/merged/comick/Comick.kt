package eu.kanade.tachiyomi.source.online.merged.comick

import com.github.michaelbull.result.Err // Changed import
import com.github.michaelbull.result.Ok // Changed import
import com.github.michaelbull.result.Result // Changed import
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.nekomanga.constants.Constants
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import tachiyomi.core.network.awaitSuccess
import uy.kohesive.injekt.injectLazy

class Comick : ReducedHttpSource() {

    override val name = "Comick"
    override val baseUrl = "https://comick.io"
    val apiUrl = "https://api.comick.fun"
    override val headers = Headers.Builder().add("Referer", "$apiUrl/").build()
    private val json: Json by injectLazy()

    // This is the actual implementation for the abstract getPageList
    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val chapterHid = chapter.url.substringAfterLast("/").substringBefore("-")
        val url = "$apiUrl/chapter/$chapterHid?tachiyomi=true".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        val images =
            json.decodeFromString<PageList>(response.body.string()).chapter.images.ifEmpty {
                // cache busting
                val url =
                    response.request.url
                        .newBuilder()
                        .addQueryParameter("_", System.currentTimeMillis().toString())
                        .build()

                val cacheRequest = Request.Builder().url(url).build()
                val cacheResponse = client.newCall(request).awaitSuccess()
                json.decodeFromString<PageList>(cacheResponse.body.string()).chapter.images
            }
        return images.mapIndexedNotNull { index, data ->
            if (data.url == null) null else Page(index = index, imageUrl = data.url)
        }
    }

    private suspend fun searchDto( // Renamed
        query: String
    ): List<SearchResponse> {
        val url =
            "$apiUrl/v1.0/search"
                .toHttpUrl()
                .newBuilder()
                .apply {
                    addQueryParameter("q", query)
                    addQueryParameter("limit", "20")
                }
                .build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    override suspend fun searchManga(query: String): List<SManga> {
        return try {
            val searchResults = searchDto(query = query)
            searchResults.map { dto ->
                SManga.create().apply {
                    title = dto.title
                    url = "/comic/${dto.hid}"
                    thumbnail_url = dto.mdCovers.firstOrNull()?.gpurl
                    initialized = false
                }
            }
        } catch (e: Exception) {
            TimberKt.e(e)
            emptyList()
        }
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        return try {
            val comicHid = mangaUrl.substringAfterLast("/")

            val lookupUrl =
                "$apiUrl/comic/$comicHid/chapters?lang=en&tachiyomi=true&limit=99999".toHttpUrl()
            val request = Request.Builder().url(lookupUrl).build()
            val response = client.newCall(request).await()

            if (!response.isSuccessful) {
                response.close()
                return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
            }

            val chapterList = json.decodeFromString<ChapterList>(response.body.string()).chapters

            Ok(
                chapterList.map { chapter ->
                    val chapterName = beautifyChapterName(chapter.vol, chapter.chap, chapter.title)
                    SChapter.create().apply {
                        vol = chapter.vol ?: ""
                        name = chapterName
                        chapter_txt = chapterName
                        url = "$mangaUrl/${chapter.hid}-chapter-${chapter.chap ?: ""}-en"
                        date_upload = chapter.createdAt?.parseDate() ?: 0L
                        chapter_number = chapter.chap?.toFloatOrNull() ?: -1f
                        val scanlatorList = listOf(Comick.name) + (chapter.groupName ?: emptyList())
                        scanlator = scanlatorList.joinToString(Constants.SCANLATOR_SEPARATOR)
                    } to false
                }
            )
        } catch (e: Exception) {
            TimberKt.e(e)
            Err(ResultError.Generic(e.message ?: "Unknown error during chapter fetch"))
        }
    }

    override fun imageRequest(page: Page): Request {
        val imageHeaders =
            headers
                .newBuilder()
                .add("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .add("Referer", "$apiUrl/")
                .build()
        return Request.Builder().url(page.imageUrl!!).headers(imageHeaders).build()
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return baseUrl + simpleChapter.url
    }

    private fun beautifyChapterName(vol: String?, chap: String?, title: String?): String {
        val str =
            buildString {
                    if (vol?.isNotEmpty() == true) {
                        append("Vol.$vol ")
                    }

                    if (chap?.isNotEmpty() == true) {
                        append("Ch.$chap ")
                    }
                    if (title?.isNotEmpty() == true) {
                        if (this.isNotEmpty()) {
                            append("- ")
                        }
                        append(title)
                    }
                }
                .trim()
        return str
    }

    private fun String?.parseDate(): Long {
        return this?.let {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(it)?.time
            } catch (_: Exception) {
                0L
            }
        } ?: 0L
    }

    companion object {
        const val name = "Comick"
        internal val MDcovers.gpurl: String?
            get() {
                return this.b2key?.let { "https://meo.comick.pictures/$it" }
            }
    }
}
