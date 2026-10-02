package eu.kanade.tachiyomi.jobs.tracking

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.TrackSearchItem
import org.nekomanga.domain.track.toTrackItem
import org.nekomanga.usecases.tracking.RegisterTracking
import org.nekomanga.usecases.tracking.SearchTracker
import org.nekomanga.usecases.tracking.TrackUseCases
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class TrackSyncProcessorTest {

    private lateinit var mangaRepository: MangaRepository
    private lateinit var trackRepository: TrackRepository
    private lateinit var trackManager: TrackManager
    private lateinit var preferences: PreferencesHelper
    private lateinit var trackUseCases: TrackUseCases
    private lateinit var searchTracker: SearchTracker
    private lateinit var registerTracking: RegisterTracking
    private lateinit var mdList: TrackService
    private lateinit var myAnimeList: TrackService

    private val manga =
        LibraryManga().apply {
            id = 5L
            title = "Manga"
            url = "/title/00000000-0000-0000-0000-000000000001"
            my_anime_list_id = "132845"
        }

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        mangaRepository = mockk(relaxed = true)
        trackRepository = mockk(relaxed = true)
        trackManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        trackUseCases = mockk(relaxed = true)
        searchTracker = mockk(relaxed = true)
        registerTracking = mockk(relaxed = true)
        mdList = mockk(relaxed = true)
        myAnimeList = mockk(relaxed = true)

        every { mdList.id } returns TrackManager.MDLIST
        every { mdList.isLogged() } returns true
        every { myAnimeList.id } returns TrackManager.MYANIMELIST
        every { myAnimeList.isLogged() } returns true
        every { trackManager.services } returns
            hashMapOf(TrackManager.MDLIST to mdList, TrackManager.MYANIMELIST to myAnimeList)
        every { trackManager.getService(TrackManager.MDLIST) } returns mdList
        every { trackManager.getService(TrackManager.MYANIMELIST) } returns myAnimeList
        every { trackManager.getIdFromManga(any(), any()) } returns "132845"
        every { preferences.autoAddTracker() } returns
            mockk<Preference<Set<String>>> {
                every { get() } returns setOf(TrackManager.MYANIMELIST.toString())
            }
        every { preferences.autoTrackContentRatingSelections() } returns
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        every { trackUseCases.searchTracker } returns searchTracker
        every { trackUseCases.registerTracking } returns registerTracking
        coEvery { mangaRepository.getLibraryList() } returns listOf(manga)

        Injekt.addSingleton(mangaRepository)
        Injekt.addSingleton(trackRepository)
        Injekt.addSingleton(trackManager)
        Injekt.addSingleton(preferences)
        Injekt.addSingleton(trackUseCases)
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given an existing MdList track when a tracker is auto added then the MdList refresh finishes first`() =
        runTest {
            val mdListTrack =
                Track.create(TrackManager.MDLIST).apply {
                    id = 7L
                    manga_id = manga.id!!
                    title = manga.title
                }
            val malTrack =
                Track.create(TrackManager.MYANIMELIST).apply {
                    manga_id = manga.id!!
                    media_id = 132845L
                    title = manga.title
                }
            val order = mutableListOf<String>()
            coEvery { trackRepository.getTracksForMangaByIds(any()) } returns listOf(mdListTrack)
            coEvery { mdList.refresh(mdListTrack) } coAnswers
                {
                    order.add("refresh")
                    mdListTrack
                }
            coEvery { searchTracker.byId(any(), any(), any(), any()) } returns
                TrackingConstants.TrackSearchResult.Success(
                    listOf(
                        TrackSearchItem(
                            coverUrl = "",
                            summary = "",
                            publishingStatus = "",
                            publishingType = "",
                            startDate = "",
                            trackItem = malTrack.toTrackItem(),
                        )
                    )
                )
            coEvery { registerTracking.await(any(), any()) } coAnswers
                {
                    order.add("add")
                    TrackingUpdate.Success
                }

            TrackSyncProcessor(UnconfinedTestDispatcher(testScheduler))
                .process(updateNotification = { _, _, _ -> }, completeNotification = {})

            assertEquals(listOf("refresh", "add"), order)
        }
}
