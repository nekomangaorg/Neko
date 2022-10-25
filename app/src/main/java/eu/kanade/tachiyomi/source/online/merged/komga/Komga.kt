package eu.kanade.tachiyomi.source.online.merged.komga

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.util.lang.toResultError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.injectLazy

class Komga : ReducedHttpSource() {

    override val baseUrl: String = "https://demo.komga.org"

    private val json: Json by injectLazy()

    override val client: OkHttpClient =
        network.client.newBuilder()
            .authenticator { _, response ->
                if (response.request.header("Authorization") != null) {
                    null // Give up, we've already failed to authenticate.
                } else {
                    response.request.newBuilder()
                        .addHeader("Authorization", Credentials.basic("demo@komga.org", "komga-demo"))
                        .build()
                }
            }
            .dns(Dns.SYSTEM) // don't use DNS over HTTPS as it breaks IP addressing
            .build()

    override suspend fun searchManga(query: String): List<SManga> {
        val url = "$baseUrl/api/v1/series?search=$query&unpaged=true&deleted=false".toHttpUrlOrNull()!!.newBuilder()
        val response = client.newCall(GET(url.toString(), headers)).await()
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        return responseBody.use { body ->
            with(json.decodeFromString<KomgaPageWrapperDto<KomgaSeriesDto>>(body.string())) {
                content.map { series ->
                    SManga.create().apply {
                        this.title = series.metadata.title
                        this.url = "$baseUrl/api/v1/series/${series.id}"
                        this.thumbnail_url = "${this.url}/thumbnail"
                    }
                }
            }
        }
    }

    override suspend fun fetchChapters(mangaUrl: String): Result<List<SChapter>, ResultError> {
        return withContext(Dispatchers.IO) {
            com.github.michaelbull.result.runCatching {
                val response = client.newCall(GET("\"${mangaUrl}/books?unpaged=true&media_status=READY&deleted=false", headers)).await()
                val responseBody = response.body
                    ?: throw IllegalStateException("Komga: Response code ${response.code}")

                val page = responseBody.use { json.decodeFromString<KomgaPageWrapperDto<KomgaBookDto>>(it.string()).content }
                val r = page.map { book ->
                    SChapter.create().apply {
                        chapter_number = book.metadata.numberSort
                        name = "${book.metadata.number} - ${book.metadata.title} (${book.size})"
                        url = "$baseUrl/api/v1/books/${book.id}"
                        scanlator = book.metadata.authors.groupBy({ it.role }, { it.name })["translator"]?.joinToString()
                        date_upload = book.metadata.releaseDate?.let { parseDate(it) }
                            ?: parseDateTime(book.fileLastModified)
                    }
                }
                return@runCatching r.sortedByDescending { it.chapter_number }
            }.mapError {
                XLog.e(it)
                (it.localizedMessage ?: "Komga Error").toResultError()
            }
        }
    }

    override suspend fun fetchPageList(chapter: SChapter): List<Page> {
        val response = client.newCall(GET("$baseUrl${chapter.url}", headers)).await()
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        val pages = responseBody.use { body -> json.decodeFromString<List<KomgaPageDto>>(body.string()) }
        return pages.map {
            val url = "${response.request.url}/${it.number}" +
                if (!supportedImageTypes.contains(it.mediaType)) {
                    "?convert=png"
                } else {
                    ""
                }
            Page(
                index = it.number - 1,
                imageUrl = url,
            )
        }
    }

    private fun parseDate(date: String?): Long =
        if (date == null)
            Date().time
        else {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date).time
            } catch (ex: Exception) {
                Date().time
            }
        }

    private fun parseDateTime(date: String?): Long =
        if (date == null)
            Date().time
        else {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(date).time
            } catch (ex: Exception) {
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US).parse(date).time
                } catch (ex: Exception) {
                    Date().time
                }
            }
        }

    companion object {
        private val supportedImageTypes = listOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/jxl")
    }
}


