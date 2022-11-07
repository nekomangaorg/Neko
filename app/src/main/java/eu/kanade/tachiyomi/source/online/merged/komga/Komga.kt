package eu.kanade.tachiyomi.source.online.merged.komga

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.system.withIOContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    override val baseUrl: String = ""

    override val name: String = Komga.name

    private val json: Json by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    fun hostUrl() = preferences.sourceUrl(this) ?: ""

    suspend fun loginWithUrl(username: String, password: String, url: String): Boolean {
        return withIOContext {
            val komgaUrl = "$url/api/v1/series?page=0&size=1".toHttpUrlOrNull()!!.newBuilder()
            val response = createClient(username, password).newCall(GET(komgaUrl.toString(), headers)).await()
            response.isSuccessful
        }
    }

    fun hasCredentials(): Boolean {
        val username = preferences.sourceUsername(this@Komga) ?: ""
        val password = preferences.sourcePassword(this@Komga) ?: ""
        val url = hostUrl()
        return listOf(username, password, url).none { it.isBlank() }
    }

    suspend fun isLoggedIn(): Boolean {
        return withIOContext {
            val username = preferences.sourceUsername(this@Komga) ?: ""
            val password = preferences.sourcePassword(this@Komga) ?: ""
            val url = hostUrl()
            if (listOf(username, password, url).any { it.isBlank() }) {
                return@withIOContext false
            }
            return@withIOContext loginWithUrl(username, password, url)
        }
    }

    private fun createClient(username: String, password: String): OkHttpClient {
        return network.client.newBuilder()
            .authenticator { _, response ->
                if (response.request.header("Authorization") != null) {
                    null // Give up, we've already failed to authenticate.
                } else {
                    response.request.newBuilder()
                        .addHeader("Authorization", Credentials.basic(username, password))
                        .build()
                }
            }
            .dns(Dns.SYSTEM) // don't use DNS over HTTPS as it breaks IP addressing
            .build()
    }

    fun customClient() = createClient(preferences.sourceUsername(this)!!, preferences.sourcePassword(this)!!)

    override suspend fun searchManga(query: String): List<SManga> {
        val url = "${hostUrl()}/api/v1/series?search=$query&unpaged=true&deleted=false".toHttpUrlOrNull()!!.newBuilder()
        val response = customClient().newCall(GET(url.toString(), headers)).await()
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        return responseBody.use { body ->
            with(json.decodeFromString<KomgaPageWrapperDto<KomgaSeriesDto>>(body.string())) {
                content.map { series ->
                    SManga.create().apply {
                        this.title = series.metadata.title
                        this.url = "${hostUrl()}/api/v1/series/${series.id}"
                        this.thumbnail_url = "${this.url}/thumbnail"
                    }
                }
            }
        }
    }

    override suspend fun fetchChapters(mangaUrl: String): Result<List<SChapter>, ResultError> {
        return withContext(Dispatchers.IO) {
            com.github.michaelbull.result.runCatching {
                val response = customClient().newCall(GET("$mangaUrl/books?unpaged=true&media_status=READY&deleted=false", headers)).await()
                val responseBody = response.body
                    ?: throw IllegalStateException("Komga: Response code ${response.code}")

                val page = responseBody.use { json.decodeFromString<KomgaPageWrapperDto<KomgaBookDto>>(it.string()).content }
                val r = page.map { book ->
                    SChapter.create().apply {
                        chapter_number = book.metadata.numberSort
                        name = "${book.metadata.number} - ${book.metadata.title}"
                        url = "${hostUrl()}/api/v1/books/${book.id}"
                        scanlator = this@Komga.name
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
        val response = customClient().newCall(GET("${hostUrl()}${chapter.url}", headers)).await()
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
        val name = "Komga"
        private val supportedImageTypes = listOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/jxl")
    }
}


