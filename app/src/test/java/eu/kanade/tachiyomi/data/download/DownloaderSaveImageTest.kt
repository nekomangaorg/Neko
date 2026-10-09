package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.webkit.MimeTypeMap
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.system.ImageUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import java.io.InputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.nekomanga.R
import org.nekomanga.constants.Constants.TMP_FILE_SUFFIX

class DownloaderSaveImageTest {

    @get:Rule val folder = TemporaryFolder()

    private lateinit var tmpDir: File

    private val context =
        mockk<Context> {
            every { getString(R.string.download_notifier_page_not_image) } returns NOT_IMAGE
            every { getString(R.string.download_notifier_cannot_create_file) } returns
                CANNOT_CREATE_FILE
        }

    @Before
    fun setup() {
        tmpDir = folder.newFolder("chapter_tmp")

        // UniFile's RawFile checks names with TextUtils and builds its uri with Uri.fromFile, and
        // the android.jar stubs run neither.
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
        mockkStatic(Uri::class)
        every { Uri.fromFile(any()) } returns mockk()

        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns
            mockk {
                every { getExtensionFromMimeType(any()) } answers
                    {
                        when (firstArg<String?>()) {
                            "image/png" -> "png"
                            "image/jpeg" -> "jpg"
                            "image/bmp" -> "bmp"
                            else -> null
                        }
                    }
            }

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
        // BitmapFactory is an android.jar stub too, so read the BMP signature instead.
        every { ImageUtil.findPlatformImageMime(any()) } answers
            {
                val head = ByteArray(BMP_SIGNATURE.size)
                val read = firstArg<() -> InputStream>()().use { it.read(head) }
                if (read == head.size && head.contentEquals(BMP_SIGNATURE)) "image/bmp" else null
            }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `saves an image under the extension of its bytes`() {
        val png = PNG_SIGNATURE + byteArrayOf(1, 2, 3, 4)

        val file = save(response(200, png, "image/png"))

        assertEquals("001.png", file.name)
        assertEquals(listOf("001.png"), tmpDir.list()!!.toList())
        assertArrayEquals(png, File(tmpDir, "001.png").readBytes())
    }

    @Test
    fun `reads the image type from the bytes when the content type is not an image`() {
        val png = PNG_SIGNATURE + byteArrayOf(1, 2, 3, 4)
        val file = save(response(200, png, "application/octet-stream"))

        assertEquals("001.png", file.name)
        assertEquals(listOf("001.png"), tmpDir.list()!!.toList())
    }

    @Test
    fun `fails an http error response and saves nothing`() {
        val error =
            assertThrows(Exception::class.java) {
                save(response(404, "<html>Not Found</html>".toByteArray(), "text/html"))
            }

        assertEquals("HTTP 404 from example.org", error.message)
        assertEquals(emptyList<String>(), tmpDir.list()!!.toList())
    }

    @Test
    fun `fails a successful response that is not an image and saves nothing`() {
        val error =
            assertThrows(Exception::class.java) {
                save(response(200, "<html>Too many requests</html>".toByteArray(), "text/html"))
            }

        assertEquals(NOT_IMAGE, error.message)
        assertEquals(emptyList<String>(), tmpDir.list()!!.toList())
    }

    @Test
    fun `fails an html page served with an image content type and saves nothing`() {
        val html = "<!DOCTYPE html>\n<html><head><title>Just a moment...</title></head></html>"

        val error =
            assertThrows(Exception::class.java) {
                save(response(200, html.toByteArray(), "image/jpeg"))
            }

        assertEquals(NOT_IMAGE, error.message)
        assertEquals(emptyList<String>(), tmpDir.list()!!.toList())
    }

    @Test
    fun `saves a bmp page that only the platform decoder reads`() {
        val file = save(response(200, BMP, "image/bmp"))

        assertEquals("001.bmp", file.name)
    }

    @Test
    fun `reads a bmp from the bytes when the content type is not an image`() {
        val file = save(response(200, BMP, "application/octet-stream"))

        assertEquals("001.bmp", file.name)
    }

    @Test
    fun `fails an html page with an inline svg and saves nothing`() {
        val html =
            "<!DOCTYPE html>\n<html><body>" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"></svg>" +
                "</body></html>"

        val error =
            assertThrows(Exception::class.java) {
                save(response(200, html.toByteArray(), "image/svg+xml"))
            }

        assertEquals(NOT_IMAGE, error.message)
        assertEquals(emptyList<String>(), tmpDir.list()!!.toList())
    }

    @Test
    fun `names the page by its bytes when the content type names another format`() {
        val png = PNG_SIGNATURE + byteArrayOf(1, 2, 3, 4)

        val file = save(response(200, png, "image/jpeg"))

        assertEquals("001.png", file.name)
        assertEquals(listOf("001.png"), tmpDir.list()!!.toList())
    }

    @Test
    fun `fails and saves nothing when the page file cannot be renamed`() {
        // A non-empty directory under the final name makes the rename fail.
        File(tmpDir, "001.png").mkdir()
        File(tmpDir, "001.png/x").writeBytes(byteArrayOf(0))

        val error =
            assertThrows(Exception::class.java) {
                save(response(200, PNG_SIGNATURE + byteArrayOf(1, 2), "image/png"))
            }

        assertEquals(CANNOT_CREATE_FILE, error.message)
        assertEquals(listOf("001.png"), tmpDir.list()!!.toList())
    }

    @Test
    fun `closes the response when the temp file cannot be created`() {
        // A directory in the way makes createFile return null.
        File(tmpDir, "001$TMP_FILE_SUFFIX").mkdir()
        var closed = false
        val source =
            object : ForwardingSource(Buffer().write(PNG_SIGNATURE)) {
                override fun close() {
                    closed = true
                    super.close()
                }
            }

        val error =
            assertThrows(Exception::class.java) {
                save(response(200, source.buffer().asResponseBody("image/png".toMediaType())))
            }

        assertEquals(CANNOT_CREATE_FILE, error.message)
        assertTrue(closed)
    }

    private fun save(response: Response): UniFile =
        Downloader.saveImage(context, response, UniFile.fromFile(tmpDir)!!, "001")

    private fun response(code: Int, bytes: ByteArray, contentType: String): Response =
        response(code, bytes.toResponseBody(contentType.toMediaType()))

    private fun response(code: Int, body: ResponseBody): Response =
        Response.Builder()
            .request(Request.Builder().url(IMAGE_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("message")
            .body(body)
            .build()

    companion object {
        private const val IMAGE_URL = "https://example.org/data/page1.png"
        private const val NOT_IMAGE = "Downloaded page isn't an image"
        private const val CANNOT_CREATE_FILE = "Couldn't create a file in the download folder"
        private val PNG_SIGNATURE =
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        private val BMP_SIGNATURE = "BM".toByteArray()
        private val BMP = BMP_SIGNATURE + byteArrayOf(1, 2, 3, 4)
    }
}
