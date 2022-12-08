package eu.kanade.tachiyomi.source.online

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.online.models.dto.ErrorResponse
import eu.kanade.tachiyomi.source.online.models.dto.LoginRequestDto
import eu.kanade.tachiyomi.source.online.models.dto.RefreshTokenDto
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.loggycat
import eu.kanade.tachiyomi.util.throws
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import logcat.LogPriority
import uy.kohesive.injekt.injectLazy

class MangaDexLoginHelper {

    val networkHelper: NetworkHelper by injectLazy()
    val authService: MangaDexAuthService by lazy { networkHelper.authService }
    val service: MangaDexService by lazy { networkHelper.service }
    val preferences: PreferencesHelper by injectLazy()
    private val json: Json by injectLazy()

    val tag = "||LoginHelper"

    suspend fun isAuthenticated(): Boolean {
        val lastRefreshTime = preferences.lastRefreshTime()
        loggycat(LogPriority.INFO, tag = tag) {
            """
             last refresh time $lastRefreshTime
             current time ${System.currentTimeMillis()}
            """.trimIndent()
        }
        if ((lastRefreshTime + TimeUnit.MINUTES.toMillis(15)) > System.currentTimeMillis()) {
            loggycat(LogPriority.INFO, tag = tag) { "Token was refreshed recently dont hit dex to check" }
            return true
        }
        loggycat(LogPriority.INFO, tag = tag) { "token was not refreshed recently hit dex auth check" }
        authService.checkToken()
        val checkTokenResponse = authService.checkToken()
            .onFailure {
                this.log("checking token")
            }

        val authenticated = checkTokenResponse.getOrNull()?.isAuthenticated ?: false

        loggycat(LogPriority.INFO, tag = tag) { "check token is authenticated $authenticated" }
        return authenticated
    }

    suspend fun refreshToken(): Boolean {
        val refreshToken = preferences.refreshToken()
        if (refreshToken.isNullOrEmpty()) {
            loggycat(LogPriority.INFO, tag = tag) { "refresh token is null can't refresh token" }
            return false
        }

        loggycat(LogPriority.INFO, tag = tag) { "refreshing token" }

        val refreshTokenResponse = service.refreshToken(RefreshTokenDto(refreshToken))
            .onFailure {
                this.log("trying to refresh token")
            }
        val refreshTokenDto = refreshTokenResponse.getOrNull()
        val result = refreshTokenDto?.result == "ok"
        if (result) {
            preferences.setTokens(
                refreshTokenDto!!.token.refresh,
                refreshTokenDto.token.session,
            )
        } else {
            loggycat(LogPriority.ERROR) { "error refreshing token" }
        }
        return result
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

    suspend fun login(): Boolean {
        val source = MangaDex()
        val username = preferences.sourceUsername(source)
        val password = preferences.sourcePassword(source)
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            loggycat(LogPriority.ERROR) { "No username or password stored, can't login" }
            return false
        }
        return login(username, password)
    }

    suspend fun reAuthIfNeeded() {
        if (!isAuthenticated() && !refreshToken()) {
            login()
        }
    }
}
