package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaOAuth
import java.io.IOException
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.injectLazy

class MangaBakaInterceptor(private val mangaBaka: MangaBaka) : Interceptor {
    private val json: Json by injectLazy()

    private var currentAuth = mangaBaka.restoreToken()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (currentAuth == null) {
            throw IOException("MangaBaka: User is not authenticated")
        }
        val currentAuth = currentAuth!!

        if (currentAuth.isExpired()) {
            val response = chain.proceed(MangaBakaApi.refreshTokenRequest(currentAuth.refreshToken))
            if (response.isSuccessful) {
                setAuth(with(json) { response.parseAs() })
            } else {
                response.close()
            }
        }

        return originalRequest
            .newBuilder()
            .addHeader("Authorization", "Bearer ${currentAuth.accessToken}")
            .build()
            .let(chain::proceed)
    }

    fun setAuth(auth: MangaBakaOAuth?) {
        currentAuth = auth
        mangaBaka.saveToken(auth)
    }
}
