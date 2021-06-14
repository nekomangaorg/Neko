package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.dto.CacheApiMangaSerializer
import eu.kanade.tachiyomi.source.online.handlers.FilterHandler
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.isomorphism.util.TokenBuckets
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.util.Locale
import java.util.concurrent.TimeUnit

open class MangaDexCache : MangaDex() {

    private val db: DatabaseHelper by injectLazy()
    private val downloadManager: DownloadManager by injectLazy()
    private val similarHandler: SimilarHandler by injectLazy()
    private val filterHandler: FilterHandler by injectLazy()
    val preferences: PreferencesHelper by injectLazy()

    // Max request of 30 per second, per domain we query
    private val bucket = TokenBuckets.builder().withCapacity(30)
        .withFixedIntervalRefillStrategy(30, 1, TimeUnit.SECONDS).build()
    private val rateLimitInterceptor = Interceptor {
        bucket.consume()
        it.proceed(it.request())
    }
    private val clientLessRateLimits =
        network.nonRateLimitedClient.newBuilder().addInterceptor(rateLimitInterceptor).build()

    override suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        throw Exception("Cache source cannot update follow status")
    }

    fun fetchPopularManga(page: Int): Observable<MangaListPage> {
        // First check if we have manga to select
        val count = db.getCachedMangaCount().executeAsBlocking()
        if (count == 0) {
            throw Exception("Cache manga db seems empty, try re-downloading it (enable and disable the cache)")
        }

        // Next lets query the next set of manga from the database
        // NOTE: page id starts from 1, and we request 1 extra entry to see if there is still more
        // NOTE: we will also filter out manga that are not in our content rating list the user wants!
        // NOTE: (small hack of using the *rating* field to store the content rating....)
        val limit = 10
        return db.getCachedMangaRange(page - 1, limit).asRxObservable().flatMapIterable { it }
            .map { cacheManga ->
                SManga.create().apply {
                    url = "/manga/${cacheManga.uuid}/"
                    title = MdUtil.cleanString(cacheManga.title)
                    thumbnail_url = MdUtil.imageUrlCacheNotFound
                    rating = cacheManga.rating
                }
            }.toList().map {
                val haveMore = (it.size > limit)
                val mangaListClean = it.take(limit).filter { manga ->
                    manga.rating in preferences.contentRatingSelections()
                }
                mangaListClean.forEach { manga ->
                    manga.rating = null
                }
                MangaListPage(mangaListClean, haveMore)
            }
    }

    fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangaListPage> {
        // First check if we have manga to select
        val count = db.getCachedMangaCount().executeAsBlocking()
        XLog.i("Number of Cached entries: $count")
        if (count == 0) {
            throw Exception("Cache manga db seems empty, try re-downloading it (enable and disable the cache)")
        }

        // Next lets query the next set of manga from the database
        // NOTE: page id starts from 1, and we request 1 extra entry to see if there is still more
        // NOTE: we will also filter out manga that are not in our content rating list the user wants!
        // NOTE: (small hack of using the *rating* field to store the content rating....)
        val limit = 10
        return db.searchCachedManga(query, page - 1, limit).asRxObservable().flatMapIterable { it }
            .map { cacheManga ->
                SManga.create().apply {
                    url = "/manga/${cacheManga.uuid}/"
                    title = MdUtil.cleanString(cacheManga.title)
                    thumbnail_url = MdUtil.imageUrlCacheNotFound
                    rating = cacheManga.rating
                }
            }.toList().map {
                val haveMore = (it.size > limit)
                val mangaListClean = it.take(limit).filter { manga ->
                    manga.rating in preferences.contentRatingSelections()
                }
                mangaListClean.forEach { manga ->
                    manga.rating = null
                }
                MangaListPage(mangaListClean, haveMore)
            }
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        return withContext(Dispatchers.IO) {
            var response = clientLessRateLimits.newCall(apiRequest(manga)).execute()
            parseMangaCacheApi(response)
        }
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        val dbManga = db.getMangadexManga(manga.url).executeAsBlocking()!!
        val dbChapters = if (manga.isMerged()) {
            db.getChaptersByMangaId(dbManga.id!!).executeAsBlocking()
                .filter { downloadManager.isChapterDownloaded(it, dbManga) || it.isMergedChapter() }
        } else {
            db.getChaptersByMangaId(dbManga.id!!).executeAsBlocking()
        }
        // don't replace manga info if it already exists waste of network, and loses the original non cached info
        val mangaToReturn = if (manga.description.isNullOrBlank()) {
            fetchMangaDetails(manga)
        } else {
            dbManga
        }

        return Pair(mangaToReturn, dbChapters)
    }

    override fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return Observable.just(emptyList())
    }

    override suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        throw Exception("Cache source cannot convert chapter id to manga id")
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return emptyList()
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return Observable.just(emptyList())
    }

    override fun fetchImage(page: Page): Observable<Response> {
        throw Exception("Cache source cannot fetch images")
    }

    override fun imageRequest(page: Page): Request {
        throw Exception("Cache source cannot request images")
    }

    override suspend fun fetchAllFollows(forceHd: Boolean): List<SManga> {
        throw Exception("Cache source cannot fetch follows")
    }

    override suspend fun updateReadingProgress(track: Track): Boolean {
        throw Exception("Cache source cannot update reading progress")
    }

    override suspend fun updateRating(track: Track): Boolean {
        throw Exception("Cache source cannot update rating")
    }

    override suspend fun fetchTrackingInfo(url: String): Track {
        return Track.create(TrackManager.MDLIST)
    }

    override fun fetchMangaSimilarObservable(
        manga: Manga,
        refresh: Boolean,
    ): Observable<MangaListPage> {
        return similarHandler.fetchSimilarObserable(manga, refresh)
    }

    override fun isLogged(): Boolean {
        return true
    }

    override suspend fun login(username: String, password: String, twoFactorCode: String): Boolean {
        return true
    }

    override suspend fun logout(): Logout {
        return Logout(true, "Cache source does not have logout")
    }

    override fun getFilterList(): FilterList {
        return FilterList(emptyList())
    }

    private fun apiRequest(manga: SManga): Request {
        val mangaId = MdUtil.getMangaId(manga.url)
        return GET(MdUtil.similarCacheMangaList + mangaId + ".json",
            headers,
            CacheControl.FORCE_NETWORK)
    }

    private fun parseMangaCacheApi(response: Response): SManga {
        // Error check http response
        if (response.code == 404) {
            throw Exception("Manga has not been cached...")
        }
        if (response.isSuccessful.not() || response.code != 200) {
            throw Exception("Error getting cache manga http code: ${response.code}")
        }

        try {
            // Else lets try to parse the response body
            // Serialize the api response
            val jsonData = response.body!!.string()
            val mangaReturn = SManga.create()
            val networkApiManga =
                MdUtil.jsonParser.decodeFromString(CacheApiMangaSerializer.serializer(), jsonData)
            mangaReturn.url = "/manga/${networkApiManga.data.id}"
            // Convert from the api format
            mangaReturn.title = MdUtil.cleanString(networkApiManga.data.attributes.title["en"]!!)
            mangaReturn.description =
                "NOTE: THIS IS A CACHED MANGA ENTRY\n" + MdUtil.cleanDescription(networkApiManga.data.attributes.description["en"]!!)
            // mangaReturn.rating = networkApiManga.toString()
            mangaReturn.thumbnail_url = MdUtil.imageUrlCacheNotFound

            // Get the external tracking ids for this manga
            val networkManga = networkApiManga.data.attributes
            networkManga.links?.let {
                it["al"]?.let { mangaReturn.anilist_id = it }
                it["kt"]?.let { mangaReturn.kitsu_id = it }
                it["mal"]?.let { mangaReturn.my_anime_list_id = it }
                it["mu"]?.let { mangaReturn.manga_updates_id = it }
                it["ap"]?.let { mangaReturn.anime_planet_id = it }
            }
            mangaReturn.status = when (networkManga.status) {
                "ongoing" -> SManga.ONGOING
                "completed" -> SManga.PUBLICATION_COMPLETE
                "cancelled" -> SManga.CANCELLED
                "hiatus" -> SManga.HIATUS
                else -> SManga.UNKNOWN
            }

            // List the labels for this manga
            val tags = filterHandler.getTags()
            val genres = (
                listOf(networkManga.publicationDemographic?.capitalize(Locale.US)) +
                    networkManga.tags?.map { it.id }
                        ?.map { dexTagId -> tags.firstOrNull { tag -> tag.id == dexTagId } }
                        ?.map { tag -> tag?.name } +
                    listOf(
                        "Content Rating - " + (networkManga.contentRating?.capitalize(Locale.US)
                            ?: "Unknown")
                    )
                )
                .filterNotNull()
            mangaReturn.genre = genres.joinToString(", ")

            return mangaReturn
        } catch (e: Exception) {
            XLog.e(e)
            throw e
        }
    }
}
