package org.nekomanga.core.network.interceptor

import com.google.common.net.HttpHeaders
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor
import org.nekomanga.core.loggycat

fun loggingInterceptor(verboseLoggingProvider: () -> Boolean, json: Json): HttpLoggingInterceptor {
    val logger: HttpLoggingInterceptor.Logger =
        HttpLoggingInterceptor.Logger { message ->
            try {
                if (message.contains("grant_type=") || message.contains("access_token\":")) {
                    loggycat(tag = "|") { "Not logging request because it contained sessionToken || refreshToken" }
                } else {
                    val element = json.parseToJsonElement(message)
                    element.loggycat(tag = "|") { json.encodeToString(element) }
                }
            } catch (ex: Exception) {
                loggycat(tag = "|") { message }
            }
        }


    return HttpLoggingInterceptor(logger).apply {
        level = when (verboseLoggingProvider()) {
            true -> HttpLoggingInterceptor.Level.BODY
            false -> HttpLoggingInterceptor.Level.BASIC
        }
        redactHeader(HttpHeaders.AUTHORIZATION)
    }
}


