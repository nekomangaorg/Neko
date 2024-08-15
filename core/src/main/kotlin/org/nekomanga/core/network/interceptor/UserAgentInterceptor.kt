package org.nekomanga.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import org.nekomanga.constants.Constants

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        return if (originalRequest.header("User-Agent").isNullOrEmpty()) {
            val newRequest =
                originalRequest
                    .newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", Constants.USER_AGENT)
                    .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
