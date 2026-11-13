package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.ResultDto
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class FollowsHandlerTest {

    private val authService = mockk<MangaDexAuthorizedUserService>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(
            mockk<NetworkServices> {
                every { authService } returns this@FollowsHandlerTest.authService
            },
        )
        coEvery { authService.updateReadingStatusForManga(MANGA_ID, any()) } returns ok()
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `a failed follow does not send the status while the title is not followed`() = runTest {
        coEvery { authService.followManga(MANGA_ID) } returns
            ApiResponse.Failure.Exception(IOException("connection reset"))
        coEvery { authService.userFollowsManga(MANGA_ID) } returns httpError(404)

        assertFalse(FollowsHandler().updateFollowStatus(MANGA_ID, FollowStatus.READING))
        coVerify(exactly = 0) { authService.updateReadingStatusForManga(any(), any()) }
    }

    @Test
    fun `a failed unfollow does not send the status while the title is still followed`() = runTest {
        coEvery { authService.unfollowManga(MANGA_ID) } returns httpError(500)
        coEvery { authService.userFollowsManga(MANGA_ID) } returns ok()

        assertFalse(FollowsHandler().updateFollowStatus(MANGA_ID, FollowStatus.DROPPED))
        coVerify(exactly = 0) { authService.updateReadingStatusForManga(any(), any()) }
    }

    @Test
    fun `a failed follow returns false when MangaDex cannot say whether the title is followed`() =
        runTest {
            coEvery { authService.followManga(MANGA_ID) } returns httpError(503)
            coEvery { authService.userFollowsManga(MANGA_ID) } returns
                ApiResponse.Failure.Exception(IOException("connection reset"))

            assertFalse(FollowsHandler().updateFollowStatus(MANGA_ID, FollowStatus.READING))
            coVerify(exactly = 0) { authService.updateReadingStatusForManga(any(), any()) }
        }

    @Test
    fun `a failed unfollow does not send the status when the follow check fails`() = runTest {
        coEvery { authService.unfollowManga(MANGA_ID) } returns httpError(500)
        coEvery { authService.userFollowsManga(MANGA_ID) } returns httpError(500)

        assertFalse(FollowsHandler().updateFollowStatus(MANGA_ID, FollowStatus.DROPPED))
        coVerify(exactly = 0) { authService.updateReadingStatusForManga(any(), any()) }
    }

    @Test
    fun `an unfollow answered with 404 sends the status when the title is not followed`() =
        runTest {
            coEvery { authService.unfollowManga(MANGA_ID) } returns httpError(404)
            coEvery { authService.userFollowsManga(MANGA_ID) } returns httpError(404)

            assertTrue(FollowsHandler().updateFollowStatus(MANGA_ID, FollowStatus.DROPPED))
            coVerify {
                authService.updateReadingStatusForManga(MANGA_ID, ReadingStatusDto("dropped"))
            }
        }

    @Test
    fun `a failed follow sends the status when the title is already followed`() = runTest {
        coEvery { authService.followManga(MANGA_ID) } returns httpError(500)
        coEvery { authService.userFollowsManga(MANGA_ID) } returns ok()

        assertTrue(FollowsHandler().updateFollowStatus(MANGA_ID, FollowStatus.READING))
        coVerify { authService.updateReadingStatusForManga(MANGA_ID, ReadingStatusDto("reading")) }
    }

    @Test
    fun `a follow that succeeds sends the status without checking the follow list`() = runTest {
        coEvery { authService.followManga(MANGA_ID) } returns ok()

        assertTrue(FollowsHandler().updateFollowStatus(MANGA_ID, FollowStatus.READING))
        coVerify { authService.updateReadingStatusForManga(MANGA_ID, ReadingStatusDto("reading")) }
        coVerify(exactly = 0) { authService.userFollowsManga(any()) }
    }

    @Test
    fun `a failed status update returns false`() = runTest {
        coEvery { authService.followManga(MANGA_ID) } returns ok()
        coEvery { authService.updateReadingStatusForManga(MANGA_ID, any()) } returns httpError(500)

        assertFalse(FollowsHandler().updateFollowStatus(MANGA_ID, FollowStatus.READING))
    }

    private fun ok(): ApiResponse<ResultDto> = ApiResponse.Success(ResultDto("ok"))

    private fun httpError(code: Int): ApiResponse<ResultDto> =
        ApiResponse.Failure.Error(
            Response.error<ResultDto>(
                code,
                """{"result":"error"}""".toResponseBody("application/json".toMediaType()),
            ),
        )

    private companion object {
        const val MANGA_ID = "00000000-0000-4000-8000-000000000001"
    }
}
