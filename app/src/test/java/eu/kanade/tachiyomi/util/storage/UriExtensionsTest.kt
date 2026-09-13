package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.nekomanga.BuildConfig

class UriExtensionsTest {

    private val context = mockk<Context>()

    @Before
    fun setUp() {
        mockkStatic(FileProvider::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(FileProvider::class)
    }

    @Test
    fun testContentUriIsReturnedAsIs() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "content"

        assertSame(uri, uri.getUriWithAuthority(context))
    }

    @Test
    fun testFileUriIsWrappedByFileProvider() {
        val path = "/cache/shared_image/page.jpg"
        val uri = mockk<Uri>()
        every { uri.scheme } returns "file"
        every { uri.path } returns path
        val provided = mockk<Uri>()
        every {
            FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.provider",
                File(path),
            )
        } returns provided

        assertSame(provided, uri.getUriWithAuthority(context))
    }

    @Test
    fun testNonContentNonFileUriIsReturnedAsIs() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "https"

        assertSame(uri, uri.getUriWithAuthority(context))
    }

    @Test
    fun testNullSchemeUriIsReturnedAsIs() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns null

        assertSame(uri, uri.getUriWithAuthority(context))
    }

    @Test
    fun testFileUriConversionFailureFallsBackToOriginalUri() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "file"
        every { uri.path } returns null

        assertSame(uri, uri.getUriWithAuthority(context))
    }
}
