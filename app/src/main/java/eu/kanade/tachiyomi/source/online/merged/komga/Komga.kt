package eu.kanade.tachiyomi.source.online.merged.komga

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.system.withIOContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.nekomanga.core.network.GET
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import uy.kohesive.injekt.injectLazy

class Komga : ReducedHttpSource() {

    override val baseUrl: String = ""

    override val name: String = Komga.name

    private val json: Json by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    fun hostUrl() = preferences.sourceUrl(this).get()

    suspend fun loginWithUrl(username: String, password: String, url: String): Boolean {
        return withIOContext {
            try {
                val komgaUrl = "$url/api/v1/series?page=0&size=1".toHttpUrlOrNull()!!.newBuilder()
                val response =
                    createClient(username, password)
                        .newCall(GET(komgaUrl.toString(), headers))
                        .await()
                response.isSuccessful
            } catch (e: Exception) {
                TimberKt.w(e) { "error logging into komga" }
                false
            }
        }
    }

    fun hasCredentials(): Boolean {
        val username = preferences.sourceUsername(this@Komga).get()
        val password = preferences.sourcePassword(this@Komga).get()
        val url = hostUrl()
        return listOf(username, password, url).none { it.isBlank() }
    }

    suspend fun isLoggedIn(): Boolean {
        return withIOContext {
            val username = preferences.sourceUsername(this@Komga).get()
            val password = preferences.sourcePassword(this@Komga).get()
            val url = hostUrl()
            if (listOf(username, password, url).any { it.isBlank() }) {
                return@withIOContext false
            }
            return@withIOContext loginWithUrl(username, password, url)
        }
    }

    override val headers =
        Headers.Builder().add("Referer", hostUrl()).add("User-Agent", userAgent).build()

    override val client: OkHttpClient
        get() = super.client.newBuilder().dns(Dns.SYSTEM).build()

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return hostUrl() + simpleChapter.url
    }

    private fun createClient(username: String, password: String): OkHttpClient {
        return client
            .newBuilder()
            .authenticator { _, response ->
                if (response.request.header("Authorization") != null) {
                    null // Give up, we've already failed to authenticate.
                } else {
                    response.request
                        .newBuilder()
                        .addHeader("Authorization", Credentials.basic(username, password))
                        .build()
                }
            }
            .build()
    }

    fun customClient() =
        createClient(preferences.sourceUsername(this).get(), preferences.sourcePassword(this).get())

    override suspend fun searchManga(query: String): List<SManga> {
        if (hostUrl().isBlank()) {
            throw Exception("Invalid host name")
        }
        val apiUrl =
            "${hostUrl()}/api/v1/series"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("search", query)
                .addQueryParameter("unpaged", "true")
                .addQueryParameter("deleted", "false")
                .toString()

        val response = customClient().newCall(GET(apiUrl, headers)).await()
        val responseBody = response.body

        return responseBody.use { body ->
            with(json.decodeFromString<KomgaPaginatedResponseDto<KomgaSeriesDto>>(body.string())) {
                content.map { series ->
                    SManga.create().apply {
                        this.title = series.metadata.title
                        this.url = "/api/v1/series/${series.id}"
                        this.thumbnail_url = "${hostUrl()}${this.url}/thumbnail"
                    }
                }
            }
        }
    }

    override suspend fun fetchChapters(mangaUrl: String): Result<List<SChapter>, ResultError> {
        return withContext(Dispatchers.IO) {
            com.github.michaelbull.result
                .runCatching {
                    if (hostUrl().isBlank()) {
                        throw Exception("Invalid host name")
                    }
                    val apiUrl =
                        "${hostUrl()}$mangaUrl/books"
                            .toHttpUrl()
                            .newBuilder()
                            .addQueryParameter("unpaged", "true")
                            .addQueryParameter("media_status", "READY")
                            .addQueryParameter("deleted", "false")
                            .toString()
                    val response = customClient().newCall(GET(apiUrl, headers)).await()
                    val responseBody = response.body

                    val page =
                        responseBody.use {
                            json
                                .decodeFromString<KomgaPaginatedResponseDto<KomgaBookDto>>(
                                    it.string())
                                .content
                        }
                    val r =
                        page.map { book ->
                            SChapter.create().apply {
                                chapter_number = book.metadata.numberSort
                                name = "${book.metadata.number} - ${book.metadata.title}"
                                url = "/api/v1/books/${book.id}"
                                scanlator = this@Komga.name
                                date_upload =
                                    book.metadata.releaseDate?.toDate()
                                        ?: book.fileLastModified.toDateTime()
                            }
                        }
                    return@runCatching r.sortedByDescending { it.chapter_number }
                }
                .mapError {
                    TimberKt.e(it) { "Error fetching komga chapters" }
                    (it.localizedMessage ?: "Komga Error").toResultError()
                }
        }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        if (hostUrl().isBlank()) {
            throw Exception("Invalid host name")
        }
        val chapterUrl = "${hostUrl()}${chapter.url}/pages"
        val response = customClient().newCall(GET(chapterUrl, headers)).await()
        val responseBody = response.body

        val pages =
            responseBody.use { body -> json.decodeFromString<List<KomgaPageDto>>(body.string()) }
        return pages.map { page ->
            Page(
                index = page.number - 1,
                imageUrl =
                    "${response.request.url}/${page.number}" +
                        ("?convert=png".takeIf { !supportedImageTypes.contains(page.mediaType) }
                            ?: ""),
            )
        }
    }

    private fun String?.toDate(): Long {
        return runCatching { this?.let { dateIso8601Formatter.parse(it)?.time } }.getOrNull() ?: 0L
    }

    private fun String.toDateTime(): Long {
        val firstAttempt = runCatching { dateTimeIso8601Formatter.parse(this)?.time }

        return firstAttempt.getOrNull()
            ?: runCatching { dateTimeFullIso8601Formatter.parse(this)?.time }.getOrNull()
            ?: 0L
    }

    private val dateIso8601Formatter by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    private val dateTimeIso8601Formatter by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }

    private val dateTimeFullIso8601Formatter by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US)
    }

    companion object {
        val name = "Komga"
        private val supportedImageTypes =
            listOf("image/jpeg", "image/png", "image/gif", "image/webp")
    }
}
