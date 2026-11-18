package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.mdlist.MdListStatusUpdateFailed
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem

class UpdateTrackStatusTest {

    private val updateTrackingService = mockk<UpdateTrackingService>()
    private val mdListService = mockk<MdList>()
    private val trackManager = mockk<TrackManager> { every { mdList } returns mdListService }
    private val useCase = UpdateTrackStatus(updateTrackingService, trackManager)

    private val mdListTrack =
        TrackItem(
            id = 1L,
            mangaId = 1L,
            trackServiceId = TrackManager.MDLIST,
            mediaId = 0L,
            libraryId = null,
            title = "Title",
            lastChapterRead = 5f,
            totalChapters = 0,
            score = 0f,
            status = FollowStatus.READING.int,
            trackingUrl = TRACKING_URL,
            startedReadingDate = 0L,
            finishedReadingDate = 0L,
        )
    private val mdListServiceItem =
        serviceItem(TrackManager.MDLIST, isMdList = true, FollowStatus.entries.map { it.int })
    private val mdListTrackAndService =
        TrackingConstants.TrackAndService(mdListTrack, mdListServiceItem)
    private val unfollowedIndex = mdListServiceItem.statusList.indexOf(FollowStatus.UNFOLLOWED.int)

    @Test
    fun `given status index when update track status then track is updated with correct status`() =
        runTest {
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

    @Test
    fun `given MDList and Unfollowed when update track status then MangaDex is asked to unfollow`() =
        runTest {
            coEvery { updateTrackingService.await(any(), any(), any()) } returns
                TrackingUpdate.Success
            coEvery { mdListService.unfollow(any()) } answers { firstArg() }

            val result = useCase.await(unfollowedIndex, mdListTrackAndService)

            assertEquals(TrackingUpdate.Success, result)
            coVerify {
                mdListService.unfollow(
                    match {
                        it.status == FollowStatus.UNFOLLOWED.int && it.tracking_url == TRACKING_URL
                    },
                )
            }
            coVerify(exactly = 0) { updateTrackingService.await(any(), any(), any()) }
        }

    @Test
    fun `given MangaDex refuses the unfollow when update track status then an error is returned`() =
        runTest {
            coEvery { updateTrackingService.await(any(), any(), any()) } returns
                TrackingUpdate.Success
            coEvery { mdListService.unfollow(any()) } throws MdListStatusUpdateFailed()

            val result = useCase.await(unfollowedIndex, mdListTrackAndService)

            assertTrue("Expected an error, got $result", result is TrackingUpdate.Error)
            val error = result as TrackingUpdate.Error
            assertEquals("Error updating tracker", error.message)
            assertTrue(error.exception is MdListStatusUpdateFailed)
        }

    @Test
    fun `given MDList and a followed status when update track status then the tracker update is used`() =
        runTest {
            coEvery { updateTrackingService.await(any(), any(), any()) } returns
                TrackingUpdate.Success
            val statusIndex = mdListServiceItem.statusList.indexOf(FollowStatus.ON_HOLD.int)

            val result = useCase.await(statusIndex, mdListTrackAndService)

            assertEquals(TrackingUpdate.Success, result)
            coVerify {
                updateTrackingService.await(
                    mdListTrack.copy(status = FollowStatus.ON_HOLD.int),
                    mdListServiceItem,
                    any(),
                )
            }
            coVerify(exactly = 0) { mdListService.unfollow(any()) }
        }

    @Test
    fun `given another tracker with status 0 when update track status then the tracker update is used`() =
        runTest {
            coEvery { updateTrackingService.await(any(), any(), any()) } returns
                TrackingUpdate.Success
            // MangaUpdates uses 0 for its Reading list, the number MDList uses for Unfollowed
            val mangaUpdatesItem =
                serviceItem(TrackManager.MANGA_UPDATES, isMdList = false, listOf(0, 1, 2, 3, 4))
            val track = mdListTrack.copy(trackServiceId = TrackManager.MANGA_UPDATES, status = 1)

            val result =
                useCase.await(0, TrackingConstants.TrackAndService(track, mangaUpdatesItem))

            assertEquals(TrackingUpdate.Success, result)
            coVerify {
                updateTrackingService.await(track.copy(status = 0), mangaUpdatesItem, any())
            }
            coVerify(exactly = 0) { mdListService.unfollow(any()) }
        }

    private fun serviceItem(id: Int, isMdList: Boolean, statusList: List<Int>) =
        TrackServiceItem(
            id = id,
            nameRes = 0,
            logoRes = 0,
            logoColor = 0,
            statusList = statusList,
            supportsReadingDates = false,
            canRemoveFromService = false,
            isAutoAddTracker = false,
            isMdList = isMdList,
            status = { "" },
            displayScore = { "" },
            scoreList = listOf(),
            indexToScore = { 0f },
        )

    private companion object {
        const val TRACKING_URL = "https://mangadex.org/title/00000000-0000-0000-0000-000000000001"
    }
}
