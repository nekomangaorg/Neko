package org.nekomanga.usecases.category

import org.nekomanga.presentation.screens.library.LibrarySort
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
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import org.junit.Test
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.DisplayManga
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
                categoryRepository.insertCategory(
                    match { insertedCategory ->
                        insertedCategory.name == "Regular" &&
                            insertedCategory.mangaSort == LibrarySort.Title.categoryValueDescending
                    }
                )
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
                categoryRepository.insertCategory(
                    match { insertedCategory ->
                        insertedCategory.name == "Regular" &&
                            insertedCategory.mangaSort == LibrarySort.DateAdded.categoryValue
                    }
                )
            }
        }

    @Test
    fun `given no categories when adding new category then order is 1 and category is inserted`() =
        runTest {
            // Arrange
            val categoryName = "New Category"
            coEvery { categoryRepository.getCategories() } returns emptyList()
            coEvery { categoryRepository.insertCategory(any()) } returns 1

            // Act
            useCase.addNewCategory(categoryName)

            // Assert
            coVerify(exactly = 1) {
                categoryRepository.insertCategory(
                    match { insertedCategory ->
                        insertedCategory.name == categoryName && insertedCategory.order == 1
                    }
                )
            }
        }

    @Test
    fun `given existing categories when adding new category then order is max existing plus 1`() =
        runTest {
            // Arrange
            val categoryName = "New Category"
            val category1 =
                CategoryImpl().apply {
                    name = "Cat 1"
                    order = 2
                }
            val category2 =
                CategoryImpl().apply {
                    name = "Cat 2"
                    order = 5
                }
            coEvery { categoryRepository.getCategories() } returns listOf(category1, category2)
            coEvery { categoryRepository.insertCategory(any()) } returns 1

            // Act
            useCase.addNewCategory(categoryName)

            // Assert
            coVerify(exactly = 1) {
                categoryRepository.insertCategory(
                    match { insertedCategory ->
                        insertedCategory.name == categoryName && insertedCategory.order == 6
                    }
                )
            }
        }

    @Test
    fun `given invalid manga ID when updating manga categories then no category is updated`() =
        runTest {
            // Arrange
            val mangaId = 999L
            coEvery { mangaRepository.getMangaById(mangaId) } returns null

            // Act
            useCase.updateMangaCategories(mangaId, emptyList())

            // Assert
            coVerify(exactly = 0) { categoryRepository.setMangaCategories(any(), any()) }
        }

    @Test
    fun `given valid manga ID and enabled categories when updating manga categories then categories are set correctly`() =
        runTest {
            // Arrange
            val mangaId = 123L
            val dbManga = Manga.create("url", "Title").apply { id = mangaId }
            coEvery { mangaRepository.getMangaById(mangaId) } returns dbManga

            val categoryItem1 =
                CategoryItem(id = 10, name = "Cat 10", sortOrder = LibrarySort.Title)
            val categoryItem2 =
                CategoryItem(id = 20, name = "Cat 20", sortOrder = LibrarySort.Title)
            val enabledCategories = listOf(categoryItem1, categoryItem2)

            coEvery { categoryRepository.setMangaCategories(any(), any()) } just runs

            // Act
            useCase.updateMangaCategories(mangaId, enabledCategories)

            // Assert
            coVerify(exactly = 1) {
                categoryRepository.setMangaCategories(
                    match { list ->
                        list.size == 2 &&
                            list[0].manga_id == mangaId &&
                            list[0].category_id == 10 &&
                            list[1].manga_id == mangaId &&
                            list[1].category_id == 20
                    },
                    listOf(mangaId),
                )
            }
        }

    @Test
    fun `given multiple mangas and categories when setting manga categories then setMangaCategories is called with all mappings`() =
        runTest {
            // Arrange
            val artwork1 = Artwork(mangaId = 1L)
            val artwork2 = Artwork(mangaId = 2L)
            val mangaList =
                listOf(
                    DisplayManga(
                        mangaId = 1L,
                        inLibrary = true,
                        currentArtwork = artwork1,
                        url = "url1",
                        originalTitle = "Manga 1",
                        userTitle = "Manga 1",
                    ),
                    DisplayManga(
                        mangaId = 2L,
                        inLibrary = true,
                        currentArtwork = artwork2,
                        url = "url2",
                        originalTitle = "Manga 2",
                        userTitle = "Manga 2",
                    ),
                )

            val dbManga1 = Manga.create("url1", "Manga 1").apply { id = 1L }
            val dbManga2 = Manga.create("url2", "Manga 2").apply { id = 2L }
            coEvery { mangaRepository.getMangaByIds(listOf(1L, 2L)) } returns
                listOf(dbManga1, dbManga2)

            val categoryItem1 =
                CategoryItem(id = 10, name = "Cat 10", sortOrder = LibrarySort.Title)
            val categoryItem2 =
                CategoryItem(id = 20, name = "Cat 20", sortOrder = LibrarySort.Title)
            val categories = listOf(categoryItem1, categoryItem2)

            coEvery { categoryRepository.setMangaCategories(any(), any()) } just runs

            // Act
            useCase.setMangaCategories(mangaList, categories)

            // Assert
            coVerify(exactly = 1) {
                categoryRepository.setMangaCategories(
                    match { list ->
                        list.size == 4 &&
                            list.any { it.manga_id == 1L && it.category_id == 10 } &&
                            list.any { it.manga_id == 1L && it.category_id == 20 } &&
                            list.any { it.manga_id == 2L && it.category_id == 10 } &&
                            list.any { it.manga_id == 2L && it.category_id == 20 }
                    },
                    listOf(1L, 2L),
                )
            }
        }
}
