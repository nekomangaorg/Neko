package eu.kanade.tachiyomi.util.system

import kotlin.time.measureTimedValue
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.logging.TimberKt

inline fun <T> logTimeTaken(text: String? = null, block: () -> T): T {
    val result = measureTimedValue(block)
    TimberKt.d { "||NEKO- ${text ?: ""} took ${result.duration.inWholeMilliseconds} ms" }
    return result.value
}

inline fun <T> logTimeTakenSeconds(text: String? = null, block: () -> T): T {
    val result = measureTimedValue(block)
    TimberKt.d { "||NEKO- ${text ?: ""} took ${result.duration.inWholeSeconds} s" }
    return result.value
}

inline fun <T> saveTimeTaken(preferences: LibraryPreferences, block: () -> T): T {
    val result = measureTimedValue(block)
    val timeTaken = "${result.duration.inWholeSeconds}s"
    preferences.lastUpdateDuration().set(timeTaken)
    TimberKt.d { "||NEKO- LibraryUpdate took $timeTaken" }
    return result.value
}
