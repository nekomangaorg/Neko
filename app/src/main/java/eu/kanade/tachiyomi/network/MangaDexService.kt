package eu.kanade.tachiyomi.network

import eu.kanade.tachiyomi.source.online.dto.AtHomeImageReportDto
import eu.kanade.tachiyomi.source.online.dto.ChapterDto
import eu.kanade.tachiyomi.source.online.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.dto.LegacyIdDto
import eu.kanade.tachiyomi.source.online.dto.LegacyMappingDto
import eu.kanade.tachiyomi.source.online.dto.MangaDto
import eu.kanade.tachiyomi.source.online.dto.MangaListDto
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

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}?includes[]=${MdConstants.coverArt}")
    suspend fun search(@QueryMap options: ProxyRetrofitQueryMap): Response<MangaListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}/{id}?includes[]=${MdConstants.coverArt}&includes[]=${MdConstants.author}&includes[]=${MdConstants.artist}")
    suspend fun viewManga(@Path("id") id: String): Response<MangaDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}/{id}/feed?limit=500&includes[]=${MdConstants.scanlator}")
    suspend fun viewChapters(
        @Path("id") id: String,
        @Query(value = "locales[]") locales: List<String>,
        @Query("offset") offset: Int,
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
    suspend fun atHomeImageReport(@Body atHomeImageReportDto: AtHomeImageReportDto)
}