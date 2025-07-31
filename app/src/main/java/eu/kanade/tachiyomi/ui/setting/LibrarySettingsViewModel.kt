package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.launchIO
import kotlin.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LibrarySettingsViewModel : ViewModel() {

    val libraryPreferences by injectLazy<LibraryPreferences>()

    val db by injectLazy<DatabaseHelper>()

    private val _dbCategories = MutableStateFlow((emptyList<Category>()))

    val dbCategories = _dbCategories.asStateFlow()

    init {
        viewModelScope.launch {
            db.getCategories().asFlow().distinctUntilChanged().collectLatest { categories ->
                _dbCategories.value = listOf(Category.createDefault()) + categories
            }
        }
    }

    fun setLibrarySearchSuggestion() {
        launchIO {
            val sourceManager = Injekt.get<SourceManager>()
            LibraryPresenter.setSearchSuggestion(libraryPreferences, db, sourceManager)
        }
    }
}
