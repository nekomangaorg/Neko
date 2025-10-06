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
import eu.kanade.tachiyomi.util.unique
import java.util.Date
import kotlinx.collections.immutable.toPersistentList
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
                        ?: (displayScreenType as? DisplayScreenType.FeedUpdates)?.titleRes
                        ?: (displayScreenType as? DisplayScreenType.RecentlyAdded)?.titleRes
                        ?: (displayScreenType as? DisplayScreenType.PopularNewTitles)?.titleRes,
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                isComfortableGrid = libraryPreferences.layoutLegacy().get() == 2,
                rawColumnCount = libraryPreferences.gridSize().get(),
                libraryEntryVisibility = preferences.browseDisplayMode().get(),
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
                        allDisplayManga = allDisplayManga.toPersistentList(),
                        filteredDisplayManga =
                            allDisplayManga.filterVisibility(preferences).toPersistentList(),
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
                    .toPersistentList()
            _displayScreenState.update {
                it.copy(
                    categories = categories,
                    promptForCategories =
                        CategoryUtil.shouldShowCategoryPrompt(libraryPreferences, categories),
                )
            }
        }

        presenterScope.launch {
            preferences.browseAsList().changes().collectLatest {
                _displayScreenState.update { state -> state.copy(isList = it) }
            }
        }
        presenterScope.launch {
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
        presenterScope.launch {
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
        presenterScope.launchIO {
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
        presenterScope.launch {
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
