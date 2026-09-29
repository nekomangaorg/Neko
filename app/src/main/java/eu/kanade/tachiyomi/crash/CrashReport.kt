package eu.kanade.tachiyomi.crash

import android.content.Context
import android.os.Process
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The crash report folder. It keeps one report, plus a `pending` marker that stays until the user
 * has seen that report. Nothing here needs Injekt or coroutines, so the crash handler can call it
 * in the process that is dying. The write methods catch every error and return null instead.
 *
 * @param logcat writes the logcat of this process to the given file, or throws.
 */
class CrashReport(
    private val dir: File,
    private val logcat: (out: File) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** Writes [head] and the logcat of this process to a new report, then marks it pending. */
    fun saveCrash(head: String): File? {
        val report = write(head) ?: return null
        appendLogcat(report)
        deleteOtherReports(report)
        markPending()
        return report
    }

    /** Writes a report with only [head]. It runs no logcat and marks nothing pending. */
    fun saveTraceOnly(head: String): File? {
        val report = write(head) ?: return null
        deleteOtherReports(report)
        return report
    }

    /** The latest report if the user has not seen it yet, else null. */
    fun pendingReport(): File? = if (File(dir, PENDING).exists()) latestReport() else null

    fun latestReport(): File? = reports().maxByOrNull { it.name }

    /** The report called [name]. Only the name of a report in the folder matches. */
    fun report(name: String?): File? = reports().firstOrNull { it.name == name }

    fun markSeen() {
        runCatching { File(dir, PENDING).delete() }
    }

    private fun write(head: String): File? = runCatching {
        dir.mkdirs()
        File(dir, fileName(clock())).apply { writeText(head) }
    }
        .getOrNull()

    private fun appendLogcat(report: File) {
        val temp = File(dir, LOGCAT_TEMP)
        runCatching {
            temp.delete()
            report.appendText(CrashReportText.LOGCAT_HEADING)
            val error = runCatching { logcat(temp) }.exceptionOrNull()
            if (temp.isFile) {
                temp.inputStream().use { input ->
                    FileOutputStream(report, true).use { output -> input.copyTo(output) }
                }
            }
            if (error != null) {
                report.appendText(CrashReportText.logcatFailed(error))
            }
        }
        runCatching { temp.delete() }
    }

    private fun deleteOtherReports(keep: File) {
        reports().filter { it != keep }.forEach { runCatching { it.delete() } }
    }

    private fun markPending() {
        runCatching { File(dir, PENDING).createNewFile() }
    }

    private fun reports(): List<File> = runCatching {
        dir.listFiles { file ->
            file.isFile && file.name.startsWith(PREFIX) && file.name.endsWith(SUFFIX)
        }
    }
        .getOrNull()
        ?.toList()
        .orEmpty()

    companion object {
        const val DIR_NAME = "crash"
        const val PENDING = "pending"
        const val PREFIX = "neko_crash_log-"
        const val SUFFIX = ".txt"
        private const val LOGCAT_TEMP = "logcat.tmp"

        fun fileName(millis: Long): String =
            PREFIX + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(millis)) + SUFFIX

        fun forContext(context: Context): CrashReport =
            CrashReport(
                dir = File(context.filesDir, DIR_NAME),
                logcat = { out -> runLogcat(Process.myPid(), out) },
            )
    }
}
