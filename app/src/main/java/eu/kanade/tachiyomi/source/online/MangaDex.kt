package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
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
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.dto.ImageReportResultDto
import eu.kanade.tachiyomi.source.online.handlers.FilterHandler
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaPlusHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.PopularHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
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

    private val filterHandler: FilterHandler by injectLazy()

    private val followsHandler: FollowsHandler by injectLazy()

    private val mangaHandler: MangaHandler by injectLazy()

    private val popularHandler: PopularHandler by injectLazy()

    private val searchHandler: SearchHandler by injectLazy()

    private val pageHandler: PageHandler by injectLazy()

    private val similarHandler: SimilarHandler by injectLazy()

    private val loginHelper: MangaDexLoginHelper by injectLazy()

    private val mangaPlusHandler: MangaPlusHandler by injectLazy()

    // chapter url where we get the token, last request time
    private val tokenTracker = hashMapOf<String, Long>()

    override suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        return followsHandler.updateFollowStatus(mangaID, followStatus)
    }

    fun getRandomManga(): Flow<SManga?> {
        return flow {
            if (network.service.randomManga().isSuccessful) {
                emit(network.service.randomManga().body()!!.toBasicManga())
            } else {
                emit(null)
            }
        }.catch { e ->
            XLog.e("error getting random manga", e)
            emit(null)
        }.flowOn(Dispatchers.IO)
    }

    override fun fetchPopularManga(page: Int): Observable<MangaListPage> {
        return popularHandler.fetchPopularManga(page)
    }

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangaListPage> {
        return searchHandler.fetchSearchManga(
            page,
            query,
            filters
        )
    }

    override fun fetchFollows(): Observable<MangaListPage> {
        return followsHandler.fetchFollows()
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        return mangaHandler.fetchMangaDetails(manga)
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        return mangaHandler.fetchMangaAndChapterDetails(manga)
    }

    override fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return mangaHandler.fetchChapterListObservable(manga)
    }

    open suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return mangaHandler.getMangaIdFromChapterId(urlChapterId)
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return mangaHandler.fetchChapterList(manga)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return pageHandler.fetchPageList(chapter, isLogged())
    }

    override fun fetchImage(page: Page): Observable<Response> {
        if (page.imageUrl!!.contains("mangaplus", true)) {
            return mangaPlusHandler.client.newCall(GET(page.imageUrl!!, headers))
                .asObservableSuccess()
        } else {
            return nonRateLimitedClient.newCallWithProgress(imageRequest(page), page).asObservable()
                .doOnNext { response ->

                    val byteSize = response.peekBody(Long.MAX_VALUE).bytes().size
                    val duration = response.receivedResponseAtMillis - response.sentRequestAtMillis
                    val cache = response.header("X-Cache", "") == "HIT"
                    val result = ImageReportResultDto(
                        page.imageUrl!!,
                        response.isSuccessful,
                        byteSize,
                        cache,
                        duration
                    )

                    val jsonString = MdUtil.jsonParser.encodeToString(result)

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
        val atHomeHeaders = if (isLogged()) {
            MdUtil.getAuthHeaders(headers, preferences)
        } else {
            network.headers
        }

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
                    MdUtil.atHomeUrlHostUrl(tokenRequestUrl, client, atHomeHeaders, cacheControl)
                }
            }
        return GET(mdAtHomeServerUrl + page.imageUrl, headers)
    }

    override suspend fun fetchAllFollows(forceHd: Boolean): List<SManga> {
        return followsHandler.fetchAllFollows()
    }

    open suspend fun updateReadingProgress(track: Track): Boolean {
        return followsHandler.updateReadingProgress(track)
    }

    open suspend fun updateRating(track: Track): Boolean {
        return followsHandler.updateRating(track)
    }

    override suspend fun fetchTrackingInfo(url: String): Track {
        if (!isLogged()) {
            throw Exception("Not Logged in to MangaDex")
        }
        return followsHandler.fetchTrackingInfo(url)
    }

    override fun fetchMangaSimilarObservable(
        manga: Manga,
        refresh: Boolean,
    ): Observable<MangaListPage> {
        return similarHandler.fetchSimilarObserable(manga, refresh)
    }

    override fun isLogged(): Boolean {
        return preferences.sourceUsername(this).isNullOrBlank().not() && preferences.sourcePassword(
            this).isNullOrBlank().not() && preferences.sessionToken().isNullOrBlank().not() &&
            preferences.refreshToken().isNullOrBlank().not()
    }

    override suspend fun login(
        username: String,
        password: String,
        twoFactorCode: String,
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
            network.client.newCall(
                POST(
                    MdUtil.logoutUrl,
                    MdUtil.getAuthHeaders(headers, preferences)
                )
            ).await()
            return@withContext Logout(true)
        }
    }

    override fun getFilterList(): FilterList {
        return filterHandler.getMDFilterList()
    }
}
