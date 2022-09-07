package eu.kanade.tachiyomi.ui.similar

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

/**
 * Presenter of [SimilarController]
 */
class SimilarPresenter(
    private val mangaUUID: String,
    private val repo: SimilarRepository = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
) : BaseCoroutinePresenter<SimilarController>() {

    private val _similarScreenState = MutableStateFlow(
        SimilarScreenState(
            isList = preferences.browseAsList().get(),
            outlineCovers = preferences.outlineOnCovers().get(),
            isComfortableGrid = preferences.libraryLayout().get() == 2,
            rawColumnCount = preferences.gridSize().get(),
            promptForCategories = preferences.defaultCategory() == -1,
        ),
    )

    val similarScreenState: StateFlow<SimilarScreenState> = _similarScreenState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        getSimilarManga()

        presenterScope.launch {
            if (similarScreenState.value.promptForCategories) {
                val categories = db.getCategories().executeAsBlocking()
                _similarScreenState.update {
                    it.copy(
                        categories = categories.map { category -> category.toCategoryItem() }.toImmutableList(),
                    )
                }
            }

        }
        presenterScope.launch {
            preferences.browseAsList().asFlow().collectLatest {
                _similarScreenState.update { state ->
                    state.copy(isList = it)
                }
            }
        }
    }

    fun refresh() {
        getSimilarManga(true)
    }

    private fun getSimilarManga(forceRefresh: Boolean = false) {
        presenterScope.launch {
            if (mangaUUID.isNotEmpty()) {

                _similarScreenState.update {
                    it.copy(isRefreshing = true, displayManga = persistentMapOf())
                }

                val list = repo.fetchSimilar(mangaUUID, forceRefresh)
                _similarScreenState.update {
                    it.copy(isRefreshing = false, displayManga = list.associate { it.type to it.manga.toImmutableList() }.toImmutableMap())
                }
            }
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
                    _similarScreenState.value.categories.firstOrNull {
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
            var mapKey = -1
            var mangaIndex = -1

            _similarScreenState.value.displayManga.onEach { entry ->
                if (mapKey == -1) {
                    val tempListIndex = entry.value.indexOfFirst { it.mangaId == mangaId }
                    if (tempListIndex != -1) {
                        mangaIndex = tempListIndex
                        mapKey = entry.key
                    }
                }
            }

            val tempList = _similarScreenState.value.displayManga[mapKey]!!.toMutableList()
            val tempDisplayManga = tempList[mangaIndex].copy(inLibrary = favorite)
            tempList[mangaIndex] = tempDisplayManga
            val tempMap = _similarScreenState.value.displayManga.toMutableMap()

            tempMap[mapKey] = tempList.toImmutableList()

            _similarScreenState.update {
                it.copy(
                    displayManga = tempMap.toImmutableMap(),
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
            _similarScreenState.update {
                it.copy(categories = db.getCategories().executeAsBlocking().map { category -> category.toCategoryItem() }.toImmutableList())
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!similarScreenState.value.isList)
    }
}
