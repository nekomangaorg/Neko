package org.nekomanga.usecases.category

import eu.kanade.tachiyomi.ui.library.LibrarySort
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
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.library.LibraryPreferences
import tachiyomi.core.preference.Preference

class ModifyCategoryUseCaseTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var mangaRepository: MangaRepository
    private lateinit var libraryPreferences: LibraryPreferences
    private lateinit var useCase: ModifyCategoryUseCase

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = mockk()
        mangaRepository = mockk()
        libraryPreferences = mockk()
        useCase = ModifyCategoryUseCase(categoryRepository, mangaRepository, libraryPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given dynamic category when updating sort ascending then library preferences sort is inverted`() =
        runTest {
            // Arrange
            val category =
                CategoryItem(
                    id = -1,
                    name = "Dynamic",
                    sortOrder = LibrarySort.Title,
                    isAscending = true,
                    isDynamic = true,
                )
            val mockSortAscendingPref = mockk<Preference<Boolean>>()
            every { libraryPreferences.sortAscending() } returns mockSortAscendingPref
            every { mockSortAscendingPref.set(any()) } just runs

            // Act
            useCase.updateCategorySortAscending(category)

            // Assert
            verify(exactly = 1) { mockSortAscendingPref.set(false) }
        }

    @Test
    fun `given system category when updating sort ascending then library preferences sort is inverted`() =
        runTest {
            // Arrange
            val category =
                CategoryItem(
                    id = 0,
                    name = CategoryItem.SYSTEM_CATEGORY,
                    sortOrder = LibrarySort.Title,
                    isAscending = false,
                    isSystemCategory = true,
                )
            val mockSortAscendingPref = mockk<Preference<Boolean>>()
            every { libraryPreferences.sortAscending() } returns mockSortAscendingPref
            every { mockSortAscendingPref.set(any()) } just runs

            // Act
            useCase.updateCategorySortAscending(category)

            // Assert
            verify(exactly = 1) { mockSortAscendingPref.set(true) }
        }

    @Test
    fun `given regular category when updating sort ascending then category is updated in database`() =
        runTest {
            // Arrange
            val category =
                CategoryItem(
                    id = 1,
                    name = "Regular",
                    sortOrder = LibrarySort.Title,
                    isAscending = true,
                    isDynamic = false,
                    isSystemCategory = false,
                )
            coEvery { categoryRepository.insertCategory(any()) } returns 1

            // Act
            useCase.updateCategorySortAscending(category)

            // Assert
            coVerify(exactly = 1) {
                categoryRepository.insertCategory(match { it.name == "Regular" })
            }
        }

    @Test
    fun `given dynamic category when updating library sort then library preferences sort mode is updated`() =
        runTest {
            // Arrange
            val category =
                CategoryItem(
                    id = -1,
                    name = "Dynamic",
                    sortOrder = LibrarySort.Title,
                    isAscending = true,
                    isDynamic = true,
                )
            val mockSortingModePref = mockk<Preference<Int>>()
            every { libraryPreferences.sortingMode() } returns mockSortingModePref
            every { mockSortingModePref.set(any()) } just runs

            // Act
            useCase.updateCategoryLibrarySort(category, LibrarySort.LastRead)

            // Assert
            verify(exactly = 1) { mockSortingModePref.set(LibrarySort.LastRead.mainValue) }
        }

    @Test
    fun `given system category when updating library sort then library preferences sort mode is updated`() =
        runTest {
            // Arrange
            val category =
                CategoryItem(
                    id = 0,
                    name = CategoryItem.SYSTEM_CATEGORY,
                    sortOrder = LibrarySort.Title,
                    isAscending = true,
                    isSystemCategory = true,
                )
            val mockSortingModePref = mockk<Preference<Int>>()
            every { libraryPreferences.sortingMode() } returns mockSortingModePref
            every { mockSortingModePref.set(any()) } just runs

            // Act
            useCase.updateCategoryLibrarySort(category, LibrarySort.Unread)

            // Assert
            verify(exactly = 1) { mockSortingModePref.set(LibrarySort.Unread.mainValue) }
        }

    @Test
    fun `given regular category when updating library sort then category sort order is updated in database`() =
        runTest {
            // Arrange
            val category =
                CategoryItem(
                    id = 1,
                    name = "Regular",
                    sortOrder = LibrarySort.Title,
                    isAscending = true,
                    isDynamic = false,
                    isSystemCategory = false,
                )
            coEvery { categoryRepository.insertCategory(any()) } returns 1

            // Act
            useCase.updateCategoryLibrarySort(category, LibrarySort.DateAdded)

            // Assert
            coVerify(exactly = 1) {
                categoryRepository.insertCategory(match { it.name == "Regular" })
            }
        }
}
