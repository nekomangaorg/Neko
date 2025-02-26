package eu.kanade.tachiyomi.util.category

import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.library.LibraryPreferences

class CategoryUtil {
    companion object {
        fun shouldShowCategoryPrompt(
            libraryPreferences: LibraryPreferences,
            categoryItems: List<CategoryItem>,
        ): Boolean {
            return libraryPreferences.defaultCategory().get() == -1 && categoryItems.isNotEmpty()
        }
    }
}
