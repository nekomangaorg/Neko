package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.chapter.ChapterItem
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton

class RefreshTrackingUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockTrackRepository: TrackRepository
    private lateinit var mockChapterRepository: ChapterRepository

    private lateinit var trackManager: TrackManager
    private lateinit var preferences: PreferencesHelper
    private lateinit var useCase: RefreshTrackingUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockTrackRepository = mockk()
        mockChapterRepository = mockk()
        trackManager = mockk()
        preferences = mockk()

        // Injekt
        Injekt.addSingleton(mockTrackRepository)
        Injekt.addSingleton(mockChapterRepository)
        Injekt.addSingleton(trackManager)
        Injekt.addSingleton(preferences)

        useCase =
            RefreshTrackingUseCase(
                mockChapterRepository,
                mockTrackRepository,
                trackManager,
                preferences,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        val fields = Injekt::class.java.declaredFields
        for (field in fields) {
            if (field.name == "registrars") {
                field.isAccessible = true
                val map = field.get(Injekt) as MutableMap<*, *>
                map.clear()
            }
        }
    }

    @Test
    fun `given tracks when syncing with unread chapters then marks chapters as read up to tracker progress`() =
        runTest {
            // Arrange
            val mangaId = 1L
            val mockTrack =
                mockk<Track>(relaxed = true) {
                    every { sync_id } returns 1
                    every { last_chapter_read } returns 5f
                    every { id } returns 1L
                    every { manga_id } returns mangaId
                }

            coEvery { mockTrackRepository.getTracksForManga(mangaId) } returns listOf(mockTrack)

            val mockService =
                mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            every { trackManager.getService(1) } returns mockService

            coEvery { mockService.refresh(any()) } returns mockTrack

            coEvery { mockTrackRepository.insertTrack(any()) } returns 1L

            val syncChaptersPref = mockk<Preference<Boolean>>()
            every { preferences.syncChaptersWithTracker() } returns syncChaptersPref
            every { syncChaptersPref.get() } returns true

            val mockChapter3 =
                mockk<Chapter>(relaxed = true) {
                    every { read } returns false
                    every { chapter_number } returns 3f
                    every { id } returns 3L
                    every { manga_id } returns mangaId
                }
            val mockChapter6 =
                mockk<Chapter>(relaxed = true) {
                    every { read } returns false
                    every { chapter_number } returns 6f
                    every { id } returns 6L
                    every { manga_id } returns mangaId
                }

            coEvery { mockChapterRepository.getChaptersForManga(mangaId) } returns
                listOf(mockChapter3, mockChapter6)

            var markedChapters = emptyList<ChapterItem>()
            var errors = 0

            // Act
            useCase.refreshTracking(
                mangaId = mangaId,
                onRefreshError = { _, _, _ -> errors++ },
                onChaptersToMarkRead = { chapters -> markedChapters = chapters },
            )

            // Assert
            assertEquals(0, errors)
            assertEquals(1, markedChapters.size)
            assertEquals(3f, markedChapters[0].chapter.chapterNumber)
        }
}
