package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.getOrElse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
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
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.manga.MangaMappings
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.throws
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import uy.kohesive.injekt.injectLazy

class SimilarHandler {

    private val network: NetworkHelper by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    private val mappings: MangaMappings by injectLazy()
    private val preferencesHelper: PreferencesHelper by injectLazy()

    suspend fun fetchRelated(dexId: String): List<SManga> {
        val related = withIOContext {
            network.service.relatedManga(dexId)
                .onError {
                    this.log("trying to get related manga")
                }
                .onException {
                    this.log("trying to get related manga")
                }
                .getOrNull()
        }
        related ?: return emptyList()

        val mangaIdMap = related.data.mapNotNull {
            if (it.relationships.isEmpty()) return@mapNotNull null
            it.relationships.first().id to it.attributes.relation
        }.toMap()

        val mangaList = similarGetMangadexMangaList(mangaIdMap.keys.toList(), false)

        val thumbQuality = preferencesHelper.thumbnailQuality()

        val list = mangaList.data.map {
            it.toBasicManga(thumbQuality).apply {
                this.relationship = mangaIdMap[MdUtil.getMangaId(this.url)]
            }
        }
        return list
    }

    /**
     * fetch our similar mangaList
     */
    suspend fun fetchSimilar(
        similarDbEntry: MangaSimilar?,
        dexId: String,
        forceRefresh: Boolean = false,
    ): List<SManga> {
        if (similarDbEntry != null && !forceRefresh) {
            // Get cache from database if we have it
            try {
                val dbDto =
                    MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(similarDbEntry.data)
                val thumbQuality = preferencesHelper.thumbnailQuality()
                val idsToManga = dbDto.similarMdexApi?.data?.map {
                    it.id to it.toBasicManga(thumbQuality)
                }?.toMap() ?: emptyMap()
                val mangaList = dbDto.similarApi?.matches?.mapNotNull {
                    idsToManga[it.id]?.relationship =
                        String.format("%.2f", 100.0 * it.score) + "% similarity"
                    idsToManga[it.id]
                } ?: emptyList()
                return mangaList.sortedByDescending {
                    it.relationship?.split("%")?.get(0)?.toDouble()
                }
            } catch (e: Exception) {
                XLog.e(e)
            }
        }
        // Main network request
        val response = network.similarService.getSimilarManga(dexId).onError {
            val type = "trying to get similar manga"
            this.log(type)
            if (this.statusCode.code == 404) {
                this.throws(type)
            }
            /* can uncomment when this is onFailure again
            if (this !is ApiResponse.Failure.Error<*> || this.statusCode.code != 404) {
                   this.throws(type)
               }*/
        }.onException {
            val type = "trying to get similar manga"
            this.log(type)
            this.throws(type)
        }.getOrElse { null }
        return similarMangaParse(dexId, response)
    }

