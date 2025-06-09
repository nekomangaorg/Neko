package eu.kanade.tachiyomi.source.online.merged.comick

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SimpleChapter
import eu.kanade.tachiyomi.util.system.ResultError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton

// Minimal Injekt setup for tests
object TestInjektModule : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingleton(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
        )
    }
}

class ComickTest {

    private lateinit var source: Comick
    private lateinit var mockWebServer: MockWebServer
    private val client =
        OkHttpClient.Builder().build() // Client for Comick, not used by MockWebServer directly

    @Before
    fun setUp() {
        Injekt.registerModule(TestInjektModule)

        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Pass the mockWebServer URL to the Comick instance
        source = Comick(customBaseUrl = mockWebServer.url("/").toString().removeSuffix("/"))
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        Injekt.clear()
    }

    @Test
    fun `test searchManga success`() = runBlocking {
        val searchQuery = "test query"
        val searchResponseJson =
            """
        [
            {
                "hid": "search-hid-1",
                "slug": "search-manga-1",
                "title": "Search Manga 1",
                "md_covers": [{ "b2key": "cover1.jpg" }],
                "year": 2023,
                "rating": "9.5",
                "created_at": "2023-01-01T00:00:00Z",
                "updated_at": "2023-01-01T00:00:00Z",
                "content_rating": "safe",
                "last_chapter": "10",
                "lang": "en",
                "demographic": "shounen"
            }
        ]
        """
        mockWebServer.enqueue(MockResponse().setBody(searchResponseJson))

        val result = source.searchManga(searchQuery)

        assertTrue(result.isNotEmpty())
        val manga = result[0]
        assertEquals("Search Manga 1", manga.title)
        assertEquals("/comic/search-manga-1", manga.url)
        assertEquals(
            "https://meo.comick.pictures/cover1.jpg",
            manga.thumbnail_url,
        ) // Assuming gpurl logic
        assertFalse(manga.initialized)

        val request = mockWebServer.takeRequest()
        assertEquals(
            "/v1.0/search?q=$searchQuery&page=1&limit=20&genres=all&demographics=all",
            request.path,
        )
    }

    @Test
    fun `test searchManga empty result`() = runBlocking {
        val searchQuery = "empty query"
        val emptyResponseJson = "[]"
        mockWebServer.enqueue(MockResponse().setBody(emptyResponseJson))

        val result = source.searchManga(searchQuery)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test fetchChapters success`() = runBlocking {
        val mangaUrl = "/comic/manga-hid-123"
        val chapterListResponseJsonPage1 =
            """
        {
            "chapters": [
                { "hid": "chap-hid-1", "lang": "en", "title": "Chapter 1", "chap": "1", "created_at": "2023-01-15T10:00:00Z", "group_name": ["Group A"]},
                { "hid": "chap-hid-2", "lang": "en", "title": "Chapter 2", "chap": "2", "created_at": "2023-01-16T10:00:00Z", "group_name": ["Group B"]}
            ],
            "total": 2,
            "limit": 500
        }
        """
        // If testing pagination, enqueue multiple responses:
        // mockWebServer.enqueue(MockResponse().setBody(chapterListResponseJsonPage1))
        // mockWebServer.enqueue(MockResponse().setBody(chapterListResponseJsonPage2)) // if total >
        // limit
        mockWebServer.enqueue(MockResponse().setBody(chapterListResponseJsonPage1))

        val result = source.fetchChapters(mangaUrl)

        assertTrue(result is Ok)
        val chapters = result.get()!!
        assertEquals(2, chapters.size)
        assertEquals("Chapter 1", chapters[0].name)
        assertEquals("/chapter/chap-hid-1", chapters[0].url)
        assertEquals(1, chapters[0].chapter_number.toInt())
        assertNotNull(chapters[0].date_upload)
        assertEquals("Group A", chapters[0].scanlator)

        val request = mockWebServer.takeRequest()
        assertEquals(
            "/comic/manga-hid-123/chapters?lang=en&page=1&limit=500&sort=chap",
            request.path,
        )
    }

    @Test
    fun `test fetchChapters network error`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val result = source.fetchChapters("/comic/some-manga")
        assertTrue(result is Err)
        assertTrue(result.getError() is ResultError.Network)
    }

    @Test
    fun `testGetPageList success`() = runBlocking {
        val chapterUrl = "/chapter/chapter-hid-for-pages"
        val pageListJson =
            """
        {
            "chapter": {
                "hid": "chapter-hid-for-pages",
                "images": [
                    {"b2key": "page1_b2.jpg"},
                    {"b2key": "page2_b2.jpg"}
                ]
            }
        }
        """
        mockWebServer.enqueue(MockResponse().setBody(pageListJson))
        val dummyChapter = SChapter.create().apply { url = chapterUrl }
        val result = source.getPageList(dummyChapter)

        assertEquals(2, result.size)
        assertEquals("https://meo.comick.pictures/page1_b2.jpg", result[0].imageUrl)
        assertEquals("https://meo.comick.pictures/page2_b2.jpg", result[1].imageUrl)

        val request = mockWebServer.takeRequest()
        assertEquals("/chapter/chapter-hid-for-pages", request.path)
    }

    @Test
    fun `testGetPageList error`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        val dummyChapter = SChapter.create().apply { url = "/chapter/nonexistent-chapter" }
        var pages: List<Page>? = null
        var exception: Exception? = null
        try {
            pages = source.getPageList(dummyChapter)
        } catch (e: Exception) {
            exception = e
        }
        // ReducedHttpSource getPageList is suspend fun, but doesn't return Result
        // It's expected to throw on error, or the source should handle it and return emptyList
        // Based on current Comick.kt, it will throw due to awaitSuccess()
        assertNotNull(exception) // Expecting an exception from awaitSuccess
        assertTrue(
            pages == null || pages.isEmpty()
        ) // Depending on exact error handling in getPageList
    }

    @Test
    fun `testImageRequest`() {
        val testImageUrl = "https://meo.comick.pictures/some_image.jpg"
        val page = Page(index = 0, imageUrl = testImageUrl, url = "")
        val request = source.imageRequest(page)

        assertEquals(testImageUrl, request.url.toString())
        assertEquals(
            "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
            request.header("Accept"),
        )
        assertEquals(source.baseUrl + "/", request.header("Referer")) // baseUrl ends with /
        assertEquals("meo.comick.pictures", request.header("Host"))
    }

    @Test
    fun `testGetChapterUrl`() {
        val relativeChapterUrl = "/chapter/chappy-123"
        val simpleChapter = SimpleChapter().apply { url = relativeChapterUrl }
        val expectedUrl = source.baseUrl + relativeChapterUrl
        val actualUrl = source.getChapterUrl(simpleChapter)
        assertEquals(expectedUrl, actualUrl)
    }
}
