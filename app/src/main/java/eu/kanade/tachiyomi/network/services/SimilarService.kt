package eu.kanade.tachiyomi.network.services

import eu.kanade.tachiyomi.source.online.dto.SimilarMangaDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SimilarService {

    @GET("{id}.json")
    suspend fun getSimilarManga(@Path("id") mangaId: String): Response<SimilarMangaDto>
}
