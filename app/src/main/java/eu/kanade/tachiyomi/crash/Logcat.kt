package eu.kanade.tachiyomi.crash

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * A main-thread crash blocks input until the process ends, and Android reports an ANR after 5
 * seconds without an input response.
 */
internal const val LOGCAT_TIMEOUT_SECONDS = 3L

/**
 * Writes the logcat lines of process [pid] to [out]. `-f` writes the file directly, because a pipe
 * that nobody reads blocks logcat once the pipe buffer is full.
 */
internal fun runLogcat(pid: Int, out: File) {
    val process =
        ProcessBuilder("logcat", "-d", "--pid=$pid", "*:D", "-f", out.absolutePath)
            .redirectErrorStream(true)
            .start()
    awaitLogcat(process, LOGCAT_TIMEOUT_SECONDS)
}

/** Waits up to [timeoutSeconds] for [process]. Throws when it timed out or exited with an error. */
internal fun awaitLogcat(process: Process, timeoutSeconds: Long) {
    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        process.destroy()
        throw IOException("logcat did not finish in $timeoutSeconds seconds")
    }
    val exitCode = process.exitValue()
    if (exitCode != 0) {
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim().take(500)
        throw IOException("logcat exited with $exitCode: $output")
    }
}
