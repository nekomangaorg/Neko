package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
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
import kotlinx.coroutines.flow.flow
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
    val downloadManager: DownloadManager = Injekt.get(),
) : BaseCoroutinePresenter<LibraryComposeController>() {
    private val _libraryScreenState = MutableStateFlow(LibraryScreenState())

    val libraryScreenState: StateFlow<LibraryScreenState> = _libraryScreenState.asStateFlow()

    /* fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
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
            combine(flow4, flow5, flow6::Triple),
            //combine(flow6, flow7, ::Pair),
        ) { t1, t2, */
    /*t3*/
    /* ->
    transform(t1.first, t1.second, t1.third, t2.first, t2.second, t2.third */
    /*t3.first, t3.second*/
    /*)
    }*/

    override fun onResume() {
        super.onResume()
        observeLibraryUpdates()
    }

    override fun onCreate() {
        super.onCreate()
        observeLibraryUpdates()
        preferenceUpdates()

        presenterScope.launchIO {
            combine(
                    libraryMangaListFlow(),
                    categoryListFlow(),
                    libraryPreferences.collapsedCategories().changes(),
                    libraryPreferences.sortingMode().changes(),
                    libraryPreferences.groupBy().changes(),
                ) { libraryMangaList, categoryList, collapsedCategories, sortingMode, groupBy ->
                    val collapsedCategorySet =
                        collapsedCategories.mapNotNull { it.toIntOrNull() }.toSet()

                    val removeArticles = libraryPreferences.removeArticles().get()

                    val layout = libraryPreferences.layout().get()
                    val gridSize = libraryPreferences.gridSize().get()

                    val libraryViewType =
                        when (layout) {
                            2 ->
                                LibraryViewType.Grid(
                                    rawColumnCount = gridSize,
                                    LibraryViewType.GridType.Comfortable,
                                )
                            1 ->
                                LibraryViewType.Grid(
                                    rawColumnCount = gridSize,
                                    LibraryViewType.GridType.Compact,
                                )
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
                                    val unsortedMangaList =
                                        mangaMap[categoryItem.id]?.toPersistentList()
                                            ?: persistentListOf()

                                    if (
                                        categoryItem.isSystemCategory && unsortedMangaList.isEmpty()
                                    ) {
                                        return@mapNotNull null
                                    }

                                    val updatedCategoryItem =
                                        categoryItem.copy(
                                            isHidden = categoryItem.id in collapsedCategorySet
                                        )

                                    val sortedMangaList =
                                        unsortedMangaList.sortedWith(
                                            LibraryMangaItemComparator(
                                                librarySort = categoryItem.sortOrder,
                                                removeArticles = removeArticles,
                                                mangaOrder = categoryItem.mangaOrder,
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
                    updateDownloadBadges()
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

    fun preferenceUpdates() {

        presenterScope.launchIO {
            securityPreferences.incognitoMode().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _libraryScreenState.update { it.copy(incognitoMode = enabled) }
            }
        }

        presenterScope.launchIO {
            libraryPreferences.outlineOnCovers().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _libraryScreenState.update { it.copy(outlineCovers = enabled) }
            }
        }

        presenterScope.launchIO {
            libraryPreferences.showDownloadBadge().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _libraryScreenState.update { it.copy(showDownloadBadges = enabled) }
            }
        }

        presenterScope.launchIO {
            libraryPreferences.unreadBadgeType().changes().distinctUntilChanged().collectLatest {
                type ->
                _libraryScreenState.update { it.copy(showUnreadBadges = type == 2) }
            }
        }

        presenterScope.launchIO {
            combine(
                    libraryPreferences.gridSize().changes(),
                    libraryPreferences.layout().changes(),
                ) { gridSize, layout ->
                    gridSize to layout
                }
                .distinctUntilChanged()
                .collectLatest { pair ->
                    val viewType =
                        when (pair.second) {
                            0 -> LibraryViewType.List
                            1 -> LibraryViewType.Grid(pair.first, LibraryViewType.GridType.Compact)
                            else ->
                                LibraryViewType.Grid(
                                    pair.first,
                                    LibraryViewType.GridType.Comfortable,
                                )
                        }
                    _libraryScreenState.update { it.copy(libraryViewType = viewType) }
                }
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
                        category.copy(isAscending = true, sortOrder = librarySort).toDbCategory()
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
                                    mangaOrder = updatedCategory.mangaOrder,
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
                val updatedLibraryCategoryItem =
                    LibraryCategoryItem(
                        updatedCategory,
                        mutableItems[index].isRefreshing,
                        newSortedList,
                    )
                mutableItems[index] = updatedLibraryCategoryItem

                _libraryScreenState.update { it.copy(items = mutableItems.toPersistentList()) }
            }
        }
    }

    fun dragAndDropManga(
        fromIndex: Int,
        toIndex: Int,
        category: CategoryItem,
        libraryMangaItem: LibraryMangaItem,
    ) {
        presenterScope.launchIO {
            val libraryCategoryItemIndex =
                _libraryScreenState.value.items.indexOfFirst { it.categoryItem.id == category.id }
            val libraryCategoryItem = _libraryScreenState.value.items[libraryCategoryItemIndex]
            val mutableLibraryItems = libraryCategoryItem.libraryItems.toMutableList()
            mutableLibraryItems[toIndex] = mutableLibraryItems[fromIndex]
            val dbCategory = category.toDbCategory()
            dbCategory.mangaOrder = mutableLibraryItems.map { item -> item.displayManga.mangaId }
            db.insertCategory(dbCategory).executeOnIO()
            val updatedLibraryCategoryItem =
                libraryCategoryItem.copy(libraryItems = mutableLibraryItems.toPersistentList())
            val mutableItems = _libraryScreenState.value.items.toMutableList()
            mutableItems[libraryCategoryItemIndex] = updatedLibraryCategoryItem
            _libraryScreenState.update { it.copy(items = mutableItems.toPersistentList()) }
        }
    }

    private var searchJob: Job? = null

    fun observeLibraryUpdates() {
        pausablePresenterScope.launchIO {
            flow {
                    while (true) {
                        _libraryScreenState.value.items.forEachIndexed { index, libraryCategoryItem
                            ->
                            val isInQueue =
                                LibraryUpdateJob.categoryInQueue(
                                    libraryCategoryItem.categoryItem.id
                                )
                            if (
                                (libraryCategoryItem.isRefreshing && !isInQueue) ||
                                    (!libraryCategoryItem.isRefreshing && isInQueue)
                            ) {
                                emit(index)
                            }
                        }
                    }
                }
                .collect { index ->
                    val mutableItems = _libraryScreenState.value.items.toMutableList()
                    mutableItems[index] =
                        mutableItems[index].copy(isRefreshing = !mutableItems[index].isRefreshing)
                    _libraryScreenState.update { it.copy(items = mutableItems.toPersistentList()) }
                }
        }
    }

    fun updateDownloadBadges() {
        presenterScope.launchIO {
            _libraryScreenState.value.items.forEachIndexed { index, libraryItem ->
                presenterScope.launchIO {
                    val items =
                        libraryItem.libraryItems
                            .map {
                                val dbManga = db.getManga(it.displayManga.mangaId).executeOnIO()!!
                                it.copy(downloadCount = downloadManager.getDownloadCount(dbManga))
                            }
                            .toPersistentList()
                    val mutableItemList = _libraryScreenState.value.items.toMutableList()
                    mutableItemList[index] = libraryItem.copy(libraryItems = items)
                    _libraryScreenState.update {
                        it.copy(items = mutableItemList.toPersistentList())
                    }
                }
            }
        }
    }

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
