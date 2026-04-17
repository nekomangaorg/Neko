package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibrarySort
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.injectLazy

class LibrarySettingsViewModel : ViewModel() {

    val libraryPreferences by injectLazy<LibraryPreferences>()

    val preferences by injectLazy<PreferencesHelper>()

    val categoryRepository: CategoryRepository by injectLazy()

    private val _allCategories =
        MutableStateFlow<PersistentList<CategoryItem>>((persistentListOf()))
    val allCategories = _allCategories.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().distinctUntilChanged().collectLatest { categories
                ->
                _allCategories.value =
                    (listOf(Category.createSystemCategory()) + categories)
                        .sortedBy { it.order }
                        .map { it.toCategoryItem() }
                        .toPersistentList()
            }
        }
    }

    fun addNewCategory(categoryName: String) {
        viewModelScope.launchIO {
            val category = Category.create(categoryName)
            category.order = (_allCategories.value.maxOfOrNull { it.order } ?: 0) + 1

            // Insert into database.
            category.mangaSort = LibrarySort.Title.categoryValue
            categoryRepository.insertCategory(category)
        }
    }

    fun addUpdateCategory(newCategoryName: String, id: Int?) {
        viewModelScope.launchIO {
            if (id == null) {
                addNewCategory(newCategoryName)
            } else {
                val category = _allCategories.value.firstOrNull { it.id == id }
                if (category != null) {
                    val updatedCategory = category.copy(name = newCategoryName)
                    categoryRepository.insertCategory(updatedCategory.toDbCategory())
                }
            }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launchIO {
            val category = _allCategories.value.firstOrNull { it.id == id }
            if (category != null) {
                categoryRepository.deleteCategory(category.toDbCategory())
            }
        }
    }

    fun onChangeOrder(category: CategoryItem, newIndex: Int) {
        viewModelScope.launchIO {
            val dbCategories = categoryRepository.getCategories().toMutableList()

            val currentIndex = dbCategories.indexOfFirst { category.id == it.id }

            dbCategories.add(newIndex, dbCategories.removeAt(currentIndex))

            // Default category is not saved to DB and is always 0 order
            dbCategories.forEachIndexed { index, dbCategory -> dbCategory.order = index + 1 }
            categoryRepository.insertCategories(dbCategories)
        }
    }
}
