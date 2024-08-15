package eu.kanade.tachiyomi.util.category

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.domain.category.CategoryItem

class CategoryUtil {
    companion object {
        fun shouldShowCategoryPrompt(
            preferencesHelper: PreferencesHelper,
            categoryItems: List<CategoryItem>
        ): Boolean {
            return preferencesHelper.defaultCategory().get() == -1 && categoryItems.isNotEmpty()
        }
    }
}
