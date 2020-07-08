package eu.kanade.tachiyomi.source.online

import com.github.salomonbrys.kotson.string
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.POSTWithCookie
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.CoverHandler
import eu.kanade.tachiyomi.source.online.handlers.FilterHandler
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaPlusHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.PopularHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.handlers.serializers.ImageReportResult
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ImplicitReflectionSerializer
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder
import java.util.Date
import kotlin.collections.set

open class MangaDex() : HttpSource() {

    private val preferences: PreferencesHelper by injectLazy()

    private val tokenTracker = hashMapOf<String, Long>()

    private fun clientBuilder(): OkHttpClient = clientBuilder(preferences.r18()!!.toInt())

    private fun clientBuilder(
        r18Toggle: Int,
        okHttpClient: OkHttpClient = network.client
    ): OkHttpClient = okHttpClient.newBuilder()
        .addNetworkInterceptor { chain ->
            var originalCookies = chain.request().header("Cookie") ?: ""
            val newReq = chain
                .request()
                .newBuilder()
                .header("Cookie", "$originalCookies; ${cookiesHeader(r18Toggle)}")
                .build()
            chain.proceed(newReq)
        }.build()

    private fun cookiesHeader(r18Toggle: Int): String {
        val cookies = mutableMapOf<String, String>()
        cookies["mangadex_h_toggle"] = r18Toggle.toString()
        return buildCookies(cookies)
    }

