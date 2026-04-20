package org.nekomanga.usecases.category

import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoryUseCases(
    mangaRepository: MangaRepository = Injekt.get(),
    categoryRepository: CategoryRepository = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
) {
    val modifyCategory =
        ModifyCategoryUseCase(categoryRepository, mangaRepository, libraryPreferences)
}
