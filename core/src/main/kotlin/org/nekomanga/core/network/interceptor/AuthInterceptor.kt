package org.nekomanga.core.network.interceptor

import com.google.common.net.HttpHeaders
import okhttp3.Interceptor

fun authInterceptor(sessionTokenProvider: () -> String) = Interceptor { chain ->
    val newRequest =
        when {
            sessionTokenProvider().isBlank() -> chain.request()
            else -> {
                val originalRequest = chain.request()
                originalRequest
                    .newBuilder()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${sessionTokenProvider()}")
                    .method(originalRequest.method, originalRequest.body)
                    .build()
            }
        }
    chain.proceed(newRequest)
}
