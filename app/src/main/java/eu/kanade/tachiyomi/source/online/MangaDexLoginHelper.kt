package eu.kanade.tachiyomi.source.online

import android.app.Application
import android.content.Context
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.models.dto.LoginResponseDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.toast
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Response
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.POST
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.HttpException
import tachiyomi.core.network.await
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangaDexLoginHelper {

    private val networkHelper: NetworkHelper by lazy { Injekt.get() }
    private val preferences: MangaDexPreferences by injectLazy()
    private val context: Context by lazy { Injekt.get<Application>().applicationContext }

    val tag = "||LoginHelper"

    fun wasTokenRefreshedRecently(): Boolean {
        val lastRefreshTime = preferences.lastRefreshTime().get()
        TimberKt.i {
            "$tag last refresh time $lastRefreshTime current time ${System.currentTimeMillis()}"
        }

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
            toast("Refresh token null, logged out of MangaDex")
            unexpectedInvalidate()
            return false
        }

        repeat(MAX_REFRESH_ATTEMPTS) { attempt ->
            when (val outcome = attemptRefresh(refreshToken)) {
                is RefreshOutcome.Success -> {
                    preferences.setTokens(outcome.refreshToken, outcome.accessToken)
                    preferences.unexpectedLogout().set(false)
                    return true
                }
                is RefreshOutcome.Persistent -> {
                    TimberKt.e(outcome.cause) { "$tag refresh rejected by MangaDex" }
                    toast("Unable to refresh token, logged out of MangaDex")
                    unexpectedInvalidate()
                    return false
                }
                is RefreshOutcome.Transient -> {
                    TimberKt.w(outcome.cause) {
                        "$tag transient failure refreshing token (attempt ${attempt + 1}/$MAX_REFRESH_ATTEMPTS)"
                    }
                    if (attempt < MAX_REFRESH_ATTEMPTS - 1) {
                        delay(RETRY_BACKOFF_MILLIS)
                    }
                }
            }
        }

        TimberKt.w { "$tag transient failures exhausted refreshing token; leaving session intact" }
        return false
    }

    private suspend fun attemptRefresh(refreshToken: String): RefreshOutcome {
        val formBody =
            FormBody.Builder()
                .add("client_id", MdConstants.Login.clientId)
                .add("grant_type", MdConstants.Login.refreshToken)
                .add("refresh_token", refreshToken)
                .add("code_verifier", preferences.codeVerifier().get())
                .add("redirect_uri", MdConstants.Login.redirectUri)
                .build()
        val request =
            POST(url = MdConstants.Api.baseAuthUrl + MdConstants.Api.token, body = formBody)

        val response =
            try {
                networkHelper.client.newCall(request).await()
            } catch (e: IOException) {
                return RefreshOutcome.Transient(e)
            }

        return response.use { resp -> classifyResponse(resp) }
    }

    private fun classifyResponse(response: Response): RefreshOutcome {
        return when {
            response.isSuccessful ->
                runCatching { response.parseAs<LoginResponseDto>(MdUtil.jsonParser) }
                    .fold(
                        onSuccess = {
                            RefreshOutcome.Success(
                                refreshToken = it.refreshToken,
                                accessToken = it.accessToken,
                            )
                        },
                        onFailure = { RefreshOutcome.Persistent(it) },
                    )
            else -> classifyHttpFailure(response.code)
        }
    }

    /** Login given the generated authorization code */
    suspend fun login(authorizationCode: String): Boolean {

        val loginFormBody =
            FormBody.Builder()
                .add("client_id", MdConstants.Login.clientId)
                .add("grant_type", MdConstants.Login.authorizationCode)
                .add("code", authorizationCode)
                .add("code_verifier", preferences.codeVerifier().get())
                .add("redirect_uri", MdConstants.Login.redirectUri)
                .build()

        val error =
            kotlin
                .runCatching {
                    with(MdUtil.jsonParser) {
                        val data =
                            networkHelper.mangadexClient
                                .newCall(
                                    POST(
                                        url = MdConstants.Api.baseAuthUrl + MdConstants.Api.token,
                                        body = loginFormBody,
                                    )
                                )
                                .await()
                                .parseAs<LoginResponseDto>()
                        preferences.setTokens(data.refreshToken, data.accessToken)
                        preferences.unexpectedLogout().set(false)
                    }
                }
                .exceptionOrNull()

        return when (error == null) {
            true -> true
            false -> {
                TimberKt.e(error) { "Error logging in" }
                invalidate()
                false
            }
        }
    }

    fun toast(msg: String) {
        launchUI { context.toast(msg) }
    }

    suspend fun logout(): Boolean {
        val sessionToken = preferences.sessionToken().get()
        val refreshToken = preferences.refreshToken().get()
        if (refreshToken.isEmpty() || sessionToken.isEmpty()) {
            invalidate()
            preferences.unexpectedLogout().set(false)
            return true
        }

        val formBody =
            FormBody.Builder()
                .add("client_id", MdConstants.Login.clientId)
                .add("refresh_token", refreshToken)
                .add("redirect_uri", MdConstants.Login.redirectUri)
                .build()

        val error =
            kotlin
                .runCatching {
                    networkHelper.mangadexClient
                        .newCall(
                            POST(
                                url = MdConstants.Api.baseAuthUrl + MdConstants.Api.logout,
                                headers =
                                    Headers.Builder()
                                        .add("Authorization", "Bearer $sessionToken")
                                        .build(),
                                body = formBody,
                            )
                        )
                        .await()
                    invalidate()
                    preferences.unexpectedLogout().set(false)
                }
                .exceptionOrNull()

        return when (error == null) {
            true -> true
            false -> {
                TimberKt.e(error) { "Error logging out" }
                false
            }
        }
    }

    /** Clears the session and refresh tokens */
    fun invalidate() {
        preferences.removeMangaDexUserName()
        preferences.removeTokens()
    }

    /**
     * Marks the user as unexpectedly logged out (only if they were actually logged in) and clears
     * the session and refresh tokens. The flag is observed by UI surfaces that notify the user.
     */
    fun unexpectedInvalidate() {
        if (isLoggedIn()) {
            preferences.unexpectedLogout().set(true)
        }
        invalidate()
    }

    fun isLoggedIn(): Boolean {
        return preferences.refreshToken().get().isNotEmpty() &&
            preferences.sessionToken().get().isNotEmpty()
    }

    fun isLoggedInFlow(): Flow<Boolean> {
        return preferences
            .refreshToken()
            .changes()
            .combine(preferences.sessionToken().changes()) { refreshToken, sessionToken ->
                refreshToken.isNotEmpty() && sessionToken.isNotEmpty()
            }
            .distinctUntilChanged()
    }

    fun sessionToken(): String {
        return preferences.sessionToken().get()
    }

    fun refreshToken(): String {
        return preferences.refreshToken().get()
    }

    sealed interface RefreshOutcome {
        data class Success(val refreshToken: String, val accessToken: String) : RefreshOutcome

        data class Transient(val cause: Throwable) : RefreshOutcome

        data class Persistent(val cause: Throwable) : RefreshOutcome
    }

    companion object {
        private const val MAX_REFRESH_ATTEMPTS = 2
        private const val RETRY_BACKOFF_MILLIS = 500L

        internal fun classifyHttpFailure(code: Int): RefreshOutcome {
            return if (code in 500..599) {
                RefreshOutcome.Transient(HttpException(code))
            } else {
                RefreshOutcome.Persistent(HttpException(code))
            }
        }
    }
}
