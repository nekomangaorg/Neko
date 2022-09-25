package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.CheckTokenDto
import eu.kanade.tachiyomi.source.online.models.dto.LoginRequestDto
import eu.kanade.tachiyomi.source.online.models.dto.LoginResponseDto
import eu.kanade.tachiyomi.source.online.models.dto.LogoutDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.MarkStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.RatingDto
import eu.kanade.tachiyomi.source.online.models.dto.RatingResponseDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadChapterDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusMapDto
import eu.kanade.tachiyomi.source.online.models.dto.ResultDto
import eu.kanade.tachiyomi.source.online.utils.MdApi
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexAuthService {

    @Headers("Cache-Control: no-cache")
    @POST(MdApi.login)
    suspend fun login(@Body request: LoginRequestDto): ApiResponse<LoginResponseDto>

    @Headers("Cache-Control: no-cache")
    @POST(MdApi.logout)
    suspend fun logout(): ApiResponse<LogoutDto>

    @Headers("Cache-Control: no-cache")
    @GET(MdApi.checkToken)
    suspend fun checkToken(): ApiResponse<CheckTokenDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.userFollows}?limit=100&includes[]=${MdConstants.Types.coverArt}")
    suspend fun userFollowList(@Query("offset") offset: Int): ApiResponse<MangaListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}/{id}/status")
    suspend fun readingStatusForManga(
        @Path("id") mangaId: String,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): ApiResponse<ReadingStatusDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdApi.manga}/{id}/read")
    suspend fun readChaptersForManga(
        @Path("id") mangaId: String,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): ApiResponse<ReadChapterDto>

    @POST("${MdApi.manga}/{id}/status")
    suspend fun updateReadingStatusForManga(
        @Path("id") mangaId: String,
        @Body readingStatusDto: ReadingStatusDto,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): ApiResponse<ResultDto>

    @GET(MdApi.readingStatusForAllManga)
    suspend fun readingStatusAllManga(@Header("Cache-Control") cacheControl: String = "no-cache"): ApiResponse<ReadingStatusMapDto>

    @GET(MdApi.readingStatusForAllManga)
    suspend fun readingStatusByType(
        @Query("status") status: String,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): ApiResponse<ReadingStatusMapDto>

    @POST("${MdApi.manga}/{id}/read")
    suspend fun markStatusForMultipleChapters(
        @Path("id") mangaId: String,
        @Body markStatusDto: MarkStatusDto,
    ): ApiResponse<ResultDto>

    @POST("${MdApi.manga}/{id}/follow")
    suspend fun followManga(@Path("id") mangaId: String): ApiResponse<ResultDto>

    @DELETE("${MdApi.manga}/{id}/follow")
    suspend fun unfollowManga(@Path("id") mangaId: String): ApiResponse<ResultDto>

    @GET(MdApi.rating)
    suspend fun retrieveRating(@Query("manga[]") mangaId: String): ApiResponse<RatingResponseDto>

    @POST("${MdApi.rating}/{id}")
    suspend fun updateRating(
        @Path("id") mangaId: String,
        @Body ratingDto: RatingDto,
    ): ApiResponse<ResultDto>

    @DELETE("${MdApi.rating}/{id}")
    suspend fun removeRating(@Path("id") mangaId: String): ApiResponse<ResultDto>
}
