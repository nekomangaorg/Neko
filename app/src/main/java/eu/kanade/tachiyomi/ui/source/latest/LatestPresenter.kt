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

class LatestPresenter(
    private val latestRepository: LatestRepository = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<LatestController>() {

    private val _latestScreenState = MutableStateFlow(
        LatestScreenState(
            isList = preferences.browseAsList().get(),
            outlineCovers = preferences.outlineOnCovers().get(),
            isComfortableGrid = preferences.libraryLayout().get() == 2,
            rawColumnCount = preferences.gridSize().get(),
            promptForCategories = preferences.defaultCategory() == -1,
        ),
    )
    val latestScreenState: StateFlow<LatestScreenState> = _latestScreenState.asStateFlow()

    private val paginator = DefaultPaginator(
        initialKey = _latestScreenState.value.page,
        onLoadUpdated = {
            _latestScreenState.update { state ->
                state.copy(isLoading = it)
            }
        },
        onRequest = { nextPage ->
            latestRepository.getPage(nextPage)
        },
        getNextKey = {
            _latestScreenState.value.page + 1
        },
        onError = { resultError ->
            _latestScreenState.update {
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
            _latestScreenState.update {
                it.copy(displayManga = (_latestScreenState.value.displayManga + items).toImmutableList(), page = newKey, endReached = hasNexPage)
            }
        },
    )

    override fun onCreate() {
        super.onCreate()

        loadNextItems()

        presenterScope.launch {
            if (latestScreenState.value.promptForCategories) {
                val categories = db.getCategories().executeAsBlocking()
                _latestScreenState.update {
                    it.copy(
                        categories = categories.map { category -> category.toCategoryItem() }.toImmutableList(),
                    )
                }
            }
        }
        presenterScope.launch {
            preferences.browseAsList().asFlow().collectLatest {
                _latestScreenState.update { state ->
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
                    _latestScreenState.value.categories.firstOrNull {
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
            val index = _latestScreenState.value.displayManga.indexOfFirst { it.mangaId == mangaId }
            val tempList = _latestScreenState.value.displayManga.toMutableList()
            val tempDisplayManga = tempList[index].copy(inLibrary = favorite)
            tempList[index] = tempDisplayManga
            _latestScreenState.update {
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
            _latestScreenState.update {
                it.copy(categories = db.getCategories().executeAsBlocking().map { category -> category.toCategoryItem() }.toImmutableList())
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!latestScreenState.value.isList)
    }

    fun updateCovers() {
        if (isScopeInitialized) {
            presenterScope.launch {
                val newDisplayManga = _latestScreenState.value.displayManga.map {
                    val dbManga = db.getManga(it.mangaId).executeOnIO()!!
                    it.copy(currentArtwork = it.currentArtwork.copy(url = dbManga.user_cover ?: "", originalArtwork = dbManga.thumbnail_url ?: MdConstants.noCoverUrl))
                }.toImmutableList()
                _latestScreenState.update {
                    it.copy(displayManga = newDisplayManga)
                }
            }
        }
    }
}
