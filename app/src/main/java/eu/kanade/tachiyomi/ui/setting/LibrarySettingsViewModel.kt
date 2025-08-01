package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import eu.kanade.tachiyomi.ui.library.LibrarySort
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.launchIO
import kotlin.getValue
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LibrarySettingsViewModel : ViewModel() {

    val libraryPreferences by injectLazy<LibraryPreferences>()

    val db by injectLazy<DatabaseHelper>()

    private val _dbCategories = MutableStateFlow<PersistentList<CategoryItem>>((persistentListOf()))

    val dbCategories = _dbCategories.asStateFlow()

    init {
        viewModelScope.launch {
            db.getCategories().asFlow().distinctUntilChanged().collectLatest { categories ->
                _dbCategories.value =
                    (listOf(Category.createDefault()) + categories)
                        .map { it.toCategoryItem() }
                        .toPersistentList()
            }
        }
    }

    fun setLibrarySearchSuggestion() {
        launchIO {
            val sourceManager = Injekt.get<SourceManager>()
            LibraryPresenter.setSearchSuggestion(libraryPreferences, db, sourceManager)
        }
    }

    fun addNewCategory(categoryName: String) {
        viewModelScope.launchIO {
            val category = Category.create(categoryName)
            category.order = (_dbCategories.value.maxOfOrNull { it.order } ?: 0) + 1

            // Insert into database.
            category.mangaSort = LibrarySort.Title.categoryValue
            db.insertCategory(category).executeAsBlocking()
        }
    }

    fun addUpdateCategory(newCategoryName: String, id: Int?) {
        viewModelScope.launchIO {
            if (id == null) {
                addNewCategory(newCategoryName)
            } else {
                val category = _dbCategories.value.firstOrNull { it.id == id }
                if (category != null) {
                    category.copy(name = newCategoryName)
                    db.insertCategory(category.toDbCategory()).executeAsBlocking()
                }
            }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launchIO {
            val category = _dbCategories.value.firstOrNull { it.id == id }
            if (category != null) {
                db.deleteCategory(category.toDbCategory()).executeAsBlocking()
            }
        }
    }
}
