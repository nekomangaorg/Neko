package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.AggregateDto
import eu.kanade.tachiyomi.source.online.models.dto.AtHomeImageReportDto
import eu.kanade.tachiyomi.source.online.models.dto.AuthorListDto
import eu.kanade.tachiyomi.source.online.models.dto.ChapterDto
import eu.kanade.tachiyomi.source.online.models.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.models.dto.GroupListDto
import eu.kanade.tachiyomi.source.online.models.dto.LegacyIdDto
import eu.kanade.tachiyomi.source.online.models.dto.LegacyMappingDto
import eu.kanade.tachiyomi.source.online.models.dto.ListDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.RelationListDto
import eu.kanade.tachiyomi.source.online.models.dto.RelationshipDtoList
import eu.kanade.tachiyomi.source.online.models.dto.StatisticResponseDto
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.ProxyRetrofitQueryMap
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MangaDexService {

    @GET("${MdConstants.Api.manga}?includes[]=${MdConstants.Types.coverArt}")
    suspend fun search(@QueryMap options: ProxyRetrofitQueryMap): ApiResponse<MangaListDto>

    @GET(MdConstants.Api.author)
    suspend fun searchAuthor(
        @Query(value = "name") query: String,
        @Query(value = "limit") limit: Int,
    ): ApiResponse<AuthorListDto>

    @GET(MdConstants.Api.group)
    suspend fun searchGroup(
        @Query(value = "name") query: String,
        @Query(value = "limit") limit: Int,
    ): ApiResponse<GroupListDto>

    @GET("${MdConstants.Api.manga}?&order[createdAt]=desc&includes[]=${MdConstants.Types.coverArt}")
    suspend fun recentlyAdded(@QueryMap options: ProxyRetrofitQueryMap): ApiResponse<MangaListDto>

    @GET(
        "${MdConstants.Api.manga}?&order[followedCount]=desc&includes[]=${MdConstants.Types.coverArt}&hasAvailableChapters=true"
    )
    suspend fun popularNewReleases(
        @QueryMap options: ProxyRetrofitQueryMap
    ): ApiResponse<MangaListDto>

    @GET(
        "${MdConstants.Api.manga}/{id}?includes[]=${MdConstants.Types.coverArt}&includes[]=${MdConstants.Types.author}&includes[]=${MdConstants.Types.artist}"
    )
    suspend fun viewManga(@Path("id") id: String): ApiResponse<MangaDto>

    @GET("${MdConstants.Api.manga}/{id}/aggregate")
    suspend fun aggregateChapters(
        @Path("id") mangaId: String,
        @Query(value = "translatedLanguage[]") translatedLanguages: List<String>,
    ): ApiResponse<AggregateDto>

    @GET("${MdConstants.Api.statistics}${MdConstants.Api.manga}/{id}")
    suspend fun mangaStatistics(@Path("id") mangaId: String): ApiResponse<StatisticResponseDto>

    @GET("${MdConstants.Api.statistics}${MdConstants.Api.chapter}/{id}")
    suspend fun chapterStatistics(@Path("id") chapterId: String): ApiResponse<StatisticResponseDto>

    @GET("${MdConstants.Api.manga}/{id}/relation")
    suspend fun relatedManga(@Path("id") id: String): ApiResponse<RelationListDto>

    @Headers("Cache-Control: no-cache")
    @GET(
        "${MdConstants.Api.manga}/{id}/feed?limit=500&contentRating[]=${MdConstants.ContentRating.safe}&contentRating[]=${MdConstants.ContentRating.suggestive}&contentRating[]=${MdConstants.ContentRating.erotica}&contentRating[]=${MdConstants.ContentRating.pornographic}&includes[]=${MdConstants.Types.scanlator}&order[volume]=desc&order[chapter]=desc"
    )
    suspend fun viewChapters(
        @Path("id") id: String,
        @Query(value = "translatedLanguage[]") translatedLanguages: List<String>,
        @Query("offset") offset: Int,
    ): ApiResponse<ChapterListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdConstants.Api.chapter}?order[readableAt]=desc&includeFutureUpdates=0")
    suspend fun latestChapters(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("translatedLanguage[]") translatedLanguages: List<String>,
        @Query("contentRating[]") contentRating: List<String>,
        @Query("excludedGroups[]") blockedScanlators: List<String>,
    ): ApiResponse<ChapterListDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdConstants.Api.cover}?order[volume]=desc")
    suspend fun viewArtwork(
        @Query("manga[]") mangaUUID: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): ApiResponse<RelationshipDtoList>

    @Headers("Cache-Control: no-cache")
    @GET("${MdConstants.Api.chapter}/{id}")
    suspend fun viewChapter(@Path("id") id: String): ApiResponse<ChapterDto>

    @Headers("Cache-Control: no-cache")
    @GET("${MdConstants.Api.manga}/random")
    suspend fun randomManga(
        @Query("contentRating[]") contentRating: List<String>
    ): ApiResponse<MangaDto>

    @Headers("Cache-Control: no-cache")
    @GET(MdConstants.Api.group)
    suspend fun scanlatorGroup(@Query("name") scanlator: String): ApiResponse<GroupListDto>

    @GET("${MdConstants.Api.list}/{id}")
    suspend fun viewList(@Path("id") id: String): ApiResponse<ListDto>

    @POST(MdConstants.Api.legacyMapping)
    suspend fun legacyMapping(@Body legacyMapping: LegacyIdDto): ApiResponse<LegacyMappingDto>

    @POST(MdConstants.atHomeReportUrl)
    suspend fun atHomeImageReport(@Body atHomeImageReportDto: AtHomeImageReportDto)
}
