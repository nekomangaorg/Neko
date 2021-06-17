package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.dto.CoverListDto
import eu.kanade.tachiyomi.source.online.dto.SimilarMangaDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl.Companion.toHttpUrl
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
            return similarDtoToMangaListPage(manga, similarDto)
        }

        val response = network.similarService.getSimilarManga(MdUtil.getMangaId(manga.url))
        return similarMangaParse(manga, response)
    }

    private fun similarMangaParse(
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
        val mangaPages = similarDtoToMangaListPage(manga, similarDto)
        val similarDtoString = MdUtil.jsonParser.encodeToString(similarDto)

        // Insert into our database and return
        val mangaSimilar = MangaSimilar.create().apply {
            manga_id = MdUtil.getMangaId(manga.url)
            data = similarDtoString
        }
        db.insertSimilar(mangaSimilar).executeAsBlocking()
        return mangaPages
    }

    private fun similarDtoToMangaListPage(
        manga: Manga,
        similarMangaDto: SimilarMangaDto,
    ): MangaListPage {
        // TODO: also filter based on the content rating here?
        // TODO: also append here the related manga?

        val ids = similarMangaDto.matches.map {
            it.id
        }

        val coverUrl = MdUtil.coverUrl.toHttpUrl().newBuilder().apply {
            ids.forEach { mangaId ->
                addQueryParameter("manga[]", mangaId)
            }
            addQueryParameter("limit", ids.size.toString())
        }.build().toString()
        val response = network.client.newCall(GET(coverUrl)).execute()
        val coverList =
            MdUtil.jsonParser.decodeFromString<CoverListDto>(response.body!!.string())

        val unique = coverList.results.distinctBy { it.relationships[0].id }

        val coverMap = unique.map { coverResponse ->
            val fileName = coverResponse.data.attributes.fileName
            val mangaId = coverResponse.relationships.first { it.type.equals("manga", true) }.id
            val thumbnailUrl = "${MdUtil.cdnUrl}/covers/$mangaId/$fileName"
            Pair(mangaId, thumbnailUrl)
        }.toMap()

        val mangaList = similarMangaDto.matches.map {
            SManga.create().apply {
                url = "/title/" + it.id
                title = MdUtil.cleanString(it.title["en"]!!)
                thumbnail_url = coverMap[it.id]
            }
        }
        return MangaListPage(mangaList, false)
    }
}
