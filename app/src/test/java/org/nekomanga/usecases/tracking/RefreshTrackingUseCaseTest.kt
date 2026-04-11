package org.nekomanga.usecases.tracking

import com.pushtorefresh.storio.sqlite.operations.get.PreparedGetListOfObjects
import com.pushtorefresh.storio.sqlite.operations.put.PreparedPutObject
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.system.executeOnIO
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.chapter.ChapterItem
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.get

class RefreshTrackingUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: DatabaseHelper
    private lateinit var trackManager: TrackManager
    private lateinit var preferences: PreferencesHelper
    private lateinit var useCase: RefreshTrackingUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = mockk()
        trackManager = mockk()
        preferences = mockk()

        mockkStatic("eu.kanade.tachiyomi.util.system.DatabaseExtensionsKt")

        // Injekt
        Injekt.addSingleton(db)
        Injekt.addSingleton(trackManager)
        Injekt.addSingleton(preferences)

        useCase = RefreshTrackingUseCase(db, trackManager, preferences)
    }

    @After
    fun tearDown() {
        try {
            unmockkStatic("eu.kanade.tachiyomi.util.system.DatabaseExtensionsKt")
        } finally {
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

            val mockGetTracks = mockk<PreparedGetListOfObjects<Track>>()
            every { db.getTracks(mangaId) } returns mockGetTracks
            coEvery { mockGetTracks.executeOnIO() } returns listOf(mockTrack)

            val mockService =
                mockk<TrackService>(relaxed = true) { every { isLogged() } returns true }
            every { trackManager.getService(1) } returns mockService

            coEvery { mockService.refresh(any()) } returns mockTrack

            val mockInsertTrack = mockk<PreparedPutObject<Track>>()
            every { db.insertTrack(any()) } returns mockInsertTrack
            val mockPutResult = mockk<PutResult>(relaxed = true)
            coEvery { mockInsertTrack.executeOnIO() } returns mockPutResult

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

            val mockGetChapters = mockk<PreparedGetListOfObjects<Chapter>>()
            every { db.getChapters(mangaId) } returns mockGetChapters
            coEvery { mockGetChapters.executeOnIO() } returns listOf(mockChapter3, mockChapter6)

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
