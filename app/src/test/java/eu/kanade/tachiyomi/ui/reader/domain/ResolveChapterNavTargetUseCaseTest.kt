package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveChapterNavTargetUseCaseTest {

    private val useCase = ResolveChapterNavTargetUseCase()

    private fun createReaderChapter(id: Long, chapterNumber: Float = id.toFloat()): ReaderChapter {
        val chapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = chapterNumber
            }
        return ReaderChapter(chapter)
    }

    private val ch1 = createReaderChapter(1L)
    private val ch2 = createReaderChapter(2L)
    private val ch3 = createReaderChapter(3L)
    private val chapters = listOf(ch1, ch2, ch3)

    @Test
    fun `when scrolling forward to next chapter, target is Start (page 0)`() {
        val target = useCase(currentChapter = ch1, selectedChapter = ch2, chapterList = chapters)
        assertEquals(ChapterNavTarget.Start, target)
    }

    @Test
    fun `when scrolling backward to previous chapter, target is End (last page)`() {
        val target = useCase(currentChapter = ch2, selectedChapter = ch1, chapterList = chapters)
        assertEquals(ChapterNavTarget.End, target)
    }

    @Test
    fun `when opening chapter initially with null currentChapter, target is Resume (saved progress)`() {
        val target = useCase(currentChapter = null, selectedChapter = ch2, chapterList = chapters)
        assertEquals(ChapterNavTarget.Resume, target)
    }

    @Test
    fun `when re-selecting current chapter, target is Resume`() {
        val target = useCase(currentChapter = ch2, selectedChapter = ch2, chapterList = chapters)
        assertEquals(ChapterNavTarget.Resume, target)
    }

    @Test
    fun `when nextChapter matches selectedChapter, target is Start`() {
        val target =
            useCase(
                currentChapter = ch1,
                selectedChapter = ch2,
                chapterList = chapters,
                nextChapter = ch2,
                prevChapter = null,
            )
        assertEquals(ChapterNavTarget.Start, target)
    }

    @Test
    fun `when prevChapter matches selectedChapter, target is End`() {
        val target =
            useCase(
                currentChapter = ch2,
                selectedChapter = ch1,
                chapterList = chapters,
                nextChapter = ch3,
                prevChapter = ch1,
            )
        assertEquals(ChapterNavTarget.End, target)
    }

    @Test
    fun `when chapters not found in list, falls back to chapter_number comparison`() {
        val chA = createReaderChapter(100L, chapterNumber = 10f)
        val chB = createReaderChapter(101L, chapterNumber = 11f)
        val targetForward =
            useCase(currentChapter = chA, selectedChapter = chB, chapterList = emptyList())
        assertEquals(ChapterNavTarget.Start, targetForward)

        val targetBackward =
            useCase(currentChapter = chB, selectedChapter = chA, chapterList = emptyList())
        assertEquals(ChapterNavTarget.End, targetBackward)
    }
}
