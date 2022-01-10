package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.data.models.DisplaySManga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.models.dto.AnilistMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.models.dto.MalMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaDataDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.RelatedMangaDto
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

    suspend fun fetchRelated(
        dexId: String,
        forceRefresh: Boolean,
    ): List<DisplaySManga> {
        if (forceRefresh) {
            val related = withIOContext {
                network.service.relatedManga(dexId)
                    .onFailure {
                        XLog.e("trying to get related manga, $this")
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

            val relatedMangaList = mangaList.data.map {
                it.toRelatedMangaDto(thumbQuality, mangaIdMap[it.id] ?: "")
            }

            // Update the Manga Similar database
            val mangaDb = db.getSimilar(dexId).executeAsBlocking()
            val dbDto = getDbDto(mangaDb)
            dbDto.relatedManga = relatedMangaList
            insertMangaSimilar(dexId, dbDto, mangaDb)
        }

        val dbDto = getDbDto(db.getSimilar(dexId).executeAsBlocking())
        return dbDto.relatedManga?.map {
            it.toDisplaySManga()
        } ?: emptyList()
    }

    private fun insertMangaSimilar(
        dexId: String,
        dbDto: SimilarMangaDatabaseDto,
        mangaDb: MangaSimilar?,
    ) {
        // If we have the manga in our database, then we should update it, otherwise insert as new
        val similarDatabaseDtoString = MdUtil.jsonParser.encodeToString(dbDto)
        val mangaSimilar = MangaSimilar.create().apply {
            id = mangaDb?.id
            manga_id = dexId
            data = similarDatabaseDtoString
        }

        db.insertSimilar(mangaSimilar).executeAsBlocking()
    }

    private fun getDbDto(mangaDb: MangaSimilar?): SimilarMangaDatabaseDto {
        return runCatching {
            MdUtil.jsonParser.decodeFromString<SimilarMangaDatabaseDto>(mangaDb!!.data)
        }.getOrElse {
            SimilarMangaDatabaseDto()
        }
    }

    private fun RelatedMangaDto.toDisplaySManga() = DisplaySManga(
        sManga = SManga.create().apply {
            this.url = this@toDisplaySManga.url
            this.thumbnail_url = this@toDisplaySManga.thumbnail
            this.title = this@toDisplaySManga.title
        },
        displayText = this@toDisplaySManga.relation)

    private fun MangaDataDto.toRelatedMangaDto(
        thumbQuality: Int,
        otherText: String,
    ): RelatedMangaDto {
        val manga = this.toBasicManga(thumbQuality)
        return RelatedMangaDto(manga.url,
            manga.title,
            manga.thumbnail_url!!,
            otherText)
    }

    /**
     * fetch our similar mangaList
     */
    suspend fun fetchSimilar(
        dexId: String,
        forceRefresh: Boolean,
    ): List<DisplaySManga> {
        if (forceRefresh) {

            val response = network.similarService.getSimilarManga(dexId)
                .onFailure {
                    XLog.e("trying to get similar manga, $this")
                }.getOrNull()

            similarMangaParse(dexId, response)
        }

        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
        val dbDto = getDbDto(mangaDb)
        // Get data from db
        return dbDto.similarManga?.map { it.toDisplaySManga() }?.sortedByDescending {
            it.displayText?.split("%")?.get(0)?.toDouble()
        } ?: emptyList()
    }

    private suspend fun similarMangaParse(
        dexId: String,
        similarDto: SimilarMangaDto?,
    ) {
        similarDto ?: return

        // Get our page of mangaList
        val ids = similarDto.matches.map { it.id }
        val scores = similarDto.matches.map { it.score }
        val mangaListDto = similarGetMangadexMangaList(ids)

        val thumbQuality = preferencesHelper.thumbnailQuality()
        val similarMangaList = mangaListDto.data.mapIndexed { index, it ->
            it.toRelatedMangaDto(thumbQuality,
                String.format("%f.2", 100.0 * scores[index]) + "% match")
        }

        //insert the new info into the db
        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
        val dbDto = getDbDto(mangaDb)
        dbDto.similarApi = similarDto
        dbDto.similarManga = similarMangaList
        insertMangaSimilar(dexId, dbDto, mangaDb)
    }

    /**
     * fetch our similar mangaList from external service Anilist
     */
    suspend fun fetchAnilist(
        dexId: String,
        forceRefresh: Boolean,
    ): List<DisplaySManga> {
        // See if we have a valid mapping for our Anlist service
        val anilistId = mappings.getExternalID(dexId, "al") ?: return emptyList()

        if (forceRefresh) {
            // Main network request
            val graphql =
                """{ Media(id: ${anilistId}, type: MANGA) { recommendations { edges { node { mediaRecommendation { id format } rating } } } } }"""
            val response = network.similarService.getAniListGraphql(graphql).onError {
                val type = "trying to get Anilist recommendations"
                this.log(type)
                if (this.statusCode.code == 404) {
                    this.throws(type)
                }
            }.onException {
                val type = "trying to get Anilist recommendations"
                this.log(type)
                this.throws(type)
            }.getOrNull()

            anilistRecommendationParse(dexId, response)
        }

        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
        val dbDto = getDbDto(mangaDb)
        // Get data from db
        return dbDto.aniListManga?.map { it.toDisplaySManga() }?.sortedByDescending {
            it.displayText?.split(" ")?.get(0)?.toDouble()
        } ?: emptyList()
    }

    private suspend fun anilistRecommendationParse(
        dexId: String,
        similarDto: AnilistMangaRecommendationsDto?,
    ) {
        // Error check http response
        similarDto ?: return

        // Get our page of mangaList
        val idPairs = similarDto.data.Media.recommendations.edges.map {
            if (it.node.mediaRecommendation.format != "MANGA")
                return@map null
            val id = mappings.getMangadexID(it.node.mediaRecommendation.id.toString(), "al")
            val text = it.node.rating.toString() + " user votes"
            id to text
        }.filterNotNull().toMap()

        val mangaListDto = similarGetMangadexMangaList(idPairs.mapNotNull { it.key })

        val thumbQuality = preferencesHelper.thumbnailQuality()

        val mangaList = mangaListDto.data.map {
            it.toRelatedMangaDto(thumbQuality, idPairs[it.id] ?: "")
        }

        //update db
        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
        val dbDto = getDbDto(mangaDb)

        dbDto.aniListApi = similarDto
        dbDto.aniListManga = mangaList

        insertMangaSimilar(dexId, dbDto, mangaDb)
    }

    /**
     * fetch our similar mangaList from external service myanimelist
     */
    suspend fun fetchSimilarExternalMalManga(
        dexId: String,
        forceRefresh: Boolean,
    ): List<DisplaySManga> {
        // See if we have a valid mapping for our MAL service
        val malId = mappings.getExternalID(dexId, "mal")
            ?: return emptyList()

        if (forceRefresh) {
            val response = network.similarService.getSimilarMalManga(malId).onError {
                val type = "trying to get MAL similar manga"
                this.log(type)
                if (this.statusCode.code == 404) {
                    this.throws(type)
                }
            }.onException {
                val type = "trying to get MAL similar manga"
                this.log(type)
                this.throws(type)
            }.getOrNull()
            similarMangaExternalMalParse(dexId, response)
        }

        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
        val dbDto = getDbDto(mangaDb)
        // Get data from db
        return dbDto.myAnimeListManga?.map { it.toDisplaySManga() }?.sortedByDescending {
            it.displayText?.split(" ")?.get(0)?.toDouble()
        } ?: emptyList()
    }

    private suspend fun similarMangaExternalMalParse(
        dexId: String,
        similarDto: MalMangaRecommendationsDto?,
    ) {
        // Error check http response
        similarDto ?: return

        // Get our page of mangaList
        val idPairs = similarDto.recommendations.mapNotNull {
            val id = mappings.getMangadexID(it.mal_id.toString(), "mal")
            val text = it.recommendation_count.toString() + " user votes"
            id to text
        }.toMap()
        val mangaListDto = similarGetMangadexMangaList(idPairs.mapNotNull { it.key })

        // Convert to lookup array
        // TODO: Also filter out manga here that are already presented
        val thumbQuality = preferencesHelper.thumbnailQuality()

        val mangaList = mangaListDto.data.map {
            it.toRelatedMangaDto(thumbQuality, idPairs[it.id] ?: "")
        }

        val mangaDb = db.getSimilar(dexId).executeAsBlocking()
        val dbDto = getDbDto(mangaDb)

        dbDto.myAnimelistApi = similarDto
        dbDto.myAnimeListManga = mangaList

        insertMangaSimilar(dexId, dbDto, mangaDb)
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
