package tachiyomi.core.network

import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.serializer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.Producer
import rx.Subscription

val jsonMime = "application/json; charset=utf-8".toMediaType()

fun Call.asObservable(): Observable<Response> {
    return Observable.unsafeCreate { subscriber ->
        // Since Call is a one-shot type, clone it for each new subscriber.
        val call = clone()

        // Wrap the call in a helper which handles both unsubscription and backpressure.
        val requestArbiter =
            object : AtomicBoolean(), Producer, Subscription {
                override fun request(n: Long) {
                    if (n == 0L || !compareAndSet(false, true)) return

                    try {
                        val response = call.execute()
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(response)
                            subscriber.onCompleted()
                        }
                    } catch (e: Exception) {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onError(e)
                        }
                    }
                }

                override fun unsubscribe() {
                    call.cancel()
                }

                override fun isUnsubscribed(): Boolean {
                    return call.isCanceled()
                }
            }

        subscriber.add(requestArbiter)
        subscriber.setProducer(requestArbiter)
    }
}

// Based on https://github.com/gildor/kotlin-coroutines-okhttp
private suspend fun Call.await(callStack: Array<StackTraceElement>): Response {
    return suspendCancellableCoroutine { continuation ->
        val callback =
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val exception =
                            Exception("HTTP error ${response.code}").apply {
                                stackTrace = callStack
                            }
                        continuation.resumeWithException(exception)
                        return
                    } else {
                        continuation.resume(response)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    val exception = IOException(e.message, e).apply { stackTrace = callStack }
                    continuation.resumeWithException(exception)
                }
            }

        enqueue(callback)

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Ignore cancel exception
            }
        }
    }
}

suspend fun Call.await(): Response {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    return await(callStack)
}

suspend fun Call.awaitSuccess(): Response {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    val response = await(callStack)
    if (!response.isSuccessful) {
        response.close()
        val exception = Exception("HTTP error ${response.code}")

        throw exception.apply { stackTrace = callStack }
    }
    return response
}

fun OkHttpClient.newCachelessCallWithProgress(request: Request, listener: ProgressListener): Call {
    val progressClient =
        newBuilder()
            .cache(null)
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse
                    .newBuilder()
                    .body(ProgressResponseBody(originalResponse.body, listener))
                    .build()
            }
            .build()

    return progressClient.newCall(request)
}

context(Json)
inline fun <reified T> Response.parseAs(): T {
    return decodeFromJsonResponse(serializer(), this)
}

context(Json)
fun <T> decodeFromJsonResponse(
    deserializer: DeserializationStrategy<T>,
    response: Response,
): T {
    return response.body.source().use { decodeFromBufferedSource(deserializer, it) }
}
