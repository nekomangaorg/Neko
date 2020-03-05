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
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.migration.MigrationFlags
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.lang.removeArticles
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.Companion.STATE_EXCLUDE
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.Companion.STATE_IGNORE
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.Companion.STATE_INCLUDE
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.Companion.STATE_REALLY_EXCLUDE
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

/**
 * Class containing library information.
 */
private data class Library(val categories: List<Category>, val mangaMap: LibraryMap)

/**
 * Typealias for the library manga, using the category as keys, and list of manga as values.
 */
private typealias LibraryMap = Map<Int, List<LibraryItem>>

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

    private val context = preferences.context

    private val loggedServices by lazy { Injekt.get<TrackManager>().services.filter { it.isLogged } }
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
    private var rawMangaMap:LibraryMap? = null

    private var currentMangaMap:LibraryMap? = null

    private var totalChapters:Map<Long, Int>? = null

    fun isDownloading() = downloadManager.hasQueue()

    fun onDestroy() {
        if (currentMangaMap != null)
            currentLibrary = Library(categories, currentMangaMap!!)
    }

    fun onRestore() {
        categories = currentLibrary?.categories ?: return
        currentMangaMap = currentLibrary?.mangaMap
        currentLibrary = null
    }

    fun getLibrary() {
        launchUI {
            totalChapters = null
            val freshStart = !preferences.libraryAsSingleList().getOrDefault()
                && (currentMangaMap?.values?.firstOrNull()?.firstOrNull()?.header != null)
            val mangaMap = withContext(Dispatchers.IO) {
                val library = getLibraryFromDB()
                library.apply { setDownloadCount(library.mangaMap) }
                rawMangaMap = library.mangaMap
                var mangaMap = library.mangaMap
                mangaMap = applyFilters(mangaMap)
                mangaMap = applySort(mangaMap)
                mangaMap
            }
            currentMangaMap = mangaMap
            updateView(categories, mangaMap, freshStart)
            withContext(Dispatchers.IO) {
                setTotalChapters()
            }
        }
    }

    fun getLibraryBlocking() {
        val mangaMap = {
            val library = getLibraryFromDB()
            library.apply { setDownloadCount(library.mangaMap) }
            rawMangaMap = library.mangaMap
            var mangaMap = library.mangaMap
            mangaMap = applyFilters(mangaMap)
            mangaMap = applySort(mangaMap)
            mangaMap
        }()
        currentMangaMap = mangaMap
        launchUI {
            updateView(categories, mangaMap, true)
        }
    }

    fun getAllManga(): LibraryMap? {
        return currentMangaMap
    }

    fun getMangaInCategory(catId: Int?): List<LibraryItem>? {
        val categoryId = catId ?: return null
        return currentMangaMap?.get(categoryId)
    }

    /**
     * Applies library filters to the given map of manga.
     *
     * @param map the map to filter.
     */
    private fun applyFilters(map: LibraryMap): LibraryMap {
        val filterDownloaded = preferences.filterDownloaded().getOrDefault()

        val filterUnread = preferences.filterUnread().getOrDefault()

        val filterCompleted = preferences.filterCompleted().getOrDefault()

        val filterTracked = preferences.filterTracked().getOrDefault()

        val filterMangaType by lazy { preferences.filterMangaType().getOrDefault() }

        val filterFn: (LibraryItem) -> Boolean = f@ { item ->
            // Filter when there isn't unread chapters.
            if (filterUnread == STATE_INCLUDE &&
                (item.manga.unread == 0 || db.getChapters(item.manga).executeAsBlocking()
                    .size != item.manga.unread)) return@f false
            if (filterUnread == STATE_EXCLUDE &&
                (item.manga.unread == 0 ||
                    db.getChapters(item.manga).executeAsBlocking().size == item.manga.unread))
                return@f false
            if (filterUnread == STATE_REALLY_EXCLUDE && item.manga.unread > 0) return@f false

            if (filterMangaType > 0) {
                if (filterMangaType != item.manga.mangaType()) return@f false
            }

            if (filterCompleted == STATE_INCLUDE && item.manga.status != SManga.COMPLETED)
                return@f false
            if (filterCompleted == STATE_EXCLUDE && item.manga.status == SManga.COMPLETED)
                return@f false

            if (filterTracked != STATE_IGNORE) {
                val tracks = db.getTracks(item.manga).executeAsBlocking()

                val trackCount = loggedServices.count { service ->
                    tracks.any { it.sync_id == service.id }
                }
                if (filterTracked == STATE_INCLUDE && trackCount == 0) return@f false
                if (filterTracked == STATE_EXCLUDE && trackCount > 0) return@f false
            }
            // Filter when there are no downloads.
            if (filterDownloaded != STATE_IGNORE) {
                val isDownloaded = when {
                    item.manga.source == LocalSource.ID -> true
                    item.downloadCount != -1 -> item.downloadCount > 0
                    else -> downloadManager.getDownloadCount(item.manga) > 0
                }
                return@f if (filterDownloaded == STATE_INCLUDE) isDownloaded else !isDownloaded
            }
            true
        }

        return map.mapValues { entry -> entry.value.filter(filterFn) }
    }

    /**
     * Sets downloaded chapter count to each manga.
     *
     * @param map the map of manga.
     */
    private fun setDownloadCount(map: LibraryMap) {
        if (!preferences.downloadBadge().getOrDefault()) {
            // Unset download count if the preference is not enabled.
            for ((_, itemList) in map) {
                for (item in itemList) {
                    item.downloadCount = -1
                }
            }
            return
        }

        for ((_, itemList) in map) {
            for (item in itemList) {
                item.downloadCount = downloadManager.getDownloadCount(item.manga)
            }
        }
    }

    private fun setUnreadBadge(map: LibraryMap) {
        val unreadType = preferences.unreadBadgeType().getOrDefault()
        for ((_, itemList) in map) {
            for (item in itemList) {
                item.unreadType = unreadType
            }
        }
    }

    private fun applyCatSort(map: LibraryMap, catId: Int?): LibraryMap {
        if (catId == null) return map
        val categoryManga = map[catId] ?: return map
        val catSorted = applySort(mapOf(catId to categoryManga), catId)
        val mutableMap = map.toMutableMap()
        mutableMap[catId] = catSorted.values.first()
        return mutableMap
    }

    private fun applySort(map: LibraryMap, catId: Int?): LibraryMap {
        if (catId == null) return map
        val category = if (catId == 0) createDefaultCategory() else
            db.getCategories().executeAsBlocking().find { it.id == catId } ?: return map
        allCategories.find { it.id == catId }?.apply {
            mangaOrder = category.mangaOrder
            mangaSort = category.mangaSort
        }

        val lastReadManga by lazy {
            var counter = 0
            db.getLastReadManga().executeAsBlocking().associate { it.id!! to counter++ }
        }

        val sortFn: (LibraryItem, LibraryItem) -> Int = { i1, i2 ->
            i1.chapterCount = -1
            i2.chapterCount = -1
            sortCategory(i1, i2, lastReadManga, category)
        }
        val comparator = Comparator(sortFn)

        return  map.mapValues { entry -> entry.value.sortedWith(comparator) }
    }

    /**
     * Applies library sorting to the given map of manga.
     *
     * @param map the map to sort.
     */
    private fun applySort(map: LibraryMap): LibraryMap {
        val sortingMode = preferences.librarySortingMode().getOrDefault()

        val lastReadManga by lazy {
            var counter = 0
            db.getLastReadManga().executeAsBlocking().associate { it.id!! to counter++ }
        }

        val ascending = preferences.librarySortingAscending().getOrDefault()
        val useDnD = preferences.libraryAsSingleList().getOrDefault() && !preferences
            .hideCategories().getOrDefault()

        val sortFn: (LibraryItem, LibraryItem) -> Int = { i1, i2 ->
            i1.chapterCount = -1
            i2.chapterCount = -1
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
                sortingMode == LibrarySort.LAST_UPDATED -> i2.manga.last_update.compareTo(i1
                    .manga.last_update)
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
                else -> 0
            }
            if (compare == 0) {
                if (ascending) sortAlphabetical(i1, i2)
                else sortAlphabetical(i2, i1)
            }
            else compare
        }

        val comparator = if (ascending)
            Comparator(sortFn)
        else
            Collections.reverseOrder(sortFn)

        return map.mapValues { entry -> entry.value.sortedWith(comparator) }
    }

    private fun setTotalChapters() {
        if (totalChapters != null) return
        val mangaMap = rawMangaMap ?: return
        totalChapters = mangaMap.flatMap{
            it.value
        }.associate {
            it.manga.id!! to db.getChapters(it.manga).executeAsBlocking().size
        }
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

    private fun sortCategory(i1: LibraryItem, i2: LibraryItem,
        lastReadManga: Map<Long, Int>, initCat: Category? = null
    ): Int {
        return if (initCat != null || i1.manga.category == i2.manga.category) {
            val category = initCat ?: allCategories.find { it.id == i1.manga.category } ?: return 0
            if (category.mangaOrder.isNullOrEmpty() && category.mangaSort == null) {
                category.changeSortTo(preferences.librarySortingMode().getOrDefault())
                db.insertCategory(category).asRxObservable().subscribe()
            }
            val compare = when {
                category.mangaSort != null -> {
                    var sort = when (category.sortingMode()) {
                        LibrarySort.ALPHA -> sortAlphabetical(i1, i2)
                        LibrarySort.LAST_UPDATED -> i2.manga.last_update.compareTo(i1.manga.last_update)
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
                        else -> sortAlphabetical(i1, i2)
                    }
                    if (!category.isAscending()) sort *= -1
                    sort
                }
                category?.mangaOrder?.isEmpty() == false -> {
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
                if (category?.isAscending() != false) sortAlphabetical(i1, i2)
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
            i1.manga.currentTitle().removeArticles().compareTo(i2.manga.currentTitle().removeArticles(), true)
        else i1.manga.currentTitle().compareTo(i2.manga.currentTitle(), true)
    }

    /**
     * Get the categories and all its manga from the database.
     *
     * @return an observable of the categories and its manga.
     */
    private fun getLibraryFromDB(): Library {
        val categories = db.getCategories().executeAsBlocking().toMutableList()
        val libraryLayout = preferences.libraryLayout()
        val showCategories = !preferences.hideCategories().getOrDefault()
        val unreadBadgeType = preferences.unreadBadgeType().getOrDefault()
        var libraryManga = db.getLibraryMangas().executeAsBlocking()
        if (!showCategories)
            libraryManga = libraryManga.distinctBy { it.id }
        /*val libraryMap = libraryManga.map { manga ->
            LibraryItem(manga, libraryLayout).apply { unreadType = unreadBadgeType }
        }.groupBy {
            if (showCategories) it.manga.category else 0
        }*/
        val categoryAll = Category.createAll(context,
            preferences.librarySortingMode().getOrDefault(),
            preferences.librarySortingAscending().getOrDefault())
        val catItemAll = LibraryHeaderItem({ categoryAll }, -1)
        val libraryMap =
            if (!preferences.libraryAsSingleList().getOrDefault()) {
                libraryManga.map { manga ->
                    LibraryItem(manga, libraryLayout, preferences.uniformGrid(),null).apply { unreadType =
                        unreadBadgeType }
                }.groupBy {
                    if (showCategories) it.manga.category else -1
                }
            }
        else {
                libraryManga.groupBy { manga ->
                    if (showCategories) manga.category else -1
                    //LibraryItem(manga, libraryLayout).apply { unreadType = unreadBadgeType }
                }.map { entry ->
                    val categoryItem =
                        if (!showCategories) catItemAll else
                            (LibraryHeaderItem({ getCategory(it) }, entry.key))
                    entry.value.map {
                        LibraryItem(
                            it, libraryLayout, preferences.uniformGrid(), categoryItem
                        ).apply { unreadType = unreadBadgeType }
                    }
                }.map {
                    val cat = if (showCategories) it.firstOrNull()?.manga?.category ?: 0 else -1
                    cat to it
                    //LibraryItem(manga, libraryLayout).apply { unreadType = unreadBadgeType }
                }.toMap()
            }
        if (libraryMap.containsKey(0))
            categories.add(0, createDefaultCategory())

        if (categories.size == 1 && showCategories)
            categories.first().name = context.getString(R.string.label_library)

        this.allCategories = categories
        this.categories = if (!showCategories) arrayListOf(categoryAll)
        else categories

        return Library(this.categories, libraryMap)
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
            var mangaMap = rawMangaMap ?: return@launchUI
            mangaMap = withContext(Dispatchers.IO) { applyFilters(mangaMap) }
            mangaMap = withContext(Dispatchers.IO) { applySort(mangaMap) }
            currentMangaMap = mangaMap
            updateView(categories, mangaMap)
        }
    }

    suspend fun updateView(categories: List<Category>, mangaMap: LibraryMap, freshStart:Boolean
    = false) {
        if (view !is LibraryListController) {
            view.onNextLibraryUpdate(categories, mangaMap, freshStart)
        }
        else {
            val mangaList = withContext(Dispatchers.IO) {
                val list = mutableListOf<LibraryItem>()
                for (element in mangaMap.toSortedMap(compareBy { entry ->
                    categories.find { it.id == entry }?.order ?: -1
                })) {
                    list.addAll(element.value)
                }
                list
            }
            view.onNextLibraryUpdate(mangaList, freshStart)
        }
    }

    fun getList(): List<LibraryItem> {
        val list = mutableListOf<LibraryItem>()
        for (element in currentMangaMap!!.toSortedMap(compareBy { entry ->
            categories.find { it.id == entry }?.order ?: -1
        })) {
            list.addAll(element.value)
        }
        return list
    }

    fun updateViewBlocking() {
        val mangaMap = currentMangaMap ?: return
        if (view !is LibraryListController) {
            if (mangaMap.values.firstOrNull()?.firstOrNull()?.header != null)
                return
            view.onNextLibraryUpdate(categories, mangaMap, true)
        }
        else {
            val list = mutableListOf<LibraryItem>()
            for (element in mangaMap.toSortedMap(compareBy { entry ->
                categories.find { it.id == entry }?.order ?: -1
            })) {
                list.addAll(element.value)
            }
            view.onNextLibraryUpdate(list, true)
        }

    }

    /**
     * Requests the library to have download badges added/removed.
     */
    fun requestDownloadBadgesUpdate() {
        launchUI {
            val mangaMap = rawMangaMap ?: return@launchUI
            withContext(Dispatchers.IO) { setDownloadCount(mangaMap) }
            rawMangaMap = mangaMap
            val current = currentMangaMap ?: return@launchUI
            withContext(Dispatchers.IO) { setDownloadCount(current) }
            currentMangaMap = current
            updateView(categories, current)
        }
    }

    /**
     * Requests the library to have unread badges changed.
     */
    fun requestUnreadBadgesUpdate() {
        //getLibrary()
        launchUI {
            val mangaMap = rawMangaMap ?: return@launchUI
            withContext(Dispatchers.IO) { setUnreadBadge(mangaMap) }
            rawMangaMap = mangaMap
            val current = currentMangaMap ?: return@launchUI
            withContext(Dispatchers.IO) { setUnreadBadge(current) }
            currentMangaMap = current
            updateView(categories, current)
        }
    }

    /**
     * Requests the library to be sorted.
     */
    fun requestSortUpdate() {
        launchUI {
            var mangaMap = currentMangaMap ?: return@launchUI
            mangaMap = withContext(Dispatchers.IO) { applySort(mangaMap) }
            currentMangaMap = mangaMap
            updateView(categories, mangaMap)
        }
    }

    fun requestCatSortUpdate(catId: Int) {
        launchUI {
            var mangaMap = currentMangaMap ?: return@launchUI
            mangaMap = withContext(Dispatchers.IO) { applyCatSort(mangaMap, catId) }
            currentMangaMap = mangaMap
            updateView(categories, mangaMap)
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
                .reduce { set1: Iterable<Category>, set2 -> set1.intersect(set2) }
    }

    /**
     * Remove the selected manga from the library.
     *
     * @param mangas the list of manga to delete.
     * @param deleteChapters whether to also delete downloaded chapters.
     */
    fun removeMangaFromLibrary(mangas: List<Manga>) {

        GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            // Create a set of the list
            val mangaToDelete = mangas.distinctBy { it.id }
            mangaToDelete.forEach { it.favorite = false }

            db.insertMangas(mangaToDelete).executeAsBlocking()
            getLibrary()
        }
    }

    fun confirmDeletion(mangas: List<Manga>) {
        GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            val mangaToDelete = mangas.distinctBy { it.id }
            mangaToDelete.forEach { manga ->
                db.resetMangaInfo(manga).executeAsBlocking()
                coverCache.deleteFromCache(manga.thumbnail_url)
                val source = sourceManager.get(manga.source) as? HttpSource
                if (source != null)
                    downloadManager.deleteManga(manga, source)
            }
        }
    }

    fun updateManga(manga: LibraryManga) {
        GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            val rawMap = rawMangaMap ?: return@launch
            val currentMap = currentMangaMap ?: return@launch
            val id = manga.id ?: return@launch
            val dbManga = db.getLibraryManga(id).executeAsBlocking() ?: return@launch
            arrayOf(rawMap, currentMap).forEach { map ->
                map.apply {
                    forEach { entry ->
                        entry.value.forEach { item ->
                            if (item.manga.id == dbManga.id) {
                                item.manga.last_update = dbManga.last_update
                                item.manga.unread = dbManga.unread
                            }
                        }
                    }
                }
            }
            getLibrary()
        }
    }

    fun addMangas(mangas: List<Manga>) {

        GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            val mangaToAdd = mangas.distinctBy { it.id }
            mangaToAdd.forEach { it.favorite = true }
            db.insertMangas(mangaToAdd).executeAsBlocking()
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
    }

    fun migrateManga(prevManga: Manga, manga: Manga, replace: Boolean) {
        val source = sourceManager.get(manga.source) ?: return

        //state = state.copy(isReplacingManga = true)

        Observable.defer { source.fetchChapterList(manga) }
            .onErrorReturn { emptyList() }
            .doOnNext { migrateMangaInternal(source, it, prevManga, manga, replace) }
            .onErrorReturn { emptyList() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            //.doOnUnsubscribe { state = state.copy(isReplacingManga = false) }
            .subscribe()
    }

    fun hideShowTitle(mangas: List<Manga>, hide: Boolean) {
        mangas.forEach { it.hide_title = hide }
        db.inTransaction {
            mangas.forEach {
                db.updateMangaHideTitle(it).executeAsBlocking()
            }
        }
    }

    private fun migrateMangaInternal(source: Source, sourceChapters: List<SChapter>,
        prevManga: Manga, manga: Manga, replace: Boolean) {

        val flags = preferences.migrateFlags().getOrDefault()
        val migrateChapters = MigrationFlags.hasChapters(flags)
        val migrateCategories = MigrationFlags.hasCategories(flags)
        val migrateTracks = MigrationFlags.hasTracks(flags)

        db.inTransaction {
            // Update chapters read
            if (migrateChapters) {
                try {
                    syncChaptersWithSource(db, sourceChapters, manga, source)
                } catch (e: Exception) {
                    // Worst case, chapters won't be synced
                }

                val prevMangaChapters = db.getChapters(prevManga).executeAsBlocking()
                val maxChapterRead =
                    prevMangaChapters.filter { it.read }.maxBy { it.chapter_number }?.chapter_number
                if (maxChapterRead != null) {
                    val dbChapters = db.getChapters(manga).executeAsBlocking()
                    for (chapter in dbChapters) {
                        if (chapter.isRecognizedNumber && chapter.chapter_number <= maxChapterRead) {
                            chapter.read = true
                        }
                    }
                    db.insertChapters(dbChapters).executeAsBlocking()
                }
            }
            // Update categories
            if (migrateCategories) {
                val categories = db.getCategoriesForManga(prevManga).executeAsBlocking()
                val mangaCategories = categories.map { MangaCategory.create(manga, it) }
                db.setMangaCategories(mangaCategories, listOf(manga))
            }
            // Update track
            if (migrateTracks) {
                val tracks = db.getTracks(prevManga).executeAsBlocking()
                for (track in tracks) {
                    track.id = null
                    track.manga_id = manga.id!!
                }
                db.insertTracks(tracks).executeAsBlocking()
            }
            // Update favorite status
            if (replace) {
                prevManga.favorite = false
                db.updateMangaFavorite(prevManga).executeAsBlocking()
            }
            manga.favorite = true
            db.updateMangaFavorite(manga).executeAsBlocking()

            // SearchPresenter#networkToLocalManga may have updated the manga title, so ensure db gets updated title
            db.updateMangaTitle(manga).executeAsBlocking()
        }
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
        }
        else {
            if (category.id == 0) preferences.defaultMangaOrder().set(category.mangaSort.toString())
            else Injekt.get<DatabaseHelper>().insertCategory(category).executeAsBlocking()
            requestCatSortUpdate(category.id!!)
        }
    }

    fun rearrangeCategory(catId: Int?, mangaIds: List<Long>) {
        GlobalScope.launch(Dispatchers.IO) {
            val category = categories.find { catId == it.id } ?: return@launch
            category.mangaSort = null
            category.mangaOrder = mangaIds
            if (category.id == 0) preferences.defaultMangaOrder().set(mangaIds.joinToString("/"))
            else db.insertCategory(category).executeAsBlocking()
            requestCatSortUpdate(category.id!!)
        }
    }

    fun moveMangaToCategory(item: LibraryItem, catId: Int?, mangaIds: List<Long>, useDND: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            val categoryId = catId ?: return@launch
            val category = categories.find { catId == it.id } ?: return@launch
            val manga = item.manga

            val mangaMap = currentMangaMap?.toMutableMap() ?: return@launch
            val oldCatId = item.manga.category
            val oldCatMap = mangaMap[manga.category]?.toMutableList() ?: return@launch
            val newCatMap = mangaMap[catId]?.toMutableList() ?: return@launch
            oldCatMap.remove(item)
            newCatMap.add(item)
            mangaMap[oldCatId] = oldCatMap
            mangaMap[catId] = newCatMap
            currentMangaMap = mangaMap

            item.manga.category = categoryId


            val mc = ArrayList<MangaCategory>()
            val categories =
                db.getCategoriesForManga(manga).executeAsBlocking().filter { it.id  != oldCatId } + listOf(category)

            for (cat in categories) {
                mc.add(MangaCategory.create(manga, cat))
            }

            db.setMangaCategories(mc, listOf(manga))

            if (useDND) {
                category.mangaSort = null
                val ids = mangaIds.toMutableList()
                if (!ids.contains(manga.id!!)) ids.add(manga.id!!)
                category.mangaOrder = ids
                if (category.id == 0) preferences.defaultMangaOrder().set(mangaIds.joinToString("/"))
                else db.insertCategory(category).executeAsBlocking()
            }
            getLibrary()
        }
    }

    fun mangaIsInCategory(manga: LibraryManga, catId: Int?): Boolean {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking().map { it.id }
        return catId in categories
    }

    private companion object {
        var currentLibrary:Library? = null
    }
}
