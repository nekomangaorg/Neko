package eu.kanade.tachiyomi.crash

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatTest {

    private class FakeProcess(
        private val finishes: Boolean,
        private val exitCode: Int = 0,
        private val output: String = "",
    ) : Process() {
        var destroyed = false
            private set

        override fun getOutputStream(): OutputStream = ByteArrayOutputStream()

        override fun getInputStream(): InputStream = ByteArrayInputStream(output.toByteArray())

        override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))

        override fun waitFor(): Int = exitCode

        override fun waitFor(timeout: Long, unit: TimeUnit): Boolean = finishes

        override fun exitValue(): Int =
            if (finishes) exitCode else throw IllegalThreadStateException("still running")

        override fun destroy() {
            destroyed = true
        }
    }

    @Test
    fun `a logcat that exits with 0 in time passes`() {
        val process = FakeProcess(finishes = true)

        awaitLogcat(process, timeoutSeconds = 3)

        assertFalse(process.destroyed)
    }

    @Test
    fun `a logcat that does not finish in time is destroyed`() {
        val process = FakeProcess(finishes = false)

        val error =
            assertThrows(IOException::class.java) { awaitLogcat(process, timeoutSeconds = 3) }

        assertTrue(process.destroyed)
        assertEquals("logcat did not finish in 3 seconds", error.message)
    }

    @Test
    fun `a logcat that fails names its exit code and output`() {
        val process = FakeProcess(finishes = true, exitCode = 1, output = "unknown option --pid\n")

        val error =
            assertThrows(IOException::class.java) { awaitLogcat(process, timeoutSeconds = 3) }

        assertEquals("logcat exited with 1: unknown option --pid", error.message)
    }
}
