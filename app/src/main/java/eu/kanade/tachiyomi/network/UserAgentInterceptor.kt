package eu.kanade.tachiyomi.network

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        return if (originalRequest.header("User-Agent").isNullOrEmpty()) {
            val newRequest = originalRequest
                .newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
