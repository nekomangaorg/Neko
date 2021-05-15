package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.online.handlers.serializers.CheckTokenResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.LoginRequest
import eu.kanade.tachiyomi.source.online.handlers.serializers.LoginResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.RefreshTokenRequest
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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

    val network: NetworkHelper by injectLazy()
    val preferences: PreferencesHelper by injectLazy()

    suspend fun isAuthenticated(authHeaders: Headers): Boolean {
        val lastRefreshTime = preferences.lastRefreshTime()
        XLog.i("last refresh time $lastRefreshTime")
        XLog.i("current time ${System.currentTimeMillis()}")
        if ((lastRefreshTime + TimeUnit.MINUTES.toMillis(15)) > System.currentTimeMillis()) {
            XLog.i("Token was refreshed recently dont hit dex to check")
            return true
        }
        XLog.i("token was not refreshed recently hit dex auth check")
        val response = network.client.newCall(GET(MdUtil.checkTokenUrl, authHeaders, CacheControl.FORCE_NETWORK)).await()
        val body = MdUtil.jsonParser.decodeFromString<CheckTokenResponse>(response.body!!.string())
        return body.isAuthenticated
    }

    suspend fun refreshToken(authHeaders: Headers): Boolean {
        val refreshToken = preferences.refreshToken()
        if (refreshToken.isNullOrEmpty()) {
            XLog.i("refresh token is null can't refresh token")
            return false
        }
        val result = RefreshTokenRequest(refreshToken)
        val jsonString = MdUtil.jsonParser.encodeToString(RefreshTokenRequest.serializer(), result)
        val postResult = network.client.newCall(
            POST(
                MdUtil.refreshTokenUrl,
                authHeaders,
                jsonString.toRequestBody("application/json".toMediaType())
            )
        ).await()

        val jsonResponse = MdUtil.jsonParser.decodeFromString<LoginResponse>(postResult.body!!.string())
        preferences.setTokens(jsonResponse.token.refresh, jsonResponse.token.session)
        XLog.i("refreshing token sug")
        return jsonResponse.result == "ok"
    }

    suspend fun login(
        username: String,
        password: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {

            val loginRequest = LoginRequest(username, password)

            val jsonString = MdUtil.jsonParser.encodeToString(LoginRequest.serializer(), loginRequest)

            val postResult = network.client.newCall(
                POST(
                    url = MdUtil.loginUrl,
                    body = jsonString.toRequestBody("application/json".toMediaType())
                )
            ).await()

            if (postResult.code == 200) {

                val loginResponse = MdUtil.jsonParser.decodeFromString<LoginResponse>(postResult.body!!.string())
                preferences.setRefreshToken(loginResponse.token.refresh)
                preferences.setSessionToken(loginResponse.token.session)
                preferences.setSourceCredentials(MangaDex(), username, password)
                return@withContext true
            }
            return@withContext false
        }
    }

    suspend fun login(): Boolean {
        val source = MangaDex()
        val username = preferences.sourceUsername(source)
        val password = preferences.sourcePassword(source)
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            XLog.i("No username or password stored, can't login")
            return false
        }
        return login(username, password)
    }
}