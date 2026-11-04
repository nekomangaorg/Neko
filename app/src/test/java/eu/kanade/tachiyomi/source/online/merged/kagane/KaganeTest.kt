package eu.kanade.tachiyomi.source.online.merged.kagane

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.nekomanga.constants.Constants
import org.nekomanga.core.network.GET
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class KaganeTest {

    /** Status code and body per request path. Any other path answers 404. */
    private val responses =
        mutableMapOf(
            DETAILS_PATH to (200 to DETAILS),
            SOURCES_PATH to (200 to SOURCES),
            INDEX_PATH to (200 to INDEX),
        )

    /** Response headers per request path. Content-Type is JSON unless listed here. */
    private val responseHeaders = mutableMapOf<String, Headers>()

    /** Every request the fake server answered, in order. */
    private val requests = CopyOnWriteArrayList<Request>()

    private val server = Interceptor { chain ->
        val request = chain.request()
        requests += request
        val path = request.url.encodedPath
        val (code, body) = responses[path] ?: (404 to "")
        val headers = responseHeaders[path] ?: Headers.headersOf()
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP $code")
            .headers(headers)
            .body(
                body.toResponseBody((headers["Content-Type"] ?: "application/json").toMediaType())
            )
            .build()
    }

    private lateinit var kagane: Kagane

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        val client = OkHttpClient.Builder().addInterceptor(server).build()
        Injekt.addSingleton(
            mockk<NetworkHelper>(relaxed = true) { every { cloudFlareClient } returns client }
        )
        Injekt.addSingleton(Json { ignoreUnknownKeys = true })
        Injekt.addSingleton(
            mockk<MangaDexPreferences>(relaxed = true) { every { dataSaver().get() } returns false }
        )
        kagane = Kagane()
    }

    /**
     * Kagane's token interceptor with the fake server behind it, where the network is in the app.
     */
    private fun imageClient() =
        OkHttpClient.Builder()
            .addInterceptor(kagane::refreshTokenInterceptor)
            .addInterceptor(server)
            .build()

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    private fun scanlators(vararg names: String) = names.joinToString(Constants.SCANLATOR_SEPARATOR)

    @Test
    fun `chapters come from the book index, newest first`() = runTest {
        val chapters = kagane.fetchChapters("S1").get().orEmpty().map { it.first }

        chapters.map { it.url } shouldBe
            listOf("/series/S1/reader/b3", "/series/S1/reader/b2", "/series/S1/reader/b1")
        chapters.map { it.name } shouldBe listOf("Ch.2.5 - Extra", "Ch.2", "Ch.1")
        chapters.map { it.scanlator } shouldBe
            listOf(
                scanlators("Kagane", "MangaPlus"),
                scanlators("Kagane", "MangaPlus", "Group A"),
                scanlators("Kagane", "MangaPlus", "Group A"),
            )
        chapters.map { it.language } shouldBe listOf("es-la", "es-la", "es-la")
        chapters.map { it.date_upload } shouldBe
            listOf(1_770_319_199_000L, 1_770_232_799_000L, 1_770_146_399_000L)
    }

    @Test
    fun `index columns are read by field name`() = runTest {
        responses[INDEX_PATH] = 200 to REORDERED_INDEX

        val chapters = kagane.fetchChapters("S1").get().orEmpty().map { it.first }

        chapters.map { it.url } shouldBe listOf("/series/S1/reader/b2", "/series/S1/reader/b1")
        chapters.map { it.name } shouldBe listOf("Ch.2", "Ch.1")
        chapters.map { it.date_upload } shouldBe listOf(1_770_232_799_000L, 1_770_146_399_000L)
    }

    @Test
    fun `chapters are newest first whatever order the index lists them in`() = runTest {
        responses[INDEX_PATH] = 200 to SHUFFLED_INDEX

        val chapters = kagane.fetchChapters("S1").get().orEmpty().map { it.first }

        chapters.map { it.url } shouldBe
            listOf("/series/S1/reader/b3", "/series/S1/reader/b2", "/series/S1/reader/b1")
    }

    @Test
    fun `a null or missing release date leaves date_upload at 0`() = runTest {
        responses[INDEX_PATH] = 200 to UNDATED_INDEX

        val chapters = kagane.fetchChapters("S1").get().orEmpty().map { it.first }

        chapters.map { it.url to it.date_upload } shouldBe
            listOf(
                "/series/S1/reader/b3" to 1_770_319_199_000L,
                "/series/S1/reader/b2" to 0L,
                "/series/S1/reader/b1" to 0L,
            )
    }

    @Test
    fun `an HTTP error from the book index is returned`() = runTest {
        responses[INDEX_PATH] = 500 to ""

        kagane.fetchChapters("S1").getError() shouldBe ResultError.HttpError(500, "HTTP 500")
    }

    @Test
    fun `an incomplete book index fails instead of listing part of the chapters`() = runTest {
        responses[INDEX_PATH] = 200 to INCOMPLETE_INDEX

        kagane.fetchChapters("S1").getError() shouldBe
            ResultError.Generic("Incomplete Kagane chapter index: expected 3, got 2")
    }

    @Test
    fun `a book index without a book_id field fails`() = runTest {
        responses[INDEX_PATH] = 200 to NO_ID_INDEX

        kagane.fetchChapters("S1").getError() shouldBe
            ResultError.Generic("Kagane book index missing 'book_id' field")
    }

    @Test
    fun `page image requests carry the site's Origin and Referer`() = runTest {
        kagane.getImage(Page(0, url = IMAGE_URL, imageUrl = IMAGE_URL)).close()

        val request = requests.single()
        request.header("Origin") shouldBe "https://kagane.moe"
        request.header("Referer") shouldBe "https://kagane.moe/"
    }

    @Test
    fun `a 403 HTML page from the image host fails with a blocked network message`() {
        responses[IMAGE_PATH] = 403 to "<!DOCTYPE html><title>Attention Required!</title>"
        responseHeaders[IMAGE_PATH] = Headers.headersOf("Content-Type", HTML)

        val error =
            assertThrows(IOException::class.java) {
                imageClient().newCall(GET(IMAGE_URL)).execute()
            }

        error.message shouldBe "cdn.test is blocking this network, try another connection"
        requests.map { it.url.encodedPath } shouldBe listOf(IMAGE_PATH)
    }

    @Test
    fun `a 403 without an HTML page fetches a new token and retries`() {
        responses[IMAGE_PATH] = 403 to ""
        responses[INTEGRITY_PATH] = 200 to INTEGRITY
        responses[CHALLENGE_PATH] = 200 to CHALLENGE

        imageClient().newCall(GET(IMAGE_URL)).execute().close()

        requests.last().url.queryParameter("token") shouldBe "fresh"
    }

    @Test
    fun `a Cloudflare challenge from the image host is left to the challenge solver`() {
        responses[IMAGE_PATH] = 403 to "<!DOCTYPE html><title>Just a moment...</title>"
        responseHeaders[IMAGE_PATH] =
            Headers.headersOf("Content-Type", HTML, "cf-mitigated", "challenge")
        responses[INTEGRITY_PATH] = 200 to INTEGRITY
        responses[CHALLENGE_PATH] = 200 to CHALLENGE

        val response = imageClient().newCall(GET(IMAGE_URL)).execute()

        response.use { it.header("cf-mitigated") shouldBe "challenge" }
    }

    private companion object {
        const val DETAILS_PATH = "/api/v2/series/S1"
        const val SOURCES_PATH = "/api/v2/sources/list"
        const val INDEX_PATH = "/api/v2/series/S1/books/index"
        const val INTEGRITY_PATH = "/api/integrity"
        const val CHALLENGE_PATH = "/api/v2/books/c1"
        const val IMAGE_PATH = "/api/v2/books/page/c1/p1.jxl"
        const val IMAGE_URL = "https://cdn.test$IMAGE_PATH?token=old"
        const val HTML = "text/html; charset=UTF-8"

        const val INTEGRITY = """{"token":"integrity","exp":4102444800}"""
        const val CHALLENGE =
            """{"access_token":"fresh","cache_url":"https://cdn.test","manifest":{"pages":[]}}"""

        const val DETAILS =
            """{"title":"Series","translated_language":"es-419","source_id":"src1"}"""
        const val SOURCES =
            """{"sources":[{"source_id":"src1","source_type":"Official","title":"MangaPlus"}]}"""

        // Field list and value types as kagane.si sent them on 2026-10-22.
        const val INDEX =
            """{"count":3,"fields":["book_id","sort_no","chapter_no","volume_no","group_ix","released_at","title","page_count","published_on","internal_release"],"groups":[{"avatar_image_id":null,"group_id":"g1","is_scanlator":true,"is_trusted":false,"is_uploader":false,"title":"Group A"}],"books":[["b1",1.0,"1",null,[0],1770146399,"Chapter 1",20,null,false],["b2",2.0,"2",null,[0],1770232799,"Chapter 2",20,null,false],["b3",3.0,"2.5",null,[],1770319199,"Extra",20,null,false]]}"""
        const val REORDERED_INDEX =
            """{"count":2,"fields":["title","released_at","group_ix","volume_no","chapter_no","sort_no","book_id"],"groups":[],"books":[["Chapter 1",1770146399,[],null,"1",1.0,"b1"],["Chapter 2",1770232799,[],null,"2",2.0,"b2"]]}"""
        const val SHUFFLED_INDEX =
            """{"count":3,"fields":["book_id","sort_no","title"],"groups":[],"books":[["b2",2.0,"Chapter 2"],["b3",3.0,"Chapter 3"],["b1",1.0,"Chapter 1"]]}"""
        // b1 has a null release date and b2's row ends before the released_at column.
        const val UNDATED_INDEX =
            """{"count":3,"fields":["book_id","sort_no","released_at"],"groups":[],"books":[["b1",1.0,null],["b2",2.0],["b3",3.0,1770319199]]}"""
        const val INCOMPLETE_INDEX =
            """{"count":3,"fields":["book_id","sort_no","chapter_no","volume_no","group_ix","released_at","title"],"groups":[],"books":[["b1",1.0,"1",null,[],1770146399,"Chapter 1"],["b2",2.0,"2",null,[],1770232799,"Chapter 2"]]}"""
        const val NO_ID_INDEX =
            """{"count":1,"fields":["sort_no","title"],"groups":[],"books":[[1.0,"Chapter 1"]]}"""
    }
}
