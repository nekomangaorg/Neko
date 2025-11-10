package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastAny
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
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
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
import eu.kanade.tachiyomi.util.isAvailable
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.toLibraryMangaItem
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases
import org.nekomanga.util.system.filterAsync
import org.nekomanga.util.system.mapAsync
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryPresenter(
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    val securityPreferences: SecurityPreferences = Injekt.get(),
    val mangadexPreferences: MangaDexPreferences = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    val chapterFilter: ChapterFilter = Injekt.get(),
    val workManager: WorkManager = Injekt.get(),
    val chapterUseCases: ChapterUseCases = Injekt.get(),
) : BaseCoroutinePresenter<LibraryController>() {

    private val _libraryScreenState = MutableStateFlow(LibraryScreenState())
    private val _mangaRefreshingState = MutableStateFlow(emptySet<Long>())

    private val loggedServices by lazy {
        Injekt.get<TrackManager>().services.values.filter { it.isLogged() || it.isMdList() }
    }

    val libraryScreenState: StateFlow<LibraryScreenState> = _libraryScreenState.asStateFlow()

    fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        flow7: Flow<T7>,
        flow8: Flow<T8>,
        transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
    ): Flow<R> =
        combine(
            combine(flow, flow2, flow3, ::Triple),
            combine(flow4, flow5, flow6, ::Triple),
            combine(flow7, flow8, ::Pair),
        ) { t1, t2, t3 ->
            transform(
                t1.first,
                t1.second,
                t1.third,
                t2.first,
                t2.second,
                t2.third,
                t3.first,
                t3.second,
            )
        }

    /** Save the current list to speed up loading later */
    override fun onDestroy() {
        super.onDestroy()
        lastLibraryCategoryItems = _libraryScreenState.value.items
    }

    override fun onCreate() {
        super.onCreate()
        presenterScope.launchIO {
            if (lastLibraryCategoryItems == null) {
                _libraryScreenState.update { it.copy(isFirstLoad = true) }
                // inital fast load
                val mangaList = libraryMangaListFlow.first()
                val categoryList = categoryListFlow.first()
                val libraryViewPreferences = libraryViewFlow.first()

                val collapsedCategorySet =
                    libraryViewPreferences.collapsedCategories
                        .mapNotNull { it.toIntOrNull() }
                        .toSet()

                val unsortedLibraryCategoryItems =
                    when (libraryViewPreferences.groupBy) {
                        LibraryGroup.ByCategory -> {
                            groupByCategory(mangaList, categoryList, collapsedCategorySet)
                        }
                        LibraryGroup.ByAuthor,
                        LibraryGroup.ByContent,
                        LibraryGroup.ByLanguage,
                        LibraryGroup.ByStatus,
                        LibraryGroup.ByTag -> {
                            groupByDynamic(
                                libraryMangaList = mangaList,
                                collapsedDynamicCategorySet =
                                    libraryViewPreferences.collapsedDynamicCategories,
                                currentLibraryGroup = libraryViewPreferences.groupBy,
                                sortOrder = libraryViewPreferences.sortingMode,
                                sortAscending = libraryViewPreferences.sortAscending,
                                loggedInTrackStatus = emptyMap(),
                            )
                        }
                        // by track status requires an extra network call, so we will just show
                        // ungrouped for the fast path
                        LibraryGroup.ByTrackStatus -> {
                            groupByUngrouped(
                                mangaList,
                                libraryViewPreferences.sortingMode,
                                libraryViewPreferences.sortAscending,
                            )
                        }
                        LibraryGroup.Ungrouped -> {
                            groupByUngrouped(
                                mangaList,
                                libraryViewPreferences.sortingMode,
                                libraryViewPreferences.sortAscending,
                            )
                        }
                    }

                _libraryScreenState.update {
                    it.copy(items = unsortedLibraryCategoryItems.toPersistentList())
                }
            } else {
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
            }

            lastLibraryCategoryItems = null
        }
        observeLibraryUpdates()
        preferenceUpdates()

        presenterScope.launchIO {
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
            _libraryScreenState.update { it.copy(groupByOptions = groupItems.toPersistentList()) }
        }

        presenterScope.launchIO {
            combine(
                    filteredMangaListFlow,
                    categoryListFlow,
                    filterPreferencesFlow,
                    trackMapFlow,
                    libraryViewFlow,
                    _mangaRefreshingState.asStateFlow(),
                    lastReadMangaFlow,
                    lastFetchMangaFlow,
                ) {
                    filteredMangaList,
                    categoryList,
                    libraryFilters,
                    trackMap,
                    libraryViewPreferences,
                    mangaRefreshingState,
                    lastReadMap,
                    lastFetchMap ->
                    val collapsedCategorySet =
                        libraryViewPreferences.collapsedCategories
                            .mapNotNull { it.toIntOrNull() }
                            .toSet()

                    val removeArticles = libraryPreferences.removeArticles().get()

                    val downloadCountMapAsync = async {
                        downloadManager.getDownloadCounts(
                            filteredMangaList.map { it.displayManga.toDbManga() }
                        )
                    }

                    val layout = libraryPreferences.layout().get()
                    val gridSize = libraryPreferences.gridSize().get()

                    val unsortedLibraryCategoryItems =
                        when (libraryViewPreferences.groupBy) {
                            LibraryGroup.ByCategory -> {
                                groupByCategory(
                                    filteredMangaList,
                                    categoryList,
                                    collapsedCategorySet,
                                )
                            }
                            LibraryGroup.ByAuthor,
                            LibraryGroup.ByContent,
                            LibraryGroup.ByLanguage,
                            LibraryGroup.ByStatus,
                            LibraryGroup.ByTag,
                            LibraryGroup.ByTrackStatus -> {
                                groupByDynamic(
                                    libraryMangaList = filteredMangaList,
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
                                    filteredMangaList,
                                    libraryViewPreferences.sortingMode,
                                    libraryViewPreferences.sortAscending,
                                )
                            }
                        }

                    var allCollapsed = true

                    val downloadCountMap = downloadCountMapAsync.await()
                    val items =
                        unsortedLibraryCategoryItems
                            .mapAsync { libraryCategoryItem ->
                                if (!libraryCategoryItem.categoryItem.isHidden) {
                                    allCollapsed = false
                                }

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

                                val sortedMangaList =
                                    libraryCategoryItem.libraryItems
                                        .distinctBy { it.displayManga.mangaId }
                                        .mapAsync { item ->
                                            item.copy(
                                                downloadCount =
                                                    downloadCountMap[item.displayManga.mangaId]
                                                        ?: 0,
                                                trackCount =
                                                    trackMap[item.displayManga.mangaId]?.size ?: 0,
                                            )
                                        }
                                        .sortedWith(comparator)
                                        .applyFilters(libraryFilters, trackMap)
                                        .toPersistentList()

                                val isRefreshing =
                                    libraryCategoryItem.libraryItems.fastAny {
                                        it.displayManga.mangaId in mangaRefreshingState
                                    }

                                libraryCategoryItem.copy(
                                    libraryItems = sortedMangaList,
                                    isRefreshing = isRefreshing,
                                )
                            }
                            .sortedBy { it.libraryItems.isEmpty() }
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

    val libraryMangaListFlow: Flow<List<LibraryMangaItem>> =
        db.getLibraryMangaList()
            .asFlow()
            .map { dbManga -> dbManga.map { it.toLibraryMangaItem() } }
            .distinctUntilChanged()
            .conflate()

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

    val trackMapFlow: Flow<Map<Long, List<String>>> =
        db.getAllTracks()
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

    val lastReadMangaFlow =
        db.getLastReadManga()
            .asFlow()
            .map { list -> list.mapIndexed { index, manga -> manga.id!! to index }.toMap() }
            .conflate()

    val lastFetchMangaFlow =
        db.getLastFetchedManga()
            .asFlow()
            .map { list -> list.mapIndexed { index, manga -> manga.id!! to index }.toMap() }
            .conflate()

    val filteredMangaListFlow =
        combine(
                libraryMangaListFlow,
                libraryScreenState
                    .map { it.searchQuery }
                    .distinctUntilChanged()
                    .debounce(SEARCH_DEBOUNCE_MILLIS),
            ) { mangaList, searchQuery ->
                _libraryScreenState.update { it.copy(initialSearch = "") }

                if (searchQuery.isNullOrBlank()) {
                    mangaList
                } else {
                    mangaList.filter { libraryMangaItem -> libraryMangaItem.matches(searchQuery) }
                }
            }
            .conflate()

    fun preferenceUpdates() {

        presenterScope.launchIO {
            preferences.useVividColorHeaders().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _libraryScreenState.update { it.copy(useVividColorHeaders = enabled) }
            }
        }

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
            libraryPreferences
                .libraryHorizontalCategories()
                .changes()
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    _libraryScreenState.update { it.copy(horizontalCategories = enabled) }
                }
        }

        presenterScope.launchIO {
            libraryPreferences
                .showLibraryButtonBar()
                .changes()
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    _libraryScreenState.update { it.copy(showLibraryButtonBar = enabled) }
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

    private suspend fun List<LibraryMangaItem>.applyFilters(
        libraryFilters: LibraryFilters,
        trackMap: Map<Long, List<String>>,
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

            completedCondition &&
                downloadedCondition &&
                unreadCondition &&
                trackedCondition &&
                missingChaptersCondition &&
                bookmarkedCondition &&
                mergedCondition &&
                unavailableCondition &&
                mangaTypeCondition
        }
    }

    fun categoryItemClick(category: CategoryItem) {
        presenterScope.launchIO {
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

    fun horizontalCategoriesToggled() {
        presenterScope.launchIO { libraryPreferences.libraryHorizontalCategories().toggle() }
    }

    fun showLibraryButtonBarToggled() {
        presenterScope.launchIO { libraryPreferences.showLibraryButtonBar().toggle() }
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

            mangaList.mapAsync { manga ->
                val dbManga = db.getManga(manga.mangaId).executeOnIO()!!
                val mangaCategories = dbCategories.map { MangaCategory.create(dbManga, it) }
                db.setMangaCategories(mangaCategories, listOf(dbManga))
            }
            clearSelectedManga()
            if (libraryPreferences.groupBy().get() == LibraryGroup.ByCategory) {
                libraryPreferences.groupBy().set(LibraryGroup.Ungrouped)
                libraryPreferences.groupBy().set(LibraryGroup.ByCategory)
            }
        }
    }

    fun categoryAscendingClick(category: CategoryItem) {
        presenterScope.launchIO {
            if (category.isDynamic || category.isSystemCategory) {
                libraryPreferences.sortAscending().set(!category.isAscending)
            } else {
                val updatedDbCategory = category.toDbCategory(true)
                db.insertCategory(updatedDbCategory).executeOnIO()
            }
        }
    }

    fun categoryItemLibrarySortClick(category: CategoryItem, librarySort: LibrarySort) {
        presenterScope.launchIO {
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

        for (libraryMangaItem in libraryMangaList) {
            val groupingKeys =
                when (currentLibraryGroup) {
                    LibraryGroup.ByAuthor -> libraryMangaItem.author
                    LibraryGroup.ByContent -> libraryMangaItem.contentRating
                    LibraryGroup.ByLanguage -> libraryMangaItem.language
                    LibraryGroup.ByStatus -> libraryMangaItem.status
                    LibraryGroup.ByTag -> libraryMangaItem.genre
                    LibraryGroup.ByTrackStatus -> {
                        loggedInTrackStatus[libraryMangaItem.displayManga.mangaId]
                            ?: listOf("Not tracked")
                    }
                    else -> libraryMangaItem.language
                }

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
                            dynamicCategoryName(currentLibraryGroup.type, categoryName) in
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
        presenterScope.launchIO {
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

        presenterScope.launchIO {
            filterPreferencesFlow.distinctUntilChanged().collectLatest { libraryFilters ->
                _libraryScreenState.update {
                    it.copy(
                        libraryFilters = libraryFilters,
                        hasActiveFilters = libraryFilters.hasActiveFilter(),
                    )
                }
            }
        }

        presenterScope.launchIO {
            downloadManager
                .statusFlow()
                .catch { error -> TimberKt.e(error) }
                .collect { download ->
                    if (download.status == Download.State.DOWNLOADED) {
                        updateDownloadBadges(download.mangaItem.id)
                    }
                }
        }

        presenterScope.launchIO {
            downloadManager.removedChaptersFlow.collect { id -> updateDownloadBadges(id) }
        }
    }

    fun updateDownloadBadges(mangaId: Long) {
        presenterScope.launchIO {
            val manga = db.getManga(mangaId).executeOnIO() ?: return@launchIO
            val downloadCount = downloadManager.getDownloadCount(manga)
            _libraryScreenState.update { state ->
                val newItems =
                    state.items
                        .map { categoryItem ->
                            categoryItem.copy(
                                libraryItems =
                                    categoryItem.libraryItems
                                        .map { libraryMangaItem ->
                                            if (libraryMangaItem.displayManga.mangaId == mangaId) {
                                                libraryMangaItem.copy(downloadCount = downloadCount)
                                            } else {
                                                libraryMangaItem
                                            }
                                        }
                                        .toPersistentList()
                            )
                        }
                        .toPersistentList()
                state.copy(items = newItems)
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

    fun toggleIncognitoMode() {
        presenterScope.launchIO { securityPreferences.incognitoMode().toggle() }
    }

    fun initialSearch(searchQuery: String) {
        _libraryScreenState.update {
            it.copy(searchQuery = searchQuery, initialSearch = searchQuery)
        }
    }

    fun search(searchQuery: String?) {
        _libraryScreenState.update { it.copy(searchQuery = searchQuery) }
    }

    /** sync selected manga to mangadex follows */
    fun syncMangaToDex() {
        presenterScope.launchIO {
            val mangaIds =
                _libraryScreenState.value.selectedItems
                    .map { it.displayManga.mangaId }
                    .distinct()
                    .joinToString()
            StatusSyncJob.startNow(workManager, mangaIds)
            _libraryScreenState.update { it.copy(selectedItems = persistentListOf()) }
        }
    }

    fun selectAllLibraryMangaItems(libraryMangaItems: List<LibraryMangaItem>) {
        presenterScope.launchIO {
            val currentSelected = _libraryScreenState.value.selectedItems.toList()

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

            _libraryScreenState.update {
                it.copy(selectedItems = newSelectedItems.toPersistentList())
            }
        }
    }

    fun deleteSelectedLibraryMangaItems() {
        presenterScope.launchNonCancellable {
            val currentSelected = _libraryScreenState.value.selectedItems.toList()
            clearSelectedManga()
            currentSelected.mapAsync { libraryMangaItem ->
                val dbManga = db.getManga(libraryMangaItem.displayManga.mangaId).executeOnIO()!!
                dbManga.favorite = false
                db.insertManga(dbManga).executeOnIO()
                coverCache.deleteFromCache(dbManga)
                downloadManager.deleteManga(dbManga)
            }
        }
    }

    fun libraryItemLongClick(libraryMangaItem: LibraryMangaItem) {
        presenterScope.launchIO {
            val index =
                _libraryScreenState.value.selectedItems.indexOfFirst {
                    it.displayManga.mangaId == libraryMangaItem.displayManga.mangaId
                }
            val updatedSelectedItems =
                if (index >= 0) {
                    _libraryScreenState.value.selectedItems.removeAt(index)
                } else {
                    val categoryItems =
                        db.getCategoriesForManga(libraryMangaItem.displayManga.mangaId)
                            .executeOnIO()
                            .map { it.toCategoryItem() }
                    val copy = libraryMangaItem.copy(allCategories = categoryItems)
                    _libraryScreenState.value.selectedItems.add(copy)
                }

            _libraryScreenState.update {
                it.copy(selectedItems = updatedSelectedItems.distinct().toPersistentList())
            }
        }
    }

    fun markChapters(markAction: ChapterMarkActions) {
        presenterScope.launchIO {
            val selectedItems = _libraryScreenState.value.selectedItems
            _libraryScreenState.update { it.copy(selectedItems = persistentListOf()) }

            selectedItems.mapAsync { selectedItem ->
                val dbManga = db.getManga(selectedItem.displayManga.mangaId).executeOnIO()!!
                val chapterItems =
                    db.getChapters(selectedItem.displayManga.mangaId).executeOnIO().map {
                        it.toSimpleChapter()!!.toChapterItem()
                    }
                chapterUseCases.markChapters(markAction, chapterItems)
                chapterUseCases.markChaptersRemote(markAction, dbManga.uuid(), chapterItems)
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
                    downloadAction == DownloadAction.ImmediateDownload
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
                    }
                    DownloadAction.RemoveRead -> {
                        val readDbChapters = dbChapters.filter { it.read }
                        downloadManager.deleteChapters(dbManga, readDbChapters)
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
            val availableChapters =
                chapters.filter { it.isAvailable(downloadManager, manga) }
            val chapter =
                ChapterSort(manga, chapterFilter, preferences).getNextUnreadChapter(availableChapters)
            chapter ?: return@launchIO
            openChapter(manga, chapter)
        }
    }

    fun getSelectedMangaUrls(): String {
        val urls =
            _libraryScreenState.value.selectedItems
                .map { selected -> selected.url }
                .distinct()
                .joinToString("\n")
        presenterScope.launchIO { clearSelectedManga() }
        return urls
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
