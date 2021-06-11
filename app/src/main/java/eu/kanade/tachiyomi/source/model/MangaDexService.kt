package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.source.online.handlers.dto.ChapterDto
import eu.kanade.tachiyomi.source.online.handlers.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.handlers.dto.MangaDto
import eu.kanade.tachiyomi.source.online.handlers.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.utils.MdApi
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MangaDexService {

    @GET("${MdApi.manga}?includes[]=${MdConstants.coverArt}")
    suspend fun search(@QueryMap options: MutableMap<String, Any>): Response<MangaListDto>

    @GET("${MdApi.manga}/{id}?includes[]=${MdConstants.coverArt}&includes[]=${MdConstants.author}&includes[]=${MdConstants.artist}")
    suspend fun viewManga(@Path("id") id: String): Response<MangaDto>

    @GET("${MdApi.manga}/{id}/feed?limit=500&includes[]=${MdConstants.scanlator}")
    suspend fun viewChapters(
        @Path("id") id: String,
        @Query(value = "locales[]") locales: List<String>,
        @Query("offset") offset: Int,
    ): Response<ChapterListDto>

    @GET("${MdApi.chapter}/{id}")
    suspend fun viewChapter(@Path("id") id: String): Response<ChapterDto>

    @GET("${MdApi.manga}/random")
    suspend fun randomManga(): Response<MangaDto>
}