package eu.kanade.tachiyomi.util.system

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.suspendCancellableCoroutine
import org.nekomanga.logging.TimberKt
import rx.Emitter
import rx.Observable
import rx.Subscriber
import rx.Subscription

/*
 * Util functions for bridging RxJava and coroutines. Taken from TachiyomiEH/SY.
 */

suspend fun <T> Observable<T>.awaitSingle(): T = single().awaitOne()

private suspend fun <T> Observable<T>.awaitOne(): T = suspendCancellableCoroutine { cont ->
    cont.unsubscribeOnCancellation(
        subscribe(
            object : Subscriber<T>() {
                override fun onStart() {
                    request(1)
                }

                override fun onNext(t: T) {
                    cont.resume(t)
                }

                override fun onCompleted() {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            IllegalStateException("Should have invoked onNext")
                        )
                    }
                }

                override fun onError(e: Throwable) {
                    // Rx1 observable throws NoSuchElementException if cancellation happened before
                    // element emission. To mitigate this we try to atomically resume continuation
                    // with exception:
                    // if resume failed, then we know that continuation successfully cancelled
                    // itself
                    val token = cont.tryResumeWithException(e)
                    if (token != null) {
                        cont.completeResume(token)
                    }
                }
            }
        )
    )
}

internal fun <T> CancellableContinuation<T>.unsubscribeOnCancellation(sub: Subscription) =
    invokeOnCancellation {
        sub.unsubscribe()
    }

fun <T> runAsObservable(
    scope: CoroutineScope = GlobalScope,
    backpressureMode: Emitter.BackpressureMode = Emitter.BackpressureMode.NONE,
    block: suspend () -> T,
): Observable<T> {
    return Observable.create(
        { emitter ->
            val job =
                scope.launchIO {
                    try {
                        emitter.onNext(block())
                        emitter.onCompleted()
                    } catch (e: Throwable) {
                        // Ignore `CancellationException` as error, since it indicates "normal
                        // cancellation"
                        if (e !is CancellationException) {
                            TimberKt.e(e) { "Error in coroutine bridge" }
                            emitter.onError(e)
                        } else {
                            TimberKt.d { "Coroutine cancelled" }
                            emitter.onCompleted()
                        }
                    }
                }
            emitter.setCancellation { job.cancel() }
        },
        backpressureMode,
    )
}
