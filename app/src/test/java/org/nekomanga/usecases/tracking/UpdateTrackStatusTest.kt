package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem

class UpdateTrackStatusTest {

    @Test
    fun `given status index when update track status then track is updated with correct status`() =
        runTest {
            val updateTrackingService = mockk<UpdateTrackingService>()
            val useCase = UpdateTrackStatus(updateTrackingService)

            val initialTrack = mockk<TrackItem>(relaxed = true)
            val service =
                mockk<TrackServiceItem>(relaxed = true) {
                    every { statusList } returns listOf(10, 20, 30)
                }

            val statusIndex = 1
            val expectedTrack = initialTrack.copy(status = 20)

            val trackAndService = TrackingConstants.TrackAndService(initialTrack, service)

            coEvery { updateTrackingService.await(expectedTrack, service) } returns
                TrackingUpdate.Success

            val result = useCase.await(statusIndex, trackAndService)

            assertEquals(TrackingUpdate.Success, result)
        }
}
