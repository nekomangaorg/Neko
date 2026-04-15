package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastAny
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.follows.StatusSyncJob
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
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.chapter.isAvailable
import eu.kanade.tachiyomi.util.manga.toLibraryMangaItem
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.nekomanga.constants.Constants.SEARCH_DEBOUNCE_MILLIS
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.entity.TrackEntity
import org.nekomanga.data.database.model.toCategory
import org.nekomanga.data.database.model.toChapter
import org.nekomanga.data.database.model.toEntity
import org.nekomanga.data.database.model.toLegacyModel
import org.nekomanga.data.database.model.toManga
import org.nekomanga.data.database.model.toSimpleChapter
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.TrackRepositoryImpl
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.CategoryItem.Companion.ALL_CATEGORY
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.toChapterItem
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.category.CategoryUseCases
import org.nekomanga.usecases.chapters.ChapterUseCases
import org.nekomanga.usecases.library.FilterLibraryMangaUseCase
import org.nekomanga.util.system.mapAsync
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryViewModel() : ViewModel() {
    val libraryPreferences: LibraryPreferences = Injekt.get()
    val securityPreferences: SecurityPreferences = Injekt.get()
    val mangadexPreferences: MangaDexPreferences = Injekt.get()
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()
    val coverCache: CoverCache = Injekt.get()
    private val mangaRepository: MangaRepositoryImpl = Injekt.get()
    private val chapterRepository: ChapterRepositoryImpl = Injekt.get()
    private val categoryRepository: CategoryRepositoryImpl = Injekt.get()
    private val trackRepository: TrackRepositoryImpl = Injekt.get()
    val downloadManager: DownloadManager = Injekt.get()
    val workManager: WorkManager = Injekt.get()
    val chapterItemFilter: ChapterItemFilter = Injekt.get()
    val chapterUseCases: ChapterUseCases = Injekt.get()
    val filterLibraryManga: FilterLibraryMangaUseCase = Injekt.get()
    val categoryUseCases: CategoryUseCases = CategoryUseCases()

    private val initialState =
        LibraryScreenState(
            items = lastLibraryCategoryItems?.toPersistentList() ?: persistentListOf(),
            pagerIndex = lastPagerIndex ?: 0,
            scrollPositions = lastScrollPositions ?: emptyMap(),
            isFirstLoad = lastLibraryCategoryItems == null,
            rawColumnCount = libraryPreferences.gridSize().get(),
            libraryDisplayMode = libraryPreferences.layout().get(),
            outlineCovers = libraryPreferences.outlineOnCovers().get(),
            dynamicCovers = mangaDetailsPreferences.dynamicCovers().get(),
            showUnreadBadges = libraryPreferences.showUnreadBadge().get(),
            showDownloadBadges = libraryPreferences.showDownloadBadge().get(),
            showStartReadingButton = libraryPreferences.showStartReadingButton().get(),
            horizontalCategories = libraryPreferences.libraryHorizontalCategories().get(),
            showLibraryButtonBar = libraryPreferences.showLibraryButtonBar().get(),
            incognitoMode = securityPreferences.incognitoMode().get(),
            showUnavailableFilter = mangadexPreferences.includeUnavailableChapters().get(),
            useVividColorHeaders = preferences.useVividColorHeaders().get(),
        )

    private val _internalLibraryScreenState = MutableStateFlow(initialState)

    private val _downloadRefreshTrigger = MutableStateFlow(0)

    private val _mangaRefreshingState = MutableStateFlow(emptySet<Long>())

    private val loggedServices by lazy {
        Injekt.get<TrackManager>().services.values.filter { it.isLogged() || it.isMdList() }
    }

    /** Save the current list to speed up loading later */
    override fun onCleared() {
        super.onCleared()
        val state = libraryScreenState.value
        // dont save state if it didnt actually load
        if (!state.isFirstLoad) {
            lastLibraryCategoryItems = state.items
            lastPagerIndex = state.pagerIndex
            lastScrollPositions = state.scrollPositions
        }
    }

    val categoryListFlow: Flow<List<CategoryItem>> =
        combine(
                libraryPreferences.sortingMode().changes(),
                libraryPreferences.sortAscending().changes(),
                categoryRepository.getAllCategories(),
            ) { sortingMode, sortAscending, dbCategories ->
                val librarySort = LibrarySort.valueOf(sortingMode)
                val defaultCategory = Category.createSystemCategory().toCategoryItem()
                val updatedDefaultCategory =
                    defaultCategory.copy(sortOrder = librarySort, isAscending = sortAscending)
                (listOf(updatedDefaultCategory) +
                        dbCategories.map { dbCategory -> dbCategory.toCategory().toCategoryItem() })
                    .sortedBy { it.order }
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val trackMapFlow: Flow<Map<Long, List<String>>> =
        trackRepository.getAllTracks()
            .map { tracks ->
                val serviceMap = loggedServices.associateBy { it.id }

                tracks
                    .mapNotNull { track ->
                        val trackService = serviceMap[track.syncId]
                        if (trackService == null) {
                            return@mapNotNull null
                        }
                        track.mangaId to trackService.getGlobalStatus(track.status)
                    }
                    .groupBy({ it.first }, { it.second })
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val rawLibraryMangaListFlow =
        mangaRepository.getLibraryMangaList()
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    /**
     * Flow that tracks download counts for all manga in the library. By mapping only the manga IDs
     * and using distinctUntilChanged(), we ensure that download counts are only re-queried when the
     * actual library composition changes (e.g., adding/removing manga), or when a download
     * explicitly triggers a refresh. This prevents redundant, heavy disk IO and DB lookups during
     * normal state updates like reading a chapter.
     */
    val downloadCountMapFlow: Flow<Map<Long, Int>> =
        combine(
                rawLibraryMangaListFlow
                    .map { list -> list.asSequence().mapNotNull { it.manga.id }.toList() }
                    .distinctUntilChanged(),
                _downloadRefreshTrigger,
            ) { mangaIds, _ ->
                downloadManager.getDownloadCountsById(mangaIds)
            }
            .distinctUntilChanged()
            .conflate()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    /**
     * Flow that performs the initial, expensive conversion from DB entities to UI models. This is
     * shared to ensure the conversion only happens once when the DB emits.
     */
    private val initialLibraryItemsFlow =
        rawLibraryMangaListFlow
            .map { dbMangaList -> dbMangaList.map { it.toLegacyModel().toLibraryMangaItem() } }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    /**
     * The core data list flow. It combines the pre-converted UI models with dynamic data like track
     * and download counts. This is much more efficient as the expensive conversion is decoupled
     * from frequent updates of counts.
     */
    val libraryMangaListFlow: Flow<List<LibraryMangaItem>> =
        combine(initialLibraryItemsFlow, trackMapFlow, downloadCountMapFlow) {
                items,
                trackMap,
                downloadCountMap ->
                items.map { item ->
                    val mangaId = item.displayManga.mangaId
                    val newDownloadCount = downloadCountMap[mangaId] ?: 0
                    val newTrackCount = trackMap[mangaId]?.size ?: 0

                    if (
                        item.downloadCount == newDownloadCount && item.trackCount == newTrackCount
                    ) {
                        return@map item
                    }
                    item.copy(downloadCount = newDownloadCount, trackCount = newTrackCount)
                }
            }
            .distinctUntilChanged()
            .conflate()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    /**
     * MACRO-LEVEL PERFORMANCE OPTIMIZATION (Overclock):
     *
     * Why: Previously, this flow included `collapsedCategories()` and
     * `collapsedDynamicCategories()`. These preferences are toggled at a high frequency when users
     * simply expand or collapse a UI section. By including them in `libraryViewFlow`, every single
     * toggle triggered the downstream `groupedMangaFlow` and `sortedMangaFlow` to completely
     * regroup and re-sort (O(N log N)) the entire 1000+ item library, causing massive CPU spikes
     * and stuttering.
     *
     * Architecture: We decoupled the structural sorting/grouping preferences (which require heavy
     * recalculation) from the high-frequency UI toggle state (`collapsedCategories`). This
     * `libraryViewFlow` now only emits when the user fundamentally changes *how* the list is sorted
     * or grouped.
     */
    val libraryViewFlow: Flow<LibraryViewPreferences> =
        combine(
                libraryPreferences.sortingMode().changes(),
                libraryPreferences.sortAscending().changes(),
                libraryPreferences.groupBy().changes(),
                libraryPreferences.showDownloadBadge().changes(),
            ) { sortingMode, sortAscending, groupBy, showDownloadBadges ->
                val librarySort = LibrarySort.valueOf(sortingMode)

                LibraryViewPreferences(
                    sortingMode = librarySort,
                    sortAscending = sortAscending,
                    groupBy = groupBy,
                    showDownloadBadges = showDownloadBadges,
                )
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    /**
     * MACRO-LEVEL PERFORMANCE OPTIMIZATION (Overclock):
     *
     * Why: See `libraryViewFlow`. This isolates the high-frequency UI toggles into their own
     * lightweight stream.
     *
     * Architecture: This flow is combined downstream in `itemsWithRefreshingFlow` so we can rapidly
     * flip the `isHidden` boolean on categories without forcing the entire library to be regrouped
     * and re-sorted.
     */
    val collapsedStateFlow =
        combine(
                libraryPreferences.collapsedCategories().changes(),
                libraryPreferences.collapsedDynamicCategories().changes(),
            ) { collapsedCategories, collapsedDynamicCategories ->
                Pair(collapsedCategories, collapsedDynamicCategories)
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    /**
     * Flow that tracks the last read manga to be used for sorting.
     *
     * Optimization: To prevent multi-second blocking operations on startup for large libraries
     * (e.g. 1000+ items), we first check if the current global sorting mode or any individual
     * category's sorting mode actually requires the "Last Read" data. If not needed, we emit an
     * empty map via `flatMapLatest`, completely bypassing the expensive `mangaRepository.getLastReadManga()`
     * query (which scans `History` and `Chapter` tables).
     */
    private fun getSortFlow(
        sortType: LibrarySort,
        queryFlow: () -> Flow<List<org.nekomanga.data.database.entity.MangaEntity>>,
    ): Flow<Map<Long, Int>> {
        return combine(libraryViewFlow, categoryListFlow) { viewPrefs, categories ->
                if (viewPrefs.groupBy == LibraryGroup.ByCategory) {
                    categories.fastAny { it.sortOrder == sortType }
                } else {
                    viewPrefs.sortingMode == sortType
                }
            }
            .distinctUntilChanged()
            .flatMapLatest { needsSort ->
                if (needsSort) {
                    queryFlow().map { list ->
                        list
                            .mapIndexedNotNull { index, manga -> manga.id?.let { it to index } }
                            .toMap()
                    }
                } else {
                    kotlinx.coroutines.flow.flowOf(emptyMap())
                }
            }
            .conflate()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    }

    val lastReadMangaFlow = getSortFlow(LibrarySort.LastRead) { mangaRepository.getLastReadManga() }

    /**
     * Flow that tracks the last fetched manga to be used for sorting.
     *
     * Optimization: Similar to `lastReadMangaFlow`, this uses `flatMapLatest` to skip the expensive
     * `mangaRepository.getLastFetchedManga()` query if the "Date Fetched" sorting mode is not actively used
     * globally or by any category. This prevents massive blocking DB reads during UI state updates.
     */
    val lastFetchMangaFlow =
        getSortFlow(LibrarySort.DateFetched) { mangaRepository.getLastFetchedManga() }

    val filteredMangaListFlow =
        combine(
                libraryMangaListFlow,
                _internalLibraryScreenState
                    .map { it.searchQuery to it.initialSearch }
                    .distinctUntilChanged()
                    .debounce { (query, initial) ->
                        // If the query matches the deep link 'initialSearch', skip the delay (0ms).
                        // Otherwise (manual typing), use the standard debounce (e.g., 500ms).
                        if (!query.isNullOrBlank() && query == initial) 0L
                        else SEARCH_DEBOUNCE_MILLIS
                    }
                    .map { it.first?.trim() } // Extract just the query string
                    .distinctUntilChanged(), // Prevent re-emission when 'initialSearch' is cleared
                // later
            ) { mangaList, searchQuery ->
                if (searchQuery.isNullOrBlank()) {
                    mangaList
                } else {
                    val splitQuery = if (searchQuery.contains(",")) searchQuery.split(",") else null
                    mangaList.filter { libraryMangaItem ->
                        libraryMangaItem.matches(searchQuery, splitQuery)
                    }
                }
            }
            .conflate()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val filterPreferencesFlow: Flow<LibraryFilters> =
        combine(
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
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    // 1. FILTER FLOW: Applies filters and fetches necessary data (Download counts)
    private val activeMangaFlow =
        combine(filteredMangaListFlow, filterPreferencesFlow) { mangaList, libraryFilters ->
                withContext(Dispatchers.Default) {
                    mangaList.filter { filterLibraryManga(it, libraryFilters) }
                }
            }
            .distinctUntilChanged()

    // 2. GROUP FLOW: Groups the filtered items (By Category, Status, etc.)
    private val groupedMangaFlow =
        combine(activeMangaFlow, libraryViewFlow, categoryListFlow, trackMapFlow) {
                activeMangaList,
                libraryViewPreferences,
                categoryList,
                trackMap ->
                withContext(Dispatchers.Default) {
                    when (libraryViewPreferences.groupBy) {
                        LibraryGroup.ByCategory -> {
                            groupByCategory(activeMangaList, categoryList)
                        }
                        LibraryGroup.ByAuthor,
                        LibraryGroup.ByContent,
                        LibraryGroup.ByLanguage,
                        LibraryGroup.ByStatus,
                        LibraryGroup.ByTag,
                        LibraryGroup.ByTrackStatus -> {
                            groupByDynamic(
                                libraryMangaList = activeMangaList,
                                currentLibraryGroup = libraryViewPreferences.groupBy,
                                sortOrder = libraryViewPreferences.sortingMode,
                                sortAscending = libraryViewPreferences.sortAscending,
                                loggedInTrackStatus = trackMap,
                            )
                        }
                        LibraryGroup.Ungrouped -> {
                            groupByUngrouped(
                                activeMangaList,
                                libraryViewPreferences.sortingMode,
                                libraryViewPreferences.sortAscending,
                            )
                        }
                    }
                }
            }
            .distinctUntilChanged()

    // 3. SORT FLOW: Sorts the items inside each group
    private val sortedMangaFlow =
        combine(
                groupedMangaFlow,
                libraryViewFlow,
                lastReadMangaFlow,
                lastFetchMangaFlow,
                libraryPreferences.removeArticles().changes(),
            ) { groupedItems, libraryViewPreferences, lastReadMap, lastFetchMap, removeArticles ->
                withContext(Dispatchers.Default) {
                    val sortedItems =
                        groupedItems
                            .mapAsync { libraryCategoryItem ->
                                // Create the comparator
                                val comparator =
                                    libraryMangaItemComparator(
                                        categorySort = libraryCategoryItem.categoryItem.sortOrder,
                                        categoryIsAscending =
                                            libraryCategoryItem.categoryItem.isAscending,
                                        removeArticles = removeArticles,
                                        mangaOrder = libraryCategoryItem.categoryItem.mangaOrder,
                                        lastReadMap = lastReadMap,
                                        lastFetchMap = lastFetchMap,
                                    )

                                // Sort the items in this category
                                val sortedList =
                                    libraryCategoryItem.libraryItems
                                        .sortedWith(comparator)
                                        .toPersistentList()

                                libraryCategoryItem.copy(libraryItems = sortedList)
                            }
                            .toPersistentList()

                    Pair(sortedItems, libraryViewPreferences)
                }
            }
            .distinctUntilChanged()

    /**
     * MACRO-LEVEL PERFORMANCE OPTIMIZATION (Overclock):
     * 4. FINAL STATE FLOW: Applies high-frequency states like refreshing and collapsed categories.
     *
     * Why: See `libraryViewFlow`. By separating the high-frequency UI updates into this final,
     * decoupled pipeline layer, the app no longer recalculates groupings or executes O(N log N)
     * sorting just because a user toggled a category's visibility or a manga changed its refreshing
     * state. The heavy lifting happens upstream and is cached, while these O(N) simple iterations
     * happen quickly.
     *
     * Architecture: We combine the cached, sorted arrays with the `collapsedStateFlow`. We use
     * `mapAsync` (with `Dispatchers.Default`) to quickly compute `isHidden` and `isRefreshing` on
     * all categories. `allCollapsed` is evaluated functionally after the map operation completes to
     * ensure thread-safety.
     */
    private val itemsWithRefreshingFlow =
        combine(sortedMangaFlow, _mangaRefreshingState.asStateFlow(), collapsedStateFlow) {
                sortedMangaPair,
                mangaRefreshingState,
                collapsedState ->
                val (sortedItems, libraryViewPreferences) = sortedMangaPair
                val (collapsedCategories, collapsedDynamicCategories) = collapsedState
                withContext(Dispatchers.Default) {
                    val collapsedCategorySet =
                        collapsedCategories.mapNotNull { it.toIntOrNull() }.toSet()

                    val groupTypeStr = libraryViewPreferences.groupBy.type.toString()

                    val updatedItems =
                        sortedItems
                            .mapAsync { libraryCategoryItem ->
                                val isHidden =
                                    if (libraryCategoryItem.categoryItem.isDynamic) {
                                        (groupTypeStr +
                                            dynamicCategorySplitter +
                                            libraryCategoryItem.categoryItem.name) in
                                            collapsedDynamicCategories
                                    } else {
                                        libraryCategoryItem.categoryItem.id in collapsedCategorySet
                                    }

                                val isRefreshing =
                                    if (mangaRefreshingState.isEmpty()) false
                                    else
                                        libraryCategoryItem.libraryItems.fastAny {
                                            it.displayManga.mangaId in mangaRefreshingState
                                        }

                                libraryCategoryItem.copy(
                                    isRefreshing = isRefreshing,
                                    categoryItem =
                                        libraryCategoryItem.categoryItem.copy(isHidden = isHidden),
                                )
                            }
                            .toPersistentList()

                    val allCollapsed = updatedItems.all { it.categoryItem.isHidden }

                    Pair(updatedItems, allCollapsed)
                }
            }
            .distinctUntilChanged()

    /**
     * MACRO-LEVEL PERFORMANCE OPTIMIZATION (Bolt): Prevent redundant downstream UI state
     * recalculations when multiple preference flows emit sequentially with the same values,
     * avoiding redundant UI recompositions.
     */
    private val uiSettingsFlow =
        combine(
                libraryPreferences.gridSize().changes(),
                libraryPreferences.layout().changes(),
                filterPreferencesFlow,
            ) { gridSize, layout, filters ->
                Triple(gridSize, layout, filters)
            }
            .distinctUntilChanged()

    val libraryScreenState: StateFlow<LibraryScreenState> =
        combine(
                _internalLibraryScreenState,
                itemsWithRefreshingFlow,
                trackMapFlow,
                categoryListFlow,
                libraryViewFlow,
                uiSettingsFlow,
            ) {
                state,
                itemsWithRefreshingPair,
                trackMap,
                categories,
                viewPrefs,
                uiSettings ->
                val (itemsWithRefreshing, allCollapsed) = itemsWithRefreshingPair
                val (gridSize, layout, filters) = uiSettings
                state.copy(
                    items = itemsWithRefreshing,
                    allCollapsed = allCollapsed,
                    libraryDisplayMode = layout,
                    rawColumnCount = gridSize,
                    libraryFilters = filters,
                    hasActiveFilters = filters.hasActiveFilter(),
                    currentGroupBy = viewPrefs.groupBy,
                    trackMap = trackMap.toPersistentMap(),
                    userCategories =
                        categories.filterNot { it.isSystemCategory }.toPersistentList(),
                    isFirstLoad = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = initialState,
            )

    init {

        lastLibraryCategoryItems = null
        lastPagerIndex = null
        lastScrollPositions = null

        observeLibraryUpdates()
        preferenceUpdates()

        viewModelScope.launchIO {
            val groupItems =
                mutableListOf(
                    LibraryGroup.ByCategory,
                    LibraryGroup.ByTag,
                    LibraryGroup.ByStatus,
                    LibraryGroup.ByAuthor,
                    LibraryGroup.ByContent,
                    LibraryGroup.ByLanguage,
                )
            if (loggedServices.isNotEmpty()) {
                groupItems.add(LibraryGroup.ByTrackStatus)
            }
            if (categoryRepository.getAllCategoriesList().isNotEmpty()) {
                groupItems.add(LibraryGroup.Ungrouped)
            }
            _internalLibraryScreenState.update {
                it.copy(groupByOptions = groupItems.toPersistentList())
            }
        }
    }

    fun preferenceUpdates() {

        preferences.useVividColorHeaders().changes().observeAndUpdate(viewModelScope) { value ->
            _internalLibraryScreenState.update { state -> state.copy(useVividColorHeaders = value) }
        }

        libraryPreferences.showStartReadingButton().changes().observeAndUpdate(viewModelScope) {
            value ->
            _internalLibraryScreenState.update { state ->
                state.copy(showStartReadingButton = value)
            }
        }

        mangadexPreferences.includeUnavailableChapters().changes().observeAndUpdate(
            viewModelScope
        ) { value ->
            _internalLibraryScreenState.update { state ->
                state.copy(showUnavailableFilter = value)
            }
        }

        securityPreferences.incognitoMode().changes().observeAndUpdate(viewModelScope) { value ->
            _internalLibraryScreenState.update { state -> state.copy(incognitoMode = value) }
        }

        mangaDetailsPreferences.dynamicCovers().changes().observeAndUpdate(viewModelScope) { value
            ->
            _internalLibraryScreenState.update { state -> state.copy(dynamicCovers = value) }
        }

        libraryPreferences.outlineOnCovers().changes().observeAndUpdate(viewModelScope) { value ->
            _internalLibraryScreenState.update { state -> state.copy(outlineCovers = value) }
        }

        libraryPreferences.showDownloadBadge().changes().observeAndUpdate(viewModelScope) { value ->
            _internalLibraryScreenState.update { state -> state.copy(showDownloadBadges = value) }
        }

        libraryPreferences.showUnreadBadge().changes().observeAndUpdate(viewModelScope) { value ->
            _internalLibraryScreenState.update { state -> state.copy(showUnreadBadges = value) }
        }

        libraryPreferences.libraryHorizontalCategories().changes().observeAndUpdate(
            viewModelScope
        ) { value ->
            _internalLibraryScreenState.update { state -> state.copy(horizontalCategories = value) }
        }

        libraryPreferences.showLibraryButtonBar().changes().observeAndUpdate(viewModelScope) { value
            ->
            _internalLibraryScreenState.update { state -> state.copy(showLibraryButtonBar = value) }
        }
    }

    fun categoryItemClick(category: CategoryItem) {
        viewModelScope.launchIO {
            when (category.isDynamic) {
                true -> {
                    val collapsedDynamicCategorySet =
                        libraryPreferences.collapsedDynamicCategories().get().toMutableSet()
                    val dynamicName =
                        dynamicCategoryName(libraryPreferences.groupBy().get().type, category.name)

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

    fun groupByClick(groupBy: LibraryGroup) {
        viewModelScope.launchIO { libraryPreferences.groupBy().set(groupBy) }
    }

    fun libraryDisplayModeClick(libraryDisplayMode: LibraryDisplayMode) {
        viewModelScope.launchIO { libraryPreferences.layout().set(libraryDisplayMode) }
    }

    fun rawColumnCountChanged(updatedColumnCount: Float) {
        viewModelScope.launchIO { libraryPreferences.gridSize().set(updatedColumnCount) }
    }

    fun outlineCoversToggled() {
        viewModelScope.launchIO { libraryPreferences.outlineOnCovers().toggle() }
    }

    fun downloadBadgesToggled() {
        viewModelScope.launchIO { libraryPreferences.showDownloadBadge().toggle() }
    }

    fun unreadBadgesToggled() {
        viewModelScope.launchIO { libraryPreferences.showUnreadBadge().toggle() }
    }

    fun startReadingButtonToggled() {
        viewModelScope.launchIO { libraryPreferences.showStartReadingButton().toggle() }
    }

    fun horizontalCategoriesToggled() {
        viewModelScope.launchIO { libraryPreferences.libraryHorizontalCategories().toggle() }
    }

    fun showLibraryButtonBarToggled() {
        viewModelScope.launchIO { libraryPreferences.showLibraryButtonBar().toggle() }
    }

    fun clearActiveFilters() {
        viewModelScope.launchIO {
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
        viewModelScope.launchIO {
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
        viewModelScope.launchIO { categoryUseCases.modifyCategory.addNewCategory(newCategory) }
    }

    fun editCategories(mangaList: List<DisplayManga>, categories: List<CategoryItem>) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.setMangaCategories(mangaList, categories)

            clearSelectedManga()
            if (libraryPreferences.groupBy().get() == LibraryGroup.ByCategory) {
                libraryPreferences.groupBy().set(LibraryGroup.Ungrouped)
                libraryPreferences.groupBy().set(LibraryGroup.ByCategory)
            }
        }
    }

    fun categoryAscendingClick(category: CategoryItem) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.updateCategorySortAscending(category)
        }
    }

    fun categoryItemLibrarySortClick(category: CategoryItem, librarySort: LibrarySort) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.updateCategoryLibrarySort(category, librarySort)
        }
    }

    fun groupByDynamic(
        libraryMangaList: List<LibraryMangaItem>,
        currentLibraryGroup: LibraryGroup,
        sortOrder: LibrarySort,
        sortAscending: Boolean,
        loggedInTrackStatus: Map<Long, List<String>>,
    ): PersistentList<LibraryCategoryItem> {
        val groupedMap = mutableMapOf<String, MutableList<LibraryMangaItem>>()
        val notTrackedList = listOf("Not tracked")

        val distinctMangaList = libraryMangaList.distinctBy { it.displayManga.mangaId }

        for (libraryMangaItem in distinctMangaList) {
            val groupingKeys =
                when (currentLibraryGroup) {
                    LibraryGroup.ByAuthor -> libraryMangaItem.author
                    LibraryGroup.ByContent -> libraryMangaItem.contentRating
                    LibraryGroup.ByLanguage -> libraryMangaItem.language
                    LibraryGroup.ByStatus -> libraryMangaItem.status
                    LibraryGroup.ByTag -> libraryMangaItem.genre
                    LibraryGroup.ByTrackStatus -> {
                        loggedInTrackStatus[libraryMangaItem.displayManga.mangaId] ?: notTrackedList
                    }
                    else -> libraryMangaItem.language
                }.distinct()

            for (key in groupingKeys) {
                groupedMap.getOrPut(key) { mutableListOf() }.add(libraryMangaItem)
            }
        }
        val keyComparator = currentLibraryGroup.keyComparator

        return groupedMap.entries
            .sortedWith(compareBy(keyComparator) { it.key })
            .mapIndexed { index, entry ->
                val categoryName = entry.key
                val items = entry.value
                val categoryItem =
                    CategoryItem(
                        id = index,
                        sortOrder = sortOrder,
                        isAscending = sortAscending,
                        name = categoryName,
                        isHidden = false,
                        isDynamic = true,
                    )
                LibraryCategoryItem(
                    categoryItem = categoryItem,
                    libraryItems = items.toPersistentList(),
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

        val distinctList =
            libraryMangaList.distinctBy { it.displayManga.mangaId }.toPersistentList()

        return persistentListOf(
            LibraryCategoryItem(categoryItem = allCategoryItem, libraryItems = distinctList)
        )
    }

    fun groupByCategory(
        libraryMangaList: List<LibraryMangaItem>,
        categoryList: List<CategoryItem>,
    ): List<LibraryCategoryItem> {
        if (libraryMangaList.isEmpty()) {
            return emptyList()
        }

        val mangaMap = libraryMangaList.groupBy { it.category }

        return categoryList.mapNotNull { categoryItem ->
            val unsortedMangaList = mangaMap[categoryItem.id] ?: emptyList()

            if (categoryItem.isSystemCategory && unsortedMangaList.isEmpty()) {
                return@mapNotNull null
            }

            LibraryCategoryItem(
                categoryItem = categoryItem,
                libraryItems = unsortedMangaList.toPersistentList(),
            )
        }
    }

    fun observeLibraryUpdates() {
        viewModelScope.launchIO {
            val jobActiveFlow =
                workManager
                    .getWorkInfosByTagFlow(LibraryUpdateJob.TAG)
                    .map { list -> list.any { it.state == WorkInfo.State.RUNNING } }
                    .distinctUntilChanged()

            jobActiveFlow.collectLatest { active ->
                if (active) {
                    LibraryUpdateJob.mangaToUpdateFlow.collect { mangaId ->
                        _mangaRefreshingState.update { it + mangaId }
                    }
                } else {
                    _mangaRefreshingState.update { emptySet() }
                }
            }
        }

        viewModelScope.launchIO {
            downloadManager
                .statusFlow()
                .catch { error -> TimberKt.e(error) }
                .collect { download ->
                    if (download.status == Download.State.DOWNLOADED) {
                        updateDownloadBadges(download.mangaItem.id)
                    }
                }
        }

        viewModelScope.launchIO {
            downloadManager.removedChaptersFlow.collect { id -> updateDownloadBadges(id) }
        }

        viewModelScope.launchIO {
            downloadManager.downloadCacheUpdatedFlow.collect {
                // Pass 0L as the mangaId is currently unused and we just need to trigger a global
                // refresh
                updateDownloadBadges(0L)
            }
        }
    }

    fun updateDownloadBadges(mangaId: Long) {
        _downloadRefreshTrigger.update { it + 1 }
    }

    fun collapseExpandAllCategories() {
        viewModelScope.launchIO {
            val updatedAllCollapsed = !libraryScreenState.value.allCollapsed
            val categoryItems = libraryScreenState.value.items.map { it.categoryItem }
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
                                .map { dynamicCategoryName(libraryGroupBy.type, it.name) }
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

    fun search(searchQuery: String?) {
        _internalLibraryScreenState.update { it.copy(searchQuery = searchQuery) }
    }

    fun deepLinkSearch(searchQuery: String) {
        _internalLibraryScreenState.update {
            it.copy(searchQuery = searchQuery, initialSearch = searchQuery)
        }
    }

    /** sync selected manga to mangadex follows */
    fun syncMangaToDex() {
        viewModelScope.launchIO {
            val mangaIds =
                libraryScreenState.value.selectedItems
                    .map { it.displayManga.mangaId }
                    .distinct()
                    .joinToString()
            StatusSyncJob.startNow(workManager, mangaIds)
            _internalLibraryScreenState.update { it.copy(selectedItems = persistentListOf()) }
        }
    }

    fun selectAllLibraryMangaItems(libraryMangaItems: List<LibraryMangaItem>) {
        viewModelScope.launchIO {
            val currentSelected = libraryScreenState.value.selectedItems.toList()

            val categoryItemIds = libraryMangaItems.map { it.displayManga.mangaId }.toSet()
            val selectedItemIds = currentSelected.map { it.displayManga.mangaId }.toSet()

            val allSelected = selectedItemIds.containsAll(categoryItemIds)

            val newSelectedItems: List<LibraryMangaItem>

            if (allSelected) {
                // Unselect all
                newSelectedItems = currentSelected.filter {
                    it.displayManga.mangaId !in categoryItemIds
                }
            } else {
                // Select all
                val itemsToAdd = libraryMangaItems.filter {
                    it.displayManga.mangaId !in selectedItemIds
                }

                newSelectedItems = currentSelected + itemsToAdd
            }

            _internalLibraryScreenState.update {
                it.copy(selectedItems = newSelectedItems.toPersistentList())
            }
        }
    }

    fun deleteSelectedLibraryMangaItems() {
        viewModelScope.launchNonCancellable {
            val currentSelected = libraryScreenState.value.selectedItems.toList()
            clearSelectedManga()

            val mangaIds = currentSelected.map { it.displayManga.mangaId }
            val dbMangas = mangaRepository.getMangas(mangaIds).map { it.toManga() }

            for (dbManga in dbMangas) {
                try {
                    coverCache.deleteFromCache(dbManga)
                    downloadManager.deleteManga(dbManga)
                } catch (e: Exception) {
                    TimberKt.e(e)
                }
                dbManga.favorite = false
            }

            mangaRepository.insertMangas(dbMangas.map { it.toEntity() })
        }
    }

    fun libraryItemLongClick(libraryMangaItem: LibraryMangaItem) {
        viewModelScope.launchIO {
            val index =
                libraryScreenState.value.selectedItems.indexOfFirst {
                    it.displayManga.mangaId == libraryMangaItem.displayManga.mangaId
                }
            val updatedSelectedItems =
                if (index >= 0) {
                    libraryScreenState.value.selectedItems.removeAt(index)
                } else {
                    val categoryItems =
                        categoryRepository.getCategoriesForMangaSync(libraryMangaItem.displayManga.mangaId)
                            .map { it.toCategory().toCategoryItem() }
                    val copy = libraryMangaItem.copy(allCategories = categoryItems)
                    libraryScreenState.value.selectedItems.add(copy)
                }

            _internalLibraryScreenState.update {
                it.copy(selectedItems = updatedSelectedItems.distinct().toPersistentList())
            }
        }
    }

    fun markChapters(markAction: ChapterMarkActions) {
        viewModelScope.launchIO {
            val selectedItems = libraryScreenState.value.selectedItems
            _internalLibraryScreenState.update { it.copy(selectedItems = persistentListOf()) }

            val mangaIds = selectedItems.map { it.displayManga.mangaId }
            val mangasMap = mangaRepository.getMangas(mangaIds).associateBy { it.id }
            val chaptersMap = chapterRepository.getChaptersForMangas(mangaIds).groupBy { it.mangaId }

            selectedItems.mapAsync { selectedItem ->
                val mangaId = selectedItem.displayManga.mangaId
                val mangaEntity = mangasMap[mangaId] ?: return@mapAsync
                val dbManga = mangaEntity.toManga()
                val chapterItems =
                    chaptersMap[mangaId]?.mapNotNull { it.toSimpleChapter().toChapterItem() }
                        ?: emptyList()

                chapterUseCases.markChapters(markAction, chapterItems)
                chapterUseCases.markChaptersRemote(markAction, dbManga.uuid(), chapterItems)
            }
        }
    }

    fun downloadChapters(downloadAction: DownloadAction) {
        viewModelScope.launchIO {
            val displayMangaList = libraryScreenState.value.selectedItems.map { it.displayManga }
            clearSelectedManga()
            if (
                downloadAction == DownloadAction.Cancel ||
                    downloadAction == DownloadAction.Download ||
                    downloadAction == DownloadAction.Remove ||
                    downloadAction == DownloadAction.ImmediateDownload
            ) {
                return@launchIO
            }

            val mangaIds = displayMangaList.map { it.mangaId }
            val mangasMap = mangaRepository.getMangas(mangaIds).associateBy { it.id }
            val chaptersMap = chapterRepository.getChaptersForMangas(mangaIds).groupBy { it.mangaId }

            displayMangaList.mapAsync { displayManga ->
                val mangaEntity = mangasMap[displayManga.mangaId] ?: return@mapAsync
                val dbManga = mangaEntity.toManga()
                val rawChapters =
                    chaptersMap[displayManga.mangaId]?.mapNotNull {
                        it.toSimpleChapter().toChapterItem()
                    } ?: emptyList()
                val chapterItems =
                    chapterItemFilter
                        .filterChaptersByScanlatorsAndLanguage(
                            rawChapters,
                            dbManga,
                            mangadexPreferences,
                            libraryPreferences,
                        )
                        .reversed()

                when (downloadAction) {
                    DownloadAction.DownloadAll -> {
                        downloadManager.downloadChapters(
                            dbManga,
                            chapterItems.map { it.chapter.toDbChapter() },
                        )
                    }
                    is DownloadAction.DownloadNextUnread -> {
                        val amount = downloadAction.numberToDownload
                        val unreadDbChapters =
                            chapterItems
                                .asSequence()
                                .mapNotNull {
                                    if (!it.chapter.read) it.chapter.toDbChapter() else null
                                }
                                .take(amount)
                                .toList()
                        downloadManager.downloadChapters(dbManga, unreadDbChapters)
                    }
                    DownloadAction.DownloadUnread -> {
                        val unreadDbChapters = chapterItems.mapNotNull { item ->
                            item.chapter.takeIf { !it.read }?.toDbChapter()
                        }

                        downloadManager.downloadChapters(dbManga, unreadDbChapters)
                    }
                    DownloadAction.RemoveAll -> {
                        downloadManager.deleteChapters(
                            dbManga,
                            chapterItems.map { it.chapter.toDbChapter() },
                        )
                    }
                    DownloadAction.RemoveRead -> {
                        val readDbChapters = chapterItems.mapNotNull { item ->
                            item.chapter.takeIf { it.read }?.toDbChapter()
                        }

                        downloadManager.deleteChapters(dbManga, readDbChapters)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun openNextUnread(mangaId: Long, openChapter: (Manga, Chapter) -> Unit) {
        viewModelScope.launchIO {
            val mangaEntity = mangaRepository.getMangaById(mangaId)
            val manga = mangaEntity?.toManga() ?: return@launchIO
            val chapters = chapterRepository.getChaptersForMangaSync(mangaId).map { it.toChapter() }

            // to avoid allocating an intermediate list of available chapters,
            // reducing GC overhead when the user quickly jumps to reading.
            val availableChapters = chapters.mapNotNull { chapter ->
                val simpleChapter = chapter.toSimpleChapter()
                if (chapter.isAvailable(downloadManager, manga) && simpleChapter != null) {
                    simpleChapter.toChapterItem()
                } else {
                    null
                }
            }

            val chapter = ChapterItemSort().getNextUnreadChapter(manga, availableChapters)
            chapter ?: return@launchIO
            openChapter(manga, chapter.chapter.toDbChapter())
        }
    }

    fun getSelectedMangaUrls(): String {
        val urls =
            libraryScreenState.value.selectedItems
                .map { selected -> selected.url }
                .distinct()
                .joinToString("\n")
        viewModelScope.launchIO { clearSelectedManga() }
        return urls
    }

    fun clearSelectedManga() {
        _internalLibraryScreenState.update { it.copy(selectedItems = persistentListOf()) }
    }

    fun pagerIndexChanged(index: Int) {
        _internalLibraryScreenState.update { it.copy(pagerIndex = index) }
    }

    fun scrollPositionChanged(categoryIndex: Int, scrollPosition: Int) {
        _internalLibraryScreenState.update { state ->
            val newScrollPositions = state.scrollPositions.toMutableMap()
            newScrollPositions[categoryIndex] = scrollPosition
            state.copy(scrollPositions = newScrollPositions)
        }
    }

    fun clearInitialSearch() {
        _internalLibraryScreenState.update { it.copy(initialSearch = "") }
    }

    companion object {
        private var lastLibraryCategoryItems: List<LibraryCategoryItem>? = null
        private var lastPagerIndex: Int? = null
        private var lastScrollPositions: Map<Int, Int>? = null
        private const val dynamicCategorySplitter = "??\t??\t?"

        fun onLowMemory() {
            lastLibraryCategoryItems = null
            lastPagerIndex = null
            lastScrollPositions = null
        }
    }
}
