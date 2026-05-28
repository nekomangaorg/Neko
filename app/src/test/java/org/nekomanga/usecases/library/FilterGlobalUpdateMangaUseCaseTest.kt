package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.model.SManga
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.domain.library.LibraryPreferences

class FilterGlobalUpdateMangaUseCaseTest {

    private val mockTrackManager = mockk<TrackManager>()
    private val useCase = FilterGlobalUpdateMangaUseCase(mockTrackManager)

    @Test
    fun `when categories are excluded, manga in those categories are removed`() {
        val manga1 =
            mockk<LibraryManga> {
                every { id } returns 1L
                every { category } returns 1
            }
        val manga2 =
            mockk<LibraryManga> {
                every { id } returns 2L
                every { category } returns 2
            }

        val result =
            useCase(
                libraryManga = listOf(manga1, manga2),
                includedCategories = emptyList(),
                excludedCategories = listOf(1),
                restrictions = emptySet(),
                tracksByMangaId = emptyMap(),
                planToReadStatus = "Plan to Read",
                droppedStatus = "Dropped",
                onHoldStatus = "On Hold",
                completedStatus = "Completed",
            )

        assertEquals(1, result.size)
        assertTrue(result.containsKey(2L))
    }

    @Test
    fun `when categories are included, only manga in those categories are kept`() {
        val manga1 =
            mockk<LibraryManga> {
                every { id } returns 1L
                every { category } returns 1
            }
        val manga2 =
            mockk<LibraryManga> {
                every { id } returns 2L
                every { category } returns 2
            }
        val manga3 =
            mockk<LibraryManga> {
                every { id } returns 3L
                every { category } returns 3
            }

        val result =
            useCase(
                libraryManga = listOf(manga1, manga2, manga3),
                includedCategories = listOf(2),
                excludedCategories = emptyList(),
                restrictions = emptySet(),
                tracksByMangaId = emptyMap(),
                planToReadStatus = "Plan to Read",
                droppedStatus = "Dropped",
                onHoldStatus = "On Hold",
                completedStatus = "Completed",
            )

        assertEquals(1, result.size)
        assertTrue(result.containsKey(2L))
    }

    @Test
    fun `when restrictions apply, they correctly filter manga`() {
        val manga1 =
            mockk<LibraryManga> {
                every { id } returns 1L
                every { category } returns 1
                every { unread } returns 5
                every { totalChapters } returns 10
                every { hasStarted } returns true
                every { status } returns SManga.ONGOING
            }

        val manga2 =
            mockk<LibraryManga> {
                every { id } returns 2L
                every { category } returns 1
                every { unread } returns 0
                every { totalChapters } returns 10
                every { hasStarted } returns false
                every { status } returns SManga.ONGOING
            }

        val manga3 =
            mockk<LibraryManga> {
                every { id } returns 3L
                every { category } returns 1
                every { unread } returns 0
                every { totalChapters } returns 10
                every { hasStarted } returns true
                every { status } returns SManga.COMPLETED
            }

        val result =
            useCase(
                libraryManga = listOf(manga1, manga2, manga3),
                includedCategories = emptyList(),
                excludedCategories = emptyList(),
                restrictions =
                    setOf(
                        LibraryPreferences.MANGA_HAS_UNREAD,
                        LibraryPreferences.MANGA_NOT_STARTED,
                        LibraryPreferences.MANGA_NOT_COMPLETED,
                    ),
                tracksByMangaId = emptyMap(),
                planToReadStatus = "Plan to Read",
                droppedStatus = "Dropped",
                onHoldStatus = "On Hold",
                completedStatus = "Completed",
            )

        assertEquals(3, result.size)
    }

    @Test
    fun `when tracking restrictions apply, they correctly filter out manga`() {
        val mangaId = 1L
        val manga1 =
            mockk<LibraryManga> {
                every { id } returns mangaId
                every { category } returns 1
                every { unread } returns 0
                every { totalChapters } returns 10
                every { hasStarted } returns true
                every { status } returns SManga.ONGOING
            }

        val track =
            mockk<Track> {
                every { sync_id } returns 1
                every { status } returns 1
            }

        val mockService =
            mockk<TrackService> { every { getGlobalStatus(1) } returns "Plan to Read" }

        every { mockTrackManager.getService(1) } returns mockService

        val result =
            useCase(
                libraryManga = listOf(manga1),
                includedCategories = emptyList(),
                excludedCategories = emptyList(),
                restrictions = setOf(LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ),
                tracksByMangaId = mapOf(mangaId to listOf(track)),
                planToReadStatus = "Plan to Read",
                droppedStatus = "Dropped",
                onHoldStatus = "On Hold",
                completedStatus = "Completed",
            )

        assertEquals(0, result.size)
    }
}
