package eu.kanade.tachiyomi.network.services

import eu.kanade.tachiyomi.source.online.models.dto.CheckTokenDto
import eu.kanade.tachiyomi.source.online.models.dto.LoginRequestDto
import eu.kanade.tachiyomi.source.online.models.dto.LoginResponseDto
import eu.kanade.tachiyomi.source.online.models.dto.LogoutDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadChapterDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusMapDto
import eu.kanade.tachiyomi.source.online.models.dto.RefreshTokenDto
import eu.kanade.tachiyomi.source.online.models.dto.ResultDto
import eu.kanade.tachiyomi.source.online.utils.MdApi
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexAuthService : MangaDexImageService {

    @Headers("Cache-Control: no-cache")
    @POST(MdApi.login)
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @Headers("Cache-Control: no-cache")
    @POST(MdApi.logout)
    suspend fun logout(): Response<LogoutDto>

    @Headers("Cache-Control: no-cache")
    @GET(MdApi.checkToken)
    suspend fun checkToken(): Response<CheckTokenDto>

    @Headers("Cache-Control: no-cache")
    @POST(MdApi.refreshToken)
    suspend fun refreshToken(@Body request: RefreshTokenDto): Response<LoginResponseDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.userFollows}?limit=100") // &includes[]=${MdConstants.Type.coverArt}
    suspend fun userFollowList(@Query("offset") offset: Int): Response<MangaListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}/{id}/status")
    suspend fun readingStatusForManga(@Path("id") mangaId: String): Response<ReadingStatusDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}/{id}/read")
    suspend fun readChaptersForManga(@Path("id") mangaId: String): Response<ReadChapterDto>

    @Headers("Cache-Control: no-cache")
    @POST("${MdApi.manga}/{id}/status")
    suspend fun updateReadingStatusForManga(
        @Path("id") mangaId: String,
        @Body readingStatusDto: ReadingStatusDto,
    ): Response<ResultDto>

    @Headers("Cache-Control: no-cache")
    @GET(MdApi.readingStatusForAllManga)
    suspend fun readingStatusAllManga(): Response<ReadingStatusMapDto>

    @Headers("Cache-Control: no-cache")
    @GET(MdApi.readingStatusForAllManga)
    suspend fun readingStatusByType(@Query("status") status: String): Response<ReadingStatusMapDto>

    @POST("${MdApi.chapter}/{id}/read")
    suspend fun markChapterRead(@Path("id") chapterId: String): Response<ResultDto>

    @DELETE("${MdApi.chapter}/{id}/read")
    suspend fun markChapterUnRead(@Path("id") chapterId: String): Response<ResultDto>

    @POST("${MdApi.manga}/{id}/follow")
    suspend fun followManga(@Path("id") mangaId: String): Response<ResultDto>

    @DELETE("${MdApi.manga}/{id}/follow")
    suspend fun unfollowManga(@Path("id") mangaId: String): Response<ResultDto>
}
