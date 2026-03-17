package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.track.TrackServiceItem

class LoginToTrackServiceTest {

    private lateinit var loginToTrackService: LoginToTrackService
    private lateinit var trackManager: TrackManager
    private lateinit var trackService: TrackService
    private lateinit var trackServiceItem: TrackServiceItem

    @Before
    fun setUp() {
        trackManager = mockk()
        trackService = mockk()
        loginToTrackService = LoginToTrackService(trackManager)

        trackServiceItem =
            TrackServiceItem(
                id = 1,
                nameRes = 1,
                logoRes = 1,
                logoColor = 1,
                statusList = persistentListOf(1, 2, 3),
                supportsReadingDates = false,
                canRemoveFromService = false,
                isAutoAddTracker = false,
                isMdList = false,
                status = { "status" },
                displayScore = { "score" },
                scoreList = persistentListOf("1", "2"),
                indexToScore = { 1f },
            )

        coEvery { trackManager.getService(1) } returns trackService
    }

    @Test
    fun `when login is successful then return true`() = runTest {
        coEvery { trackService.login("user", "pass") } returns true

        val result = loginToTrackService(trackServiceItem, "user", "pass")

        assertTrue(result)
    }

    @Test
    fun `when login fails then return false`() = runTest {
        coEvery { trackService.login("user", "pass") } returns false

        val result = loginToTrackService(trackServiceItem, "user", "pass")

        assertFalse(result)
    }

    @Test
    fun `when track service is null then return false`() = runTest {
        coEvery { trackManager.getService(1) } returns null

        val result = loginToTrackService(trackServiceItem, "user", "pass")

        assertFalse(result)
    }
}
