package eu.kanade.tachiyomi.source.online

import com.skydoves.sandwich.getOrThrow
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.online.models.dto.LoginResponseDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import java.util.concurrent.TimeUnit
import okhttp3.FormBody
import okhttp3.Headers
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.POST
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.injectLazy

class MangaDexLoginHelper {

    private val networkServices: NetworkServices by injectLazy()
    private val network: NetworkHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()
    private val trackManager: TrackManager by injectLazy()

    val tag = "||LoginHelper"

    fun wasTokenRefreshedRecently(): Boolean {
        val lastRefreshTime = preferences.lastRefreshTime().get()
        TimberKt.i { "$tag last refresh time $lastRefreshTime current time ${System.currentTimeMillis()}" }

        if ((lastRefreshTime + TimeUnit.MINUTES.toMillis(15)) > System.currentTimeMillis()) {
            TimberKt.i { "$tag Token was refreshed recently don't hit dex to check" }
            return true
        }

        return false
    }

    suspend fun refreshSessionToken(): Boolean {
        val refreshToken = preferences.refreshToken().get()
        if (refreshToken.isEmpty()) {
            TimberKt.i { "$tag refresh token is null can't extend session" }
            invalidate()
            return false
        }
        val formBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("grant_type", MdConstants.Login.refreshToken)
            .add("refresh_token", refreshToken)
            .add("code_verifier", preferences.codeVerifier().get())
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()
        val error = kotlin.runCatching {
            with(MdUtil.jsonParser) {
                val data = network.client.newCall(
                    POST(
                        url = MdConstants.Api.baseAuthUrl + MdConstants.Api.token,
                        body = formBody,
                    ),
                ).await().parseAs<LoginResponseDto>()

                preferences.setTokens(
                    data.refreshToken,
                    data.accessToken,
                )
            }

            trackManager.mdList.populateLists()

        }.exceptionOrNull()

        return when (error == null) {
            true -> true
            false -> {
                TimberKt.e(error) { "Error refreshing token" }
                invalidate()
                false
            }
        }
    }

    /** Login given the generated authorization code
     */
    suspend fun login(authorizationCode: String): Boolean {

        val loginFormBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("grant_type", MdConstants.Login.authorizationCode)
            .add("code", authorizationCode)
            .add("code_verifier", preferences.codeVerifier().get())
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val error = kotlin.runCatching {
            with(MdUtil.jsonParser) {
                val data = network.mangadexClient.newCall(
                    POST(
                        url = MdConstants.Api.baseAuthUrl + MdConstants.Api.token,
                        body = loginFormBody,
                    ),
                ).await().parseAs<LoginResponseDto>()

                preferences.setTokens(
                    data.refreshToken,
                    data.accessToken,
                )
            }

            val userInfo = networkServices.authService.getUserInfo().getOrThrow()
            preferences.setUserInfo(
                userInfo.data.id,
                userInfo.data.attributes.username,
            )

            trackManager.mdList.populateLists()

        }.exceptionOrNull()

        return when (error == null) {
            true -> true
            false -> {
                TimberKt.e(error) { "Error logging in" }
                invalidate()
                false
            }
        }
    }

    suspend fun logout(): Boolean {
        val sessionToken = preferences.sessionToken().get()
        val refreshToken = preferences.refreshToken().get()
        if (refreshToken.isEmpty() || sessionToken.isEmpty()) {
            invalidate()
            return true
        }

        val formBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("refresh_token", refreshToken)
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val error = kotlin.runCatching {
            network.mangadexClient.newCall(
                POST(
                    url = MdConstants.Api.baseAuthUrl + MdConstants.Api.logout,
                    headers = Headers.Builder().add("Authorization", "Bearer $sessionToken").build(),
                    body = formBody,
                ),
            ).await()
            invalidate()
        }.exceptionOrNull()

        return when (error == null) {
            true -> true
            false -> {
                TimberKt.e(error) { "Error logging out" }
                false
            }
        }
    }

    /**
     * Clears the session and refresh tokens
     */
    fun invalidate() {
        preferences.clearUserInfo()
        preferences.clearTokens()
    }

    fun isLoggedIn(): Boolean {
        return preferences.refreshToken().get().isNotEmpty() && preferences.sessionToken().get().isNotEmpty()
    }

    fun sessionToken(): String {
        return preferences.sessionToken().get()
    }
}
