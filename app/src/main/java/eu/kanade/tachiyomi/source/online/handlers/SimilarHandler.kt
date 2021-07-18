package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.dto.SimilarMangaDatabaseDto
import eu.kanade.tachiyomi.source.online.dto.SimilarMangaDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.util.manga.MangaMappings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import retrofit2.Response
import uy.kohesive.injekt.injectLazy

class SimilarHandler {

    private val network: NetworkHelper by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    private val mappings: MangaMappings by injectLazy()

    /**
     * fetch our similar mangaList
     */
    suspend fun fetchSimilarManga(manga: Manga, refresh: Boolean): MangaListPage {
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        if (mangaDb != null && !refresh) {
            try {
                val dbDto = MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(mangaDb.data)
                return similarDtoToMangaListPage(dbDto.similarApi, dbDto.mangadexApi)
            } catch (e : Exception) { }
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
        val mangaListDto = similarGetMangaList(similarDto)
        val mangaPages = similarDtoToMangaListPage(similarDto, mangaListDto)

        // Convert to a database type that has both images and similar api response
        val similarDatabaseDto = SimilarMangaDatabaseDto(similarApi = similarDto, mangadexApi = mangaListDto)
        val similarDatabaseDtoString = MdUtil.jsonParser.encodeToString(similarDatabaseDto)

        // If we have the manga in our database, then we should update it, otherwise insert as new
        val mangaSimilar = MangaSimilar.create().apply {
            manga_id = MdUtil.getMangaId(manga.url)
            data = similarDatabaseDtoString
        }
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        if(mangaDb != null) {
            mangaSimilar.id = mangaDb.id
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaPages
    }

    private suspend fun similarGetMangaList(
        similarMangaDto: SimilarMangaDto,
    ): MangaListDto {
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
        val responseBody = response.body()!!
        if (responseBody.results.size != ids.size) {
            XLog.e("error ", response.errorBody()!!.string())
            throw Exception("Unable to complete response ${responseBody.results.size} of ${ids.size} returned")
        }
        return responseBody
    }

    private fun similarDtoToMangaListPage(
        similarMangaDto: SimilarMangaDto,
        similarMangaListDto: MangaListDto,
    ): MangaListPage {

        // Convert to lookup array
        // TODO: also append here the related manga?
        // TODO: filter recommended based on if they have supported lang
        val idsToManga = hashMapOf<String, SManga>()
        similarMangaListDto.results.forEach {
            idsToManga[it.data.id] = it.toBasicManga()
        }

        // Loop through our *sorted* related array and list in that order
        val mangaList = similarMangaDto.matches.map {
            idsToManga[it.id]!!
        }
        return MangaListPage(mangaList, false)
    }
}
