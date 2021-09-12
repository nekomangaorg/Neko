package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.source.online.models.dto.LoginRequestDto
import eu.kanade.tachiyomi.source.online.models.dto.RefreshTokenDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit

/*
 * Copyright (C) 2020 The Neko Manga Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
            .onError {
                log.e("error code returned ${this.statusCode}")
            }.onException {
                log.e("error exception", this.exception)
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
            val loginResponse = authService.login(loginRequest)

            if (loginResponse.code() == 200) {
                val loginResponseDto = loginResponse.body()!!
                preferences.setTokens(
                    loginResponseDto.token.refresh,
                    loginResponseDto.token.session
                )
                preferences.setSourceCredentials(MangaDex(), username, password)
                return@withContext true
            } else {
                preferences.setTokens("", "")
                return@withContext false
            }
        }
    }

    suspend fun login(): Boolean {
        val source = MangaDex()
        val username = preferences.sourceUsername(source)
        val password = preferences.sourcePassword(source)
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            log.i("No username or password stored, can't login")
            return false
        }
        return login(username, password)
    }
}
