package org.nekomanga.presentation.screens.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryDisplayMode
import eu.kanade.tachiyomi.ui.source.browse.LibraryEntryVisibility
import eu.kanade.tachiyomi.util.category.CategoryUtil
import eu.kanade.tachiyomi.util.system.launchIO
import java.util.Date
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.usecases.manga.MangaUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SimilarViewModel(val mangaUUID: String) : ViewModel() {

    class Factory(private val mangaUUID: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SimilarViewModel(mangaUUID) as T
        }
    }

    private val repo: SimilarRepo = Injekt.get()
    private val categoryRepository: CategoryRepository = Injekt.get()
    private val mangaRepository: MangaRepository = Injekt.get()
    private val preferences: PreferencesHelper = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    private val securityPreferences: SecurityPreferences = Injekt.get()
    private val mangaUseCases: MangaUseCases by injectLazy()

    private val _similarScreenState =
        MutableStateFlow(
            SimilarScreenState(
                isList = preferences.browseAsList().get(),
                incognitoMode = securityPreferences.incognitoMode().get(),
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                dynamicCovers = mangaDetailsPreferences.dynamicCovers().get(),
                isComfortableGrid =
                    libraryPreferences.layout().get() != LibraryDisplayMode.CompactGrid,
                rawColumnCount = libraryPreferences.gridSize().get(),
                libraryEntryVisibility = preferences.browseDisplayMode().get(),
            )
        )

    val similarScreenState: StateFlow<SimilarScreenState> = _similarScreenState.asStateFlow()

    init {
        getSimilarManga()
        viewModelScope.launch {
            val categories =
                categoryRepository
                    .getCategories()
                    .map { category -> category.toCategoryItem() }
                    .toPersistentList()
            _similarScreenState.update {
                it.copy(
                    categories = categories,
                    promptForCategories =
                        CategoryUtil.shouldShowCategoryPrompt(libraryPreferences, categories),
                )
            }
        }
        preferences.browseAsList().changes().observeAndUpdate(viewModelScope) { isList ->
            _similarScreenState.update { state -> state.copy(isList = isList) }
        }

        preferences.browseDisplayMode().changes().observeAndUpdate(viewModelScope) { visibility ->
            _similarScreenState.update { state ->
                state.copy(
                    libraryEntryVisibility = visibility,
                    filteredDisplayManga = state.allDisplayManga.filterByVisibility(preferences),
                )
            }
        }
    }

    fun refresh() {
        getSimilarManga(true)
    }

    private fun getSimilarManga(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (mangaUUID.isNotEmpty()) {
                _similarScreenState.update {
                    it.copy(
                        isRefreshing = true,
                        allDisplayManga = persistentMapOf(),
                        filteredDisplayManga = persistentMapOf(),
                    )
                }

                val list = repo.fetchSimilar(mangaUUID, forceRefresh)
                val allDisplayManga =
                    list
                        .associate { group -> group.type to group.manga.toPersistentList() }
                        .toImmutableMap()
                _similarScreenState.update {
                    it.copy(
                        isRefreshing = false,
                        allDisplayManga = allDisplayManga,
                        filteredDisplayManga = allDisplayManga.filterByVisibility(preferences),
                    )
                }
            }
        }
    }

    fun toggleFavorite(mangaId: Long, categoryItems: List<CategoryItem>) {
        viewModelScope.launchIO {
            val editManga = mangaRepository.getMangaById(mangaId)!!
            editManga.apply {
                favorite = !favorite
                date_added =
                    when (favorite) {
                        true -> Date().time
                        false -> 0
                    }
            }
            mangaRepository.updateManga(editManga)

            mangaUseCases.updateMangaAggregate(mangaId, editManga.url, editManga.favorite)

            updateDisplayManga(mangaId, editManga.favorite)

            if (editManga.favorite) {
                val defaultCategory = libraryPreferences.defaultCategory().get()

                if (categoryItems.isEmpty() && defaultCategory != -1) {
                    _similarScreenState.value.categories
                        .firstOrNull { defaultCategory == it.id }
                        ?.let {
                            val categories =
                                listOf(MangaCategory.create(editManga, it.toDbCategory()))
                            categoryRepository.setMangaCategories(
                                categories,
                                listOf(editManga.id!!),
                            )
                        }
                } else if (categoryItems.isNotEmpty()) {
                    val categories = categoryItems.map {
                        MangaCategory.create(editManga, it.toDbCategory())
                    }
                    categoryRepository.setMangaCategories(categories, listOf(editManga.id!!))
                }
            }
        }
    }

    fun switchLibraryEntryVisibility(visibility: Int) {
        preferences.browseDisplayMode().set(visibility)
    }

    fun ImmutableMap<Int, PersistentList<DisplayManga>>.filterByVisibility(
        prefs: PreferencesHelper
    ): ImmutableMap<Int, PersistentList<DisplayManga>> {
        val visibilityMode = prefs.browseDisplayMode().get()

        return this.mapValues { (_, displayMangaList) ->
                displayMangaList
                    .filter { displayManga ->
                        when (visibilityMode) {
                            LibraryEntryVisibility.SHOW_IN_LIBRARY -> displayManga.inLibrary
                            LibraryEntryVisibility.SHOW_NOT_IN_LIBRARY -> !displayManga.inLibrary
                            else -> true
                        }
                    }
                    .toPersistentList()
            }
            .toImmutableMap()
    }

    private fun updateDisplayManga(mangaId: Long, favorite: Boolean) {
        viewModelScope.launch {
            val listOfKeyIndex =
                _similarScreenState.value.allDisplayManga.mapNotNull { entry ->
                    val tempListIndex = entry.value.indexOfFirst { it.mangaId == mangaId }
                    when (tempListIndex == -1) {
                        true -> null
                        false -> entry.key to tempListIndex
                    }
                }

            val tempMap = _similarScreenState.value.allDisplayManga.toMutableMap()

            listOfKeyIndex.forEach { pair ->
                val mapKey = pair.first
                val mangaIndex = pair.second
                val tempList = _similarScreenState.value.allDisplayManga[mapKey]!!.toMutableList()
                val tempDisplayManga = tempList[mangaIndex].copy(inLibrary = favorite)
                tempList[mangaIndex] = tempDisplayManga

                tempMap[mapKey] = tempList.toPersistentList()
            }

            _similarScreenState.update {
                it.copy(
                    allDisplayManga = tempMap.toImmutableMap(),
                    filteredDisplayManga = tempMap.toImmutableMap().filterByVisibility(preferences),
                )
            }
        }
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            val category = Category.create(newCategory)
            category.order =
                (_similarScreenState.value.categories.maxOfOrNull { it.order } ?: 0) + 1
            categoryRepository.insertCategory(category)
            _similarScreenState.update {
                it.copy(
                    categories =
                        categoryRepository
                            .getCategories()
                            .map { category -> category.toCategoryItem() }
                            .toPersistentList()
                )
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!similarScreenState.value.isList)
    }

    fun updateCovers() {
        viewModelScope.launchIO {
            val allMangaIds =
                _similarScreenState.value.allDisplayManga.values
                    .asSequence()
                    .flatten()
                    .map { it.mangaId }
                    .distinct()
                    .toList()

            val fetchedMangas =
                allMangaIds
                    .chunked(900)
                    .flatMap { chunk -> mangaRepository.getMangaByIds(chunk) }
                    .associateBy { it.id }

            val newDisplayManga =
                _similarScreenState.value.allDisplayManga
                    .map { entry ->
                        Pair(
                            entry.key,
                            entry.value
                                .map {
                                    val dbManga = fetchedMangas[it.mangaId]
                                    if (dbManga != null) {
                                        it.copy(
                                            currentArtwork =
                                                it.currentArtwork.copy(
                                                    cover = dbManga.user_cover ?: "",
                                                    originalCover =
                                                        dbManga.thumbnail_url
                                                            ?: MdConstants.noCoverUrl,
                                                )
                                        )
                                    } else {
                                        it
                                    }
                                }
                                .toPersistentList(),
                        )
                    }
                    .toMap()
                    .toImmutableMap()
            _similarScreenState.update {
                it.copy(
                    allDisplayManga = newDisplayManga,
                    filteredDisplayManga = newDisplayManga.filterByVisibility(preferences),
                )
            }
        }
    }
}
