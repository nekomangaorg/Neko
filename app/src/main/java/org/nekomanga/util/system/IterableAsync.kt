package org.nekomanga.util.system

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): List<R> = coroutineScope {
    // Launch a coroutine for each item and collect the Deferred results
    val deferredResults = this@mapAsync.map { item -> async { transform(item) } }

    deferredResults.awaitAll()
}
