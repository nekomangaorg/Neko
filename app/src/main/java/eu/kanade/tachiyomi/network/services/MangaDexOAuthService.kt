package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.CheckTokenDto
import eu.kanade.tachiyomi.source.online.models.dto.LoginResponseDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import okhttp3.FormBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface MangaDexOAuthService {

    @Headers("Cache-Control: no-cache")
    @POST(MdConstants.Login.tokenUrl)
    suspend fun retrieveTokens(@Body body: FormBody): ApiResponse<LoginResponseDto>

    @Headers("Cache-Control: no-cache")
    @POST(MdConstants.Login.tokenInspectionUrl)
    suspend fun checkToken(@Body body: FormBody): ApiResponse<CheckTokenDto>
}
