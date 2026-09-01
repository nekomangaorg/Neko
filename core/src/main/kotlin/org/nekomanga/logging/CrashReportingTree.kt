package org.nekomanga.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.CancellationException as JvmCancellationException
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t is CancellationException || t is JvmCancellationException) {
            return
        }

        val crashlytics =
            try {
                FirebaseCrashlytics.getInstance()
            } catch (ignored: IllegalStateException) {
                null
            }

        if (crashlytics != null) {
            if (priority == Log.ERROR) {
                crashlytics.log(message)
                if (t == null) {
                    crashlytics.recordException(Throwable(message))
                } else {
                    crashlytics.recordException(t)
                }
            } else if (priority == Log.WARN) {
                crashlytics.log(message)
            }
        }

        if (priority >= Log.INFO && Timber.forest().none { it is Timber.DebugTree }) {
            val logMessage =
                if (t != null) {
                    "$message\n${Log.getStackTraceString(t)}"
                } else {
                    message
                }
            Log.println(priority, tag ?: "Neko", logMessage)
        }
    }
}
