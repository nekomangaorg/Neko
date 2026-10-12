package eu.kanade.tachiyomi.util.system

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Think twice before using this. This is a delicate API. It is easy to accidentally create resource
 * or memory leaks when GlobalScope is used.
 *
 * **Possible replacements**
 * - suspend function
 * - custom scope like view or presenter scope
 */
@DelicateCoroutinesApi
fun launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT, block)

/**
 * Think twice before using this. This is a delicate API. It is easy to accidentally create resource
 * or memory leaks when GlobalScope is used.
 *
 * **Possible replacements**
 * - suspend function
 * - custom scope like view or presenter scope
 */
@DelicateCoroutinesApi
fun launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT, block)

/**
 * Think twice before using this. This is a delicate API. It is easy to accidentally create resource
 * or memory leaks when GlobalScope is used.
 *
 * **Possible replacements**
 * - suspend function
 * - custom scope like view or presenter scope
 */
@DelicateCoroutinesApi
fun launchNow(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED, block)

/**
 * Launches work the user already confirmed, such as a delete once its undo snackbar closes. Leaving
 * a screen clears its ViewModel before the snackbar reports that it closed, so work launched in
 * viewModelScope at that point never runs. The process scope outlives the screen.
 */
fun launchConfirmed(block: suspend CoroutineScope.() -> Unit): Job =
    ProcessLifecycleOwner.get().lifecycleScope.launchIO(block)

fun CoroutineScope.launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    launch(Dispatchers.Main, block = block)

fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    launch(Dispatchers.IO, block = block)

fun CoroutineScope.launchNonCancellable(block: suspend CoroutineScope.() -> Unit): Job = launchIO {
    withContext(NonCancellable, block)
}

suspend fun <T> withUIContext(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Main, block)

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.IO, block)

suspend fun <T> withNonCancellableContext(block: suspend CoroutineScope.() -> T) =
    withContext(NonCancellable, block)
