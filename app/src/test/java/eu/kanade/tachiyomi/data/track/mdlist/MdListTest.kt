package eu.kanade.tachiyomi.data.track.mdlist

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class MdListTest {

    private lateinit var context: Context
    private lateinit var preferences: PreferencesHelper
    private lateinit var networkService: NetworkHelper
    private lateinit var chapterRepository: ChapterRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var mangaRepository: MangaRepository
    private lateinit var trackRepository: TrackRepository
    private lateinit var sourceManager: SourceManager
    private lateinit var mangaDexLoginHelper: MangaDexLoginHelper
    private lateinit var mangaDex: MangaDex
    private lateinit var mdList: MdList

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        context = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        networkService = mockk(relaxed = true)
        chapterRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)
        mangaRepository = mockk(relaxed = true)
        trackRepository = mockk(relaxed = true)
        sourceManager = mockk(relaxed = true)
        mangaDexLoginHelper = mockk(relaxed = true)
        mangaDex = mockk(relaxed = true)

        coEvery { sourceManager.mangaDex } returns mangaDex

        Injekt.addSingleton(preferences)
        Injekt.addSingleton(networkService)
        Injekt.addSingleton(chapterRepository)
        Injekt.addSingleton(historyRepository)
        Injekt.addSingleton(mangaRepository)
        Injekt.addSingleton(trackRepository)
        Injekt.addSingleton(sourceManager)
        Injekt.addSingleton(mangaDexLoginHelper)

        mdList = MdList(context, 1)
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given network error when mdList update is called then exception is propagated`() =
        runTest {
            val track =
                Track.create(1).apply {
                    manga_id = 1L
                    tracking_url = "https://mangadex.org/title/00000000-0000-0000-0000-000000000001"
                    status = FollowStatus.READING.int
                    last_chapter_read = 5f
                }

            coEvery { mangaDex.updateFollowStatus(any(), any()) } throws
                IOException("Network error")

            var caught = false
            try {
                mdList.update(track, setToRead = true)
            } catch (e: IOException) {
                caught = true
                assertEquals("Network error", e.message)
            }
            assertTrue("Expected IOException to be thrown", caught)
        }

    @Test
    fun `given the rating is accepted when updateScore is called then the local track is saved`() =
        runTest {
            val track =
                Track.create(1).apply {
                    manga_id = 1L
                    tracking_url = "https://mangadex.org/title/00000000-0000-0000-0000-000000000001"
                    score = 7f
                }
            coEvery { mangaDex.updateRating(track) } returns true

            mdList.updateScore(track)

            coVerify(exactly = 1) { trackRepository.insertTrack(track) }
        }

    @Test
    fun `given the rating is rejected when updateScore is called then the local track is not saved`() =
        runTest {
            val track =
                Track.create(1).apply {
                    manga_id = 1L
                    tracking_url = "https://mangadex.org/title/00000000-0000-0000-0000-000000000001"
                    score = 7f
                }
            coEvery { mangaDex.updateRating(track) } returns false

            mdList.updateScore(track)

            coVerify(exactly = 0) { trackRepository.insertTrack(any()) }
        }
}
