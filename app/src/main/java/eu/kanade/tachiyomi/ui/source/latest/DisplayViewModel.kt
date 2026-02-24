package eu.kanade.tachiyomi.ui.source.latest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.map
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryDisplayMode
import eu.kanade.tachiyomi.util.category.CategoryUtil
import eu.kanade.tachiyomi.util.filterVisibility
import eu.kanade.tachiyomi.util.resync
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.unique
import java.util.Date
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.network.ResultError
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayViewModel(val displayScreenType: DisplayScreenType) : ViewModel() {

    class Factory(private val serializableDisplayScreenType: SerializableDisplayScreenType) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DisplayViewModel(serializableDisplayScreenType.toDomain()) as T
        }
    }

    private val displayRepository: DisplayRepository = Injekt.get()
    private val preferences: PreferencesHelper = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()

    private val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    private val securityPreferences: SecurityPreferences = Injekt.get()
    private val db: DatabaseHelper = Injekt.get()

    private val _displayScreenState =
        MutableStateFlow(
            DisplayScreenState(
                isList = preferences.browseAsList().get(),
                title = displayScreenType.title,
                incognitoMode = securityPreferences.incognitoMode().get(),
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                dynamicCovers = mangaDetailsPreferences.dynamicCovers().get(),
                isComfortableGrid =
                    libraryPreferences.layout().get() != LibraryDisplayMode.CompactGrid,
                rawColumnCount = libraryPreferences.gridSize().get(),
                libraryEntryVisibility = preferences.browseDisplayMode().get(),
            )
        )
    val displayScreenState: StateFlow<DisplayScreenState> = _displayScreenState.asStateFlow()

    private val paginator =
        DefaultPaginator(
            initialKey = _displayScreenState.value.page,
            onLoadUpdated = { _displayScreenState.update { state -> state.copy(isLoading = it) } },
            onRequest = { nextPage ->
                displayRepository.getPage(nextPage, displayScreenType).map { result ->
                    result.hasNextPage to listOf(result)
                }
            },
            getNextKey = { _displayScreenState.value.page + 1 },
            onError = { resultError ->
                _displayScreenState.update {
                    it.copy(
                        isLoading = false,
                        error =
                            when (resultError) {
                                is ResultError.Generic -> resultError.errorString
                                else -> (resultError as ResultError.HttpError).message
                            },
                    )
                }
            },
            onSuccess = { hasNextPage, items, newKey ->
                val isDisplayResult = _displayScreenState.value.isDisplayResult

                if (isDisplayResult) {
                    _displayScreenState.update {
                        it.copy(
                            isLoading = false,
                            page = newKey,
                            endReached = !hasNextPage,
                            alternativeDisplay = items.first().displayResult,
                        )
                    }
                } else {
                    _displayScreenState.update {
                        val allDisplayManga =
                            (_displayScreenState.value.allDisplayManga + items.first().displayManga)
                                .distinct()
                        it.copy(
                            isLoading = false,
                            page = newKey,
                            endReached = !hasNextPage,
                            allDisplayManga = allDisplayManga.toPersistentList(),
                            filteredDisplayManga =
                                allDisplayManga.filterVisibility(preferences).toPersistentList(),
                        )
                    }
                }
            },
        )

    init {
        if (
            displayScreenType is DisplayScreenType.AuthorByName ||
                displayScreenType is DisplayScreenType.GroupByName
        ) {
            _displayScreenState.update { it.copy(isDisplayResult = true) }
        }
        loadNextItems()

        viewModelScope.launch {
            val categories =
                db.getCategories()
                    .executeAsBlocking()
                    .map { category -> category.toCategoryItem() }
                    .toPersistentList()
            _displayScreenState.update {
                it.copy(
                    categories = categories,
                    promptForCategories =
                        CategoryUtil.shouldShowCategoryPrompt(libraryPreferences, categories),
                )
            }
        }

        viewModelScope.launch {
            preferences.browseAsList().changes().collectLatest {
                _displayScreenState.update { state -> state.copy(isList = it) }
            }
        }
        viewModelScope.launch {
            preferences.browseDisplayMode().changes().collectLatest { visibility ->
                _displayScreenState.update {
                    it.copy(
                        libraryEntryVisibility = visibility,
                        filteredDisplayManga =
                            it.allDisplayManga.filterVisibility(preferences).toPersistentList(),
                    )
                }
            }
        }
    }

    fun loadNextItems() {
        viewModelScope.launch { paginator.loadNextItems() }
    }

    fun toggleFavorite(mangaId: Long, categoryItems: List<CategoryItem>) {
        viewModelScope.launch {
            val editManga = db.getManga(mangaId).executeAsBlocking()!!
            editManga.apply {
                favorite = !favorite
                date_added =
                    when (favorite) {
                        true -> Date().time
                        false -> 0
                    }
            }
            db.insertManga(editManga).executeAsBlocking()

            updateDisplayManga(mangaId, editManga.favorite)

            if (editManga.favorite) {
                val defaultCategory = libraryPreferences.defaultCategory().get()

                if (categoryItems.isEmpty() && defaultCategory != -1) {
                    _displayScreenState.value.categories
                        .firstOrNull { defaultCategory == it.id }
                        ?.let {
                            val categories =
                                listOf(MangaCategory.create(editManga, it.toDbCategory()))
                            db.setMangaCategories(categories, listOf(editManga))
                        }
                } else if (categoryItems.isNotEmpty()) {
                    val categories =
                        categoryItems.map { MangaCategory.create(editManga, it.toDbCategory()) }
                    db.setMangaCategories(categories, listOf(editManga))
                }
            }
        }
    }

    private fun updateDisplayManga(mangaId: Long, favorite: Boolean) {
        viewModelScope.launch {
            val index =
                _displayScreenState.value.allDisplayManga.indexOfFirst { it.mangaId == mangaId }
            val tempDisplayManga =
                _displayScreenState.value.allDisplayManga[index].copy(inLibrary = favorite)
            _displayScreenState.update {
                it.copy(allDisplayManga = it.allDisplayManga.set(index, tempDisplayManga))
            }

            val filteredIndex =
                _displayScreenState.value.filteredDisplayManga.indexOfFirst {
                    it.mangaId == mangaId
                }
            if (filteredIndex >= 0) {
                _displayScreenState.update {
                    it.copy(
                        filteredDisplayManga =
                            it.filteredDisplayManga.set(filteredIndex, tempDisplayManga)
                    )
                }
            }

            if (preferences.browseDisplayMode().get() != 0) {
                _displayScreenState.update {
                    it.copy(
                        filteredDisplayManga =
                            it.allDisplayManga.filterVisibility(preferences).toPersistentList()
                    )
                }
            }
        }
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            val category = Category.create(newCategory)
            category.order =
                (_displayScreenState.value.categories.maxOfOrNull { it.order } ?: 0) + 1
            db.insertCategory(category).executeAsBlocking()
            _displayScreenState.update {
                it.copy(
                    categories =
                        db.getCategories()
                            .executeAsBlocking()
                            .map { category -> category.toCategoryItem() }
                            .toPersistentList()
                )
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!displayScreenState.value.isList)
    }

    fun switchLibraryEntryVisibility(visibility: Int) {
        preferences.browseDisplayMode().set(visibility)
    }

    fun updateMangaForChanges() {
        viewModelScope.launch {
            val newDisplayManga =
                _displayScreenState.value.allDisplayManga.resync(db).unique().toPersistentList()
            _displayScreenState.update {
                it.copy(
                    allDisplayManga = newDisplayManga,
                    filteredDisplayManga =
                        newDisplayManga.filterVisibility(preferences).toPersistentList(),
                )
            }
        }
    }
}
