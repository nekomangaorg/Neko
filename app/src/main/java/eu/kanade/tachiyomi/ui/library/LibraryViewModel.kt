package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastAny
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
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
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.combine
import eu.kanade.tachiyomi.util.system.executeOnIO
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.nekomanga.constants.Constants.SEARCH_DEBOUNCE_MILLIS
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.CategoryItem.Companion.ALL_CATEGORY
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases
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
    val db: DatabaseHelper = Injekt.get()
    val downloadManager: DownloadManager = Injekt.get()
    val workManager: WorkManager = Injekt.get()
    val chapterItemFilter: ChapterItemFilter = Injekt.get()
    val chapterUseCases: ChapterUseCases = Injekt.get()

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
                db.getCategories().asFlow(),
            ) { sortingMode, sortAscending, dbCategories ->
                val librarySort = LibrarySort.valueOf(sortingMode)
                val defaultCategory = Category.createSystemCategory().toCategoryItem()
                val updatedDefaultCategory =
                    defaultCategory.copy(sortOrder = librarySort, isAscending = sortAscending)
                (listOf(updatedDefaultCategory) +
                        dbCategories.map { dbCategory -> dbCategory.toCategoryItem() })
                    .sortedBy { it.order }
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val trackMapFlow: Flow<Map<Long, List<String>>> =
        db.getAllTracks()
            .asFlow()
            .mapNotNull { tracks ->
                val serviceMap = loggedServices.associateBy { it.id }

                tracks
                    .mapNotNull { track ->
                        val trackService = serviceMap[track.sync_id]
                        if (trackService == null) {
                            return@mapNotNull null
                        }
                        track.manga_id to trackService.getGlobalStatus(track.status)
                    }
                    .groupBy({ it.first }, { it.second })
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val rawLibraryMangaListFlow =
        db.getLibraryMangaList()
            .asFlow()
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
                    .map { list -> list.asSequence().mapNotNull { it.id }.toList() }
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
            .map { dbMangaList -> dbMangaList.map { it.toLibraryMangaItem() } }
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

    val lastReadMangaFlow =
        db.getLastReadManga()
            .asFlow()
            .map { list -> list.mapIndexed { index, manga -> manga.id!! to index }.toMap() }
            .conflate()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val lastFetchMangaFlow =
        db.getLastFetchedManga()
            .asFlow()
            .map { list -> list.mapIndexed { index, manga -> manga.id!! to index }.toMap() }
            .conflate()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

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

    @Suppress("UNCHECKED_CAST")
    val libraryViewFlow: Flow<LibraryViewPreferences> =
        combine(
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
                    groupBy = it[4] as LibraryGroup,
                    showDownloadBadges = it[5] as Boolean,
                )
            }
            .distinctUntilChanged()
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
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    // 1. FILTER FLOW: Applies filters and fetches necessary data (Download counts)
    private val activeMangaFlow =
        combine(filteredMangaListFlow, filterPreferencesFlow) { mangaList, libraryFilters ->
                withContext(Dispatchers.Default) {
                    mangaList.filter { it.matchesFilters(libraryFilters) }
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
                    val collapsedCategorySet =
                        libraryViewPreferences.collapsedCategories
                            .mapNotNull { it.toIntOrNull() }
                            .toSet()

                    when (libraryViewPreferences.groupBy) {
                        LibraryGroup.ByCategory -> {
                            groupByCategory(activeMangaList, categoryList, collapsedCategorySet)
                        }
                        LibraryGroup.ByAuthor,
                        LibraryGroup.ByContent,
                        LibraryGroup.ByLanguage,
                        LibraryGroup.ByStatus,
                        LibraryGroup.ByTag,
                        LibraryGroup.ByTrackStatus -> {
                            groupByDynamic(
                                libraryMangaList = activeMangaList,
                                collapsedDynamicCategorySet =
                                    libraryViewPreferences.collapsedDynamicCategories,
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
                _mangaRefreshingState.asStateFlow(),
                libraryPreferences.removeArticles().changes(),
            ) {
                groupedItems,
                libraryViewPreferences,
                lastReadMap,
                lastFetchMap,
                mangaRefreshingState,
                removeArticles ->
                withContext(Dispatchers.Default) {
                    var allCollapsed = true

                    val sortedItems =
                        groupedItems
                            .mapAsync { libraryCategoryItem ->
                                if (!libraryCategoryItem.categoryItem.isHidden) {
                                    allCollapsed = false
                                }

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

                                val isRefreshing =
                                    libraryCategoryItem.libraryItems.fastAny {
                                        it.displayManga.mangaId in mangaRefreshingState
                                    }

                                libraryCategoryItem.copy(
                                    libraryItems = sortedList,
                                    isRefreshing = isRefreshing,
                                )
                            }
                            .toPersistentList()

                    // Return a lightweight object or Pair to pass to the collector
                    Triple(sortedItems, allCollapsed, libraryViewPreferences)
                }
            }
            .distinctUntilChanged()

    private val uiSettingsFlow =
        combine(
            libraryPreferences.gridSize().changes(),
            libraryPreferences.layout().changes(),
            filterPreferencesFlow,
        ) { gridSize, layout, filters ->
            Triple(gridSize, layout, filters)
        }

    val libraryScreenState: StateFlow<LibraryScreenState> =
        combine(
                _internalLibraryScreenState,
                sortedMangaFlow,
                trackMapFlow,
                categoryListFlow,
                libraryViewFlow,
                uiSettingsFlow,
            ) {
                state,
                (sortedItems, allCollapsed, _),
                trackMap,
                categories,
                viewPrefs,
                (gridSize, layout, filters) ->
                state.copy(
                    items = sortedItems,
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
            if (db.getCategories().executeAsBlocking().isNotEmpty()) {
                groupItems.add(LibraryGroup.Ungrouped)
            }
            _internalLibraryScreenState.update {
                it.copy(groupByOptions = groupItems.toPersistentList())
            }
        }
    }

    fun preferenceUpdates() {

        viewModelScope.launchIO {
            preferences.useVividColorHeaders().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _internalLibraryScreenState.update { it.copy(useVividColorHeaders = enabled) }
            }
        }

        viewModelScope.launchIO {
            libraryPreferences
                .showStartReadingButton()
                .changes()
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    _internalLibraryScreenState.update { it.copy(showStartReadingButton = enabled) }
                }
        }

        viewModelScope.launchIO {
            mangadexPreferences
                .includeUnavailableChapters()
                .changes()
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    _internalLibraryScreenState.update { it.copy(showUnavailableFilter = enabled) }
                }
        }

        viewModelScope.launchIO {
            securityPreferences.incognitoMode().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _internalLibraryScreenState.update { it.copy(incognitoMode = enabled) }
            }
        }

        viewModelScope.launchIO {
            libraryPreferences.outlineOnCovers().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _internalLibraryScreenState.update { it.copy(outlineCovers = enabled) }
            }
        }

        viewModelScope.launchIO {
            libraryPreferences.showDownloadBadge().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _internalLibraryScreenState.update { it.copy(showDownloadBadges = enabled) }
            }
        }

        viewModelScope.launchIO {
            libraryPreferences.showUnreadBadge().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _internalLibraryScreenState.update { it.copy(showUnreadBadges = enabled) }
            }
        }

        viewModelScope.launchIO {
            libraryPreferences
                .libraryHorizontalCategories()
                .changes()
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    _internalLibraryScreenState.update { it.copy(horizontalCategories = enabled) }
                }
        }

        viewModelScope.launchIO {
            libraryPreferences
                .showLibraryButtonBar()
                .changes()
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    _internalLibraryScreenState.update { it.copy(showLibraryButtonBar = enabled) }
                }
        }

        viewModelScope.launchIO {
            combine(
                    libraryPreferences.gridSize().changes(),
                    libraryPreferences.layout().changes(),
                ) { gridSize, layout ->
                    gridSize to layout
                }
                .distinctUntilChanged()
                .collectLatest { pair ->
                    _internalLibraryScreenState.update {
                        it.copy(libraryDisplayMode = pair.second, rawColumnCount = pair.first)
                    }
                }
        }
    }

    private fun LibraryMangaItem.matchesFilters(libraryFilters: LibraryFilters): Boolean {
        // Check Unread first (most common filter)
        if (!libraryFilters.filterUnread.matches(this)) return false

        // Check Downloaded second (common and quick)
        if (!libraryFilters.filterDownloaded.matches(this)) return false

        // Check the rest
        if (!libraryFilters.filterBookmarked.matches(this)) return false
        if (!libraryFilters.filterCompleted.matches(this)) return false
        if (!libraryFilters.filterMangaType.matches(this)) return false
        if (!libraryFilters.filterMerged.matches(this)) return false
        if (!libraryFilters.filterUnavailable.matches(this)) return false
        if (!libraryFilters.filterTracked.matches(this)) return false

        return true // passed all checks
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
        viewModelScope.launchIO {
            val category = Category.create(newCategory)
            category.order =
                (libraryScreenState.value.userCategories.maxOfOrNull { it.order } ?: 0) + 1
            db.insertCategory(category).executeAsBlocking()
        }
    }

    fun editCategories(mangaList: List<DisplayManga>, categories: List<CategoryItem>) {
        viewModelScope.launchIO {
            val dbCategories = categories.map { it.toDbCategory() }

            val mangaIds = mangaList.map { it.mangaId }
            val dbMangas = db.getMangas(mangaIds).executeOnIO()
            val mangaCategories = dbMangas.flatMap { dbManga ->
                dbCategories.map { MangaCategory.create(dbManga, it) }
            }
            db.setMangaCategories(mangaCategories, dbMangas)

            clearSelectedManga()
            if (libraryPreferences.groupBy().get() == LibraryGroup.ByCategory) {
                libraryPreferences.groupBy().set(LibraryGroup.Ungrouped)
                libraryPreferences.groupBy().set(LibraryGroup.ByCategory)
            }
        }
    }

    fun categoryAscendingClick(category: CategoryItem) {
        viewModelScope.launchIO {
            if (category.isDynamic || category.isSystemCategory) {
                libraryPreferences.sortAscending().set(!category.isAscending)
            } else {
                val updatedDbCategory = category.toDbCategory(true)
                db.insertCategory(updatedDbCategory).executeOnIO()
            }
        }
    }

    fun categoryItemLibrarySortClick(category: CategoryItem, librarySort: LibrarySort) {
        viewModelScope.launchIO {
            if (category.isDynamic || category.isSystemCategory) {
                libraryPreferences.sortingMode().set(librarySort.mainValue)
            } else {
                val updatedDbCategory =
                    category.copy(isAscending = true, sortOrder = librarySort).toDbCategory()
                db.insertCategory(updatedDbCategory).executeOnIO()
            }
        }
    }

    fun groupByDynamic(
        libraryMangaList: List<LibraryMangaItem>,
        collapsedDynamicCategorySet: Set<String>,
        currentLibraryGroup: LibraryGroup,
        sortOrder: LibrarySort,
        sortAscending: Boolean,
        loggedInTrackStatus: Map<Long, List<String>>,
    ): PersistentList<LibraryCategoryItem> {
        val groupedMap = mutableMapOf<String, MutableList<LibraryMangaItem>>()
        val notTrackedList = listOf("Not tracked")
        val groupTypeStr = currentLibraryGroup.type.toString()

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
                        isHidden =
                            (groupTypeStr + dynamicCategorySplitter + categoryName) in
                                collapsedDynamicCategorySet,
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
        collapsedCategorySet: Set<Int>,
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

            val updatedCategoryItem =
                categoryItem.copy(isHidden = categoryItem.id in collapsedCategorySet)

            LibraryCategoryItem(
                categoryItem = updatedCategoryItem,
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
                newSelectedItems =
                    currentSelected.filter { it.displayManga.mangaId !in categoryItemIds }
            } else {
                // Select all
                val itemsToAdd =
                    libraryMangaItems.filter { it.displayManga.mangaId !in selectedItemIds }

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
            val dbMangas = db.getMangas(mangaIds).executeOnIO()

            dbMangas.forEach { dbManga ->
                dbManga.favorite = false
            }

            db.insertMangaList(dbMangas).executeOnIO()

            dbMangas.forEach { dbManga ->
                coverCache.deleteFromCache(dbManga)
                downloadManager.deleteManga(dbManga)
            }
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
                        db.getCategoriesForManga(libraryMangaItem.displayManga.mangaId)
                            .executeOnIO()
                            .map { it.toCategoryItem() }
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
            val mangasMap = db.getMangas(mangaIds).executeOnIO().associateBy { it.id }
            val chaptersMap = db.getChapters(mangaIds).executeOnIO().groupBy { it.manga_id }

            selectedItems.mapAsync { selectedItem ->
                val mangaId = selectedItem.displayManga.mangaId
                val dbManga = mangasMap[mangaId] ?: return@mapAsync
                val chapterItems =
                    chaptersMap[mangaId]?.mapNotNull { it.toSimpleChapter()?.toChapterItem() }
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
            val mangasMap = db.getMangas(mangaIds).executeOnIO().associateBy { it.id }
            val chaptersMap = db.getChapters(mangaIds).executeOnIO().groupBy { it.manga_id }

            displayMangaList.mapAsync { displayManga ->
                val dbManga = mangasMap[displayManga.mangaId] ?: return@mapAsync
                val rawChapters =
                    chaptersMap[displayManga.mangaId]?.mapNotNull {
                        it.toSimpleChapter()?.toChapterItem()
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
                        val unreadDbChapters =
                            chapterItems.mapNotNull {
                                if (!it.chapter.read) it.chapter.toDbChapter() else null
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
                        val readDbChapters =
                            chapterItems.mapNotNull {
                                if (it.chapter.read) it.chapter.toDbChapter() else null
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
            val manga = db.getManga(mangaId).executeOnIO()
            manga ?: return@launchIO
            val chapters = db.getChapters(manga).executeAsBlocking()
            val availableChapters = chapters.filter { it.isAvailable(downloadManager, manga) }
            val chapter =
                ChapterItemSort()
                    .getNextUnreadChapter(
                        manga,
                        availableChapters.map { it.toSimpleChapter()!!.toChapterItem() },
                    )
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