    private fun buildCookies(cookies: Map<String, String>) =
        cookies.entries.joinToString(separator = "; ", postfix = ";") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }

    private fun buildR18Client(filters: FilterList): OkHttpClient {
        filters.forEach { filter ->
            when (filter) {
                is FilterHandler.R18 -> {
                    return when (filter.state) {
                        1 -> clientBuilder(ALL)
                        2 -> clientBuilder(ONLY_R18)
                        3 -> clientBuilder(NO_R18)
                        else -> clientBuilder()
                    }
                }
            }
        }
        return clientBuilder()
    }

    override suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        return FollowsHandler(clientBuilder(), headers, preferences).updateFollowStatus(mangaID, followStatus)
    }

    fun fetchRandomMangaId(): Observable<String> {
        return MangaHandler(clientBuilder(), headers, getLangsToShow()).fetchRandomMangaId()
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return PopularHandler(clientBuilder(), headers).fetchPopularManga(page)
    }

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<MangasPage> {
        return SearchHandler(buildR18Client(filters), headers, getLangsToShow()).fetchSearchManga(
            page,
            query,
            filters
        )
    }

    override fun fetchFollows(page: Int): Observable<MangasPage> {
        return FollowsHandler(clientBuilder(), headers, preferences).fetchFollows(page)
    }

    override fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {
        return MangaHandler(clientBuilder(), headers, getLangsToShow()).fetchMangaDetailsObservable(
            manga
        )
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        val manga = MangaHandler(clientBuilder(), headers, getLangsToShow()).fetchMangaDetails(manga)
        if (preferences.forceLatestCovers()) {
            val cover = getLatestCoverUrl(manga)
            manga.thumbnail_url = cover
        }
        return manga
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        val pair = MangaHandler(clientBuilder(), headers, getLangsToShow()).fetchMangaAndChapterDetails(
            manga
        )
        if (preferences.forceLatestCovers()) {
            val cover = getLatestCoverUrl(pair.first)
            pair.first.thumbnail_url = cover
        }

        return pair
    }

    override fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return MangaHandler(
            clientBuilder(),
            headers,
            getLangsToShow()
        ).fetchChapterListObservable(manga)
    }

    suspend fun getMangaIdFromChapterId(urlChapterId: String): Int {
        return MangaHandler(clientBuilder(), headers, getLangsToShow()).getMangaIdFromChapterId(urlChapterId)
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return MangaHandler(clientBuilder(), headers, getLangsToShow()).fetchChapterList(manga)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val imageServer = preferences.imageServer().takeIf { it in SERVER_PREF_ENTRY_VALUES }
            ?: SERVER_PREF_ENTRY_VALUES.first()
        val dataSaver = when (preferences.dataSaver()) {
            true -> "1"
            false -> "0"
        }
        return PageHandler(clientBuilder(), headers, imageServer, dataSaver).fetchPageList(chapter)
    }

    @OptIn(ImplicitReflectionSerializer::class)
    override fun fetchImage(page: Page): Observable<Response> {
        if (page.imageUrl!!.contains("mangaplus", true)) {
            return MangaPlusHandler(nonRateLimitedClient).client.newCall(GET(page.imageUrl!!, headers))
                .asObservableSuccess()
        } else {
            return nonRateLimitedClient.newCallWithProgress(imageRequest(page), page).asObservable().doOnNext { response ->

                val byteSize = response.peekBody(Long.MAX_VALUE).bytes().size
                val result = ImageReportResult(
                    page.imageUrl!!, response.isSuccessful, byteSize
                )

                val jsonString = MdUtil.jsonParser.stringify(ImageReportResult.serializer(), result)

                val postResult = clientBuilder().newCall(
                    POST(
                        MdUtil.reportUrl,
                        headers,
                        jsonString.toRequestBody("application/json".toMediaType())
                    )
                )
                //postResult.execute()

                if (!response.isSuccessful) {
                    response.close()
                    throw Exception("HTTP error ${response.code}")
                }
            }
        }
    }

    fun imageRequest(page: Page): Request {
        val url = when {
            // Legacy
            page.url.isEmpty() -> page.imageUrl!!
            // Some images are hosted elsewhere
            !page.url.startsWith("http") -> baseUrl + page.url.substringBefore(",") + page.imageUrl
            // New chapters on MD servers
            page.url.startsWith(MdUtil.imageUrl) -> page.url.substringBefore(",") + page.imageUrl
            // MD@Home token handling
            else -> {
                val tokenLifespan = 5 * 60 * 1000
                val data = page.url.split(",")
                var tokenedServer = data[0]
                if (Date().time - data[2].toLong() > tokenLifespan) {
                    val tokenRequestUrl = data[1]
                    val cacheControl = if (Date().time - (tokenTracker[tokenRequestUrl] ?: 0) > tokenLifespan) {
                        tokenTracker[tokenRequestUrl] = Date().time
                        CacheControl.FORCE_NETWORK
                    } else {
                        CacheControl.FORCE_CACHE
                    }
                    val jsonData = client.newCall(GET(tokenRequestUrl, headers, cacheControl)).execute().body!!.string()
                    tokenedServer = JsonParser.parseString(jsonData).asJsonObject.get("server").string
                    Timber.d("esco new token %s", tokenedServer)
                }
                tokenedServer + page.imageUrl
            }
        }
        return GET(url, headers)
    }

    override suspend fun fetchAllFollows(forceHd: Boolean): List<SManga> {
        return FollowsHandler(clientBuilder(), headers, preferences).fetchAllFollows(forceHd)
    }

    override suspend fun updateReadingProgress(track: Track): Boolean {
        return FollowsHandler(clientBuilder(), headers, preferences).updateReadingProgress(track)
    }

    override suspend fun fetchTrackingInfo(url: String): Track {
        if (!isLogged()) {
            throw Exception("Not Logged in")
        }
        return FollowsHandler(clientBuilder(), headers, preferences).fetchTrackingInfo(url)
    }

    override fun fetchMangaSimilarObservable(manga: Manga): Observable<MangasPage> {
        return SimilarHandler(preferences).fetchSimilar(manga)
    }

    override suspend fun getLatestCoverUrl(manga: SManga): String {
        val covers = getAllCovers(manga)
        if (covers.isEmpty()) {
            return manga.thumbnail_url!!
        }
        return getAllCovers(manga).last()
    }

    override suspend fun getAllCovers(manga: SManga): List<String> {
        return CoverHandler(clientBuilder(), headers).getCovers(manga)
    }

    override fun isLogged(): Boolean {
        val httpUrl = baseUrl.toHttpUrlOrNull()!!
        return network.cookieManager.get(httpUrl).any { it.name == REMEMBER_ME }
    }

    override suspend fun login(
        username: String,
        password: String,
        twoFactorCode: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val formBody = FormBody.Builder()
                .add("login_username", username)
                .add("login_password", password)
                .add("no_js", "1")
                .add("remember_me", "1")

            twoFactorCode.let {
                formBody.add("two_factor", it)
            }

            val response = clientBuilder().newCall(
                POST(
                    "$baseUrl/ajax/actions.ajax.php?function=login",
                    headers,
                    formBody.build()
                )
            ).execute()
            response.body!!.string().isEmpty()
        }
    }

    override suspend fun logout(): Boolean {
        return withContext(Dispatchers.IO) {
            // https://mangadex.org/ajax/actions.ajax.php?function=logout
            val httpUrl = baseUrl.toHttpUrlOrNull()!!
            val listOfDexCookies = network.cookieManager.get(httpUrl)
            val cookie = listOfDexCookies.find { it.name == REMEMBER_ME }
            val token = cookie?.value
            if (token.isNullOrEmpty()) {
                return@withContext true
            }
            val result = clientBuilder().newCall(
                POSTWithCookie(
                    "$baseUrl/ajax/actions.ajax.php?function=logout",
                    REMEMBER_ME,
                    token,
                    headers
                )
            ).execute()
            val resultStr = result.body!!.string()
            if (resultStr.contains("success", true)) {
                network.cookieManager.remove(httpUrl)
                return@withContext true
            }

            false
        }
    }

    fun getLangsToShow() = preferences.langsToShow().get().split(",")

    override fun getFilterList(): FilterList {
        return FilterHandler().getFilterList()
    }

    companion object {

        // This number matches to the cookie
        private const val NO_R18 = 0
        private const val ALL = 1
        private const val ONLY_R18 = 2
        private const val REMEMBER_ME = "mangadex_rememberme_token"

        val SERVER_PREF_ENTRIES = listOf("Automatic", "NA/EU 1", "NA/EU 2")
        val SERVER_PREF_ENTRY_VALUES = listOf("0", "na", "na2")
    }
}
