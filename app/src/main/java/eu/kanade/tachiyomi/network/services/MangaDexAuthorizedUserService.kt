package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.ForumThreadDto
import eu.kanade.tachiyomi.source.online.models.dto.ForumThreadResponseDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.MarkStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.RatingDto
import eu.kanade.tachiyomi.source.online.models.dto.RatingResponseDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadChapterDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusMapDto
import eu.kanade.tachiyomi.source.online.models.dto.ResultDto
import org.nekomanga.constants.MdConstants
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexAuthorizedUserService {

    @Headers("Cache-Control: no-cache")
    @GET("${MdConstants.Api.userFollows}?limit=100&includes[]=${MdConstants.Types.coverArt}")
    suspend fun userFollowList(@Query("offset") offset: Int): ApiResponse<MangaListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdConstants.Api.manga}/{id}/status")
    suspend fun readingStatusForManga(
        @Path("id") mangaId: String,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): ApiResponse<ReadingStatusDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdConstants.Api.manga}/{id}/read")
    suspend fun readChaptersForManga(
        @Path("id") mangaId: String,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): ApiResponse<ReadChapterDto>

    @POST("${MdConstants.Api.manga}/{id}/status")
    suspend fun updateReadingStatusForManga(
        @Path("id") mangaId: String,
        @Body readingStatusDto: ReadingStatusDto,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): ApiResponse<ResultDto>

    @GET(MdConstants.Api.readingStatusForAllManga)
    suspend fun readingStatusAllManga(
        @Header("Cache-Control") cacheControl: String = "no-cache"
    ): ApiResponse<ReadingStatusMapDto>

    @GET(MdConstants.Api.readingStatusForAllManga)
    suspend fun readingStatusByType(
        @Query("status") status: String,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): ApiResponse<ReadingStatusMapDto>

    @POST("${MdConstants.Api.manga}/{id}/read")
    suspend fun markStatusForMultipleChapters(
        @Path("id") mangaId: String,
        @Body markStatusDto: MarkStatusDto,
    ): ApiResponse<ResultDto>

    @POST("${MdConstants.Api.manga}/{id}/follow")
    suspend fun followManga(@Path("id") mangaId: String): ApiResponse<ResultDto>

    @DELETE("${MdConstants.Api.manga}/{id}/follow")
    suspend fun unfollowManga(@Path("id") mangaId: String): ApiResponse<ResultDto>

    @GET(MdConstants.Api.rating)
    suspend fun retrieveRating(@Query("manga[]") mangaId: String): ApiResponse<RatingResponseDto>

    @POST("${MdConstants.Api.rating}/{id}")
    suspend fun updateRating(
        @Path("id") mangaId: String,
        @Body ratingDto: RatingDto,
    ): ApiResponse<ResultDto>

    @DELETE("${MdConstants.Api.rating}/{id}")
    suspend fun removeRating(@Path("id") mangaId: String): ApiResponse<ResultDto>

    @POST(MdConstants.Api.createThread)
    suspend fun createForumThread(
        @Body forumThreadDto: ForumThreadDto
    ): ApiResponse<ForumThreadResponseDto>
}
