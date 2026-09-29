package eu.kanade.tachiyomi.crash

import android.app.Application
import android.os.Build
import java.io.File

enum class CrashRoute {
    /** Save a report, then show the crash screen. */
    CrashScreen,

    /** Save a report, then let the previous handler end the process. */
    DefaultHandler,

    /** The crash screen crashed. No report and no crash screen, so it cannot loop. */
    DefaultHandlerWithoutReport,
}

const val ERROR_HANDLER_PROCESS_SUFFIX = ":error_handler"

fun crashRoute(isErrorHandlerProcess: Boolean, isActivityVisible: Boolean): CrashRoute =
    when {
        isErrorHandlerProcess -> CrashRoute.DefaultHandlerWithoutReport
        isActivityVisible -> CrashRoute.CrashScreen
        else -> CrashRoute.DefaultHandler
    }

fun isErrorHandlerProcess(processName: String): Boolean =
    processName.endsWith(ERROR_HANDLER_PROCESS_SUFFIX)

/** The name of this process. [Application.getProcessName] needs API 28. */
fun currentProcessName(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Application.getProcessName()
    } else {
        processNameFromCmdline(File("/proc/self/cmdline"))
    }

/** The process name from a `/proc/<pid>/cmdline` file, or "" if the file cannot be read. */
fun processNameFromCmdline(cmdline: File): String =
    try {
        cmdline.readText().substringBefore('\u0000').trim()
    } catch (_: Exception) {
        ""
    }
