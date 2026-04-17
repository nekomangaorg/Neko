package org.nekomanga.usecases.category

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoryUseCases(
    db: DatabaseHelper = Injekt.get(),
    categoryRepository: CategoryRepository = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
) {
    val modifyCategory = ModifyCategoryUseCase(db, categoryRepository, libraryPreferences)
}
