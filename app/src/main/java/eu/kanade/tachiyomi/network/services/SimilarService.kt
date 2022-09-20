package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.AnilistMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.models.dto.MUMangaDto
import eu.kanade.tachiyomi.source.online.models.dto.MalMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.models.dto.SimilarMangaDto
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SimilarService {

    @GET("https://api.similarmanga.com/similar/{id}.json")
    suspend fun getSimilarManga(@Path("id") mangaId: String): ApiResponse<SimilarMangaDto>

    @Headers("Content-Type: application/json")
    @POST("https://graphql.anilist.co/")
    suspend fun getAniListGraphql(@Query("query") query: String): ApiResponse<AnilistMangaRecommendationsDto>

    @GET("https://api.jikan.moe/v3/manga/{id}/recommendations")
    suspend fun getSimilarMalManga(@Path("id") mangaId: String): ApiResponse<MalMangaRecommendationsDto>

    @GET("https://api.mangaupdates.com/v1/series/{id}")
    suspend fun getSimilarMUManga(@Path("id") mangaId: String): ApiResponse<MUMangaDto>
}
