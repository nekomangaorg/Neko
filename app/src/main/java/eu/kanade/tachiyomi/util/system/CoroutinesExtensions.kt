package eu.kanade.tachiyomi.util.system

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

fun launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT, block)

fun launchNow(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED, block)
