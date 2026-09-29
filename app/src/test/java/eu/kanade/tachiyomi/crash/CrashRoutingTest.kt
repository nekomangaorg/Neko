package eu.kanade.tachiyomi.crash

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashRoutingTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @Test
    fun `a crash with a visible activity shows the crash screen`() {
        assertEquals(
            CrashRoute.CrashScreen,
            crashRoute(isErrorHandlerProcess = false, isActivityVisible = true),
        )
    }

    @Test
    fun `a crash with no visible activity goes to the default handler`() {
        assertEquals(
            CrashRoute.DefaultHandler,
            crashRoute(isErrorHandlerProcess = false, isActivityVisible = false),
        )
    }

    @Test
    fun `a crash in the error handler process writes no report`() {
        assertEquals(
            CrashRoute.DefaultHandlerWithoutReport,
            crashRoute(isErrorHandlerProcess = true, isActivityVisible = true),
        )
        assertEquals(
            CrashRoute.DefaultHandlerWithoutReport,
            crashRoute(isErrorHandlerProcess = true, isActivityVisible = false),
        )
    }

    @Test
    fun `the error handler process name matches`() {
        assertTrue(isErrorHandlerProcess("org.nekomanga.neko.debug:error_handler"))
    }

    @Test
    fun `the main process name does not match`() {
        assertFalse(isErrorHandlerProcess("org.nekomanga.neko.debug"))
    }

    @Test
    fun `reads the process name up to the first NUL`() {
        val cmdline =
            tempFolder.newFile("cmdline").apply {
                writeText("org.nekomanga.neko:error_handler\u0000\u0000")
            }

        assertEquals("org.nekomanga.neko:error_handler", processNameFromCmdline(cmdline))
    }

    @Test
    fun `an unreadable cmdline gives an empty name`() {
        assertEquals("", processNameFromCmdline(File(tempFolder.root, "missing")))
    }
}
