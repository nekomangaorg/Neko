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

/**
 * Authenticator that intercepts 401 requests and tries to reauth the user
 */
class MangaDexTokenAuthenticator(private val loginHelper: MangaDexLoginHelper) :
    Authenticator {

    private val mutext = Mutex()
    private val tag = "||Neko-TokenAuthenticator"

    override fun authenticate(route: Route?, response: Response): Request? {
        loggycat(LogPriority.INFO, tag = tag) { "Detected Auth error ${response.code} on ${response.request.url}" }
        val token = refreshSessionToken(loginHelper)
        return if (token.isEmpty()) {
            null
        } else {
            response.request.newBuilder().header("Authorization", token).build()
        }
    }

    private fun refreshSessionToken(loginHelper: MangaDexLoginHelper): String {
        return runBlocking {
            mutext.withLock {

                var validated = loginHelper.wasTokenRefreshedRecently()
                if (validated) {
                    loggycat(LogPriority.INFO, tag = tag) { "Token is valid, other thread must have refreshed it" }
                }
                if (!validated) {
                    loggycat(LogPriority.INFO, tag = tag) { "Token is invalid trying to refresh" }
                    validated = loginHelper.refreshSessionToken()
                }

                if (!validated) {
                    loggycat(LogPriority.INFO, tag = tag) { "Unable to refresh token user will need to relogin" }
                    loginHelper.invalidate()
                }

                loginHelper.sessionToken()

                return@runBlocking when (validated) {
                    true -> "Bearer ${loginHelper.sessionToken()}"
                    false -> ""
                }
            }
        }
    }
}
