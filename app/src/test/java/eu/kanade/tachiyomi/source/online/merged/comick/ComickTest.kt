package eu.kanade.tachiyomi.source.online.merged.comick

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import kotlinx.serialization.json.Json

// Minimal Injekt setup for tests if Comick.kt relies on it for Json
object TestInjektModule : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingleton(Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        })
    }
}

class ComickTest {

    private lateinit var source: Comick
    private lateinit var mockWebServer: okhttp3.mockwebserver.MockWebServer

    @Before
    fun setUp() {
        Injekt.importModule(TestInjektModule) // Ensure Json is available via Injekt if used by the source

        mockWebServer = okhttp3.mockwebserver.MockWebServer()
        mockWebServer.start()

        val client = OkHttpClient.Builder().build()
        // We are testing Comick.kt, which internally creates ComickApi.
        // To properly test Comick.kt in isolation, ComickApi should be injectable.
        // For now, these tests will be more like integration tests that also cover ComickApi.
        // The base URL of ComickApi will be redirected to the mockWebServer.
        source = Comick()

        // This is a workaround: Modify the internal ComickApi to use the mockWebServer URL.
        // This is not ideal as it relies on internal knowledge of Comick.kt.
        // A better approach would be to inject ComickApi or its base URL into Comick.
        val field = source.javaClass.getDeclaredField("comickApi")
        field.isAccessible = true
        val originalApi = field.get(source) as ComickApi
        val apiField = originalApi.javaClass.getDeclaredField("baseUrl")
        apiField.isAccessible = true
        apiField.set(originalApi, mockWebServer.url("/").toString())
    }

    @Test
    fun `test fetchPopularManga`() = runBlocking {
        val popularMangaJson = """
        [
            {
                "hid": "popular-hid-1",
                "slug": "popular-manga-1",
                "title": "Popular Manga 1",
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
        mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse().setBody(popularMangaJson))

        val result = source.fetchPopularManga(1)

        assertTrue(result.mangas.isNotEmpty())
        assertEquals("Popular Manga 1", result.mangas[0].title)
        assertEquals("/comic/popular-manga-1", result.mangas[0].url) // Assuming getUrlWithoutDomain works as expected
        assertFalse(result.hasNextPage) // Only one item, so no next page
    }

    @Test
    fun `test fetchLatestUpdates`() = runBlocking {
        val latestUpdatesJson = """
        [
            {
                "hid": "latest-hid-1",
                "slug": "latest-manga-1",
                "title": "Latest Manga 1",
                "md_covers": [{ "b2key": "latest_cover1.jpg" }]
            }
        ]
        """
        mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse().setBody(latestUpdatesJson))

        val result = source.fetchLatestUpdates(1)
        assertTrue(result.mangas.isNotEmpty())
        assertEquals("Latest Manga 1", result.mangas[0].title)
    }

    @Test
    fun `test fetchSearchManga`() = runBlocking {
        val searchMangaJson = """
        [
            {
                "hid": "search-hid-1",
                "slug": "search-manga-1",
                "title": "Search Result Manga 1",
                "md_covers": [{ "b2key": "search_cover1.jpg" }]
            }
        ]
        """
        mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse().setBody(searchMangaJson))
        val result = source.fetchSearchManga(1, "test query", FilterList())
        assertTrue(result.mangas.isNotEmpty())
        assertEquals("Search Result Manga 1", result.mangas[0].title)
    }

    @Test
    fun `test fetchMangaDetails`() = runBlocking {
        val mangaDetailsJson = """
        {
            "hid": "detail-hid-1",
            "title": "Detailed Manga",
            "slug": "detailed-manga",
            "cover_url": "detail_cover.jpg",
            "genres": ["action", "adventure"],
            "demographic": "seinen",
            "created_at": "2022-01-01T00:00:00Z",
            "uploaded_at": "2022-01-01T00:00:00Z",
            "last_chapter": "25",
            "mainCover": "main_detail_cover.jpg",
            "desc": "This is a detailed manga description.",
            "status": 1,
            "authors": [{"name": "Author A", "slug": "author-a"}],
            "artists": [{"name": "Artist B", "slug": "artist-b"}],
            "mdcovers": [{"b2key": "md_detail_cover.jpg"}]
        }
        """
        mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse().setBody(mangaDetailsJson))

        val manga = SManga.create().apply { url = "/comic/detailed-manga" } // URL used to derive slug/hid
        val result = source.fetchMangaDetails(manga)

        assertEquals("Detailed Manga", result.title)
        assertEquals("This is a detailed manga description.", result.description)
        assertEquals("Author A", result.author)
        assertEquals("Artist B", result.artist)
        assertTrue(result.initialized)
    }

    @Test
    fun `test fetchChapterList`() = runBlocking {
        val chapterListJson = """
        {
            "chapters": [
                {
                    "hid": "chapter-hid-1",
                    "lang": "en",
                    "title": "Chapter 1: The Beginning",
                    "chapter": "1",
                    "created_at": "2023-01-15T10:00:00Z",
                    "group_name": ["Scanlation Group A"]
                }
            ],
            "total": 1,
            "limit": 500
        }
        """
        mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse().setBody(chapterListJson))
        val manga = SManga.create().apply { url = "/comic/some-comic-hid" } // URL used to derive hid
        val result = source.fetchChapterList(manga)

        assertTrue(result.isNotEmpty())
        assertEquals("Chapter 1: The Beginning", result[0].name)
        assertEquals(1, result[0].chapter_number.toInt())
    }

    // fetchPageList requires ComickApi to be directly testable or Comick to allow PageList parsing.
    // The current Comick.fetchPageList directly calls comickApi.getPageList(chapterHid)
    // and returns its result. So a test for Comick.fetchPageList would be identical
    // to a test for ComickApi.getPageList if we mock the HTTP response.
    // This will be better tested in ComickApiTest.kt
     @Test
    fun `test fetchPageList`() = runBlocking {
        val pageListJson = """
        {
            "chapter": {
                "hid": "chapter-hid-for-pages",
                "images": [
                    {"url": "page1.jpg", "b2key": "page1_b2.jpg"},
                    {"url": "page2.jpg", "b2key": "page2_b2.jpg"}
                ]
            }
        }
        """
        mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse().setBody(pageListJson))
        val chapter = eu.kanade.tachiyomi.source.model.SChapter.create().apply { url = "/chapter/chapter-hid-for-pages"}
        val result = source.fetchPageList(chapter)
        assertEquals(2, result.size)
        assertEquals("page1.jpg", result[0].imageUrl)
    }


    // TODO: Add tests for error responses and edge cases
    // e.g., empty JSON array, malformed JSON, HTTP errors (if not handled by OkHttp client directly)
}
