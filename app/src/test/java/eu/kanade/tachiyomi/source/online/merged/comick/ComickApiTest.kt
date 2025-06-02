package eu.kanade.tachiyomi.source.online.merged.comick

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton

object TestApiInjektModule : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingleton(Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        })
    }
}

class ComickApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var comickApi: ComickApi
    private val client = OkHttpClient.Builder().build()
    private val lang = "en"

    @Before
    fun setUp() {
        Injekt.importModule(TestApiInjektModule)
        mockWebServer = MockWebServer()
        mockWebServer.start()
        comickApi = ComickApi(client, lang)
        // Modify the baseUrl in comickApi instance to use mockWebServer's URL
        val field = comickApi.javaClass.getDeclaredField("baseUrl")
        field.isAccessible = true
        field.set(comickApi, mockWebServer.url("/").toString().removeSuffix("/"))
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getComic success`() = runBlocking {
        val slug = "test-comic"
        val mockResponse = MockResponse().setBody(
            """
            {
                "hid": "comic-hid",
                "title": "Test Comic",
                "slug": "$slug"
            }
            """.trimIndent()
        )
        mockWebServer.enqueue(mockResponse)

        val comic = comickApi.getComic(slug)

        assertEquals("Test Comic", comic.title)
        assertEquals(slug, comic.slug)
        val request = mockWebServer.takeRequest()
        assertEquals("/comic/$slug", request.path)
    }

    @Test
    fun `getChapterList success`() = runBlocking {
        val comicHid = "comic-hid"
        val page = 1
        val mockResponse = MockResponse().setBody(
            """
            {
                "chapters": [{"hid": "chapter-hid", "lang": "$lang", "title": "Chapter 1"}],
                "total": 1,
                "limit": ${ComickApi.CHAPTER_LIST_LIMIT}
            }
            """.trimIndent()
        )
        mockWebServer.enqueue(mockResponse)

        val chapterList = comickApi.getChapterList(comicHid, page)

        assertTrue(chapterList.chapters.isNotEmpty())
        assertEquals("Chapter 1", chapterList.chapters[0].title)
        val request = mockWebServer.takeRequest()
        assertEquals("/comic/$comicHid/chapters?lang=$lang&page=$page&limit=${ComickApi.CHAPTER_LIST_LIMIT}&sort=chap", request.path)
    }

    @Test
    fun `getPageList success`() = runBlocking {
        val chapterHid = "chapter-hid"
        val mockResponse = MockResponse().setBody(
            """
            {
                "chapter": {
                    "hid": "$chapterHid",
                    "images": [
                        {"url": "image1.jpg", "b2key": "key1"},
                        {"url": "image2.jpg", "b2key": "key2"}
                    ]
                }
            }
            """.trimIndent()
        )
        mockWebServer.enqueue(mockResponse)

        val pageList = comickApi.getPageList(chapterHid)

        assertEquals(2, pageList.size)
        assertEquals("image1.jpg", pageList[0].imageUrl)
        val request = mockWebServer.takeRequest()
        assertEquals("/chapter/$chapterHid", request.path)
    }


    @Test
    fun `getTopComics success`() = runBlocking {
        val mockResponse = MockResponse().setBody(
            """
            [
                {"hid": "top-comic-1", "slug": "top-slug-1", "title": "Top Comic 1"}
            ]
            """.trimIndent()
        )
        mockWebServer.enqueue(mockResponse)

        val topComics = comickApi.getTopComics("hot", "day", 1, "action", "shounen", true)
        assertTrue(topComics.isNotEmpty())
        assertEquals("Top Comic 1", topComics[0].title)

        val request = mockWebServer.takeRequest()
        assertEquals("/top?type=hot&comic_types=day&page=1&genres=action&demographics=shounen&completed=true", request.path)
    }

    @Test
    fun `search success`() = runBlocking {
        val query = "test search"
        val mockResponse = MockResponse().setBody(
            """
            [
                {"hid": "search-res-1", "slug": "search-slug-1", "title": "Search Result 1"}
            ]
            """.trimIndent()
        )
        mockWebServer.enqueue(mockResponse)
        val searchResults = comickApi.search(query, 1, "fantasy", "shoujo", false)
        assertTrue(searchResults.isNotEmpty())
        assertEquals("Search Result 1", searchResults[0].title)

        val request = mockWebServer.takeRequest()
        assertEquals("/v1.0/search?q=test%20search&page=1&limit=20&genres=fantasy&demographics=shoujo&completed=false", request.path)
    }
    
    @Test(expected = Exception::class)
    fun `getComic http error`() = runBlocking {
        val slug = "error-comic"
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        comickApi.getComic(slug) // Expects awaitSuccess to throw
    }

    @Test(expected = kotlinx.serialization.SerializationException::class)
    fun `getComic malformed json`() = runBlocking {
        val slug = "malformed-comic"
        mockWebServer.enqueue(MockResponse().setBody("{malformed_json}"))
        comickApi.getComic(slug)
    }

    @Test
    fun `getGenres success`() = runBlocking {
        val mockResponse = MockResponse().setBody(
            """
            [
                {"name": "Action", "slug": "action", "group": "genre", "id": 1},
                {"name": "Adventure", "slug": "adventure", "group": "genre", "id": 2}
            ]
            """.trimIndent()
        )
        mockWebServer.enqueue(mockResponse)

        val genres = comickApi.getGenres()
        assertEquals(2, genres.size)
        assertEquals("Action", genres[0].name)
        val request = mockWebServer.takeRequest()
        assertEquals("/genre", request.path)
    }

    @Test
    fun `getDemographics success`() = runBlocking {
        val mockResponse = MockResponse().setBody(
            """
            [
                {"name": "Shounen", "slug": "shounen", "group": "demographic", "id": 1},
                {"name": "Shoujo", "slug": "shoujo", "group": "demographic", "id": 2}
            ]
            """.trimIndent()
        )
        mockWebServer.enqueue(mockResponse)

        val demographics = comickApi.getDemographics()
        assertEquals(2, demographics.size)
        assertEquals("Shounen", demographics[0].name)
        val request = mockWebServer.takeRequest()
        assertEquals("/demographic", request.path)
    }
    
    @Test
    fun `login success`() = runBlocking {
        val token = "test-auth-token"
        val mockResponse = MockResponse().setBody(
            """
            {
                "id": 123,
                "user": {"id": 123, "username": "testuser", "slug": "testuser", "email": "test@example.com", "chapterLanguages": ["en"], "restricted": false, "createdAt": "", "updatedAt": ""},
                "roles": ["User"],
                "permissions": [],
                "token": "new-jwt-token",
                "hid": "user-hid",
                "slug": "user-slug",
                "title": "Test User",
                "isPublisher": false
            }
            """.trimIndent() // Simplified Identity and User DTO
        )
        mockWebServer.enqueue(mockResponse)

        val identity = comickApi.login(token)
        assertEquals(123, identity.id)
        assertEquals("new-jwt-token", identity.token)

        val request = mockWebServer.takeRequest()
        assertEquals("/auth/token", request.path)
        assertEquals("POST", request.method)
        assertEquals("""{"token":"$token"}""", request.body.readUtf8())
    }

    @Test(expected = Exception::class)
    fun `login auth error`() = runBlocking {
        val token = "invalid-token"
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        comickApi.login(token) // Expects awaitSuccess to throw for non-2xx codes
    }

    // TODO: Add more tests for other ComickApi methods:
    // getIdentity, getBookmarks, addBookmark, removeBookmark, getReadMarkers, markChapterAsRead
    // Also test for different parameter combinations and error conditions (404, etc.) for each.
}
