package eu.kanade.tachiyomi.ui.source.latest

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
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

    lateinit var mangaList: Flow<PagingData<DisplayManga>>

    override fun onCreate() {
        super.onCreate()
        mangaList = Pager(PagingConfig(pageSize = 20)) {
            LatestPagingSource(latestRepository)
        }.flow.cachedIn(presenterScope)


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
}

