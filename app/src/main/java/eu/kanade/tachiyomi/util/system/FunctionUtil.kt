package eu.kanade.tachiyomi.util.system

import kotlin.time.measureTimedValue
import org.nekomanga.logging.TimberKt

inline fun <T> logTimeTaken(text: String? = null, block: () -> T): T {
    val result = measureTimedValue(block)
    TimberKt.d { "||NEKO- ${text ?: ""} took ${result.duration.inWholeMilliseconds} ms" }
    return result.value
}
