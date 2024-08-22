package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.models.dto.CustomListListDto
import eu.kanade.tachiyomi.source.online.models.dto.MarkStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.NewCustomListDto
import eu.kanade.tachiyomi.source.online.models.dto.RatingDto
import eu.kanade.tachiyomi.source.online.models.dto.RatingResponseDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadChapterDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusMapDto
import eu.kanade.tachiyomi.source.online.models.dto.ResultDto
import eu.kanade.tachiyomi.source.online.models.dto.UserDto
import org.nekomanga.constants.MdConstants
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexAuthorizedUserService : CommonListFunctions {

    @Headers("Cache-Control: no-cache")
    @GET(MdConstants.Api.userInfo)
    suspend fun getUserInfo(): ApiResponse<UserDto>

    @Headers("Cache-Control: no-cache")
    @GET(MdConstants.Api.listMigrated)
    suspend fun isMigrated(): ApiResponse<String>

    @Headers("Cache-Control: no-cache")
    @GET(MdConstants.Api.subscriptionFeed)
    suspend fun subscriptionFeed(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("translatedLanguage[]") translatedLanguages: List<String>,
        @Query("contentRating[]") contentRating: List<String>,
        @Query("excludedGroups[]") blockedScanlators: List<String>,
    ): ApiResponse<ChapterListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdConstants.Api.user}${MdConstants.Api.list}")
    suspend fun usersLists(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponse<CustomListListDto>

    @POST("${MdConstants.Api.manga}/{mangaId}${MdConstants.Api.list}/{listId}")
    suspend fun addToCustomList(
        @Path("mangaId") mangaUUID: String,
        @Path("listId") listUUID: String
    ): ApiResponse<ResultDto>

    @DELETE("${MdConstants.Api.manga}/{mangaId}${MdConstants.Api.list}/{listId}")
    suspend fun removeFromCustomList(
        @Path("mangaId") mangaUUID: String,
        @Path("listId") listUUID: String
    ): ApiResponse<ResultDto>

    @POST(MdConstants.Api.list)
    suspend fun createCustomList(@Body newCustomListDto: NewCustomListDto): ApiResponse<ResultDto>

    @DELETE("${MdConstants.Api.list}/{listId}")
    suspend fun deleteCustomList(@Path("listId") listUUID: String): ApiResponse<ResultDto>

    @GET("${MdConstants.Api.manga}/{mangaId}${MdConstants.Api.list}")
    suspend fun customListsContainingManga(
        @Path("mangaId") mangaUUID: String
    ): ApiResponse<CustomListListDto>

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

    @GET(MdConstants.Api.rating)
    suspend fun retrieveRating(@Query("manga[]") mangaId: String): ApiResponse<RatingResponseDto>

    @POST("${MdConstants.Api.rating}/{id}")
    suspend fun updateRating(
        @Path("id") mangaId: String,
        @Body ratingDto: RatingDto,
    ): ApiResponse<ResultDto>

    @DELETE("${MdConstants.Api.rating}/{id}")
    suspend fun removeRating(@Path("id") mangaId: String): ApiResponse<ResultDto>
}
