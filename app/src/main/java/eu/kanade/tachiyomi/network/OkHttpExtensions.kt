package eu.kanade.tachiyomi.network

import eu.kanade.tachiyomi.source.online.utils.MdUtil
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
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
        val requestArbiter = object : AtomicBoolean(), Producer, Subscription {
            override fun request(n: Long) {
                if (n == 0L || !compareAndSet(false, true)) return

                try {
                    val response = call.execute()
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onNext(response)
                        subscriber.onCompleted()
                    }
                } catch (error: Exception) {
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onError(error)
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
suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(Exception("HTTP error ${response.code}"))
                        return
                    }

                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }
            },
        )

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Ignore cancel exception
            }
        }
    }
}

fun Call.asObservableSuccess(): Observable<Response> {
    return asObservable().doOnNext { response ->

        if (!response.isSuccessful) {
            response.close()
            if (response.code == 502) {
                throw Exception("MangaDex appears to be down, or under heavy load")
            } else if (response.code == 404) {
                throw Exception("Http error 404.  It is possible that MangaDex is down, or under heavy load")
            } else {
                throw Exception("HTTP error ${response.code}")
            }
        }
    }
}

fun OkHttpClient.newCallWithProgress(request: Request, listener: ProgressListener): Call {
    val progressClient = newBuilder()
        .cache(null)
        .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body!!, listener))
                .build()
        }
        .build()

    return progressClient.newCall(request)
}

inline fun <reified T> Response.parseAs(): T {
    this.use {
        val responseBody = it.body?.string().orEmpty()
        return MdUtil.jsonParser.decodeFromString(responseBody)
    }
}
