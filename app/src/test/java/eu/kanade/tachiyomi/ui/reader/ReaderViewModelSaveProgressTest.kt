package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.manga.MangaMappings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.AppDatabase
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
class ReaderViewModelSaveProgressTest {

    private val appDatabase: AppDatabase = mockk()
    private val chapterRepository: ChapterRepository = mockk(relaxed = true)
    private val downloadManager: DownloadManager = mockk(relaxed = true)

    /** Chapter id and last_page_read of each chapter passed to updateChaptersProgress. */
    private val savedProgress = CopyOnWriteArrayList<Pair<Long?, Int>>()

    @Volatile private var incognito = true

    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        Injekt = InjektScope(DefaultRegistrar())

        val manga =
            Manga.create("/title/$MANGA_UUID", "Title").apply {
                id = MANGA_ID
                viewer_flags = 0
            }
        val mangaRepository: MangaRepository = mockk(relaxed = true)
        coEvery { mangaRepository.getMangaById(MANGA_ID) } returns manga
        coEvery { chapterRepository.getChaptersForManga(MANGA_ID) } returns
            (1L..4L).map { chapterId ->
                Chapter.create().apply {
                    id = chapterId
                    manga_id = MANGA_ID
                    url = "/chapter/$chapterId"
                    name = "Ch.$chapterId"
                    chapter_txt = "Ch.$chapterId"
                    last_page_read = STORED_PAGE
                }
            }
        coEvery { chapterRepository.getChapterById(any()) } returns null
        coEvery { chapterRepository.updateChaptersProgress(any()) } answers
            {
                firstArg<List<Chapter>>().forEach { savedProgress += it.id to it.last_page_read }
            }

        mockkStatic("androidx.room.RoomDatabaseKt")
        val transaction = slot<suspend () -> Unit>()
        coEvery { appDatabase.withTransaction(capture(transaction)) } coAnswers
            {
                transaction.captured.invoke()
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
        val incognitoMode: Preference<Boolean> = mockk { every { get() } answers { incognito } }
        val securityPreferences: SecurityPreferences = mockk(relaxed = true)
        every { securityPreferences.incognitoMode() } returns incognitoMode

        mockkObject(NotificationReceiver.Companion)
        every { NotificationReceiver.dismissNotification(any(), any(), any()) } just runs
        mockkConstructor(ChapterLoader::class)
        coEvery { anyConstructed<ChapterLoader>().loadChapter(any()) } just runs
        coEvery { downloadManager.getQueuedDownloadOrNull(any()) } returns null

        viewModel =
            ReaderViewModel(
                savedState = SavedStateHandle(),
                appDatabase = appDatabase,
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
    fun `pausing in incognito keeps the page the reader is on`() = runTest {
        val chapter = openChapter()
        selectPage(chapter, SELECTED_PAGE)

        awaitWork { viewModel.saveCurrentChapterReadingProgress() }

        assertEquals(SELECTED_PAGE, chapter.requestedPage)
    }

    @Test
    fun `saving the instance state in incognito keeps the page the reader is on`() = runTest {
        val chapter = openChapter()
        selectPage(chapter, SELECTED_PAGE)

        awaitWork { viewModel.onSaveInstanceState() }

        assertEquals(SELECTED_PAGE, chapter.requestedPage)
    }

    @Test
    fun `pausing outside incognito saves the page the reader is on`() = runTest {
        incognito = false
        val chapter = openChapter()
        selectPage(chapter, SELECTED_PAGE)

        awaitWork { viewModel.saveCurrentChapterReadingProgress() }

        assertEquals(SELECTED_PAGE, chapter.requestedPage)
        assertEquals(listOf<Pair<Long?, Int>>(3L to SELECTED_PAGE), savedProgress.toList())
    }

    @Test
    fun `pausing after incognito is turned off keeps the page the reader is on`() = runTest {
        val chapter = openChapter()
        selectPage(chapter, SELECTED_PAGE)
        // The incognito notification turns it off while the reader stays open
        incognito = false

        awaitWork { viewModel.saveCurrentChapterReadingProgress() }

        assertEquals(SELECTED_PAGE, chapter.requestedPage)
    }

    /** Opens Ch.3. It is unread, so the reader starts at its stored page. */
    private suspend fun TestScope.openChapter(): ReaderChapter {
        assertTrue(viewModel.init(MANGA_ID, 3L).getOrThrow())
        advanceUntilIdle()
        val chapter = viewModel.state.value.viewerChapters!!.currChapter
        assertEquals(STORED_PAGE, chapter.requestedPage)
        return chapter
    }

    private suspend fun selectPage(readerChapter: ReaderChapter, index: Int) = awaitWork {
        val page = ReaderPage(index).apply { chapter = readerChapter }
        viewModel.onPageSelected(page, hasExtraPage = false)
    }

    /** Runs [action], then waits until the jobs it started on the view model scope finish. */
    private suspend fun awaitWork(action: () -> Unit) {
        val scopeJob = viewModel.viewModelScope.coroutineContext.job
        val alreadyRunning = scopeJob.children.toSet()
        action()
        // They run on Dispatchers.IO, outside the test scheduler, so this waits in real time
        withContext(Dispatchers.Default) {
            withTimeout(10_000) {
                while (scopeJob.children.any { it !in alreadyRunning && !it.isCompleted }) {
                    delay(5)
                }
            }
        }
    }

    private fun <T : Any> preference(value: T): Preference<T> = mockk {
        every { get() } returns value
    }

    private companion object {
        const val MANGA_ID = 1L
        const val MANGA_UUID = "7b1f3c2a-4d5e-4f60-8a1b-2c3d4e5f6a7b"
        const val STORED_PAGE = 2
        const val SELECTED_PAGE = 9
    }
}
