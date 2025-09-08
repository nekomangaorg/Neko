package org.nekomanga.util.system

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): List<R> = coroutineScope {
    // Launch a coroutine for each item and collect the Deferred results
    val deferredResults = this@mapAsync.map { item -> async { transform(item) } }

    deferredResults.awaitAll()
}

suspend fun <T, R : Any> Iterable<T>.mapAsyncNotNull(transform: suspend (T) -> R?): List<R> =
    coroutineScope {
        // Launch a coroutine for each item and collect the Deferred results
        val deferredResults = this@mapAsyncNotNull.map { item -> async { transform(item) } }
        deferredResults.awaitAll().filterNotNull()
    }

suspend fun <T> Iterable<T>.filterAsync(predicate: suspend (T) -> Boolean): List<T> =
    coroutineScope {
        val deferred = map { item -> async { item to predicate(item) } }
        deferred.awaitAll().filter { it.second }.map { it.first }
    }
