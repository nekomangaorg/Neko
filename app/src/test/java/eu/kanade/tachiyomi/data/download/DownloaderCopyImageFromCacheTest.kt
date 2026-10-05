package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.text.TextUtils
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.util.system.ImageUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.nekomanga.domain.reader.ReaderPreferences
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class DownloaderCopyImageFromCacheTest {

    @get:Rule val folder = TemporaryFolder()

    private lateinit var chapterCache: ChapterCache
    private lateinit var tmpDir: File

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        val preloadPageAmount =
            mockk<Preference<Int>> {
                every { get() } returns 4
                every { changes() } returns emptyFlow()
            }
        Injekt.addSingleton(
            mockk<ReaderPreferences> { every { preloadPageAmount() } returns preloadPageAmount }
        )
        val cacheRoot = folder.newFolder("cache")
        chapterCache = ChapterCache(mockk<Context> { every { cacheDir } returns cacheRoot })
        tmpDir = folder.newFolder("chapter_tmp")

        // UniFile's RawFile checks names with TextUtils, which the android.jar stub does not run.
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }

        // ImageDecoder.findType is native, so stand in for it with a PNG signature check.
        mockkObject(ImageUtil)
        every { ImageUtil.findImageType(any<InputStream>()) } answers
            {
                val head = ByteArray(PNG_SIGNATURE.size)
                val read = firstArg<InputStream>().read(head)
                if (read == head.size && head.contentEquals(PNG_SIGNATURE)) {
                    ImageUtil.ImageType.PNG
                } else {
                    null
                }
            }
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `copies a cached image under its detected extension`() {
        val png = PNG_SIGNATURE + byteArrayOf(1, 2, 3, 4)
        cache(png)

        val file = copy()

        assertEquals("001.png", file?.name)
        assertEquals(listOf("001.png"), tmpDir.list()!!.toList())
        assertArrayEquals(png, File(tmpDir, "001.png").readBytes())
    }

    @Test
    fun `drops a cached entry that is not an image so the page is downloaded again`() {
        cache("<html><body>Too many requests</body></html>".toByteArray())

        val file = copy()

        assertNull(file)
        assertEquals(emptyList<String>(), tmpDir.list()!!.toList())
        assertFalse(chapterCache.isImageInCache(IMAGE_URL))
    }

    @Test
    fun `returns null when the image is not cached`() {
        assertNull(copy())
        assertEquals(emptyList<String>(), tmpDir.list()!!.toList())
    }

    private fun copy(): UniFile? =
        Downloader.copyImageFromCache(chapterCache, IMAGE_URL, UniFile.fromFile(tmpDir)!!, "001")

    private fun cache(bytes: ByteArray) {
        val response =
            Response.Builder()
                .request(Request.Builder().url(IMAGE_URL).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(bytes.toResponseBody("image/png".toMediaType()))
                .build()
        chapterCache.putImageToCache(IMAGE_URL, response)
    }

    companion object {
        private const val IMAGE_URL = "https://example.org/data/page1.png"
        private val PNG_SIGNATURE =
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
