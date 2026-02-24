package eu.kanade.tachiyomi.source.online.merged.suwayomi

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.MergedServerSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.source.online.merged.suwayomi.SuwayomiLang.Companion.fromSuwayomiLang
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.system.withIOContext
import java.text.DecimalFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.nekomanga.R
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

    private val apiUrl
        get() = "${hostUrl()}/api/graphql".toHttpUrl().newBuilder().toString()

    private val mode: LoginMode
        get() = preferences.suwayomiLoginMode().get()

    private val username
        get() = preferences.sourceUsername(this).get()

    private val password
        get() = preferences.sourcePassword(this).get()

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var cookies: String = ""

    override suspend fun loginWithUrl(username: String, password: String, url: String): Boolean {
        return withIOContext {
            try {
                val suwayomiUrl = "$url/api/graphql".toHttpUrlOrNull()!!.newBuilder()
                refresh(username, password, url)
                val headers = getHeaders(username, password)
                val response = createClient().newCall(GET(suwayomiUrl.toString(), headers)).await()
                response.isSuccessful
            } catch (e: Exception) {
                TimberKt.w(e) { "error logging into suwayomi" }
                false
            }
        }
    }

    fun refresh(user: String? = null, pass: String? = null, url: String? = null) {
        when (mode) {
            LoginMode.SimpleLogin -> {
                val formBody =
                    FormBody.Builder()
                        .add("user", user ?: username)
                        .add("pass", pass ?: password)
                        .build()
                val result =
                    client
                        .newBuilder()
                        .followRedirects(false)
                        .build()
                        .newCall(POST("${url ?: hostUrl()}/login.html", body = formBody))
                        .execute()

                // login.html redirects when successful
                if (!result.isRedirect) {
                    val err =
                        result.body.string().replace(
                            ".*<div class=\"error\">([^<]*)</div>.*"
                                .toRegex(RegexOption.DOT_MATCHES_ALL)
                        ) {
                            it.groups[1]!!.value
                        }
                    throw Exception("Login failed: ${result.code} $err")
                }
                cookies = result.header("Set-Cookie", "") ?: return
            }

            LoginMode.UILogin -> {
                refreshToken?.let {
                    val response =
                        client
                            .newCall(
                                POST(
                                    "${url ?: hostUrl()}/api/graphql",
                                    body = refreshTokenFormBuilder(),
                                )
                            )
                            .execute()
                            .body
                            .use {
                                json.decodeFromString<SuwayomiGraphQLDto<SuwayomiRefreshTokenDto>>(
                                    it.string()
                                )
                            }

                    if (response.hasErrors()) {
                        this.refreshToken = null
                    }

                    response.data?.refreshToken?.let { this.accessToken = it.accessToken }

                    return
                }

                val response =
                    client
                        .newCall(
                            POST(
                                "${url ?: hostUrl()}/api/graphql",
                                baseHeaders,
                                loginFormBuilder(user, pass),
                            )
                        )
                        .execute()
                        .body
                        .use {
                            json.decodeFromString<SuwayomiGraphQLDto<SuwayomiLoginDto>>(it.string())
                        }
                if (response.hasErrors()) {
                    return
                }

                response.data?.login?.let {
                    this.accessToken = it.accessToken
                    this.refreshToken = it.refreshToken
                }
            }

            else -> {}
        }
    }

    private fun refreshTokenFormBuilder(): RequestBody {
        val variables = buildJsonObject {
            put("input", buildJsonObject { put("refreshToken", JsonPrimitive(refreshToken)) })
        }
        return buildJsonObject {
                put("operationName", JsonPrimitive("REFRESH_LOGIN_TOKEN"))
                put(
                    "query",
                    JsonPrimitive(
                        "mutation REFRESH_LOGIN_TOKEN(\$input RefreshTokenInput!) {" +
                            "refreshToken(input: \$input) {" +
                            "accessToken}}"
                    ),
                )
                put("variables", variables)
            }
            .toString()
            .toRequestBody("application/json".toMediaType())
    }

    private fun loginFormBuilder(user: String? = null, pass: String? = null): RequestBody {
        val variables = buildJsonObject {
            put(
                "input",
                buildJsonObject {
                    put("username", JsonPrimitive(user ?: username))
                    put("password", JsonPrimitive(pass ?: password))
                },
            )
        }
        return buildJsonObject {
                put("operationName", JsonPrimitive("GET_LOGIN_TOKEN"))
                put(
                    "query",
                    JsonPrimitive(
                        "mutation GET_LOGIN_TOKEN (\$input: LoginInput!) {" +
                            "login(input: \$input) {" +
                            "accessToken refreshToken}}"
                    ),
                )
                put("variables", variables)
            }
            .toString()
            .toRequestBody("application/json".toMediaType())
    }

    override fun isConfigured(): Boolean {
        return hostUrl().isNotBlank()
    }

    override suspend fun isLoggedIn(): Boolean {
        return withIOContext {
            if (!isConfigured()) return@withIOContext false
            val url = hostUrl()

            return@withIOContext loginWithUrl(username, password, url)
        }
    }

    val baseHeaders =
        Headers.Builder()
            .add("Referer", hostUrl())
            .add("User-Agent", userAgent)
            .add("Accept", "application/json")
            .add("Content-Type", "application/json")
            .build()

    private fun getHeaders(username: String, password: String): Headers {
        return baseHeaders
            .newBuilder()
            .apply {
                when (mode) {
                    LoginMode.None -> {}
                    LoginMode.SimpleLogin -> {
                        if (cookies.isNotBlank()) add("Cookie", cookies)
                    }

                    LoginMode.UILogin -> {
                        accessToken?.let { add("Authorization", "Bearer $accessToken") }
                    }

                    LoginMode.BasicAuth ->
                        add("Authorization", Credentials.basic(username, password))
                }
            }
            .build()
    }

    override val headers: Headers
        get() = getHeaders(username, password)

    override val client: OkHttpClient
        get() = createClient()

    override fun getMangaUrl(url: String): String {
        val separator = if (url.contains(Constants.SEPARATOR)) Constants.SEPARATOR else " "
        return hostUrl() + "/manga/" + url.split(separator, limit = 2)[0]
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return hostUrl() + simpleChapter.url.split(" ", limit = 2)[0]
    }

    private fun createClient(): OkHttpClient {
        return super.client
            .newBuilder()
            .dns(Dns.SYSTEM)
            .addInterceptor { chain ->
                fun Response.isGraphQLUnauthorized(): Boolean {
                    return try {
                        val body =
                            this.peekBody(Long.MAX_VALUE).byteStream().use {
                                json.decodeFromStream<SuwayomiGraphQLErrorsDto>(it)
                            }
                        body.isGraphQLUnauthorized()
                    } catch (_: Exception) {
                        false
                    }
                }

                fun Response.isUnauthorized(): Boolean =
                    this.code == 401 || this.isGraphQLUnauthorized()

                var response = chain.proceed(chain.request())

                if (response.isUnauthorized()) {
                    refresh()
                    response = chain.proceed(chain.request().newBuilder().headers(headers).build())
                }

                return@addInterceptor response
            }
            .build()
    }

    override suspend fun searchManga(query: String): List<SManga> {
        if (hostUrl().isBlank()) {
            throw Exception("Invalid host name")
        }

        val response = client.newCall(POST(apiUrl, headers, searchMangaFormBuilder(query))).await()
        val responseBody = response.body

        return responseBody.use { body ->
            with(json.decodeFromString<SuwayomiGraphQLDto<SuwayomiSearchMangaDto>>(body.string())) {
                (data ?: throw Exception("Failed to search manga")).mangas.nodes.mapNotNull { manga
                    ->
                    manga.source ?: return@mapNotNull null
                    SManga.create().apply {
                        this.title = manga.title
                        this.url =
                            listOf(manga.id, manga.source.name, manga.source.lang)
                                .joinToString(Constants.SEPARATOR)
                        this.thumbnail_url = manga.thumbnailUrl?.let{ hostUrl() + it }
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
        val lang = parts.getOrNull(2)?.let { fromSuwayomiLang(it) }
        var sourceName = parts.getOrElse(1) { "placeholder" }
        sourceName =
            when (sourceName) {
                // Add zero-width space so that it doesn't match in filteredBySource()
                in SourceManager.mergeSourceNames -> sourceName + '\u200B'
                else -> sourceName
            }

        return withContext(Dispatchers.IO) {
            com.github.michaelbull.result
                .runCatching {
                    if (hostUrl().isBlank()) {
                        throw Exception("Invalid host name")
                    }
                    var chapters =
                        try {
                            client
                                .newCall(
                                    POST(
                                        apiUrl,
                                        headers,
                                        fetchChaptersFormBuilder(mangaId.toLong()),
                                    )
                                )
                                .await()
                                .body
                                .use {
                                    json
                                        .decodeFromString<
                                            SuwayomiGraphQLDto<SuwayomiFetchChaptersDto>
                                        >(
                                            it.string()
                                        )
                                        .data
                                        ?.fetchChapters
                                        ?.chapters
                                }
                        } catch (e: Exception) {
                            TimberKt.e(e) {
                                "Error fetching suwayomi chapters, trying backup request"
                            }
                            null
                        }
                    if (chapters == null) {
                        chapters =
                            client
                                .newCall(
                                    POST(apiUrl, headers, getChaptersFormBuilder(mangaId.toLong()))
                                )
                                .await()
                                .body
                                .use {
                                    json
                                        .decodeFromString<
                                            SuwayomiGraphQLDto<SuwayomiGetChaptersDto>
                                        >(
                                            it.string()
                                        )
                                        .data
                                        ?.chapters
                                        ?.nodes ?: throw Exception("Failed to get chapters")
                                }
                    }
                    chapters = chapters.sortedBy { it.sourceOrder }
                    var previous: Pair<Float?, Boolean> = null to false
                    val chapterPairs =
                        chapters
                            .mapIndexed { index, chapter ->
                                chapter to chapters.getOrNull(index + 1)?.chapterNumber
                            }
                            .map { (chapter, next) ->
                                val sanitized =
                                    sanitizeName(
                                        chapter.name,
                                        chapter.chapterNumber,
                                        previous,
                                        next,
                                    )
                                SChapter.create().apply {
                                    if (sanitized is Name.Sanitized) {
                                        name = sanitized.name
                                        vol = sanitized.vol
                                        chapter_number = sanitized.chapter_number
                                        chapter_txt = sanitized.chapter_txt
                                        chapter_title = sanitized.chapter_title
                                        // For next
                                        previous = chapter_number to sanitized.premiere
                                    } else {
                                        name = chapter.name
                                        chapter_number = chapter.chapterNumber
                                        chapter_txt = "Ch.${chapter.chapterNumber}"
                                    }

                                    this.vol = vol
                                    url =
                                        "/manga/${mangaId}/chapter/${chapter.sourceOrder} ${chapter.id}"
                                    val scanlators = chapter.scanlator?.split(", ") ?: emptyList()
                                    scanlator =
                                        (listOf(this@Suwayomi.name, sourceName) + scanlators)
                                            .joinToString(Constants.SCANLATOR_SEPARATOR)
                                    language = lang
                                    date_upload = chapter.uploadDate
                                } to chapter.isRead
                            }
                            .reversed()
                    return@runCatching chapterPairs
                }
                .mapError {
                    TimberKt.e(it) { "Error fetching suwayomi chapters" }
                    (it.localizedMessage ?: "Suwayomi Error").toResultError()
                }
        }
    }

    fun sanitizeName(
        rawName: String,
        chapter: Float,
        previous: Pair<Float?, Boolean>,
        next: Float?,
    ): Name {
        val edgeCases =
            mutableListOf(
                "season",
                "end",
                "epilogue",
                "original story",
                "side story",
                "special episode",
                "episode special",
                "finale",
            )
        val chapter =
            if (
                (next != null &&
                    previous.first != null &&
                    next > previous.first!! &&
                    next > 0 &&
                    chapter > next) ||
                    (previous.first != null && chapter < previous.first!! && chapter > 0)
            ) {
                // Assume that the source order is correct and the match was a false positive
                -1f
            } else {
                chapter
            }
        if (chapter < 0) {
            if (rawName.contains("prologue", true)) {
                return Name.Sanitized("Ch.0 - $rawName", "", 0f, "Ch.0", rawName)
            }
            if (next != null && previous.first != null) {
                if (next <= previous.first!! + 1) {
                    val chnum = previous.first!! + 0.1f
                    val title = rawName.trimEnd('.')
                    return Name.Sanitized(
                        "Ch.${chnum.formatFloat()} - $title",
                        "",
                        chnum,
                        "Ch.${chnum.formatFloat()}",
                        title,
                    )
                } else if (previous.first == next - 2) {
                    return Name.Sanitized(
                        "Ch.${next - 1} - $rawName",
                        "",
                        next - 1,
                        "Ch.0",
                        rawName,
                    )
                }
            }
            // Source info is not sane enough, sanitizing won't work
            if (!rawName.contains("end", true)) return Name.NotSane
        }

        // This is for bato.to normalization
        if (
            previous.first != null &&
                previous.first!! > chapter &&
                edgeCases.any { rawName.contains(it, true) }
        ) {
            var chapterNumber = previous.first!!.toLong()
            val half =
                if (rawName.contains("season", true) && rawName.contains("announcement", true)) {
                    edgeCases.add("announcement")
                    ".5"
                } else {
                    if (!previous.second) chapterNumber += 1
                    ""
                }
            val chtxt = "Ch.$chapterNumber$half"
            val title = removeEndTag(rawName)
            val name = listOf(chtxt, "-", title).joinToString(" ")
            edgeCases.remove("season")
            return Name.Sanitized(
                name,
                "",
                chapterNumber.toFloat(),
                chtxt,
                rawName,
                !edgeCases.any { rawName.contains(it, true) },
            )
        }

        var vol = ""
        val ch =
            if (chapter == chapter.toLong().toFloat()) {
                chapter.toLong().toString()
            } else {
                chapter.formatFloat()
            }

        var title = rawName.replaceFirst(emojiRegex, "").trimStart()
        val chapterName = mutableListOf<String>()
        val volumePrefixes =
            arrayOf(
                "Volume",
                "Vol.",
                "volume",
                "vol.",
                "Season",
                "S",
                "(S",
                "[S",
                "season",
                "s",
                "(s",
                "[s",
            )
        val chapterPrefixes =
            arrayOf(
                "Chapter",
                "Chap",
                "Ch.",
                "Ch-",
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
                if (prefix == "Ep" && title.startsWith("Epilogue")) return@any false
                if (
                    prefix == "Season" &&
                        title.contains("Announcement") &&
                        Regex(".*\\(ch\\. \\d+\\.?\\d*\\).*").matches(title)
                )
                    return@any false
                val delimiter =
                    when (prefix[0]) {
                        '(' -> ")"
                        '[' -> "]"
                        else -> " "
                    }
                title = title.replaceFirst(prefix, "").trimStart()
                vol = title.trimStart('0').substringBefore(delimiter, "").trimEnd(',', '.', ';')
                title = title.substringAfter(delimiter).trimStart()
                if (vol.isNotEmpty()) chapterName.add("Vol.$vol")
                return@any true
            }
            false
        }
        val chtxt = "Ch.$ch"
        chapterName.add(chtxt)
        var matched =
            if (title.startsWith(ch)) {
                title = title.replaceFirst(ch, "").trimStart('.').trimStart()
                true
            } else {
                chapterPrefixes.any { prefix ->
                    if (title.startsWith(prefix)) {
                        title = title.replaceFirst(prefix, "").trimStart()
                        if (!title[0].isDigit()) {
                            title = prefix + title
                            return@any false
                        }
                        title = title.trimStart('0').replaceFirst(ch, "").trimStart()
                        // In case the number appears multiple times
                        chapterPrefixes.forEach {
                            if (
                                title.contains(ch) &&
                                    title.contains(it) &&
                                    title.substringAfter(it).substringBefore(ch).trim().isEmpty()
                            ) {
                                val pre = title.substringBefore(it).trimEnd()
                                val pos = title.substringAfter(it).substringAfter(ch).trimStart()
                                title = (pre + pos).replace("()", "")
                            }
                        }
                        title = title.replace(".cbz", "")
                        return@any true
                    }
                    false
                }
            }
        if (Regex(".*\\(ch\\. \\d+\\.?\\d*\\).*").matches(title)) {
            // This is for Webtoon.com normalization
            val note = title.substringAfterLast(")")
            title = title.substringBeforeLast("(") + note
            matched = true
        }

        if (!matched) {
            return Name.NotSane
        }

        title = removeEndTag(title)
        title = title.trimStart(':', '-', '.').trimStart()
        if (title.isNotEmpty()) {
            chapterName.add("-")
            chapterName.add(title)
        }

        return Name.Sanitized(chapterName.joinToString(" "), vol, chapter, chtxt, title)
    }

    fun Float.formatFloat(): String {
        val df = DecimalFormat("#.###")
        df.minimumFractionDigits = 0
        df.maximumFractionDigits = 3
        df.isGroupingUsed = false
        return df.format(this.toBigDecimal().stripTrailingZeros())
    }

    fun removeEndTag(title: String): String {
        var title = title
        if (Regex("\\[end].*", RegexOption.IGNORE_CASE).matches(title)) {
            title = title.substringAfter("]").trimStart()
        } else if (Regex(".*\\[end]", RegexOption.IGNORE_CASE).matches(title)) {
            title = title.substringBefore("[").trimEnd()
        } else if (Regex("\\(end\\).*", RegexOption.IGNORE_CASE).matches(title)) {
            title = title.substringAfter(")").trimStart()
        } else if (Regex(".*\\(end\\)", RegexOption.IGNORE_CASE).matches(title)) {
            title = title.substringBefore("(").trimEnd()
        }
        return title
    }

    // If this mutation fails due to the source, server-side, fetchChapters == null
    fun fetchChaptersFormBuilder(mangaId: Long): RequestBody {
        val variables = buildJsonObject {
            put("input", buildJsonObject { put("mangaId", JsonPrimitive(mangaId)) })
        }
        return buildJsonObject {
                put("operationName", JsonPrimitive("FETCH_MANGA_CHAPTERS"))
                put(
                    "query",
                    JsonPrimitive(
                        "mutation FETCH_MANGA_CHAPTERS(\$input: FetchChaptersInput!){" +
                            "fetchChapters(input: \$input) { chapters{" +
                            "id name chapterNumber sourceOrder uploadDate isRead scanlator}}}"
                    ),
                )
                put("variables", variables)
            }
            .toString()
            .toRequestBody("application/json".toMediaType())
    }

    // Use current server data in case the mutation fails
    fun getChaptersFormBuilder(mangaId: Long): RequestBody {
        val filter = buildJsonObject {
            put("mangaId", buildJsonObject { put("equalTo", JsonPrimitive(mangaId)) })
        }
        val variables = buildJsonObject { put("filter", filter) }

        return buildJsonObject {
                put("operationName", JsonPrimitive("GET_MANGA_CHAPTERS"))
                put(
                    "query",
                    JsonPrimitive(
                        "query GET_MANGA_CHAPTERS(\$filter: ChapterFilterInput!) {" +
                            "chapters(filter: \$filter) { nodes {" +
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
        val chapterId = chapter.url.split(" ", limit = 2)[1].toLong()
        val response =
            client.newCall(POST(apiUrl, headers, fetchPagesFormBuilder(chapterId))).await()
        val responseBody = response.body

        val pages =
            responseBody.use {
                json
                    .decodeFromString<SuwayomiGraphQLDto<SuwayomiFetchChapterPagesDto>>(it.string())
                    .data
                    ?.fetchChapterPages
                    ?.pages ?: throw Exception("Failed to get pages")
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

        val chapterIds = chapters.map { it.url.split(" ", limit = 2)[1].toLong() }
        try {
            client
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
        val emojiRegex = Regex("^[\\p{So}\\p{Cn}\\p{Cs}\\x{1F000}-\\x{1FFFF}]+")
    }
}

sealed class Name {
    data class Sanitized(
        val name: String,
        val vol: String,
        val chapter_number: Float,
        val chapter_txt: String,
        val chapter_title: String,
        val premiere: Boolean = false,
    ) : Name()

    data object NotSane : Name()
}

enum class LoginMode(val titleResId: Int) {
    None(R.string.suwayomi_login_mode_none),
    BasicAuth(R.string.suwayomi_login_mode_basic),
    SimpleLogin(R.string.suwayomi_login_mode_simple),
    UILogin(R.string.suwayomi_login_mode_ui),
}
