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
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.uuid
import org.nekomanga.domain.network.ResultError
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayPresenter(
    private val displayScreenType: DisplayScreenType,
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
                title = (displayScreenType as? DisplayScreenType.List)?.title ?: "",
                titleRes = displayScreenType.titleRes,
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                isComfortableGrid = libraryPreferences.layoutLegacy().get() == 2,
                rawColumnCount = libraryPreferences.gridSize().get(),
                libraryEntryVisibility = preferences.browseDisplayMode().get(),
            )
        )
    val displayScreenState: StateFlow<DisplayScreenState> = _displayScreenState.asStateFlow()

    private val paginator by lazy {
        DefaultPaginator(
            initialKey = _displayScreenState.value.page,
            onLoadUpdated = {
                _displayScreenState.update { state -> state.copy(isPageLoading = it) }
            },
            onRequest = { nextPage -> displayRepository.getPage(nextPage, displayScreenType) },
            getNextKey = { _displayScreenState.value.page + 1 },
            onError = { resultError ->
                _displayScreenState.update {
                    it.copy(
                        isPageLoading = false,
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
                        (_displayScreenState.value.allDisplayManga.getOrElse(0) {
                                persistentListOf()
                            } + items)
                            .distinct()
                    val mangaMap =
                        if (paginator.isRefreshing) {
                            mapOf(0 to allDisplayManga.toPersistentList())
                        } else {
                            _displayScreenState.value.allDisplayManga.toMutableMap().apply {
                                this[0] = allDisplayManga.toPersistentList()
                            }
                        }

                    it.copy(
                        isPageLoading = false,
                        page = newKey,
                        endReached = !hasNextPage,
                        allDisplayManga = mangaMap.toImmutableMap(),
                        filteredDisplayManga =
                            _displayScreenState.value.filteredDisplayManga
                                .toMutableMap()
                                .apply {
                                    this[0] =
                                        allDisplayManga
                                            .filterVisibility(preferences)
                                            .toPersistentList()
                                }
                                .toImmutableMap(),
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
                        filteredDisplayManga = it.allDisplayManga.filterVisibility(preferences),
                    )
                }
            }
        }
    }

    fun refresh() {
        when (displayScreenType) {
            is DisplayScreenType.Similar -> getSimilarManga(true)
            else -> loadNextItems(true)
        }
    }

    private fun getSimilarManga(forceRefresh: Boolean = false) {
        presenterScope.launch {
            val mangaId = (displayScreenType as DisplayScreenType.Similar).mangaId
            val manga = db.getManga(mangaId).executeAsBlocking() ?: return@launch
            _displayScreenState.update {
                it.copy(
                    isRefreshing = true,
                    allDisplayManga = persistentMapOf(),
                    filteredDisplayManga = persistentMapOf(),
                )
            }

            val list = displayRepository.fetchSimilar(manga.uuid(), forceRefresh)
            val allDisplayManga =
                list
                    .associate { group -> group.type to group.manga.toPersistentList() }
                    .toImmutableMap()
            _displayScreenState.update {
                it.copy(
                    isRefreshing = false,
                    allDisplayManga = allDisplayManga,
                    filteredDisplayManga = allDisplayManga.filterVisibility(preferences),
                )
            }
        }
    }

    fun loadNextItems(isRefreshing: Boolean = false) {
        presenterScope.launch {
            if (isRefreshing) {
                paginator.reset()
                _displayScreenState.update {
                    it.copy(
                        allDisplayManga = persistentMapOf(),
                        filteredDisplayManga = persistentMapOf(),
                    )
                }
            }
            paginator.loadNextItems(isRefreshing)
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
            val listOfKeyIndex =
                _displayScreenState.value.allDisplayManga.mapNotNull { entry ->
                    val tempListIndex = entry.value.indexOfFirst { it.mangaId == mangaId }
                    when (tempListIndex == -1) {
                        true -> null
                        false -> entry.key to tempListIndex
                    }
                }

            val tempMap = _displayScreenState.value.allDisplayManga.toMutableMap()

            listOfKeyIndex.forEach { pair ->
                val mapKey = pair.first
                val mangaIndex = pair.second
                val tempList = _displayScreenState.value.allDisplayManga[mapKey]!!.toMutableList()
                val tempDisplayManga = tempList[mangaIndex].copy(inLibrary = favorite)
                tempList[mangaIndex] = tempDisplayManga

                tempMap[mapKey] = tempList.toPersistentList()
            }

            _displayScreenState.update {
                it.copy(
                    allDisplayManga = tempMap.toImmutableMap(),
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
            when (displayScreenType) {
                is DisplayScreenType.Similar -> updateCovers()
                else -> {
                    val newDisplayManga =
                        _displayScreenState.value.allDisplayManga
                            .map { entry ->
                                Pair(entry.key, entry.value.resync(db).unique().toPersistentList())
                            }
                            .toMap()
                            .toImmutableMap()
                    _displayScreenState.update {
                        it.copy(
                            allDisplayManga = newDisplayManga,
                            filteredDisplayManga = newDisplayManga.filterVisibility(preferences),
                        )
                    }
                }
            }
        }
    }

    private fun updateCovers() {
        presenterScope.launch {
            val newDisplayManga =
                _displayScreenState.value.allDisplayManga
                    .map { entry ->
                        Pair(
                            entry.key,
                            entry.value
                                .map {
                                    val dbManga = db.getManga(it.mangaId).executeAsBlocking()!!
                                    it.copy(
                                        currentArtwork =
                                            it.currentArtwork.copy(
                                                url = dbManga.user_cover ?: "",
                                                originalArtwork =
                                                    dbManga.thumbnail_url ?: MdConstants.noCoverUrl,
                                            )
                                    )
                                }
                                .toPersistentList(),
                        )
                    }
                    .toMap()
                    .toImmutableMap()
            _displayScreenState.update {
                it.copy(
                    allDisplayManga = newDisplayManga,
                    filteredDisplayManga = newDisplayManga.filterVisibility(preferences),
                )
            }
        }
    }

    companion object {
        const val MANGA_EXTRA = "manga"
    }
}
