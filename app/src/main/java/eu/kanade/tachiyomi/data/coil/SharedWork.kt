package eu.kanade.tachiyomi.data.coil

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * Runs one piece of work per key and gives its result to every caller that asks for that key while
 * it runs. The work runs in this class's own scope, so cancelling a caller ends only that caller's
 * wait.
 */
internal class SharedWork<T> {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = ConcurrentHashMap<String, Deferred<T>>()

    suspend fun await(key: String, work: suspend () -> T): T {
        var created: Deferred<T>? = null
        val deferred =
            active.compute(key) { _, existing ->
                existing?.takeIf { it.isActive } ?: scope.async { work() }.also { created = it }
            }!!
        // A newer call can put its own work under the key after clear(), or once this work is
        // done, so remove the entry only while it still holds this work.
        created?.let { own -> own.invokeOnCompletion { active.remove(key, own) } }
        return deferred.await()
    }

    fun clear() {
        active.clear()
    }
}
