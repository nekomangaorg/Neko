package eu.kanade.tachiyomi.ui.source.latest

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.source.browse.LibraryFilter
import eu.kanade.tachiyomi.util.category.CategoryUtil
import eu.kanade.tachiyomi.util.resync
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.unique
import java.util.Date
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
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
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayPresenter(
    val displayScreenType: DisplayScreenType,
    private val displayRepository: DisplayRepository = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val securityPreferences: SecurityPreferences = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<DisplayController>() {

    private val _displayScreenState =
        MutableStateFlow(
            DisplayScreenState(
                isList = preferences.browseAsList().get(),
                incognitoMode = securityPreferences.incognitoMode().get(),
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
                        (_displayScreenState.value.filteredDisplayManga[0] ?: emptyList()) + items
                    it.copy(
                        isLoading = false,
                        page = newKey,
                        endReached = !hasNextPage,
                        filteredDisplayManga =
                            persistentMapOf(0 to allDisplayManga.toPersistentList())
                                .filterVisibility(preferences),
                    )
                }
            },
        )

    override fun onCreate() {
        super.onCreate()

        when (displayScreenType) {
            is DisplayScreenType.Similar -> getSimilarManga()
            else -> loadNextItems()
        }

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
                        filteredDisplayManga = it.filteredDisplayManga.filterVisibility(preferences),
                    )
                }
            }
        }
    }

    fun refresh() {
        when (displayScreenType) {
            is DisplayScreenType.Similar -> getSimilarManga(true)
            else -> {
                // remove all display manga and start from first page
                _displayScreenState.update {
                    it.copy(filteredDisplayManga = persistentMapOf(), page = 1)
                }
                loadNextItems()
            }
        }
    }

    private fun getSimilarManga(forceRefresh: Boolean = false) {
        presenterScope.launch {
            val mangaId = (displayScreenType as DisplayScreenType.Similar).mangaId
            if (mangaId.isNotEmpty()) {
                _displayScreenState.update {
                    it.copy(
                        isRefreshing = true,
                        filteredDisplayManga = persistentMapOf(),
                    )
                }

                val list = displayRepository.fetchSimilar(mangaId, forceRefresh)
                val allDisplayManga =
                    list
                        .associate { group -> group.type to group.manga.toPersistentList() }
                        .toImmutableMap()
                _displayScreenState.update {
                    it.copy(
                        isRefreshing = false,
                        filteredDisplayManga = allDisplayManga.filterVisibility(preferences),
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
            val listOfKeyIndex =
                _displayScreenState.value.filteredDisplayManga.mapNotNull { entry ->
                    val tempListIndex = entry.value.indexOfFirst { it.mangaId == mangaId }
                    when (tempListIndex == -1) {
                        true -> null
                        false -> entry.key to tempListIndex
                    }
                }

            val tempMap = _displayScreenState.value.filteredDisplayManga.toMutableMap()

            listOfKeyIndex.forEach { pair ->
                val mapKey = pair.first
                val mangaIndex = pair.second
                val tempList =
                    _displayScreenState.value.filteredDisplayManga[mapKey]!!.toMutableList()
                val tempDisplayManga = tempList[mangaIndex].copy(inLibrary = favorite)
                tempList[mangaIndex] = tempDisplayManga

                tempMap[mapKey] = tempList.toPersistentList()
            }

            _displayScreenState.update {
                it.copy(
                    filteredDisplayManga = tempMap.toImmutableMap().filterVisibility(preferences),
                )
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
                _displayScreenState.value.filteredDisplayManga
                    .map { entry ->
                        Pair(
                            entry.key,
                            entry.value.resync(db).unique().toPersistentList(),
                        )
                    }
                    .toMap()
                    .toImmutableMap()
            _displayScreenState.update {
                it.copy(
                    filteredDisplayManga = newDisplayManga.filterVisibility(preferences),
                )
            }
        }
    }
}

fun ImmutableMap<Int, PersistentList<DisplayManga>>.filterVisibility(
    prefs: PreferencesHelper
): ImmutableMap<Int, PersistentList<DisplayManga>> {
    val visibilityMode = prefs.browseDisplayMode().get()

    return this.mapValues { (_, displayMangaList) ->
            displayMangaList
                .filter { displayManga -> LibraryFilter.filter(displayManga, visibilityMode) }
                .toPersistentList()
        }
        .toImmutableMap()
}
