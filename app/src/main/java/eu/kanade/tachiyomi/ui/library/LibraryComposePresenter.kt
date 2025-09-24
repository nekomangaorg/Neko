package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastAny
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
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
import eu.kanade.tachiyomi.ui.library.filter.FilterBookmarked
import eu.kanade.tachiyomi.ui.library.filter.FilterCompleted
import eu.kanade.tachiyomi.ui.library.filter.FilterDownloaded
import eu.kanade.tachiyomi.ui.library.filter.FilterMangaType
import eu.kanade.tachiyomi.ui.library.filter.FilterMerged
import eu.kanade.tachiyomi.ui.library.filter.FilterMissingChapters
import eu.kanade.tachiyomi.ui.library.filter.FilterTracked
import eu.kanade.tachiyomi.ui.library.filter.FilterUnavailable
import eu.kanade.tachiyomi.ui.library.filter.FilterUnread
import eu.kanade.tachiyomi.ui.library.filter.LibraryFilterType
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DownloadAction
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.toLibraryMangaItem
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import org.nekomanga.constants.Constants.SEARCH_DEBOUNCE_MILLIS
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.CategoryItem.Companion.ALL_CATEGORY
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.util.system.filterAsync
import org.nekomanga.util.system.mapAsync
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryComposePresenter(
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    val securityPreferences: SecurityPreferences = Injekt.get(),
    val mangadexPreferences: MangaDexPreferences = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    val chapterFilter: ChapterFilter = Injekt.get(),
) : BaseCoroutinePresenter<LibraryComposeController>() {

    private val _libraryScreenState = MutableStateFlow(LibraryScreenState())

    private val loggedServices by lazy {
        Injekt.get<TrackManager>().services.values.filter { it.isLogged() || it.isMdList() }
    }

    val manualTrigger = MutableSharedFlow<Unit>()

    val libraryScreenState: StateFlow<LibraryScreenState> = _libraryScreenState.asStateFlow()

    fun <T1, T2, T3, T4, T5, T6, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
    ): Flow<R> =
        combine(combine(flow, flow2, flow3, ::Triple), combine(flow4, flow5, flow6, ::Triple)) {
            t1,
            t2 ->
            transform(t1.first, t1.second, t1.third, t2.first, t2.second, t2.third)
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
                    libraryScreenState
                        .map { it.searchQuery }
                        .distinctUntilChanged()
                        .debounce(SEARCH_DEBOUNCE_MILLIS),
                    libraryMangaListFlow(),
                    categoryListFlow(),
                    filterPreferencesFlow(),
                    trackMapFlow(),
                    libraryViewFlow(),
                ) {
                    searchQuery,
                    libraryMangaList,
                    categoryList,
                    libraryFilters,
                    trackMap,
                    libraryViewPreferences ->
                    val collapsedCategorySet =
                        libraryViewPreferences.collapsedCategories
                            .mapNotNull { it.toIntOrNull() }
                            .toSet()

                    val removeArticles = libraryPreferences.removeArticles().get()

                    libraryPreferences.sortAscending().get()

                    val layout = libraryPreferences.layout().get()
                    val gridSize = libraryPreferences.gridSize().get()

                    val unsortedLibraryCategoryItems =
                        when (libraryViewPreferences.groupBy) {
                            UNGROUPED -> {
                                groupByUngrouped(
                                    libraryMangaList,
                                    libraryViewPreferences.sortingMode,
                                    libraryViewPreferences.sortAscending,
                                )
                            }
                            BY_AUTHOR,
                            BY_CONTENT,
                            BY_LANGUAGE,
                            BY_STATUS,
                            BY_TAG,
                            BY_TRACK_STATUS -> {
                                groupByDynamic(
                                    libraryMangaList = libraryMangaList,
                                    collapsedDynamicCategorySet =
                                        libraryViewPreferences.collapsedDynamicCategories,
                                    groupType = libraryViewPreferences.groupBy,
                                    sortOrder = libraryViewPreferences.sortingMode,
                                    sortAscending = libraryViewPreferences.sortAscending,
                                    loggedInTrackStatus = trackMap,
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
                            .mapAsync { libraryCategoryItem ->
                                if (!libraryCategoryItem.categoryItem.isHidden) {
                                    allCollapsed = false
                                }
                                val lastReadMap =
                                    db.getLastReadManga()
                                        .executeAsBlocking()
                                        .withIndex()
                                        .associate { (index, manga) -> manga.id!! to index }
                                val lastFetchMap =
                                    db.getLastFetchedManga()
                                        .executeAsBlocking()
                                        .withIndex()
                                        .associate { (index, manga) -> manga.id!! to index }

                                val tempSortedList =
                                    libraryCategoryItem.libraryItems
                                        .distinctBy { it.displayManga.mangaId }
                                        .sortedWith(
                                            LibraryMangaItemComparator(
                                                librarySort =
                                                    libraryCategoryItem.categoryItem.sortOrder,
                                                removeArticles = removeArticles,
                                                mangaOrder =
                                                    libraryCategoryItem.categoryItem.mangaOrder,
                                                lastReadMap = lastReadMap,
                                                lastFetchMap = lastFetchMap,
                                            )
                                        )
                                val downloadCounts =
                                    downloadManager.getDownloadCounts(
                                        tempSortedList.map { it.displayManga.toDbManga() }
                                    )
                                val sortedMangaList =
                                    tempSortedList
                                        .map {
                                            it.copy(
                                                downloadCount =
                                                    downloadCounts[it.displayManga.mangaId] ?: 0,
                                                trackCount =
                                                    trackMap[it.displayManga.mangaId]?.size ?: 0,
                                            )
                                        }
                                        .applyFilters(libraryFilters, trackMap, searchQuery)
                                libraryCategoryItem.copy(
                                    libraryItems =
                                        if (libraryCategoryItem.categoryItem.isAscending)
                                            sortedMangaList.toPersistentList()
                                        else sortedMangaList.reversed().toPersistentList()
                                )
                            }
                            .toPersistentList()

                    _libraryScreenState.update { it.copy(allCollapsed = allCollapsed) }

                    LibraryViewItem(
                        libraryDisplayMode = layout,
                        libraryCategoryItems = items,
                        rawColumnCount = gridSize,
                        currentGroupBy = libraryViewPreferences.groupBy,
                        trackMap = trackMap.toPersistentMap(),
                        userCategories =
                            categoryList
                                .filterNot { category -> category.isSystemCategory }
                                .toPersistentList(),
                    )
                }
                .distinctUntilChanged()
                .collectLatest { libraryViewItem ->
                    _libraryScreenState.update {
                        it.copy(
                            libraryDisplayMode = libraryViewItem.libraryDisplayMode,
                            rawColumnCount = libraryViewItem.rawColumnCount,
                            items = libraryViewItem.libraryCategoryItems,
                            isFirstLoad = false,
                            currentGroupBy = libraryViewItem.currentGroupBy,
                            trackMap = libraryViewItem.trackMap,
                            userCategories = libraryViewItem.userCategories,
                        )
                    }
                }
        }
    }

    fun libraryMangaListFlow(): Flow<List<LibraryMangaItem>> {
        return db.getLibraryMangaList()
            .asFlow()
            .map { dbManga -> dbManga.map { it.toLibraryMangaItem() } }
            .distinctUntilChanged()
    }

    fun categoryListFlow(): Flow<List<CategoryItem>> {
        return combine(
                libraryPreferences.sortingMode().changes(),
                libraryPreferences.sortAscending().changes(),
                db.getCategories().asFlow(),
            ) { combinedArray ->
                val librarySort = LibrarySort.valueOf(combinedArray[0] as Int)
                val defaultCategory = Category.createSystemCategory().toCategoryItem()
                val updatedDefaultCategory =
                    defaultCategory.copy(
                        sortOrder = librarySort,
                        isAscending = combinedArray[1] as Boolean,
                    )
                val dbCategories = combinedArray[2] as List<Category>
                (listOf(updatedDefaultCategory) +
                        dbCategories.map { dbCategory -> dbCategory.toCategoryItem() })
                    .sortedBy { it.order }
            }
            .distinctUntilChanged()
    }

    fun trackMapFlow(): Flow<Map<Long, List<String>>> {
        return db.getAllTracks()
            .asFlow()
            .mapNotNull { tracks ->
                tracks
                    .mapNotNull { track ->
                        val trackService = loggedServices.firstOrNull { it.id == track.sync_id }
                        if (trackService == null) {
                            return@mapNotNull null
                        }
                        track.manga_id to trackService.getGlobalStatus(track.status)
                    }
                    .groupBy({ it.first }, { it.second })
            }
            .distinctUntilChanged()
    }

    fun preferenceUpdates() {

        presenterScope.launchIO {
            libraryPreferences
                .showStartReadingButton()
                .changes()
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    _libraryScreenState.update { it.copy(showStartReadingButton = enabled) }
                }
        }

        presenterScope.launchIO {
            mangadexPreferences
                .includeUnavailableChapters()
                .changes()
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    _libraryScreenState.update { it.copy(showUnavailableFilter = enabled) }
                }
        }

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
            libraryPreferences.showUnreadBadge().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _libraryScreenState.update { it.copy(showUnreadBadges = enabled) }
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
                    _libraryScreenState.update {
                        it.copy(libraryDisplayMode = pair.second, rawColumnCount = pair.first)
                    }
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun libraryViewFlow(): Flow<LibraryViewPreferences> {
        return combine(
                libraryPreferences.collapsedCategories().changes(),
                libraryPreferences.collapsedDynamicCategories().changes(),
                libraryPreferences.sortingMode().changes(),
                libraryPreferences.sortAscending().changes(),
                libraryPreferences.groupBy().changes(),
                libraryPreferences.showDownloadBadge().changes(),
            ) {
                val librarySort = LibrarySort.valueOf(it[2] as Int)

                LibraryViewPreferences(
                    collapsedCategories = it[0] as Set<String>,
                    collapsedDynamicCategories = it[1] as Set<String>,
                    sortingMode = librarySort,
                    sortAscending = it[3] as Boolean,
                    groupBy = it[4] as Int,
                    showDownloadBadges = it[5] as Boolean,
                )
            }
            .distinctUntilChanged()
    }

    private fun filterPreferencesFlow(): Flow<LibraryFilters> {
        return combine(
            libraryPreferences.filterBookmarked().changes(),
            libraryPreferences.filterCompleted().changes(),
            libraryPreferences.filterDownloaded().changes(),
            libraryPreferences.filterMangaType().changes(),
            libraryPreferences.filterMerged().changes(),
            libraryPreferences.filterMissingChapters().changes(),
            libraryPreferences.filterTracked().changes(),
            libraryPreferences.filterUnavailable().changes(),
            libraryPreferences.filterUnread().changes(),
        ) {
            LibraryFilters(
                filterBookmarked = it[0] as FilterBookmarked,
                filterCompleted = it[1] as FilterCompleted,
                filterDownloaded = it[2] as FilterDownloaded,
                filterMangaType = it[3] as FilterMangaType,
                filterMerged = it[4] as FilterMerged,
                filterMissingChapters = it[5] as FilterMissingChapters,
                filterTracked = it[6] as FilterTracked,
                filterUnavailable = it[7] as FilterUnavailable,
                filterUnread = it[8] as FilterUnread,
            )
        }
    }

    private suspend fun List<LibraryMangaItem>.applyFilters(
        libraryFilters: LibraryFilters,
        trackMap: Map<Long, List<String>>,
        searchQuery: String?,
    ): List<LibraryMangaItem> {
        return this.filterAsync { libraryMangaItem ->
            val displayManga = libraryMangaItem.displayManga
            val bookmarkedCondition = libraryFilters.filterBookmarked.matches(libraryMangaItem)
            val completedCondition = libraryFilters.filterCompleted.matches(libraryMangaItem)
            val downloadedCondition = libraryFilters.filterDownloaded.matches(libraryMangaItem)
            val mangaTypeCondition = libraryFilters.filterMangaType.matches(libraryMangaItem)
            val mergedCondition = libraryFilters.filterMerged.matches(libraryMangaItem)
            val missingChaptersCondition =
                when (libraryFilters.filterTracked) {
                    FilterTracked.Inactive -> true
                    FilterTracked.NotTracked -> trackMap[displayManga.mangaId] == null
                    FilterTracked.Tracked -> trackMap[displayManga.mangaId] != null
                }
            val trackedCondition = libraryFilters.filterTracked.matches(libraryMangaItem)
            val unavailableCondition = libraryFilters.filterUnavailable.matches(libraryMangaItem)
            val unreadCondition = libraryFilters.filterUnread.matches(libraryMangaItem)

            val searchCondition = libraryMangaItem.matches(searchQuery)

            completedCondition &&
                downloadedCondition &&
                unreadCondition &&
                trackedCondition &&
                missingChaptersCondition &&
                bookmarkedCondition &&
                mergedCondition &&
                unavailableCondition &&
                mangaTypeCondition &&
                searchCondition
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

    fun libraryDisplayModeClick(libraryDisplayMode: LibraryDisplayMode) {
        presenterScope.launchIO { libraryPreferences.layout().set(libraryDisplayMode) }
    }

    fun rawColumnCountChanged(updatedColumnCount: Float) {
        presenterScope.launchIO { libraryPreferences.gridSize().set(updatedColumnCount) }
    }

    fun outlineCoversToggled() {
        presenterScope.launchIO { libraryPreferences.outlineOnCovers().toggle() }
    }

    fun downloadBadgesToggled() {
        presenterScope.launchIO { libraryPreferences.showDownloadBadge().toggle() }
    }

    fun unreadBadgesToggled() {
        presenterScope.launchIO { libraryPreferences.showUnreadBadge().toggle() }
    }

    fun startReadingButtonToggled() {
        presenterScope.launchIO { libraryPreferences.showStartReadingButton().toggle() }
    }

    fun clearActiveFilters() {
        presenterScope.launchIO {
            libraryPreferences.filterUnread().delete()
            libraryPreferences.filterDownloaded().delete()
            libraryPreferences.filterCompleted().delete()
            libraryPreferences.filterMangaType().delete()
            libraryPreferences.filterBookmarked().delete()
            libraryPreferences.filterMissingChapters().delete()
            libraryPreferences.filterMerged().delete()
            libraryPreferences.filterTracked().delete()
            libraryPreferences.filterUnavailable().delete()
        }
    }

    fun filterToggled(filter: LibraryFilterType) {
        presenterScope.launchIO {
            when (filter) {
                is FilterBookmarked -> libraryPreferences.filterBookmarked().set(filter)
                is FilterCompleted -> libraryPreferences.filterCompleted().set(filter)
                is FilterDownloaded -> libraryPreferences.filterDownloaded().set(filter)
                is FilterMangaType -> libraryPreferences.filterMangaType().set(filter)
                is FilterMerged -> libraryPreferences.filterMerged().set(filter)
                is FilterMissingChapters -> libraryPreferences.filterMissingChapters().set(filter)
                is FilterTracked -> libraryPreferences.filterTracked().set(filter)
                is FilterUnavailable -> libraryPreferences.filterUnavailable().set(filter)
                is FilterUnread -> libraryPreferences.filterUnread().set(filter)
            }
        }
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        presenterScope.launchIO {
            val category = Category.create(newCategory)
            category.order =
                (_libraryScreenState.value.userCategories.maxOfOrNull { it.order } ?: 0) + 1
            db.insertCategory(category).executeAsBlocking()
        }
    }

    fun editCategories(mangaList: List<DisplayManga>, categories: List<CategoryItem>) {
        presenterScope.launchIO {
            val dbCategories = categories.map { it.toDbCategory() }

            mangaList.map { manga ->
                val dbManga = db.getManga(manga.mangaId).executeOnIO()!!
                val mangaCategories = dbCategories.map { MangaCategory.create(dbManga, it) }
                launchIO { db.setMangaCategories(mangaCategories, listOf(dbManga)) }
            }
            clearSelectedManga()
            if (libraryPreferences.groupBy().get() == BY_DEFAULT) {
                libraryPreferences.groupBy().set(UNGROUPED)
                libraryPreferences.groupBy().set(BY_DEFAULT)
            }
        }
    }

    fun categoryItemLibrarySortClick(category: CategoryItem, librarySort: LibrarySort) {
        presenterScope.launchIO {
            if (category.isDynamic || category.isSystemCategory) {
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
                            val lastReadMap =
                                db.getLastReadManga().executeAsBlocking().withIndex().associate {
                                    (index, manga) ->
                                    manga.id!! to index
                                }
                            val lastFetchMap =
                                db.getLastFetchedManga()
                                    .executeAsBlocking()
                                    .withIndex()
                                    .associate { (index, manga) -> manga.id!! to index }
                            libraryCategoryItem.libraryItems
                                .sortedWith(
                                    LibraryMangaItemComparator(
                                        librarySort = updatedCategory.sortOrder,
                                        removeArticles = libraryPreferences.removeArticles().get(),
                                        mangaOrder = updatedCategory.mangaOrder,
                                        lastReadMap = lastReadMap,
                                        lastFetchMap = lastFetchMap,
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
        val groupingMap = mutableMapOf<String, MutableList<LibraryMangaItem>>()
        libraryMangaList.forEach { libraryMangaItem ->
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
            grouping.forEach { text ->
                groupingMap.getOrPut(text) { mutableListOf() }.add(libraryMangaItem)
            }
        }
        return groupingMap
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
                name = ALL_CATEGORY,
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

    fun observeLibraryUpdates() {

        pausablePresenterScope.launchIO {
            filterPreferencesFlow().distinctUntilChanged().collectLatest { libraryFilters ->
                _libraryScreenState.update {
                    it.copy(
                        libraryFilters = libraryFilters,
                        hasActiveFilters = libraryFilters.hasActiveFilter(),
                    )
                }
            }
        }

        pausablePresenterScope.launchIO {
            downloadManager
                .statusFlow()
                .catch { error -> TimberKt.e(error) }
                .collect { download ->
                    if (download.status == Download.State.DOWNLOADED) {
                        updateDownloadBadges(download.mangaItem.id)
                    }
                }
        }

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

    fun updateDownloadBadges(mangaId: Long) {
        presenterScope.launchIO {
            val currentItems = _libraryScreenState.value.items
            val categoryIndex =
                currentItems.indexOfFirst { libraryCategoryItem ->
                    libraryCategoryItem.libraryItems.any { it.displayManga.mangaId == mangaId }
                }
            if (categoryIndex == -1) return@launchIO

            val libraryCategoryItem = currentItems[categoryIndex]
            val mangaIndex =
                libraryCategoryItem.libraryItems.indexOfFirst { it.displayManga.mangaId == mangaId }
            if (mangaIndex == -1) return@launchIO

            val libraryMangaItem = libraryCategoryItem.libraryItems[mangaIndex]
            val dbManga = db.getManga(mangaId).executeOnIO() ?: return@launchIO
            val newDownloadCount = downloadManager.getDownloadCount(dbManga)

            if (libraryMangaItem.downloadCount == newDownloadCount) return@launchIO

            val newLibraryMangaItem = libraryMangaItem.copy(downloadCount = newDownloadCount)
            val newLibraryItems = libraryCategoryItem.libraryItems.toMutableList()
            newLibraryItems[mangaIndex] = newLibraryMangaItem

            val newLibraryCategoryItem =
                libraryCategoryItem.copy(libraryItems = newLibraryItems.toPersistentList())
            val newItems = currentItems.toMutableList()
            newItems[categoryIndex] = newLibraryCategoryItem

            _libraryScreenState.update { it.copy(items = newItems.toPersistentList()) }
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
        _libraryScreenState.update { it.copy(searchQuery = searchQuery) }
    }

    fun selectAllLibraryMangaItems(libraryMangaItems: List<LibraryMangaItem>) {
        presenterScope.launchIO {
            var currentSelected = _libraryScreenState.value.selectedItems.toList()
            currentSelected =
                if (libraryMangaItems.all { it in currentSelected }) {
                    currentSelected - libraryMangaItems
                } else {
                    currentSelected + libraryMangaItems
                }

            _libraryScreenState.update {
                it.copy(selectedItems = currentSelected.distinct().toPersistentList())
            }
        }
    }

    fun deleteSelectedLibraryMangaItems() {
        presenterScope.launchNonCancellable {
            var currentSelected = _libraryScreenState.value.selectedItems.toList()
            currentSelected.forEach { libraryMangaItem ->
                val dbManga = db.getManga(libraryMangaItem.displayManga.mangaId).executeOnIO()!!
                dbManga.favorite = false
                db.insertManga(dbManga).executeOnIO()
                coverCache.deleteFromCache(dbManga)
                downloadManager.deleteManga(dbManga)
            }
            _libraryScreenState.update { it.copy(selectedItems = persistentListOf()) }
        }
    }

    fun libraryItemLongClick(libraryMangaItem: LibraryMangaItem) {
        presenterScope.launchIO {
            val currentSelected = _libraryScreenState.value.selectedItems.toMutableList()
            val index =
                currentSelected.indexOfFirst {
                    it.displayManga.mangaId == libraryMangaItem.displayManga.mangaId
                }
            if (index >= 0) {
                currentSelected.removeAt(index)
            } else {
                val categoryItems =
                    db.getCategoriesForManga(libraryMangaItem.displayManga.mangaId)
                        .executeOnIO()
                        .map { it.toCategoryItem() }
                val copy = libraryMangaItem.copy(allCategories = categoryItems)
                currentSelected.add(copy)
            }

            _libraryScreenState.update {
                it.copy(selectedItems = currentSelected.distinct().toPersistentList())
            }
        }
    }

    fun downloadChapters(downloadAction: DownloadAction) {
        presenterScope.launchIO {
            val displayMangaList = _libraryScreenState.value.selectedItems.map { it.displayManga }
            clearSelectedManga()
            if (
                downloadAction == DownloadAction.Cancel ||
                    downloadAction == DownloadAction.Download ||
                    downloadAction == DownloadAction.Remove ||
                    downloadAction == DownloadAction.ImmediateDownload ||
                    downloadAction == DownloadAction.Remove
            ) {
                return@launchIO
            }

            displayMangaList.mapAsync { displayManga ->
                val dbManga = db.getManga(displayManga.mangaId).executeOnIO()!!
                val dbChapters =
                    chapterFilter
                        .filterChaptersByScanlatorsAndLanguage(
                            db.getChapters(dbManga).executeOnIO(),
                            dbManga,
                            mangadexPreferences,
                            libraryPreferences,
                        )
                        .reversed()

                when (downloadAction) {
                    DownloadAction.DownloadAll -> {
                        downloadManager.downloadChapters(dbManga, dbChapters)
                    }
                    is DownloadAction.DownloadNextUnread -> {
                        val amount = downloadAction.numberToDownload
                        val unreadDbChapters = dbChapters.filterNot { it.read }.take(amount)
                        downloadManager.downloadChapters(dbManga, unreadDbChapters)
                    }
                    DownloadAction.DownloadUnread -> {
                        val unreadDbChapters = dbChapters.filterNot { it.read }
                        downloadManager.downloadChapters(dbManga, unreadDbChapters)
                    }
                    DownloadAction.RemoveAll -> {
                        downloadManager.deleteChapters(dbManga, dbChapters)
                        updateDownloadBadges(displayManga.mangaId)
                    }
                    DownloadAction.RemoveRead -> {
                        val readDbChapters = dbChapters.filter { it.read }
                        downloadManager.deleteChapters(dbManga, readDbChapters)
                        updateDownloadBadges(displayManga.mangaId)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun openNextUnread(mangaId: Long, openChapter: (Manga, Chapter) -> Unit) {
        presenterScope.launchIO {
            val manga = db.getManga(mangaId).executeOnIO()
            manga ?: return@launchIO
            val chapters = db.getChapters(manga).executeAsBlocking()
            val chapter =
                ChapterSort(manga, chapterFilter, preferences).getNextUnreadChapter(chapters)
            chapter ?: return@launchIO
            openChapter(manga, chapter)
        }
    }

    fun clearSelectedManga() {
        _libraryScreenState.update { it.copy(selectedItems = persistentListOf()) }
    }

    companion object {
        private var lastLibraryCategoryItems: List<LibraryCategoryItem>? = null
        private const val dynamicCategorySplitter = "??\t??\t?"

        fun onLowMemory() {
            lastLibraryCategoryItems = null
        }
    }
}
