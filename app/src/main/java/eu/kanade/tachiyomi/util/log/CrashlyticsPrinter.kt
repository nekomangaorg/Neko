package eu.kanade.tachiyomi.util.log

import com.elvishew.xlog.printer.Printer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.kanade.tachiyomi.BuildConfig

class CrashlyticsPrinter(private val logLevel: Int) : Printer {
    /**
     * Print log in new line.
     *
     * @param logLevel the level of log
     * @param tag the tag of log
     * @param msg the msg of log
     */
    override fun println(logLevel: Int, tag: String?, msg: String?) {
        if (logLevel >= this.logLevel && msg != null) {
            try {
                FirebaseCrashlytics.getInstance().log(msg)
            } catch (t: Throwable) {
                // Crash in debug if shit like this happens
                if (BuildConfig.DEBUG) throw t
            }
        }
    }
}
