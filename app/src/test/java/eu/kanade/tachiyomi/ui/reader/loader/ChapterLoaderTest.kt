package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ChapterLoaderTest {

    private val manga = mockk<Manga>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>()
    private val dbChapter =
        Chapter.create().apply {
            id = 1L
            url = "/chapter/1"
        }

    private val loader =
        ChapterLoader(
            context = mockk(relaxed = true),
            downloadManager = downloadManager,
            downloadProvider = mockk(relaxed = true),
            manga = manga,
            sourceManager = mockk(relaxed = true),
        )

    private fun chapterWith(pageLoader: PageLoader): ReaderChapter =
        ReaderChapter(dbChapter).apply { this.pageLoader = pageLoader }

    private fun stubDownloaded(cache: Boolean, disk: Boolean) {
        every { downloadManager.isChapterDownloaded(dbChapter, manga, false) } returns cache
        every { downloadManager.isChapterDownloaded(dbChapter, manga, true) } returns disk
    }

    @Test
    fun `chapter loaded over http and now downloaded on disk is reported`() {
        stubDownloaded(cache = true, disk = true)

        loader.isDownloadedSinceHttpLoad(chapterWith(mockk<HttpPageLoader>())) shouldBe true
    }

    @Test
    fun `stale cache entry for a chapter missing on disk is not reported`() {
        // The cache still lists a download whose file is gone, while getPageLoader checks the disk
        // and picks HttpPageLoader again. Reporting it would reload the chapter on every preload.
        stubDownloaded(cache = true, disk = false)

        loader.isDownloadedSinceHttpLoad(chapterWith(mockk<HttpPageLoader>())) shouldBe false
    }

    @Test
    fun `chapter not in the cache is not reported and the disk is not checked`() {
        stubDownloaded(cache = false, disk = true)

        loader.isDownloadedSinceHttpLoad(chapterWith(mockk<HttpPageLoader>())) shouldBe false
        verify(exactly = 0) { downloadManager.isChapterDownloaded(dbChapter, manga, true) }
    }

    @Test
    fun `chapter already loaded from disk is not reported`() {
        stubDownloaded(cache = true, disk = true)

        loader.isDownloadedSinceHttpLoad(chapterWith(mockk<DownloadPageLoader>())) shouldBe false
    }
}
