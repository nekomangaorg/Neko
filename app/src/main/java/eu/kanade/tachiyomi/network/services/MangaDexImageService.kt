package eu.kanade.tachiyomi.network.services

import eu.kanade.tachiyomi.source.online.dto.AtHomeDto
import eu.kanade.tachiyomi.source.online.utils.MdApi
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexImageService {

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.atHomeServer}/{chapterId}")
    suspend fun getAtHomeServer(
        @Path("chapterId") chapterId: String,
        @Query("forcePort443") forcePort443: Boolean,
    ): Response<AtHomeDto>
}
