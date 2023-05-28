package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.source.online.models.dto.CustomListDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface ListFunctions {

    @GET("${MdConstants.Api.list}/{id}")
    suspend fun viewCustomListInfo(@Path("id") id: String): ApiResponse<CustomListDto>

    @GET("${MdConstants.Api.list}/{id}${MdConstants.Api.manga}")
    suspend fun viewCustomListManga(@Path("id") id: String, @QueryMap options: ProxyRetrofitQueryMap): ApiResponse<MangaListDto>
}
