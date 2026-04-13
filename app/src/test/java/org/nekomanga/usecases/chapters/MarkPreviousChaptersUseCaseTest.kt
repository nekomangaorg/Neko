package org.nekomanga.usecases.chapters

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions

class MarkPreviousChaptersUseCaseTest {

    private val useCase = MarkPreviousChaptersUseCase()

    private fun createChapter(id: Long, name: String): ChapterItem {
        return mockk<ChapterItem>(relaxed = true) {
            every { chapter.id } returns id
            every { chapter.name } returns name
        }
    }

    @Test
    fun `when chapter is not in list, returns null`() {
        val chapterItem = createChapter(1L, "Chapter 1")
        val chapterList = listOf(createChapter(2L, "Chapter 2"), createChapter(3L, "Chapter 3"))

        val result = useCase(chapterItem, chapterList, true)

        assertNull(result)
    }

    @Test
    fun `when read is true, returns chapters to mark and PreviousRead action with alt chapters`() {
        val chapter1 = createChapter(1L, "Chapter 1")
        val chapter2 = createChapter(2L, "Chapter 2")
        val chapter3 = createChapter(3L, "Chapter 3")
        val chapter4 = createChapter(4L, "Chapter 4")
        val chapterList = listOf(chapter1, chapter2, chapter3, chapter4)

        // Target chapter is chapter3 (index 2)
        val result = useCase(chapter3, chapterList, read = true)

        val (chaptersToMark, action) = result!!

        // Should return chapters before index 2 (chapter1, chapter2)
        assertEquals(listOf(chapter1, chapter2), chaptersToMark)

        // Action should be PreviousRead with chapters after index 2 (chapter4)
        assert(action is ChapterMarkActions.PreviousRead)
        action as ChapterMarkActions.PreviousRead
        assertEquals(listOf(chapter4), action.altChapters)
    }

    @Test
    fun `when read is false, returns chapters to mark and PreviousUnread action with alt chapters`() {
        val chapter1 = createChapter(1L, "Chapter 1")
        val chapter2 = createChapter(2L, "Chapter 2")
        val chapter3 = createChapter(3L, "Chapter 3")
        val chapter4 = createChapter(4L, "Chapter 4")
        val chapterList = listOf(chapter1, chapter2, chapter3, chapter4)

        // Target chapter is chapter2 (index 1)
        val result = useCase(chapter2, chapterList, read = false)

        val (chaptersToMark, action) = result!!

        // Should return chapters before index 1 (chapter1)
        assertEquals(listOf(chapter1), chaptersToMark)

        // Action should be PreviousUnread with chapters after index 1 (chapter3, chapter4)
        assert(action is ChapterMarkActions.PreviousUnread)
        action as ChapterMarkActions.PreviousUnread
        assertEquals(listOf(chapter3, chapter4), action.altChapters)
    }

    @Test
    fun `when target is first item, returns empty chapters to mark`() {
        val chapter1 = createChapter(1L, "Chapter 1")
        val chapter2 = createChapter(2L, "Chapter 2")
        val chapterList = listOf(chapter1, chapter2)

        val result = useCase(chapter1, chapterList, read = true)

        val (chaptersToMark, action) = result!!

        assertEquals(emptyList<ChapterItem>(), chaptersToMark)
        assert(action is ChapterMarkActions.PreviousRead)
        action as ChapterMarkActions.PreviousRead
        assertEquals(listOf(chapter2), action.altChapters)
    }

    @Test
    fun `when target is last item, returns empty alt chapters`() {
        val chapter1 = createChapter(1L, "Chapter 1")
        val chapter2 = createChapter(2L, "Chapter 2")
        val chapterList = listOf(chapter1, chapter2)

        val result = useCase(chapter2, chapterList, read = true)

        val (chaptersToMark, action) = result!!

        assertEquals(listOf(chapter1), chaptersToMark)
        assert(action is ChapterMarkActions.PreviousRead)
        action as ChapterMarkActions.PreviousRead
        assertEquals(emptyList<ChapterItem>(), action.altChapters)
    }
}
