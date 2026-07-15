package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.usecases.category.CategoryUseCases
import uy.kohesive.injekt.injectLazy

class LibrarySettingsViewModel : ViewModel() {

    val libraryPreferences by injectLazy<LibraryPreferences>()

    val preferences by injectLazy<PreferencesHelper>()

    val categoryUseCases: CategoryUseCases by injectLazy()

    private val _allCategories =
        MutableStateFlow<List<CategoryItem>>((listOf()))
    val allCategories = _allCategories.asStateFlow()

    init {
        viewModelScope.launch {
            categoryUseCases.getCategories.observe().distinctUntilChanged().collectLatest { categories
                ->
                _allCategories.value =
                    (listOf(Category.createSystemCategory().toCategoryItem()) + categories)
                        .sortedBy { it.order }
                        .toList()
            }
        }
    }

    fun addNewCategory(categoryName: String) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.addNewCategory(categoryName)
        }
    }

    fun addUpdateCategory(newCategoryName: String, id: Int?) {
        viewModelScope.launchIO {
            if (id == null) {
                addNewCategory(newCategoryName)
            } else {
                categoryUseCases.modifyCategory.updateCategoryName(id, newCategoryName)
            }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.deleteCategory(id)
        }
    }

    fun onChangeOrder(category: CategoryItem, newIndex: Int) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.reorderCategories(category, newIndex)
        }
    }
}
