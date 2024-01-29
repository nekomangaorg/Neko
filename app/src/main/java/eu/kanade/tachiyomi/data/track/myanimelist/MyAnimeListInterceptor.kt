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
        val originalRequest = chain.request()
        if (tokenExpired) {
            throw MALTokenExpired()
        }

        if (oauth == null) {
            throw IOException("MAL: User is not authenticated")
        }

        // Refresh access token if expired
        if (oauth!!.isExpired()) {
            setAuth(refreshToken(chain))
        }

        // Add the authorization header to the original request
        val authRequest =
            originalRequest
                .newBuilder()
                .addHeader("Authorization", "Bearer ${oauth!!.access_token}")
                .build()

        val response = chain.proceed(authRequest)
        val tokenIsExpired =
            response.headers["www-authenticate"]?.contains("The access token expired") ?: false

        // Retry the request once with a new token in case it was not already refreshed
        // by the is expired check before.
        if (response.code == 401 && tokenIsExpired) {
            response.close()

            val newToken = refreshToken(chain)
            setAuth(newToken)

            val newRequest =
                originalRequest
                    .newBuilder()
                    .addHeader("Authorization", "Bearer ${newToken.access_token}")
                    .build()

            return chain.proceed(newRequest)
        }

        return response
    }

    /**
     * Called when the user authenticates with MyAnimeList for the first time. Sets the refresh
     * token and the oauth object.
     */
    fun setAuth(oauth: OAuth?) {
        this.oauth = oauth
        myanimelist.saveOAuth(oauth)
    }

    private fun refreshToken(chain: Interceptor.Chain): OAuth {
        return runCatching {
                val oauthResponse = chain.proceed(MyAnimeListApi.refreshTokenRequest(oauth!!))

                if (oauthResponse.code == 401) {
                    myanimelist.setAuthExpired()
                }

                if (oauthResponse.isSuccessful) {
                    with(json) { oauthResponse.parseAs<OAuth>() }
                } else {
                    oauthResponse.close()
                    null
                }
            }
            .getOrNull() ?: throw MALTokenExpired()
    }
}

class MALTokenExpired : IOException("MAL: Login has expired")
