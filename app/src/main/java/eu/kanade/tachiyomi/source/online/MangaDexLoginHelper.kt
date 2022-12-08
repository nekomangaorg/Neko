package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.online.models.dto.ErrorResponse
import eu.kanade.tachiyomi.source.online.models.dto.LoginRequestDto
import eu.kanade.tachiyomi.source.online.models.dto.LoginResponseDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.throws
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import uy.kohesive.injekt.injectLazy

class MangaDexLoginHelper {

    val networkHelper: NetworkHelper by injectLazy()
    val authService: MangaDexAuthorizedUserService by lazy { networkHelper.authService }
    val service: MangaDexService by lazy { networkHelper.service }
    val preferences: PreferencesHelper by injectLazy()
    private val json: Json by injectLazy()

    val log = XLog.tag("||LoginHelper")

    suspend fun isAuthenticated(): Boolean {
        val lastRefreshTime = preferences.lastRefreshTime()
        log.i("last refresh time $lastRefreshTime")
        log.i("current time ${System.currentTimeMillis()}")
        if ((lastRefreshTime + TimeUnit.SECONDS.toMillis(10)) > System.currentTimeMillis()) {
            log.i("Token was refreshed recently dont hit dex to check")
            return true
        }
        log.i("token was not refreshed recently hit dex auth check")

        authService.checkToken()
        val checkTokenResponse = authService.checkToken()
            .onFailure {
                this.log("checking token")
            }

        val authenticated = checkTokenResponse.getOrNull()?.isAuthenticated ?: false

        log.i("check token is authenticated $authenticated")
        return authenticated
    }

    suspend fun refreshToken(): Boolean {
        val refreshToken = preferences.refreshToken()
        if (refreshToken.isNullOrEmpty()) {
            log.i("refresh token is null can't refresh token")
            return false
        }
        log.i("refreshing token")

        val formBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("code_verifier", preferences.codeVerifer())
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val response = networkHelper.client.newCall(POST(MdConstants.Login.tokenUrl, body = formBody)).await().parseAs<LoginResponseDto>()

        preferences.setTokens(
            response.refresh_token,
            response.access_token,
        )
        return true
    }

    suspend fun login(
        username: String,
        password: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val loginRequest = LoginRequestDto(username, password)

            val loginResponseDto = authService.login(loginRequest).onFailure {
                preferences.setTokens("", "")
                val type = "trying to login"
                if (this is ApiResponse.Failure.Error && this.response.errorBody() != null) {
                    val error = json.decodeFromString<ErrorResponse>(this.response.errorBody()!!.string())
                    if (error.errors.isNotEmpty()) {
                        val message = error.errors.first().detail ?: error.errors.first().title ?: "Unable to parse json error"
                        this.log(message)
                        throw Exception(message)
                    }
                }

                this.log(type)
                this.throws(type)
            }.getOrThrow()

            preferences.setTokens(
                loginResponseDto.token.refresh,
                loginResponseDto.token.session,
            )
            preferences.setSourceCredentials(MangaDex(), username, password)
            return@withContext true
        }
    }

    suspend fun reAuthIfNeeded() {
        if (!isAuthenticated() && !refreshToken()) {
            login()
        }
    }

    suspend fun login(authorizationCode: String) {
        val formBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("grant_type", "authorization_code")
            .add("code", authorizationCode)
            .add("code_verifier", preferences.codeVerifer())
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val response = networkHelper.client.newCall(POST(MdConstants.Login.tokenUrl, body = formBody)).await().parseAs<LoginResponseDto>()
        preferences.setTokens(
            response.refresh_token,
            response.access_token,
        )
    }
}
