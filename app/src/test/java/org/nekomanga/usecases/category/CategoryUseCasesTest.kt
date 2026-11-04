package org.nekomanga.usecases.category

import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.mockk
import org.junit.Test
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.library.LibraryPreferences

class CategoryUseCasesTest {

    @Test
    fun `given injected dependencies when constructing CategoryUseCases then all usecases are initialized`() {
        val mangaRepository = mockk<MangaRepository>()
        val categoryRepository = mockk<CategoryRepository>()
        val libraryPreferences = mockk<LibraryPreferences>()

        val categoryUseCases =
            CategoryUseCases(
                mangaRepository = mangaRepository,
                categoryRepository = categoryRepository,
                libraryPreferences = libraryPreferences,
            )

        categoryUseCases.modifyCategory.shouldNotBeNull()
        categoryUseCases.getCategories.shouldNotBeNull()
        categoryUseCases.validateCategoryName.shouldNotBeNull()
        categoryUseCases.calculateCategoryDiff.shouldNotBeNull()
    }
}
