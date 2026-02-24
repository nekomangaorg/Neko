package org.nekomanga.core.network.interceptor

import com.google.common.net.HttpHeaders
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor
import org.nekomanga.logging.TimberKt

fun loggingInterceptor(verboseLoggingProvider: () -> Boolean, json: Json): HttpLoggingInterceptor {
    val logger: HttpLoggingInterceptor.Logger =
        HttpLoggingInterceptor.Logger { message ->
            try {
                if (message.contains("grant_type=") || message.contains("""access_token:""")) {
                    TimberKt.d {
                        "Not logging request because it contained sessionToken || refreshToken"
                    }
                } else {
                    val element = json.parseToJsonElement(message)
                    TimberKt.d { json.encodeToString(element) }
                }
            } catch (ex: Exception) {
                TimberKt.d { message }
            }
        }

    return HttpLoggingInterceptor(logger).apply {
        level =
            when (verboseLoggingProvider()) {
                true -> HttpLoggingInterceptor.Level.HEADERS
                false -> HttpLoggingInterceptor.Level.BASIC
            }
        redactHeader(HttpHeaders.AUTHORIZATION)
        redactHeader(HttpHeaders.COOKIE)
        redactHeader(HttpHeaders.SET_COOKIE)
    }
}
