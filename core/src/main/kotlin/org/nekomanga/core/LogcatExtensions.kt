package org.nekomanga.core

import com.google.firebase.crashlytics.FirebaseCrashlytics
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

inline fun Any.loggycat(
    priority: LogPriority = LogPriority.DEBUG,
    throwable: Throwable? = null,
    tag: String? = null,
    message: () -> String = { "" },
) = logcat(priority = priority, tag = tag) {
    var msg = message()
    if (throwable != null) {
        if (msg.isNotBlank()) msg += "\n"
        msg += throwable.asLog()
    }

    if (priority == LogPriority.ERROR && !BuildConfig.DEBUG) {
        try {
            FirebaseCrashlytics.getInstance().log(msg)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
        }
    }
    msg
}

inline fun loggycat(
    tag: String,
    priority: LogPriority = LogPriority.DEBUG,
    throwable: Throwable? = null,
    message: () -> String = { "" },
) {
    with(AndroidLogcatLogger(LogPriority.VERBOSE)) {
        this.loggycat(priority = priority, throwable = throwable, tag = tag, message = message)
    }
}
