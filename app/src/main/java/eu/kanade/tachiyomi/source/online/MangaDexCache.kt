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
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.handlers.serializers.CacheApiMangaSerializer
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import rx.Observable
import uy.kohesive.injekt.injectLazy

open class MangaDexCache() : MangaDex() {

    private val preferences: PreferencesHelper by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    private val clientAnilist = network.client.newBuilder().build()
    private val clientMyAnimeList = network.client.newBuilder().build()

    override suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        throw Exception("Cache source cannot update follow status")
    }

    override fun fetchRandomMangaId(): Observable<String> {
        throw Exception("Cache source cannot get random manga")
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        throw Exception("Cache source cannot get popular manga")
    }

    override fun fetchSearchManga(
            page: Int,
            query: String,
            filters: FilterList
    ): Observable<MangasPage> {
        throw Exception("Cache source cannot search")
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
            val response = client.newCall(apiRequest(manga)).execute()
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

    private fun apiRequest(manga: SManga): Request {
        val mangaId = MdUtil.getMangaId(manga.url).toLong()
        return GET(MdUtil.apiUrlCache+mangaId.toString().padStart(5,'0')+".json", headers, CacheControl.FORCE_NETWORK)
    }

    private fun parseMangaCacheApi(jsonData : String) : SManga {
        try {

            // Serialize the api response
            val mangaReturn = SManga.create()
            val networkApiManga = MdUtil.jsonParser.decodeFromString(CacheApiMangaSerializer.serializer(), jsonData)

            // Convert from the api format
            mangaReturn.title = MdUtil.cleanString(networkApiManga.title)
            mangaReturn.description = MdUtil.cleanDescription(networkApiManga.description)
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

            // Query graph ql endpoint for our image
            // https://stackoverflow.com/a/58923947
            if(mangaReturn.anilist_id != null && mangaReturn.thumbnail_url == null) {
                val query = "{\n" +
                        "  Media(id: ${mangaReturn.anilist_id}, type: MANGA) {\n" +
                        "    coverImage {\n" +
                        "      extraLarge\n" +
                        "      large\n" +
                        "    }\n" +
                        "  }\n" +
                        "}\n"
                val json = JSONObject()
                json.put("query",query)
                val requestBody = json.toString().toRequestBody(null)
                val request = Request.Builder().url("https://graphql.anilist.co").post(requestBody)
                        .addHeader("content-type", "application/json").build()
                val response = clientAnilist.newCall(request).execute()
                val data = JSONObject(response.body!!.string())
                mangaReturn.thumbnail_url = data.getJSONObject("data")
                        .getJSONObject("Media")
                        .getJSONObject("coverImage")
                        .getString("extraLarge")
            }

            // Query MAL api for an image
            // https://jikan.docs.apiary.io/#reference/0/manga
            if(mangaReturn.my_anime_list_id != null && mangaReturn.thumbnail_url == null) {
                val request = GET("https://api.jikan.moe/v3/manga/${mangaReturn.my_anime_list_id}/pictures",headers, CacheControl.FORCE_NETWORK)
                val response = clientMyAnimeList.newCall(request).execute()
                val data = JSONObject(response.body!!.string())
                val pictures = data.getJSONArray("pictures")
                if(pictures.length() > 0) {
                    mangaReturn.thumbnail_url = pictures.getJSONObject(pictures.length()-1).getString("large")
                }
            }

            return mangaReturn
        } catch (e: Exception) {
            XLog.e(e)
            throw e
        }
    }


}