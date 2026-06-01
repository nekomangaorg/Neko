package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem

class UpdateTrackChapterTest {

    @Test
    fun `given new chapter number when update tracking chapter then track is updated with correct chapter`() =
        runTest {
            val updateTrackingService = mockk<UpdateTrackingService>()
            val useCase = UpdateTrackChapter(updateTrackingService)

            val initialTrack = mockk<TrackItem>(relaxed = true)
            val service = mockk<TrackServiceItem>()

            val newChapterNumber = 5
            val expectedTrack = initialTrack.copy(lastChapterRead = newChapterNumber.toFloat())

            val trackAndService = TrackingConstants.TrackAndService(initialTrack, service)

            coEvery { updateTrackingService.await(expectedTrack, service) } returns
                TrackingUpdate.Success

            val result = useCase.await(newChapterNumber, trackAndService)

            assertEquals(TrackingUpdate.Success, result)
        }
}
