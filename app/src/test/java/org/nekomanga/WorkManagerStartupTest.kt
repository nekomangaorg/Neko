package org.nekomanga

import android.os.Looper
import androidx.core.os.HandlerCompat
import androidx.work.Configuration
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.w3c.dom.Element

/** WorkManager has to start after App.onCreate, since a worker it resumes reads Injekt. */
class WorkManagerStartupTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `the manifest removes the WorkManager startup initializer`() {
        val manifest =
            DocumentBuilderFactory.newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(File("src/main/AndroidManifest.xml"))
        val metaData = manifest.getElementsByTagName("meta-data")
        val initializer =
            (0 until metaData.length)
                .map { metaData.item(it) as Element }
                .single { it.getAttributeNS(ANDROID_NS, "name") == WORK_MANAGER_INITIALIZER }

        assertEquals(
            "androidx.startup.InitializationProvider",
            (initializer.parentNode as Element).getAttributeNS(ANDROID_NS, "name"),
        )
        assertEquals("remove", initializer.getAttributeNS(TOOLS_NS, "node"))
    }

    @Test
    fun `App gives WorkManager the same configuration on every read`() {
        // Configuration builds a Handler on the main looper, which a JVM test does not have
        mockkStatic(Looper::class, HandlerCompat::class)
        every { Looper.getMainLooper() } returns mockk()
        every { HandlerCompat.createAsync(any()) } returns mockk()
        val app: Configuration.Provider = App()

        assertSame(app.workManagerConfiguration, app.workManagerConfiguration)
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val TOOLS_NS = "http://schemas.android.com/tools"
        const val WORK_MANAGER_INITIALIZER = "androidx.work.WorkManagerInitializer"
    }
}
