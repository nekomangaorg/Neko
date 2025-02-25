package eu.kanade.tachiyomi.util.system

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import rx.Observable
import rx.Observer

fun <T : Any> Observable<T>.asFlow(): Flow<T> = callbackFlow {
    val observer =
        object : Observer<T> {
            override fun onNext(t: T) {
                trySend(t)
            }

            override fun onError(e: Throwable) {
                close(e)
            }

            override fun onCompleted() {
                close()
            }
        }
    val subscription = subscribe(observer)
    awaitClose { subscription.unsubscribe() }
}

fun <T : Any> Observable<T>.toHotFlow(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.Eagerly,
): Flow<T> =
    callbackFlow {
            val disposable =
                subscribe({ value -> trySend(value) }, { error -> close(error) }, { close() })
            awaitClose { disposable.unsubscribe() }
        }
        .shareIn(scope, started, replay = 0)
