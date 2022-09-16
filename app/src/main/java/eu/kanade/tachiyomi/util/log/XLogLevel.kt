package eu.kanade.tachiyomi.util.log

import android.content.Context
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferenceKeys

enum class XLogLevel(val description: String) {
    MINIMAL("critical errors only"),
    EXTRA("log everything"),
    EXTREME("network inspection mode"),
    ;

    companion object {
        private var curLogLevel: Int? = null

        val currentLogLevel get() = values()[curLogLevel!!]

        fun init(context: Context) {
            curLogLevel = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PreferenceKeys.logLevel, if (BuildConfig.DEBUG) 2 else 0)
        }

        fun shouldLog(requiredLogLevel: XLogLevel): Boolean {
            return curLogLevel!! >= requiredLogLevel.ordinal
        }
    }
}
