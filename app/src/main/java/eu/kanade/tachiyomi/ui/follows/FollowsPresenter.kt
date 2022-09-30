package eu.kanade.tachiyomi.ui.follows

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
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

/**
 * Presenter of [FollowsController]
 */
class FollowsPresenter(
    private val repo: FollowsRepository = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
) : BaseCoroutinePresenter<FollowsController>() {

    private val _followsScreenState = MutableStateFlow(
        FollowsScreenState(
            isLoading = true,
            isList = preferences.browseAsList().get(),
            outlineCovers = preferences.outlineOnCovers().get(),
            isComfortableGrid = preferences.libraryLayout().get() == 2,
            rawColumnCount = preferences.gridSize().get(),
            promptForCategories = preferences.defaultCategory() == -1,
        ),
    )
    val followsScreenState: StateFlow<FollowsScreenState> = _followsScreenState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        getFollows()

        presenterScope.launch {
            if (_followsScreenState.value.promptForCategories) {
                val categories = db.getCategories().executeAsBlocking()
                _followsScreenState.update {
                    it.copy(
                        categories = categories.map { category -> category.toCategoryItem() }.toImmutableList(),
                    )
                }
            }
        }
        presenterScope.launch {
            preferences.browseAsList().asFlow().collectLatest {
                _followsScreenState.update { state ->
                    state.copy(isList = it)
                }
            }
        }
    }

    fun getFollows() {
        presenterScope.launch {
            _followsScreenState.update {
                it.copy(isLoading = true, error = null)
            }

            val result = repo.fetchFollows()
            result.onFailure { resultError ->
                _followsScreenState.update {
                    it.copy(isLoading = false, error = resultError)
                }
            }.onSuccess { displayManga ->
                _followsScreenState.update {
                    it.copy(isLoading = false, displayManga = displayManga)
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
                    _followsScreenState.value.categories.firstOrNull {
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

            _followsScreenState.value.displayManga.onEach { entry ->
                if (mapKey == -1) {
                    val tempListIndex = entry.value.indexOfFirst { it.mangaId == mangaId }
                    if (tempListIndex != -1) {
                        mangaIndex = tempListIndex
                        mapKey = entry.key
                    }
                }
            }

            val tempList = _followsScreenState.value.displayManga[mapKey]!!.toMutableList()
            val tempDisplayManga = tempList[mangaIndex].copy(inLibrary = favorite)
            tempList[mangaIndex] = tempDisplayManga
            val tempMap = _followsScreenState.value.displayManga.toMutableMap()

            tempMap[mapKey] = tempList.toImmutableList()

            _followsScreenState.update {
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
            _followsScreenState.update {
                it.copy(categories = db.getCategories().executeAsBlocking().map { category -> category.toCategoryItem() }.toImmutableList())
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!followsScreenState.value.isList)
    }

    fun updateCovers() {
        if (isScopeInitialized) {
            presenterScope.launch {
                val newDisplayManga = _followsScreenState.value.displayManga.map { entry ->
                    Pair(
                        entry.key,
                        entry.value.map {
                            val dbManga = db.getManga(it.mangaId).executeOnIO()!!
                            it.copy(currentArtwork = it.currentArtwork.copy(url = dbManga.user_cover ?: "", originalArtwork = dbManga.thumbnail_url ?: MdConstants.noCoverUrl))
                        }.toImmutableList(),
                    )
                }.toMap().toImmutableMap()
                _followsScreenState.update {
                    it.copy(displayManga = newDisplayManga)
                }
            }
        }
    }
}
