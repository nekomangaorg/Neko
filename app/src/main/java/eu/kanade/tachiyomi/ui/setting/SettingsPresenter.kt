package eu.kanade.tachiyomi.ui.setting

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.storage.StoragePreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsPresenter() : BaseCoroutinePresenter<SettingsController>() {
    val preferencesHelper by injectLazy<PreferencesHelper>()
    val mangaDetailsPreferences by injectLazy<MangaDetailsPreferences>()
    val libraryPreferences by injectLazy<LibraryPreferences>()
    val storagePreferences by injectLazy<StoragePreferences>()
    val db by injectLazy<DatabaseHelper>()

    private val _dbCategories = MutableStateFlow((emptyList<Category>()))

    val dbCategories = _dbCategories.asStateFlow()

    init {
        db.getCategories()
            .asFlow()
            .map { categories -> _dbCategories.update { categories } }
            .stateIn(presenterScope, SharingStarted.WhileSubscribed(5000), emptyList<Category>())
    }

    fun setLibrarySearchSuggestion() {
        launchIO {
            val sourceManager = Injekt.get<SourceManager>()
            LibraryPresenter.setSearchSuggestion(libraryPreferences, db, sourceManager)
        }
    }
}
