package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.LoginResponseDto
import eu.kanade.tachiyomi.source.online.utils.MdApi
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface MangaDexOAuthService {
    @FormUrlEncoded
    @POST(MdApi.token)
    suspend fun retrieveTokens(@FieldMap fields: Map<String, String>): ApiResponse<LoginResponseDto>
}
