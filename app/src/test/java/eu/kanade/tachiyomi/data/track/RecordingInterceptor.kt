package eu.kanade.tachiyomi.data.track

import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/**
 * Answers every call with a 200 and [responseBody] and records the request, so no request leaves
 * the test. Added to the base client, it runs before the interceptors a tracker API adds.
 */
class RecordingInterceptor(private val responseBody: String = "{}") : Interceptor {
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body =
            request.body?.let { requestBody -> Buffer().also(requestBody::writeTo).readUtf8() }
        requests += RecordedRequest(request.method, request.url.encodedPath, body)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody.toResponseBody("application/json".toMediaType()))
            .build()
    }
}

data class RecordedRequest(val method: String, val path: String, val body: String?)
