package eu.kanade.tachiyomi.data.cache

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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

class ChapterCacheTest {

    @get:Rule val folder = TemporaryFolder()

    private lateinit var chapterCache: ChapterCache

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
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `putImageToCache throws and stores nothing when the body fails mid-read`() {
        val response = imageResponse(failingSource(firstChunk = byteArrayOf(-1, -40, -1)))

        assertThrows(IOException::class.java) { chapterCache.putImageToCache(IMAGE_URL, response) }
        assertFalse(chapterCache.isImageInCache(IMAGE_URL))
    }

    /** A body that returns [firstChunk] and then fails, like a connection dropped mid-image. */
    private fun failingSource(firstChunk: ByteArray): Source =
        object : Source {
            private var sent = false

            override fun read(sink: Buffer, byteCount: Long): Long {
                if (sent) throw IOException("unexpected end of stream")
                sent = true
                sink.write(firstChunk)
                return firstChunk.size.toLong()
            }

            override fun timeout(): Timeout = Timeout.NONE

            override fun close() {}
        }

    private fun imageResponse(body: Source): Response =
        Response.Builder()
            .request(Request.Builder().url(IMAGE_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.buffer().asResponseBody("image/jpeg".toMediaType()))
            .build()

    companion object {
        private const val IMAGE_URL = "https://example.org/data/page1.jpg"
    }
}
