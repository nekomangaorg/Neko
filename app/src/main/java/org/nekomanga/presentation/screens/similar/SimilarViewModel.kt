package org.nekomanga.presentation.screens.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.source.browse.LibraryEntryVisibility
import eu.kanade.tachiyomi.util.category.CategoryUtil
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.screens.library.LibraryDisplayMode
import org.nekomanga.usecases.category.CategoryUseCases
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
    private val categoryUseCases: CategoryUseCases by injectLazy()
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
            val categories = categoryUseCases.getCategories.get()
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
                        allDisplayManga = mapOf(),
                        filteredDisplayManga = mapOf(),
                    )
                }

                val list = repo.fetchSimilar(mangaUUID, forceRefresh)
                val allDisplayManga =
                    list.associate { group -> group.type to group.manga.toList() }.toMap()
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
            val isFavorite =
                mangaUseCases.toggleMangaFavorite(
                    mangaId = mangaId,
                    categoryItems = categoryItems,
                    categoriesProvider = { _similarScreenState.value.categories },
                ) ?: return@launchIO

            updateDisplayManga(mangaId, isFavorite)
        }
    }

    fun switchLibraryEntryVisibility(visibility: Int) {
        preferences.browseDisplayMode().set(visibility)
    }

    fun Map<Int, List<DisplayManga>>.filterByVisibility(
        prefs: PreferencesHelper
    ): Map<Int, List<DisplayManga>> {
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
                    .toList()
            }
            .toMap()
    }

    private fun updateDisplayManga(mangaId: Long, favorite: Boolean) {
        viewModelScope.launch {
            val currentAll = _similarScreenState.value.allDisplayManga
            val updatedAll = currentAll.mapValues { (_, list) ->
                list.map { manga ->
                    if (manga.mangaId == mangaId) manga.copy(inLibrary = favorite) else manga
                }
            }

            _similarScreenState.update {
                it.copy(
                    allDisplayManga = updatedAll,
                    filteredDisplayManga = updatedAll.filterByVisibility(preferences),
                )
            }
        }
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.addNewCategory(newCategory)
            _similarScreenState.update {
                it.copy(categories = categoryUseCases.getCategories.get())
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

            if (allMangaIds.isEmpty()) return@launchIO

            val fetchedMangas =
                allMangaIds
                    .chunked(900)
                    .flatMap { chunk -> mangaRepository.getMangaByIds(chunk) }
                    .associateBy { it.id }

            val newDisplayManga =
                _similarScreenState.value.allDisplayManga.mapValues { (_, list) ->
                    list.map { manga ->
                        val dbManga = fetchedMangas[manga.mangaId]
                        if (dbManga != null) {
                            manga.copy(
                                currentArtwork =
                                    manga.currentArtwork.copy(
                                        cover = dbManga.user_cover ?: "",
                                        originalCover =
                                            dbManga.thumbnail_url ?: MdConstants.noCoverUrl,
                                    )
                            )
                        } else {
                            manga
                        }
                    }
                }
            _similarScreenState.update {
                it.copy(
                    allDisplayManga = newDisplayManga,
                    filteredDisplayManga = newDisplayManga.filterByVisibility(preferences),
                )
            }
        }
    }
}
