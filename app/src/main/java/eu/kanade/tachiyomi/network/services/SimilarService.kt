package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface SimilarService {

    @GET("https://cdn.jsdelivr.net/gh/nekomangaorg/similar-data@main/similar/{uuidTwoDigitPrefix}/{uuidThreeDigitPrefix}.html")
    suspend fun getSimilarMangaString(@Path("uuidTwoDigitPrefix") uuidTwoDigitPrefix: String, @Path("uuidThreeDigitPrefix") uuidThreeDigitPrefix: String): ApiResponse<String>
}
