package org.nekomanga.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log(message)

        if (priority == Log.ERROR) {
            if (t == null) {
                crashlytics.recordException(Throwable(message))
            } else {
                crashlytics.recordException(t)
            }
        }
        if (priority == Log.WARN) {
            crashlytics.log(message)
        }

        if (priority >= Log.INFO) {
            val logMessage = if (t != null) {
                "$message\n${Log.getStackTraceString(t)}"
            } else {
                message
            }
            Log.println(priority, tag ?: "Neko", logMessage)
        }
    }
}
