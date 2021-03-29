package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.FilterHandler
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.handlers.serializers.CacheApiMangaSerializer
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import rx.Observable
import uy.kohesive.injekt.injectLazy
import kotlin.random.Random

open class MangaDexCache() : MangaDex() {

    private val preferences: PreferencesHelper by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    private val clientAnilist = network.client.newBuilder().build()
    private val clientMyAnimeList = network.client.newBuilder().build()

    override suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        throw Exception("Cache source cannot update follow status")
    }

    override fun fetchRandomMangaId(): Observable<String> {
        return Observable.just(Random(1060).nextInt().toString())
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {

        // First check if we have manga to select
        val count = db.getCachedMangaCount().executeAsBlocking()
        if (count == 0) {
            throw Exception("Cache manga db seems empty, try re-downloading it (enable and disable the cache)")
        }

        // Next lets query the next set of manga from the database
        // NOTE: page id starts from 1, and we request 1 extra entry to see if there is still more
        val limit = 16
        return db.getCachedMangaRange(page-1, limit).asRxObservable().flatMapIterable { it }
                .map { cacheManga ->
                    SManga.create().apply {
                        initialized = false
                        url = "/manga/${cacheManga.mangaId}"
                        title = MdUtil.cleanString(cacheManga.title)
                        thumbnail_url = null
                    }
                }.toList().map { MangasPage(it.take(limit-1), it.size == limit) }

    }

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<MangasPage> {
        val count = db.getCachedMangaCount().executeAsBlocking()
        XLog.i("Number of Cached entries: $count")
        if (count == 0) {
            throw Exception("Cache manga db seems empty, try re-downloading it (enable and disable the cache)")
        }

        // Next lets query the next set of manga from the database
        // NOTE: page id starts from 1, and we request 1 extra entry to see if there is still more
        val limit = 16
        return db.searchCachedManga(query, page-1, limit).asRxObservable().flatMapIterable { it }
            .map { cacheManga ->
                SManga.create().apply {
                    initialized = false
                    url = "/manga/${cacheManga.mangaId}"
                    title = MdUtil.cleanString(cacheManga.title)
                    thumbnail_url = null
                }
            }.toList().map { MangasPage(it.take(limit-1), it.size == limit) }
    }

    override fun fetchFollows(): Observable<MangasPage> {
        throw Exception("Cache source cannot get follows")
    }

    override fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {
        return client.newCall(apiRequest(manga))
            .asObservableSuccess()
            .map { response ->
                parseMangaCacheApi(response.body!!.string())
            }
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        return withContext(Dispatchers.IO) {
            var response = client.newCall(apiRequest(manga)).execute()
            if (!response.isSuccessful) {
                response = client.newCall(apiRequest(manga, true)).execute()
            }
            parseMangaCacheApi(response.body!!.string())
        }
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        val id = MdUtil.getMangaId(manga.url).toLong()
        val dbChapters = db.getChaptersByMangaId(id).executeAsBlocking()
        return Pair(fetchMangaDetails(manga), dbChapters)
    }

    override fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return Observable.just(emptyList())
    }

    override suspend fun getMangaIdFromChapterId(urlChapterId: String): Int {
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

    override fun fetchMangaSimilarObservable(manga: Manga): Observable<MangasPage> {
        return SimilarHandler(preferences).fetchSimilar(manga)
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

    private fun apiRequest(manga: SManga, useOtherUrl: Boolean = true): Request {
        val mangaId = MdUtil.getMangaId(manga.url).toLong()
        val url = when {
            useOtherUrl -> MdUtil.apiUrlCache
            else -> MdUtil.apiUrlCdnCache
        }
        return GET(url + mangaId.toString().padStart(5, '0') + ".json", headers, CacheControl.FORCE_NETWORK)
    }

    private fun parseMangaCacheApi(jsonData: String): SManga {
        try {
            // Serialize the api response
            val mangaReturn = SManga.create()
            val networkApiManga = MdUtil.jsonParser.decodeFromString(CacheApiMangaSerializer.serializer(), jsonData)
            mangaReturn.url = "/manga/${networkApiManga.id}"
            // Convert from the api format
            mangaReturn.title = MdUtil.cleanString(networkApiManga.title)
            mangaReturn.description = "NOTE: THIS IS A CACHED MANGA ENTRY\n" + MdUtil.cleanDescription(networkApiManga.description)
            mangaReturn.rating = networkApiManga.rating.toString()

            // Get the external tracking ids for this manga
            networkApiManga.external["al"].let {
                mangaReturn.anilist_id = it
            }
            networkApiManga.external["kt"].let {
                mangaReturn.kitsu_id = it
            }
            networkApiManga.external["mal"].let {
                mangaReturn.my_anime_list_id = it
            }
            networkApiManga.external["mu"].let {
                mangaReturn.manga_updates_id = it
            }
            networkApiManga.external["ap"].let {
                mangaReturn.anime_planet_id = it
            }
            mangaReturn.status = SManga.UNKNOWN

            // List the labels for this manga
            val genres = networkApiManga.demographic.toMutableList()
            genres += networkApiManga.content
            genres += networkApiManga.format
            genres += networkApiManga.genre
            genres += networkApiManga.theme
            if (networkApiManga.is_r18) {
                genres.add("Hentai")
            }
            mangaReturn.genre = genres.joinToString(", ")

            mangaReturn.thumbnail_url = getThumbnail(mangaReturn.anilist_id, mangaReturn.my_anime_list_id)

            return mangaReturn
        } catch (e: Exception) {
            XLog.e(e)
            throw e
        }
    }

    fun getThumbnail(anilist_id: String?, my_anime_list_id: String?): String {
        // Query graph ql endpoint for our image
        // https://stackoverflow.com/a/58923947
        if (anilist_id != null) {
            val query = "{\n" +
                "  Media(id: ${anilist_id}, type: MANGA) {\n" +
                "    coverImage {\n" +
                "      extraLarge\n" +
                "      large\n" +
                "    }\n" +
                "  }\n" +
                "}\n"
            val json = JSONObject()
            json.put("query", query)
            val requestBody = json.toString().toRequestBody(null)
            val request = Request.Builder().url("https://graphql.anilist.co").post(requestBody)
                .addHeader("content-type", "application/json").build()
            val response = clientAnilist.newCall(request).execute()
            if(response.isSuccessful && response.code == 200) {
                val data = JSONObject(response.body!!.string())
                return data.getJSONObject("data")
                        .getJSONObject("Media")
                        .getJSONObject("coverImage")
                        .getString("extraLarge")
            }
        }

        // Query MAL api for an image
        // https://jikan.docs.apiary.io/#reference/0/manga
        if (my_anime_list_id != null) {
            val request = GET("https://api.jikan.moe/v3/manga/${my_anime_list_id}/pictures", headers, CacheControl.FORCE_NETWORK)
            val response = clientMyAnimeList.newCall(request).execute()
            if(response.isSuccessful && response.code == 200) {
                val data = JSONObject(response.body!!.string())
                val pictures = data.getJSONArray("pictures")
                if (pictures.length() > 0) {
                    return pictures.getJSONObject(pictures.length() - 1).getString("large")
                }
            }
        }
        return MdUtil.imageUrlCacheNotFound
    }
}