package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.data.database.models.Chapter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter

@OptIn(ExperimentalCoroutinesApi::class)
class MarkChapterUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockChapterRepository: ChapterRepository
    private lateinit var markChapterUseCase: MarkChapterUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockChapterRepository = mockk()
        markChapterUseCase = MarkChapterUseCase(mockChapterRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given unread action with specific parameters when marking chapters then chapters are updated correctly`() =
        runTest {
            // Arrange
            val expectedLastRead = 5
            val expectedPagesLeft = 10
            val markAction =
                ChapterMarkActions.Unread(
                    lastRead = expectedLastRead,
                    pagesLeft = expectedPagesLeft,
                )

            val simpleChapter =
                SimpleChapter.create()
                    .copy(id = 1L, mangaId = 100L, read = true, lastPageRead = 20, pagesLeft = 0)
            val chapterItem = ChapterItem(chapter = simpleChapter)

            val capturedChapters = slot<List<Chapter>>()

            coEvery {
                mockChapterRepository.updateChaptersProgress(capture(capturedChapters))
            } returns mockk()

            // Act
            markChapterUseCase(markAction, listOf(chapterItem))

            // Assert
            coVerify(exactly = 1) { mockChapterRepository.updateChaptersProgress(any()) }

            val updatedChapters = capturedChapters.captured
            assertEquals(1, updatedChapters.size)

            val updatedChapter = updatedChapters.first()
            assertEquals(1L, updatedChapter.id)
            assertFalse("Chapter should be marked as unread", updatedChapter.read)
            assertEquals(expectedLastRead, updatedChapter.last_page_read)
            assertEquals(expectedPagesLeft, updatedChapter.pages_left)
        }
}
