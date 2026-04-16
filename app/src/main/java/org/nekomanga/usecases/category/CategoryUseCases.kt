package org.nekomanga.usecases.category

import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoryUseCases(
    mangaRepositoryImpl: MangaRepositoryImpl = Injekt.get(),
    categoryRepositoryImpl: CategoryRepositoryImpl = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
) {
    val modifyCategory =
        ModifyCategoryUseCase(mangaRepositoryImpl, categoryRepositoryImpl, libraryPreferences)
}
