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
        Injekt.addSingleton(
            mockk<NetworkServices> {
                every { atHomeService } returns
                    mockk<MangaDexAtHomeService> {
                        coEvery { getAtHomeServer(CHAPTER_ID, true) } returns
                            ApiResponse.Success(atHome)
                    }
            }
        )
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `a fresh token uses the page server while the tracker is resizing`() = runTest {
        val tracker = ResizingTracker(CHAPTER_ID, System.currentTimeMillis())

        ImageHandler(tracker).getImage(page(), isLogged = false).close()

        assertEquals(listOf(PAGE_BASE_URL + IMAGE_PATH), requestedUrls)
    }

    @Test
    fun `an expired token fetches a new at home server`() = runTest {
        val now = System.currentTimeMillis()
        val tracker = mutableMapOf(CHAPTER_ID to now - TimeUnit.MINUTES.toMillis(6))

        ImageHandler(tracker).getImage(page(), isLogged = false).close()

        assertEquals(listOf(REFRESHED_BASE_URL + IMAGE_PATH), requestedUrls)
        assertTrue(tracker.getValue(CHAPTER_ID) >= now)
    }

    private fun page() = Page(1, PAGE_BASE_URL, IMAGE_PATH, CHAPTER_ID)

    /**
     * Returns the entry on the first read and null after that, which is what a HashMap read sees
     * when another thread's put is moving the entries into a bigger table.
     */
    private class ResizingTracker(key: String, time: Long) :
        HashMap<String, Long>(mapOf(key to time)) {
        private var reads = 0

        override fun get(key: String): Long? = if (reads++ == 0) super.get(key) else null
    }

    private companion object {
        const val CHAPTER_ID = "chapter-1"
        const val PAGE_BASE_URL = "https://page.example.org"
        const val REFRESHED_BASE_URL = "https://refreshed.example.org"
        const val IMAGE_PATH = "/data/hash/1.png"
    }
}
