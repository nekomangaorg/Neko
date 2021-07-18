package eu.kanade.tachiyomi.network.services

import eu.kanade.tachiyomi.source.online.dto.MalMangaRecommendationsDto
import eu.kanade.tachiyomi.source.online.dto.SimilarMangaDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SimilarService {

    @GET("https://api.similarmanga.com/similar/{id}.json")
    suspend fun getSimilarManga(@Path("id") mangaId: String): Response<SimilarMangaDto>

    @GET("https://api.jikan.moe/v3/manga/{id}/recommendations")
    suspend fun getSimilarMalManga(@Path("id") mangaId: String): Response<MalMangaRecommendationsDto>
}
