package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.database.models.Manga
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
    private val v5DbHelper: V5DbHelper by injectLazy()

    /**
     * fetch our similar mangas
     */
    fun fetchSimilar(manga: Manga): Observable<MangasPage> {
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
        if (response.code == 404) {
            return MangasPage(emptyList(), false)
        }
        if (response.isSuccessful.not()) {
            throw Exception("Error getting search manga http code: ${response.code}")
        }
        // TODO: also filter based on the content rating here?
        // TODO: also append here the related manga?
        val mlResponse = MdUtil.jsonParser.decodeFromString<SimilarMangaResponse>(response.body!!.string())
        val mangaList = mlResponse.matches.map {
            SManga.create().apply {
                url = "/manga/" + it.id
                title = MdUtil.cleanString(it.title["en"]!!)
                thumbnail_url = V5DbQueries.getAltCover(v5DbHelper.dbCovers, it.id) ?: MdUtil.imageUrlCacheNotFound
            }
        }
        return MangasPage(mangaList, false)
    }
}
