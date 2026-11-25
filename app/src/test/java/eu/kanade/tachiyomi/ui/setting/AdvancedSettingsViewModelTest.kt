package eu.kanade.tachiyomi.ui.setting

import android.app.Application
import android.util.Log
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.jobs.library.BlockingWork
import eu.kanade.tachiyomi.jobs.library.LibraryRepairGate
import eu.kanade.tachiyomi.jobs.library.LibraryRepairJob
import eu.kanade.tachiyomi.jobs.library.LibraryRepairer
import eu.kanade.tachiyomi.jobs.library.RepairCheck
import eu.kanade.tachiyomi.jobs.library.RepairGroup
import eu.kanade.tachiyomi.jobs.library.RepairInventory
import eu.kanade.tachiyomi.jobs.library.SkipReason
import eu.kanade.tachiyomi.jobs.library.SkippedEntry
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import timber.log.Timber
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
    private lateinit var application: Application
    private lateinit var libraryRepairer: LibraryRepairer
    private lateinit var libraryRepairGate: LibraryRepairGate

    private val doubledTitle =
        RepairInventory(
            work = listOf(RepairGroup("uuid", listOf(1L, 2L), "Title")),
            duplicateTitles = 1,
            damagedEntries = 0,
            skipped = emptyList(),
        )

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
        application = mockk(relaxed = true)
        libraryRepairer = mockk()
        libraryRepairGate = mockk(relaxed = true)

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
        Injekt.addSingleton(application)
        Injekt.addSingleton(libraryRepairer)
        Injekt.addSingleton(libraryRepairGate)
        mockkObject(LibraryRepairJob.Companion)
        justRun { LibraryRepairJob.start(any()) }
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

    @Test
    fun `given nothing to repair when checkLibraryRepair then a toast and no confirmation`() =
        runTest(testDispatcher) {
            coEvery { libraryRepairer.check(libraryRepairGate) } returns RepairCheck.NothingToRepair
            val viewModel = AdvancedSettingsViewModel()

            val toasts = toastsDuring(viewModel) { viewModel.checkLibraryRepair().join() }

            assertEquals(listOf(UiText.StringResource(R.string.library_repair_nothing)), toasts)
            assertEquals(null, viewModel.libraryRepairConfirm.value)
        }

    @Test
    fun `given a doubled title when checkLibraryRepair then the confirmation holds the inventory`() =
        runTest(testDispatcher) {
            coEvery { libraryRepairer.check(libraryRepairGate) } returns
                RepairCheck.Confirm(doubledTitle)
            val viewModel = AdvancedSettingsViewModel()

            val toasts = toastsDuring(viewModel) { viewModel.checkLibraryRepair().join() }

            assertEquals(doubledTitle, viewModel.libraryRepairConfirm.value)
            assertEquals(emptyList<UiText>(), toasts)
        }

    @Test
    fun `given titles to repair when checkLibraryRepair then the counts are logged`() =
        runTest(testDispatcher) {
            val logs = mutableListOf<String>()
            val tree =
                object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        if (priority == Log.INFO) logs += message
                    }
                }
            coEvery { libraryRepairer.check(libraryRepairGate) } returns
                RepairCheck.Confirm(
                    RepairInventory(
                        work = listOf(RepairGroup("uuid", listOf(1L, 2L), "Title")),
                        duplicateTitles = 3,
                        damagedEntries = 2,
                        skipped = listOf(SkippedEntry("Other", SkipReason.NotMangaDex)),
                    ),
                )
            val viewModel = AdvancedSettingsViewModel()

            Timber.plant(tree)
            try {
                viewModel.checkLibraryRepair().join()
            } finally {
                Timber.uproot(tree)
            }

            assertEquals(
                listOf(
                    "Library repair check found 3 duplicate titles, 2 damaged entries, 1 skipped",
                ),
                logs,
            )
        }

    @Test
    fun `given a confirmation when startLibraryRepair then the job starts and the confirmation clears`() =
        runTest(testDispatcher) {
            coEvery { libraryRepairer.check(libraryRepairGate) } returns
                RepairCheck.Confirm(doubledTitle)
            val viewModel = AdvancedSettingsViewModel()
            viewModel.checkLibraryRepair().join()

            viewModel.startLibraryRepair()

            verify(exactly = 1) { LibraryRepairJob.start(application) }
            assertEquals(null, viewModel.libraryRepairConfirm.value)
        }

    @Test
    fun `given a confirmation when dismissLibraryRepair then it clears and no job starts`() =
        runTest(testDispatcher) {
            coEvery { libraryRepairer.check(libraryRepairGate) } returns
                RepairCheck.Confirm(doubledTitle)
            val viewModel = AdvancedSettingsViewModel()
            viewModel.checkLibraryRepair().join()

            viewModel.dismissLibraryRepair()

            verify(exactly = 0) { LibraryRepairJob.start(any()) }
            assertEquals(null, viewModel.libraryRepairConfirm.value)
        }

    @Test
    fun `given the repair is running when checkLibraryRepair then the running toast shows`() =
        runTest(testDispatcher) {
            coEvery { libraryRepairer.check(libraryRepairGate) } returns RepairCheck.Running
            val viewModel = AdvancedSettingsViewModel()

            val toasts = toastsDuring(viewModel) { viewModel.checkLibraryRepair().join() }

            assertEquals(listOf(UiText.StringResource(R.string.library_repair_running)), toasts)
            assertEquals(null, viewModel.libraryRepairConfirm.value)
        }

    @Test
    fun `given running work when checkLibraryRepair then the toast names each kind of work once`() =
        runTest(testDispatcher) {
            every { application.getString(R.string.library_repair_work_tracking) } returns
                "tracking sync"
            every { application.getString(R.string.library_repair_work_restore) } returns "restore"
            coEvery { libraryRepairer.check(libraryRepairGate) } returns
                RepairCheck.Blocked(
                    listOf(
                        BlockingWork.TRACKING_SYNC,
                        BlockingWork.DELAYED_TRACKING,
                        BlockingWork.RESTORE,
                    ),
                )
            val viewModel = AdvancedSettingsViewModel()

            val toasts = toastsDuring(viewModel) { viewModel.checkLibraryRepair().join() }

            assertEquals(
                listOf(
                    UiText.StringResource(
                        R.string.library_repair_blocked,
                        "tracking sync, restore",
                    ),
                ),
                toasts,
            )
            assertEquals(null, viewModel.libraryRepairConfirm.value)
        }

    @Test
    fun `given the check fails when checkLibraryRepair then an error toast and no confirmation`() =
        runTest(testDispatcher) {
            coEvery { libraryRepairer.check(libraryRepairGate) } throws
                IllegalStateException("disk")
            val viewModel = AdvancedSettingsViewModel()

            val toasts = toastsDuring(viewModel) { viewModel.checkLibraryRepair().join() }

            assertEquals(listOf(UiText.StringResource(R.string.library_repair_error)), toasts)
            assertEquals(null, viewModel.libraryRepairConfirm.value)
        }

    @Test
    fun `given the check is cancelled when checkLibraryRepair then it ends cancelled with no toast`() =
        runTest(testDispatcher) {
            coEvery { libraryRepairer.check(libraryRepairGate) } throws
                CancellationException("left the screen")
            val viewModel = AdvancedSettingsViewModel()
            var cancelled = false

            val toasts =
                toastsDuring(viewModel) {
                    val job = viewModel.checkLibraryRepair()
                    job.join()
                    cancelled = job.isCancelled
                }

            assertEquals(emptyList<UiText>(), toasts)
            assertEquals(true, cancelled)
        }

    /** The toasts [viewModel] sends while [action] runs. */
    private suspend fun TestScope.toastsDuring(
        viewModel: AdvancedSettingsViewModel,
        action: suspend () -> Unit,
    ): List<UiText> {
        val toasts = mutableListOf<UiText>()
        val collector =
            launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.toastEvent.toList(toasts) }
        action()
        collector.cancel()
        return toasts
    }
}
