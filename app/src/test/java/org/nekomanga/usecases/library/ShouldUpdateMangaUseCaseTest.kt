package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.model.SManga
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.domain.library.LibraryPreferences

class ShouldUpdateMangaUseCaseTest {

    private val trackManager = mockk<TrackManager>()
    private val useCase = ShouldUpdateMangaUseCase(trackManager)

    private fun mockManga(
        unread: Int = 0,
        read: Int = 0,
        status: Int = SManga.UNKNOWN,
    ): LibraryManga {
        val manga = mockk<LibraryManga>()
        every { manga.unread } returns unread
        every { manga.read } returns read
        every { manga.status } returns status
        every { manga.totalChapters } returns (read + unread)
        every { manga.hasStarted } returns (read > 0)
        return manga
    }

    private fun mockTrack(syncId: Int = 1, status: Int = 0): Track {
        val track = mockk<Track>()
        every { track.sync_id } returns syncId
        every { track.status } returns status
        return track
    }

    @Test
    fun `when restrictions empty, returns null`() {
        val manga = mockManga(unread = 5, read = 0)
        val result = useCase(manga, emptySet(), emptyList())
        assertNull(result)
    }

    @Test
    fun `when HAS_UNREAD active and manga has unread chapters, returns HAS_UNREAD`() {
        val manga = mockManga(unread = 1)
        val result = useCase(manga, setOf(LibraryPreferences.MANGA_HAS_UNREAD), emptyList())
        assertEquals(LibraryPreferences.MANGA_HAS_UNREAD, result)
    }

    @Test
    fun `when HAS_UNREAD active and manga is fully read, returns null`() {
        val manga = mockManga(unread = 0, read = 10)
        val result = useCase(manga, setOf(LibraryPreferences.MANGA_HAS_UNREAD), emptyList())
        assertNull(result)
    }

    @Test
    fun `when NOT_STARTED active and manga total chapters gt 0 and has not started, returns NOT_STARTED`() {
        val manga = mockManga(unread = 5, read = 0)
        val result = useCase(manga, setOf(LibraryPreferences.MANGA_NOT_STARTED), emptyList())
        assertEquals(LibraryPreferences.MANGA_NOT_STARTED, result)
    }

    @Test
    fun `when NOT_STARTED active and manga total chapters is 0, returns null`() {
        val manga = mockManga(unread = 0, read = 0)
        val result = useCase(manga, setOf(LibraryPreferences.MANGA_NOT_STARTED), emptyList())
        assertNull(result)
    }

    @Test
    fun `when NOT_STARTED active and manga has started, returns null`() {
        val manga = mockManga(unread = 5, read = 2)
        val result = useCase(manga, setOf(LibraryPreferences.MANGA_NOT_STARTED), emptyList())
        assertNull(result)
    }

    @Test
    fun `when NOT_COMPLETED active and manga status is completed, returns NOT_COMPLETED`() {
        val manga = mockManga(status = SManga.COMPLETED)
        val result = useCase(manga, setOf(LibraryPreferences.MANGA_NOT_COMPLETED), emptyList())
        assertEquals(LibraryPreferences.MANGA_NOT_COMPLETED, result)
    }

    @Test
    fun `when NOT_COMPLETED active and manga status is not completed, returns null`() {
        val manga = mockManga(status = SManga.ONGOING)
        val result = useCase(manga, setOf(LibraryPreferences.MANGA_NOT_COMPLETED), emptyList())
        assertNull(result)
    }

    @Test
    fun `when TRACKING_PLAN_TO_READ active and track matches status, returns TRACKING_PLAN_TO_READ`() {
        val manga = mockManga()
        val track = mockTrack(syncId = 1, status = 2)
        val service = mockk<TrackService>()
        every { trackManager.getService(1) } returns service
        every { service.getGlobalStatus(2) } returns "Plan to Read"
        every { trackManager.getGlobalStatusResId("Plan to Read") } returns
            R.string.follows_plan_to_read

        val result =
            useCase(manga, setOf(LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ), listOf(track))
        assertEquals(LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ, result)
    }

    @Test
    fun `when TRACKING_DROPPED active and track matches status, returns TRACKING_DROPPED`() {
        val manga = mockManga()
        val track = mockTrack(syncId = 1, status = 3)
        val service = mockk<TrackService>()
        every { trackManager.getService(1) } returns service
        every { service.getGlobalStatus(3) } returns "Dropped"
        every { trackManager.getGlobalStatusResId("Dropped") } returns R.string.follows_dropped

        val result = useCase(manga, setOf(LibraryPreferences.MANGA_TRACKING_DROPPED), listOf(track))
        assertEquals(LibraryPreferences.MANGA_TRACKING_DROPPED, result)
    }

    @Test
    fun `when TRACKING_ON_HOLD active and track matches status, returns TRACKING_ON_HOLD`() {
        val manga = mockManga()
        val track = mockTrack(syncId = 1, status = 4)
        val service = mockk<TrackService>()
        every { trackManager.getService(1) } returns service
        every { service.getGlobalStatus(4) } returns "On Hold"
        every { trackManager.getGlobalStatusResId("On Hold") } returns R.string.follows_on_hold

        val result = useCase(manga, setOf(LibraryPreferences.MANGA_TRACKING_ON_HOLD), listOf(track))
        assertEquals(LibraryPreferences.MANGA_TRACKING_ON_HOLD, result)
    }

    @Test
    fun `when TRACKING_COMPLETED active and track matches status, returns TRACKING_COMPLETED`() {
        val manga = mockManga()
        val track = mockTrack(syncId = 1, status = 5)
        val service = mockk<TrackService>()
        every { trackManager.getService(1) } returns service
        every { service.getGlobalStatus(5) } returns "Completed"
        every { trackManager.getGlobalStatusResId("Completed") } returns R.string.follows_completed

        val result =
            useCase(manga, setOf(LibraryPreferences.MANGA_TRACKING_COMPLETED), listOf(track))
        assertEquals(LibraryPreferences.MANGA_TRACKING_COMPLETED, result)
    }

    @Test
    fun `when tracking status active but track does not match, returns null`() {
        val manga = mockManga()
        val track = mockTrack(syncId = 1, status = 6)
        val service = mockk<TrackService>()
        every { trackManager.getService(1) } returns service
        every { service.getGlobalStatus(6) } returns "Reading"
        every { trackManager.getGlobalStatusResId("Reading") } returns R.string.follows_reading

        val result =
            useCase(manga, setOf(LibraryPreferences.MANGA_TRACKING_COMPLETED), listOf(track))
        assertNull(result)
    }
}
