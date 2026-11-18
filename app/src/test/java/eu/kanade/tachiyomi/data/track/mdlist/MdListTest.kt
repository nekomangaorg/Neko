package eu.kanade.tachiyomi.data.track.mdlist

import android.content.Context
import android.text.TextUtils
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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

        // bind skips v3 ids with TextUtils.isDigitsOnly, which the android.jar stub does not run.
        mockkStatic(TextUtils::class)
        every { TextUtils.isDigitsOnly(any()) } answers
            {
                firstArg<CharSequence>().all(Char::isDigit)
            }

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
        unmockkStatic(TextUtils::class)
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
    fun `given MangaDex saves the status when mdList update is called then the track and manga are saved`() =
        runTest {
            val track = mdListTrack(FollowStatus.ON_HOLD)
            val manga = MangaImpl().apply { id = 1L }
            coEvery { mangaDex.updateFollowStatus(any(), any()) } returns true

            mdList.update(track, setToRead = false, manga = manga, chapters = emptyList())

            assertEquals(FollowStatus.ON_HOLD, manga.follow_status)
            coVerify { mangaRepository.updateManga(manga) }
            coVerify { trackRepository.insertTrack(track) }
        }

    @Test
    fun `given MangaDex refuses the status when mdList update is called then nothing is saved`() =
        runTest {
            val track = mdListTrack(FollowStatus.ON_HOLD)
            val manga = MangaImpl().apply { id = 1L }
            coEvery { mangaDex.updateFollowStatus(any(), any()) } returns false

            var caught = false
            try {
                mdList.update(track, setToRead = false, manga = manga, chapters = emptyList())
            } catch (_: MdListStatusUpdateFailed) {
                caught = true
            }

            assertTrue("Expected MdListStatusUpdateFailed to be thrown", caught)
            coVerify(exactly = 0) { mangaRepository.updateManga(any()) }
            coVerify(exactly = 0) { trackRepository.insertTrack(any()) }
        }

    @Test
    fun `given MangaDex refuses the completed status when mdList bind is called then its own status is saved`() =
        runTest {
            coEvery { mangaDex.fetchTrackingInfo(TRACKING_URL) } returns
                mdListTrack(FollowStatus.READING)
            // Every chapter is read and the last one is marked [END], so update completes the track
            coEvery { mangaRepository.getMangaById(1L) } returns MangaImpl().apply { id = 1L }
            coEvery { chapterRepository.getChaptersForManga(1L) } returns
                listOf(
                    Chapter.create().apply {
                        name = "Chapter 10 [END]"
                        read = true
                    },
                )
            coEvery { mangaDex.updateFollowStatus(any(), any()) } returns false
            val savedStatuses = mutableListOf<Int>()
            coEvery { trackRepository.insertTrack(any()) } answers
                {
                    savedStatuses += firstArg<Track>().status
                    1L
                }

            val bound = mdList.bind(mdListTrack(FollowStatus.UNFOLLOWED))

            coVerify { mangaDex.updateFollowStatus(any(), FollowStatus.COMPLETED) }
            assertEquals(FollowStatus.READING.int, bound.status)
            assertEquals(listOf(FollowStatus.READING.int), savedStatuses)
        }

    private fun mdListTrack(status: FollowStatus) =
        Track.create(TrackManager.MDLIST).apply {
            manga_id = 1L
            tracking_url = TRACKING_URL
            this.status = status.int
        }

    private companion object {
        const val TRACKING_URL = "https://mangadex.org/title/00000000-0000-0000-0000-000000000001"
    }
}
