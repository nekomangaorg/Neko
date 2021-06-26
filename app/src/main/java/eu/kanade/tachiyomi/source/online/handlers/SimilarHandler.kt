package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.dto.SimilarMangaDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import retrofit2.Response
import uy.kohesive.injekt.injectLazy

class SimilarHandler {

    private val network: NetworkHelper by injectLazy()
    private val db: DatabaseHelper by injectLazy()

    /**
     * fetch our similar mangaList
     */
    suspend fun fetchSimilarManga(manga: Manga, refresh: Boolean): MangaListPage {
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        if (mangaDb != null && !refresh) {
            val similarDto = MdUtil.jsonParser.decodeFromString<SimilarMangaDto>(mangaDb.data)
            return similarDtoToMangaListPage(similarDto)
        }

        val response = network.similarService.getSimilarManga(MdUtil.getMangaId(manga.url))
        return similarMangaParse(manga, response)
    }

    private suspend fun similarMangaParse(
        manga: Manga,
        response: Response<SimilarMangaDto>,
    ): MangaListPage {
        // Error check http response
        if (response.code() == 404) {
            return MangaListPage(emptyList(), false)
        }
        if (response.isSuccessful.not() || response.code() != 200) {
            throw Exception("Error getting search manga http code: ${response.code()}")
        }
        // Get our page of mangaList
        val similarDto = response.body()!!
        val mangaPages = similarDtoToMangaListPage(similarDto)
        val similarDtoString = MdUtil.jsonParser.encodeToString(similarDto)

        // Insert into our database and return
        val mangaSimilar = MangaSimilar.create().apply {
            manga_id = MdUtil.getMangaId(manga.url)
            data = similarDtoString
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaPages
    }

    private suspend fun similarDtoToMangaListPage(
        similarMangaDto: SimilarMangaDto,
    ): MangaListPage {
        // TODO: also filter based on the content rating here?
        // TODO: also append here the related manga?

        val ids = similarMangaDto.matches.map {
            it.id
        }

        val queryMap = mutableMapOf(
            "limit" to ids.size,
            "ids[]" to ids
        )
        val response = network.service.search(ProxyRetrofitQueryMap(queryMap))

        if (response.isSuccessful.not()) {
            XLog.e("error ", response.errorBody()!!.string())
            throw Exception("Error getting manga http code: ${response.code()}")
        }

        val mangaList = response.body()!!.results.map {
            it.toBasicManga()
        }

        return MangaListPage(mangaList, false)
    }
}
