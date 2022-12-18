package eu.kanade.tachiyomi.network

import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.system.loggycat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(val loginHelper: MangaDexLoginHelper) :
    Authenticator {

    private val mutext = Mutex()
    private val tag = "||Neko-TokenAuthenticator"

    override fun authenticate(route: Route?, response: Response): Request? {
        loggycat(LogPriority.INFO, tag = tag) { "Detected Auth error ${response.code} on ${response.request.url}" }
        val token = refreshToken(loginHelper)
        return if (token.isEmpty()) {
            null
        } else {
            response.request.newBuilder().header("Authorization", token).build()
        }
    }

    fun refreshToken(loginHelper: MangaDexLoginHelper): String {
        var validated = false
        return runBlocking {
            mutext.withLock {
                val checkToken =
                    loginHelper.isAuthenticated()
                if (checkToken) {
                    loggycat(LogPriority.INFO, tag = tag) { "Token is valid, other thread must have refreshed it" }
                    validated = true
                }
                if (!validated) {
                    loggycat(LogPriority.INFO, tag = tag) { "Token is invalid trying to refresh" }
                    validated =
                        loginHelper.refreshToken()
                }

                if (!validated) {
                    loggycat(LogPriority.INFO, tag = tag) { "Did not refresh token, trying to login" }
                    validated = loginHelper.login()
                }
                return@runBlocking when {
                    validated -> "Bearer ${loginHelper.preferences.sessionToken()!!}"
                    else -> ""
                }
            }
        }
    }
}
