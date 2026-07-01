package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.presentation.screens.library.LibrarySort
import tachiyomi.core.preference.Preference

class ToggleMangaFavoriteTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mangaRepository: MangaRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var libraryPreferences: LibraryPreferences
    private lateinit var updateMangaAggregate: UpdateMangaAggregate
    private lateinit var toggleMangaFavorite: ToggleMangaFavorite

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mangaRepository = mockk()
        categoryRepository = mockk()
        libraryPreferences = mockk()
        updateMangaAggregate = mockk()

        toggleMangaFavorite =
            ToggleMangaFavorite(
                mangaRepository = mangaRepository,
                categoryRepository = categoryRepository,
                libraryPreferences = libraryPreferences,
                updateMangaAggregate = updateMangaAggregate,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given non-existent manga id when toggling favorite then returns null`() = runTest {
        // Arrange
        val mangaId = 1L
        coEvery { mangaRepository.getMangaById(mangaId) } returns null

        // Act
        val result = toggleMangaFavorite(mangaId, emptyList()) { emptyList() }

        // Assert
        assertNull(result)
    }

    @Test
    fun `given favorite manga when toggling favorite then updates to unfavorite`() = runTest {
        // Arrange
        val mangaId = 1L
        val mockManga = mockk<Manga>(relaxed = true)
        every { mockManga.id } returns mangaId
        every { mockManga.favorite } returns true
        every { mockManga.url } returns "/manga/1"
        coEvery { mangaRepository.getMangaById(mangaId) } returns mockManga
        coEvery { mangaRepository.updateManga(mockManga) } just runs
        coEvery { updateMangaAggregate(mangaId, "/manga/1", false) } just runs

        // Act
        val result = toggleMangaFavorite(mangaId, emptyList()) { emptyList() }

        // Assert
        assertEquals(false, result)
        verify(exactly = 1) { mockManga.favorite = false }
        verify(exactly = 1) { mockManga.date_added = 0L }
        coVerify(exactly = 1) { mangaRepository.updateManga(mockManga) }
        coVerify(exactly = 1) { updateMangaAggregate(mangaId, "/manga/1", false) }
    }

    @Test
    fun `given unfavorite manga when toggling favorite and categoryItems is not empty then sets categories`() =
        runTest {
            // Arrange
            val mangaId = 1L
            val mockManga = mockk<Manga>(relaxed = true)
            every { mockManga.id } returns mangaId
            every { mockManga.favorite } returns false
            every { mockManga.url } returns "/manga/1"
            coEvery { mangaRepository.getMangaById(mangaId) } returns mockManga
            coEvery { mangaRepository.updateManga(mockManga) } just runs
            coEvery { updateMangaAggregate(mangaId, "/manga/1", true) } just runs

            val category =
                CategoryItem(
                    id = 2,
                    name = "Test Category",
                    sortOrder = LibrarySort.Title,
                    isAscending = true,
                )
            coEvery { categoryRepository.setMangaCategories(any(), listOf(mangaId)) } just runs

            // Act
            val result = toggleMangaFavorite(mangaId, listOf(category)) { emptyList() }

            // Assert
            assertEquals(true, result)
            verify(exactly = 1) { mockManga.favorite = true }
            coVerify(exactly = 1) { mangaRepository.updateManga(mockManga) }
            coVerify(exactly = 1) { updateMangaAggregate(mangaId, "/manga/1", true) }
            coVerify(exactly = 1) { categoryRepository.setMangaCategories(any(), listOf(mangaId)) }
        }

    @Test
    fun `given unfavorite manga when toggling favorite and categoryItems is empty and defaultCategory is set then adds to default category`() =
        runTest {
            // Arrange
            val mangaId = 1L
            val mockManga = mockk<Manga>(relaxed = true)
            every { mockManga.id } returns mangaId
            every { mockManga.favorite } returns false
            every { mockManga.url } returns "/manga/1"
            coEvery { mangaRepository.getMangaById(mangaId) } returns mockManga
            coEvery { mangaRepository.updateManga(mockManga) } just runs
            coEvery { updateMangaAggregate(mangaId, "/manga/1", true) } just runs

            val mockDefaultCategoryPref = mockk<Preference<Int>>()
            every { libraryPreferences.defaultCategory() } returns mockDefaultCategoryPref
            every { mockDefaultCategoryPref.get() } returns 5

            val category =
                CategoryItem(
                    id = 5,
                    name = "Default Category",
                    sortOrder = LibrarySort.Title,
                    isAscending = true,
                )
            coEvery { categoryRepository.setMangaCategories(any(), listOf(mangaId)) } just runs

            // Act
            val result = toggleMangaFavorite(mangaId, emptyList()) { listOf(category) }

            // Assert
            assertEquals(true, result)
            verify(exactly = 1) { mockManga.favorite = true }
            coVerify(exactly = 1) { mangaRepository.updateManga(mockManga) }
            coVerify(exactly = 1) { updateMangaAggregate(mangaId, "/manga/1", true) }
            coVerify(exactly = 1) { categoryRepository.setMangaCategories(any(), listOf(mangaId)) }
        }
}
