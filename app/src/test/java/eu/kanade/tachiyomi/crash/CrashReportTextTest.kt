package eu.kanade.tachiyomi.crash

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTextTest {

    private val divider = "*".repeat(80)

    @Test
    fun `head puts the details after the device info and the trace between dividers`() {
        val head =
            CrashReportText.head(
                debugInfo = "Device product name: sdk_gphone64_x86_64",
                details = mapOf("Process" to "org.nekomanga.neko", "PID" to "4242"),
                trace = "java.lang.IllegalStateException: boom\n\tat A.b(A.kt:1)\n",
            )

        assertEquals(
            "Device product name: sdk_gphone64_x86_64\n" +
                "Process: org.nekomanga.neko\n" +
                "PID: 4242\n" +
                "\n" +
                "$divider\n" +
                "Exception that caused crash\n" +
                "$divider\n" +
                "java.lang.IllegalStateException: boom\n" +
                "\tat A.b(A.kt:1)\n" +
                "$divider\n" +
                "\n",
            head,
        )
    }

    @Test
    fun `head adds no blank line when the device info already ends with one`() {
        assertEquals(
            CrashReportText.head("info", emptyMap(), "trace"),
            CrashReportText.head("info\n", emptyMap(), "trace"),
        )
    }

    @Test
    fun `head without details still ends the device info line before the trace block`() {
        val head = CrashReportText.head("info", emptyMap(), "trace")

        assertTrue(head.startsWith("info\n\n$divider\n"))
    }

    @Test
    fun `logcatFailed names the error`() {
        assertEquals(
            "Logcat failed: java.io.IOException: logcat exited with 1\n",
            CrashReportText.logcatFailed(IOException("logcat exited with 1")),
        )
    }

    @Test
    fun `formatTime writes the date, the time and the zone offset`() {
        val time = CrashReportText.formatTime(1_800_000_000_000L)

        assertTrue(time, Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} [+-]\d{4}""").matches(time))
    }
}
