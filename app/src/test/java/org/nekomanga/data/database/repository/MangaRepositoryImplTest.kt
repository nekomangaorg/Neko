package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MangaImpl
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nekomanga.data.database.dao.LibraryDao
import org.nekomanga.data.database.dao.MangaDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.model.LibraryMangaRaw
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.library.ScanlatorFilterOption
import org.nekomanga.domain.site.MangaDexPreferences

class MangaRepositoryImplTest {

    // SQLite caps a statement at 999 bind variables (SQLITE_MAX_VARIABLE_NUMBER) on Android <= 10.
    // Exceeding it throws "too many SQL variables". The repository chunks well under this; the
    // tests
    // assert against the real crash threshold so they fail for any chunk size that would actually
    // crash, regardless of the exact margin chosen in production.
    private val sqliteMaxVariables = 999

    private val mangaDao: MangaDao = mockk()

    private val repository =
        MangaRepositoryImpl(
            libraryDao = mockk<LibraryDao>(),
            mangaDao = mangaDao,
            mangaDexPreferences = mockk<MangaDexPreferences>(),
            libraryPreferences = mockk<LibraryPreferences>(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    @Test
    fun `getMangaByUrls splits expanded url list so no query exceeds the SQLite variable limit`() =
        runTest {
            // Each "/title/..." url is expanded by the repository into url + "/manga/..." alt url,
            // so 900 inputs become 1800 bind variables in a single query unless chunked.
            val inputUrls = (1..900).map { "/title/uuid-$it" }
            val expectedExpanded = inputUrls.flatMap {
                listOf(it, it.replaceFirst("/title/", "/manga/"))
            }

            val capturedQueries = mutableListOf<List<String>>()
            coEvery { mangaDao.getMangaByUrls(capture(capturedQueries)) } returns emptyList()

            repository.getMangaByUrls(inputUrls)

            // No single DAO query may exceed the SQLite bind-variable limit.
            capturedQueries.forEach { query -> query shouldHaveAtMostSize sqliteMaxVariables }
            // Every url (and its alt url) must still be queried across the chunks - nothing
            // dropped.
            capturedQueries.flatten() shouldContainExactlyInAnyOrder expectedExpanded
        }

    @Test
    fun `getMangaByUrls deduplicates urls so the same manga is not queried twice`() = runTest {
        // Input contains both forms of the SAME manga. Without distinct(), the alt-url
        // expansion would query ["/title/uuid-1", "/manga/uuid-1", "/manga/uuid-1",
        // "/title/uuid-1"], doubling the bind variables and repeating the lookup.
        val inputUrls = listOf("/title/uuid-1", "/manga/uuid-1")

        val capturedQueries = mutableListOf<List<String>>()
        coEvery { mangaDao.getMangaByUrls(capture(capturedQueries)) } returns emptyList()

        repository.getMangaByUrls(inputUrls)

        capturedQueries.flatten() shouldContainExactlyInAnyOrder
            listOf("/title/uuid-1", "/manga/uuid-1")
    }

    @Test
    fun `observeLibrary gives a slow collector the latest library and skips the lists between`() =
        runTest {
            val libraryDao =
                mockk<LibraryDao> {
                    every { observeLibrary() } returns
                        flow {
                            emit(listOf(rawManga(1L)))
                            delay(10)
                            emit(listOf(rawManga(2L)))
                            delay(10)
                            emit(listOf(rawManga(3L)))
                        }
                }
            val mangaDexPreferences =
                mockk<MangaDexPreferences> {
                    every { blockedGroups().changes() } returns flowOf(emptySet())
                    every { blockedUploaders().changes() } returns flowOf(emptySet())
                }
            val libraryPreferences =
                mockk<LibraryPreferences> {
                    every { chapterScanlatorFilterOption().changes() } returns
                        flowOf(ScanlatorFilterOption.ANY)
                }
            val libraryRepository =
                MangaRepositoryImpl(
                    libraryDao = libraryDao,
                    mangaDao = mangaDao,
                    mangaDexPreferences = mangaDexPreferences,
                    libraryPreferences = libraryPreferences,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            val seen = libraryRepository.observeLibrary().onEach { delay(1_000) }.toList()

            assertEquals(listOf(1L, 3L), seen.map { it.single().id })
        }

    private fun rawManga(id: Long) =
        LibraryMangaRaw(
            manga =
                MangaImpl()
                    .apply {
                        this.id = id
                        url = "/title/uuid-$id"
                        title = "Title $id"
                    }
                    .toEntity(),
            unread = "",
            hasRead = "",
            bookmarkCount = 0,
            unavailableCount = 0,
            category = 0,
        )

    @Test
    fun `getMangaByIds splits id list so no query exceeds the SQLite variable limit`() = runTest {
        val inputIds = (1L..1500L).toList()

        val capturedQueries = mutableListOf<List<Long>>()
        coEvery { mangaDao.getMangaByIds(capture(capturedQueries)) } returns emptyList()

        repository.getMangaByIds(inputIds)

        capturedQueries.forEach { query -> query shouldHaveAtMostSize sqliteMaxVariables }
        capturedQueries.flatten() shouldContainExactlyInAnyOrder inputIds
    }
}
