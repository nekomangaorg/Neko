package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem

class UpdateTrackingServiceTest {

    private val trackItem =
        TrackItem(
            id = 1L,
            mangaId = 2L,
            trackServiceId = 3,
            mediaId = 4L,
            libraryId = 5L,
            title = "Title",
            lastChapterRead = 6f,
            totalChapters = 10,
            score = 7f,
            status = 1,
            trackingUrl = "url",
            startedReadingDate = 0L,
            finishedReadingDate = 0L,
        )

    private val serviceItem =
        TrackServiceItem(
            id = 3,
            nameRes = 0,
            logoRes = 0,
            logoColor = 0,
            statusList = listOf(),
            supportsReadingDates = false,
            canRemoveFromService = false,
            isAutoAddTracker = false,
            isMdList = false,
            status = { "" },
            displayScore = { "" },
            scoreList = listOf(),
            indexToScore = { 0f },
        )

    @Test
    fun `given valid service and track item when update is successful then returns success`() =
        runTest {
            // Arrange
            val trackRepository = mockk<TrackRepository>()
            val trackManager = mockk<TrackManager>()
            val trackService = mockk<TrackService>()
            val useCase = UpdateTrackingService(trackRepository, trackManager)

            every { trackManager.getService(3) } returns trackService
            coEvery { trackService.update(any(), any()) } returns mockk<Track>()
            coEvery { trackRepository.insertTrack(any()) } returns 1L

            // Act
            val result = useCase.await(trackItem, serviceItem)

            // Assert
            assertEquals(TrackingUpdate.Success, result)
        }

    @Test
    fun `given missing service when update is called then returns error wrapping IllegalStateException`() =
        runTest {
            // Arrange
            val trackRepository = mockk<TrackRepository>()
            val trackManager = mockk<TrackManager>()
            val useCase = UpdateTrackingService(trackRepository, trackManager)

            every { trackManager.getService(3) } returns null

            // Act
            val result = useCase.await(trackItem, serviceItem)

            // Assert
            assertTrue(result is TrackingUpdate.Error)
            result as TrackingUpdate.Error
            assertTrue(result.exception is IllegalStateException)
            assertEquals("Service not found", result.exception.message)
            assertEquals("Error updating tracker", result.message)
        }

    @Test
    fun `given service throws error when update is called then returns error`() = runTest {
        // Arrange
        val trackRepository = mockk<TrackRepository>()
        val trackManager = mockk<TrackManager>()
        val trackService = mockk<TrackService>()
        val useCase = UpdateTrackingService(trackRepository, trackManager)
        val expectedException = RuntimeException("Network error")

        every { trackManager.getService(3) } returns trackService
        coEvery { trackService.update(any(), any()) } throws expectedException

        // Act
        val result = useCase.await(trackItem, serviceItem)

        // Assert
        assertTrue(result is TrackingUpdate.Error)
        result as TrackingUpdate.Error
        assertEquals(expectedException, result.exception)
        assertEquals("Error updating tracker", result.message)
    }
}
