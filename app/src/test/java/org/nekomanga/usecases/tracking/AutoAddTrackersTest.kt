package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import eu.kanade.tachiyomi.util.system.NetworkState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toDbTrack
import tachiyomi.core.preference.Preference

class AutoAddTrackersTest {

    private val preferences: PreferencesHelper = mockk()
    private val mangaDexPreferences: MangaDexPreferences = mockk()
    private val trackManager: TrackManager = mockk()
    private val trackRepository: TrackRepository = mockk()
    private val downloadManager: DownloadManager = mockk()
    private val updateTrackingService: UpdateTrackingService = mockk()
    private val searchTracker: SearchTracker = mockk()
    private val registerTracking: RegisterTracking = mockk()

    private val useCase =
        AutoAddTrackers(
            preferences = preferences,
            mangaDexPreferences = mangaDexPreferences,
            trackManager = trackManager,
            trackRepository = trackRepository,
            downloadManager = downloadManager,
            updateTrackingService = updateTrackingService,
            searchTracker = searchTracker,
            registerTracking = registerTracking,
        )

    private fun mockNetworkState(isOnline: Boolean) {
        val networkState = mockk<NetworkState> { every { this@mockk.isOnline } returns isOnline }
        every { downloadManager.networkStateFlow() } returns flowOf(networkState)
    }

    private fun mockPreferences(
        autoAddTracker: Set<String> = emptySet(),
        autoAddToMangaDexLibrary: Int = 0,
        autoTrackContentRatingSelections: Set<String> = emptySet(),
    ) {
        val autoAddTrackerPref = mockk<Preference<Set<String>>> {
            every { get() } returns autoAddTracker
        }
        every { preferences.autoAddTracker() } returns autoAddTrackerPref

        val autoAddToMangaDexLibraryPref = mockk<Preference<Int>> {
            every { get() } returns autoAddToMangaDexLibrary
        }
        every { mangaDexPreferences.autoAddToMangaDexLibrary() } returns
            autoAddToMangaDexLibraryPref

        val autoTrackContentRatingSelectionsPref = mockk<Preference<Set<String>>> {
            every { get() } returns autoTrackContentRatingSelections
        }
        every { preferences.autoTrackContentRatingSelections() } returns
            autoTrackContentRatingSelectionsPref
    }

    @Test
    fun `given no auto-add trackers selected when invoked then does not auto-add`() = runTest {
        mockPreferences(autoAddTracker = emptySet())
        mockNetworkState(isOnline = true)

        val mangaItem = mockk<MangaItem>(relaxed = true) {
            every { favorite } returns true
        }

        useCase(
            mangaItem = mangaItem,
            loggedInTrackerService = emptyList(),
            tracks = emptyList(),
            onShowSnackbar = { _, _ -> },
        )

        coVerify(exactly = 0) { trackRepository.insertTrack(any()) }
    }

    @Test
    fun `given auto-add trackers when offline then shows snackbar and returns`() = runTest {
        mockPreferences(
            autoAddTracker = setOf("1", "2"),
            autoTrackContentRatingSelections = setOf("safe"),
        )
        mockNetworkState(isOnline = false)

        val mangaMock = mockk<Manga> {
            every { getContentRating() } returns "safe"
        }
        val mangaItem = mockk<MangaItem>(relaxed = true) {
            every { favorite } returns true
            every { toManga() } returns mangaMock
        }

        var snackbarMessage: String? = null
        useCase(
            mangaItem = mangaItem,
            loggedInTrackerService = emptyList(),
            tracks = emptyList(),
            onShowSnackbar = { message, _ -> snackbarMessage = message },
        )

        assertEquals("No network connection, cannot autolink tracker", snackbarMessage)
    }

    @Test
    fun `given mdList auto-add configured when online and mdList track does not exist then creates and binds track`() =
        runTest {
            mockPreferences(autoAddTracker = setOf("123")) // mdList has specific id
            mockNetworkState(isOnline = true)

            val mangaMock = mockk<Manga>(relaxed = true) {
                every { getContentRating() } returns "safe"
            }
            val mangaItem = mockk<MangaItem>(relaxed = true) {
                every { favorite } returns true
                every { toManga() } returns mangaMock
            }

            val mdListServiceItem = mockk<TrackServiceItem> {
                every { isMdList } returns true
                every { id } returns 123
            }

            val mdListMock = mockk<MdList>(relaxed = true)
            every { trackManager.mdList } returns mdListMock

            val initialTrack = mockk<Track>(relaxed = true)
            every { mdListMock.matchingTrack(any()) } returns false
            every { mdListMock.createInitialTracker(any()) } returns initialTrack
            coEvery { mdListMock.bind(any()) } returns Unit
            coEvery { trackRepository.insertTrack(any()) } returns 1L

            useCase(
                mangaItem = mangaItem,
                loggedInTrackerService = listOf(mdListServiceItem),
                tracks = emptyList(),
                onShowSnackbar = { _, _ -> },
            )

            coVerify(exactly = 1) { trackRepository.insertTrack(initialTrack) }
            coVerify(exactly = 1) { mdListMock.bind(initialTrack) }
        }
}
