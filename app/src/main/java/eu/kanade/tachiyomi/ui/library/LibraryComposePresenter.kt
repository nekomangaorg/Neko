package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.toLibraryMangaItem
import kotlin.collections.map
import kotlin.collections.toSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.LibraryMangaItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryComposePresenter(
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    val securityPreferences: SecurityPreferences = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<LibraryComposeController>() {
    private val _libraryScreenState =
        MutableStateFlow(
            LibraryScreenState(
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                incognitoMode = securityPreferences.incognitoMode().get(),
            )
        )

    val libraryScreenState: StateFlow<LibraryScreenState> = _libraryScreenState.asStateFlow()

    fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        flow7: Flow<T7>,
        transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
    ): Flow<R> =
        combine(
            combine(flow, flow2, flow3, ::Triple),
            combine(flow4, flow5, ::Pair),
            combine(flow6, flow7, ::Pair),
        ) { t1, t2, t3 ->
            transform(t1.first, t1.second, t1.third, t2.first, t2.second, t3.first, t3.second)
        }

    override fun onCreate() {
        super.onCreate()

        presenterScope.launchIO {
            combine(
                    libraryMangaListFlow(),
                    categoryListFlow(),
                    libraryPreferences.collapsedCategories().changes(),
                    // unreadBadgesFlow, //downloadBadgesFlow, unread/download // maybe just their
                    // own flows watching the libraryScreenState
                    libraryPreferences.sortingMode().changes(),
                    libraryPreferences.layout().changes(),
                    libraryPreferences.gridSize().changes(),
                    libraryPreferences.groupBy().changes(),
                ) {
                    libraryMangaList,
                    categoryList,
                    collapsedCategories,
                    sortingMode,
                    layout,
                    gridSize,
                    groupBy ->
                    val collapsedCategorySet =
                        collapsedCategories.mapNotNull { it.toIntOrNull() }.toSet()

                    val removeArticles = libraryPreferences.removeArticles().get()

                    val libraryViewType =
                        when (layout) {
                            2 -> LibraryViewType.ComfortableGrid(rawColumnCount = gridSize)
                            1 -> LibraryViewType.CompactGrid(rawColumnCount = gridSize)
                            else -> LibraryViewType.List
                        }

                    //  Group by categories, or dynamic categories
                    // libraryPreferences.sortingMode().get(),
                    // libraryPreferences.sortAscending().get(),
                    //
                    // update

                    // lookup unread count then update
                    // lookup download count then update if enabled

                    /*     val allCategoryItem =
                    CategoryItem(
                        id = -1,
                        name = "All",
                        order = -1,
                        mangaSort =
                            (LibrarySort.valueOf(libraryPreferences.sortingMode().get())
                                    ?: LibrarySort.Title)
                                .categoryValue,
                        isDynamic = true,
                    )*/

                    /* val allLibraryCategoryItem =
                        LibraryCategoryItem(
                            categoryItem = allCategoryItem,
                            libraryMangaList.toPersistentList(),
                        )
                    persistentListOf(allLibraryCategoryItem)*/

                    val items =
                        if (libraryMangaList.toPersistentList().isNotEmpty()) {
                            val mangaMap = libraryMangaList.groupBy { it.category }.toMap()

                            categoryList
                                .mapNotNull { categoryItem ->
                                    if (categoryItem.isSystemCategory) {
                                        return@mapNotNull null
                                    }

                                    val updatedCategoryItem =
                                        categoryItem.copy(
                                            isHidden = categoryItem.id in collapsedCategorySet
                                        )

                                    val unsortedMangaList =
                                        mangaMap[categoryItem.id]?.toPersistentList()
                                            ?: persistentListOf()

                                    val sortedMangaList =
                                        unsortedMangaList.sortedWith(
                                            LibraryMangaItemComparator(
                                                librarySort = categoryItem.sortOrder,
                                                removeArticles = removeArticles,
                                                lastReadMapFn = {
                                                    var counter = 0
                                                    db.getLastReadManga()
                                                        .executeAsBlocking()
                                                        .associate { it.id!! to counter++ }
                                                },
                                                lastFetchMapFn = {
                                                    var counter = 0
                                                    db.getLastFetchedManga()
                                                        .executeAsBlocking()
                                                        .associate { it.id!! to counter++ }
                                                },
                                            )
                                        )

                                    LibraryCategoryItem(
                                        categoryItem = updatedCategoryItem,
                                        libraryItems =
                                            if (updatedCategoryItem.isAscending)
                                                sortedMangaList.toPersistentList()
                                            else sortedMangaList.reversed().toPersistentList(),
                                    )
                                }
                                .toPersistentList()
                        } else {
                            persistentListOf()
                        }

                    LibraryViewItem(libraryViewType = libraryViewType, libraryCategoryItems = items)
                }
                .distinctUntilChanged()
                .collectLatest { libraryViewItem ->
                    _libraryScreenState.update {
                        it.copy(
                            libraryViewType = libraryViewItem.libraryViewType,
                            items = libraryViewItem.libraryCategoryItems,
                        )
                    }
                }
        }
    }

    fun libraryMangaListFlow(): Flow<List<LibraryMangaItem>> {
        return db.getLibraryMangaList().asFlow().map { dbManga ->
            dbManga.map { it.toLibraryMangaItem() }
        }
    }

    fun categoryListFlow(): Flow<List<CategoryItem>> {
        return db.getCategories().asFlow().map { dbCategories ->
            (listOf(Category.createDefault()) + dbCategories)
                .sortedBy { it.order }
                .map { it.toCategoryItem() }
        }
    }

    fun categoryItemClick(category: CategoryItem) {
        presenterScope.launchIO {
            val collapsedCategory =
                libraryPreferences
                    .collapsedCategories()
                    .get()
                    .mapNotNull { it.toIntOrNull() }
                    .toMutableSet()

            if (category.id in collapsedCategory) {
                collapsedCategory.remove(category.id)
            } else {
                collapsedCategory.add(category.id)
            }
            libraryPreferences
                .collapsedCategories()
                .set(collapsedCategory.map { it.toString() }.toSet())
        }
    }

    fun categoryItemLibrarySortClick(category: CategoryItem, librarySort: LibrarySort) {
        presenterScope.launchIO {
            val updatedDbCategory =
                when (category.sortOrder == librarySort) {
                    true -> category.toDbCategory(true)
                    false ->
                        category.copy(isAscending = false, sortOrder = librarySort).toDbCategory()
                }

            db.insertCategory(updatedDbCategory).executeOnIO()

            val updatedCategory = updatedDbCategory.toCategoryItem()

            val mutableItems = _libraryScreenState.value.items.toMutableList()
            val index =
                _libraryScreenState.value.items.indexOfFirst { it.categoryItem.id == category.id }
            if (index >= 0) {
                val libraryCategoryItem = mutableItems[index]
                val newSortedList =
                    if (category.sortOrder == librarySort) {
                        libraryCategoryItem.libraryItems.reversed().toPersistentList()
                    } else {
                        libraryCategoryItem.libraryItems
                            .sortedWith(
                                LibraryMangaItemComparator(
                                    librarySort = updatedCategory.sortOrder,
                                    removeArticles = libraryPreferences.removeArticles().get(),
                                    lastReadMapFn = {
                                        var counter = 0
                                        db.getLastReadManga().executeAsBlocking().associate {
                                            it.id!! to counter++
                                        }
                                    },
                                    lastFetchMapFn = {
                                        var counter = 0
                                        db.getLastFetchedManga().executeAsBlocking().associate {
                                            it.id!! to counter++
                                        }
                                    },
                                )
                            )
                            .toPersistentList()
                    }
                val updatedLibraryCategoryItem = LibraryCategoryItem(updatedCategory, newSortedList)
                mutableItems[index] = updatedLibraryCategoryItem

                _libraryScreenState.update { it.copy(items = mutableItems.toPersistentList()) }
            }
        }
    }

    private var searchJob: Job? = null

    fun toggleIncognitoMode() {
        presenterScope.launchIO { securityPreferences.incognitoMode().toggle() }
    }

    fun refreshing(start: Boolean) {
        presenterScope.launchIO { _libraryScreenState.update { it.copy(isRefreshing = start) } }
    }

    fun search(searchQuery: String?) {
        searchJob?.cancel()
        searchJob = presenterScope.launchIO {}
    }
}
