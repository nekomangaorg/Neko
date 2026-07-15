package org.nekomanga.usecases.category

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import org.nekomanga.data.database.repository.CategoryRepository

class GetCategoriesUseCaseTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var useCase: GetCategoriesUseCase

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = mockk()
        useCase = GetCategoriesUseCase(categoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given categories in repository when getting categories then returns mapped CategoryItems`() =
        runTest {
            // Arrange
            val cat1 = CategoryImpl().apply { id = 1; name = "Category 1"; order = 1 }
            val cat2 = CategoryImpl().apply { id = 2; name = "Category 2"; order = 2 }
            coEvery { categoryRepository.getCategories() } returns listOf(cat1, cat2)

            // Act
            val result = useCase.get()

            // Assert
            assertEquals(2, result.size)
            assertEquals(1, result[0].id)
            assertEquals("Category 1", result[0].name)
            assertEquals(2, result[1].id)
            assertEquals("Category 2", result[1].name)
        }

    @Test
    fun `given categories in repository when observing categories then returns mapped CategoryItems flow`() =
        runTest {
            // Arrange
            val cat1 = CategoryImpl().apply { id = 10; name = "Observed 1"; order = 10 }
            coEvery { categoryRepository.observeCategories() } returns flowOf(listOf(cat1))

            // Act
            val result = useCase.observe().first()

            // Assert
            assertEquals(1, result.size)
            assertEquals(10, result[0].id)
            assertEquals("Observed 1", result[0].name)
        }
}
