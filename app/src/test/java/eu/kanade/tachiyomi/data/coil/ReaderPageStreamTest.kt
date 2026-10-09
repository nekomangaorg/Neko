package eu.kanade.tachiyomi.data.coil

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageStreamTest {

    /** Loader that, like HttpPageLoader, queues a page again on retry and fetches it later. */
    private class RefetchingLoader : PageLoader() {
        val retried = mutableListOf<ReaderPage>()

        override suspend fun getPages(): List<ReaderPage> = emptyList()

        override suspend fun loadPage(page: ReaderPage) {}

        override fun retryPage(page: ReaderPage) {
            retried += page
            page.stream = null
            page.status = Page.State.QUEUE
        }
    }

    /** Loader for local files, which has nothing to fetch again. */
    private class LocalLoader : PageLoader() {
        override suspend fun getPages(): List<ReaderPage> = emptyList()

        override suspend fun loadPage(page: ReaderPage) {}
    }

    private fun readyPage(loader: PageLoader, stream: () -> InputStream): ReaderPage {
        val chapter = ReaderChapter(Chapter.create()).apply { pageLoader = loader }
        return ReaderPage(index = 0, imageUrl = "img_0", stream = stream).apply {
            this.chapter = chapter
            status = Page.State.READY
        }
    }

    private val missingFile: () -> InputStream = {
        throw FileNotFoundException("open failed: ENOENT (No such file or directory)")
    }

    @Test
    fun `a ready page whose cached file is gone is fetched again`() = runTest {
        val loader = RefetchingLoader()
        val page = readyPage(loader, missingFile)

        val opened = async { openReaderPageStream(page).use { it.readBytes() } }
        runCurrent()
        assertEquals(listOf(page), loader.retried)

        page.stream = { byteArrayOf(1, 2, 3).inputStream() }
        page.status = Page.State.READY

        assertArrayEquals(byteArrayOf(1, 2, 3), opened.await())
    }

    @Test
    fun `a page fetched again that fails keeps the error`() = runTest {
        val loader = RefetchingLoader()
        val page = readyPage(loader, missingFile)

        val opened = async { runCatching { openReaderPageStream(page) } }
        runCurrent()
        page.status = Page.State.ERROR

        assertTrue(opened.await().exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `a missing local file is not fetched again`() = runTest {
        val page = readyPage(LocalLoader(), missingFile)

        val error = runCatching { openReaderPageStream(page) }.exceptionOrNull()

        assertTrue(error is FileNotFoundException)
    }

    @Test
    fun `a read error other than a missing file is not fetched again`() = runTest {
        val loader = RefetchingLoader()
        val page = readyPage(loader) { throw IOException("read failed") }

        val error = runCatching { openReaderPageStream(page) }.exceptionOrNull()

        assertEquals("read failed", error?.message)
        assertTrue(loader.retried.isEmpty())
    }
}
