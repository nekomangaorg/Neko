package org.nekomanga.core.network.interceptor

import com.google.common.net.HttpHeaders
import java.util.UUID
import okhttp3.Interceptor
import okhttp3.Response

class HeadersInterceptor(private val referer: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .removeHeader(HttpHeaders.USER_AGENT)
                .header(HttpHeaders.USER_AGENT, "Neko " + System.getProperty("http.agent"))
                .header(HttpHeaders.REFERER, referer)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("x-request-id", "Neko-" + UUID.randomUUID())
                .build()

        return chain.proceed(request)
    }
}
