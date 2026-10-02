package eu.kanade.tachiyomi.ui.setting

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var preferences: PreferencesHelper
    private lateinit var readerPreferences: ReaderPreferences
    private lateinit var mangaDetailsPreferences: MangaDetailsPreferences
    private lateinit var networkPreferences: NetworkPreferences
    private lateinit var networkHelper: NetworkHelper
    private lateinit var downloadManager: DownloadManager
    private lateinit var mangaRepository: MangaRepository
    private lateinit var chapterRepository: ChapterRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var historyRepository: HistoryRepository

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(testDispatcher)

        preferences = mockk(relaxed = true)
        readerPreferences = mockk(relaxed = true)
        mangaDetailsPreferences = mockk(relaxed = true)
        networkPreferences = mockk(relaxed = true)
        networkHelper = mockk(relaxed = true)
        downloadManager = mockk(relaxed = true)
        mangaRepository = mockk(relaxed = true)
        chapterRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)

        Injekt.addSingleton(preferences)
        Injekt.addSingleton(readerPreferences)
        Injekt.addSingleton(mangaDetailsPreferences)
        Injekt.addSingleton(networkPreferences)
        Injekt.addSingleton(networkHelper)
        Injekt.addSingleton(downloadManager)
        Injekt.addSingleton(mangaRepository)
        Injekt.addSingleton(chapterRepository)
        Injekt.addSingleton(categoryRepository)
        Injekt.addSingleton(historyRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `reindexDownloads calls downloadManager and emits progress and complete toasts`() =
        runTest(testDispatcher) {
            every { downloadManager.isReindexing } returns false
            coEvery { downloadManager.reindexDownloads() } returns true

            val viewModel = AdvancedSettingsViewModel()

            val toastEvents = mutableListOf<UiText>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.toastEvent.toList(toastEvents)
                }

            viewModel.reindexDownloads().join()

            coVerify(exactly = 1) { downloadManager.reindexDownloads() }
            assertEquals(
                listOf(
                    R.string.reindex_downloads_invalidate,
                    R.string.reindex_downloads_complete,
                ),
                toastEvents.map { (it as UiText.StringResource).resourceId },
            )

            job.cancel()
        }

    @Test
    fun `reindexDownloads when already running emits in progress toast and skips reindexing`() =
        runTest(testDispatcher) {
            every { downloadManager.isReindexing } returns true

            val viewModel = AdvancedSettingsViewModel()

            val toastEvents = mutableListOf<UiText>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.toastEvent.toList(toastEvents)
                }

            viewModel.reindexDownloads().join()

            coVerify(exactly = 0) { downloadManager.reindexDownloads() }
            assertEquals(
                listOf(R.string.reindex_in_progress),
                toastEvents.map { (it as UiText.StringResource).resourceId },
            )

            job.cancel()
        }

    @Test
    fun `reindexDownloads when downloadManager returns false does not emit complete toast`() =
        runTest(testDispatcher) {
            every { downloadManager.isReindexing } returns false
            coEvery { downloadManager.reindexDownloads() } returns false

            val viewModel = AdvancedSettingsViewModel()

            val toastEvents = mutableListOf<UiText>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.toastEvent.toList(toastEvents)
                }

            viewModel.reindexDownloads().join()

            coVerify(exactly = 1) { downloadManager.reindexDownloads() }
            assertEquals(
                listOf(R.string.reindex_downloads_invalidate),
                toastEvents.map { (it as UiText.StringResource).resourceId },
            )

            job.cancel()
        }
}
