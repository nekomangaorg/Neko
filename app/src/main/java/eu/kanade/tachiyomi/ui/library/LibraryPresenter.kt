package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
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
            view.onNextLibraryUpdate(categories, mangaMap)
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
        view.onNextLibraryUpdate(categories, mangaMap)
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

            if (filterMangaType == LibraryManga.MANGA &&
                item.manga.mangaType() == LibraryManga.MANWHA)
                return@f false
            if ((filterMangaType == LibraryManga.MANWHA) &&
                item.manga.mangaType() == LibraryManga.MANGA) return@f false


            if (filterCompleted == STATE_INCLUDE && item.manga.status != SManga.COMPLETED)
                return@f false
            if (filterCompleted == STATE_EXCLUDE && item.manga.status == SManga.COMPLETED)
                return@f false

            if (filterTracked != STATE_IGNORE) {
                val db = Injekt.get<DatabaseHelper>()
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
        val category = db.getCategories().executeAsBlocking().find { it.id == catId } ?: return map
        allCategories.find { it.id == catId }?.apply {
            mangaOrder = category.mangaOrder
            mangaSort = category.mangaSort
        }

        val lastReadManga by lazy {
            var counter = 0
            db.getLastReadManga().executeAsBlocking().associate { it.id!! to counter++ }
        }

        val sortFn: (LibraryItem, LibraryItem) -> Int = { i1, i2 ->
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
            compare
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
        val totalChapterManga by lazy {
            var counter = 0
            db.getTotalChapterManga().executeAsBlocking().associate { it.id!! to counter++ }
        }
        val catListing by lazy {
            val default = createDefaultCategory()
            default.order = -1
            listOf(default) + db.getCategories().executeAsBlocking()
        }

        val ascending = preferences.librarySortingAscending().getOrDefault()

        val sortFn: (LibraryItem, LibraryItem) -> Int = { i1, i2 ->
            val compare = when (sortingMode) {
                LibrarySort.ALPHA -> sortAlphabetical(i1, i2)
                LibrarySort.LAST_READ -> {
                    // Get index of manga, set equal to list if size unknown.
                    val manga1LastRead = lastReadManga[i1.manga.id!!] ?: lastReadManga.size
                    val manga2LastRead = lastReadManga[i2.manga.id!!] ?: lastReadManga.size
                    manga1LastRead.compareTo(manga2LastRead)
                }
                LibrarySort.LAST_UPDATED -> i2.manga.last_update.compareTo(i1.manga.last_update)
                LibrarySort.UNREAD ->
                    when {
                        i1.manga.unread == i2.manga.unread -> 0
                        i1.manga.unread == 0 -> if (ascending) 1 else -1
                        i2.manga.unread == 0 -> if (ascending) -1 else 1
                        else -> i1.manga.unread.compareTo(i2.manga.unread)
                    }
                LibrarySort.TOTAL -> {
                    val manga1TotalChapter = totalChapterManga[i1.manga.id!!] ?: 0
                    val mange2TotalChapter = totalChapterManga[i2.manga.id!!] ?: 0
                    manga1TotalChapter.compareTo(mange2TotalChapter)
                }
                LibrarySort.DRAG_AND_DROP -> {
                    if (i1.manga.category == i2.manga.category) {
                        val category = catListing.find { it.id == i1.manga.category }
                        when {
                            category?.mangaSort != null -> {
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
                                    else -> sortAlphabetical(i1, i2)
                                }
                                if (!category.isAscending())
                                    sort *= -1
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
                    }
                    else {
                        val category = catListing.find { it.id == i1.manga.category }?.order ?: -1
                        val category2 = catListing.find { it.id == i2.manga.category }?.order ?: -1
                        category.compareTo(category2)
                    }
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
        val libraryAsList = preferences.libraryAsList()
        val showCategories = !preferences.hideCategories().getOrDefault()
        val unreadBadgeType = preferences.unreadBadgeType().getOrDefault()
        var libraryManga = db.getLibraryMangas().executeAsBlocking()
        if (!showCategories)
            libraryManga = libraryManga.distinctBy { it.id }
        val libraryMap = libraryManga.map { manga ->
            LibraryItem(manga, libraryAsList).apply { unreadType = unreadBadgeType }
        }.groupBy {
            if (showCategories) it.manga.category else 0
        }
        if (libraryMap.containsKey(0))
            categories.add(0, createDefaultCategory())

        this.allCategories = categories
        this.categories = if (preferences.hideCategories().getOrDefault())
            arrayListOf(createDefaultCategory())
        else categories

        return Library(this.categories, libraryMap)
    }

    private fun createDefaultCategory(): Category {
        val default = Category.createDefault(context)
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
            view.onNextLibraryUpdate(categories, mangaMap)
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
            view.onNextLibraryUpdate(categories, current)
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
            view.onNextLibraryUpdate(categories, current)
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
            view.onNextLibraryUpdate(categories, mangaMap)
        }
    }

    fun requestCatSortUpdate(catId: Int) {
        launchUI {
            var mangaMap = currentMangaMap ?: return@launchUI
            mangaMap = withContext(Dispatchers.IO) { applyCatSort(mangaMap, catId) }
            currentMangaMap = mangaMap
            view.onNextLibraryUpdate(categories, mangaMap)
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
            rawMangaMap = rawMap
            currentMangaMap = currentMap
            requestSortUpdate()
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

    private companion object {
        var currentLibrary:Library? = null
    }
}
