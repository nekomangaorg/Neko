package eu.kanade.tachiyomi.data.coil

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.nekomanga.logging.TimberKt

/** Result of one full page decode. */
internal sealed interface FullDecode<out T> {
    class Decoded<T>(val image: T) : FullDecode<T>

    /**
     * [permanent] is true when decoding the same bytes again gives the same failure, and false when
     * a later attempt can succeed, as after running out of memory.
     */
    class Failed(val permanent: Boolean) : FullDecode<Nothing>
}

/**
 * Full page decodes, shared through [SharedWork] by callers that ask for the same page while it
 * decodes. A page whose decode failed for good is not decoded again until a retry or [clear], so
 * its other slices and later scroll passes do not repeat the decode.
 */
internal class SharedFullDecodes<T : Any>(ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val work = SharedWork<FullDecode<T>>(ioDispatcher)
    private val failedKeys = ConcurrentHashMap.newKeySet<String>()

    /**
     * Returns the decoded page, or null when the decode failed now or failed for good before.
     * [retry] forgets an earlier failure and decodes the page again.
     */
    suspend fun await(
        key: String,
        retry: Boolean,
        decode: SharedWork<FullDecode<T>>.Run.() -> FullDecode<T>,
    ): T? {
        if (retry) {
            failedKeys.remove(key)
        } else if (key in failedKeys) {
            TimberKt.d { "Not decoding page $key again, its full decode failed" }
            return null
        }
        val result =
            work.await(key) {
                decode().also {
                    if (it is FullDecode.Failed && it.permanent) {
                        unlessCleared { failedKeys.add(key) }
                    }
                }
            }
        return (result as? FullDecode.Decoded)?.image
    }

    /**
     * Forgets running work and failed pages. The work is cleared first, so work that fails after
     * the failed pages are forgotten skips its mark.
     */
    fun clear() {
        work.clear()
        failedKeys.clear()
    }
}
