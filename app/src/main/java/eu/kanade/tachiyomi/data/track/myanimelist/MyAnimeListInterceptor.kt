package eu.kanade.tachiyomi.data.track.myanimelist

import java.io.IOException
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.injectLazy

class MyAnimeListInterceptor(private val myanimelist: MyAnimeList) : Interceptor {

    private val json: Json by injectLazy()

    private var oauth: OAuth? = myanimelist.loadOAuth()
    private val tokenExpired
        get() = myanimelist.getIfAuthExpired()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (tokenExpired) {
            throw MALTokenExpired()
        }

        val originalRequest = chain.request()

        // Refresh access token if expired
        if (oauth?.isExpired() == true) {
            refreshToken(chain)
        }

        if (oauth == null) {
            throw IOException("MAL: User is not authenticated")
        }

        // Add the authorization header to the original request
        val authRequest =
            originalRequest
                .newBuilder()
                .addHeader("Authorization", "Bearer ${oauth!!.access_token}")
                .build()

        return chain.proceed(authRequest)
    }

    /**
     * Called when the user authenticates with MyAnimeList for the first time. Sets the refresh
     * token and the oauth object.
     */
    fun setAuth(oauth: OAuth?) {
        this.oauth = oauth
        myanimelist.saveOAuth(oauth)
    }

    private fun refreshToken(chain: Interceptor.Chain): OAuth =
        synchronized(this) {
            if (tokenExpired) throw MALTokenExpired()
            oauth
                ?.takeUnless { it.isExpired() }
                ?.let {
                    return@synchronized it
                }

            val response =
                try {
                    chain.proceed(MyAnimeListApi.refreshTokenRequest(oauth!!))
                } catch (_: Throwable) {
                    throw MALTokenRefreshFailed()
                }

            if (response.code == 401) {
                myanimelist.setAuthExpired()
                throw MALTokenExpired()
            }

            return runCatching {
                    if (response.isSuccessful) {
                        with(json) { response.parseAs<OAuth>() }
                    } else {
                        response.close()
                        null
                    }
                }
                .getOrNull()
                ?.also {
                    this.oauth = it
                    myanimelist.saveOAuth(it)
                } ?: throw MALTokenRefreshFailed()
        }
}

class MALTokenRefreshFailed : IOException("MAL: Failed to refresh account token")

class MALTokenExpired : IOException("MAL: Login has expired")
