package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.CustomListDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.ProxyRetrofitQueryMap
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface CommonListFunctions {

    @GET("${MdConstants.Api.list}/{id}")
    suspend fun viewCustomListInfo(@Path("id") id: String): ApiResponse<CustomListDto>

    @GET("${MdConstants.Api.list}/{id}${MdConstants.Api.manga}")
    suspend fun viewCustomListManga(
        @Path("id") id: String,
        @QueryMap options: ProxyRetrofitQueryMap
    ): ApiResponse<MangaListDto>
}
