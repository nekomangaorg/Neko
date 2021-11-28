package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.source.online.models.dto.LoginRequestDto
import eu.kanade.tachiyomi.source.online.models.dto.RefreshTokenDto
import eu.kanade.tachiyomi.util.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit

class MangaDexLoginHelper {

    val authService: MangaDexAuthService by lazy { Injekt.get<NetworkHelper>().authService }
    val preferences: PreferencesHelper by injectLazy()

    val log = XLog.tag("||LoginHelper")

    suspend fun isAuthenticated(): Boolean {
        val lastRefreshTime = preferences.lastRefreshTime()
        log.i("last refresh time $lastRefreshTime")
        log.i("current time ${System.currentTimeMillis()}")
        if ((lastRefreshTime + TimeUnit.MINUTES.toMillis(15)) > System.currentTimeMillis()) {
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

        val refreshTokenResponse = authService.refreshToken(RefreshTokenDto(refreshToken))
            .onError {
                log.e("error code returned ${this.statusCode}")
            }.onException {
                log.e("error exception", this.exception)
            }

        val refreshTokenDto = refreshTokenResponse.getOrNull()
        val result = refreshTokenDto?.result == "ok"
        log.i("refresh token was succesful : $result")
        if (result) {
            preferences.setTokens(
                refreshTokenDto!!.token.refresh,
                refreshTokenDto!!.token.session
            )
        } else {
            log.e("error refreshing token")
        }
        return result
    }

    suspend fun login(
        username: String,
        password: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val loginRequest = LoginRequestDto(username, password)
            return@withContext when (val loginResponse = authService.login(loginRequest)) {
                is ApiResponse.Failure<*> -> {
                    preferences.setTokens("", "")
                    loginResponse.log("trying to login")
                    false
                }
                else -> {
                    val loginResponseDto = loginResponse.getOrThrow()
                    preferences.setTokens(
                        loginResponseDto.token.refresh,
                        loginResponseDto.token.session
                    )
                    preferences.setSourceCredentials(MangaDex(), username, password)
                    true
                }
            }
        }
    }

    suspend fun login(): Boolean {
        val source = MangaDex()
        val username = preferences.sourceUsername(source)
        val password = preferences.sourcePassword(source)
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            XLog.e("No username or password stored, can't login")
            return false
        }
        return login(username, password)
    }
}
