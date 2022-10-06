package eu.kanade.tachiyomi.ui.source.browse

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.launchIO
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
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
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.network.message
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseComposePresenter(
    private val browseRepository: BrowseRepository = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<MangaDetailController>() {

    private val _browseScreenState = MutableStateFlow(
        BrowseScreenState(
            isList = preferences.browseAsList().get(),
            showLibraryEntries = preferences.browseShowLibrary().get(),
            outlineCovers = preferences.outlineOnCovers().get(),
            isComfortableGrid = preferences.libraryLayout().get() == 2,
            rawColumnCount = preferences.gridSize().get(),
            promptForCategories = preferences.defaultCategory() == -1,
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
                val displayMangaHolder = it.displayMangaHolder.copy(displayManga = (it.displayMangaHolder.displayManga + items).toImmutableList())
                it.copy(isLoading = false, displayMangaHolder = displayMangaHolder, page = newKey, endReached = hasNexPage)
            }
        },
    )

    override fun onCreate() {
        super.onCreate()

        getHomepage()

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
            preferences.browseShowLibrary().asFlow().collectLatest {
                _browseScreenState.update {
                    it.copy(homePageManga = it.homePageManga.updateVisibility())
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
                    state.copy(homePageManga = it.updateVisibility(), isLoading = false)
                }
            }
        }
    }

    private fun getFollows() {
        presenterScope.launch {
            if (_browseScreenState.value.displayMangaHolder.browseScreenType != BrowseScreenType.Follows) {
                _browseScreenState.update { state ->
                    state.copy(isLoading = true)
                }
                browseRepository.getFollows().onFailure {
                    _browseScreenState.update { state ->
                        state.copy(error = it.message(), isLoading = false)
                    }
                }.onSuccess {
                    _browseScreenState.update { state ->
                        state.copy(displayMangaHolder = DisplayMangaHolder(BrowseScreenType.Follows, it.updateVisibility().toImmutableList()), isLoading = false)
                    }
                }
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

            updateDisplayManga(mangaId, editManga.favorite, browseScreenType)

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

    private fun updateDisplayManga(mangaId: Long, favorite: Boolean, browseScreenType: BrowseScreenType) {
        presenterScope.launch {
            when (browseScreenType) {
                BrowseScreenType.Homepage -> {
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
                else -> {
                    val index = _browseScreenState.value.displayMangaHolder.displayManga.indexOfFirst { it.mangaId == mangaId }
                    val tempList = _browseScreenState.value.displayMangaHolder.displayManga.toMutableList()
                    val tempDisplayManga = tempList[index].copy(inLibrary = favorite)
                    tempList[index] = tempDisplayManga
                    _browseScreenState.update {
                        it.copy(
                            displayMangaHolder = it.displayMangaHolder.copy(displayManga = tempList.toPersistentList()),
                        )
                    }
                }

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
        presenterScope.launch {
            preferences.browseAsList().set(!browseScreenState.value.isList)
            _browseScreenState.update {
                it.copy(isList = !it.isList)
            }
        }
    }

    fun changeScreenType(browseScreenType: BrowseScreenType) {
        presenterScope.launch {
            when (browseScreenType) {
                BrowseScreenType.Follows -> getFollows()
                else -> Unit
            }
            _browseScreenState.update {
                it.copy(screenType = browseScreenType)
            }
        }
    }

    fun switchLibraryVisibility() {
        presenterScope.launch {
            val showEntries = !browseScreenState.value.showLibraryEntries
            preferences.browseShowLibrary().set(showEntries)
            _browseScreenState.update {
                it.copy(showLibraryEntries = showEntries)
            }
        }
    }

    /**
     * Updates the visibility of HomePageManga
     */
    private fun List<HomePageManga>.updateVisibility(): ImmutableList<HomePageManga> {
        return this.map { homePageManga ->
            homePageManga.copy(
                displayManga = homePageManga.displayManga.updateVisibility().toImmutableList(),
            )
        }.toImmutableList()
    }

    /**
     * Marks display manga as visible when show library entries is enabled, otherwise hides library entries
     */
    private fun List<DisplayManga>.updateVisibility(): List<DisplayManga> {
        return this.map { displayManga ->
            when (preferences.browseShowLibrary().get()) {
                true -> {
                    displayManga.copy(isVisible = true)
                }
                false -> {
                    displayManga.copy(isVisible = !displayManga.inLibrary)
                }
            }
        }
    }
}
