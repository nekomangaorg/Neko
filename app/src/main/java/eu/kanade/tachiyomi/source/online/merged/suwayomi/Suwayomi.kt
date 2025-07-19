package eu.kanade.tachiyomi.source.online.merged.suwayomi

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.MergedServerSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.source.online.merged.suwayomi.SuwayomiLang.Companion.fromSuwayomiLang
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
import org.nekomanga.constants.Constants
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import uy.kohesive.injekt.injectLazy

class Suwayomi : MergedServerSource() {

    override val baseUrl: String = ""

    override val name: String = Suwayomi.name

    private val json: Json by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    override fun hostUrl() = preferences.sourceUrl(this).get()

    override fun requiresCredentials(): Boolean = false

    override suspend fun loginWithUrl(username: String, password: String, url: String): Boolean {
        return withIOContext {
            try {
                val suwayomiUrl = "$url/api/graphql".toHttpUrlOrNull()!!.newBuilder()
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

    override fun isConfigured(): Boolean {
        return hostUrl().isNotBlank()
    }

    override suspend fun isLoggedIn(): Boolean {
        return withIOContext {
            if (!isConfigured()) return@withIOContext false
            val username = preferences.sourceUsername(this@Suwayomi).get()
            val password = preferences.sourcePassword(this@Suwayomi).get()
            val url = hostUrl()

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

    override fun getMangaUrl(url: String): String {
        val separator = if (url.contains(Constants.SEPARATOR)) Constants.SEPARATOR else " "
        return hostUrl() + "/manga/" + url.split(separator, limit = 2)[0]
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return hostUrl() + simpleChapter.url.split(" ", limit = 2)[0]
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
            with(json.decodeFromString<SuwayomiGraphQLDto<SuwayomiSearchMangaDto>>(body.string())) {
                data.mangas.nodes.map { manga ->
                    SManga.create().apply {
                        this.title = manga.title
                        this.url =
                            listOf(manga.id, manga.source.name, manga.source.lang)
                                .joinToString(Constants.SEPARATOR)
                        this.thumbnail_url = hostUrl() + manga.thumbnailUrl
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
                        "query SEARCH_MANGA{" +
                            "mangas(condition:{inLibrary:true}," +
                            "filter:{title:{includesInsensitive:\"${query}\"}}){" +
                            "nodes{id title thumbnailUrl source{name lang}}}}"
                    ),
                )
            }
            .toString()
            .toRequestBody("application/json".toMediaType())

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        val separator = if (mangaUrl.contains(Constants.SEPARATOR)) Constants.SEPARATOR else " "
        val parts = mangaUrl.split(separator, limit = 3)
        val mangaId = parts[0]
        val sourceName = parts.getOrNull(1)
        val lang = parts.getOrNull(2)?.let { fromSuwayomiLang(it) }

        return withContext(Dispatchers.IO) {
            com.github.michaelbull.result
                .runCatching {
                    if (hostUrl().isBlank()) {
                        throw Exception("Invalid host name")
                    }
                    val apiUrl = "${hostUrl()}/api/graphql".toHttpUrl().newBuilder().toString()
                    val response =
                        customClient()
                            .newCall(
                                POST(apiUrl, headers, fetchChaptersFormBuilder(mangaId.toLong()))
                            )
                            .await()

                    val responseBody = response.body

                    val chapters =
                        responseBody.use {
                            json
                                .decodeFromString<SuwayomiGraphQLDto<SuwayomiFetchChaptersDto>>(
                                    it.string()
                                )
                                .data
                                .fetchChapters
                                .chapters
                        }
                    val r =
                        chapters.map { chapter ->
                            val sanitized = sanitizeName(chapter.name, chapter.chapterNumber)
                            SChapter.create().apply {
                                chapter_number = chapter.chapterNumber
                                if (sanitized is Name.Sanitized) {
                                    name = sanitized.name
                                    vol = sanitized.vol
                                    chapter_txt = sanitized.chapter_txt
                                    chapter_title = sanitized.chapter_title
                                } else {
                                    name = chapter.name
                                    chapter_txt = "Ch.${chapter.chapterNumber}"
                                }

                                this.vol = vol
                                url =
                                    "/manga/${mangaId}/chapter/${chapter.sourceOrder}" +
                                        " " +
                                        "${chapter.id}"
                                scanlator =
                                    listOfNotNull(this@Suwayomi.name, sourceName, chapter.scanlator)
                                        .joinToString(Constants.SCANLATOR_SEPARATOR)
                                language = lang
                                date_upload = chapter.uploadDate
                            } to chapter.isRead
                        }
                    return@runCatching r.sortedByDescending { it.first.chapter_number }
                }
                .mapError {
                    TimberKt.e(it) { "Error fetching suwayomi chapters" }
                    (it.localizedMessage ?: "Suwayomi Error").toResultError()
                }
        }
    }

    fun sanitizeName(rawName: String, chapter: Float): Name {
        if (chapter < 0) {
            // Source info is not sane, sanitizing won't work
            return Name.NotSane
        }
        var vol = ""
        val ch =
            if (chapter == chapter.toLong().toFloat()) {
                    chapter.toLong()
                } else {
                    chapter
                }
                .toString()

        var title = rawName
        val chapterName = mutableListOf<String>()
        val volumePrefixes =
            arrayOf("Volume", "Vol.", "volume", "vol.", "Season", "S", "(S", "season", "s", "(s")
        val chapterPrefixes =
            arrayOf(
                "Chapter",
                "Chap",
                "Ch.",
                "Ch",
                "chapter",
                "chap",
                "ch.",
                "ch",
                "#",
                "Episode",
                "Ep.",
                "Ep",
                "episode",
                "ep.",
                "ep",
            )

        volumePrefixes.any { prefix ->
            if (title.startsWith(prefix)) {
                if (prefix == "S" && !Regex("[Ss]\\d+.*").matches(title)) return@any false
                val delimiter =
                    when (prefix.startsWith('(')) {
                        true -> ")"
                        false -> " "
                    }
                title = title.replace(prefix, "").trimStart()
                vol = title.trimStart('0').substringBefore(delimiter, "").trimEnd(',', '.', ';')
                title = title.substringAfter(delimiter).trimStart()
                if (vol.isNotEmpty()) chapterName.add("Vol.$vol")
                return@any true
            }
            false
        }
        val chtxt = "Ch.$ch"
        chapterName.add(chtxt)
        if (
            !chapterPrefixes.any { prefix ->
                if (title.startsWith(prefix)) {
                    title = title.replaceFirst(prefix, "").trimStart()
                    title = title.trimStart('0').replaceFirst(ch, "").trimStart()
                    return@any true
                }
                false
            }
        ) {
            return Name.NotSane
        }

        if (Regex("\\[end].*", RegexOption.IGNORE_CASE).matches(title)) {
            title = title.substringAfter("]").trimStart()
        }
        title = title.trimStart(':', '-').trimStart()
        if (title.isNotEmpty()) {
            chapterName.add("-")
            chapterName.add(title)
        }

        return Name.Sanitized(chapterName.joinToString(" "), vol, chtxt, title)
    }

    fun fetchChaptersFormBuilder(mangaId: Long): RequestBody {
        val variables = buildJsonObject {
            put("input", buildJsonObject { put("mangaId", JsonPrimitive(mangaId)) })
        }
        return buildJsonObject {
                put("operationName", JsonPrimitive("GET_MANGA_CHAPTERS_FETCH"))
                put(
                    "query",
                    JsonPrimitive(
                        "mutation GET_MANGA_CHAPTERS_FETCH(\$input: FetchChaptersInput!){" +
                            "fetchChapters(input: \$input) { chapters{" +
                            "id name chapterNumber sourceOrder uploadDate isRead scanlator}}}"
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
        val chapterId = chapter.url.split(" ", limit = 2)[1].toLong()
        val response =
            customClient().newCall(POST(apiUrl, headers, fetchPagesFormBuilder(chapterId))).await()
        val responseBody = response.body

        val pages =
            responseBody.use {
                json
                    .decodeFromString<SuwayomiGraphQLDto<SuwayomiFetchChapterPagesDto>>(it.string())
                    .data
                    .fetchChapterPages
                    .pages
            }
        return pages.withIndex().map { (index, page) ->
            Page(index, imageUrl = "${hostUrl()}${page}")
        }
    }

    fun fetchPagesFormBuilder(chapterId: Long): RequestBody {
        val variables = buildJsonObject {
            put("input", buildJsonObject { put("chapterId", JsonPrimitive(chapterId)) })
        }
        return buildJsonObject {
                put("operationName", JsonPrimitive("GET_CHAPTER_PAGES_FETCH"))
                put(
                    "query",
                    JsonPrimitive(
                        "mutation GET_CHAPTER_PAGES_FETCH(\$input:FetchChapterPagesInput!){" +
                            "fetchChapterPages(input:\$input){pages}}"
                    ),
                )
                put("variables", variables)
            }
            .toString()
            .toRequestBody("application/json".toMediaType())
    }

    override suspend fun updateStatusChapters(chapters: List<SChapter>, read: Boolean) {
        if (hostUrl().isBlank()) {
            throw Exception("Invalid host name")
        }
        val apiUrl = "${hostUrl()}/api/graphql".toHttpUrl().newBuilder().toString()

        val chapterIds = chapters.map { it.url.split(" ", limit = 2)[1].toLong() }
        try {
            customClient()
                .newCall(POST(apiUrl, headers, updateChapterFormBuilder(chapterIds, read)))
                .await()
        } catch (e: Exception) {
            TimberKt.w(e) { "error updating chapter status in suwayomi" }
        }
    }

    fun updateChapterFormBuilder(chapterIds: List<Long>, read: Boolean): RequestBody {
        val variables = buildJsonObject {
            put(
                "input",
                buildJsonObject {
                    put("ids", JsonArray(chapterIds.map { JsonPrimitive(it) }))
                    put("patch", buildJsonObject { put("isRead", JsonPrimitive(read)) })
                },
            )
        }
        return buildJsonObject {
                put("operationName", JsonPrimitive("UPDATE_CHAPTERS"))
                put(
                    "query",
                    JsonPrimitive(
                        "mutation UPDATE_CHAPTERS(\$input:UpdateChaptersInput!){" +
                            "updateChapters(input:\$input){__typename}}"
                    ),
                )
                put("variables", variables)
            }
            .toString()
            .toRequestBody("application/json".toMediaType())
    }

    companion object {
        val name = "Suwayomi"
    }
}

sealed class Name {
    data class Sanitized(
        val name: String,
        val vol: String,
        val chapter_txt: String,
        val chapter_title: String,
    ) : Name()

    data object NotSane : Name()
}
