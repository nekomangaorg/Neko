package eu.kanade.tachiyomi.crash

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.nekomanga.BuildConfig

/** Text of a crash report. Only [debugInfo] reads Android values; the rest is plain JVM code. */
object CrashReportText {

    const val LOGCAT_HEADING = "Logcat:\n"

    private val divider = "*".repeat(80)

    fun debugInfo(): String {
        return """
            App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}, ${BuildConfig.COMMIT_SHA}, ${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TIME})
            Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Android build ID: ${Build.DISPLAY}
            Device brand: ${Build.BRAND}
            Device manufacturer: ${Build.MANUFACTURER}
            Device name: ${Build.DEVICE}
            Device model: ${Build.MODEL}
            Device product name: ${Build.PRODUCT}
        """
            .trimIndent()
    }

    /**
     * The report up to its logcat section: [debugInfo], one `key: value` line for each entry of
     * [details], then [trace] between divider lines.
     */
    fun head(debugInfo: String, details: Map<String, String>, trace: String): String = buildString {
        append(debugInfo.trimEnd()).append('\n')
        details.forEach { (key, value) -> append(key).append(": ").append(value).append('\n') }
        append('\n')
        append(divider).append('\n')
        append("Exception that caused crash\n")
        append(divider).append('\n')
        append(trace.trimEnd()).append('\n')
        append(divider).append("\n\n")
    }

    fun logcatFailed(error: Throwable): String = "Logcat failed: $error\n"

    fun formatTime(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(millis))
}
