package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.asFlow
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
import org.nekomanga.domain.reader.ReaderPreferences
import uy.kohesive.injekt.injectLazy

class DownloadSettingsViewModel : ViewModel() {

    val readerPreferences by injectLazy<ReaderPreferences>()

    val preferences by injectLazy<PreferencesHelper>()

    val db by injectLazy<DatabaseHelper>()

    private val _allCategories =
        MutableStateFlow<PersistentList<CategoryItem>>((persistentListOf()))
    val allCategories = _allCategories.asStateFlow()

    init {
        viewModelScope.launch {
            db.getCategories().asFlow().distinctUntilChanged().collectLatest { categories ->
                _allCategories.value =
                    (listOf(Category.createSystemCategory()) + categories)
                        .sortedBy { it.order }
                        .map { it.toCategoryItem() }
                        .toPersistentList()
            }
        }
    }
}
