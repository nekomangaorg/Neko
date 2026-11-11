package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupMergeManga
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.notificationManager
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
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.MergeMangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/**
 * Library rows can hold source -1, the value a manga has before anything sets it. Backups list
 * those rows like any other, and Pull follows adds a second row for the same title next to them.
 */
class BackupRestorerTest {

    private val context = mockk<Context>(relaxed = true)
    private val uri = mockk<Uri>()
    private val appDatabase = mockk<AppDatabase>()
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val chapterRepository = mockk<ChapterRepository>(relaxed = true)
    private val historyRepository = mockk<HistoryRepository>(relaxed = true)
    private val mangaRepository = mockk<MangaRepository>(relaxed = true)
    private val mergeMangaRepository = mockk<MergeMangaRepository>(relaxed = true)
    private val trackRepository = mockk<TrackRepository>(relaxed = true)
    private val sourceManager = mockk<SourceManager>()

    private val insertedManga = mutableListOf<Manga>()
    private val updatedManga = mutableListOf<Manga>()
    private val insertedChapters = mutableListOf<Chapter>()
    private val insertedMerges = mutableListOf<MergeMangaImpl>()
    private var databaseRows = emptyList<Manga>()
    private var storedMerges = emptyList<MergeMangaImpl>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())

        mockkObject(BackupUtil)
        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        every { any<Context>().notificationManager } returns mockk(relaxed = true)
        mockkConstructor(RestoreHelper::class)
        every { anyConstructed<RestoreHelper>().showProgressNotification(any(), any(), any()) } just
            runs
        every {
            anyConstructed<RestoreHelper>()
                .showResultNotification(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
        } just runs
        every { anyConstructed<RestoreHelper>().writeErrorLog(any(), any(), any()) } returns null

        mockkStatic("androidx.room.RoomDatabaseKt")
        val transaction = slot<suspend () -> Unit>()
        coEvery { appDatabase.withTransaction(capture(transaction)) } coAnswers
            {
                transaction.captured.invoke()
            }

        every { sourceManager.isMangadex(any()) } answers { callOriginal() }
        every { sourceManager.isMangadexOrUnset(any(), any()) } answers { callOriginal() }
        coEvery { categoryRepository.getCategories() } returns emptyList()
        coEvery { chapterRepository.getChaptersForMangaIds(any()) } returns emptyList()
        coEvery { historyRepository.getHistoryByMangaIds(any()) } returns emptyList()
        coEvery { mergeMangaRepository.getMergeMangaList(any<List<Long>>()) } answers
            {
                val mangaIds = firstArg<List<Long>>()
                storedMerges.filter { it.mangaId in mangaIds }
            }
        coEvery { trackRepository.getTracksForMangaByIds(any()) } returns emptyList()
        coEvery { mangaRepository.getMangaList() } answers { databaseRows }
        coEvery { mangaRepository.insertManga(capture(insertedManga)) } answers
            {
                100L + insertedManga.size
            }
        coEvery { mangaRepository.updateManga(capture(updatedManga)) } just runs
        coEvery { chapterRepository.insertChapters(any()) } answers
            {
                val chapters = firstArg<List<Chapter>>()
                insertedChapters += chapters
                chapters.indices.map { it + 1_000L }
            }
        coEvery { mergeMangaRepository.insertMergeManga(capture(insertedMerges)) } returns 1L

        Injekt.addSingleton(appDatabase)
        Injekt.addSingleton(categoryRepository)
        Injekt.addSingleton(chapterRepository)
        Injekt.addSingleton(historyRepository)
        Injekt.addSingleton(mangaRepository)
        Injekt.addSingleton(mergeMangaRepository)
        Injekt.addSingleton(trackRepository)
        Injekt.addSingleton(sourceManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `row with source -1 and its copy restore as one manga with the data of both`() {
        restore(
            backupRow(
                source = -1L,
                merges = listOf(BackupMergeManga(url = "/toonily/a", mergeType = TOONILY)),
                chapters = listOf(chapter("/chapter/merged"), chapter("/chapter/shared")),
            ),
            backupRow(
                source = MANGADEX_SOURCE,
                chapters = listOf(chapter("/chapter/shared"), chapter("/chapter/new")),
            ),
        )

        assertEquals(listOf(MANGADEX_SOURCE), insertedManga.map { it.source })
        assertEquals(listOf("/toonily/a"), insertedMerges.map { it.url })
        assertEquals(
            setOf("/chapter/merged", "/chapter/shared", "/chapter/new"),
            insertedChapters.map { it.url }.toSet(),
        )
        assertEquals(3, insertedChapters.size)
        assertResult(restored = 1, restoreAmount = 1, skipped = 0, total = 1)
    }

    @Test
    fun `row with source -1 restores as a MangaDex manga`() {
        restore(backupRow(source = -1L))

        assertEquals(listOf(MANGADEX_SOURCE), insertedManga.map { it.source })
        assertResult(restored = 1, restoreAmount = 1, skipped = 0, total = 1)
    }

    @Test
    fun `restoring over a stored row with source -1 repairs it and adds no copy`() {
        databaseRows = listOf(storedRow(id = 7L, source = -1L))

        restore(backupRow(source = MANGADEX_SOURCE, dateAdded = 1_790_013_813_030L))

        assertEquals(emptyList<Manga>(), insertedManga)
        val updated = updatedManga.single()
        assertEquals(7L, updated.id)
        assertEquals(MANGADEX_SOURCE, updated.source)
        assertEquals(1_790_013_813_030L, updated.date_added)
    }

    @Test
    fun `restoring over a doubled title updates the row with the real source`() {
        databaseRows =
            listOf(storedRow(id = 8L, source = MANGADEX_SOURCE), storedRow(id = 9L, source = -1L))

        restore(backupRow(source = MANGADEX_SOURCE))

        assertEquals(emptyList<Manga>(), insertedManga)
        assertEquals(listOf(8L), updatedManga.map { it.id })
    }

    @Test
    fun `restoring over a stored title adds the Komga series it does not have`() {
        databaseRows = listOf(storedRow(id = 7L, source = MANGADEX_SOURCE))
        storedMerges =
            listOf(
                MergeMangaImpl(
                    id = 1L,
                    mangaId = 7L,
                    url = "/api/v1/series/0A",
                    mergeType = MergeType.Komga,
                ),
            )

        restore(
            backupRow(
                source = MANGADEX_SOURCE,
                merges =
                    listOf(
                        BackupMergeManga(url = "/api/v1/series/0A", mergeType = KOMGA),
                        BackupMergeManga(url = "/api/v1/series/0B", mergeType = KOMGA),
                    ),
            ),
        )

        assertEquals(
            listOf(Triple(7L, MergeType.Komga, "/api/v1/series/0B")),
            insertedMerges.map { Triple(it.mangaId, it.mergeType, it.url) },
        )
    }

    @Test
    fun `restoring over a stored title does not add a second Toonily merge`() {
        databaseRows = listOf(storedRow(id = 7L, source = MANGADEX_SOURCE))
        storedMerges =
            listOf(
                MergeMangaImpl(
                    id = 1L,
                    mangaId = 7L,
                    url = "/toonily/a",
                    mergeType = MergeType.Toonily,
                ),
            )

        restore(
            backupRow(
                source = MANGADEX_SOURCE,
                merges = listOf(BackupMergeManga(url = "/toonily/b", mergeType = TOONILY)),
            ),
        )

        assertEquals(emptyList<MergeMangaImpl>(), insertedMerges)
    }

    @Test
    fun `row with source -1 and a url that is not MangaDex stays skipped`() {
        restore(backupRow(source = -1L, url = "/manga/12345"))

        assertEquals(emptyList<Manga>(), insertedManga)
        assertResult(restored = 0, restoreAmount = 0, skipped = 1, total = 1)
    }

    @Test
    fun `row from another source stays skipped`() {
        restore(backupRow(source = 12_345L))

        assertEquals(emptyList<Manga>(), insertedManga)
        assertResult(restored = 0, restoreAmount = 0, skipped = 1, total = 1)
    }

    private fun restore(vararg rows: BackupManga) = runBlocking {
        every { BackupUtil.decodeBackup(context, uri) } returns Backup(rows.toList())

        assertTrue(BackupRestorer(context, mockk(relaxed = true)).restoreBackup(uri))

        // A failed row only lands in this list, so nothing else would show it
        verify { anyConstructed<RestoreHelper>().writeErrorLog(emptyList(), any(), any()) }
    }

    private fun assertResult(restored: Int, restoreAmount: Int, skipped: Int, total: Int) {
        verify {
            anyConstructed<RestoreHelper>()
                .showResultNotification(
                    any(),
                    any(),
                    any(),
                    restored,
                    restoreAmount,
                    skipped,
                    total,
                    any(),
                    any(),
                    any(),
                )
        }
    }

    private fun backupRow(
        source: Long,
        url: String = "/manga/$UUID",
        dateAdded: Long = 0L,
        merges: List<BackupMergeManga> = emptyList(),
        chapters: List<BackupChapter> = emptyList(),
    ) =
        BackupManga(
            source = source,
            url = url,
            title = TITLE,
            dateAdded = dateAdded,
            chapters = chapters,
            mergeMangaList = merges,
        )

    private fun chapter(url: String) = BackupChapter(url = url, name = url)

    private fun storedRow(id: Long, source: Long) =
        Manga.create("/title/$UUID", TITLE, source).apply { this.id = id }

    private companion object {
        const val MANGADEX_SOURCE = 2499283573021220255L
        const val UUID = "00000000-0000-4000-8000-000000000001"
        const val TITLE = "Title"
        val TOONILY = MergeType.Toonily.id
        val KOMGA = MergeType.Komga.id
    }
}