    private suspend fun similarMangaParse(
        dexId: String,
        similarDto: SimilarMangaDto?,
    ): List<SManga> {
        // Error check http response
        if (similarDto == null) {
            return emptyList()
        }

        // Get our page of mangaList
        val ids = similarDto.matches.map { it.id }
        val scores = similarDto.matches.map { it.score }
        val mangaListDto = similarGetMangadexMangaList(ids)

        // Loop through our *sorted* related array and list in that order
        val idsToManga = hashMapOf<String, SManga>()
        val thumbQuality = preferencesHelper.thumbnailQuality()
        mangaListDto.data.forEachIndexed { idx, it ->
            idsToManga[it.id] = it.toBasicManga(thumbQuality)
            idsToManga[it.id]?.relationship =
                String.format("%.2f", 100.0 * scores[idx]) + "% similarity"
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
        return mangaList.sortedByDescending { it.relationship?.split("%")?.get(0)?.toDouble() }
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
                val idPairs = dbDto.anilistApi!!.data.Media.recommendations.edges.map {
                    if (it.node.mediaRecommendation.format != "MANGA")
                        return@map null
                    val id = mappings.getMangadexID(it.node.mediaRecommendation.id.toString(), "al")
                    val text = it.node.rating.toString() + " rating"
                    Pair(id, text)
                }.filterNotNull()
                val mangaList = idPairs.map {
                    idsToManga[it.first]!!.apply{this.relationship = it.second}
                }
                return mangaList.sortedByDescending {
                    it.relationship?.split(" ")?.get(0)?.toDouble()
                }
            } catch (e: Exception) {
                XLog.e(e)
            }
        }
        // Main network request
        val graphql =
            """{ Media(id: ${anilistId}, type: MANGA) { recommendations { edges { node { mediaRecommendation { id format } rating } } } } }"""
        val response = network.similarService.getAniListGraphql(graphql).onError {
            val type = "trying to get Anilist similar manga"
            this.log(type)
            if (this.statusCode.code == 404) {
                this.throws(type)
            }
            /* can uncomment when this is onFailure again
            if (this !is ApiResponse.Failure.Error<*> || this.statusCode.code != 404) {
                   this.throws(type)
               }*/
        }.onException {
            val type = "trying to get Anilist similar manga"
            this.log(type)
            this.throws(type)
        }.getOrElse { null }
        return similarMangaExternalAnilistParse(dexId, response)
    }

    private suspend fun similarMangaExternalAnilistParse(
        dexId: String,
        similarDto: AnilistMangaRecommendationsDto?,
    ): List<SManga> {
        // Error check http response
        if (similarDto == null) {
            return emptyList()
        }

        // Get our page of mangaList
        val idPairs = similarDto.data.Media.recommendations.edges.map {
            if (it.node.mediaRecommendation.format != "MANGA")
                return@map null
            val id = mappings.getMangadexID(it.node.mediaRecommendation.id.toString(), "al")
            val text = it.node.rating.toString() + " rating"
            Pair(id, text)
        }.filterNotNull()
        val mangaListDto = similarGetMangadexMangaList(idPairs.mapNotNull { it.first })

        // Convert to lookup array
        // TODO: Also filter out manga here that are already presented?
        val idsToManga = hashMapOf<String, SManga>()
        val thumbQuality = preferencesHelper.thumbnailQuality()
        mangaListDto.data.forEach {
            idsToManga[it.id] = it.toBasicManga(thumbQuality)
        }

        // Loop through our related array and list in that order
        val mangaList = idPairs.map {
            idsToManga[it.first]!!.apply{this.relationship = it.second}
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
        return mangaList.sortedByDescending { it.relationship?.split(" ")?.get(0)?.toDouble() }
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
                val idPairs = dbDto.myanimelistApi!!.recommendations.mapNotNull {
                    val id = mappings.getMangadexID(it.mal_id.toString(), "mal")
                    val text = it.recommendation_count.toString() + " rating"
                    Pair(id, text)
                }
                val mangaList = idPairs.map {
                    idsToManga[it.first]!!.apply{this.relationship = it.second}
                }
                return mangaList.sortedByDescending {
                    it.relationship?.split(" ")?.get(0)?.toDouble()
                }
            } catch (e: Exception) {
                XLog.enableStackTrace(10).e(e)
            }
        }
        // Main network request
        val response = network.similarService.getSimilarMalManga(malId).onError {
            val type = "trying to get MAL similar manga"
            this.log(type)
            if (this.statusCode.code == 404) {
                this.throws(type)
            }
            /* can uncomment when this is onFailure again
            if (this !is ApiResponse.Failure.Error<*> || this.statusCode.code != 404) {
                   this.throws(type)
               }*/
        }.onException {
            val type = "trying to get MAL similar manga"
            this.log(type)
            this.throws(type)
        }.getOrElse { null }
        return similarMangaExternalMalParse(dexId, response)
    }

    private suspend fun similarMangaExternalMalParse(
        dexId: String,
        similarDto: MalMangaRecommendationsDto?,
    ): List<SManga> {
        // Error check http response
        if (similarDto == null) {
            return emptyList()
        }

        // Get our page of mangaList
        val idPairs = similarDto.recommendations.map {
            val id = mappings.getMangadexID(it.mal_id.toString(), "mal")
            val text = it.recommendation_count.toString() + " rating"
            Pair(id, text)
        }.filterNotNull()
        val mangaListDto = similarGetMangadexMangaList(idPairs.mapNotNull { it.first })

        // Convert to lookup array
        // TODO: Also filter out manga here that are already presented
        val idsToManga = hashMapOf<String, SManga>()
        val thumbQuality = preferencesHelper.thumbnailQuality()
        mangaListDto.data.forEach {
            idsToManga[it.id] = it.toBasicManga(thumbQuality)
        }

        // Loop through our *sorted* related array and list in that order
        val mangaList = idPairs.map {
            idsToManga[it.first]!!.apply{this.relationship = it.second}
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
        return mangaList.sortedByDescending { it.relationship?.split(" ")?.get(0)?.toDouble() }
    }

    /**
     * this will get the manga objects with cover_art for all the specified ids
     */
    private suspend fun similarGetMangadexMangaList(
        mangaIds: List<String>, strictMatch: Boolean = true,
    ): MangaListDto {
        val queryMap = mutableMapOf(
            "limit" to mangaIds.size,
            "ids[]" to mangaIds,
        )
        val responseBody = network.service.search(ProxyRetrofitQueryMap(queryMap)).onError {
            val type = "searching for manga in similar handler"
            this.log(type)
            this.throws(type)
        }.onException {
            val type = "searching for manga in similar handler"
            this.log(type)
            this.throws(type)
        }.getOrThrow()

        if (strictMatch && responseBody.data.size != mangaIds.size) {
            XLog.e("manga returned doesn't match number of manga expected")
            throw Exception("Unable to complete response ${responseBody.data.size} of ${mangaIds.size} returned")
        }
        return responseBody
    }
}
