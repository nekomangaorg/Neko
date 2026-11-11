package eu.kanade.tachiyomi.util.manga

import eu.kanade.tachiyomi.data.database.models.MangaImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nekomanga.data.database.dao.LibraryDao
import org.nekomanga.data.database.dao.MangaDao
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.site.MangaDexPreferences

class SourceMangaToDisplayMangaTest {

    /** The manga table. The dao answers from it the way its queries would. */
    private val table = mutableListOf<MangaEntity>()

    /** When true, url lookups answer newest first instead of in url index order. */
    private var newestFirst = false

    private val mangaDao: MangaDao = mockk {
        coEvery { getMangaByUrlAndSourceSync(any(), any()) } answers
            {
                table
                    .filter { it.url == firstArg<String>() && it.source == secondArg<Long>() }
                    .minByOrNull { it.id }
            }
        // SQLite answers `url IN (...)` through the url index: by url, then by id
        coEvery { getMangaByUrls(any()) } answers
            {
                val rows = table.filter { it.url in firstArg<List<String>>() }
                if (newestFirst) rows.sortedByDescending { it.id }
                else rows.sortedWith(compareBy({ it.url }, { it.id }))
            }
        coEvery { insertManga(any()) } answers
            {
                table += firstArg<MangaEntity>().copy(id = NEW_ROW_ID)
                NEW_ROW_ID
            }
        coEvery { insertMangaList(any()) } answers
            {
                firstArg<List<MangaEntity>>().mapIndexed { index, row ->
                    val id = NEW_ROW_ID + index
                    table += row.copy(id = id)
                    id
                }
            }
        coEvery { updateManga(any()) } answers { replace(firstArg()) }
        coEvery { updateMangaList(any()) } answers
            {
                firstArg<List<MangaEntity>>().forEach { replace(it) }
            }
    }

    private val mangaRepository =
        MangaRepositoryImpl(
            libraryDao = mockk<LibraryDao>(),
            mangaDao = mangaDao,
            mangaDexPreferences = mockk<MangaDexPreferences>(),
            libraryPreferences = mockk<LibraryPreferences>(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    @Test
    fun `deep link opens a damaged library entry instead of adding a copy`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)

        val opened = sourceManga().toDisplayManga(mangaRepository, MANGADEX_SOURCE)

        assertEquals(1L, opened.mangaId)
        assertEquals(listOf(Triple(1L, UNSET_SOURCE, true)), rows())
    }

    @Test
    fun `deep link opens the library entry when a newer copy exists`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = false)

        assertEquals(1L, sourceManga().toDisplayManga(mangaRepository, MANGADEX_SOURCE).mangaId)
        assertEquals(2, table.size)
    }

    @Test
    fun `deep link opens the oldest library entry when both rows are in the library`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = true)

        assertEquals(
            1L,
            sourceManga().toDisplayManga(mangaRepository, MANGADEX_SOURCE).mangaId,
        )
    }

    @Test
    fun `deep link opens the oldest library entry whatever order the rows come back in`() =
        runTest {
            newestFirst = true
            table += row(id = 1, source = UNSET_SOURCE, favorite = true)
            table += row(id = 2, source = MANGADEX_SOURCE, favorite = true)

            assertEquals(
                1L,
                sourceManga().toDisplayManga(mangaRepository, MANGADEX_SOURCE).mangaId,
            )
        }

    @Test
    fun `deep link opens the library entry when it is the newer row`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = false)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = true)

        assertEquals(2L, sourceManga().toDisplayManga(mangaRepository, MANGADEX_SOURCE).mangaId)
    }

    @Test
    fun `deep link opens the oldest row when no row is in the library`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = false)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = false)

        assertEquals(1L, sourceManga().toDisplayManga(mangaRepository, MANGADEX_SOURCE).mangaId)
    }

    @Test
    fun `deep link finds a row stored with the other url form`() = runTest {
        table += row(id = 1, source = MANGADEX_SOURCE, favorite = true, url = MANGA_URL)

        assertEquals(1L, sourceManga().toDisplayManga(mangaRepository, MANGADEX_SOURCE).mangaId)
        assertEquals(1, table.size)
    }

    @Test
    fun `deep link to a title with no row adds one with the MangaDex source`() = runTest {
        val opened = sourceManga().toDisplayManga(mangaRepository, MANGADEX_SOURCE)

        assertEquals(NEW_ROW_ID, opened.mangaId)
        assertEquals(listOf(Triple(NEW_ROW_ID, MANGADEX_SOURCE, false)), rows())
    }

    @Test
    fun `browse list shows the library entry when a newer copy exists`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = false)

        assertEquals(listOf(1L), browse())
    }

    @Test
    fun `browse list picks the same row whatever order the rows come back in`() = runTest {
        newestFirst = true
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = false)

        assertEquals(listOf(1L), browse())
    }

    @Test
    fun `browse list shows the oldest library entry when both rows are in the library`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)
        table += row(id = 2, source = MANGADEX_SOURCE, favorite = true)

        assertEquals(listOf(1L), browse())
    }

    @Test
    fun `browse list shows a damaged library entry and adds nothing`() = runTest {
        table += row(id = 1, source = UNSET_SOURCE, favorite = true)

        assertEquals(listOf(1L), browse())
        assertEquals(1, table.size)
    }

    private fun sourceManga() = SourceManga(currentThumbnail = "", url = TITLE_URL, title = "Title")

    private suspend fun browse(): List<Long> =
        listOf(sourceManga()).toDisplayManga(mangaRepository, MANGADEX_SOURCE).map { it.mangaId }

    private fun rows() = table.sortedBy { it.id }.map { Triple(it.id, it.source, it.favorite) }

    private fun replace(row: MangaEntity) {
        table[table.indexOfFirst { it.id == row.id }] = row
    }

    private fun row(id: Long, source: Long, favorite: Boolean, url: String = TITLE_URL) =
        MangaImpl()
            .apply {
                this.id = id
                this.source = source
                this.url = url
                this.title = "Title"
                this.favorite = favorite
            }
            .toEntity()

    private companion object {
        const val MANGADEX_SOURCE = 2499283573021220255L
        const val UNSET_SOURCE = -1L
        const val NEW_ROW_ID = 1000L
        const val TITLE_URL = "/title/00000000-0000-4000-8000-000000000001"
        const val MANGA_URL = "/manga/00000000-0000-4000-8000-000000000001"
    }
}
