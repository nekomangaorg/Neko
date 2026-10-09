package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.network.httpErrorMessage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.reader.ReaderPreferences
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class HttpPageLoaderTest {

    private val chapterCache = mockk<ChapterCache>(relaxed = true)
    private val source = mockk<HttpSource>()
    private val chapter = mockk<ReaderChapter>(relaxed = true)
    private val page = ReaderPage(0, imageUrl = IMAGE_URL)
    private lateinit var cachedFile: File
    private lateinit var loader: HttpPageLoader

    @Before
    fun setUp() {
        val preloadPageAmount = mockk<Preference<Int>>()
        every { preloadPageAmount.get() } returns 0
        every { preloadPageAmount.changes() } returns emptyFlow()
        val readerPreferences = mockk<ReaderPreferences>()
        every { readerPreferences.preloadPageAmount() } returns preloadPageAmount
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(readerPreferences)

        cachedFile = File.createTempFile("page", ".0").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        every { chapterCache.isImageInCache(IMAGE_URL) } returns true
        every { chapterCache.getImageFile(IMAGE_URL) } returns cachedFile
        coEvery { source.getImage(page) } answers { okResponse() }
        every { chapter.pages } returns listOf(page)
        page.chapter = chapter

        loader = HttpPageLoader(chapter, source, chapterCache)
    }

    @After
    fun tearDown() {
        loader.recycle()
        cachedFile.delete()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `http error message names the status code and the host`() {
        httpErrorMessage(410, "atsu.moe") shouldBe "HTTP 410 from atsu.moe"
    }

    @Test
    fun `loading a cached page reads the cache without downloading`() = runBlocking {
        val load = launch(Dispatchers.IO) { loader.loadPage(page) }

        awaitReadyStream()
        load.cancelAndJoin()

        coVerify(exactly = 0) { source.getImage(any()) }
    }

    @Test
    fun `retry of a ready page downloads the image again even when it is cached`() = runBlocking {
        page.stream = { cachedFile.inputStream() }
        page.status = Page.State.READY

        loader.retryPage(page)
        awaitReadyStream()

        coVerify(exactly = 1) { source.getImage(page) }
        verify(exactly = 1) { chapterCache.putImageToCache(IMAGE_URL, any()) }
    }

    private suspend fun awaitReadyStream() {
        withTimeout(5_000) {
            page.statusFlow.first { it == Page.State.READY && page.stream != null }
        }
    }

    private fun okResponse(): Response =
        Response.Builder()
            .request(Request.Builder().url(IMAGE_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(byteArrayOf(4, 5, 6).toResponseBody())
            .build()

    private companion object {
        const val IMAGE_URL = "https://uploads.example.org/data/hash/1.jpg"
    }
}
