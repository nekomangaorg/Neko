package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.models.dto.AnilistMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.models.dto.MalMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.SimilarMangaDatabaseDto
import eu.kanade.tachiyomi.source.online.models.dto.SimilarMangaDto
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
        // Get cache from database if we have it
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        if (mangaDb != null && !refresh) {
            try {
                val dbDto = MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(mangaDb.data)
                val idsToManga = hashMapOf<String, SManga>()
                dbDto.similarMdexApi!!.results.forEach {
                    idsToManga[it.data.id] = it.toBasicManga()
                }
                val mangaList = dbDto.similarApi!!.matches.map { idsToManga[it.id]!! }
                return MangaListPage(mangaList, true)
            } catch (e : Exception) {
                XLog.e(e)
            }
        }
        // Main network request
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
            throw Exception("Error getting similar manga http code: ${response.code()}")
        }

        // Get our page of mangaList
        val similarDto = response.body()!!
        val ids = similarDto.matches.map { it.id }
        val mangaListDto = similarGetMangadexMangaList(ids)

        // Loop through our *sorted* related array and list in that order
        val idsToManga = hashMapOf<String, SManga>()
        mangaListDto.results.forEach {
            idsToManga[it.data.id] = it.toBasicManga()
        }
        val mangaList = ids.map { idsToManga[it]!! }
        val mangaPages = MangaListPage(mangaList, true)

        // Convert to a database type that has both images and similar api response
        // We will get the latest database info and the update the contents with our new content
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        var dbDto = SimilarMangaDatabaseDto()
        if (mangaDb != null) {
            try {
                dbDto = MdUtil.jsonParser.decodeFromString(mangaDb.data)
            } catch (e: Exception) {
                XLog.e(e)
            }
        }
        dbDto.similarApi = similarDto
        dbDto.similarMdexApi = mangaListDto

        // If we have the manga in our database, then we should update it, otherwise insert as new
        val similarDatabaseDtoString = MdUtil.jsonParser.encodeToString(dbDto)
        val mangaSimilar = MangaSimilar.create().apply {
            manga_id = MdUtil.getMangaId(manga.url)
            data = similarDatabaseDtoString
        }
        if(mangaDb != null) {
            mangaSimilar.id = mangaDb.id
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaPages
    }

    /**
     * fetch our similar mangaList from external service Anilist
     */
    suspend fun fetchSimilarExternalAnilistManga(manga: Manga, refresh: Boolean): MangaListPage {
        // See if we have a valid mapping for our Anlist service
        val anilistId = mappings.getExternalID(MdUtil.getMangaId(manga.url), "al")
            ?: return MangaListPage(emptyList(), false)
        // Get the cache if we have it
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        if (mangaDb != null && !refresh) {
            try {
                val dbDto = MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(mangaDb.data)
                val idsToManga = hashMapOf<String, SManga>()
                dbDto.anilistMdexApi!!.results.forEach {
                    idsToManga[it.data.id] = it.toBasicManga()
                }
                val ids = dbDto.anilistApi!!.data.Media.recommendations.edges.map {
                    if(it.node.mediaRecommendation.format != "MANGA")
                        return@map null
                    mappings.getMangadexID(it.node.mediaRecommendation.id.toString(), "al")
                }.filterNotNull()
                val mangaList = ids.map {
                    val tmp = idsToManga[it]!!
                    tmp.external_source_icon = "al"
                    tmp
                }
                return MangaListPage(mangaList, true)
            } catch (e : Exception) {
                XLog.e(e)
            }
        }
        // Main network request
        val graphql = """{ Media(id: ${anilistId}, type: MANGA) { recommendations { edges { node { mediaRecommendation { id format } rating } } } } }"""
        val response = network.similarService.getAniListGraphql(graphql)
        return similarMangaExternalAnilistParse(manga, response)
    }

    private suspend fun similarMangaExternalAnilistParse(
        manga: Manga,
        response: Response<AnilistMangaRecommendationsDto>,
    ): MangaListPage {
        // Error check http response
        if (response.code() == 404) {
            return MangaListPage(emptyList(), false)
        }
        if (response.isSuccessful.not() || response.code() != 200) {
            throw Exception("Error getting Anilist http code: ${response.code()}")
        }

        // Get our page of mangaList
        val similarDto = response.body()!!
        val ids = similarDto.data.Media.recommendations.edges.map {
            if(it.node.mediaRecommendation.format != "MANGA")
                return@map null
            mappings.getMangadexID(it.node.mediaRecommendation.id.toString(), "al")
        }.filterNotNull()
        val mangaListDto = similarGetMangadexMangaList(ids)

        // Convert to lookup array
        // TODO: We should probably sort this based on score from MAL!!!
        // TODO: Also filter out manga here that are already presented
        val idsToManga = hashMapOf<String, SManga>()
        mangaListDto.results.forEach {
            idsToManga[it.data.id] = it.toBasicManga()
        }

        // Loop through our *sorted* related array and list in that order
        val mangaList = ids.map {
            val tmp = idsToManga[it]!!
            tmp.external_source_icon = "al"
            tmp
        }
        val mangaPages = MangaListPage(mangaList, true)

        // Convert to a database type that has both images and similar api response
        // We will get the latest database info and the update the contents with our new content
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        var dbDto = SimilarMangaDatabaseDto()
        if (mangaDb != null) {
            try {
                dbDto = MdUtil.jsonParser.decodeFromString(mangaDb.data)
            } catch (e: Exception) {
                XLog.e(e)
            }
        }
        dbDto.anilistApi = similarDto
        dbDto.anilistMdexApi = mangaListDto

        // If we have the manga in our database, then we should update it, otherwise insert as new
        val similarDatabaseDtoString = MdUtil.jsonParser.encodeToString(dbDto)
        val mangaSimilar = MangaSimilar.create().apply {
            manga_id = MdUtil.getMangaId(manga.url)
            data = similarDatabaseDtoString
        }
        if(mangaDb != null) {
            mangaSimilar.id = mangaDb.id
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaPages

    }

    /**
     * fetch our similar mangaList from external service myanimelist
     */
    suspend fun fetchSimilarExternalMalManga(manga: Manga, refresh: Boolean): MangaListPage {
        // See if we have a valid mapping for our MAL service
        val malId = mappings.getExternalID(MdUtil.getMangaId(manga.url), "mal")
            ?: return MangaListPage(emptyList(), false)
        // Get the cache if we have it
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        if (mangaDb != null && !refresh) {
            try {
                val dbDto = MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(mangaDb.data)
                val idsToManga = hashMapOf<String, SManga>()
                dbDto.myanimelistMdexApi!!.results.forEach {
                    idsToManga[it.data.id] = it.toBasicManga()
                }
                val ids = dbDto.myanimelistApi!!.recommendations.map {
                    mappings.getMangadexID(it.mal_id.toString(), "mal")
                }.filterNotNull()
                val mangaList = ids.map {
                    val tmp = idsToManga[it]!!
                    tmp.external_source_icon = "mal"
                    tmp
                }
                return MangaListPage(mangaList, true)
            } catch (e : Exception) {
                XLog.e(e)
            }
        }
        // Main network request
        val response = network.similarService.getSimilarMalManga(malId)
        return similarMangaExternalMalParse(manga, response)
    }

    private suspend fun similarMangaExternalMalParse(
        manga: Manga,
        response: Response<MalMangaRecommendationsDto>,
    ): MangaListPage {
        // Error check http response
        if (response.code() == 404) {
            return MangaListPage(emptyList(), false)
        }
        if (response.isSuccessful.not() || response.code() != 200) {
            throw Exception("Error getting MyAnimeList http code: ${response.code()}")
        }

        // Get our page of mangaList
        val similarDto = response.body()!!
        val ids = similarDto.recommendations.map {
            mappings.getMangadexID(it.mal_id.toString(), "mal")
        }.filterNotNull()
        val mangaListDto = similarGetMangadexMangaList(ids)

        // Convert to lookup array
        // TODO: We should probably sort this based on score from MAL!!!
        // TODO: Also filter out manga here that are already presented
        val idsToManga = hashMapOf<String, SManga>()
        mangaListDto.results.forEach {
            idsToManga[it.data.id] = it.toBasicManga()
        }

        // Loop through our *sorted* related array and list in that order
        val mangaList = ids.map {
            val tmp = idsToManga[it]!!
            tmp.external_source_icon = "mal"
            tmp
        }
        val mangaPages = MangaListPage(mangaList, true)

        // Convert to a database type that has both images and similar api response
        // We will get the latest database info and the update the contents with our new content
        val mangaDb = db.getSimilar(MdUtil.getMangaId(manga.url)).executeAsBlocking()
        var dbDto = SimilarMangaDatabaseDto()
        if (mangaDb != null) {
            try {
                dbDto = MdUtil.jsonParser.decodeFromString(mangaDb.data)
            } catch (e: Exception) {
                XLog.e(e)
            }
        }
        dbDto.myanimelistApi = similarDto
        dbDto.myanimelistMdexApi = mangaListDto

        // If we have the manga in our database, then we should update it, otherwise insert as new
        val similarDatabaseDtoString = MdUtil.jsonParser.encodeToString(dbDto)
        val mangaSimilar = MangaSimilar.create().apply {
            manga_id = MdUtil.getMangaId(manga.url)
            data = similarDatabaseDtoString
        }
        if(mangaDb != null) {
            mangaSimilar.id = mangaDb.id
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaPages

    }

    /**
     * this will get the manga objects with cover_art for all the specified ids
     */
    private suspend fun similarGetMangadexMangaList(
        mangaIds: List<String>,
    ): MangaListDto {
        val queryMap = mutableMapOf(
            "limit" to mangaIds.size,
            "ids[]" to mangaIds
        )
        val response = network.service.search(ProxyRetrofitQueryMap(queryMap))
        if (response.isSuccessful.not()) {
            XLog.e("error ", response.errorBody()!!.string())
            throw Exception("Error getting manga http code: ${response.code()}")
        }
        val responseBody = response.body()!!
        if (responseBody.results.size != mangaIds.size) {
            throw Exception("Unable to complete response ${responseBody.results.size} of ${mangaIds.size} returned")
        }
        return responseBody
    }

}
