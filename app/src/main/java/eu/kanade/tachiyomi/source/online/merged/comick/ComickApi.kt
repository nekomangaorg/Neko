package eu.kanade.tachiyomi.source.online.merged.comick

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy

class ComickApi(private val client: OkHttpClient, private val lang: String) {
    private val json: Json by injectLazy()

    private val baseUrl = COMICK_URL

    suspend fun getComic(slug: String): ComickComic {
        val url = "$baseUrl/comic/$slug".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun getChapterList(comicHid: String, page: Int): ChapterList {
        val url = "$baseUrl/comic/$comicHid/chapters?lang=$lang&page=$page&limit=$CHAPTER_LIST_LIMIT&sort=chap".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun getPageList(chapterHid: String): List<Page> {
        val url = "$baseUrl/chapter/$chapterHid".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        val chapterData = json.decodeFromString<PageList>(response.body.string()).chapter
        return chapterData.images.mapIndexed { i, page ->
            Page(i, imageUrl = page.url!!, B2key = page.b2key)
        }
    }

    suspend fun getTopComics(
        comicType: String,
        timeSpan: String,
        page: Int,
        genre: String = "all",
        demographic: String = "all",
        completed: Boolean? = null,
    ): List<SearchResponse> {
        val url = "$baseUrl/top".toHttpUrl().newBuilder().apply {
            addQueryParameter("type", comicType)
            addQueryParameter("comic_types", timeSpan)
            addQueryParameter("page", page.toString())
            if (genre != "all") addQueryParameter("genres", genre)
            if (demographic != "all") addQueryParameter("demographics", demographic)
            if (completed != null) addQueryParameter("completed", completed.toString())
        }.build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun search(
        query: String,
        page: Int,
        genre: String = "all",
        demographic: String = "all",
        completed: Boolean? = null,
    ): List<SearchResponse> {
        val url = "$baseUrl/v1.0/search".toHttpUrl().newBuilder().apply {
            addQueryParameter("q", query)
            addQueryParameter("page", page.toString())
            addQueryParameter("limit", "20")
            if (genre != "all") addQueryParameter("genres", genre)
            if (demographic != "all") addQueryParameter("demographics", demographic)
            if (completed != null) addQueryParameter("completed", completed.toString())
        }.build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun getGenres(): List<Genre> {
        val url = "$baseUrl/genre".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun getDemographics(): List<Demographic> {
        val url = "$baseUrl/demographic".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun getIdentity(): Identity {
        val url = "$baseUrl/auth/me".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun login(token: String): Identity {
        val url = "$baseUrl/auth/token".toHttpUrl()
        val requestBody = """{"token": "$token"}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun getBookmarks(page: Int): List<Bookmark> {
        val url = "$baseUrl/me/bookmarks?type=comic&page=$page".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun addBookmark(comicHid: String): RequestResult {
        val url = "$baseUrl/comic/$comicHid/bookmark".toHttpUrl()
        val request = Request.Builder().url(url).post("".toRequestBody()).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun removeBookmark(comicHid: String): RequestResult {
        val url = "$baseUrl/comic/$comicHid/unbookmark".toHttpUrl()
        val request = Request.Builder().url(url).post("".toRequestBody()).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun getReadMarkers(comicHids: List<String>): List<ReadMarker> {
        val url = "$baseUrl/me/read-markers".toHttpUrl()
        val requestBody = """{"comics": ${comicHids.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    suspend fun markChapterAsRead(chapterHid: String, comicHid: String, guid: String? = null): RequestResult {
        val url = "$baseUrl/chapter/$chapterHid/read".toHttpUrl()
        val requestBody = """{"comic_hid": "$comicHid", "guid": ${if (guid != null) "\"$guid\"" else null}}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    companion object {
        const val COMICK_URL = "https://api.comick.fun"
        const val CHAPTER_LIST_LIMIT = 500
    }
}

fun getComickSourceName(): String = "Comick" // Renamed from 喜剧演员
fun getUrlWithoutDomain(url: String): String {
    return url.substringAfter("https://comick.fun")
}
fun getHid(url: String): String {
    return url.substringAfterLast("/")
}

fun popularLaunchStrategy(
    page: Int,
    time: String,
    type: String,
    additionalParam: String? = null,
): String {
    return "$POPULAR_LAUNCH_STRATEGY,$page,$time,$type" + (additionalParam?.let { ",$it" } ?: "")
}
const val POPULAR_LAUNCH_STRATEGY = "popularLaunch" // Renamed from POPULAR_LAUNCH

fun genreLaunchStrategy(
    page: Int,
    time: String,
    type: String,
    genre: String,
): String {
    return "$GENRE_LAUNCH_STRATEGY,$page,$time,$type,$genre"
}
const val GENRE_LAUNCH_STRATEGY = "genreLaunch" // Renamed from GENRE_LAUNCH

fun recentLaunchStrategy(page: Int): String {
    return "$RECENT_LAUNCH_STRATEGY,$page"
}
const val RECENT_LAUNCH_STRATEGY = "recentLaunch" // Renamed from RECENT_LAUNCH

fun newLaunchStrategy(page: Int): String {
    return "$NEW_LAUNCH_STRATEGY,$page"
}
const val NEW_LAUNCH_STRATEGY = "newLaunch" // Renamed from NEW_LAUNCH

fun bookmarksLaunchStrategy(page: Int): String {
    return "$BOOKMARKS_LAUNCH_STRATEGY,$page"
}

const val BOOKMARKS_LAUNCH_STRATEGY = "bookmarksLaunch" // Renamed from BOOKMARKS_LAUNCH
