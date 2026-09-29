package eu.kanade.tachiyomi.crash

import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashReportTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private var now = 1_800_000_000_000L

    private val crashDir: File
        get() = File(tempFolder.root, CrashReport.DIR_NAME)

    private fun crashReport(
        dir: File = crashDir,
        logcat: (File) -> Unit = { out -> out.writeText("I/neko: line\n") },
    ) = CrashReport(dir, logcat, clock = { now })

    private fun reportFiles(): List<File> =
        crashDir.listFiles()!!.filter { it.name.startsWith(CrashReport.PREFIX) }

    @Test
    fun `saveCrash writes the head, then the logcat output`() {
        val report = crashReport().saveCrash("head\n")

        assertEquals("head\nLogcat:\nI/neko: line\n", report!!.readText())
    }

    @Test
    fun `saveCrash keeps only the newest report`() {
        val crashReport = crashReport()
        val first = crashReport.saveCrash("first\n")!!
        now += 60_000
        val second = crashReport.saveCrash("second\n")!!

        assertFalse(first.exists())
        assertEquals(listOf(second), reportFiles())
        assertEquals(second, crashReport.latestReport())
    }

    @Test
    fun `a clock set back still keeps only the new report`() {
        val crashReport = crashReport()
        crashReport.saveCrash("first\n")
        now -= 60_000
        val second = crashReport.saveCrash("second\n")!!

        assertEquals(listOf(second), reportFiles())
        assertEquals(second, crashReport.latestReport())
    }

    @Test
    fun `a logcat that fails leaves a line naming the error`() {
        val report =
            crashReport(logcat = { throw IOException("logcat exited with 1") }).saveCrash("head\n")

        assertEquals(
            "head\nLogcat:\nLogcat failed: java.io.IOException: logcat exited with 1\n",
            report!!.readText(),
        )
    }

    @Test
    fun `a logcat that times out keeps what it wrote before the failure line`() {
        val report =
            crashReport(
                    logcat = { out ->
                        out.writeText("I/neko: partial\n")
                        throw IOException("logcat did not finish in 3 seconds")
                    }
                )
                .saveCrash("head\n")

        assertEquals(
            "head\nLogcat:\nI/neko: partial\n" +
                "Logcat failed: java.io.IOException: logcat did not finish in 3 seconds\n",
            report!!.readText(),
        )
    }

    @Test
    fun `saveCrash removes the logcat temp file`() {
        crashReport().saveCrash("head\n")

        assertEquals(emptyList<String>(), crashDir.list()!!.filter { it.endsWith(".tmp") })
    }

    @Test
    fun `saveTraceOnly runs no logcat and marks nothing pending`() {
        var logcatRuns = 0
        val crashReport = crashReport(logcat = { logcatRuns++ })

        val report = crashReport.saveTraceOnly("head\n")

        assertEquals("head\n", report!!.readText())
        assertEquals(0, logcatRuns)
        assertNull(crashReport.pendingReport())
    }

    @Test
    fun `a saved crash is pending until marked seen`() {
        val crashReport = crashReport()
        val report = crashReport.saveCrash("head\n")

        assertEquals(report, crashReport.pendingReport())
        crashReport.markSeen()
        assertNull(crashReport.pendingReport())
        assertEquals(report, crashReport.latestReport())
    }

    @Test
    fun `a pending marker without a report shows nothing`() {
        crashDir.mkdirs()
        File(crashDir, CrashReport.PENDING).createNewFile()

        assertNull(crashReport().pendingReport())
    }

    @Test
    fun `report returns only a report by its exact name`() {
        val crashReport = crashReport()
        val saved = crashReport.saveCrash("head\n")!!

        assertEquals(saved, crashReport.report(saved.name))
        assertNull(crashReport.report("../${saved.name}"))
        assertNull(crashReport.report(CrashReport.PENDING))
        assertNull(crashReport.report(null))
    }

    @Test
    fun `a folder that cannot be created saves nothing`() {
        val blocked = tempFolder.newFile(CrashReport.DIR_NAME)
        val crashReport = crashReport(dir = blocked)

        assertNull(crashReport.saveCrash("head\n"))
        assertNull(crashReport.saveTraceOnly("head\n"))
        assertNull(crashReport.pendingReport())
        assertTrue(blocked.isFile)
    }

    @Test
    fun `a failed write does not mark an older report pending`() {
        val crashReport = crashReport()
        crashReport.saveCrash("old\n")
        crashReport.markSeen()
        now += 60_000
        File(crashDir, CrashReport.fileName(now)).mkdirs()

        assertNull(crashReport.saveCrash("new\n"))
        assertNull(crashReport.pendingReport())
    }
}
