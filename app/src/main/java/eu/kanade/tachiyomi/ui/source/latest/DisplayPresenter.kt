package eu.kanade.tachiyomi.ui.source.latest

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.category.CategoryUtil
import eu.kanade.tachiyomi.util.filterVisibility
import eu.kanade.tachiyomi.util.resync
import eu.kanade.tachiyomi.util.system.launchIO
import java.util.Date
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.network.ResultError
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayPresenter(
    displayScreenType: DisplayScreenType,
    private val displayRepository: DisplayRepository = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<DisplayController>() {

    private val _displayScreenState =
        MutableStateFlow(
            DisplayScreenState(
                isList = preferences.browseAsList().get(),
                title = (displayScreenType as? DisplayScreenType.List)?.title ?: "",
                titleRes =
                    (displayScreenType as? DisplayScreenType.LatestChapters)?.titleRes
                        ?: (displayScreenType as? DisplayScreenType.RecentlyAdded)?.titleRes
                        ?: (displayScreenType as? DisplayScreenType.PopularNewTitles)?.titleRes,
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                isComfortableGrid = libraryPreferences.layout().get() == 2,
                rawColumnCount = libraryPreferences.gridSize().get(),
                showLibraryEntries = preferences.browseShowLibrary().get(),
            )
        )
    val displayScreenState: StateFlow<DisplayScreenState> = _displayScreenState.asStateFlow()

    private val paginator =
        DefaultPaginator(
            initialKey = _displayScreenState.value.page,
            onLoadUpdated = { _displayScreenState.update { state -> state.copy(isLoading = it) } },
            onRequest = { nextPage -> displayRepository.getPage(nextPage, displayScreenType) },
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
                _displayScreenState.update {
                    val allDisplayManga =
                        (_displayScreenState.value.allDisplayManga + items).distinct()
                    it.copy(
                        isLoading = false,
                        page = newKey,
                        endReached = !hasNextPage,
                        allDisplayManga = allDisplayManga.toImmutableList(),
                        filteredDisplayManga =
                            allDisplayManga.filterVisibility(preferences).toImmutableList(),
                    )
                }
            },
        )

    override fun onCreate() {
        super.onCreate()

        loadNextItems()

        presenterScope.launch {
            val categories =
                db.getCategories()
                    .executeAsBlocking()
                    .map { category -> category.toCategoryItem() }
                    .toImmutableList()
            _displayScreenState.update {
                it.copy(
                    categories = categories,
                    promptForCategories =
                        CategoryUtil.shouldShowCategoryPrompt(preferences, categories),
                )
            }
        }

        presenterScope.launch {
            preferences.browseAsList().changes().collectLatest {
                _displayScreenState.update { state -> state.copy(isList = it) }
            }
        }
        presenterScope.launch {
            preferences.browseShowLibrary().changes().collectLatest { show ->
                _displayScreenState.update {
                    it.copy(
                        showLibraryEntries = show,
                        filteredDisplayManga =
                            it.allDisplayManga.filterVisibility(preferences).toImmutableList(),
                    )
                }
            }
        }
    }

    fun loadNextItems() {
        presenterScope.launch { paginator.loadNextItems() }
    }

    fun toggleFavorite(mangaId: Long, categoryItems: List<CategoryItem>) {
        presenterScope.launch {
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
                val defaultCategory = preferences.defaultCategory().get()

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
        presenterScope.launch {
            val index =
                _displayScreenState.value.allDisplayManga.indexOfFirst { it.mangaId == mangaId }
            val tempList = _displayScreenState.value.allDisplayManga.toMutableList()
            val tempDisplayManga = tempList[index].copy(inLibrary = favorite)
            tempList[index] = tempDisplayManga

            _displayScreenState.update { it.copy(allDisplayManga = tempList.toImmutableList()) }

            val filteredIndex =
                _displayScreenState.value.filteredDisplayManga.indexOfFirst {
                    it.mangaId == mangaId
                }
            if (filteredIndex >= 0) {
                val tempFilterList = _displayScreenState.value.filteredDisplayManga.toMutableList()
                tempFilterList[filteredIndex] = tempDisplayManga
                _displayScreenState.update {
                    it.copy(filteredDisplayManga = tempFilterList.toImmutableList())
                }
            }

            if (!preferences.browseShowLibrary().get()) {
                _displayScreenState.update {
                    it.copy(
                        filteredDisplayManga =
                            it.allDisplayManga.filterVisibility(preferences).toImmutableList()
                    )
                }
            }
        }
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        presenterScope.launchIO {
            val category = Category.create(newCategory)
            db.insertCategory(category).executeAsBlocking()
            _displayScreenState.update {
                it.copy(
                    categories =
                        db.getCategories()
                            .executeAsBlocking()
                            .map { category -> category.toCategoryItem() }
                            .toImmutableList()
                )
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!displayScreenState.value.isList)
    }

    fun switchLibraryVisibility() {
        preferences.browseShowLibrary().set(!displayScreenState.value.showLibraryEntries)
    }

    fun updateMangaForChanges() {
        presenterScope.launch {
            val newDisplayManga =
                _displayScreenState.value.allDisplayManga.resync(db).toImmutableList()
            _displayScreenState.update {
                it.copy(
                    allDisplayManga = newDisplayManga,
                    filteredDisplayManga =
                        newDisplayManga.filterVisibility(preferences).toImmutableList(),
                )
            }
        }
    }
}
