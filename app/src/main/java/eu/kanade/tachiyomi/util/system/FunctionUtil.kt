package eu.kanade.tachiyomi.util.system

import kotlin.time.measureTimedValue

inline fun <T> logTimeTaken(text: String? = null, block: () -> T): T {
    val result = measureTimedValue(block)
    loggycat("||NEKO-TIMER") { "${text ?: ""} took ${result.duration.inWholeMilliseconds} ms" }
    return result.value
}
