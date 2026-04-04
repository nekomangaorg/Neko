package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.site.MangaDexPreferences

class MarkChaptersRemoteTest {

    private lateinit var statusHandler: StatusHandler
    private lateinit var mangaDexPreferences: MangaDexPreferences
    private lateinit var markChaptersRemote: MarkChaptersRemote

    @Before
    fun setup() {
        statusHandler = mockk()
        mangaDexPreferences = mockk()
        markChaptersRemote = MarkChaptersRemote(statusHandler, mangaDexPreferences)
    }

    @Test
    fun `given skipSync true when marking mixed chapters then ignores readingSync and skips status calls`() =
        runTest {
            // Arrange
            val mangaUuid = "manga-uuid-123"
            val markAction = ChapterMarkActions.Read(canUndo = false)

            val nonMergedChapter =
                SimpleChapter.create()
                    .copy(id = 1L, mangaDexChapterId = "md-chapter-1", scanlator = "Some Scanlator")
            val mergedChapter =
                SimpleChapter.create()
                    .copy(
                        id = 2L,
                        mangaDexChapterId = "md-chapter-2",
                        scanlator = "Komga", // This makes isMergedChapter() true
                    )

            val chapterItems =
                listOf(
                    ChapterItem(chapter = nonMergedChapter),
                    ChapterItem(chapter = mergedChapter),
                )

            // Mock reading sync true just to be sure skipSync supersedes it
            every { mangaDexPreferences.readingSync().get() } returns true

            // Act
            markChaptersRemote(markAction, mangaUuid, chapterItems, skipSync = true)

            // Assert
            coVerify(exactly = 0) { statusHandler.markChaptersStatus(any(), any(), any()) }

            coVerify(exactly = 0) { statusHandler.markMergedChaptersStatus(any(), any()) }
        }

    @Test
    fun `given read action with sync true when marking mixed chapters then updates both merged and non-merged status`() =
        runTest {
            // Arrange
            val mangaUuid = "manga-uuid-123"
            val markAction = ChapterMarkActions.Read(canUndo = false)

            val nonMergedChapter =
                SimpleChapter.create()
                    .copy(id = 1L, mangaDexChapterId = "md-chapter-1", scanlator = "Some Scanlator")
            val mergedChapter =
                SimpleChapter.create()
                    .copy(
                        id = 2L,
                        mangaDexChapterId = "md-chapter-2",
                        scanlator = "Komga", // This makes isMergedChapter() true
                    )

            val chapterItems =
                listOf(
                    ChapterItem(chapter = nonMergedChapter),
                    ChapterItem(chapter = mergedChapter),
                )

            every { mangaDexPreferences.readingSync().get() } returns true

            coEvery { statusHandler.markChaptersStatus(any(), any(), any()) } returns Unit
            coEvery { statusHandler.markMergedChaptersStatus(any(), any()) } returns Unit

            // Act
            markChaptersRemote(markAction, mangaUuid, chapterItems, skipSync = false)

            // Assert
            coVerify(exactly = 1) {
                statusHandler.markChaptersStatus(
                    mangaId = mangaUuid,
                    chapterIds = listOf("md-chapter-1"),
                    read = true,
                )
            }

            coVerify(exactly = 1) {
                statusHandler.markMergedChaptersStatus(
                    chapters =
                        match { it.size == 1 && it.first().mangadex_chapter_id == "md-chapter-2" },
                    read = true,
                )
            }
        }
}
