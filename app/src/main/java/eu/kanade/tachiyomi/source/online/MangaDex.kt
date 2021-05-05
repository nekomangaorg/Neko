package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
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
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.util.Date
import kotlin.collections.set

open class MangaDex : HttpSource() {

    private val preferences: PreferencesHelper by injectLazy()

    private val filterHandler = FilterHandler(preferences)

    private val loginHelper = MangaDexLoginHelper(network.client, preferences)

    // chapter url where we get the token, last request time
    private val tokenTracker = hashMapOf<String, Long>()

    override suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        return FollowsHandler(network.client, headers, preferences).updateFollowStatus(mangaID, followStatus)
    }

    open fun fetchRandomMangaId(): Observable<String> {
        return MangaHandler(network.client, headers, filterHandler, getLangsToShow()).fetchRandomMangaId()
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return PopularHandler(network.client, headers).fetchPopularManga(page)
    }

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<MangasPage> {
        return SearchHandler(network.client, headers, getLangsToShow(), filterHandler).fetchSearchManga(
            page,
            query,
            filters
        )
    }

    override fun fetchFollows(): Observable<MangasPage> {
        return FollowsHandler(network.client, headers, preferences).fetchFollows()
    }

    override fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {
        return MangaHandler(network.client, headers, filterHandler, getLangsToShow(), preferences.forceLatestCovers()).fetchMangaDetailsObservable(
            manga
        )
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        return MangaHandler(network.client, headers, filterHandler, getLangsToShow(), preferences.forceLatestCovers()).fetchMangaDetails(manga)
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        val pair = MangaHandler(network.client, headers, filterHandler, getLangsToShow(), preferences.forceLatestCovers()).fetchMangaAndChapterDetails(
            manga
        )
        return pair
    }

    override fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return MangaHandler(
            network.client,
            headers,
            filterHandler,
            getLangsToShow(),
        ).fetchChapterListObservable(manga)
    }

    open suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return MangaHandler(network.client, headers, filterHandler, getLangsToShow()).getMangaIdFromChapterId(urlChapterId)
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return MangaHandler(network.client, headers, filterHandler, getLangsToShow()).fetchChapterList(manga)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return PageHandler(network.client, headers, preferences.dataSaver()).fetchPageList(chapter)
    }

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

                val jsonString = MdUtil.jsonParser.encodeToString(ImageReportResult.serializer(), result)

                val postResult = network.client.newCall(
                    POST(
                        MdUtil.reportUrl,
                        headers,
                        jsonString.toRequestBody("application/json".toMediaType())
                    )
                )
                try {
                    postResult.execute()
                } catch (e: Exception) {
                    Timber.e(e, "error trying to post to dex@home")
                }

                if (!response.isSuccessful) {
                    response.close()
                    throw Exception("HTTP error ${response.code}")
                }
            }
        }
    }

    open fun imageRequest(page: Page): Request {
        val data = page.url.split(",")
        val mdAtHomeServerUrl =
            when (Date().time - data[2].toLong() > MdUtil.mdAtHomeTokenLifespan) {
                false -> data[0]
                true -> {
                    val tokenRequestUrl = data[1]
                    val cacheControl =
                        if (Date().time - (
                                tokenTracker[tokenRequestUrl]
                                    ?: 0
                                ) > MdUtil.mdAtHomeTokenLifespan
                        ) {
                            tokenTracker[tokenRequestUrl] = Date().time
                            CacheControl.FORCE_NETWORK
                        } else {
                            CacheControl.FORCE_CACHE
                        }
                    MdUtil.atHomeUrlHostUrl(tokenRequestUrl, client)
                }
            }
        return GET(mdAtHomeServerUrl + page.imageUrl, headers)
    }

    override suspend fun fetchAllFollows(forceHd: Boolean): List<SManga> {
        return FollowsHandler(network.client, headers, preferences).fetchAllFollows()
    }

    open suspend fun updateReadingProgress(track: Track): Boolean {
        return FollowsHandler(network.client, headers, preferences).updateReadingProgress(track)
    }

    open suspend fun updateRating(track: Track): Boolean {
        return FollowsHandler(network.client, headers, preferences).updateRating(track)
    }

    override suspend fun fetchTrackingInfo(url: String): Track {
        if (!isLogged()) {
            throw Exception("Not Logged in to MangaDex")
        }
        return FollowsHandler(network.client, headers, preferences).fetchTrackingInfo(url)
    }

    override fun fetchMangaSimilarObservable(manga: Manga): Observable<MangasPage> {
        return SimilarHandler(preferences).fetchSimilar(manga)
    }

    override fun isLogged(): Boolean {
        return preferences.sourcePassword(this) != null
    }

    override suspend fun login(
        username: String,
        password: String,
        twoFactorCode: String
    ): Boolean {
        return loginHelper.login(username, password)
    }

    suspend fun checkIfUp(): Boolean {
        return withContext(Dispatchers.IO) {
            true
            // val response = network.client.newCall(GET(MdUtil.apiUrl + MdUtil.apiManga + 1)).await()
            // response.isSuccessful
        }
    }

    override suspend fun logout(): Logout {
        return withContext(Dispatchers.IO) {

            val catch = runCatching {
                val result = network.client.newCall(
                    POST(
                        MdUtil.logoutUrl,
                        MdUtil.getAuthHeaders(headers, preferences)
                    )
                ).await()
            }
            return@withContext Logout(false, "Unknown error")
        }
    }

    fun getLangsToShow() = preferences.langsToShow().get().split(",")

    override fun getFilterList(): FilterList {
        return filterHandler.getMDFilterList()
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
