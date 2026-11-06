package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.manga.MangaMappings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.storage.StorageManager
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelChapterSwapTest {

    private val downloadManager: DownloadManager = mockk(relaxed = true)
    private val queueDeleteSaw = mutableListOf<Long?>()

    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setup() {
        // A coroutine on the unconfined dispatcher resumes inline only when its waker does not run
        // on an unconfined dispatcher itself, and the swap runs on Main.
        Dispatchers.setMain(StandardTestDispatcher())
        Injekt = InjektScope(DefaultRegistrar())

        val manga =
            Manga.create("/title/$MANGA_UUID", "Title").apply {
                id = MANGA_ID
                viewer_flags = 0
            }
        val mangaRepository: MangaRepository = mockk(relaxed = true)
        coEvery { mangaRepository.getMangaById(MANGA_ID) } returns manga
        val chapterRepository: ChapterRepository = mockk(relaxed = true)
        coEvery { chapterRepository.getChaptersForManga(MANGA_ID) } returns
            (1L..4L).map { chapterId ->
                Chapter.create().apply {
                    id = chapterId
                    manga_id = MANGA_ID
                    url = "/chapter/$chapterId"
                    name = "Ch.$chapterId"
                    chapter_txt = "Ch.$chapterId"
                }
            }

        val chapterItemFilter: ChapterItemFilter = mockk()
        every { chapterItemFilter.filterChaptersForReader(any(), any(), any(), any()) } answers
            {
                firstArg()
            }

        val sourceManager: SourceManager = mockk(relaxed = true)
        val libraryPreferences: LibraryPreferences = mockk()
        every { libraryPreferences.enableLocalChapters() } returns preference(false)
        val mangaDexPreferences: MangaDexPreferences = mockk()
        every { mangaDexPreferences.readingSync() } returns preference(false)
        val storageManager: StorageManager = mockk(relaxed = true)
        val preferences: PreferencesHelper = mockk(relaxed = true)
        val mangaDetailsPreferences: MangaDetailsPreferences = mockk(relaxed = true)
        every { mangaDetailsPreferences.sortChapterOrder() } returns preference(0)
        Injekt.addSingleton(sourceManager)
        Injekt.addSingleton(downloadManager)
        Injekt.addSingleton(libraryPreferences)
        Injekt.addSingleton(mangaDexPreferences)
        Injekt.addSingleton(storageManager)
        Injekt.addSingleton(preferences)
        Injekt.addSingleton(mangaDetailsPreferences)
        Injekt.addSingleton(chapterItemFilter)
        Injekt.addSingleton(mockk<MangaMappings>(relaxed = true))
        Injekt.addSingleton<Application>(mockk(relaxed = true))

        val readerPreferences: ReaderPreferences = mockk(relaxed = true)
        every { readerPreferences.defaultReadingMode() } returns preference(0)
        // Skips saving progress and history, which this test is not about
        val securityPreferences: SecurityPreferences = mockk(relaxed = true)
        every { securityPreferences.incognitoMode() } returns preference(true)

        mockkObject(NotificationReceiver.Companion)
        every { NotificationReceiver.dismissNotification(any(), any(), any()) } just runs
        mockkConstructor(ChapterLoader::class)
        coEvery { anyConstructed<ChapterLoader>().loadChapter(any()) } just runs
        coEvery { downloadManager.getQueuedDownloadOrNull(any()) } answers
            {
                queueDeleteSaw += viewModel.state.value.viewerChapters?.currChapter?.chapter?.id
                null
            }

        viewModel =
            ReaderViewModel(
                savedState = SavedStateHandle(),
                appDatabase = mockk(relaxed = true),
                chapterRepository = chapterRepository,
                historyRepository = mockk(relaxed = true),
                mangaRepository = mangaRepository,
                trackRepository = mockk(relaxed = true),
                sourceManager = sourceManager,
                downloadManager = downloadManager,
                coverCache = mockk(relaxed = true),
                preferences = preferences,
                mangaDexPreferences = mangaDexPreferences,
                readerPreferences = readerPreferences,
                securityPreferences = securityPreferences,
                chapterItemFilter = chapterItemFilter,
                storageManager = storageManager,
                mangaDetailsPreferences = mangaDetailsPreferences,
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `going back counts each reference once when releasing a chapter writes the state`() =
        runTest {
            // Given the reader on Ch.3 while Ch.4 is still loading, and a collector that writes the
            // state when Ch.4 is recycled, as the transition card collector does
            assertTrue(viewModel.init(MANGA_ID, 3L).getOrThrow())
            val opened = viewModel.state.value.viewerChapters!!
            val dropped = opened.nextChapter!!
            dropped.state = ReaderChapter.State.Loading
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                dropped.stateFlow
                    .filter { it == ReaderChapter.State.Wait }
                    .collect { viewModel.setMenuVisibility(true) }
            }

            // When the reader goes back to Ch.2
            viewModel.loadChapter(opened.prevChapter!!)

            // Then each chapter of the new set holds one reference and the dropped Ch.4 holds none
            val swapped = viewModel.state.value.viewerChapters!!
            val inOrder =
                listOf(swapped.prevChapter!!, swapped.currChapter, swapped.nextChapter!!, dropped)
            assertTrue(viewModel.state.value.menuVisible)
            assertEquals(listOf<Long?>(1L, 2L, 3L, 4L), inOrder.map { it.chapter.id })
            assertEquals(listOf(1, 1, 1, 0), inOrder.map { it.references })
        }

    @Test
    fun `the download queue delete runs after the swap`() = runTest {
        assertTrue(viewModel.init(MANGA_ID, 3L).getOrThrow())
        val opened = viewModel.state.value.viewerChapters!!
        queueDeleteSaw.clear()

        viewModel.loadChapter(opened.prevChapter!!)

        assertEquals(listOf<Long?>(2L), queueDeleteSaw)
    }

    private fun <T : Any> preference(value: T): Preference<T> = mockk {
        every { get() } returns value
    }

    private companion object {
        const val MANGA_ID = 1L
        const val MANGA_UUID = "7b1f3c2a-4d5e-4f60-8a1b-2c3d4e5f6a7b"
    }
}
