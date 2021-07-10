package eu.kanade.tachiyomi.util.system

import com.elvishew.xlog.XLog
import kotlin.time.measureTimedValue

inline fun <T> logTimeTaken(text: String? = null, block: () -> T): T {
    val result = measureTimedValue(block)
    XLog.tag("||NEKO-TIMER").disableStackTrace()
        .d("${text ?: ""} took ${result.duration.inWholeMilliseconds} ms")
    return result.value
}
