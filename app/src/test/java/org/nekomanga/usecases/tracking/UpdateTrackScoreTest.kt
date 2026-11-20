package org.nekomanga.usecases.tracking

import android.content.Context
import android.text.TextUtils
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.mdlist.MdListScoreUpdateFailed
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class UpdateTrackScoreTest {

    private lateinit var trackRepository: TrackRepository
    private lateinit var mangaDex: MangaDex
    private lateinit var useCase: UpdateTrackScore

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        trackRepository = mockk(relaxed = true)
        mangaDex = mockk(relaxed = true)
        val sourceManager = mockk<SourceManager>(relaxed = true)
        every { sourceManager.mangaDex } returns mangaDex

        // MdList finds v3 ids with TextUtils.isDigitsOnly, which the android.jar stub does not run.
        mockkStatic(TextUtils::class)
        every { TextUtils.isDigitsOnly(any()) } answers
            {
                firstArg<CharSequence>().all(Char::isDigit)
            }

        Injekt.addSingleton(mockk<PreferencesHelper>(relaxed = true))
        Injekt.addSingleton(mockk<NetworkHelper>(relaxed = true))
        Injekt.addSingleton(mockk<ChapterRepository>(relaxed = true))
        Injekt.addSingleton(mockk<HistoryRepository>(relaxed = true))
        Injekt.addSingleton(mockk<MangaRepository>(relaxed = true))
        Injekt.addSingleton(trackRepository)
        Injekt.addSingleton(sourceManager)
        Injekt.addSingleton(mockk<MangaDexLoginHelper>(relaxed = true))

        val mdList = MdList(mockk<Context>(relaxed = true), TrackManager.MDLIST)
        val trackManager = mockk<TrackManager>()
        every { trackManager.mdList } returns mdList
        useCase = UpdateTrackScore(updateTrackingService = mockk(), trackManager = trackManager)
    }

    @After
    fun tearDown() {
        unmockkStatic(TextUtils::class)
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given MangaDex saves the score when await is called then the score is saved on the track`() =
        runTest {
            coEvery { mangaDex.updateRating(any()) } returns true

            val result = useCase.await(7, trackAndService(UUID_URL))

            assertEquals(TrackingUpdate.Success, result)
            coVerify(exactly = 1) { mangaDex.updateRating(match { it.score == 7f }) }
            coVerify(exactly = 1) {
                trackRepository.insertTrack(match { it.id == TRACK_ID && it.score == 7f })
            }
        }

    @Test
    fun `given MangaDex refuses the score when await is called then an error is returned and nothing is saved`() =
        runTest {
            coEvery { mangaDex.updateRating(any()) } returns false

            val result = useCase.await(7, trackAndService(UUID_URL))

            assertTrue("Expected TrackingUpdate.Error, got $result", result is TrackingUpdate.Error)
            assertTrue((result as TrackingUpdate.Error).exception is MdListScoreUpdateFailed)
            coVerify(exactly = 0) { trackRepository.insertTrack(any()) }
        }

    @Test
    fun `given a title on a v3 id when await is called then the score is saved without sending it`() =
        runTest {
            val result = useCase.await(7, trackAndService(V3_URL))

            assertEquals(TrackingUpdate.Success, result)
            coVerify(exactly = 0) { mangaDex.updateRating(any()) }
            coVerify(exactly = 1) { trackRepository.insertTrack(match { it.score == 7f }) }
        }

    private fun trackAndService(trackingUrl: String) =
        TrackingConstants.TrackAndService(
            track =
                TrackItem(
                    id = TRACK_ID,
                    mangaId = 1L,
                    trackServiceId = TrackManager.MDLIST,
                    mediaId = 0L,
                    libraryId = null,
                    title = "",
                    lastChapterRead = 0f,
                    totalChapters = 0,
                    score = 3f,
                    status = FollowStatus.READING.int,
                    trackingUrl = trackingUrl,
                    startedReadingDate = 0L,
                    finishedReadingDate = 0L,
                ),
            service =
                TrackServiceItem(
                    id = TrackManager.MDLIST,
                    nameRes = 0,
                    logoRes = 0,
                    logoColor = 0,
                    statusList = emptyList(),
                    supportsReadingDates = false,
                    canRemoveFromService = false,
                    isAutoAddTracker = true,
                    isMdList = true,
                    status = { "" },
                    displayScore = { "" },
                    scoreList = (0..10).map(Int::toString),
                    indexToScore = { index -> index.toFloat() },
                ),
        )

    private companion object {
        const val TRACK_ID = 4L
        const val UUID_URL = "https://mangadex.org/title/00000000-0000-0000-0000-000000000001"
        const val V3_URL = "https://mangadex.org/title/12345"
    }
}
