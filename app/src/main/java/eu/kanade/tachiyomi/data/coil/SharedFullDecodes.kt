package eu.kanade.tachiyomi.data.coil

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.nekomanga.logging.TimberKt

/**
 * Full page decodes, shared through [SharedWork] by callers that ask for the same page at the same
 * retry generation while it decodes. A page whose decode failed is not decoded again until it is
 * asked for at a higher retry generation or [clear] runs, so its other slices and later scroll
 * passes do not repeat the decode.
 */
internal class SharedFullDecodes<T : Any>(ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val work = SharedWork<T?>(ioDispatcher)

    /** The highest retry generation at which each failed page failed. */
    private val failedAt = ConcurrentHashMap<String, Int>()

    /**
     * Returns the decoded page, or null when the decode failed now or failed before at
     * [retryGeneration] or higher. A request at a higher generation never joins a decode that an
     * older generation started.
     */
    suspend fun await(
        key: String,
        retryGeneration: Int,
        decode: SharedWork<T?>.Run.() -> T?,
    ): T? {
        val failedGeneration = failedAt[key]
        if (failedGeneration != null && failedGeneration >= retryGeneration) {
            TimberKt.d { "Not decoding page $key again, its full decode failed" }
            return null
        }
        return work.await("$key@$retryGeneration") {
            decode().also { image ->
                unlessCleared {
                    if (image == null) {
                        failedAt.merge(key, retryGeneration) { old, new -> maxOf(old, new) }
                    } else {
                        failedAt.computeIfPresent(key) { _, failed ->
                            failed.takeIf { it > retryGeneration }
                        }
                    }
                }
            }
        }
    }

    /**
     * Forgets running work and failed pages. The work is cleared first, so work that fails after
     * the failed pages are forgotten skips its mark.
     */
    fun clear() {
        work.clear()
        failedAt.clear()
    }
}
