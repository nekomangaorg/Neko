package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadCacheTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var provider: DownloadProvider
    private lateinit var sourceManager: SourceManager
    private lateinit var storageManager: StorageManager
    private lateinit var mangaRepository: MangaRepository

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(testDispatcher)

        provider = mockk(relaxed = true)
        sourceManager = mockk(relaxed = true)
        storageManager = mockk(relaxed = true)
        mangaRepository = mockk(relaxed = true)

        every { storageManager.baseDirChanges } returns MutableSharedFlow()
        every { storageManager.getDownloadsDirectory() } returns null

        Injekt.addSingleton(storageManager)
        Injekt.addSingleton(mangaRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `isReindexing prevents renew from executing`() =
        runTest(testDispatcher) {
            val cache = DownloadCache(provider, sourceManager, storageManager)
            cache.isReindexing.set(true)

            cache.forceRenewCache()

            assertTrue(cache.isReindexing.get())
            cache.scope.cancel()
        }

    @Test
    fun `cancelRenewJob completes non-cancellably even when caller coroutine is cancelled`() =
        runTest(testDispatcher) {
            val cache = DownloadCache(provider, sourceManager, storageManager)

            val started = CompletableDeferred<Unit>()
            val jobCancelled = CompletableDeferred<Boolean>()
            val callerJob =
                launch(Dispatchers.Default) {
                    started.complete(Unit)
                    try {
                        awaitCancellation()
                    } finally {
                        try {
                            cache.cancelRenewJob()
                            jobCancelled.complete(true)
                        } catch (e: Exception) {
                            jobCancelled.complete(false)
                        }
                    }
                }

            started.await()
            callerJob.cancelAndJoin()

            val completed = jobCancelled.await()
            assertTrue(completed)
            cache.scope.cancel()
        }

    @Test
    fun `cancelRenewJob when no renewJob exists completes cleanly`() =
        runTest(testDispatcher) {
            val cache = DownloadCache(provider, sourceManager, storageManager)

            cache.cancelRenewJob()

            cache.scope.cancel()
        }

    @Test
    fun `renewCache without downloads directory invokes progress callback with zeros`() =
        runTest(testDispatcher) {
            val cache = DownloadCache(provider, sourceManager, storageManager)
            every { storageManager.getDownloadsDirectory() } returns null

            var progressCalled = false
            cache.renewCache { progress, total, title ->
                assertEquals(0, progress)
                assertEquals(0, total)
                assertNull(title)
                progressCalled = true
            }

            assertTrue(progressCalled)
            cache.scope.cancel()
        }

    @Test
    fun `renewCache without source directory invokes progress callback with zeros`() =
        runTest(testDispatcher) {
            val cache = DownloadCache(provider, sourceManager, storageManager)
            val downloadsDir: UniFile = mockk(relaxed = true)
            every { downloadsDir.listFiles() } returns emptyArray()
            every { storageManager.getDownloadsDirectory() } returns downloadsDir
            every { provider.getSourceDirName() } returns "MangaDex"

            var progressCalled = false
            cache.renewCache { progress, total, title ->
                assertEquals(0, progress)
                assertEquals(0, total)
                assertNull(title)
                progressCalled = true
            }

            assertTrue(progressCalled)
            cache.scope.cancel()
        }

    @Test
    fun `renewCache populates cache and emits changes flow`() =
        runTest(testDispatcher) {
            val mangaId = 123L
            val manga = Manga.create("/manga/123", "Sample Manga").apply { id = mangaId }
            coEvery { mangaRepository.getMangaList() } returns listOf(manga)

            val rootDownloadsDir = mockk<UniFile>(relaxed = true)
            val sourceDir = mockk<UniFile>(relaxed = true)
            val mangaDir = mockk<UniFile>(relaxed = true)
            val chapterFile1 = mockk<UniFile>(relaxed = true)
            val chapterFile2 = mockk<UniFile>(relaxed = true)

            every { provider.getSourceDirName() } returns "MangaDex"
            every { rootDownloadsDir.listFiles() } returns arrayOf(sourceDir)
            every { sourceDir.name } returns "MangaDex"
            every { sourceDir.listFiles() } returns arrayOf(mangaDir)
            every { mangaDir.name } returns "Sample Manga"
            every { mangaDir.isDirectory } returns true
            every { chapterFile1.name } returns "Ch. 1 - 01234567-89ab-cdef-0123-456789abcdef.cbz"
            every { chapterFile2.name } returns "Ch. 2.cbz"
            every { mangaDir.listFiles() } returns arrayOf(chapterFile1, chapterFile2)
            every { storageManager.getDownloadsDirectory() } returns rootDownloadsDir

            val cache = DownloadCache(provider, sourceManager, storageManager)
            cache.cancelRenewJob()

            val changesEmitted = CompletableDeferred<Unit>()
            val changesJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    cache.changes.collect { changesEmitted.complete(Unit) }
                }

            val progressUpdates = mutableListOf<Int>()
            cache.renewCache { progress, total, _ -> progressUpdates.add(progress) }
            testScheduler.advanceUntilIdle()

            assertEquals(2, cache.getDownloadCount(manga))
            assertTrue(progressUpdates.isNotEmpty())
            assertTrue(changesEmitted.isCompleted)

            changesJob.cancel()
            cache.scope.cancel()
        }

    @Test
    fun `isChapterDownloaded with skipCache delegates directly to provider findChapterDir`() {
        val cache = DownloadCache(provider, sourceManager, storageManager)
        val chapter: Chapter = mockk(relaxed = true)
        val manga: Manga = mockk(relaxed = true)
        val mockChapterDir = mockk<UniFile>()

        every { provider.findChapterDir(chapter, manga) } returns mockChapterDir

        val result = cache.isChapterDownloaded(chapter, manga, skipCache = true)

        assertTrue(result)
        verify(exactly = 1) { provider.findChapterDir(chapter, manga) }
        cache.scope.cancel()
    }

    @Test
    fun `isChapterDownloaded with skipCache returns false when chapter directory not found`() {
        val cache = DownloadCache(provider, sourceManager, storageManager)
        val chapter: Chapter = mockk(relaxed = true)
        val manga: Manga = mockk(relaxed = true)

        every { provider.findChapterDir(chapter, manga) } returns null

        val result = cache.isChapterDownloaded(chapter, manga, skipCache = true)

        assertFalse(result)
        verify(exactly = 1) { provider.findChapterDir(chapter, manga) }
        cache.scope.cancel()
    }
}
