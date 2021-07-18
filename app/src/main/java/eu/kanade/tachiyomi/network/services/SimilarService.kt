package eu.kanade.tachiyomi.network.services

import eu.kanade.tachiyomi.source.online.models.dto.AnilistMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.models.dto.MalMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.models.dto.SimilarMangaDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SimilarService {

    @GET("https://api.similarmanga.com/similar/{id}.json")
    suspend fun getSimilarManga(@Path("id") mangaId: String): Response<SimilarMangaDto>

    @Headers("Content-Type: application/json")
    @POST("https://graphql.anilist.co/")
    suspend fun getAniListGraphql(@Query("query") query: String): Response<AnilistMangaRecommendationsDto>

    @GET("https://api.jikan.moe/v3/manga/{id}/recommendations")
    suspend fun getSimilarMalManga(@Path("id") mangaId: String): Response<MalMangaRecommendationsDto>
}
