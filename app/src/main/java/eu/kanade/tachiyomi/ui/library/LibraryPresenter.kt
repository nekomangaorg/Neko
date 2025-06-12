package eu.kanade.tachiyomi.ui.library

import android.app.Application
import android.content.Context
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Chapter.Companion.copy
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.minusAssign
import eu.kanade.tachiyomi.data.preference.plusAssign
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.follows.StatusSyncJob
import eu.kanade.tachiyomi.jobs.library.DelayedLibrarySuggestionsJob
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.merged.mangalife.MangaLife
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.feed.FeedRepository
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_AUTHOR
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_CONTENT
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_DEFAULT
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_LANGUAGE
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_TAG
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_TRACK_STATUS
import eu.kanade.tachiyomi.ui.library.LibraryGroup.UNGROUPED
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet.Companion.STATE_EXCLUDE
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet.Companion.STATE_IGNORE
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet.Companion.STATE_INCLUDE
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.getSlug
import eu.kanade.tachiyomi.util.lang.capitalizeWords
import eu.kanade.tachiyomi.util.lang.chopByWords
import eu.kanade.tachiyomi.util.lang.removeArticles
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.withUIContext
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/** Presenter of [LibraryController]. */
class LibraryPresenter(
    val db: DatabaseHelper = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val chapterFilter: ChapterFilter = Injekt.get(),
) : BaseCoroutinePresenter<LibraryController>() {

    private val context: Context by lazy { Injekt.get<Application>().applicationContext }

    private val loggedServices by lazy {
        Injekt.get<TrackManager>().services.values.filter { it.isLogged() || it.isMdList() }
    }

    private val statusHandler: StatusHandler by injectLazy()

    var groupType = libraryPreferences.groupBy().get()

    val isLoggedIntoTracking
        get() = loggedServices.isNotEmpty()

    private val loginHelper by lazy { Injekt.get<MangaDexLoginHelper>() }

    /** Current categories of the library. */
    var categories: List<Category> = emptyList()
        private set

    private var removeArticles: Boolean = libraryPreferences.removeArticles().get()

    /** All categories of the library, in case they are hidden because of hide categories is on */
    var allCategories: List<Category> = emptyList()
        private set

    /** List of all manga to update the */
    var libraryItems: List<LibraryItem> = emptyList()
    private var sectionedLibraryItems: MutableMap<Int, List<LibraryItem>> = mutableMapOf()
    var currentCategory = -1
        private set

    var allLibraryItems: List<LibraryItem> = emptyList()
        private set

    var hiddenLibraryItems: List<LibraryItem> = emptyList()
    var forceShowAllCategories = false
    val showAllCategories
        get() = forceShowAllCategories || libraryPreferences.showAllCategories().get()

    private val libraryIsGrouped
        get() = groupType != UNGROUPED

    var hasActiveFilters: Boolean = run {
        val filterDownloaded = libraryPreferences.filterDownloaded().get()

        val filterUnread = libraryPreferences.filterUnread().get()

        val filterCompleted = libraryPreferences.filterCompleted().get()

        val filterTracked = libraryPreferences.filterTracked().get()

        val filterMangaType = libraryPreferences.filterMangaType().get()

        val filterMissingChapters = libraryPreferences.filterMissingChapters().get()

        val filterMerged = libraryPreferences.filterMerged().get()

        !(filterDownloaded == 0 &&
            filterUnread == 0 &&
            filterCompleted == 0 &&
            filterTracked == 0 &&
            filterMangaType == 0 &&
            filterMissingChapters == 0 &&
            filterMerged == 0)
    }

    /** Save the current list to speed up loading later */
    override fun onDestroy() {
        super.onDestroy()
        lastLibraryItems = libraryItems
        lastCategories = categories
    }

    override fun onCreate() {
        super.onCreate()
        lastLibraryItems?.let { libraryItems = it }
        lastCategories?.let { categories = it }
        lastCategories = null
        lastLibraryItems = null
        getLibrary()
        if (!libraryPreferences.showSearchSuggestions().isSet()) {
            DelayedLibrarySuggestionsJob.setupTask(context, true)
        } else if (
            libraryPreferences.showSearchSuggestions().get() &&
                Date().time >=
                    libraryPreferences.lastSearchSuggestion().get() + TimeUnit.HOURS.toMillis(2)
        ) {
            // Doing this instead of a job in case the app isn't used often
            presenterScope.launchIO {
                setSearchSuggestion(libraryPreferences, db, sourceManager)
                withUIContext { view?.setTitle() }
            }
        }
    }

    fun getItemCountInCategories(categoryId: Int): Int {
        val items = sectionedLibraryItems[categoryId]
        return if (
            items?.firstOrNull()?.manga?.isHidden() == true ||
                items?.firstOrNull()?.manga?.isBlank() == true
        ) {
            items.firstOrNull()?.manga?.read ?: 0
        } else {
            sectionedLibraryItems[categoryId]?.size ?: 0
        }
    }

    /** Get favorited manga for library and sort and filter it */
    fun getLibrary() {
        if (categories.isEmpty()) {
            val dbCategories = db.getCategories().executeAsBlocking()
            if (
                (dbCategories + Category.createDefault(context)).distinctBy { it.order }.size !=
                    dbCategories.size + 1
            ) {
                reorderCategories(dbCategories)
            }
            categories = lastCategories ?: db.getCategories().executeAsBlocking().toMutableList()
        }
        presenterScope.launch {
            val (library, hiddenItems) = withContext(Dispatchers.IO) { getLibraryFromDB() }
            setDownloadCount(library)
            setUnreadBadge(library)
            setDownloadCount(hiddenItems)
            setUnreadBadge(hiddenItems)
            allLibraryItems = library
            hiddenLibraryItems = hiddenItems
            var mangaMap = library
            mangaMap = applyFilters(mangaMap)
            mangaMap = applySort(mangaMap)
            val freshStart = libraryItems.isEmpty()
            sectionLibrary(mangaMap, freshStart)
        }
    }

    private fun reorderCategories(categories: List<Category>) {
        val sortedCategories = categories.sortedBy { it.order }
        sortedCategories.forEachIndexed { i, category -> category.order = i }
        db.insertCategories(sortedCategories).executeAsBlocking()
    }

    fun getCurrentCategory() = categories.find { it.id == currentCategory }

    fun switchSection(order: Int) {
        libraryPreferences.lastUsedCategory().set(order)
        val category = categories.find { it.order == order }?.id ?: return
        currentCategory = category
        view?.onNextLibraryUpdate(sectionedLibraryItems[currentCategory] ?: blankItem())
    }

    fun blankItem(id: Int = currentCategory): List<LibraryItem> {
        return listOf(
            LibraryItem(LibraryManga.createBlank(id), LibraryHeaderItem({ getCategory(id) }, id))
        )
    }

    fun restoreLibrary() {
        val items = libraryItems
        val show = showAllCategories || !libraryIsGrouped || categories.size == 1
        sectionedLibraryItems = items.groupBy { it.header.category.id!! }.toMutableMap()
        if (!show && currentCategory == -1) {
            currentCategory =
                categories.find { it.order == libraryPreferences.lastUsedCategory().get() }?.id ?: 0
        }
        view?.onNextLibraryUpdate(
            if (!show) {
                sectionedLibraryItems[currentCategory]
                    ?: sectionedLibraryItems[categories.first().id]
                    ?: blankItem()
            } else {
                libraryItems
            },
            true,
        )
    }

    fun getMangaInCategories(catId: Int?): List<LibraryManga>? {
        catId ?: return null
        return allLibraryItems.filter { it.header.category.id == catId }.map { it.manga }
    }

    private suspend fun sectionLibrary(items: List<LibraryItem>, freshStart: Boolean = false) {
        libraryItems = items
        val showAll = showAllCategories || !libraryIsGrouped || categories.size <= 1
        sectionedLibraryItems = items.groupBy { it.header.category.id ?: 0 }.toMutableMap()
        if (!showAll && currentCategory == -1) {
            currentCategory =
                categories.find { it.order == libraryPreferences.lastUsedCategory().get() }?.id ?: 0
        }
        withUIContext {
            view?.onNextLibraryUpdate(
                if (!showAll) {
                    sectionedLibraryItems[currentCategory]
                        ?: sectionedLibraryItems[categories.first().id]
                        ?: blankItem()
                } else {
                    libraryItems
                },
                freshStart,
            )
        }
    }

    /**
     * Applies library filters to the given list of manga.
     *
     * @param items the items to filter.
     */
    private fun applyFilters(items: List<LibraryItem>): List<LibraryItem> {
        val filterDownloaded = libraryPreferences.filterDownloaded().get()

        val filterUnread = libraryPreferences.filterUnread().get()

        val filterCompleted = libraryPreferences.filterCompleted().get()

        val filterTracked = libraryPreferences.filterTracked().get()

        val filterMangaType = libraryPreferences.filterMangaType().get()

        val filterBookmarked = libraryPreferences.filterBookmarked().get()

        val filterUnavailable = libraryPreferences.filterUnavailable().get()

        val showEmptyCategoriesWhileFiltering =
            libraryPreferences.showEmptyCategoriesWhileFiltering().get()

        val filterTrackers = FilterBottomSheet.FILTER_TRACKER

        val filterMerged = libraryPreferences.filterMerged().get()

        val filterMissingChapters = libraryPreferences.filterMissingChapters().get()

        val filtersOff =
            filterDownloaded == 0 &&
                filterUnread == 0 &&
                filterCompleted == 0 &&
                filterTracked == 0 &&
                filterMangaType == 0 &&
                filterUnavailable == 0
        hasActiveFilters = !filtersOff
        val missingCategorySet = categories.mapNotNull { it.id }.toMutableSet()
        val filteredItems =
            items
                .filter f@{ item ->
                    if (!showEmptyCategoriesWhileFiltering && item.manga.isHidden()) {
                        val subItems =
                            sectionedLibraryItems[item.manga.category]?.takeUnless { it.size <= 1 }
                                ?: hiddenLibraryItems.filter {
                                    it.manga.category == item.manga.category
                                }
                        if (subItems.isEmpty()) {
                            return@f filtersOff
                        } else {
                            return@f subItems.any {
                                matchesFilters(
                                    it,
                                    filterDownloaded,
                                    filterUnread,
                                    filterCompleted,
                                    filterTracked,
                                    filterMangaType,
                                    filterBookmarked,
                                    filterUnavailable,
                                    filterTrackers,
                                    filterMerged,
                                    filterMissingChapters,
                                )
                            }
                        }
                    } else if (item.manga.isBlank() || item.manga.isHidden()) {
                        missingCategorySet.remove(item.manga.category)
                        return@f if (showAllCategories) {
                            filtersOff || showEmptyCategoriesWhileFiltering
                        } else {
                            true
                        }
                    }
                    val matches =
                        matchesFilters(
                            item,
                            filterDownloaded,
                            filterUnread,
                            filterCompleted,
                            filterTracked,
                            filterMangaType,
                            filterBookmarked,
                            filterUnavailable,
                            filterTrackers,
                            filterMerged,
                            filterMissingChapters,
                        )
                    if (matches) {
                        missingCategorySet.remove(item.manga.category)
                    }
                    matches
                }
                .toMutableList()
        if (showEmptyCategoriesWhileFiltering) {
            missingCategorySet.forEach { filteredItems.add(blankItem(it).first()) }
        }
        return filteredItems
    }

    private fun matchesFilters(
        item: LibraryItem,
        filterDownloaded: Int,
        filterUnread: Int,
        filterCompleted: Int,
        filterTracked: Int,
        filterMangaType: Int,
        filterBookmarked: Int,
        filterUnavailable: Int,
        filterTrackers: String,
        filterMerged: Int,
        filterMissingChapters: Int,
    ): Boolean {

        if (filterUnread == STATE_INCLUDE && item.manga.unread == 0) return false
        if (filterUnread == STATE_EXCLUDE && item.manga.unread > 0) return false

        // Filter for unread chapters
        if (filterUnread == 3 && !(item.manga.unread > 0 && !item.manga.hasStarted)) return false
        if (filterUnread == 4 && !(item.manga.unread > 0 && item.manga.hasStarted)) return false

        if (filterBookmarked == STATE_INCLUDE && item.manga.bookmarkCount == 0) return false
        if (filterBookmarked == STATE_EXCLUDE && item.manga.bookmarkCount > 0) return false

        if (filterUnavailable == STATE_INCLUDE && item.manga.unavailableCount == 0) return false
        if (filterUnavailable == STATE_EXCLUDE && item.manga.availableCount == 0) return false

        if (filterMangaType > 0) {
            if (
                if (filterMangaType == Manga.TYPE_MANHWA) {
                    (filterMangaType != item.manga.seriesType() &&
                        filterMangaType != Manga.TYPE_WEBTOON)
                } else {
                    filterMangaType != item.manga.seriesType()
                }
            ) {
                return false
            }
        }

        // Filter for completed status of manga
        if (filterCompleted == STATE_INCLUDE && item.manga.status != SManga.COMPLETED) return false
        if (filterCompleted == STATE_EXCLUDE && item.manga.status == SManga.COMPLETED) return false

        if (filterMerged != STATE_IGNORE) {
            val hasMerged = db.getMergeMangaList(item.manga).executeAsBlocking().isNotEmpty()
            if (filterMerged == STATE_INCLUDE && !hasMerged) return false
            if (filterMerged == STATE_EXCLUDE && hasMerged) return false
        }

        if (filterMissingChapters == STATE_INCLUDE && item.manga.missing_chapters == null)
            return false
        if (filterMissingChapters == STATE_EXCLUDE && item.manga.missing_chapters != null)
            return false

        // Filter for tracked (or per tracked service)
        if (filterTracked != STATE_IGNORE) {
            val tracks = db.getTracks(item.manga).executeAsBlocking()

            val hasTrack =
                loggedServices.any { service ->
                    tracks.any {
                        if (
                            service.isMdList() &&
                                (!loginHelper.isLoggedIn() ||
                                    it.status == FollowStatus.UNFOLLOWED.int)
                        ) {
                            false
                        } else {
                            it.sync_id == service.id
                        }
                    }
                }
            val service =
                if (filterTrackers.isNotEmpty()) {
                    loggedServices.find { context.getString(it.nameRes()) == filterTrackers }
                } else {
                    null
                }
            if (filterTracked == STATE_INCLUDE) {
                if (!hasTrack) return false
                if (filterTrackers.isNotEmpty()) {
                    if (service != null) {
                        val hasServiceTrack = tracks.any { it.sync_id == service.id }
                        if (!hasServiceTrack) return false
                        if (filterTracked == STATE_EXCLUDE && hasServiceTrack) return false
                    }
                }
            } else if (filterTracked == STATE_EXCLUDE) {
                if (hasTrack && filterTrackers.isEmpty()) return false
                if (filterTrackers.isNotEmpty()) {
                    if (service != null) {
                        val hasServiceTrack =
                            tracks.any {
                                if (!service.isMdList()) {
                                    it.sync_id == service.id
                                } else {
                                    FollowStatus.UNFOLLOWED != FollowStatus.fromInt(it.status)
                                }
                            }
                        if (hasServiceTrack) return false
                    }
                }
            }
        }
        // Filter for downloaded manga
        if (filterDownloaded != STATE_IGNORE) {
            val isDownloaded =
                when {
                    item.downloadCount != -1 -> item.downloadCount > 0
                    else -> downloadManager.getDownloadCount(item.manga) > 0
                }
            return if (filterDownloaded == STATE_INCLUDE) isDownloaded else !isDownloaded
        }

        return true
    }

    /**
     * Sets downloaded chapter count to each manga.
     *
     * @param itemList the map of manga.
     */
    private fun setDownloadCount(itemList: List<LibraryItem>) {
        if (!libraryPreferences.showDownloadBadge().get()) {
            // Unset download count if the preference is not enabled.
            for (item in itemList) {
                item.downloadCount = -1
            }
            return
        }

        for (item in itemList) {
            item.downloadCount = downloadManager.getDownloadCount(item.manga)
        }
    }

    private fun setUnreadBadge(itemList: List<LibraryItem>) {
        val unreadType = libraryPreferences.unreadBadgeType().get()
        for (item in itemList) {
            item.unreadType = unreadType
        }
    }

    /**
     * Applies library sorting to the given list of manga.
     *
     * @param itemList the map to sort.
     */
    private fun applySort(itemList: List<LibraryItem>): List<LibraryItem> {
        val lastReadManga by lazy {
            var counter = 0
            db.getLastReadManga().executeAsBlocking().associate { it.id!! to counter++ }
        }

        val lastFetchedManga by lazy {
            var counter = 0
            db.getLastFetchedManga().executeAsBlocking().associate { it.id!! to counter++ }
        }

        val sortFn: (LibraryItem, LibraryItem) -> Int = { i1, i2 ->
            if (i1.header.category.id == i2.header.category.id) {
                val category = i1.header.category
                if (category.mangaOrder.isEmpty() && category.mangaSort == null) {
                    category.changeSortTo(libraryPreferences.sortingMode().get())
                    if (category.id == 0) {
                        libraryPreferences.defaultMangaOrder().set(category.mangaSort.toString())
                    } else if (!category.isDynamic) db.insertCategory(category).executeAsBlocking()
                }
                val compare =
                    when {
                        category.mangaSort != null -> {
                            var sort =
                                when (category.sortingMode() ?: LibrarySort.Title) {
                                    LibrarySort.Title -> sortAlphabetical(i1, i2)
                                    LibrarySort.LatestChapter ->
                                        i2.manga.last_update.compareTo(i1.manga.last_update)
                                    LibrarySort.Unread ->
                                        when {
                                            i1.manga.unread == i2.manga.unread -> 0
                                            i1.manga.unread == 0 ->
                                                if (category.isAscending()) 1 else -1
                                            i2.manga.unread == 0 ->
                                                if (category.isAscending()) -1 else 1
                                            else -> i2.manga.unread.compareTo(i1.manga.unread)
                                        }
                                    LibrarySort.LastRead -> {
                                        val manga1LastRead =
                                            lastReadManga[i1.manga.id!!] ?: lastReadManga.size
                                        val manga2LastRead =
                                            lastReadManga[i2.manga.id!!] ?: lastReadManga.size
                                        manga1LastRead.compareTo(manga2LastRead)
                                    }
                                    LibrarySort.TotalChapters -> {
                                        i2.manga.totalChapters.compareTo(i1.manga.totalChapters)
                                    }
                                    LibrarySort.DateFetched -> {
                                        val manga1LastRead =
                                            lastFetchedManga[i1.manga.id!!] ?: lastFetchedManga.size
                                        val manga2LastRead =
                                            lastFetchedManga[i2.manga.id!!] ?: lastFetchedManga.size
                                        manga1LastRead.compareTo(manga2LastRead)
                                    }
                                    LibrarySort.Rating -> {
                                        if (i2.manga.rating == null && i1.manga.rating != null) {
                                            -1
                                        } else if (
                                            i2.manga.rating != null && i1.manga.rating == null
                                        ) {
                                            1
                                        } else if (
                                            i2.manga.rating == null && i1.manga.rating == null
                                        ) {
                                            0
                                        } else {
                                            val i2Rating =
                                                ((i2.manga.rating!!.toDouble() * 100).roundToInt() /
                                                    100.0)
                                            val i1Rating =
                                                ((i1.manga.rating!!.toDouble() * 100).roundToInt() /
                                                    100.0)
                                            i2Rating.compareTo(i1Rating)
                                        }
                                    }
                                    LibrarySort.DateAdded ->
                                        i2.manga.date_added.compareTo(i1.manga.date_added)
                                    LibrarySort.DragAndDrop -> {
                                        if (category.isDynamic) {
                                            val category1 =
                                                allCategories
                                                    .find { i1.manga.category == it.id }
                                                    ?.order ?: 0
                                            val category2 =
                                                allCategories
                                                    .find { i2.manga.category == it.id }
                                                    ?.order ?: 0
                                            category1.compareTo(category2)
                                        } else {
                                            sortAlphabetical(i1, i2)
                                        }
                                    }
                                }
                            if (!category.isAscending()) sort *= -1
                            sort
                        }
                        category.mangaOrder.isNotEmpty() -> {
                            val order = category.mangaOrder
                            val index1 = order.indexOf(i1.manga.id!!)
                            val index2 = order.indexOf(i2.manga.id!!)
                            when {
                                index1 == index2 -> 0
                                index1 == -1 -> -1
                                index2 == -1 -> 1
                                else -> index1.compareTo(index2)
                            }
                        }
                        else -> 0
                    }
                if (compare == 0) {
                    sortAlphabetical(i1, i2)
                } else {
                    compare
                }
            } else {
                val category = i1.header.category.order
                val category2 = i2.header.category.order
                category.compareTo(category2)
            }
        }

        return itemList.sortedWith(Comparator(sortFn))
    }

    /**
     * Gets the category by id
     *
     * @param categoryId id of the categoty to get
     */
    private fun getCategory(categoryId: Int): Category {
        val category = categories.find { categoryId == it.id } ?: createDefaultCategory()
        category.isAlone = categories.size <= 1
        return category
    }

    /**
     * Sort 2 manga by the their title (and remove articles if need be)
     *
     * @param i1 the first manga
     * @param i2 the second manga to compare
     */
    private fun sortAlphabetical(i1: LibraryItem, i2: LibraryItem): Int {
        return if (removeArticles) {
            i1.manga.title.removeArticles().compareTo(i2.manga.title.removeArticles(), true)
        } else {
            i1.manga.title.compareTo(i2.manga.title, true)
        }
    }

    /**
     * Get the categories and all its manga from the database.
     *
     * @return an list of all the manga in a itemized form.
     */
    private fun getLibraryFromDB(): Pair<List<LibraryItem>, List<LibraryItem>> {
        removeArticles = libraryPreferences.removeArticles().get()
        val categories = db.getCategories().executeAsBlocking().toMutableList()
        var libraryManga = db.getLibraryMangaList().executeAsBlocking()
        val showAll = showAllCategories
        if (groupType > BY_DEFAULT) {
            libraryManga = libraryManga.distinctBy { it.id }
        }
        val hiddenItems = mutableListOf<LibraryItem>()

        val items =
            if (groupType <= BY_DEFAULT || !libraryIsGrouped) {
                val categoryAll =
                    Category.createAll(
                        context,
                        libraryPreferences.sortingMode().get(),
                        libraryPreferences.sortAscending().get(),
                    )
                val catItemAll = LibraryHeaderItem({ categoryAll }, -1)
                val categorySet = mutableSetOf<Int>()
                val headerItems =
                    (categories.mapNotNull { category ->
                            val id = category.id
                            if (id == null) {
                                null
                            } else {
                                id to LibraryHeaderItem({ getCategory(id) }, id)
                            }
                        } + (-1 to catItemAll) + (0 to LibraryHeaderItem({ getCategory(0) }, 0)))
                        .toMap()

                val items =
                    libraryManga
                        .mapNotNull {
                            val headerItem =
                                (if (!libraryIsGrouped) {
                                    catItemAll
                                } else {
                                    headerItems[it.category]
                                }) ?: return@mapNotNull null
                            categorySet.add(it.category)
                            LibraryItem(it, headerItem)
                        }
                        .toMutableList()

                val categoriesHidden =
                    if (forceShowAllCategories) {
                        emptySet()
                    } else {
                        libraryPreferences
                            .collapsedCategories()
                            .get()
                            .mapNotNull { it.toIntOrNull() }
                            .toSet()
                    }

                if (categorySet.contains(0)) categories.add(0, createDefaultCategory())
                if (libraryIsGrouped) {
                    categories.forEach { category ->
                        val catId = category.id ?: return@forEach
                        if (
                            catId > 0 &&
                                !categorySet.contains(catId) &&
                                (catId !in categoriesHidden || !showAll)
                        ) {
                            val headerItem = headerItems[catId]
                            if (headerItem != null) {
                                items.add(LibraryItem(LibraryManga.createBlank(catId), headerItem))
                            }
                        } else if (catId in categoriesHidden && showAll && categories.size > 1) {
                            val mangaToRemove = items.filter { it.manga.category == catId }
                            val mergedTitle =
                                mangaToRemove.joinToString("-") {
                                    it.manga.title + "-" + it.manga.author
                                }
                            sectionedLibraryItems[catId] = mangaToRemove
                            hiddenItems.addAll(mangaToRemove)
                            items.removeAll(mangaToRemove)
                            val headerItem = headerItems[catId]
                            if (headerItem != null) {
                                TimberKt.d {
                                    "Dynamic Category library grouped: size ${mangaToRemove.size}"
                                }
                                items.add(
                                    LibraryItem(
                                        LibraryManga.createHide(
                                            catId,
                                            mergedTitle,
                                            mangaToRemove.size,
                                        ),
                                        headerItem,
                                    )
                                )
                            }
                        }
                    }
                }

                categories.forEach {
                    it.isHidden = it.id in categoriesHidden && showAll && categories.size > 1
                }
                this.categories =
                    if (!libraryIsGrouped) {
                        arrayListOf(categoryAll)
                    } else {
                        categories
                    }

                items
            } else {
                val (items, customCategories) = getCustomMangaItems(libraryManga)
                this.categories = customCategories
                items
            }

        this.allCategories = categories

        return items to hiddenItems
    }

    private fun getCustomMangaItems(
        libraryManga: List<LibraryManga>
    ): Pair<List<LibraryItem>, List<Category>> {
        val tagItems: MutableMap<String, LibraryHeaderItem> = mutableMapOf()

        // internal function to make headers
        fun makeOrGetHeader(name: String, checkNameSwap: Boolean = false): LibraryHeaderItem {
            return if (tagItems.containsKey(name)) {
                tagItems[name]!!
            } else {
                if (checkNameSwap && name.contains(" ")) {
                    val swappedName = name.split(" ").reversed().joinToString(" ")
                    if (tagItems.containsKey(swappedName)) {
                        return tagItems[swappedName]!!
                    }
                }
                val headerItem = LibraryHeaderItem({ getCategory(it) }, tagItems.count())
                tagItems[name] = headerItem
                headerItem
            }
        }

        val unknown = context.getString(R.string.unknown)
        val items =
            libraryManga
                .map { manga ->
                    when (groupType) {
                        BY_TAG -> {
                            val tags =
                                if (manga.genre.isNullOrBlank()) {
                                    listOf(unknown)
                                } else {
                                    manga.genre
                                        ?.split(",")
                                        ?.filter { !it.contains("content rating:", true) }
                                        ?.mapNotNull {
                                            val tag = it.trim().capitalizeWords()
                                            tag.ifBlank { null }
                                        } ?: listOf(unknown)
                                }
                            tags.map { LibraryItem(manga, makeOrGetHeader(it)) }
                        }
                        BY_TRACK_STATUS -> {
                            val tracks = db.getTracks(manga).executeAsBlocking()
                            val results =
                                tracks
                                    .mapNotNull { track ->
                                        val service =
                                            Injekt.get<TrackManager>().getService(track.sync_id)
                                        return@mapNotNull when (service?.isLogged()) {
                                            true -> Pair(track, service)
                                            else -> null
                                        }
                                    }
                                    .map { trackAndService ->
                                        trackAndService.second.getGlobalStatus(
                                            trackAndService.first.status
                                        )
                                    }
                                    .distinct()
                                    .map { status -> LibraryItem(manga, makeOrGetHeader(status)) }

                            when (results.isEmpty()) {
                                true ->
                                    listOf(
                                        LibraryItem(
                                            manga,
                                            makeOrGetHeader(context.getString(R.string.not_tracked)),
                                        )
                                    )
                                false -> results
                            }
                        }
                        BY_AUTHOR -> {
                            if (manga.artist.isNullOrBlank() && manga.author.isNullOrBlank()) {
                                listOf(LibraryItem(manga, makeOrGetHeader(unknown)))
                            } else {
                                listOfNotNull(
                                        manga.author.takeUnless { it.isNullOrBlank() },
                                        manga.artist.takeUnless { it.isNullOrBlank() },
                                    )
                                    .map {
                                        it.split(",", "/", " x ", " - ", ignoreCase = true)
                                            .mapNotNull { name ->
                                                val author = name.trim()
                                                author.ifBlank { null }
                                            }
                                    }
                                    .flatten()
                                    .distinct()
                                    .map { LibraryItem(manga, makeOrGetHeader(it, true)) }
                            }
                        }
                        BY_CONTENT -> {
                            val contentRating = manga.getContentRating()
                            if (contentRating.isNullOrBlank()) {
                                listOf(LibraryItem(manga, makeOrGetHeader(unknown)))
                            } else {
                                listOf(LibraryItem(manga, makeOrGetHeader(contentRating)))
                            }
                        }
                        BY_LANGUAGE -> {
                            val language = MdLang.fromIsoCode(manga.lang_flag ?: "###")?.prettyPrint
                            if (language.isNullOrBlank()) {
                                listOf(LibraryItem(manga, makeOrGetHeader(unknown)))
                            } else {
                                listOf(LibraryItem(manga, makeOrGetHeader(language)))
                            }
                        }
                        else -> listOf(LibraryItem(manga, makeOrGetHeader(mapStatus(manga.status))))
                    }
                }
                .flatten()
                .toMutableList()

        val hiddenDynamics = libraryPreferences.collapsedDynamicCategories().get()
        var headers =
            tagItems
                .map { item ->
                    Category.createCustom(
                            item.key,
                            libraryPreferences.sortingMode().get(),
                            libraryPreferences.sortAscending().get(),
                        )
                        .apply {
                            id = item.value.catId
                            isHidden = getDynamicCategoryName(this) in hiddenDynamics
                        }
                }
                .sortedWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) {
                        if (groupType == BY_TRACK_STATUS) {
                            mapTrackingOrder(it.name)
                        } else {
                            it.name
                        }
                    }
                )
        if (libraryPreferences.collapsedDynamicAtBottom().get()) {
            headers = headers.filterNot { it.isHidden } + headers.filter { it.isHidden }
        }
        headers.forEach { category ->
            val catId = category.id ?: return@forEach
            val headerItem = tagItems[category.name]
            if (category.isHidden) {
                val mangaToRemove = items.filter { it.header.catId == catId }
                val mergedTitle =
                    mangaToRemove.joinToString("-") { it.manga.title + "-" + it.manga.author }
                sectionedLibraryItems[catId] = mangaToRemove
                items.removeAll { it.header.catId == catId }
                if (headerItem != null) {
                    TimberKt.d {
                        "Dynamic Category getCustomMangaItem- cat[${catId}] size[${mangaToRemove.size}]"
                    }

                    items.add(
                        LibraryItem(
                            LibraryManga.createHide(catId, mergedTitle, mangaToRemove.size),
                            headerItem,
                        )
                    )
                }
            }
        }

        headers.forEachIndexed { index, category -> category.order = index }
        return items to headers
    }

    private fun mapStatus(status: Int): String {
        return context.getString(
            when (status) {
                SManga.LICENSED -> R.string.licensed
                SManga.ONGOING -> R.string.ongoing
                SManga.COMPLETED -> R.string.completed
                SManga.PUBLICATION_COMPLETE -> R.string.publication_complete
                SManga.CANCELLED -> R.string.cancelled
                SManga.HIATUS -> R.string.hiatus
                else -> R.string.unknown
            }
        )
    }

    private fun mapTrackingOrder(status: String): String {
        with(context) {
            return when (status) {
                getString(R.string.reading),
                getString(R.string.currently_reading) -> "1"
                getString(R.string.rereading) -> "2"
                getString(R.string.plan_to_read),
                getString(R.string.want_to_read) -> "3"
                getString(R.string.on_hold),
                getString(R.string.paused) -> "4"
                getString(R.string.completed) -> "5"
                getString(R.string.dropped) -> "6"
                else -> "7"
            }
        }
    }

    /** Create a default category with the sort set */
    private fun createDefaultCategory(): Category {
        val default = Category.createDefault(view?.applicationContext ?: context)
        default.order = -1
        val defOrder = libraryPreferences.defaultMangaOrder().get()
        if (defOrder.firstOrNull()?.isLetter() == true) {
            default.mangaSort = defOrder.first()
        } else {
            default.mangaOrder = defOrder.split("/").mapNotNull { it.toLongOrNull() }
        }
        return default
    }

    /** Requests the library to be filtered. */
    fun requestFilterUpdate() {
        presenterScope.launch {
            var mangaMap = allLibraryItems
            mangaMap = applyFilters(mangaMap)
            mangaMap = applySort(mangaMap)
            sectionLibrary(mangaMap)
        }
    }

    fun requestBadgeUpdate(badgeUpdate: (List<LibraryItem>) -> Unit) {
        presenterScope.launch {
            val mangaMap = allLibraryItems
            badgeUpdate(mangaMap)
            allLibraryItems = mangaMap
            val current = libraryItems
            badgeUpdate(current)
            sectionLibrary(current)
        }
    }

    /** Requests the library to have download badges added/removed. */
    fun requestDownloadBadgesUpdate() {
        requestBadgeUpdate { setDownloadCount(it) }
    }

    /** Requests the library to have unread badges changed. */
    fun requestUnreadBadgesUpdate() {
        requestBadgeUpdate { setUnreadBadge(it) }
    }

    /** Requests the library to be sorted. */
    private fun requestSortUpdate() {
        presenterScope.launch {
            var mangaMap = libraryItems
            mangaMap = applySort(mangaMap)
            sectionLibrary(mangaMap)
        }
    }

    /**
     * Returns the common categories for the given list of manga.
     *
     * @param mangaList the list of manga.
     */
    fun getCommonCategories(mangaList: List<Manga>): Collection<Category> {
        if (mangaList.isEmpty()) return emptyList()
        return mangaList
            .toSet()
            .map { db.getCategoriesForManga(it).executeAsBlocking() }
            .reduce { set1: Iterable<Category>, set2 ->
                set1.intersect(set2.toSet()).toMutableList()
            }
    }

    fun getMangaUrls(mangaList: List<Manga>): List<String> {
        return mangaList.mapNotNull { manga ->
            val source = sourceManager.get(manga.source) as? HttpSource ?: return@mapNotNull null
            source.mangaDetailsRequest(manga).url.toString() + "/" + manga.getSlug()
        }
    }

    /**
     * Delete the downloaded chapters from the selected manga from the library.
     *
     * @param mangaList the list of manga to delete.
     */
    fun deleteChaptersForManga(mangaList: List<Manga>) {
        presenterScope.launch {
            // Create a set of the list
            val mangaToDeleteChapters = mangaList.distinctBy { it.id }
            mangaToDeleteChapters.forEach {
                val chapters = db.getChapters(it.id!!).executeOnIO()
                deleteChapters(it, chapters)
            }
            getLibrary()
        }
    }

    /**
     * Remove the selected manga from the library.
     *
     * @param mangaList the list of manga to delete.
     */
    fun removeMangaFromLibrary(mangaList: List<Manga>) {
        presenterScope.launch {
            // Create a set of the list
            val mangaToDelete = mangaList.distinctBy { it.id }
            mangaToDelete.forEach { it.favorite = false }
            db.insertMangaList(mangaToDelete).executeOnIO()
            getLibrary()
        }
    }

    /** Remove manga from the library and delete the downloads */
    fun confirmDeletion(mangaList: List<Manga>) {
        presenterScope.launchNonCancellable {
            val mangaToDelete = mangaList.distinctBy { it.id }
            mangaToDelete.forEach { manga ->
                coverCache.deleteFromCache(manga)
                val source = sourceManager.get(manga.source) as? HttpSource
                if (source != null) {
                    downloadManager.deleteManga(manga)
                }
            }
        }
    }

    /** Called when Library Service updates a manga, update the item as well */
    fun updateManga() {
        presenterScope.launch { getLibrary() }
    }

    /** Undo the removal of the manga once in library */
    fun reAddMangaList(mangaList: List<Manga>) {
        presenterScope.launch {
            val mangaToAdd = mangaList.distinctBy { it.id }
            mangaToAdd.forEach { it.favorite = true }
            db.insertMangaList(mangaToAdd).executeOnIO()
            getLibrary()
            mangaToAdd.forEach { db.insertManga(it).executeAsBlocking() }
        }
    }

    /**
     * Move the given list of manga to categories.
     *
     * @param categories the selected categories.
     * @param mangaList the list of manga to move.
     */
    fun moveMangaListToCategories(categories: List<Category>, mangaList: List<Manga>) {
        val mc = ArrayList<MangaCategory>()

        for (manga in mangaList) {
            for (cat in categories) {
                mc.add(MangaCategory.create(manga, cat))
            }
        }
        db.setMangaCategories(mc, mangaList)
        getLibrary()
    }

    /** Returns first unread chapter of a manga */
    fun getFirstUnread(manga: Manga): Chapter? {
        val chapters = db.getChapters(manga).executeAsBlocking()
        return ChapterSort(manga, chapterFilter, preferences).getNextUnreadChapter(chapters)
    }

    /** Update a category's sorting */
    fun sortCategory(catId: Int, order: Char) {
        val category = categories.find { catId == it.id } ?: return
        category.mangaSort = order
        if (catId == -1 || category.isDynamic) {
            val sort = category.sortingMode() ?: LibrarySort.Title
            libraryPreferences.sortingMode().set(sort.mainValue)
            libraryPreferences.sortAscending().set(category.isAscending())
            categories.forEach { it.mangaSort = category.mangaSort }
        } else if (catId >= 0) {
            if (category.id == 0) {
                libraryPreferences.defaultMangaOrder().set(category.mangaSort.toString())
            } else {
                Injekt.get<DatabaseHelper>().insertCategory(category).executeAsBlocking()
            }
        }
        requestSortUpdate()
    }

    /** Update a category's order */
    fun rearrangeCategory(catId: Int?, mangaIds: List<Long>) {
        presenterScope.launch {
            val category = categories.find { catId == it.id } ?: return@launch
            if (category.isDynamic) return@launch
            category.mangaSort = null
            category.mangaOrder = mangaIds
            if (category.id == 0) {
                libraryPreferences.defaultMangaOrder().set(mangaIds.joinToString("/"))
            } else {
                db.insertCategory(category).executeOnIO()
            }
            requestSortUpdate()
        }
    }

    /** Shift a manga's category via drag & drop */
    fun moveMangaToCategory(manga: LibraryManga, catId: Int?, mangaIds: List<Long>) {
        presenterScope.launch {
            val categoryId = catId ?: return@launch
            val category = categories.find { catId == it.id } ?: return@launch
            if (category.isDynamic) return@launch

            val oldCatId = manga.category
            manga.category = categoryId

            val mc = ArrayList<MangaCategory>()
            val categories =
                if (catId == 0) {
                    emptyList()
                } else {
                    db.getCategoriesForManga(manga).executeOnIO().filter { it.id != oldCatId } +
                        listOf(category)
                }

            for (cat in categories) {
                mc.add(MangaCategory.create(manga, cat))
            }

            db.setMangaCategories(mc, listOf(manga))

            if (category.mangaSort == null) {
                val ids = mangaIds.toMutableList()
                if (!ids.contains(manga.id!!)) ids.add(manga.id!!)
                category.mangaOrder = ids
                if (category.id == 0) {
                    libraryPreferences.defaultMangaOrder().set(mangaIds.joinToString("/"))
                } else {
                    db.insertCategory(category).executeAsBlocking()
                }
            }
            getLibrary()
        }
    }

    /** Returns if manga is in a category by id */
    fun mangaIsInCategory(manga: LibraryManga, catId: Int?): Boolean {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking().map { it.id }
        return catId in categories
    }

    fun toggleCategoryVisibility(categoryId: Int) {
        // if (categories.find { it.id == categoryId }?.isDynamic == true) return
        if (groupType == BY_DEFAULT) {
            val categoriesHidden =
                libraryPreferences
                    .collapsedCategories()
                    .get()
                    .mapNotNull { it.toIntOrNull() }
                    .toMutableSet()
            if (categoryId in categoriesHidden) {
                categoriesHidden.remove(categoryId)
            } else {
                categoriesHidden.add(categoryId)
            }
            libraryPreferences
                .collapsedCategories()
                .set(categoriesHidden.map { it.toString() }.toMutableSet())
        } else {
            val categoriesHidden =
                libraryPreferences.collapsedDynamicCategories().get().toMutableSet()
            val category = getCategory(categoryId)
            val dynamicName = getDynamicCategoryName(category)
            if (dynamicName in categoriesHidden) {
                categoriesHidden.remove(dynamicName)
            } else {
                categoriesHidden.add(dynamicName)
            }
            libraryPreferences.collapsedDynamicCategories().set(categoriesHidden)
        }
        getLibrary()
    }

    private fun getDynamicCategoryName(category: Category): String =
        groupType.toString() + dynamicCategorySplitter + category.name

    fun toggleAllCategoryVisibility() {
        if (groupType == BY_DEFAULT) {
            if (allCategoriesExpanded()) {
                libraryPreferences
                    .collapsedCategories()
                    .set(allCategories.map { it.id.toString() }.toMutableSet())
            } else {
                libraryPreferences.collapsedCategories().set(mutableSetOf())
            }
        } else {
            if (allCategoriesExpanded()) {
                libraryPreferences.collapsedDynamicCategories() +=
                    categories.map { getDynamicCategoryName(it) }
            } else {
                libraryPreferences.collapsedDynamicCategories() -=
                    categories.map { getDynamicCategoryName(it) }
            }
        }
        getLibrary()
    }

    fun allCategoriesExpanded(): Boolean {
        return if (groupType == BY_DEFAULT) {
            libraryPreferences.collapsedCategories().get().isEmpty()
        } else {
            categories.none { it.isHidden }
        }
    }

    /** download All unread */
    fun downloadUnread(mangaList: List<Manga>) {
        presenterScope.launch {
            withContext(Dispatchers.IO) {
                mangaList.forEach { manga ->
                    val scanlatorsToIgnore = ChapterUtil.getScanlators(manga.filtered_scanlators)
                    val chapters =
                        db.getChapters(manga).executeAsBlocking().filter { chapter ->
                            !chapter.read &&
                                chapter.scanlatorList().none { scanlator ->
                                    scanlator in scanlatorsToIgnore
                                }
                        }
                    downloadManager.downloadChapters(manga, chapters)
                }
            }
            if (libraryPreferences.showDownloadBadge().get()) {
                requestDownloadBadgesUpdate()
            }
        }
    }

    fun markReadStatus(mangaList: List<Manga>, markRead: Boolean): HashMap<Manga, List<Chapter>> {
        val mapMangaChapters = HashMap<Manga, List<Chapter>>()
        presenterScope.launchIO {
            mangaList.forEach { manga ->
                val oldChapters = db.getChapters(manga).executeAsBlocking()
                val chapters = oldChapters.copy()
                chapters.forEach {
                    it.read = markRead
                    it.last_page_read = 0
                }
                db.updateChaptersProgress(chapters).executeAsBlocking()

                mapMangaChapters[manga] = oldChapters
            }
            getLibrary()
        }
        return mapMangaChapters
    }

    fun undoMarkReadStatus(mangaList: HashMap<Manga, List<Chapter>>) {
        presenterScope.launchNonCancellable {
            mangaList.forEach { (_, chapters) ->
                db.updateChaptersProgress(chapters).executeAsBlocking()
            }
            getLibrary()
        }
    }

    fun confirmMarkReadStatus(mangaList: HashMap<Manga, List<Chapter>>, markRead: Boolean) {
        if (preferences.readingSync().get()) {
            mangaList.forEach { entry ->
                val (mergedChapters, nonMergedChapters) =
                    entry.value.partition { it.isMergedChapter() }
                if (nonMergedChapters.isNotEmpty()) {
                    presenterScope.launch {
                        statusHandler.markChaptersStatus(
                            MdUtil.getMangaUUID(entry.key.url),
                            nonMergedChapters.map { it.mangadex_chapter_id },
                            markRead,
                        )
                    }
                }
                if (mergedChapters.isNotEmpty()) {
                    presenterScope.launch {
                        statusHandler.markMergedChaptersStatus(mergedChapters, markRead)
                    }
                }
            }
        }

        if (preferences.removeAfterMarkedAsRead().get() && markRead) {
            mangaList.forEach { (manga, oldChapters) -> deleteChapters(manga, oldChapters) }
            if (libraryPreferences.showDownloadBadge().get()) {
                requestDownloadBadgesUpdate()
            }
        }
    }

    /** sync selected manga to mangadex follows */
    fun syncMangaToDex(mangaList: List<Manga>) {
        presenterScope.launch {
            withContext(Dispatchers.IO) {
                val mangaIds = mangaList.mapNotNull { it.id }.joinToString()
                StatusSyncJob.startNow(context, mangaIds)
            }
        }
    }

    private fun deleteChapters(manga: Manga, chapters: List<Chapter>) {
        downloadManager.deleteChapters(chapters, manga)
    }

    companion object {
        private var lastLibraryItems: List<LibraryItem>? = null
        private var lastCategories: List<Category>? = null
        private const val dynamicCategorySplitter = "??\t??\t?"

        private val randomTags = arrayOf(0, 1, 2)
        private const val randomSource = 4
        private const val randomTitle = 3
        private const val randomTag = 0
        private val randomGroupOfTags = arrayOf(1, 2)
        private const val randomGroupOfTagsNormal = 1
        private const val randomGroupOfTagsNegate = 2

        fun onLowMemory() {
            lastLibraryItems = null
            lastCategories = null
        }

        suspend fun setSearchSuggestion(
            libraryPreferences: LibraryPreferences,
            db: DatabaseHelper,
            sourceManager: SourceManager,
        ) {
            val random: Random = run {
                val cal = Calendar.getInstance()
                cal.time = Date()
                cal[Calendar.MINUTE] = 0
                cal[Calendar.SECOND] = 0
                cal[Calendar.MILLISECOND] = 0
                Random(cal.time.time)
            }

            val recentManga by lazy { runBlocking { FeedRepository.getRecentlyReadManga() } }
            val libraryManga by lazy { db.getLibraryMangaList().executeAsBlocking() }
            libraryPreferences
                .searchSuggestions()
                .set(
                    when (val value = random.nextInt(0, 5)) {
                        randomSource -> {
                            val distinctSources = libraryManga.distinctBy { it.source }
                            val randomSource =
                                sourceManager
                                    .get(distinctSources.randomOrNull(random)?.source ?: 0L)
                                    ?.name
                            randomSource?.chopByWords(30)
                        }
                        randomTitle -> {
                            libraryManga.randomOrNull(random)?.title?.chopByWords(30)
                        }
                        in randomTags -> {
                            val tags =
                                recentManga
                                    .map { it.genre.orEmpty().split(",").map(String::trim) }
                                    .flatten()
                                    .filter { it.isNotBlank() }
                            val distinctTags = tags.distinct()
                            if (value in randomGroupOfTags && distinctTags.size > 6) {
                                val shortestTagsSort = distinctTags.sortedBy { it.length }
                                val offset = random.nextInt(0, distinctTags.size / 2 - 2)
                                var offset2 = random.nextInt(0, distinctTags.size / 2 - 2)
                                while (offset2 == offset) {
                                    offset2 = random.nextInt(0, distinctTags.size / 2 - 2)
                                }
                                if (value == randomGroupOfTagsNormal) {
                                    "${shortestTagsSort[offset]}, " + shortestTagsSort[offset2]
                                } else {
                                    "${shortestTagsSort[offset]}, -" + shortestTagsSort[offset2]
                                }
                            } else {
                                val group = tags.groupingBy { it }.eachCount()
                                val groupedTags = distinctTags.sortedByDescending { group[it] }
                                groupedTags.take(8).randomOrNull(random)
                            }
                        }
                        else -> ""
                    } ?: ""
                )

            if (!libraryPreferences.showSearchSuggestions().isSet()) {
                libraryPreferences.showSearchSuggestions().set(true)
            }
            libraryPreferences.lastSearchSuggestion().set(Date().time)
        }

        /** Update the library manga with new merge manga info */
        fun updateMergeMangaDBAndFiles() {
            val db: DatabaseHelper = Injekt.get()
            val context by injectLazy<Application>()
            val downloadProvider = DownloadProvider(context)
            val libraryManga = db.getLibraryMangaList().executeAsBlocking()
            val mergeManga = libraryManga.filter { it.merge_manga_url != null }

            db.inTransaction {
                mergeManga.forEach { manga ->
                    GlobalScope.launchIO {
                        downloadProvider.renameChapterFoldersForLegacyMerged(manga)
                    }
                    db.insertMergeManga(
                            MergeMangaImpl(
                                mangaId = manga.id!!,
                                url = manga.merge_manga_url!!,
                                mergeType = MergeType.MangaLife,
                            )
                        )
                        .executeAsBlocking()
                    manga.merge_manga_url = null
                    db.insertManga(manga).executeAsBlocking()
                    db.getChapters(manga)
                        .executeAsBlocking()
                        .filter { it.scanlator?.equals(MangaLife.oldName) == true }
                        .map { chp ->
                            chp.scanlator = MangaLife.name
                            db.insertChapter(chp).executeAsBlocking()
                        }
                }
            }
        }

        /** Remove any saved filters that have invalid languages */
        fun updateSavedFilters() {
            val db: DatabaseHelper = Injekt.get()
            val updatedFilters =
                db.getBrowseFilters().executeAsBlocking().map { filter ->
                    filter.copy(
                        dexFilters =
                            filter.dexFilters
                                .replace(""",{"language":"OTHER","state":true}""", "")
                                .replace(""",{"language":"OTHER","state":false}""", "")
                                .replace(""",{"language":"SERBO_CROATIAN","state":false}""", "")
                                .replace(""",{"language":"SERBO_CROATIAN","state":true}""", "")
                    )
                }
            db.insertBrowseFilters(updatedFilters).executeAsBlocking()
        }

        suspend fun updateRatiosAndColors() {
            val db: DatabaseHelper = Injekt.get()
            val libraryManga = db.getFavoriteMangaList().executeOnIO()
            libraryManga.forEach { manga ->
                try {
                    withUIContext { MangaCoverMetadata.setRatioAndColors(manga) }
                } catch (_: Exception) {}
            }
            MangaCoverMetadata.savePrefs()
        }
    }
}
