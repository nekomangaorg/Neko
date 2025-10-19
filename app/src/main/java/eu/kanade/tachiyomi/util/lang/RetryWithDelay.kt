package eu.kanade.tachiyomi.util.lang

import kotlinx.coroutines.delay

suspend fun <T> retryCoroutine(
    maxRetries: Int = 1,
    retryStrategy: (Int) -> Int = { 1000 },
    block: suspend () -> T,
): T {
    var a = 0
    while (a < maxRetries) {
        try {
            return block()
        } catch (e: Exception) {
            a++
            if (a == maxRetries) {
                throw e
            }
            delay(retryStrategy(a).toLong())
        }
    }
    // This should not be reached
    error("Unexpected loop termination")
}
