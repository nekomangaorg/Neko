package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.online.handlers.serializers.CheckTokenResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.LoginRequest
import eu.kanade.tachiyomi.source.online.handlers.serializers.LoginResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.RefreshTokenRequest
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

/*
 * Copyright (C) 2020 The Neko Manga Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

class MangaDexLoginHelper(val client: OkHttpClient, val preferences: PreferencesHelper) {
    suspend fun isAuthenticated(authHeaders: Headers): Boolean {
        val response = client.newCall(GET(MdUtil.checkTokenUrl, authHeaders, CacheControl.FORCE_NETWORK)).await()
        val body = MdUtil.jsonParser.decodeFromString(CheckTokenResponse.serializer(), response.body!!.toString())
        return body.isAuthenticated
    }

    suspend fun refreshToken(authHeaders: Headers): Boolean {
        val refreshToken = preferences.refreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return false
        }
        val result = RefreshTokenRequest(refreshToken)
        val jsonString = MdUtil.jsonParser.encodeToString(RefreshTokenRequest.serializer(), result)
        val postResult = client.newCall(
            POST(
                MdUtil.refreshTokenUrl,
                authHeaders,
                jsonString.toRequestBody("application/json".toMediaType())
            )
        ).await()
        val refresh = runCatching {
            val jsonResponse = MdUtil.jsonParser.decodeFromString(LoginResponse.serializer(), postResult.body!!.toString())
            preferences.setTokens(jsonResponse.token.refresh, jsonResponse.token.session)
        }
        return refresh.isSuccess
    }

    suspend fun login(
        username: String,
        password: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {

            val loginRequest = LoginRequest(username, password)

            val jsonString = MdUtil.jsonParser.encodeToString(LoginRequest.serializer(), loginRequest)

            val postResult = client.newCall(
                POST(
                    MdUtil.loginUrl,
                    Headers.Builder().build(),
                    jsonString.toRequestBody("application/json".toMediaType())
                )
            ).await()

            //if it fails to parse then login failed
            val refresh = runCatching {
                MdUtil.jsonParser.decodeFromString(LoginResponse.serializer(), postResult.body!!.toString())
            }
            return@withContext postResult.code == 200 && refresh.isSuccess
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