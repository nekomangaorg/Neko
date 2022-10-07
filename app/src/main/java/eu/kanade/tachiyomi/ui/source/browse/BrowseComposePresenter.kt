package eu.kanade.tachiyomi.ui.source.browse

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.filterVisibility
import eu.kanade.tachiyomi.util.resync
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.updateVisibility
import java.util.Date
import kotlinx.collections.immutable.toImmutableList
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
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.network.message
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseComposePresenter(
    val incomingQuery: String,
    private val browseRepository: BrowseRepository = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<BrowseComposeController>() {

    private val _browseScreenState = MutableStateFlow(
        BrowseScreenState(
            isList = preferences.browseAsList().get(),
            showLibraryEntries = preferences.browseShowLibrary().get(),
            outlineCovers = preferences.outlineOnCovers().get(),
            isComfortableGrid = preferences.libraryLayout().get() == 2,
            rawColumnCount = preferences.gridSize().get(),
            promptForCategories = preferences.defaultCategory() == -1,
            query = incomingQuery,
            screenType = BrowseScreenType.Homepage,
        ),
    )
    val browseScreenState: StateFlow<BrowseScreenState> = _browseScreenState.asStateFlow()

    private val paginator = DefaultPaginator(
        initialKey = _browseScreenState.value.page,
        onLoadUpdated = {
            _browseScreenState.update { state ->
                state.copy(isLoading = it)
            }
        },
        onRequest = { nextPage ->
            browseRepository.getSearchPage(nextPage, "", FilterList())
        },
        getNextKey = {
            _browseScreenState.value.page + 1
        },
        onError = { resultError ->
            _browseScreenState.update {
                it.copy(
                    isLoading = false,
                    error = when (resultError) {
                        is ResultError.Generic -> resultError.errorString
                        else -> (resultError as ResultError.HttpError).message
                    },
                )
            }
        },
        onSuccess = { hasNexPage, items, newKey ->
            _browseScreenState.update {
                val allDisplayManga = it.displayMangaHolder.allDisplayManga + items
                val displayMangaHolder =
                    it.displayMangaHolder.copy(allDisplayManga = allDisplayManga.toImmutableList(), filteredDisplayManga = allDisplayManga.filterVisibility(preferences).toImmutableList())
                it.copy(isLoading = false, displayMangaHolder = displayMangaHolder, page = newKey, endReached = hasNexPage)
            }
        },
    )

    override fun onCreate() {
        super.onCreate()

        if (browseScreenState.value.query.isNotBlank()) {
            getSearchPage()
        } else {
            getHomepage()
        }

        presenterScope.launch {
            _browseScreenState.update {
                it.copy(sideNavMode = SideNavMode.findByPrefValue(preferences.sideNavMode().get()), isLoggedIn = browseRepository.isLoggedIn())
            }
        }

        presenterScope.launch {
            if (browseScreenState.value.promptForCategories) {
                val categories = db.getCategories().executeAsBlocking()
                _browseScreenState.update {
                    it.copy(
                        categories = categories.map { category -> category.toCategoryItem() }.toPersistentList(),
                    )
                }
            }
        }
        presenterScope.launch {
            preferences.browseAsList().asFlow().collectLatest {
                _browseScreenState.update { state ->
                    state.copy(isList = it)
                }
            }
        }
        presenterScope.launch {
            preferences.browseShowLibrary().asFlow().collectLatest { bool ->
                _browseScreenState.update {
                    it.copy(showLibraryEntries = bool)
                }
                presenterScope.launch {
                    _browseScreenState.update {
                        it.copy(homePageManga = it.homePageManga.updateVisibility(preferences))
                    }
                }
                presenterScope.launch {
                    _browseScreenState.update {
                        it.copy(displayMangaHolder = it.displayMangaHolder.copy(filteredDisplayManga = it.displayMangaHolder.allDisplayManga.filterVisibility(preferences).toImmutableList()))
                    }
                }
            }
        }
    }

    fun loadNextItems() {
        presenterScope.launch {
            paginator.loadNextItems()
        }
    }

    private fun getHomepage() {
        presenterScope.launch {
            browseRepository.getHomePage().onFailure {
                _browseScreenState.update { state ->
                    state.copy(error = it.message(), isLoading = false)
                }
            }.onSuccess {
                _browseScreenState.update { state ->
                    state.copy(homePageManga = it.updateVisibility(preferences), isLoading = false)
                }
            }
        }
    }

    private fun getFollows(forceUpdate: Boolean) {
        presenterScope.launch {
            if (forceUpdate || _browseScreenState.value.displayMangaHolder.browseScreenType != BrowseScreenType.Follows) {
                _browseScreenState.update { state ->
                    state.copy(isLoading = true)
                }
                browseRepository.getFollows().onFailure {
                    _browseScreenState.update { state ->
                        state.copy(error = it.message(), isLoading = false)
                    }
                }.onSuccess {
                    _browseScreenState.update { state ->
                        state.copy(displayMangaHolder = DisplayMangaHolder(BrowseScreenType.Follows, it.toImmutableList(), it.filterVisibility(preferences).toImmutableList()), isLoading = false)
                    }
                }
            }
        }
    }

    fun getSearchPage(clearQuery: Boolean = false) {
        presenterScope.launchIO {
            _browseScreenState.update { state ->
                state.copy(isLoading = true)
            }

            val currentQuery = browseScreenState.value.query
            val deepLinkType = DeepLinkType.getDeepLinkType(currentQuery)
            val uuid = DeepLinkType.removePrefix(currentQuery, deepLinkType)

            when (deepLinkType) {
                DeepLinkType.None -> {
                    browseRepository.getSearchPage(browseScreenState.value.page, browseScreenState.value.query).onFailure {
                        _browseScreenState.update { state ->
                            state.copy(error = it.message(), isLoading = false, displayMangaHolder = DisplayMangaHolder(BrowseScreenType.Filter), screenType = BrowseScreenType.Filter)
                        }
                    }.onSuccess { (hasNextPage, displayMangaList) ->
                        _browseScreenState.update { state ->
                            state.copy(
                                displayMangaHolder = DisplayMangaHolder(
                                    BrowseScreenType.Filter,
                                    displayMangaList.toImmutableList(),
                                    displayMangaList.filterVisibility(preferences).toImmutableList(),
                                ),
                                isLoading = false, endReached = !hasNextPage,
                            )
                        }
                    }
                }
                DeepLinkType.Manga -> {
                    browseRepository.getDeepLinkManga(uuid).onFailure {
                        _browseScreenState.update { state ->
                            state.copy(error = it.message(), isLoading = false)
                        }
                    }.onSuccess { dm ->
                        _browseScreenState.update { it.copy(query = "") }
                        controller?.openManga(dm.mangaId)
                    }
                }
                else -> Unit
            }
        }
    }

    fun toggleFavorite(mangaId: Long, categoryItems: List<CategoryItem>, browseScreenType: BrowseScreenType) {
        presenterScope.launch {
            val editManga = db.getManga(mangaId).executeAsBlocking()!!
            editManga.apply {
                favorite = !favorite
                date_added = when (favorite) {
                    true -> Date().time
                    false -> 0
                }
            }
            db.insertManga(editManga).executeAsBlocking()

            updateDisplayManga(mangaId, editManga.favorite)

            if (editManga.favorite) {
                val defaultCategory = preferences.defaultCategory()

                if (categoryItems.isEmpty() && defaultCategory != -1) {
                    _browseScreenState.value.categories.firstOrNull {
                        defaultCategory == it.id
                    }?.let {
                        val categories = listOf(MangaCategory.create(editManga, it.toDbCategory()))
                        db.setMangaCategories(categories, listOf(editManga))
                    }
                } else if (categoryItems.isNotEmpty()) {
                    val categories = categoryItems.map { MangaCategory.create(editManga, it.toDbCategory()) }
                    db.setMangaCategories(categories, listOf(editManga))
                }
            }
        }
    }

    private fun updateDisplayManga(mangaId: Long, favorite: Boolean) {
        presenterScope.launch {
            val tempList = _browseScreenState.value.homePageManga.map { homePageManga ->
                val index = homePageManga.displayManga.indexOfFirst { it.mangaId == mangaId }
                if (index == -1) {
                    homePageManga
                } else {
                    val tempMangaList = homePageManga.displayManga.toMutableList()
                    val tempDisplayManga = tempMangaList[index].copy(inLibrary = favorite)
                    tempMangaList[index] = tempDisplayManga
                    homePageManga.copy(displayManga = tempMangaList.toImmutableList())
                }
            }.toImmutableList()
            _browseScreenState.update {
                it.copy(homePageManga = tempList)
            }
        }
        presenterScope.launch {
            val index = _browseScreenState.value.displayMangaHolder.allDisplayManga.indexOfFirst { it.mangaId == mangaId }
            if (index >= 0) {
                val tempList = _browseScreenState.value.displayMangaHolder.allDisplayManga.toMutableList()
                val tempDisplayManga = tempList[index].copy(inLibrary = favorite)
                tempList[index] = tempDisplayManga
                _browseScreenState.update {
                    it.copy(
                        displayMangaHolder = it.displayMangaHolder.copy(allDisplayManga = tempList.toPersistentList()),
                    )
                }

                val filteredIndex = _browseScreenState.value.displayMangaHolder.filteredDisplayManga.indexOfFirst { it.mangaId == mangaId }
                if (filteredIndex >= 0) {
                    val tempFilterList = _browseScreenState.value.displayMangaHolder.filteredDisplayManga.toMutableList()
                    tempFilterList[filteredIndex] = tempDisplayManga
                    _browseScreenState.update {
                        it.copy(
                            displayMangaHolder = it.displayMangaHolder.copy(filteredDisplayManga = tempFilterList.toPersistentList()),
                        )
                    }
                }
            }
        }
    }

    //TODO this eventually will take the list of filters
    fun filterChanged(searchQuery: String) {
        presenterScope.launch {
            _browseScreenState.update {
                it.copy(query = searchQuery)
            }
        }
    }

    /**
     * Add New Category
     */
    fun addNewCategory(newCategory: String) {
        presenterScope.launchIO {
            val category = Category.create(newCategory)
            db.insertCategory(category).executeAsBlocking()
            _browseScreenState.update {
                it.copy(categories = db.getCategories().executeAsBlocking().map { category -> category.toCategoryItem() }.toPersistentList())
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!browseScreenState.value.isList)
    }

    fun changeScreenType(browseScreenType: BrowseScreenType, forceUpdate: Boolean = false) {
        presenterScope.launch {
            when (browseScreenType) {
                BrowseScreenType.Follows -> getFollows(forceUpdate)
                BrowseScreenType.Filter -> getSearchPage()
                else -> Unit
            }
            _browseScreenState.update {
                it.copy(screenType = browseScreenType, error = null)
            }
        }
    }

    fun retry() {
        changeScreenType(browseScreenState.value.screenType, true)
    }

    fun switchLibraryVisibility() {
        preferences.browseShowLibrary().set(!browseScreenState.value.showLibraryEntries)
    }

    fun updateMangaForChanges() {
        if (isScopeInitialized) {
            presenterScope.launch {
                val newHomePageManga = _browseScreenState.value.homePageManga.resync(db).updateVisibility(preferences)
                _browseScreenState.update {
                    it.copy(homePageManga = newHomePageManga)
                }
            }
            presenterScope.launch {
                val allDisplayManga = _browseScreenState.value.displayMangaHolder.allDisplayManga.resync(db)
                _browseScreenState.update {
                    it.copy(
                        displayMangaHolder = it.displayMangaHolder.copy(
                            allDisplayManga = allDisplayManga.toImmutableList(),
                            filteredDisplayManga = allDisplayManga.filterVisibility(preferences).toImmutableList(),
                        ),
                    )
                }
            }
        }
    }
}
