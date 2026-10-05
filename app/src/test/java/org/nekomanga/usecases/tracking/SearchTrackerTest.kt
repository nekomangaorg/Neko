package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.constants.Constants
import org.nekomanga.domain.track.TrackServiceItem

class SearchTrackerTest {

    private val manga =
        Manga.create(pathUrl = "/manga/test-uuid", title = "Chainsaw Man", source = 1L)

    private val serviceItem =
        TrackServiceItem(
            id = 3,
            nameRes = 1001,
            logoRes = 0,
            logoColor = 0,
            statusList = emptyList(),
            supportsReadingDates = false,
            canRemoveFromService = false,
            isAutoAddTracker = false,
            isMdList = false,
            status = { "" },
            displayScore = { "" },
            scoreList = emptyList(),
            indexToScore = { 0f },
        )

    private val trackSearch =
        TrackSearch.create(serviceId = 3).apply {
            media_id = 42L
            title = "Chainsaw Man"
            summary = "A synopsis"
            publishing_status = "releasing"
            publishing_type = "manga"
            start_date = "2020-01-01"
            cover_url = "https://example.com/cover.jpg"
            tracking_url = "https://example.com/track/42"
        }

    @Test
    fun `given network failure when search tracker is called then emits loading and catches error with service name`() =
        runTest {
            // Arrange
            val trackManager = mockk<TrackManager>()
            val trackService = mockk<TrackService>()
            every { trackManager.getIdFromManga(serviceItem, manga) } returns ""
            every { trackManager.getService(serviceItem.id) } returns trackService
            coEvery { trackService.search(any(), any(), any()) } throws
                IOException("Connection timed out")

            val useCase = SearchTracker(trackManager)

            // Act
            val emissions =
                useCase
                    .await(
                        title = "Chainsaw Man",
                        service = serviceItem,
                        manga = manga,
                        previouslyTracker = false,
                    )
                    .toList()

            // Assert
            assertEquals(2, emissions.size)
            assertEquals(TrackSearchResult.Loading, emissions[0])
            assertTrue(emissions[1] is TrackSearchResult.Error)

            val error = emissions[1] as TrackSearchResult.Error
            assertEquals("Connection timed out", error.errorMessage)
            assertEquals(serviceItem.nameRes, error.trackerNameRes)
        }

    @Test
    fun `given search returning results when await is called then emits loading and success with mapped search items and matching id`() =
        runTest {
            // Arrange
            val trackManager = mockk<TrackManager>()
            val trackService = mockk<TrackService>()
            every { trackManager.getIdFromManga(serviceItem, manga) } returns "remote-id-123"
            every { trackManager.getService(serviceItem.id) } returns trackService
            coEvery { trackService.search("Chainsaw Man", manga, false) } returns
                listOf(trackSearch)

            val useCase = SearchTracker(trackManager)

            // Act
            val emissions =
                useCase
                    .await(
                        title = "Chainsaw Man",
                        service = serviceItem,
                        manga = manga,
                        previouslyTracker = false,
                    )
                    .toList()

            // Assert
            assertEquals(2, emissions.size)
            assertEquals(TrackSearchResult.Loading, emissions[0])
            assertTrue(emissions[1] is TrackSearchResult.Success)

            val success = emissions[1] as TrackSearchResult.Success
            assertEquals(1, success.trackSearchResult.size)
            assertEquals("Chainsaw Man", success.trackSearchResult[0].trackItem.title)
            assertEquals(42L, success.trackSearchResult[0].trackItem.mediaId)
            assertTrue(success.hasMatchingId)
        }

    @Test
    fun `given search returning empty results when await is called then emits loading and no result`() =
        runTest {
            // Arrange
            val trackManager = mockk<TrackManager>()
            val trackService = mockk<TrackService>()
            every { trackManager.getIdFromManga(serviceItem, manga) } returns null
            every { trackManager.getService(serviceItem.id) } returns trackService
            coEvery { trackService.search("Unknown Manga", manga, false) } returns emptyList()

            val useCase = SearchTracker(trackManager)

            // Act
            val emissions =
                useCase
                    .await(
                        title = "Unknown Manga",
                        service = serviceItem,
                        manga = manga,
                        previouslyTracker = false,
                    )
                    .toList()

            // Assert
            assertEquals(2, emissions.size)
            assertEquals(TrackSearchResult.Loading, emissions[0])
            assertEquals(TrackSearchResult.NoResult, emissions[1])
        }

    @Test
    fun `given missing service in track manager when await is called then emits loading and error wrapping service not found`() =
        runTest {
            // Arrange
            val trackManager = mockk<TrackManager>()
            every { trackManager.getIdFromManga(serviceItem, manga) } returns null
            every { trackManager.getService(serviceItem.id) } returns null

            val useCase = SearchTracker(trackManager)

            // Act
            val emissions =
                useCase
                    .await(
                        title = "Chainsaw Man",
                        service = serviceItem,
                        manga = manga,
                        previouslyTracker = false,
                    )
                    .toList()

            // Assert
            assertEquals(2, emissions.size)
            assertEquals(TrackSearchResult.Loading, emissions[0])
            assertTrue(emissions[1] is TrackSearchResult.Error)

            val error = emissions[1] as TrackSearchResult.Error
            assertEquals("Service not found", error.errorMessage)
            assertEquals(serviceItem.nameRes, error.trackerNameRes)
        }

    @Test
    fun `given tracker id when byId is called then queries search with tracker search id prefix`() =
        runTest {
            // Arrange
            val trackManager = mockk<TrackManager>()
            val trackService = mockk<TrackService>()
            every { trackManager.getService(serviceItem.id) } returns trackService
            coEvery {
                trackService.search(Constants.TRACKER_SEARCH_ID_PREFIX + "12345", manga, false)
            } returns listOf(trackSearch)

            val useCase = SearchTracker(trackManager)

            // Act
            val result =
                useCase.byId(
                    id = "12345",
                    service = serviceItem,
                    manga = manga,
                    previouslyTracker = false,
                )

            // Assert
            assertTrue(result is TrackSearchResult.Success)
            val success = result as TrackSearchResult.Success
            assertEquals(1, success.trackSearchResult.size)
            assertEquals("Chainsaw Man", success.trackSearchResult[0].trackItem.title)
            assertFalse(success.hasMatchingId)
        }

    @Test
    fun `given network error during non-flow search when awaitNonFlow is called then returns error with service name`() =
        runTest {
            // Arrange
            val trackManager = mockk<TrackManager>()
            val trackService = mockk<TrackService>()
            every { trackManager.getService(serviceItem.id) } returns trackService
            coEvery { trackService.search(any(), any(), any()) } throws IOException("Timeout")

            val useCase = SearchTracker(trackManager)

            // Act
            val result =
                useCase.awaitNonFlow(
                    title = "Chainsaw Man",
                    service = serviceItem,
                    manga = manga,
                    previouslyTracker = false,
                )

            // Assert
            assertTrue(result is TrackSearchResult.Error)
            val error = result as TrackSearchResult.Error
            assertEquals("Timeout", error.errorMessage)
            assertEquals(serviceItem.nameRes, error.trackerNameRes)
        }
}
