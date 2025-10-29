package org.nekomanga.presentation.screens

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.similar.SimilarRepository
import eu.kanade.tachiyomi.ui.source.browse.LibraryEntryVisibility
import eu.kanade.tachiyomi.ui.source.latest.DisplayRepository
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenContent
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenState
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.category.CategoryUtil
import eu.kanade.tachiyomi.util.filterVisibility
import eu.kanade.tachiyomi.util.resync
import eu.kanade.tachiyomi.util.system.executeOnIO
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
import org.nekomanga.constants.MdConstants
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
    private val displayScreenType: DisplayScreenType,
    private val displayRepository: DisplayRepository = Injekt.get(),
    private val similarRepository: SimilarRepository = Injekt.get(),
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
                title = (displayScreenType as? DisplayScreenType.List)?.title ?: "",
                titleRes =
                when (displayScreenType) {
                    is DisplayScreenType.LatestChapters -> displayScreenType.titleRes
                    is DisplayScreenType.FeedUpdates -> displayScreenType.titleRes
                    is DisplayScreenType.RecentlyAdded -> displayScreenType.titleRes
                    is DisplayScreenType.PopularNewTitles -> displayScreenType.titleRes
                    else -> null
                },
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                isComfortableGrid = libraryPreferences.layoutLegacy().get() == 2,
                rawColumnCount = libraryPreferences.gridSize().get(),
                libraryEntryVisibility = preferences.browseDisplayMode().get(),
            ),
        )
    val displayScreenState: StateFlow<DisplayScreenState> = _displayScreenState.asStateFlow()

    private val paginator by lazy {
        DefaultPaginator(
            initialKey = _displayScreenState.value.page,
            onLoadUpdated = {
                _displayScreenState.update { state -> state.copy(isLoading = it) }
            },
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
                        ((it.allDisplayManga as DisplayScreenContent.List).manga + items)
                            .distinct()
                    it.copy(
                        isLoading = false,
                        page = newKey,
                        endReached = !hasNextPage,
                        allDisplayManga = DisplayScreenContent.List(allDisplayManga.toPersistentList()),
                        filteredDisplayManga =
                        DisplayScreenContent.List(allDisplayManga.filterVisibility(preferences).toPersistentList()),
                    )
                }
            },
        )
    }

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
                        filteredDisplayManga =
                        when (displayScreenType) {
                            is DisplayScreenType.Similar ->
                                DisplayScreenContent.Graded(
                                    (it.allDisplayManga as DisplayScreenContent.Graded)
                                        .manga
                                        .filterByVisibility(preferences)
                                )
                            else ->
                                DisplayScreenContent.List(
                                    (it.allDisplayManga as DisplayScreenContent.List)
                                        .manga
                                        .filterVisibility(preferences)
                                        .toPersistentList()
                                )
                        },
                    )
                }
            }
        }
    }

    fun loadNextItems() {
        presenterScope.launch { paginator.loadNextItems() }
    }

    fun refresh() {
        getSimilarManga(true)
    }

    private fun getSimilarManga(forceRefresh: Boolean = false) {
        presenterScope.launch {
            val mangaId = (displayScreenType as DisplayScreenType.Similar).mangaId
            if (mangaId.isNotEmpty()) {
                _displayScreenState.update {
                    it.copy(
                        isRefreshing = true,
                        allDisplayManga = DisplayScreenContent.Graded(persistentMapOf()),
                        filteredDisplayManga = DisplayScreenContent.Graded(persistentMapOf()),
                    )
                }

                val list = similarRepository.fetchSimilar(mangaId, forceRefresh)
                val allDisplayManga =
                    list
                        .groupBy { it.type }
                        .mapValues { it.value.toPersistentList() }
                        .toImmutableMap()
                _displayScreenState.update {
                    it.copy(
                        isRefreshing = false,
                        allDisplayManga = DisplayScreenContent.Graded(allDisplayManga),
                        filteredDisplayManga =
                        DisplayScreenContent.Graded(
                            allDisplayManga.filterByVisibility(preferences),
                        ),
                    )
                }
            }
        }
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
            when (displayScreenType) {
                is DisplayScreenType.Similar -> {
                    val allDisplayManga =
                        (_displayScreenState.value.allDisplayManga as DisplayScreenContent.Graded)
                            .manga
                    val listOfKeyIndex =
                        allDisplayManga.mapNotNull { entry ->
                            val tempListIndex = entry.value.indexOfFirst { it.mangaId == mangaId }
                            when (tempListIndex == -1) {
                                true -> null
                                false -> entry.key to tempListIndex
                            }
                        }

                    val tempMap = allDisplayManga.toMutableMap()

                    listOfKeyIndex.forEach { pair ->
                        val mapKey = pair.first
                        val mangaIndex = pair.second
                        val tempList = allDisplayManga[mapKey]!!.toMutableList()
                        val tempDisplayManga = tempList[mangaIndex].copy(inLibrary = favorite)
                        tempList[mangaIndex] = tempDisplayManga

                        tempMap[mapKey] = tempList.toPersistentList()
                    }

                    _displayScreenState.update {
                        it.copy(
                            allDisplayManga = DisplayScreenContent.Graded(tempMap.toImmutableMap()),
                            filteredDisplayManga =
                            DisplayScreenContent.Graded(
                                tempMap.toImmutableMap().filterByVisibility(preferences),
                            ),
                        )
                    }
                }
                else -> {
                    val allDisplayManga =
                        (_displayScreenState.value.allDisplayManga as DisplayScreenContent.List)
                            .manga
                    val index = allDisplayManga.indexOfFirst { it.mangaId == mangaId }
                    if (index == -1) return@launch
                    val tempDisplayManga = allDisplayManga[index].copy(inLibrary = favorite)
                    _displayScreenState.update {
                        it.copy(
                            allDisplayManga =
                            DisplayScreenContent.List(allDisplayManga.set(index, tempDisplayManga)),
                        )
                    }

                    val filteredDisplayManga =
                        (_displayScreenState.value.filteredDisplayManga as DisplayScreenContent.List)
                            .manga
                    val filteredIndex = filteredDisplayManga.indexOfFirst { it.mangaId == mangaId }
                    if (filteredIndex >= 0) {
                        _displayScreenState.update {
                            it.copy(
                                filteredDisplayManga =
                                DisplayScreenContent.List(
                                    filteredDisplayManga.set(filteredIndex, tempDisplayManga),
                                ),
                            )
                        }
                    }

                    if (preferences.browseDisplayMode().get() != 0) {
                        _displayScreenState.update {
                            it.copy(
                                filteredDisplayManga =
                                DisplayScreenContent.List(
                                    (it.allDisplayManga as DisplayScreenContent.List)
                                        .manga
                                        .filterVisibility(preferences)
                                        .toPersistentList(),
                                ),
                            )
                        }
                    }
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
                        .toPersistentList(),
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
            when (displayScreenType) {
                is DisplayScreenType.Similar -> {
                    val allDisplayManga =
                        (_displayScreenState.value.allDisplayManga as DisplayScreenContent.Graded)
                            .manga
                    val newDisplayManga =
                        allDisplayManga
                            .map { entry ->
                                Pair(
                                    entry.key,
                                    entry.value
                                        .map {
                                            val dbManga = db.getManga(it.mangaId).executeOnIO()!!
                                            it.copy(
                                                currentArtwork =
                                                it.currentArtwork.copy(
                                                    url = dbManga.user_cover ?: "",
                                                    originalArtwork =
                                                    dbManga.thumbnail_url
                                                        ?: MdConstants.noCoverUrl,
                                                ),
                                            )
                                        }
                                        .toPersistentList(),
                                )
                            }
                            .toMap()
                            .toImmutableMap()
                    _displayScreenState.update {
                        it.copy(
                            allDisplayManga = DisplayScreenContent.Graded(newDisplayManga),
                            filteredDisplayManga =
                            DisplayScreenContent.Graded(
                                newDisplayManga.filterByVisibility(preferences),
                            ),
                        )
                    }
                }
                else -> {
                    val allDisplayManga =
                        (_displayScreenState.value.allDisplayManga as DisplayScreenContent.List)
                            .manga
                    val newDisplayManga = allDisplayManga.resync(db).unique().toPersistentList()
                    _displayScreenState.update {
                        it.copy(
                            allDisplayManga = DisplayScreenContent.List(newDisplayManga),
                            filteredDisplayManga =
                            DisplayScreenContent.List(
                                newDisplayManga.filterVisibility(preferences).toPersistentList(),
                            ),
                        )
                    }
                }
            }
        }
    }
}

fun ImmutableMap<Int, PersistentList<DisplayManga>>.filterByVisibility(
    prefs: PreferencesHelper,
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
