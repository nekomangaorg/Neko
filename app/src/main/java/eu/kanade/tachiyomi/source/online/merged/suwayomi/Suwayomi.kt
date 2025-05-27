package eu.kanade.tachiyomi.source.online.merged.suwayomi

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.MergedLoginSource
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import uy.kohesive.injekt.injectLazy

class Suwayomi : MergedLoginSource() {

    override val baseUrl: String = ""

    override val name: String = Suwayomi.name

    private val json: Json by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    override fun requiresCredentials(): Boolean = false

    override fun hostUrl() = preferences.sourceUrl(this).get()

    override suspend fun loginWithUrl(username: String, password: String, url: String): Boolean {
        return withIOContext {
            try {
                val suwayomiUrl = "$url/api/v1/settings/about".toHttpUrlOrNull()!!.newBuilder()
                val response =
                    createClient(username, password)
                        .newCall(GET(suwayomiUrl.toString(), headers))
                        .await()
                response.isSuccessful
            } catch (e: Exception) {
                TimberKt.w(e) { "error logging into suwayomi" }
                false
            }
        }
    }

    override fun hasCredentials(): Boolean {
        val username = preferences.sourceUsername(this@Suwayomi).get()
        val password = preferences.sourcePassword(this@Suwayomi).get()
        val url = hostUrl()
        return listOf(username, password, url).none { it.isBlank() }
    }

    override suspend fun isLoggedIn(): Boolean {
        return withIOContext {
            val username = preferences.sourceUsername(this@Suwayomi).get()
            val password = preferences.sourcePassword(this@Suwayomi).get()
            val url = hostUrl()
            if (listOf(username, password, url).any { it.isBlank() }) {
                return@withIOContext false
            }
            return@withIOContext loginWithUrl(username, password, url)
        }
    }

    override val headers =
        Headers.Builder()
            .add("Referer", hostUrl())
            .add("User-Agent", userAgent)
            .add("Accept", "application/json")
            .add("Content-Type", "application/json")
            .build()

    override val client: OkHttpClient
        get() = super.client.newBuilder().dns(Dns.SYSTEM).build()

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return "${hostUrl()}/manga/${simpleChapter.url}"
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
        val apiUrl = "${hostUrl()}/api/graphql".toHttpUrl().newBuilder().toString()

        val response =
            customClient().newCall(POST(apiUrl, headers, searchMangaFormBuilder(query))).await()
        val responseBody = response.body

        return responseBody.use { body ->
            with(
                json.decodeFromString<SuwayomiGraphQLResponseDto<SuwayomiSearchMangaDto>>(
                    body.string()
                )
            ) {
                data.mangas.nodes.map { manga ->
                    SManga.create().apply {
                        this.title = manga.title
                        this.url = "${manga.id}"
                        this.thumbnail_url = "${hostUrl()}/api/v1/manga/${this.url}/thumbnail"
                    }
                }
            }
        }
    }

    fun searchMangaFormBuilder(query: String): RequestBody =
        buildJsonObject {
                put("operationName", JsonPrimitive("SEARCH_MANGA"))
                put(
                    "query",
                    JsonPrimitive(
                        "query SEARCH_MANGA{mangas(filter:{title:{likeInsensitive:\"%${query}%\"}}){nodes{id title}}}"
                    ),
                )
            }
            .toString()
            .toRequestBody("application/json".toMediaType())

    override suspend fun fetchChapters(mangaUrl: String): Result<List<SChapter>, ResultError> {
        return withContext(Dispatchers.IO) {
            com.github.michaelbull.result
                .runCatching {
                    if (hostUrl().isBlank()) {
                        throw Exception("Invalid host name")
                    }
                    val apiUrl = "${hostUrl()}/api/graphql".toHttpUrl().newBuilder().toString()
                    val response =
                        customClient()
                            .newCall(POST(apiUrl, headers, fetchChaptersFormBuilder(mangaUrl)))
                            .await()
                    val responseBody = response.body

                    val chapters =
                        responseBody.use {
                            json
                                .decodeFromString<
                                    SuwayomiGraphQLResponseDto<SuwayomiFetchChaptersDto>
                                >(
                                    it.string()
                                )
                                .data
                                .fetchChapters
                                .chapters
                        }
                    val r =
                        chapters.map { chapter ->
                            SChapter.create().apply {
                                chapter_number = chapter.chapterNumber
                                name = chapter.name
                                url = "${chapter.id}"
                                scanlator = this@Suwayomi.name
                                date_upload = chapter.uploadDate
                            }
                        }
                    return@runCatching r.sortedByDescending { it.chapter_number }
                }
                .mapError {
                    TimberKt.e(it) { "Error fetching suwayomi chapters" }
                    (it.localizedMessage ?: "Suwayomi Error").toResultError()
                }
        }
    }

    fun fetchChaptersFormBuilder(mangaId: String): RequestBody {
        val variables = buildJsonObject {
            put("input", buildJsonObject { put("mangaId", JsonPrimitive(mangaId.toInt())) })
        }
        return buildJsonObject {
                put("operationName", JsonPrimitive("GET_MANGA_CHAPTERS_FETCH"))
                put(
                    "query",
                    JsonPrimitive(
                        "mutation GET_MANGA_CHAPTERS_FETCH(\$input: FetchChaptersInput!) {" +
                            "fetchChapters(input: \$input) {chapters {id name chapterNumber uploadDate}}}"
                    ),
                )
                put("variables", variables)
            }
            .toString()
            .toRequestBody("application/json".toMediaType())
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        if (hostUrl().isBlank()) {
            throw Exception("Invalid host name")
        }
        val apiUrl = "${hostUrl()}/api/graphql".toHttpUrl().newBuilder().toString()
        val response =
            customClient()
                .newCall(POST(apiUrl, headers, fetchPagesFormBuilder(chapter.url)))
                .await()
        val responseBody = response.body

        val pages =
            responseBody.use {
                json
                    .decodeFromString<SuwayomiGraphQLResponseDto<SuwayomiFetchChapterPagesDto>>(
                        it.string()
                    )
                    .data
                    .fetchChapterPages
                    .pages
            }
        return pages.withIndex().map { (index, page) ->
            Page(index, imageUrl = "${hostUrl()}${page}")
        }
    }

    fun fetchPagesFormBuilder(chapterId: String): RequestBody {
        val variables = buildJsonObject {
            put("input", buildJsonObject { put("chapterId", JsonPrimitive(chapterId.toInt())) })
        }
        return buildJsonObject {
                put("operationName", JsonPrimitive("GET_CHAPTER_PAGES_FETCH"))
                put(
                    "query",
                    JsonPrimitive(
                        "mutation GET_CHAPTER_PAGES_FETCH(\$input:FetchChapterPagesInput!) {" +
                            "fetchChapterPages(input:\$input){pages}}"
                    ),
                )
                put("variables", variables)
            }
            .toString()
            .toRequestBody("application/json".toMediaType())
    }

    companion object {
        val name = "Suwayomi"
        private val supportedImageTypes =
            listOf("image/jpeg", "image/png", "image/gif", "image/webp")
    }
}
