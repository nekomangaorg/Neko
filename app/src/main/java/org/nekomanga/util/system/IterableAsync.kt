package org.nekomanga.util.system

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

suspend fun <T, R> Iterable<T>.mapAsync(
    launchDelayMillis: Long = 0L,
    transform: suspend (T) -> R,
): List<R> = coroutineScope {
    // Launch a coroutine for each item and collect the Deferred results
    val deferredResults = mutableListOf<Deferred<R>>()

    // Use a for-loop instead of .map to insert a delay
    for (item in this@mapAsync) {
        // Launch the new coroutine
        val deferred = async { transform(item) }

        // Add its Deferred to the list
        deferredResults.add(deferred)

        // If a delay is specified, wait before launching the next one
        if (launchDelayMillis > 0) {
            delay(launchDelayMillis)
        }
    }

    // Wait for all the launched jobs to complete
    deferredResults.awaitAll()

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
