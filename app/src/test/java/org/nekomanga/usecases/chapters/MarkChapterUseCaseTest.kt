package org.nekomanga.usecases.chapters

import com.pushtorefresh.storio.sqlite.operations.put.PreparedPutCollectionOfObjects
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.util.system.executeOnIO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
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
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter

@OptIn(ExperimentalCoroutinesApi::class)
class MarkChapterUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: DatabaseHelper
    private lateinit var markChapterUseCase: MarkChapterUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = mockk()
        markChapterUseCase = MarkChapterUseCase(db)

        mockkStatic("eu.kanade.tachiyomi.util.system.DatabaseExtensionsKt")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("eu.kanade.tachiyomi.util.system.DatabaseExtensionsKt")
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

            val mockPreparedPut = mockk<PreparedPutCollectionOfObjects<Chapter>>()
            val capturedChapters = slot<List<Chapter>>()

            every { db.updateChaptersProgress(capture(capturedChapters)) } returns mockPreparedPut
            coEvery { mockPreparedPut.executeOnIO() } returns mockk()

            // Act
            markChapterUseCase(markAction, listOf(chapterItem))

            // Assert
            coVerify(exactly = 1) { db.updateChaptersProgress(any()) }
            coVerify(exactly = 1) { mockPreparedPut.executeOnIO() }

            val updatedChapters = capturedChapters.captured
            assertEquals(1, updatedChapters.size)

            val updatedChapter = updatedChapters.first()
            assertEquals(1L, updatedChapter.id)
            assertFalse("Chapter should be marked as unread", updatedChapter.read)
            assertEquals(expectedLastRead, updatedChapter.last_page_read)
            assertEquals(expectedPagesLeft, updatedChapter.pages_left)
        }
}
