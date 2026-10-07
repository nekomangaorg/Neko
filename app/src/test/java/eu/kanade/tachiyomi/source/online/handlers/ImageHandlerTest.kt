package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAtHomeService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.models.dto.AtHomeChapterDto
import eu.kanade.tachiyomi.source.online.models.dto.AtHomeDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.site.MangaDexPreferences
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class ImageHandlerTest {

    private val requestedUrls = Collections.synchronizedList(mutableListOf<String>())
    private val atHomeService = mockk<MangaDexAtHomeService>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        val client =
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    requestedUrls += chain.request().url.toString()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(byteArrayOf(1).toResponseBody("image/png".toMediaType()))
                        .build()
                }
                .build()
        Injekt.addSingleton(
            mockk<NetworkHelper> {
                every { cdnClient } returns client
                every { headers } returns Headers.headersOf()
            }
        )
        val port443 = mockk<Preference<Boolean>> { every { get() } returns true }
        Injekt.addSingleton(
            mockk<MangaDexPreferences> { every { usePort443ForImageServer() } returns port443 }
        )
        val atHome =
            AtHomeDto(REFRESHED_BASE_URL, AtHomeChapterDto("hash", emptyList(), emptyList()))
        coEvery { atHomeService.getAtHomeServer(CHAPTER_ID, true) } returns
            ApiResponse.Success(atHome)
        Injekt.addSingleton(
            mockk<NetworkServices> {
                every { atHomeService } returns this@ImageHandlerTest.atHomeService
            }
        )
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `a fresh token uses the page server while the tracker is resizing`() = runTest {
        val tracker =
            ResizingTracker(
                CHAPTER_ID,
                ImageHandler.AtHomeServer(PAGE_BASE_URL, System.currentTimeMillis()),
            )

        ImageHandler(tracker).getImage(page(), isLogged = false).close()

        assertEquals(listOf(PAGE_BASE_URL + IMAGE_PATH), requestedUrls)
    }

    @Test
    fun `an expired token fetches a new at home server`() = runTest {
        val now = System.currentTimeMillis()
        val tracker = mutableMapOf(CHAPTER_ID to expiredServer(now))

        ImageHandler(tracker).getImage(page(), isLogged = false).close()

        assertEquals(listOf(REFRESHED_BASE_URL + IMAGE_PATH), requestedUrls)
        assertEquals(REFRESHED_BASE_URL, tracker.getValue(CHAPTER_ID).baseUrl)
        assertTrue(tracker.getValue(CHAPTER_ID).time >= now)
    }

    @Test
    fun `pages after a refresh use the refreshed server`() = runTest {
        val now = System.currentTimeMillis()
        val handler = ImageHandler(mutableMapOf(CHAPTER_ID to expiredServer(now)))

        handler.getImage(page(), isLogged = false).close()
        handler.getImage(page(2, SECOND_IMAGE_PATH), isLogged = false).close()

        assertEquals(
            listOf(REFRESHED_BASE_URL + IMAGE_PATH, REFRESHED_BASE_URL + SECOND_IMAGE_PATH),
            requestedUrls,
        )
    }

    @Test
    fun `pages after a failed refresh keep the current server`() = runTest {
        coEvery { atHomeService.getAtHomeServer(CHAPTER_ID, true) } returns
            ApiResponse.Failure.Exception(IOException("offline"))
        val handler = ImageHandler(mutableMapOf(CHAPTER_ID to expiredServer()))

        assertTrue(runCatching { handler.getImage(page(), isLogged = false) }.isFailure)
        handler.getImage(page(2, SECOND_IMAGE_PATH), isLogged = false).close()

        assertEquals(listOf(PAGE_BASE_URL + SECOND_IMAGE_PATH), requestedUrls)
    }

    private fun expiredServer(now: Long = System.currentTimeMillis()) =
        ImageHandler.AtHomeServer(PAGE_BASE_URL, now - TimeUnit.MINUTES.toMillis(6))

    private fun page(index: Int = 1, imagePath: String = IMAGE_PATH) =
        Page(index, PAGE_BASE_URL, imagePath, CHAPTER_ID)

    /**
     * Returns the entry on the first read and null after that, which is what a HashMap read sees
     * when another thread's put is moving the entries into a bigger table.
     */
    private class ResizingTracker(key: String, server: ImageHandler.AtHomeServer) :
        HashMap<String, ImageHandler.AtHomeServer>(mapOf(key to server)) {
        private var reads = 0

        override fun get(key: String): ImageHandler.AtHomeServer? =
            if (reads++ == 0) super.get(key) else null
    }

    private companion object {
        const val CHAPTER_ID = "chapter-1"
        const val PAGE_BASE_URL = "https://page.example.org"
        const val REFRESHED_BASE_URL = "https://refreshed.example.org"
        const val IMAGE_PATH = "/data/hash/1.png"
        const val SECOND_IMAGE_PATH = "/data/hash/2.png"
    }
}
