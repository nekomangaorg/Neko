package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.presentation.screens.reader.viewer.ChapterTransitionUiModel

class ResolveChapterTransitionUiModelUseCaseTest {

    private val downloadManager = mockk<DownloadManager>()
    private val useCase = ResolveChapterTransitionUiModelUseCase(downloadManager)

    private fun createChapter(id: Long, chapterNumber: Float = id.toFloat()): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = chapterNumber
                this.mangadex_chapter_id = "md_$id"
            }
        return ReaderChapter(dbChapter)
    }

    private fun createMangaItem(id: Long = 1L): MangaItem {
        return MangaItem(
            id = id,
            title = "Test Manga",
            url = "/manga/$id",
            favorite = true,
        )
    }

    @Test
    fun `resolve prev transition with missing chapters calculates gap and download statuses`() {
        val prevChapter = createChapter(1L, chapterNumber = 1f)
        val currChapter = createChapter(5L, chapterNumber = 5f)
        val transition = ChapterTransition.Prev(from = currChapter, to = prevChapter)
        val manga = createMangaItem()

        every { downloadManager.isChapterDownloaded(currChapter.chapter, any(), any()) } returns
            true
        every { downloadManager.isChapterDownloaded(prevChapter.chapter, any(), any()) } returns
            false

        val uiModel = useCase(transition, manga)

        assertTrue(uiModel is ChapterTransitionUiModel.Prev)
        val prevModel = uiModel as ChapterTransitionUiModel.Prev
        assertEquals("Chapter 5", prevModel.fromChapterName)
        assertTrue(prevModel.isFromDownloaded)
        assertNotNull(prevModel.toChapter)
        assertEquals("Chapter 1", prevModel.toChapter?.name)
        assertFalse(prevModel.toChapter?.isDownloaded ?: true)
        assertEquals(3, prevModel.missingChaptersCount)
    }

    @Test
    fun `resolve next transition when next chapter is null`() {
        val currChapter = createChapter(10L, chapterNumber = 10f)
        val transition = ChapterTransition.Next(from = currChapter, to = null)

        every { downloadManager.isChapterDownloaded(any(), any(), any()) } returns false

        val uiModel = useCase(transition, null)

        assertTrue(uiModel is ChapterTransitionUiModel.Next)
        val nextModel = uiModel as ChapterTransitionUiModel.Next
        assertEquals("Chapter 10", nextModel.fromChapterName)
        assertFalse(nextModel.isFromDownloaded)
        assertNull(nextModel.toChapter)
        assertEquals(0, nextModel.missingChaptersCount)
    }

    @Test
    fun `resolve next transition with adjacent chapter has zero missing chapters`() {
        val currChapter = createChapter(1L, chapterNumber = 1f)
        val nextChapter = createChapter(2L, chapterNumber = 2f)
        val transition = ChapterTransition.Next(from = currChapter, to = nextChapter)
        val manga = createMangaItem()

        every { downloadManager.isChapterDownloaded(currChapter.chapter, any(), any()) } returns
            true
        every { downloadManager.isChapterDownloaded(nextChapter.chapter, any(), any()) } returns
            true

        val uiModel = useCase(transition, manga)

        assertTrue(uiModel is ChapterTransitionUiModel.Next)
        val nextModel = uiModel as ChapterTransitionUiModel.Next
        assertEquals("Chapter 1", nextModel.fromChapterName)
        assertTrue(nextModel.isFromDownloaded)
        assertNotNull(nextModel.toChapter)
        assertEquals("Chapter 2", nextModel.toChapter?.name)
        assertTrue(nextModel.toChapter?.isDownloaded ?: false)
        assertEquals(0, nextModel.missingChaptersCount)
        assertEquals(ChapterTransitionUiModel.PreloadState.Ready, nextModel.toChapter?.preloadState)
    }

    @Test
    fun `resolve transition reflects chapter error preload state`() {
        val currChapter = createChapter(1L, chapterNumber = 1f)
        val nextChapter =
            createChapter(2L, chapterNumber = 2f).apply {
                state = ReaderChapter.State.Error(Exception("Network timeout"))
            }
        val transition = ChapterTransition.Next(from = currChapter, to = nextChapter)

        every { downloadManager.isChapterDownloaded(any(), any(), any()) } returns false

        val uiModel = useCase(transition, null)

        val nextModel = uiModel as ChapterTransitionUiModel.Next
        val target = nextModel.toChapter
        assertNotNull(target)
        assertTrue(target?.preloadState is ChapterTransitionUiModel.PreloadState.Error)
        assertEquals(
            "Network timeout",
            (target?.preloadState as ChapterTransitionUiModel.PreloadState.Error).message,
        )
    }
}
