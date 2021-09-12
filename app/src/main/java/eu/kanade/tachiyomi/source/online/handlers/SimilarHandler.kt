package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
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
    private val preferencesHelper: PreferencesHelper by injectLazy()

    /**
     * fetch our similar mangaList
     */
    suspend fun fetchSimilar(
        similarDbEntry: MangaSimilar?,
        dexId: String,
        forceRefresh: Boolean = false,
    ): List<SManga> {
        // Get cache from database if we have it
        if (similarDbEntry != null && !forceRefresh) {
            try {
                val dbDto =
                    MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(similarDbEntry.data)
                val thumbQuality = preferencesHelper.thumbnailQuality()
                val idsToManga = dbDto.similarMdexApi?.data?.map {
                    it.id to it.toBasicManga(thumbQuality)
                }?.toMap() ?: emptyMap()

                return dbDto.similarApi?.matches?.mapNotNull { idsToManga[it.id] } ?: emptyList()
            } catch (e: Exception) {
                XLog.e(e)
            }
        }
        // Main network request
        val response = network.similarService.getSimilarManga(dexId)
        return similarMangaParse(dexId, response)
    }

    private suspend fun similarMangaParse(
        dexId: String,
        response: Response<SimilarMangaDto>,
    ): List<SManga> {
        // Error check http response
        if (response.code() == 404) {
            return emptyList()
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
        val thumbQuality = preferencesHelper.thumbnailQuality()

        mangaListDto.data.forEach {
            idsToManga[it.id] = it.toBasicManga(thumbQuality)
        }
        val mangaList = ids.map { idsToManga[it]!! }

        // Convert to a database type that has both images and similar api response
        // We will get the latest database info and the update the contents with our new content
        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
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
            manga_id = dexId
            data = similarDatabaseDtoString
        }
        if (mangaDb != null) {
            mangaSimilar.id = mangaDb.id
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaList
    }

    /**
     * fetch our similar mangaList from external service Anilist
     */
    suspend fun fetchAnilist(
        similarDbEntry: MangaSimilar?,
        dexId: String,
        forceRefresh: Boolean = false,
    ): List<SManga> {
        // See if we have a valid mapping for our Anlist service

        val anilistId = mappings.getExternalID(dexId, "al") ?: return emptyList()
        // Get the cache if we have it
        if (similarDbEntry != null && !forceRefresh) {
            try {
                val dbDto =
                    MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(similarDbEntry.data)
                val idsToManga = hashMapOf<String, SManga>()
                dbDto.anilistMdexApi!!.data.forEach {
                    idsToManga[it.id] = it.toBasicManga()
                }
                val ids = dbDto.anilistApi!!.data.Media.recommendations.edges.map {
                    if (it.node.mediaRecommendation.format != "MANGA")
                        return@map null
                    mappings.getMangadexID(it.node.mediaRecommendation.id.toString(), "al")
                }.filterNotNull()
                val mangaList = ids.map {
                    val tmp = idsToManga[it]!!
                    tmp
                }
                return mangaList
            } catch (e: Exception) {
                XLog.e(e)
            }
        }
        // Main network request
        val graphql =
            """{ Media(id: ${anilistId}, type: MANGA) { recommendations { edges { node { mediaRecommendation { id format } rating } } } } }"""
        val response = network.similarService.getAniListGraphql(graphql)
        return similarMangaExternalAnilistParse(dexId, response)
    }

    private suspend fun similarMangaExternalAnilistParse(
        dexId: String,
        response: Response<AnilistMangaRecommendationsDto>,
    ): List<SManga> {
        // Error check http response
        if (response.code() == 404) {
            return emptyList()
        }
        if (response.isSuccessful.not() || response.code() != 200) {
            throw Exception("Error getting Anilist http code: ${response.code()}")
        }

        // Get our page of mangaList
        val similarDto = response.body()!!
        val ids = similarDto.data.Media.recommendations.edges.map {
            if (it.node.mediaRecommendation.format != "MANGA")
                return@map null
            mappings.getMangadexID(it.node.mediaRecommendation.id.toString(), "al")
        }.filterNotNull()
        val mangaListDto = similarGetMangadexMangaList(ids)

        // Convert to lookup array
        // TODO: We should probably sort this based on score from MAL!!!
        // TODO: Also filter out manga here that are already presented
        val idsToManga = hashMapOf<String, SManga>()
        val thumbQuality = preferencesHelper.thumbnailQuality()

        mangaListDto.data.forEach {
            idsToManga[it.id] = it.toBasicManga(thumbQuality)
        }

        // Loop through our *sorted* related array and list in that order
        val mangaList = ids.map {
            val tmp = idsToManga[it]!!
            tmp
        }

        // Convert to a database type that has both images and similar api response
        // We will get the latest database info and the update the contents with our new content
        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
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
            manga_id = dexId
            data = similarDatabaseDtoString
        }
        if (mangaDb != null) {
            mangaSimilar.id = mangaDb.id
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaList
    }

    /**
     * fetch our similar mangaList from external service myanimelist
     */
    suspend fun fetchSimilarExternalMalManga(
        similarDbEntry: MangaSimilar?,
        dexId: String,
        forceRefresh: Boolean = false,
    ): List<SManga> {
        // See if we have a valid mapping for our MAL service
        val malId = mappings.getExternalID(dexId, "mal")
            ?: return emptyList()
        // Get the cache if we have it
        if (similarDbEntry != null && !forceRefresh) {
            try {
                val dbDto =
                    MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(similarDbEntry.data)
                val idsToManga = hashMapOf<String, SManga>()
                val thumbQuality = preferencesHelper.thumbnailQuality()

                dbDto.myanimelistMdexApi!!.data.forEach {
                    idsToManga[it.id] = it.toBasicManga(thumbQuality)
                }
                val ids = dbDto.myanimelistApi!!.recommendations.mapNotNull {
                    mappings.getMangadexID(it.mal_id.toString(), "mal")
                }
                val mangaList = ids.map {
                    val tmp = idsToManga[it]!!
                    tmp
                }
                return mangaList
            } catch (e: Exception) {
                XLog.enableStackTrace(10).e(e)
            }
        }
        // Main network request
        val response = network.similarService.getSimilarMalManga(malId)
        return similarMangaExternalMalParse(dexId, response)
    }

    private suspend fun similarMangaExternalMalParse(
        dexId: String,
        response: Response<MalMangaRecommendationsDto>,
    ): List<SManga> {
        // Error check http response
        if (response.code() == 404) {
            return emptyList()
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
        val thumbQuality = preferencesHelper.thumbnailQuality()

        mangaListDto.data.forEach {
            idsToManga[it.id] = it.toBasicManga(thumbQuality)
        }

        // Loop through our *sorted* related array and list in that order
        val mangaList = ids.map {
            val tmp = idsToManga[it]!!
            tmp
        }

        // Convert to a database type that has both images and similar api response
        // We will get the latest database info and the update the contents with our new content
        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
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
            manga_id = dexId
            data = similarDatabaseDtoString
        }
        if (mangaDb != null) {
            mangaSimilar.id = mangaDb.id
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaList
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
        if (responseBody.data.size != mangaIds.size) {
            throw Exception("Unable to complete response ${responseBody.data.size} of ${mangaIds.size} returned")
        }
        return responseBody
    }
}
