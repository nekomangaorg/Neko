package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.SimilarMangaResponse
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.v5.db.V5DbHelper
import eu.kanade.tachiyomi.v5.db.V5DbQueries
import kotlinx.serialization.decodeFromString
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

class SimilarHandler {

    private val network: NetworkHelper by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    private val v5DbHelper: V5DbHelper by injectLazy()

    /**
     * fetch our similar mangas
     */
    fun fetchSimilarObserable(manga: Manga, refresh: Boolean): Observable<MangasPage> {
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        if (mangaDb != null && !refresh) {
            return Observable.just(similarStringToMangasPage(manga, mangaDb.data))
        }
        return network.client.newCall(similarMangaRequest(manga))
            .asObservableSuccess()
            .map { response ->
                similarMangaParse(manga, response)
            }
    }

    private fun similarMangaRequest(manga: Manga): Request {
        val tempUrl = MdUtil.similarBaseApi + MdUtil.getMangaId(manga.url) + ".json"
        return GET(tempUrl, network.headers, CacheControl.FORCE_NETWORK)
    }

    private fun similarMangaParse(manga: Manga, response: Response): MangasPage {
        // Error check http response
        if (response.code == 404) {
            return MangasPage(emptyList(), false)
        }
        if (response.isSuccessful.not() || response.code != 200) {
            throw Exception("Error getting search manga http code: ${response.code}")
        }
        // Get our page of mangas
        val bodyData = response.body!!.string()
        val mangaPages = similarStringToMangasPage(manga, bodyData)
        // Insert into our database and return
        val mangaSimilar = MangaSimilar.create().apply {
            manga_id = MdUtil.getMangaId(manga.url)
            data = bodyData
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaPages
    }

    private fun similarStringToMangasPage(manga: Manga, data: String): MangasPage {
        // TODO: also filter based on the content rating here?
        // TODO: also append here the related manga?
        val mlResponse = MdUtil.jsonParser.decodeFromString<SimilarMangaResponse>(data)
        val mangaList = mlResponse.matches.map {
            SManga.create().apply {
                url = "/title/" + it.id
                title = MdUtil.cleanString(it.title["en"]!!)
                thumbnail_url = V5DbQueries.getAltCover(v5DbHelper.dbCovers, it.id) ?: MdUtil.imageUrlCacheNotFound
            }
        }
        return MangasPage(mangaList, false)
    }
}
