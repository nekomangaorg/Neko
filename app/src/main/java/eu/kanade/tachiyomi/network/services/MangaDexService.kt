package eu.kanade.tachiyomi.network.services

import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.source.online.dto.AtHomeImageReportDto
import eu.kanade.tachiyomi.source.online.dto.ChapterDto
import eu.kanade.tachiyomi.source.online.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.dto.LegacyIdDto
import eu.kanade.tachiyomi.source.online.dto.LegacyMappingDto
import eu.kanade.tachiyomi.source.online.dto.MangaDto
import eu.kanade.tachiyomi.source.online.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.dto.ResultDto
import eu.kanade.tachiyomi.source.online.utils.MdApi
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MangaDexService : MangaDexImageService {

    @GET("${MdApi.manga}?includes[]=${MdConstants.Types.coverArt}")
    suspend fun search(@QueryMap options: ProxyRetrofitQueryMap): Response<MangaListDto>

    @GET("${MdApi.manga}/{id}?includes[]=${MdConstants.Types.coverArt}&includes[]=${MdConstants.Types.author}&includes[]=${MdConstants.Types.artist}")
    suspend fun viewManga(@Path("id") id: String): Response<MangaDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}/{id}/feed?limit=500&includes[]=${MdConstants.Types.scanlator}&order[volume]=desc&order[chapter]=desc")
    suspend fun viewChapters(
        @Path("id") id: String,
        @Query(value = "translatedLanguage[]") translatedLanguages: List<String>,
        @Query("offset") offset: Int,
    ): Response<ChapterListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.chapter}?order[publishAt]=desc")
    suspend fun latestChapters(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query(value = "translatedLanguage[]") translatedLanguages: List<String>,
    ): Response<ChapterListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.chapter}/{id}")
    suspend fun viewChapter(@Path("id") id: String): Response<ChapterDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}/random")
    suspend fun randomManga(): Response<MangaDto>

    @POST(MdApi.legacyMapping)
    suspend fun legacyMapping(@Body legacyMapping: LegacyIdDto): Response<List<LegacyMappingDto>>

    @POST(MdConstants.atHomeReportUrl)
    suspend fun atHomeImageReport(@Body atHomeImageReportDto: AtHomeImageReportDto): Response<ResultDto>
}
