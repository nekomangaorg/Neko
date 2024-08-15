package org.nekomanga.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun CoroutineScope.launchDelayed(timeMillis: Long = 150L, block: () -> Unit) {
    this.launch {
        delay(timeMillis)
        block()
    }
}

suspend fun <T> withDefContext(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Default, block)
