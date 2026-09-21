package eu.kanade.tachiyomi.ui.reader.loader

import android.app.Application
import android.content.res.Resources
import android.util.DisplayMetrics
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.domain.CheckTallPageUseCase
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DownloadPageLoaderTest {

    private val chapter = mockk<ReaderChapter>(relaxed = true)
    private val manga = mockk<Manga>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>(relaxed = true)
    private val downloadProvider = mockk<DownloadProvider>(relaxed = true)
    private val checkTallPage = mockk<CheckTallPageUseCase>()
    private val context = mockk<Application>(relaxed = true)
    private val resources = mockk<Resources>(relaxed = true)

    @Test
    fun `getPages precomputes splits on background thread for downloaded pages`() = runTest {
        val dbChapter =
            Chapter.create().apply {
                id = 1L
                url = "/chapter/1"
            }
        every { chapter.chapter } returns dbChapter

        val chapterDir = mockk<UniFile>(relaxed = true)
        every { chapterDir.isFile } returns false
        every { downloadProvider.findChapterDir(dbChapter, manga) } returns chapterDir

        val metrics = DisplayMetrics().apply { heightPixels = 1920 }
        every { resources.displayMetrics } returns metrics
        every { context.resources } returns resources

        val mockPage1 = Page(0, "url_0", "img_0")
        val mockPage2 = Page(1, "url_1", "img_1")
        every { downloadManager.buildPageList(manga, dbChapter) } returns
            listOf(mockPage1, mockPage2)

        val split1 = ReaderPageSplit(mockk(relaxed = true), 0, 1000)
        val split2 = ReaderPageSplit(mockk(relaxed = true), 1000, 1000)
        val expectedSplits = listOf(split1, split2)

        every { checkTallPage(match { it.index == 0 }, 1920) } returns expectedSplits
        every { checkTallPage(match { it.index == 1 }, 1920) } returns null

        val loader =
            DownloadPageLoader(
                chapter = chapter,
                manga = manga,
                downloadManager = downloadManager,
                downloadProvider = downloadProvider,
                checkTallPage = checkTallPage,
                context = context,
            )

        val pages = loader.getPages()

        assertEquals(2, pages.size)
        // First page should have precomputed splits
        assertNotNull(pages[0].precomputedSplits)
        assertEquals(expectedSplits, pages[0].precomputedSplits)

        // Second page should have emptyList (marked as not tall)
        assertNotNull(pages[1].precomputedSplits)
        assertEquals(emptyList<ReaderPageSplit>(), pages[1].precomputedSplits)
    }
}
