package org.nekomanga.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class CrashReportingTreeTest {

    private val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseCrashlytics::class)
        mockkStatic(Log::class)

        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.getStackTraceString(any()) } answers {
            val throwable = firstArg<Throwable>()
            throwable.message ?: "exception"
        }
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        unmockkStatic(FirebaseCrashlytics::class)
        unmockkStatic(Log::class)
    }

    @Test
    fun `log debug level does not write to logcat`() {
        val tree = CrashReportingTree()
        Timber.plant(tree)

        Timber.tag("TestTag").d("Debug Message")

        verify(exactly = 0) { Log.println(any(), any(), any()) }
        verify(exactly = 0) { mockCrashlytics.log(any()) }
    }

    @Test
    fun `log info level writes to logcat`() {
        val tree = CrashReportingTree()
        Timber.plant(tree)

        Timber.tag("TestTag").i("Info Message")

        verify(exactly = 1) { Log.println(Log.INFO, "TestTag", "Info Message") }
        verify(exactly = 0) { mockCrashlytics.log(any()) }
    }

    @Test
    fun `log warn level writes to logcat and logs warning to crashlytics`() {
        val tree = CrashReportingTree()
        Timber.plant(tree)

        Timber.tag("TestTag").w("Warn Message")

        verify(exactly = 1) { Log.println(Log.WARN, "TestTag", "Warn Message") }
        verify(exactly = 1) { mockCrashlytics.log("Warn Message") }
    }

    @Test
    fun `log error level writes to logcat and records exception`() {
        val tree = CrashReportingTree()
        Timber.plant(tree)
        val exception = Exception("Error Exception")

        Timber.tag("TestTag").e(exception, "Error Message")

        verify(exactly = 1) {
            Log.println(
                Log.ERROR,
                "TestTag",
                match { it.contains("Error Message") && it.contains("Error Exception") },
            )
        }
        verify(exactly = 1) { mockCrashlytics.recordException(exception) }
    }
}
