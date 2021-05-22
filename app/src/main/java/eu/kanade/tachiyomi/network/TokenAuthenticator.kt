package eu.kanade.tachiyomi.network

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(val loginHelper: MangaDexLoginHelper) :
    Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        XLog.i("Detected Auth error ${response.code} on ${response.request.url}")
        XLog.i("header session key:  ${response.request.header("Authorization")}")

        val token = refreshToken(loginHelper)
        return if (token.isEmpty()) {
            null
        } else {
            response.request.newBuilder().header("Authorization", token).build()
        }
    }

    @Synchronized
    fun refreshToken(loginHelper: MangaDexLoginHelper): String {
        var validated: Boolean = false

        runBlocking {
            val checkToken = loginHelper.isAuthenticated(MdUtil.getAuthHeaders(Headers.Builder().build(), loginHelper.preferences))
            if (checkToken) {
                XLog.i("Token is valid, other thread must have refreshed it")
                validated = true
            }
            if (validated.not()) {
                XLog.i("Token is invalid trying to refresh")
                validated = loginHelper.refreshToken(MdUtil.getAuthHeaders(Headers.Builder().build(), loginHelper.preferences))
            }

            if (validated.not()) {
                XLog.i("Did not refresh token, trying to login")
                validated = loginHelper.login()
            }
        }
        return when {
            validated -> "bearer: ${loginHelper.preferences.sessionToken()!!}"
            else -> ""
        }
    }
}