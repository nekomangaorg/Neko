package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.nekomanga.data.database.model.toCategory
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.reader.ReaderPreferences
import uy.kohesive.injekt.injectLazy

class DownloadSettingsViewModel : ViewModel() {

    val readerPreferences by injectLazy<ReaderPreferences>()

    val preferences by injectLazy<PreferencesHelper>()

    private val categoryRepository by injectLazy<CategoryRepositoryImpl>()

    private val _allCategories =
        MutableStateFlow<PersistentList<CategoryItem>>((persistentListOf()))
    val allCategories = _allCategories.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .map { categories -> categories.map { it.toCategory() } }
                .distinctUntilChanged()
                .collectLatest { categories ->
                    _allCategories.value =
                        (listOf(Category.createSystemCategory()) + categories)
                            .sortedBy { it.order }
                            .map { it.toCategoryItem() }
                            .toPersistentList()
                }
        }
    }
}
