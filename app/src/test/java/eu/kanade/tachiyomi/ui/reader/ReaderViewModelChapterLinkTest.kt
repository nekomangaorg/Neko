package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.room.withTransaction
import com.github.michaelbull.result.Ok
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.MangaDetailChapterInformation
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.MangaDex
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.constants.MdConstants
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
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
class ReaderViewModelChapterLinkTest {

    private val appDatabase: AppDatabase = mockk()
    private val chapterRepository: ChapterRepository = mockk()
    private val mangaRepository: MangaRepository = mockk(relaxed = true)
    private val mangaDex: MangaDex = mockk()

    private val manga = Manga.create("/title/$MANGA_UUID", "Title").apply { id = MANGA_ID }

    private val chaptersInDatabase = mutableListOf<Chapter>()
    private val deleted = mutableListOf<Chapter>()
    private val updated = mutableListOf<Chapter>()

    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        Injekt = InjektScope(DefaultRegistrar())

        val sourceManager: SourceManager = mockk()
        every { sourceManager.mangaDex } returns mangaDex
        val downloadManager: DownloadManager = mockk(relaxed = true)
        val libraryPreferences: LibraryPreferences = mockk()
        every { libraryPreferences.enableLocalChapters() } returns preference(false)
        val mangaDexPreferences: MangaDexPreferences = mockk()
        every { mangaDexPreferences.readingSync() } returns preference(false)
        val storageManager: StorageManager = mockk(relaxed = true)
        Injekt.addSingleton(sourceManager)
        Injekt.addSingleton(downloadManager)
        Injekt.addSingleton(libraryPreferences)
        Injekt.addSingleton(mangaDexPreferences)
        Injekt.addSingleton(storageManager)

        mockkStatic("androidx.room.RoomDatabaseKt")
        val transaction = slot<suspend () -> Unit>()
        coEvery { appDatabase.withTransaction(capture(transaction)) } coAnswers
            {
                transaction.captured.invoke()
            }

        coEvery { chapterRepository.getChapterByUrl(any()) } answers
            {
                chaptersInDatabase.firstOrNull { it.url == firstArg<String>() }
            }
        coEvery { chapterRepository.getChaptersForManga(MANGA_ID) } answers
            {
                chaptersInDatabase.toList()
            }
        coEvery { chapterRepository.insertChapters(any()) } answers
            {
                val inserted = firstArg<List<Chapter>>()
                chaptersInDatabase.addAll(inserted)
                inserted.indices.map { it + 100L }
            }
        coEvery { chapterRepository.deleteChapters(any()) } answers
            {
                deleted.addAll(firstArg<List<Chapter>>())
            }
        coEvery { chapterRepository.updateChapters(any()) } answers
            {
                updated.addAll(firstArg<List<Chapter>>())
            }

        coEvery { mangaRepository.getMangaByUrl(manga.url) } returns manga
        // Stops the reader init right after the sync, this test is about what the sync did.
        coEvery { mangaRepository.getMangaById(MANGA_ID) } returns null

        coEvery { mangaDex.getMangaIdFromChapterId(LINKED_UUID) } returns MANGA_UUID

        val readerPreferences: ReaderPreferences = mockk(relaxed = true)
        every { readerPreferences.defaultReadingMode() } returns preference(0)

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
                preferences = mockk(relaxed = true),
                mangaDexPreferences = mangaDexPreferences,
                readerPreferences = readerPreferences,
                securityPreferences = mockk(relaxed = true),
                chapterItemFilter = mockk(relaxed = true),
                storageManager = storageManager,
                mangaDetailsPreferences = mockk(relaxed = true),
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `a chapter link that syncs keeps the merged chapters of the manga`() = runTest {
        val onMangaDex = mangaDexChapter(id = 10L, uuid = STORED_UUID, number = "1")
        chaptersInDatabase += onMangaDex
        chaptersInDatabase += mergedChapter(id = 11L, number = "1.5")
        mangaDexReturns(onMangaDex, mangaDexChapter(id = 0L, uuid = LINKED_UUID, number = "2"))

        viewModel.loadChapterURL(LINKED_UUID)

        assertEquals(emptyList<Long?>(), deleted.map { it.id })
    }

    @Test
    fun `a merged chapter a chapter link keeps stays read and sorts among the fetched chapters`() =
        runTest {
            val onMangaDex = mangaDexChapter(id = 10L, uuid = STORED_UUID, number = "1")
            chaptersInDatabase += onMangaDex
            chaptersInDatabase +=
                mergedChapter(id = 11L, number = "1.5").apply {
                    read = true
                    smart_order = 0
                }
            mangaDexReturns(onMangaDex, mangaDexChapter(id = 0L, uuid = LINKED_UUID, number = "2"))

            viewModel.loadChapterURL(LINKED_UUID)

            // Newest first: Ch.2 is 0, the merged Ch.1.5 is 1, Ch.1 is 2.
            val merged = updated.single { it.id == 11L }
            assertEquals(1, merged.smart_order)
            assertTrue(merged.read)
        }

    @Test
    fun `a chapter link still deletes a MangaDex chapter missing from the fetched list`() =
        runTest {
            val onMangaDex = mangaDexChapter(id = 10L, uuid = STORED_UUID, number = "1")
            chaptersInDatabase += onMangaDex
            chaptersInDatabase += mangaDexChapter(id = 12L, uuid = REMOVED_UUID, number = "1.5")
            mangaDexReturns(onMangaDex, mangaDexChapter(id = 0L, uuid = LINKED_UUID, number = "2"))

            viewModel.loadChapterURL(LINKED_UUID)

            assertEquals(listOf<Long?>(12L), deleted.map { it.id })
        }

    private fun mangaDexReturns(vararg chapters: Chapter) {
        val site =
            SManga.create().apply {
                url = manga.url
                title = manga.title
            }
        coEvery { mangaDex.fetchMangaAndChapterDetails(any(), false) } returns
            Ok(
                MangaDetailChapterInformation(
                    sManga = site,
                    sChapters = chapters.map { SChapter.create().apply { copyFrom(it) } },
                )
            )
    }

    private fun mangaDexChapter(id: Long, uuid: String, number: String): Chapter =
        Chapter.create().apply {
            this.id = id
            manga_id = MANGA_ID
            url = MdConstants.chapterSuffix + uuid
            name = "Ch.$number"
            chapter_txt = "Ch.$number"
            mangadex_chapter_id = uuid
        }

    private fun mergedChapter(id: Long, number: String): Chapter =
        Chapter.create().apply {
            this.id = id
            manga_id = MANGA_ID
            url = "/merged/$id"
            name = "Ch.$number"
            chapter_txt = "Ch.$number"
            scanlator = MergeType.getMergeTypeName(MergeType.WeebCentral)
        }

    private fun <T : Any> preference(value: T): Preference<T> = mockk {
        every { get() } returns value
    }

    companion object {
        private const val MANGA_ID = 1L
        private const val MANGA_UUID = "7b1f3c2a-4d5e-4f60-8a1b-2c3d4e5f6a7b"
        private const val LINKED_UUID = "2fec6641-beb1-4ae9-a32f-204dd06688ac"
        private const val STORED_UUID = "9d3c5a1e-6b7f-4c2d-8e0a-1f2b3c4d5e6f"
        private const val REMOVED_UUID = "5e6f7a8b-9c0d-4e1f-a2b3-c4d5e6f7a8b9"
    }
}
