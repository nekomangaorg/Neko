package eu.kanade.tachiyomi.util.chapter

import androidx.room.withTransaction
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isLocalSource
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.site.MangaDexPreferences
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class ChapterSourceSyncTest {

    private val appDatabase: AppDatabase = mockk()
    private val chapterRepository: ChapterRepository = mockk()
    private val mangaRepository: MangaRepository = mockk(relaxed = true)
    private val downloadManager: DownloadManager = mockk(relaxed = true)

    private val manga = Manga.create("/title/1", "Same Title").apply { id = 1L }
    private val ownChapter = mangaDexChapter(id = 10L, mangaId = 1L, uuid = OWN_UUID)

    // Manga 2 has the same title, so its downloads sit in the same folder as the ones of manga 1.
    private val chaptersInDatabase =
        mutableListOf(
            ownChapter,
            mangaDexChapter(id = 20L, mangaId = 2L, uuid = OTHER_UUID),
            mangaDexChapter(id = 21L, mangaId = 2L, uuid = SECOND_OTHER_UUID),
        )

    private val urlLookups = mutableListOf<List<String>>()
    private val insertedBatches = mutableListOf<List<Chapter>>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        val libraryPreferences: LibraryPreferences = mockk()
        val mangaDexPreferences: MangaDexPreferences = mockk()
        val localChaptersEnabled = booleanPreference(true)
        val readingSyncDisabled = booleanPreference(false)
        every { libraryPreferences.enableLocalChapters() } returns localChaptersEnabled
        every { mangaDexPreferences.readingSync() } returns readingSyncDisabled
        Injekt.addSingleton(downloadManager)
        Injekt.addSingleton(libraryPreferences)
        Injekt.addSingleton(mangaDexPreferences)

        mockkStatic("androidx.room.RoomDatabaseKt")
        val transaction = slot<suspend () -> Unit>()
        coEvery { appDatabase.withTransaction(capture(transaction)) } coAnswers
            {
                transaction.captured.invoke()
            }

        coEvery { chapterRepository.getChaptersForManga(1L) } answers
            {
                chaptersInDatabase.filter { it.manga_id == 1L }
            }
        coEvery { chapterRepository.getChaptersByUrls(capture(urlLookups)) } answers
            {
                val urls = firstArg<List<String>>()
                chaptersInDatabase.filter { it.url in urls }
            }
        coEvery { chapterRepository.insertChapters(capture(insertedBatches)) } answers
            {
                firstArg<List<Chapter>>().indices.map { it + 100L }
            }
        coEvery { chapterRepository.deleteChapter(any()) } just Runs
        coEvery { chapterRepository.deleteChapters(any()) } just Runs
        coEvery { chapterRepository.updateChapters(any()) } just Runs

        every { downloadManager.getAllDownloads(manga) } returns emptyList()
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `chapterUuid reads the uuid from a cbz name`() {
        assertEquals(OWN_UUID, chapterUuid("Ch.1 - $OWN_UUID.cbz"))
        assertEquals(OWN_UUID, chapterUuid("Local_Ch.1 - $OWN_UUID.cbz"))
    }

    @Test
    fun `chapterUuid reads the uuid from a folder name that contains a dot`() {
        assertEquals(OWN_UUID, chapterUuid("Vol.2 Ch.1 - $OWN_UUID"))
        assertEquals(OWN_UUID, chapterUuid("Local_Ch.1 - $OWN_UUID"))
    }

    @Test
    fun `chapterUuid ignores whitespace around the uuid`() {
        assertEquals(OWN_UUID, chapterUuid("Ch.1 - $OWN_UUID .cbz"))
    }

    @Test
    fun `chapterUuid returns null for a name without a uuid`() {
        assertNull(chapterUuid("Ch.1"))
        assertNull(chapterUuid("Ch.1 - Scanlator.cbz"))
        assertNull(chapterUuid(".nomedia"))
    }

    @Test
    fun `otherMangaChapterUuids returns only the uuids another manga stores`() = runTest {
        val uuids =
            otherMangaChapterUuids(
                listOf(OWN_UUID, OTHER_UUID, ORPHAN_UUID),
                mangaId = 1L,
                chapterRepository = chapterRepository,
            )

        assertEquals(setOf(OTHER_UUID), uuids)
    }

    @Test
    fun `otherMangaChapterUuids skips the query when there is nothing to look up`() = runTest {
        val uuids =
            otherMangaChapterUuids(
                emptyList(),
                mangaId = 1L,
                chapterRepository = chapterRepository,
            )

        assertEquals(emptySet<String>(), uuids)
        coVerify(exactly = 0) { chapterRepository.getChaptersByUrls(any()) }
    }

    @Test
    fun `sync removes the Local_ prefix from a cbz of another manga`() = runTest {
        val file = download("Local_Ch.1 - $OTHER_UUID.cbz")
        every { downloadManager.getAllDownloads(manga) } returns listOf(file)

        syncChapters()

        verify { file.renameTo("Ch.1 - $OTHER_UUID.cbz") }
        assertEquals(emptyList<String>(), localChaptersInserted())
    }

    @Test
    fun `sync leaves a chapter folder of another manga untouched`() = runTest {
        val folder = download("Ch.1 - $OTHER_UUID")
        every { downloadManager.getAllDownloads(manga) } returns listOf(folder)

        syncChapters()

        verify(exactly = 0) { folder.renameTo(any()) }
        assertEquals(emptyList<String>(), localChaptersInserted())
    }

    @Test
    fun `sync removes the Local_ prefix from a chapter folder of this manga`() = runTest {
        val folder = download("Local_Ch.1 - $OWN_UUID")
        every { downloadManager.getAllDownloads(manga) } returns listOf(folder)

        syncChapters()

        verify { folder.renameTo("Ch.1 - $OWN_UUID") }
        assertEquals(emptyList<String>(), localChaptersInserted())
    }

    @Test
    fun `sync turns a download no manga stores into a local chapter`() = runTest {
        val file = download("Ch.3 - $ORPHAN_UUID.cbz")
        every { downloadManager.getAllDownloads(manga) } returns listOf(file)

        syncChapters()

        verify { file.renameTo("Local_Ch.3 - $ORPHAN_UUID.cbz") }
        assertEquals(listOf("Ch.3 - $ORPHAN_UUID"), localChaptersInserted())
    }

    @Test
    fun `sync looks up every download in the folder with one query`() = runTest {
        val downloads =
            listOf(
                download("Ch.1 - $OTHER_UUID.cbz"),
                download("Ch.2 - $SECOND_OTHER_UUID.cbz"),
                download("Ch.3 - $ORPHAN_UUID.cbz"),
            )
        every { downloadManager.getAllDownloads(manga) } returns downloads

        syncChapters()

        assertEquals(1, urlLookups.size)
        assertEquals(
            listOf(OTHER_UUID, SECOND_OTHER_UUID, ORPHAN_UUID)
                .map { MdConstants.chapterSuffix + it }
                .toSet(),
            urlLookups.single().toSet(),
        )
    }

    @Test
    fun `sync deletes a local chapter that holds a chapter of another manga`() = runTest {
        val localChapter =
            Chapter.create().apply {
                id = 11L
                manga_id = 1L
                url = "${Constants.LOCAL_SOURCE}/Local_Ch.1 - $OTHER_UUID.cbz"
                name = "Ch.1 - $OTHER_UUID"
                scanlator = Constants.LOCAL_SOURCE
                isUnavailable = true
            }
        chaptersInDatabase.add(localChapter)

        syncChapters()

        coVerify { chapterRepository.deleteChapter(localChapter) }
    }

    private suspend fun syncChapters() {
        syncChaptersWithSource(
            appDatabase,
            chapterRepository,
            mangaRepository,
            rawSourceChapters = listOf(SChapter.create().apply { copyFrom(ownChapter) }),
            manga = manga,
        )
    }

    private fun localChaptersInserted(): List<String> =
        insertedBatches.flatten().filter { it.isLocalSource() }.map { it.name }

    private fun download(fileName: String): UniFile =
        mockk(relaxed = true) { every { this@mockk.name } returns fileName }

    private fun booleanPreference(value: Boolean): Preference<Boolean> = mockk {
        every { get() } returns value
    }

    private fun mangaDexChapter(id: Long, mangaId: Long, uuid: String): Chapter =
        Chapter.create().apply {
            this.id = id
            manga_id = mangaId
            url = MdConstants.chapterSuffix + uuid
            name = "Ch.1"
            chapter_txt = "Ch.1"
            mangadex_chapter_id = uuid
        }

    companion object {
        private const val OWN_UUID = "2fec6641-beb1-4ae9-a32f-204dd06688ac"
        private const val OTHER_UUID = "9d3c5a1e-6b7f-4c2d-8e0a-1f2b3c4d5e6f"
        private const val SECOND_OTHER_UUID = "0a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
        private const val ORPHAN_UUID = "5e6f7a8b-9c0d-4e1f-a2b3-c4d5e6f7a8b9"
    }
}
