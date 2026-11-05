package eu.kanade.tachiyomi.data.library

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.michaelbull.result.Ok
import com.google.common.util.concurrent.ListenableFuture
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.MangaDetailChapterInformation
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.MergeMangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.usecases.library.ShouldUpdateMangaUseCase
import org.nekomanga.usecases.manga.MangaUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/**
 * A library grouped by anything but category refreshes one section at a time. When an update is
 * already running, the job adds that section to its queue and later writes each queued row back
 * whole, so those rows must carry everything the database holds.
 */
class LibraryUpdateJobQueueTest {

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val workerParams =
        mockk<WorkerParameters>(relaxed = true) {
            every { inputData } returns Data.EMPTY
            every { tags } returns emptySet()
        }

    private val mangaRepository = mockk<MangaRepository>(relaxed = true)
    private val trackRepository = mockk<TrackRepository>(relaxed = true)
    private val mangaDex = mockk<MangaDex>()
    private val libraryPreferences = mockk<LibraryPreferences>(relaxed = true)

    private val jobRunning = CompletableDeferred<Unit>()
    private val finishJob = CompletableDeferred<Unit>()
    private val libraryReads = AtomicInteger(0)

    private var libraryRows = emptyList<LibraryManga>()
    private var restrictions = emptySet<String>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(UnconfinedTestDispatcher())

        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        val running = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }
        every { workManager.getWorkInfosByTag(LibraryUpdateJob.TAG) } returns
            mockk<ListenableFuture<List<WorkInfo>>> { every { get() } returns listOf(running) }

        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        coEvery { any<CoroutineWorker>().tryToSetForeground() } just runs
        mockkConstructor(LibraryUpdateNotifier::class)
        every {
            anyConstructed<LibraryUpdateNotifier>().showProgressNotification(any(), any(), any())
        } just runs
        every { anyConstructed<LibraryUpdateNotifier>().cancelProgressNotification() } just runs
        mockkObject(Notifications)
        every { Notifications.isNotificationChannelEnabled(any(), any()) } returns false

        every { libraryPreferences.autoUpdateMangaRestrictions().get() } answers { restrictions }
        every { libraryPreferences.whichCategoriesToUpdate().get() } returns emptySet()
        every { libraryPreferences.whichCategoriesToExclude().get() } returns emptySet()
        every { libraryPreferences.skipMangaMetadataDuringUpdate().get() } returns false
        every { libraryPreferences.updateCovers().get() } returns false
        every { libraryPreferences.prioritizeLibraryUpdates().get() } returns false

        val mangaDexPreferences =
            mockk<MangaDexPreferences>(relaxed = true) {
                every { blockedGroups().get() } returns emptySet()
                every { blockedUploaders().get() } returns emptySet()
                every { readingSync().get() } returns false
            }
        val preferences =
            mockk<PreferencesHelper>(relaxed = true) {
                every { downloadNewChapters().get() } returns false
            }
        val sourceManager =
            mockk<SourceManager> {
                every { mangaDex } returns this@LibraryUpdateJobQueueTest.mangaDex
            }
        val mergeMangaRepository =
            mockk<MergeMangaRepository> {
                coEvery { getMergeMangaList(any<Long>()) } returns emptyList()
            }
        val trackManager = mockk<TrackManager>(relaxed = true)

        coEvery { trackRepository.getAllTracks() } returns emptyList()
        coEvery { trackRepository.getTracksForManga(any()) } returns emptyList()
        coEvery { mangaDex.fetchMangaAndChapterDetails(any(), any()) } returns
            Ok(
                MangaDetailChapterInformation(
                    // A new title from the site would rename the manga's folders
                    SManga.create().apply { title = "" },
                    sChapters = emptyList(),
                )
            )

        // The job's own read at the start of doWork waits here, with the job already running. The
        // read for the refreshed section comes after it.
        coEvery { mangaRepository.getLibraryList() } coAnswers
            {
                if (libraryReads.getAndIncrement() == 0) {
                    jobRunning.complete(Unit)
                    finishJob.await()
                    emptyList()
                } else {
                    libraryRows
                }
            }

        Injekt.addSingleton(mangaRepository)
        Injekt.addSingleton(trackRepository)
        Injekt.addSingleton(libraryPreferences)
        Injekt.addSingleton(mangaDexPreferences)
        Injekt.addSingleton(preferences)
        Injekt.addSingleton(sourceManager)
        Injekt.addSingleton(mergeMangaRepository)
        Injekt.addSingleton(trackManager)
        Injekt.addSingleton(mockk<CategoryRepository>(relaxed = true))
        Injekt.addSingleton(mockk<MangaUseCases>(relaxed = true))
        Injekt.addSingleton(mockk<MangaDexLoginHelper>(relaxed = true))
        Injekt.addSingleton(ShouldUpdateMangaUseCase(trackManager))
    }

    @After
    fun tearDown() {
        finishJob.complete(Unit)
        unmockkAll()
        Dispatchers.resetMain()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `refreshing a section during an update writes its rows back with their stored fields`() =
        duringRunningUpdate {
            val row =
                libraryRow(2).apply {
                    source = MANGADEX_SOURCE
                    date_added = 1_790_013_813_030L
                    viewer_flags = 1
                    chapter_flags = 4353
                    follow_status = FollowStatus.COMPLETED
                    filtered_scanlators = "Group A"
                    filtered_language = "fr"
                    dynamic_cover = "https://example.org/cover.png"
                    user_title = "My title"
                }
            libraryRows = listOf(row)

            refreshSection(row)

            val written = slot<Manga>()
            coVerify(timeout = WAIT_MILLIS) { mangaRepository.updateManga(capture(written)) }
            assertEquals(MANGADEX_SOURCE, written.captured.source)
            assertEquals(1_790_013_813_030L, written.captured.date_added)
            assertEquals(1, written.captured.viewer_flags)
            assertEquals(4353, written.captured.chapter_flags)
            assertEquals(FollowStatus.COMPLETED, written.captured.follow_status)
            assertEquals("Group A", written.captured.filtered_scanlators)
            assertEquals("fr", written.captured.filtered_language)
            assertEquals("https://example.org/cover.png", written.captured.dynamic_cover)
            assertEquals("My title", written.captured.user_title)
        }

    @Test
    fun `refreshing a section during an update leaves the rest of the library alone`() =
        duringRunningUpdate {
            val outside = libraryRow(1)
            val inSection = libraryRow(2)
            libraryRows = listOf(outside, inSection)

            refreshSection(inSection)

            coVerify(timeout = WAIT_MILLIS) { mangaRepository.updateManga(match { it.id == 2L }) }
            coVerify(exactly = 0) {
                mangaDex.fetchMangaAndChapterDetails(match { it.url == outside.url }, any())
            }
        }

    @Test
    fun `refreshing a section during an update skips what the restrictions exclude`() =
        duringRunningUpdate {
            restrictions = setOf(LibraryPreferences.MANGA_NOT_COMPLETED)
            val completed = libraryRow(1).apply { status = SManga.COMPLETED }
            val ongoing = libraryRow(2).apply { status = SManga.ONGOING }
            libraryRows = listOf(completed, ongoing)

            refreshSection(completed, ongoing)

            coVerify(timeout = WAIT_MILLIS) { mangaRepository.updateManga(match { it.id == 2L }) }
            coVerify(exactly = 0) {
                mangaDex.fetchMangaAndChapterDetails(match { it.url == completed.url }, any())
            }
        }

    @Test
    fun `refreshing a section with no update running starts a job for just its rows`() =
        runBlocking<Unit> {
            every { workManager.getWorkInfosByTag(LibraryUpdateJob.TAG) } returns
                mockk<ListenableFuture<List<WorkInfo>>> { every { get() } returns emptyList() }

            refreshSection(libraryRow(2), libraryRow(3))

            verify { libraryPreferences.libraryUpdateIds().set("2||3") }
            val request = slot<OneTimeWorkRequest>()
            verify { workManager.enqueueUniqueWork(any<String>(), any(), capture(request)) }
            val input = request.captured.workSpec.input
            assertTrue(input.getBoolean(LibraryUpdateJob.KEY_MANGA_LIST, false))
            // doWork filters by this category when it is set, and a section is not a category
            assertEquals(-1, input.getInt(LibraryUpdateJob.KEY_CATEGORY, -1))
        }

    private fun duringRunningUpdate(block: suspend () -> Unit) = runBlocking {
        val job = LibraryUpdateJob(context, workerParams)
        val work = async(Dispatchers.IO) { job.doWork() }
        withTimeout(WAIT_MILLIS) { jobRunning.await() }
        try {
            block()
        } finally {
            finishJob.complete(Unit)
            withTimeout(WAIT_MILLIS) { work.await() }
        }
        Unit
    }

    private suspend fun refreshSection(vararg rows: LibraryManga) {
        LibraryUpdateJob.startNow(context, section, mangaIdsToUse = rows.mapNotNull { it.id })
    }

    private fun libraryRow(id: Long) =
        LibraryManga().apply {
            this.id = id
            url = "/title/00000000-0000-4000-8000-00000000000$id"
            title = "Title $id"
            favorite = true
            initialized = true
        }

    private companion object {
        const val WAIT_MILLIS = 20_000L
        const val MANGADEX_SOURCE = 2499283573021220255L
        val section = Category.create("Completed").apply { id = 3 }
    }
}
