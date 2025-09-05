package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastAny
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_AUTHOR
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_CONTENT
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_DEFAULT
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_LANGUAGE
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_STATUS
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_TAG
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_TRACK_STATUS
import eu.kanade.tachiyomi.ui.library.LibraryGroup.UNGROUPED
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.toLibraryMangaItem
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
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

    private val loggedServices by lazy {
        Injekt.get<TrackManager>().services.values.filter { it.isLogged() || it.isMdList() }
    }

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

    override fun onResume() {
        super.onResume()
        observeLibraryUpdates()
    }

    /** Save the current list to speed up loading later */
    override fun onDestroy() {
        super.onDestroy()
        lastLibraryCategoryItems = _libraryScreenState.value.items
    }

    override fun onCreate() {
        super.onCreate()
        presenterScope.launchIO {
            lastLibraryCategoryItems?.let { items ->
                val notAllCollapsed = items.fastAny { !it.categoryItem.isHidden }
                _libraryScreenState.update {
                    it.copy(
                        isFirstLoad = false,
                        items = items.toPersistentList(),
                        allCollapsed = !notAllCollapsed,
                    )
                }
            }
            lastLibraryCategoryItems = null
        }
        observeLibraryUpdates()
        preferenceUpdates()

        presenterScope.launchIO {
            // check if logged in
            val groupItems =
                mutableListOf(BY_DEFAULT, BY_TAG, BY_STATUS, BY_AUTHOR, BY_CONTENT, BY_LANGUAGE)
            if (loggedServices.isNotEmpty()) {
                groupItems.add(BY_TRACK_STATUS)
            }
            if (db.getCategories().executeAsBlocking().isNotEmpty()) {
                groupItems.add(UNGROUPED)
            }
            _libraryScreenState.update { it.copy(groupByOptions = groupItems.toPersistentList()) }
        }

        presenterScope.launchIO {
            combine(
                    libraryMangaListFlow(),
                    categoryListFlow(),
                    libraryPreferences.collapsedCategories().changes(),
                    libraryPreferences.collapsedDynamicCategories().changes(),
                    libraryPreferences.sortingMode().changes(),
                    libraryPreferences.sortAscending().changes(),
                    libraryPreferences.groupBy().changes(),
                ) {
                    libraryMangaList,
                    categoryList,
                    collapsedCategories,
                    collapsedDynamicCategories,
                    sortingMode,
                    sortAscending,
                    groupBy ->
                    val collapsedCategorySet =
                        collapsedCategories.mapNotNull { it.toIntOrNull() }.toSet()

                    _libraryScreenState.update { it.copy(currentGroupBy = groupBy) }

                    val removeArticles = libraryPreferences.removeArticles().get()

                    libraryPreferences.sortAscending().get()

                    val librarySort = LibrarySort.valueOf(sortingMode)

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

                    val unsortedLibraryCategoryItems =
                        when (groupBy) {
                            UNGROUPED -> {
                                groupByUngrouped(libraryMangaList, librarySort, sortAscending)
                            }
                            BY_AUTHOR,
                            BY_CONTENT,
                            BY_LANGUAGE,
                            BY_STATUS,
                            BY_TAG,
                            BY_TRACK_STATUS -> {
                                val mapOfLoggedInTrackStatus =
                                    if (groupBy == BY_TRACK_STATUS) {
                                        db.getAllTracks()
                                            .executeOnIO()
                                            .mapNotNull { track ->
                                                val trackService =
                                                    loggedServices.firstOrNull {
                                                        it.id == track.sync_id
                                                    }
                                                if (trackService == null) {
                                                    return@mapNotNull null
                                                }
                                                track.manga_id to
                                                    trackService.getGlobalStatus(track.status)
                                            }
                                            .groupBy({ it.first }, { it.second })
                                    } else {
                                        emptyMap()
                                    }

                                groupByDynamic(
                                    libraryMangaList = libraryMangaList,
                                    collapsedDynamicCategorySet = collapsedDynamicCategories,
                                    groupType = groupBy,
                                    sortOrder = librarySort,
                                    sortAscending = sortAscending,
                                    loggedInTrackStatus = mapOfLoggedInTrackStatus,
                                )
                            }
                            else -> {
                                /*BY_DEFAULT*/
                                groupByCategory(
                                    libraryMangaList,
                                    categoryList,
                                    collapsedCategorySet,
                                )
                            }
                        }

                    var allCollapsed = true

                    val items =
                        unsortedLibraryCategoryItems
                            .map { libraryCategoryItem ->
                                if (!libraryCategoryItem.categoryItem.isHidden) {
                                    allCollapsed = false
                                }
                                val sortedMangaList =
                                    libraryCategoryItem.libraryItems
                                        .distinctBy { it.displayManga.mangaId }
                                        .sortedWith(
                                            LibraryMangaItemComparator(
                                                librarySort =
                                                    libraryCategoryItem.categoryItem.sortOrder,
                                                removeArticles = removeArticles,
                                                mangaOrder =
                                                    libraryCategoryItem.categoryItem.mangaOrder,
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
                                libraryCategoryItem.copy(
                                    libraryItems =
                                        if (libraryCategoryItem.categoryItem.isAscending)
                                            sortedMangaList.toPersistentList()
                                        else sortedMangaList.reversed().toPersistentList()
                                )
                            }
                            .toPersistentList()

                    _libraryScreenState.update { it.copy(allCollapsed = allCollapsed) }

                    LibraryViewItem(libraryViewType = libraryViewType, libraryCategoryItems = items)
                }
                .distinctUntilChanged()
                .collectLatest { libraryViewItem ->
                    _libraryScreenState.update {
                        it.copy(
                            libraryViewType = libraryViewItem.libraryViewType,
                            items = libraryViewItem.libraryCategoryItems,
                            isFirstLoad = false,
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
            when (category.isDynamic) {
                true -> {
                    val collapsedDynamicCategorySet =
                        libraryPreferences.collapsedDynamicCategories().get().toMutableSet()
                    val dynamicName =
                        dynamicCategoryName(libraryPreferences.groupBy().get(), category.name)

                    if (dynamicName in collapsedDynamicCategorySet) {
                        collapsedDynamicCategorySet.remove(dynamicName)
                    } else {
                        collapsedDynamicCategorySet.add(dynamicName)
                    }
                    libraryPreferences
                        .collapsedDynamicCategories()
                        .set(collapsedDynamicCategorySet.toSet())
                }
                false -> {
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
        }
    }

    fun groupByClick(groupBy: Int) {
        presenterScope.launchIO { libraryPreferences.groupBy().set(groupBy) }
    }

    fun categoryItemLibrarySortClick(category: CategoryItem, librarySort: LibrarySort) {
        presenterScope.launchIO {
            if (category.isDynamic) {
                when (category.sortOrder == librarySort) {
                    true -> libraryPreferences.sortAscending().set(!category.isAscending)
                    false -> libraryPreferences.sortingMode().set(librarySort.mainValue)
                }
            } else {

                val updatedDbCategory =
                    when (category.sortOrder == librarySort) {
                        true -> category.toDbCategory(true)
                        false ->
                            category
                                .copy(isAscending = true, sortOrder = librarySort)
                                .toDbCategory()
                    }

                db.insertCategory(updatedDbCategory).executeOnIO()

                val updatedCategory = updatedDbCategory.toCategoryItem()

                val mutableItems = _libraryScreenState.value.items.toMutableList()
                val index =
                    _libraryScreenState.value.items.indexOfFirst {
                        it.categoryItem.id == category.id
                    }
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
    }

    fun groupByDynamic(
        libraryMangaList: List<LibraryMangaItem>,
        collapsedDynamicCategorySet: Set<String>,
        groupType: Int,
        sortOrder: LibrarySort,
        sortAscending: Boolean,
        loggedInTrackStatus: Map<Long, List<String>>,
    ): PersistentList<LibraryCategoryItem> {

        return libraryMangaList
            .flatMap { libraryMangaItem ->
                val grouping =
                    when (groupType) {
                        BY_AUTHOR -> libraryMangaItem.author
                        BY_CONTENT -> libraryMangaItem.contentRating
                        BY_LANGUAGE -> libraryMangaItem.language
                        BY_STATUS -> libraryMangaItem.status
                        BY_TAG -> libraryMangaItem.genre
                        BY_TRACK_STATUS -> {
                            loggedInTrackStatus[libraryMangaItem.displayManga.mangaId]
                                ?: listOf("Not tracked")
                        }
                        else -> libraryMangaItem.language
                    }

                grouping.map { text -> text to libraryMangaItem }
            }
            .groupBy({ (text, _) -> text }, { (_, libraryMangaItem) -> libraryMangaItem })
            .toList()
            .sortedBy { it.first }
            .mapIndexed { index, pair ->
                val id = index
                val categoryItem =
                    CategoryItem(
                        id = id,
                        sortOrder = sortOrder,
                        isAscending = sortAscending,
                        name = pair.first,
                        isHidden =
                            dynamicCategoryName(groupType, pair.first) in
                                collapsedDynamicCategorySet,
                        isDynamic = true,
                    )
                LibraryCategoryItem(
                    categoryItem = categoryItem,
                    libraryItems = pair.second.toPersistentList(),
                )
            }
            .toPersistentList()
    }

    private fun dynamicCategoryName(groupType: Int, categoryName: String): String {
        return groupType.toString() + dynamicCategorySplitter + categoryName
    }

    fun groupByUngrouped(
        libraryMangaList: List<LibraryMangaItem>,
        sortOrder: LibrarySort,
        isAscending: Boolean,
    ): PersistentList<LibraryCategoryItem> {

        val allCategoryItem =
            CategoryItem(
                id = -1,
                name = "All",
                order = -1,
                sortOrder = sortOrder,
                isAscending = isAscending,
                isDynamic = true,
            )

        return persistentListOf(
            LibraryCategoryItem(
                categoryItem = allCategoryItem,
                libraryItems = libraryMangaList.toPersistentList(),
            )
        )
    }

    fun groupByCategory(
        libraryMangaList: List<LibraryMangaItem>,
        categoryList: List<CategoryItem>,
        collapsedCategorySet: Set<Int>,
    ): List<LibraryCategoryItem> {
        return if (libraryMangaList.toPersistentList().isNotEmpty()) {
            val mangaMap = libraryMangaList.groupBy { it.category }.toMap()

            categoryList
                .mapNotNull { categoryItem ->
                    val unsortedMangaList =
                        mangaMap[categoryItem.id]?.toPersistentList() ?: persistentListOf()

                    if (categoryItem.isSystemCategory && unsortedMangaList.isEmpty()) {
                        return@mapNotNull null
                    }

                    val updatedCategoryItem =
                        categoryItem.copy(isHidden = categoryItem.id in collapsedCategorySet)

                    LibraryCategoryItem(
                        categoryItem = updatedCategoryItem,
                        libraryItems = unsortedMangaList.toPersistentList(),
                    )
                }
                .toPersistentList()
        } else {
            persistentListOf()
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

    /*    private fun getLibraryItemPreferencesFlow(): Flow<ItemPreferences> {
        return combine(
            libraryPreferences.downloadBadge().changes(),
            libraryPreferences.unreadBadge().changes(),
            libraryPreferences.localBadge().changes(),
            libraryPreferences.languageBadge().changes(),
            libraryPreferences.autoUpdateMangaRestrictions().changes(),
            libraryPreferences.filterDownloaded().changes(),
            libraryPreferences.filterUnread().changes(),
            libraryPreferences.filterStarted().changes(),
            libraryPreferences.filterBookmarked().changes(),
            libraryPreferences.filterCompleted().changes(),
            libraryPreferences.filterIntervalCustom().changes(),
        ) {
            ItemPreferences(
                downloadBadge = it[0] as Boolean,
                unreadBadge = it[1] as Boolean,
                localBadge = it[2] as Boolean,
                languageBadge = it[3] as Boolean,
                skipOutsideReleasePeriod = LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in (it[4] as Set<*>),
                globalFilterDownloaded = it[5] as Boolean,
                filterDownloaded = it[6] as TriState,
                filterUnread = it[7] as TriState,
                filterStarted = it[8] as TriState,
                filterBookmarked = it[9] as TriState,
                filterCompleted = it[10] as TriState,
                filterIntervalCustom = it[11] as TriState,
            )
        }
    }*/

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

    fun collapseExpandAllCategories() {
        presenterScope.launchIO {
            val updatedAllCollapsed = !_libraryScreenState.value.allCollapsed
            val categoryItems = _libraryScreenState.value.items.map { it.categoryItem }
            if (categoryItems.isEmpty()) {
                return@launchIO
            }

            when (categoryItems[0].isDynamic) {
                true -> {

                    val collapsedDynamicCategorySet =
                        if (!updatedAllCollapsed) {
                            emptySet()
                        } else {
                            val libraryGroupBy = libraryPreferences.groupBy().get()
                            categoryItems
                                .map { dynamicCategoryName(libraryGroupBy, it.name) }
                                .toSet()
                        }

                    libraryPreferences.collapsedDynamicCategories().set(collapsedDynamicCategorySet)
                }
                false -> {
                    val collapsedCategorySet =
                        if (!updatedAllCollapsed) {
                            emptySet()
                        } else {
                            categoryItems.map { it.id.toString() }.toSet()
                        }
                    libraryPreferences.collapsedCategories().set(collapsedCategorySet)
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

    companion object {
        private var lastLibraryCategoryItems: List<LibraryCategoryItem>? = null
        private const val dynamicCategorySplitter = "??\t??\t?"

        fun onLowMemory() {
            lastLibraryCategoryItems = null
        }
    }
}
