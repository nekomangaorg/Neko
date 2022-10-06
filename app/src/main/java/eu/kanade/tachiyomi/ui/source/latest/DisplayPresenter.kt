package eu.kanade.tachiyomi.ui.source.latest

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.executeOnIO
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
import org.nekomanga.domain.network.ResultError
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayPresenter(
    displayScreenType: DisplayScreenType,
    private val displayRepository: DisplayRepository = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<DisplayController>() {

    private val _displayScreenState = MutableStateFlow(
        DisplayScreenState(
            isList = preferences.browseAsList().get(),
            title = (displayScreenType as? DisplayScreenType.List)?.title ?: "",
            titleRes = (displayScreenType as? DisplayScreenType.LatestChapters)?.titleRes ?: (displayScreenType as? DisplayScreenType.RecentlyAdded)?.titleRes,
            outlineCovers = preferences.outlineOnCovers().get(),
            isComfortableGrid = preferences.libraryLayout().get() == 2,
            rawColumnCount = preferences.gridSize().get(),
            promptForCategories = preferences.defaultCategory() == -1,
        ),
    )
    val displayScreenState: StateFlow<DisplayScreenState> = _displayScreenState.asStateFlow()

    private val paginator = DefaultPaginator(
        initialKey = _displayScreenState.value.page,
        onLoadUpdated = {
            _displayScreenState.update { state ->
                state.copy(isLoading = it)
            }
        },
        onRequest = { nextPage ->
            displayRepository.getPage(nextPage, displayScreenType)
        },
        getNextKey = {
            _displayScreenState.value.page + 1
        },
        onError = { resultError ->
            _displayScreenState.update {
                it.copy(
                    isLoading = false,
                    error = when (resultError) {
                        is ResultError.Generic -> resultError.errorString
                        else -> (resultError as ResultError.HttpError).message
                    },
                )
            }
        },
        onSuccess = { hasNextPage, items, newKey ->
            _displayScreenState.update {
                it.copy(displayManga = (_displayScreenState.value.displayManga + items).toImmutableList(), page = newKey, endReached = !hasNextPage)
            }
        },
    )

    override fun onCreate() {
        super.onCreate()

        loadNextItems()

        presenterScope.launch {
            if (displayScreenState.value.promptForCategories) {
                val categories = db.getCategories().executeAsBlocking()
                _displayScreenState.update {
                    it.copy(
                        categories = categories.map { category -> category.toCategoryItem() }.toImmutableList(),
                    )
                }
            }
        }
        presenterScope.launch {
            preferences.browseAsList().asFlow().collectLatest {
                _displayScreenState.update { state ->
                    state.copy(isList = it)
                }
            }
        }
    }

    fun loadNextItems() {
        presenterScope.launch {
            paginator.loadNextItems()
        }
    }

    fun toggleFavorite(mangaId: Long, categoryItems: List<CategoryItem>) {
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
                    _displayScreenState.value.categories.firstOrNull {
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
            val index = _displayScreenState.value.displayManga.indexOfFirst { it.mangaId == mangaId }
            val tempList = _displayScreenState.value.displayManga.toMutableList()
            val tempDisplayManga = tempList[index].copy(inLibrary = favorite)
            tempList[index] = tempDisplayManga
            _displayScreenState.update {
                it.copy(
                    displayManga = tempList.toImmutableList(),
                )
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
            _displayScreenState.update {
                it.copy(categories = db.getCategories().executeAsBlocking().map { category -> category.toCategoryItem() }.toImmutableList())
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!displayScreenState.value.isList)
    }

    fun updateMangaForChanges() {
        if (isScopeInitialized) {
            presenterScope.launch {
                val newDisplayManga = _displayScreenState.value.displayManga.map {
                    val dbManga = db.getManga(it.mangaId).executeOnIO()!!
                    it.copy(inLibrary = dbManga.favorite, currentArtwork = it.currentArtwork.copy(url = dbManga.user_cover ?: "", originalArtwork = dbManga.thumbnail_url ?: MdConstants.noCoverUrl))
                }.toImmutableList()
                _displayScreenState.update {
                    it.copy(displayManga = newDisplayManga)
                }
            }
        }
    }
}
