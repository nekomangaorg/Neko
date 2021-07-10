package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.FilterHandler
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.ImageHandler
import eu.kanade.tachiyomi.source.online.handlers.LatestChapterHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

open class MangaDex : HttpSource() {

    private val preferences: PreferencesHelper by injectLazy()

    private val filterHandler: FilterHandler by injectLazy()

    private val followsHandler: FollowsHandler by injectLazy()

    private val mangaHandler: MangaHandler by injectLazy()

    private val searchHandler: SearchHandler by injectLazy()

    private val pageHandler: PageHandler by injectLazy()

    private val imageHandler: ImageHandler by injectLazy()

    private val similarHandler: SimilarHandler by injectLazy()

    private val loginHelper: MangaDexLoginHelper by injectLazy()

    private val latestChapterHandler: LatestChapterHandler by injectLazy()

    suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
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

    suspend fun search(page: Int, query: String, filters: FilterList): MangaListPage {
        return searchHandler.search(page, query, filters)
    }

    suspend fun latestChapters(page: Int): MangaListPage {
        return latestChapterHandler.getPage(page)
    }

    suspend fun fetchFollowList(): MangaListPage {
        return followsHandler.fetchFollows()
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        return mangaHandler.fetchMangaDetails(manga)
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        return mangaHandler.fetchMangaAndChapterDetails(manga)
    }

    open suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return mangaHandler.getMangaIdFromChapterId(urlChapterId)
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return mangaHandler.fetchChapterList(manga)
    }

    override suspend fun fetchPageList(chapter: SChapter): List<Page> {
        return pageHandler.fetchPageList(chapter, isLogged())
    }

    override suspend fun fetchImage(page: Page): Response {
        return imageHandler.getImage(page, isLogged())
    }

    suspend fun fetchAllFollows(): List<SManga> {
        return followsHandler.fetchAllFollows()
    }

    open suspend fun updateReadingProgress(track: Track): Boolean {
        return followsHandler.updateReadingProgress(track)
    }

    open suspend fun updateRating(track: Track): Boolean {
        return followsHandler.updateRating(track)
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        if (!isLogged()) {
            throw Exception("Not Logged in to MangaDex")
        }
        return followsHandler.fetchTrackingInfo(url)
    }

    suspend fun fetchSimilarManga(
        manga: Manga,
        refresh: Boolean,
    ): MangaListPage {
        return similarHandler.fetchSimilarManga(manga, refresh)
    }

    override fun isLogged(): Boolean {
        return preferences.sourceUsername(this).isNullOrBlank().not() && preferences.sourcePassword(
            this
        ).isNullOrBlank().not() && preferences.sessionToken().isNullOrBlank().not() &&
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
            network.authService.logout()
            return@withContext Logout(true)
        }
    }

    override val headers: Headers
        get() = network.headers

    override fun getFilterList(): FilterList {
        return filterHandler.getMDFilterList()
    }
}
