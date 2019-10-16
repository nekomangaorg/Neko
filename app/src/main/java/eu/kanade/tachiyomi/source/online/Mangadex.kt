package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.handlers.*
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

    private fun clientBuilder(r18Toggle: Int): OkHttpClient = network.cloudflareClient.newBuilder()
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


    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return PopularHandler(clientBuilder(), headers).fetchPopularManga(page)

    }


    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return SearchHandler(buildR18Client(filters), headers, internalLang).fetchSearchManga(page, query, filters)

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


    override fun fetchFollows(page: Int): Observable<MangasPage> {
        return FollowsHandler(clientBuilder(), headers).fetchFollows(page)
    }


    fun changeFollowStatus(manga: SManga): Observable<Boolean> {
        return FollowsHandler(clientBuilder(), headers).changeFollowStatus(manga)
    }

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return MangaHandler(clientBuilder(), headers, internalLang).fetchMangaDetails(manga)
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return MangaHandler(clientBuilder(), headers, internalLang).fetchChapterList(manga)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return PageHandler(client, headers, preferences.imageServer().toString()).fetchPageList(chapter)
    }


    override fun isLogged(): Boolean {
        val httpUrl = baseUrl.toHttpUrlOrNull()!!
        return network.cookieManager.get(httpUrl).any { it.name == "mangadex_rememberme_token" }
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

    override fun getFilterList(): FilterList {
        return FilterHandler().getFilterList()
    }

    companion object {

        // This number matches to the cookie
        private const val NO_R18 = 0
        private const val ALL = 1
        private const val ONLY_R18 = 2


    }
}
