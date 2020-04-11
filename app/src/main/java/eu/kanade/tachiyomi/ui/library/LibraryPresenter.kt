package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.util.lang.removeArticles
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.Companion.STATE_EXCLUDE
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.Companion.STATE_IGNORE
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.Companion.STATE_INCLUDE
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.Companion.STATE_REALLY_EXCLUDE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

/**
 * Presenter of [LibraryController].
 */
class LibraryPresenter(
    private val view: LibraryController,
    private val db: DatabaseHelper = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get()
) {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    private val context = preferences.context

    private val loggedServices by lazy { Injekt.get<TrackManager>().services.filter { it.isLogged } }

    private val source by lazy { Injekt.get<SourceManager>().getMangadex() }

    /**
     * Categories of the library.
     */
    var categories: List<Category> = emptyList()
        private set

    var allCategories: List<Category> = emptyList()
        private set

    /**
     * List of all manga to update the
     */
    var libraryItems: List<LibraryItem> = emptyList()
    private var allLibraryItems: List<LibraryItem> = emptyList()

    private var totalChapters: Map<Long, Int>? = null

    fun onDestroy() {
        lastLibraryItems = libraryItems
        lastCategories = categories
    }

    fun onRestore() {
        libraryItems = lastLibraryItems ?: return
        categories = lastCategories ?: return
        lastCategories = null
        lastLibraryItems = null
    }

    fun getLibrary() {
        launchUI {
            totalChapters = null
            val mangaMap = withContext(Dispatchers.IO) {
                val library = getLibraryFromDB()
                library.apply { setDownloadCount(library) }
                allLibraryItems = library
                var mangaMap = library
                mangaMap = applyFilters(mangaMap)
                mangaMap = applySort(mangaMap)
                mangaMap
            }
            val freshStart = libraryItems.isEmpty()
            libraryItems = mangaMap
            view.onNextLibraryUpdate(libraryItems, freshStart)
            withContext(Dispatchers.IO) {
                setTotalChapters()
            }
        }
    }

    /**
     * Applies library filters to the given map of manga.
     *
     * @param map the map to filter.
     */
    private fun applyFilters(map: List<LibraryItem>): List<LibraryItem> {
        val filterDownloaded = preferences.filterDownloaded().getOrDefault()

        val filterUnread = preferences.filterUnread().getOrDefault()

        val filterCompleted = preferences.filterCompleted().getOrDefault()

        val filterTracked = preferences.filterTracked().getOrDefault()

        val filterMangaType = preferences.filterMangaType().getOrDefault()

        val filterTrackers = FilterBottomSheet.FILTER_TRACKER

        return map.filter f@{ item ->
            if (item.manga.isBlank()) {
                return@f filterDownloaded == 0 && filterUnread == 0 && filterCompleted == 0 &&
                    filterTracked == 0 && filterMangaType == 0
            }
            // Filter for unread chapters
            if (filterUnread == STATE_INCLUDE && (item.manga.unread == 0 || db.getChapters(item.manga)
                    .executeAsBlocking().size != item.manga.unread)
            ) return@f false
            if (filterUnread == STATE_EXCLUDE && (item.manga.unread == 0 || db.getChapters(item.manga)
                    .executeAsBlocking().size == item.manga.unread)
            ) return@f false
            if (filterUnread == STATE_REALLY_EXCLUDE && item.manga.unread > 0) return@f false

            if (filterMangaType > 0) {
                if (if (filterMangaType == Manga.TYPE_MANHWA) (filterMangaType != item.manga.mangaType() && filterMangaType != Manga.TYPE_WEBTOON)
                    else filterMangaType != item.manga.mangaType()
                ) return@f false
            }

            // Filter for completed status of manga
            if (filterCompleted == STATE_INCLUDE && item.manga.status != SManga.COMPLETED) return@f false
            if (filterCompleted == STATE_EXCLUDE && item.manga.status == SManga.COMPLETED) return@f false

            // Filter for tracked (or per tracked service)
            if (filterTracked != STATE_IGNORE) {
                val tracks = db.getTracks(item.manga).executeAsBlocking()

                val hasTrack = loggedServices.any { service ->
                    tracks.any { it.sync_id == service.id }
                }
                val service = if (filterTrackers.isNotEmpty()) loggedServices.find {
                    it.name == filterTrackers
                } else null
                if (filterTracked == STATE_INCLUDE) {
                    if (!hasTrack) return@f false
                    if (filterTrackers.isNotEmpty()) {
                        if (service != null) {
                            val hasServiceTrack = tracks.any { it.sync_id == service.id }
                            if (!hasServiceTrack) return@f false
                            if (filterTracked == STATE_EXCLUDE && hasServiceTrack) return@f false
                        }
                    }
                } else if (filterTracked == STATE_EXCLUDE) {
                    if (!hasTrack && filterTrackers.isEmpty()) return@f false
                    if (filterTrackers.isNotEmpty()) {
                        if (service != null) {
                            val hasServiceTrack = tracks.any { it.sync_id == service.id }
                            if (hasServiceTrack) return@f false
                        }
                    }
                }
            }
            // Filter for downloaded manga
            if (filterDownloaded != STATE_IGNORE) {
                val isDownloaded = when {
                    item.downloadCount != -1 -> item.downloadCount > 0
                    else -> downloadManager.getDownloadCount(item.manga) > 0
                }
                return@f if (filterDownloaded == STATE_INCLUDE) isDownloaded else !isDownloaded
            }
            true
        }
    }

    /**
     * Sets downloaded chapter count to each manga.
     *
     * @param map the map of manga.
     */
    private fun setDownloadCount(itemList: List<LibraryItem>) {
        if (!preferences.downloadBadge().getOrDefault()) {
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
        val unreadType = preferences.unreadBadgeType().getOrDefault()
        for (item in itemList) {
            item.unreadType = unreadType
        }
    }

    /**
     * Applies library sorting to the given map of manga.
     *
     * @param map the map to sort.
     */
    private fun applySort(map: List<LibraryItem>): List<LibraryItem> {
        val sortingMode = preferences.librarySortingMode().getOrDefault()

        val lastReadManga by lazy {
            var counter = 0
            db.getLastReadManga().executeAsBlocking().associate { it.id!! to counter++ }
        }

        val ascending = preferences.librarySortingAscending().getOrDefault()
        val useDnD = !preferences.hideCategories().getOrDefault()

        val sortFn: (LibraryItem, LibraryItem) -> Int = { i1, i2 ->
            if (!(sortingMode == LibrarySort.DRAG_AND_DROP || useDnD)) {
                i1.chapterCount = -1
                i2.chapterCount = -1
            }
            val compare = when {
                sortingMode == LibrarySort.DRAG_AND_DROP || useDnD ->
                    sortCategory(i1, i2, lastReadManga)
                sortingMode == LibrarySort.ALPHA -> sortAlphabetical(i1, i2)
                sortingMode == LibrarySort.LAST_READ -> {
                    // Get index of manga, set equal to list if size unknown.
                    val manga1LastRead = lastReadManga[i1.manga.id!!] ?: lastReadManga.size
                    val manga2LastRead = lastReadManga[i2.manga.id!!] ?: lastReadManga.size
                    manga1LastRead.compareTo(manga2LastRead)
                }
                sortingMode == LibrarySort.LATEST_CHAPTER -> i2.manga.last_update.compareTo(
                    i1
                        .manga.last_update
                )
                sortingMode == LibrarySort.UNREAD ->
                    when {
                        i1.manga.unread == i2.manga.unread -> 0
                        i1.manga.unread == 0 -> if (ascending) 1 else -1
                        i2.manga.unread == 0 -> if (ascending) -1 else 1
                        else -> i1.manga.unread.compareTo(i2.manga.unread)
                    }
                sortingMode == LibrarySort.TOTAL -> {
                    setTotalChapters()
                    val manga1TotalChapter = totalChapters!![i1.manga.id!!] ?: 0
                    val mange2TotalChapter = totalChapters!![i2.manga.id!!] ?: 0
                    i1.chapterCount = totalChapters!![i1.manga.id!!] ?: 0
                    i2.chapterCount = totalChapters!![i2.manga.id!!] ?: 0
                    manga1TotalChapter.compareTo(mange2TotalChapter)
                }
                sortingMode == LibrarySort.DATE_ADDED -> {
                    i2.manga.date_added.compareTo(i1.manga.date_added)
                }
                else -> 0
            }
            if (compare == 0) {
                if (ascending) sortAlphabetical(i1, i2)
                else sortAlphabetical(i2, i1)
            } else compare
        }

        val comparator = if (ascending || useDnD)
            Comparator(sortFn)
        else
            Collections.reverseOrder(sortFn)

        return map.sortedWith(comparator)
    }

    private fun setTotalChapters() {
        if (totalChapters != null) return
        val mangaMap = allLibraryItems
        totalChapters = mangaMap.map {
            it.manga.id!! to db.getChapters(it.manga).executeAsBlocking().size
        }.toMap()
    }

    private fun getCategory(categoryId: Int): Category {
        val category = categories.find { it.id == categoryId } ?: createDefaultCategory()
        if (category.isFirst == null) {
            category.isFirst = (category.id ?: 0 <= 0 ||
                (category.order == 0 && categories.none { it.id == 0 }))
        }
        if (category.isLast == null) category.isLast = categories.lastOrNull()?.id == category.id
        return category
    }

    private fun sortCategory(
        i1: LibraryItem,
        i2: LibraryItem,
        lastReadManga: Map<Long, Int>,
        initCat: Category? = null
    ): Int {
        return if (initCat != null || i1.manga.category == i2.manga.category) {
            val category = initCat ?: allCategories.find { it.id == i1.manga.category } ?: return 0
            if (category.mangaOrder.isNullOrEmpty() && category.mangaSort == null) {
                category.changeSortTo(preferences.librarySortingMode().getOrDefault())
                if (category.id == 0) preferences.defaultMangaOrder()
                    .set(category.mangaSort.toString())
                else db.insertCategory(category).asRxObservable().subscribe()
            }
            i1.chapterCount = -1
            i2.chapterCount = -1
            val compare = when {
                category.mangaSort != null -> {
                    var sort = when (category.sortingMode()) {
                        LibrarySort.ALPHA -> sortAlphabetical(i1, i2)
                        LibrarySort.LATEST_CHAPTER -> i2.manga.last_update.compareTo(i1.manga.last_update)
                        LibrarySort.UNREAD -> when {
                            i1.manga.unread == i2.manga.unread -> 0
                            i1.manga.unread == 0 -> if (category.isAscending()) 1 else -1
                            i2.manga.unread == 0 -> if (category.isAscending()) -1 else 1
                            else -> i1.manga.unread.compareTo(i2.manga.unread)
                        }
                        LibrarySort.LAST_READ -> {
                            val manga1LastRead = lastReadManga[i1.manga.id!!] ?: lastReadManga.size
                            val manga2LastRead = lastReadManga[i2.manga.id!!] ?: lastReadManga.size
                            manga1LastRead.compareTo(manga2LastRead)
                        }
                        LibrarySort.TOTAL -> {
                            setTotalChapters()
                            val manga1TotalChapter = totalChapters!![i1.manga.id!!] ?: 0
                            val mange2TotalChapter = totalChapters!![i2.manga.id!!] ?: 0
                            i1.chapterCount = totalChapters!![i1.manga.id!!] ?: 0
                            i2.chapterCount = totalChapters!![i2.manga.id!!] ?: 0
                            manga1TotalChapter.compareTo(mange2TotalChapter)
                        }
                        LibrarySort.DATE_ADDED -> i2.manga.date_added.compareTo(i1.manga.date_added)
                        else -> sortAlphabetical(i1, i2)
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
                if (category.isAscending()) sortAlphabetical(i1, i2)
                else sortAlphabetical(i2, i1)
            } else compare
        } else {
            val category = allCategories.find { it.id == i1.manga.category }?.order ?: -1
            val category2 = allCategories.find { it.id == i2.manga.category }?.order ?: -1
            category.compareTo(category2)
        }
    }

    private fun sortAlphabetical(i1: LibraryItem, i2: LibraryItem): Int {
        return if (preferences.removeArticles().getOrDefault())
            i1.manga.title.removeArticles().compareTo(i2.manga.title.removeArticles(), true)
        else i1.manga.title.compareTo(i2.manga.title, true)
    }

    /**
     * Get the categories and all its manga from the database.
     *
     * @return an observable of the categories and its manga.
     */
    private fun getLibraryFromDB(): List<LibraryItem> {
        val categories = db.getCategories().executeAsBlocking().toMutableList()
        val libraryLayout = preferences.libraryLayout()
        val showCategories = !preferences.hideCategories().getOrDefault()
        var libraryManga = db.getLibraryMangas().executeAsBlocking()
        val seekPref = preferences.alwaysShowSeeker()
        if (!showCategories)
            libraryManga = libraryManga.distinctBy { it.id }
        val categoryAll = Category.createAll(
            context,
            preferences.librarySortingMode().getOrDefault(),
            preferences.librarySortingAscending().getOrDefault()
        )
        val catItemAll = LibraryHeaderItem({ categoryAll }, -1, seekPref)
        val categorySet = mutableSetOf<Int>()
        val headerItems = (categories.mapNotNull { category ->
            val id = category.id
            if (id == null) null
            else id to LibraryHeaderItem({ getCategory(id) }, id, seekPref)
        } + (-1 to catItemAll) +
            (0 to LibraryHeaderItem({ getCategory(0) }, 0, seekPref))).toMap()
        val items = libraryManga.map {
            val headerItem = if (!showCategories) catItemAll else
                headerItems[it.category]
            categorySet.add(it.category)
            LibraryItem(it, libraryLayout, preferences.uniformGrid(), seekPref, headerItem)
        }.toMutableList()

        if (showCategories) {
            categories.forEach { category ->
                if (category.id ?: 0 <= 0 && !categorySet.contains(category.id)) {
                    val headerItem = headerItems[category.id ?: 0]
                    items.add(
                        LibraryItem(
                            LibraryManga.createBlank(category.id!!),
                            libraryLayout,
                            preferences.uniformGrid(),
                            preferences.alwaysShowSeeker(),
                            headerItem
                        )
                    )
                }
            }
        }

        if (categories.size == 1 && showCategories) categories.first().name =
            context.getString(R.string.library)

        if (categorySet.contains(0))
            categories.add(0, createDefaultCategory())

        this.allCategories = categories
        this.categories = if (!showCategories) arrayListOf(categoryAll)
        else categories

        return items
    }

    private fun createDefaultCategory(): Category {
        val default = Category.createDefault(context)
        default.order = -1
        val defOrder = preferences.defaultMangaOrder().getOrDefault()
        if (defOrder.firstOrNull()?.isLetter() == true) default.mangaSort = defOrder.first()
        else default.mangaOrder = defOrder.split("/").mapNotNull { it.toLongOrNull() }
        return default
    }

    /**
     * Requests the library to be filtered.
     */
    fun requestFilterUpdate() {
        launchUI {
            var mangaMap = allLibraryItems
            mangaMap = withContext(Dispatchers.IO) { applyFilters(mangaMap) }
            mangaMap = withContext(Dispatchers.IO) { applySort(mangaMap) }
            libraryItems = mangaMap
            view.onNextLibraryUpdate(libraryItems)
        }
    }

    /**
     * Requests the library to have download badges added/removed.
     */
    fun requestDownloadBadgesUpdate() {
        launchUI {
            val mangaMap = allLibraryItems
            withContext(Dispatchers.IO) { setDownloadCount(mangaMap) }
            allLibraryItems = mangaMap
            val current = libraryItems
            withContext(Dispatchers.IO) { setDownloadCount(current) }
            libraryItems = current
            view.onNextLibraryUpdate(libraryItems)
        }
    }

    /**
     * Requests the library to have unread badges changed.
     */
    fun requestUnreadBadgesUpdate() {
        launchUI {
            val mangaMap = allLibraryItems
            withContext(Dispatchers.IO) { setUnreadBadge(mangaMap) }
            libraryItems = mangaMap
            val current = libraryItems
            withContext(Dispatchers.IO) { setUnreadBadge(current) }
            libraryItems = current
            view.onNextLibraryUpdate(libraryItems)
        }
    }

    /**
     * Requests the library to be sorted.
     */
    private fun requestSortUpdate() {
        launchUI {
            var mangaMap = libraryItems
            mangaMap = withContext(Dispatchers.IO) { applySort(mangaMap) }
            libraryItems = mangaMap
            view.onNextLibraryUpdate(libraryItems)
        }
    }

    /**
     * Returns the common categories for the given list of manga.
     *
     * @param mangas the list of manga.
     */
    fun getCommonCategories(mangas: List<Manga>): Collection<Category> {
        if (mangas.isEmpty()) return emptyList()
        return mangas.toSet()
            .map { db.getCategoriesForManga(it).executeAsBlocking() }
            .reduce { set1: Iterable<Category>, set2 -> set1.intersect(set2).toMutableList() }
    }

    /**
     * Remove the selected manga from the library.
     *
     * @param mangas the list of manga to delete.
     */
    fun removeMangaFromLibrary(mangas: List<Manga>) {
        scope.launch {
            // Create a set of the list
            val mangaToDelete = mangas.distinctBy { it.id }
            mangaToDelete.forEach { it.favorite = false }

            db.insertMangas(mangaToDelete).executeOnIO()
            getLibrary()
        }
    }

    fun confirmDeletion(mangas: List<Manga>) {
        scope.launch {
            val mangaToDelete = mangas.distinctBy { it.id }
            mangaToDelete.forEach { manga ->
                db.resetMangaInfo(manga).executeOnIO()
                coverCache.deleteFromCache(manga.thumbnail_url)
                val source = sourceManager.get(manga.source) as? HttpSource
                if (source != null)
                    downloadManager.deleteManga(manga, source)
            }
        }
    }

    fun updateManga(manga: LibraryManga) {
        scope.launch {
            val rawMap = allLibraryItems
            val currentMap = libraryItems
            val id = manga.id ?: return@launch
            val dbManga = db.getLibraryManga(id).executeOnIO() ?: return@launch
            arrayOf(rawMap, currentMap).forEach { map ->
                map.forEach { item ->
                    if (item.manga.id == dbManga.id) {
                        item.manga.last_update = dbManga.last_update
                        item.manga.unread = dbManga.unread
                    }
                }
            }
            getLibrary()
        }
    }

    fun reAddMangas(mangas: List<Manga>) {
        scope.launch {
            val mangaToAdd = mangas.distinctBy { it.id }
            mangaToAdd.forEach { it.favorite = true }
            db.insertMangas(mangaToAdd).executeOnIO()
            getLibrary()
            mangaToAdd.forEach { db.insertManga(it).executeAsBlocking() }
        }
    }

    /**
     * Move the given list of manga to categories.
     *
     * @param categories the selected categories.
     * @param mangas the list of manga to move.
     */
    fun moveMangasToCategories(categories: List<Category>, mangas: List<Manga>) {
        val mc = ArrayList<MangaCategory>()

        for (manga in mangas) {
            for (cat in categories) {
                mc.add(MangaCategory.create(manga, cat))
            }
        }
        db.setMangaCategories(mc, mangas)
        getLibrary()
    }

    fun getFirstUnread(manga: Manga): Chapter? {
        val chapters = db.getChapters(manga).executeAsBlocking()
        return chapters.sortedByDescending { it.source_order }.find { !it.read }
    }

    fun sortCategory(catId: Int, order: Int) {
        val category = categories.find { catId == it.id } ?: return
        category.mangaSort = ('a' + (order - 1))
        if (catId == -1) {
            val sort = category.sortingMode() ?: LibrarySort.ALPHA
            preferences.librarySortingMode().set(sort)
            preferences.librarySortingAscending().set(category.isAscending())
            requestSortUpdate()
        } else {
            if (category.id == 0) preferences.defaultMangaOrder().set(category.mangaSort.toString())
            else Injekt.get<DatabaseHelper>().insertCategory(category).executeAsBlocking()
            requestSortUpdate()
        }
    }

    fun rearrangeCategory(catId: Int?, mangaIds: List<Long>) {
        scope.launch {
            val category = categories.find { catId == it.id } ?: return@launch
            category.mangaSort = null
            category.mangaOrder = mangaIds
            if (category.id == 0) preferences.defaultMangaOrder().set(mangaIds.joinToString("/"))
            else db.insertCategory(category).executeOnIO()
            requestSortUpdate()
        }
    }

    fun moveMangaToCategory(
        manga: LibraryManga,
        catId: Int?,
        mangaIds: List<Long>
    ) {
        scope.launch {
            val categoryId = catId ?: return@launch
            val category = categories.find { catId == it.id } ?: return@launch

            val oldCatId = manga.category
            manga.category = categoryId

            val mc = ArrayList<MangaCategory>()
            val categories =
                if (catId == 0) emptyList()
                else
                    db.getCategoriesForManga(manga).executeOnIO()
                        .filter { it.id != oldCatId } + listOf(category)

            for (cat in categories) {
                mc.add(MangaCategory.create(manga, cat))
            }

            db.setMangaCategories(mc, listOf(manga))

            if (category.mangaSort == null) {
                val ids = mangaIds.toMutableList()
                if (!ids.contains(manga.id!!)) ids.add(manga.id!!)
                category.mangaOrder = ids
                if (category.id == 0) preferences.defaultMangaOrder()
                    .set(mangaIds.joinToString("/"))
                else db.insertCategory(category).executeAsBlocking()
            }
            getLibrary()
        }
    }

    fun mangaIsInCategory(manga: LibraryManga, catId: Int?): Boolean {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking().map { it.id }
        return catId in categories
    }

    fun syncMangaToDex(mangaList: List<Manga>) {
        scope.launch {
            withContext(Dispatchers.IO) {
                mangaList.forEach {
                    source.updateFollowStatus(MdUtil.getMangaId(it.url), FollowStatus.READING)
                }
            }
        }
    }

    companion object {
        private var lastLibraryItems: List<LibraryItem>? = null
        private var lastCategories: List<Category>? = null
    }
}
