package eu.kanade.tachiyomi.source.online.merged.comick

import eu.kanade.tachiyomi.network.POST // For login and other POST requests
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SimpleChapter
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import com.github.michaelbull.result.Result // Changed import
import com.github.michaelbull.result.Ok // Changed import
import com.github.michaelbull.result.Err // Changed import
import eu.kanade.tachiyomi.util.system.ResultError // Keep ResultError for the E type
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy
// SimpleDateFormat and Locale might be needed for chapter date parsing in new fetchChapters
import java.text.SimpleDateFormat
import java.util.Locale

class Comick(
    override val lang: String = "en",
    private val customBaseUrl: String? = null
) : ReducedHttpSource() {

    override val name = "Comick"
    override val baseUrl = customBaseUrl ?: COMICK_URL // Use customBaseUrl if provided
    override val supportsLatest = false // Changed as per instruction

    private val json: Json by injectLazy()
    // Removed: private val comickApi = ComickApi(client, lang)

    // --- Methods moved from ComickApi ---

    private suspend fun getComicDto(slug: String): ComickComic { // Renamed to avoid clash if a direct SManga getter is made
        val url = "$baseUrl/comic/$slug".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun getChapterListDto(comicHid: String, page: Int): ChapterList { // Renamed
        val url = "$baseUrl/comic/$comicHid/chapters?lang=$lang&page=$page&limit=$CHAPTER_LIST_LIMIT&sort=chap".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    // This is the actual implementation for the abstract getPageList
    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val chapterHid = getHid(chapter.url) // getHid will be moved here
        val url = "$baseUrl/chapter/$chapterHid".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        val chapterData = json.decodeFromString<PageList>(response.body.string()).chapter
        return chapterData.images.mapIndexed { i, image ->
            if (image.b2key != null) {
                Page(index = i, imageUrl = "https://meo.comick.pictures/${image.b2key}", url = "")
            } else {
                Page(index = i, imageUrl = null, url = "")
            }
        }
    }


    private suspend fun getTopComicsDto( // Renamed
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

    private suspend fun searchDto( // Renamed
        query: String,
        page: Int, // page might not be used with ReducedHttpSource's searchManga directly
        genre: String = "all",
        demographic: String = "all",
        completed: Boolean? = null,
    ): List<SearchResponse> {
        val url = "$baseUrl/v1.0/search".toHttpUrl().newBuilder().apply {
            addQueryParameter("q", query)
            addQueryParameter("page", page.toString()) // Keep page for now, might be fixed to 1 or removed
            addQueryParameter("limit", "20") // This limit is for Comick's API
            if (genre != "all") addQueryParameter("genres", genre)
            if (demographic != "all") addQueryParameter("demographics", demographic)
            if (completed != null) addQueryParameter("completed", completed.toString())
        }.build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun getGenresDto(): List<Genre> { // Renamed
        val url = "$baseUrl/genre".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun getDemographicsDto(): List<Demographic> { // Renamed
        val url = "$baseUrl/demographic".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun getIdentityDto(): Identity { // Renamed
        val url = "$baseUrl/auth/me".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun loginDto(token: String): Identity { // Renamed
        val url = "$baseUrl/auth/token".toHttpUrl()
        val requestBody = """{"token": "$token"}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun getBookmarksDto(page: Int): List<Bookmark> { // Renamed
        val url = "$baseUrl/me/bookmarks?type=comic&page=$page".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun addBookmarkDto(comicHid: String): RequestResult { // Renamed
        val url = "$baseUrl/comic/$comicHid/bookmark".toHttpUrl()
        val request = Request.Builder().url(url).post("".toRequestBody()).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun removeBookmarkDto(comicHid: String): RequestResult { // Renamed
        val url = "$baseUrl/comic/$comicHid/unbookmark".toHttpUrl()
        val request = Request.Builder().url(url).post("".toRequestBody()).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun getReadMarkersDto(comicHids: List<String>): List<ReadMarker> { // Renamed
        val url = "$baseUrl/me/read-markers".toHttpUrl()
        val requestBody = """{"comics": ${comicHids.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    private suspend fun markChapterAsReadDto(chapterHid: String, comicHid: String, guid: String? = null): RequestResult { // Renamed
        val url = "$baseUrl/chapter/$chapterHid/read".toHttpUrl()
        val requestBody = """{"comic_hid": "$comicHid", "guid": ${if (guid != null) "\"$guid\"" else null}}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = client.newCall(request).awaitSuccess()
        return json.decodeFromString(response.body.string())
    }

    // --- End of methods moved from ComickApi ---


    override suspend fun searchManga(query: String): List<SManga> {
        return try {
            // ReducedHttpSource searchManga does not have page or filters.
            // We'll use default page 1 and no filters for now.
            // Genre, demographic, completed filters were available in old search.
            // These are not directly supported by ReducedHttpSource.searchManga.
            // For a basic implementation, we ignore them.
            val searchResults = searchDto(query = query, page = 1) // Default to page 1
            searchResults.map { dto ->
                SManga.create().apply {
                    title = dto.title
                    url = getUrlWithoutDomain("/comic/${dto.slug}")
                    thumbnail_url = dto.mdCovers.firstOrNull()?.gpurl // Corrected: Use extension property directly
                    // SManga from search is typically not initialized
                    initialized = false
                }
            }
        } catch (e: Exception) {
            // Log error e
            emptyList()
        }
    }

    override suspend fun fetchChapters(mangaUrl: String): Result<List<SChapter>, ResultError> { // Return type uses the new Result
        return try {
            val comicHid = getHid(mangaUrl)
            val chaptersList = mutableListOf<ComickChapter>()
            var page = 1
            var hasMore = true

            while (hasMore) {
                val result = getChapterListDto(comicHid, page)
                chaptersList.addAll(result.chapters)
                hasMore = chaptersList.size < result.total
                if (hasMore) {
                    page++
                }
            }

            Ok( // Use Ok from michaelbull
                chaptersList.map { chapter ->
                    SChapter.create().apply {
                        name = chapter.title ?: "Chapter ${chapter.chap ?: chapter.id}"
                        url = getUrlWithoutDomain("/chapter/${chapter.hid}")
                        date_upload = chapter.createdAt?.let {
                            try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(it)?.time } catch (_: Exception) { 0L }
                        } ?: 0L
                        chapter_number = chapter.chap?.toFloatOrNull() ?: -1f
                        scanlator = chapter.groupName?.joinToString()
                    }
                }
            )
        } catch (e: Exception) {
            Err(ResultError.Network(e.message ?: "Unknown error during chapter fetch")) // Use Err from michaelbull
        }
    }

    override fun imageRequest(page: Page): Request {
        val imageHeaders = headers.newBuilder()
            .add("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .add("Referer", "$baseUrl/")
            // Host might be derived from page.imageUrl if it's a full URL from a different domain
            // If using meo.comick.pictures, then:
            .add("Host", "meo.comick.pictures")
            .build()
        return Request.Builder()
            .url(page.imageUrl!!)
            .headers(imageHeaders)
            .build()
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        // Assuming simpleChapter.url is the relative URL (/chapter/hid)
        return baseUrl + simpleChapter.url
    }


    // --- Utility functions moved from ComickApi ---
    // These can be private or internal as needed.
    private fun getUrlWithoutDomain(url: String): String {
        return url.substringAfter("https://comick.fun") // COMICK_URL's host
    }

    private fun getHid(url: String): String {
        return url.substringAfterLast("/")
    }
    // Strategy functions - evaluate if needed for new method implementations
    // For now, keep them private.
    private fun popularLaunchStrategy(page: Int, time: String, type: String, additionalParam: String? = null): String {
        return "$POPULAR_LAUNCH_STRATEGY,$page,$time,$type" + (additionalParam?.let { ",$it" } ?: "")
    }
    private fun genreLaunchStrategy(page: Int, time: String, type: String, genre: String): String {
        return "$GENRE_LAUNCH_STRATEGY,$page,$time,$type,$genre"
    }
    private fun recentLaunchStrategy(page: Int): String {
        return "$RECENT_LAUNCH_STRATEGY,$page"
    }
    private fun newLaunchStrategy(page: Int): String {
        return "$NEW_LAUNCH_STRATEGY,$page"
    }
    private fun bookmarksLaunchStrategy(page: Int): String {
        return "$BOOKMARKS_LAUNCH_STRATEGY,$page"
    }


    companion object {
        const val COMICK_URL = "https://api.comick.fun"
        const val CHAPTER_LIST_LIMIT = 500

        // For popularLaunchStrategy etc. - evaluate if needed
        private const val POPULAR_LAUNCH_STRATEGY = "popularLaunch"
        private const val GENRE_LAUNCH_STRATEGY = "genreLaunch"
        private const val RECENT_LAUNCH_STRATEGY = "recentLaunch"
        private const val NEW_LAUNCH_STRATEGY = "newLaunch"
        private const val BOOKMARKS_LAUNCH_STRATEGY = "bookmarksLaunch"

        // getComickSourceName might not be needed if Comick.name is sufficient.
        // fun getComickSourceName(): String = "Comick"


        internal val MDcovers.gpurl: String?
            get() {
                return this.b2key?.let { "https://meo.comick.pictures/$it" }
            }
    }
}
