package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.POSTWithCookie
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.handlers.*
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.collections.set

open class Mangadex(override val lang: String, private val internalLang: String, private val langCode: Int) : HttpSource() {

    private val preferences: PreferencesHelper by injectLazy()

    private fun clientBuilder(): OkHttpClient = clientBuilder(preferences.r18()!!.toInt())

    private fun clientBuilder(r18Toggle: Int): OkHttpClient = network.client.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                val originalCookies = chain.request().header("Cookie") ?: ""
                val newReq = chain
                        .request()
                        .newBuilder()
                        .header("Cookie", "$originalCookies; ${cookiesHeader(r18Toggle, langCode)}")
                        .build()
                chain.proceed(newReq)
            }.build()


    private fun cookiesHeader(r18Toggle: Int, langCode: Int): String {
        val cookies = mutableMapOf<String, String>()
        cookies["mangadex_h_toggle"] = r18Toggle.toString()
        cookies["mangadex_filter_langs"] = langCode.toString()
        return buildCookies(cookies)
    }

    private fun buildCookies(cookies: Map<String, String>) = cookies.entries.joinToString(separator = "; ", postfix = ";") {
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
        return FollowsHandler(clientBuilder(), headers).updateFollowStatus(mangaID, followStatus)
    }

    fun fetchRandomMangaId(): Observable<String> {
        return MangaHandler(clientBuilder(), headers, internalLang).fetchRandomMangaId()
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return PopularHandler(clientBuilder(), headers).fetchPopularManga(page)

    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return SearchHandler(buildR18Client(filters), headers, internalLang).fetchSearchManga(page, query, filters)

    }

    override fun fetchFollows(page: Int): Observable<MangasPage> {
        return FollowsHandler(clientBuilder(), headers).fetchFollows(page)
    }

    override fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {
        return MangaHandler(clientBuilder(), headers, internalLang).fetchMangaDetailsObservable(manga)
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        return MangaHandler(clientBuilder(), headers, internalLang).fetchMangaDetails(manga)
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        return MangaHandler(clientBuilder(), headers, internalLang).fetchMangaAndChapterDetails(manga)
    }

    override fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return MangaHandler(clientBuilder(), headers, internalLang).fetchChapterListObservable(manga)
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return MangaHandler(clientBuilder(), headers, internalLang).fetchChapterList(manga)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val imageServer = preferences.imageServer().takeIf { it in SERVER_PREF_ENTRY_VALUES }
                ?: SERVER_PREF_ENTRY_VALUES.first()
        return PageHandler(client, headers, imageServer).fetchPageList(chapter)
    }

    override suspend fun fetchAllFollows(): List<SManga> {
        return FollowsHandler(clientBuilder(), headers).fetchAllFollows()
    }

    override suspend fun updateReadingProgress(track: Track): Boolean {
        return FollowsHandler(clientBuilder(), headers).updateReadingProgress(track)
    }


    override suspend fun fetchTrackingInfo(manga: SManga): Track {
        if (!isLogged()) {
            throw Exception("Not Logged in")
        }
        return FollowsHandler(clientBuilder(), headers).fetchTrackingInfo(manga)
    }


    override fun isLogged(): Boolean {
        val httpUrl = baseUrl.toHttpUrlOrNull()!!
        return network.cookieManager.get(httpUrl).any { it.name == REMEMBER_ME }
    }

    override fun login(username: String, password: String, twoFactorCode: String): Observable<Boolean> {
        val formBody = FormBody.Builder()
                .add("login_username", username)
                .add("login_password", password)
                .add("no_js", "1")
                .add("remember_me", "1")

        twoFactorCode.let {
            formBody.add("two_factor", it)
        }

        return clientBuilder().newCall(POST("$baseUrl/ajax/actions.ajax.php?function=login", headers, formBody.build()))
                .asObservable()
                .map { it.body!!.string().isEmpty() }
    }

    override suspend fun logout(): Boolean {
        return withContext(Dispatchers.IO) {
            //https://mangadex.org/ajax/actions.ajax.php?function=logout
            val httpUrl = baseUrl.toHttpUrlOrNull()!!
            val listOfDexCookies = network.cookieManager.get(httpUrl)
            val cookie = listOfDexCookies.find { it.name == REMEMBER_ME }
            val token = cookie?.value
            if (token.isNullOrEmpty()) {
                return@withContext true
            }
            val result = clientBuilder().newCall(POSTWithCookie("$baseUrl/ajax/actions.ajax.php?function=logout", REMEMBER_ME, token, headers)).execute()
            val resultStr = result.body!!.string()
            if (resultStr.contains("success", true)) {
                network.cookieManager.remove(httpUrl)
                return@withContext true
            }

            false
        }
    }

    override fun getFilterList(): FilterList {
        return FilterHandler().getFilterList()
    }

    companion object {

        // This number matches to the cookie
        private const val NO_R18 = 0
        private const val ALL = 1
        private const val ONLY_R18 = 2
        private const val REMEMBER_ME = "mangadex_rememberme_token"

        val SERVER_PREF_ENTRIES = listOf("Automatic", "NA/EU 1", "NA/EU 2", "Rest of the world")
        val SERVER_PREF_ENTRY_VALUES = listOf("0", "na", "na2", "row")
    }
}
