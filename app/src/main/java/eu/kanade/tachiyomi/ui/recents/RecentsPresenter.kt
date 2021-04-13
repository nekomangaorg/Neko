package eu.kanade.tachiyomi.ui.recents

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.HistoryImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import java.util.Date
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class RecentsPresenter(
    val controller: RecentsController?,
    val preferences: PreferencesHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get()
) : DownloadQueue.DownloadListener, LibraryServiceListener {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    private var recentsJob: Job? = null
    var recentItems = listOf<RecentMangaItem>()
        private set
    var query = ""
        set(value) {
            field = value
            resetOffsets()
        }
    private val newAdditionsHeader = RecentMangaHeaderItem(RecentMangaHeaderItem.NEWLY_ADDED)
    private val newChaptersHeader = RecentMangaHeaderItem(RecentMangaHeaderItem.NEW_CHAPTERS)
    private val continueReadingHeader = RecentMangaHeaderItem(
        RecentMangaHeaderItem
            .CONTINUE_READING
    )
    var finished = false
    var heldItems: HashMap<Int, List<RecentMangaItem>> = hashMapOf()
    private var shouldMoveToTop = false
    var viewType: Int = preferences.recentsViewType().get()
        set(value) {
            field = value
            ENDLESS_LIMIT = if (value == VIEW_TYPE_UNGROUP_ALL) 25 else 50
        }

    private fun resetOffsets() {
        finished = false
        shouldMoveToTop = true
        updatesOffset = 0
        historyOffset = 0
        additionsOffset = 0
    }

    private var updatesOffset = 0
    private var historyOffset = 0
    private var additionsOffset = 0
    var isLoading = false
        private set

    private val isOnFirstPage: Boolean
        get() = additionsOffset + historyOffset + updatesOffset == 0

    init {
        preferences.showReadInAllRecents()
            .asFlow()
            .drop(1)
            .onEach {
                resetOffsets()
                getRecents()
            }
            .launchIn(scope)
    }

    fun onCreate() {
        downloadManager.addListener(this)
        LibraryUpdateService.setListener(this)
        if (lastRecents != null) {
            if (recentItems.isEmpty()) {
                recentItems = lastRecents ?: emptyList()
            }
            lastRecents = null
        }
        getRecents()
    }

    fun getRecents(updatePageCount: Boolean = false) {
        val oldQuery = query
        recentsJob?.cancel()
        recentsJob = scope.launch {
            runRecents(oldQuery, updatePageCount)
        }
    }

    private suspend fun runRecents(
        oldQuery: String = "",
        updatePageCount: Boolean = false,
        retryCount: Int = 0,
        itemCount: Int = 0,
        limit: Boolean = false,
        customViewType: Int? = null
    ) {
        if (retryCount > 15) {
            finished = true
            setDownloadedChapters(recentItems)
            if (customViewType == null) {
                withContext(Dispatchers.Main) {
                    controller?.showLists(recentItems, false)
                    isLoading = false
                }
            }
            return
        }
        val viewType = customViewType ?: viewType

//        Timber.d("starting up items new page: $updatePageCount")
        val showRead = preferences.showReadInAllRecents().get() && !limit
        val isUngrouped = viewType > VIEW_TYPE_GROUP_ALL && query.isEmpty()

        val isCustom = customViewType != null
        val isEndless = isUngrouped && !limit
//        Timber.d("set up cal items")
        val (cReading, rUpdates, nAdditions) = db.inTransactionReturn {
            val cReading = if (viewType != VIEW_TYPE_ONLY_UPDATES) {
                if (query.isEmpty() && viewType != VIEW_TYPE_ONLY_HISTORY) {
                    db.getAllRecents(
                        query,
                        showRead,
                        isEndless,
                        if (isCustom) ENDLESS_LIMIT else historyOffset,
                        !updatePageCount && !isOnFirstPage
                    )
                } else {
                    db.getRecentMangaLimit(
                        query,
                        isEndless,
                        if (isCustom) ENDLESS_LIMIT else historyOffset,
                        !updatePageCount && !isOnFirstPage
                    )
                }.executeAsBlocking()
            } else emptyList()
//            Timber.d("set up cReader items: ${cReading.size}")
            val rUpdates = when {
                viewType == VIEW_TYPE_ONLY_UPDATES -> db.getRecentChapters(
                    query,
                    if (isCustom) ENDLESS_LIMIT else updatesOffset,
                    !updatePageCount && !isOnFirstPage
                ).executeAsBlocking().map {
                    MangaChapterHistory(it.manga, it.chapter, HistoryImpl())
                }
                viewType != VIEW_TYPE_ONLY_HISTORY -> db.getUpdatedManga(
                    query,
                    isEndless,
                    if (isCustom) ENDLESS_LIMIT else updatesOffset,
                    !updatePageCount && !isOnFirstPage
                ).executeAsBlocking()
                else -> emptyList()
            }
            rUpdates.forEach {
                it.history.last_read = it.chapter.date_fetch
            }
//            Timber.d("set up rUpdates items: ${rUpdates.size}")
            val nAdditions = if (viewType < VIEW_TYPE_ONLY_HISTORY) {
                db.getRecentlyAdded(
                    query,
                    isEndless,
                    if (isCustom) ENDLESS_LIMIT else additionsOffset,
                    !updatePageCount && !isOnFirstPage
                ).executeAsBlocking()
            } else emptyList()
            nAdditions.forEach {
                it.history.last_read = it.manga.date_added
            }
//            Timber.d("set up nAdditons items: ${nAdditions.size}")
            Triple(cReading, rUpdates, nAdditions)
        }

        if (!isCustom &&
            (historyOffset + updatesOffset + additionsOffset == 0 || updatePageCount)
        ) {
            additionsOffset += nAdditions.size
            historyOffset += cReading.size
            updatesOffset += rUpdates.size
        }

//        Timber.d("loaded items: ")
        if (query != oldQuery) return
//        Timber.d("query matches items: ")
        val mangaList = (cReading + rUpdates + nAdditions).sortedByDescending {
            it.history.last_read
        }.distinctBy {
            if (query.isEmpty() && viewType != VIEW_TYPE_ONLY_HISTORY && viewType != VIEW_TYPE_ONLY_UPDATES) it.manga.id else it.chapter.id
        }.filter { mch ->
            if (updatePageCount && !isOnFirstPage && query.isEmpty()) {
                if (viewType != VIEW_TYPE_ONLY_HISTORY && viewType != VIEW_TYPE_ONLY_UPDATES) {
                    recentItems.none { mch.manga.id == it.mch.manga.id }
                } else {
                    recentItems.none { mch.chapter.id == it.mch.chapter.id }
                }
            } else true
        }
        val pairs = mangaList.mapNotNull {
            val chapter = when {
                viewType == VIEW_TYPE_ONLY_UPDATES -> it.chapter
                it.chapter.read || it.chapter.id == null -> getNextChapter(it.manga)
                    ?: if (showRead && it.chapter.id != null) it.chapter else null
                it.history.id == null -> getFirstUpdatedChapter(it.manga, it.chapter)
                    ?: if (showRead && it.chapter.id != null) it.chapter else null
                else -> it.chapter
            }
            if (chapter == null) if ((query.isNotEmpty() || viewType > VIEW_TYPE_UNGROUP_ALL) &&
                it.chapter.id != null
            ) Pair(it, it.chapter)
            else null
            else Pair(it, chapter)
        }
//        Timber.d("setting new items")
        val newItems = if (query.isEmpty() && !isUngrouped) {
            val nChaptersItems =
                pairs.asSequence()
                    .filter { it.first.history.id == null && it.first.chapter.id != null }
                    .sortedWith { f1, f2 ->
                        if (abs(f1.second.date_fetch - f2.second.date_fetch) <=
                            TimeUnit.HOURS.toMillis(12)
                        ) {
                            f2.second.date_upload.compareTo(f1.second.date_upload)
                        } else {
                            f2.second.date_fetch.compareTo(f1.second.date_fetch)
                        }
                    }
                    .take(4).map {
                        RecentMangaItem(
                            it.first,
                            it.second,
                            newChaptersHeader
                        )
                    }.toMutableList()
            val cReadingItems =
                pairs.filter { it.first.history.id != null }.take(9 - nChaptersItems.size).map {
                    RecentMangaItem(
                        it.first,
                        it.second,
                        continueReadingHeader
                    )
                }.toMutableList()
            if (nChaptersItems.isNotEmpty()) {
                nChaptersItems.add(RecentMangaItem(header = newChaptersHeader))
            }
            if (cReadingItems.isNotEmpty()) {
                cReadingItems.add(RecentMangaItem(header = continueReadingHeader))
            }
            val nAdditionsItems = pairs.filter { it.first.chapter.id == null }.take(4)
                .map { RecentMangaItem(it.first, it.second, newAdditionsHeader) }
            listOf(nChaptersItems, cReadingItems, nAdditionsItems).sortedByDescending {
                it.firstOrNull()?.mch?.history?.last_read ?: 0L
            }.flatten()
        } else {
            if (viewType == VIEW_TYPE_ONLY_UPDATES) {
                val map =
                    TreeMap<Date, MutableList<Pair<MangaChapterHistory, Chapter>>> { d1, d2 ->
                        d2
                            .compareTo(d1)
                    }
                val byDay =
                    pairs.groupByTo(map, { getMapKey(it.first.history.last_read) })
                byDay.flatMap {
                    val dateItem = DateItem(it.key, true)
                    it.value.map { item ->
                        RecentMangaItem(item.first, item.second, dateItem)
                    }
                }
            } else pairs.map { RecentMangaItem(it.first, it.second, null) }
        }
//        Timber.d("setting some items")
        if (customViewType == null) {
            recentItems = if (isOnFirstPage || !updatePageCount) {
                newItems
            } else {
                recentItems + newItems
            }
        } else {
            heldItems[customViewType] = newItems
        }
        val newCount = itemCount + newItems.size
        val hasNewItems = newItems.isNotEmpty()
        if (updatePageCount && newCount < 25 && viewType != VIEW_TYPE_GROUP_ALL && query.isEmpty() && !limit) {
            // Timber.d("needs to retry. has New items: $hasNewItems, offset: $subUpdatesOffset")
            runRecents(oldQuery, true, retryCount + (if (hasNewItems) 0 else 1), newCount)
            return
        }
        if (!limit) {
            setDownloadedChapters(recentItems)
            if (customViewType == null) {
                withContext(Dispatchers.Main) {
                    controller?.showLists(recentItems, hasNewItems, shouldMoveToTop)
                    isLoading = false
                    shouldMoveToTop = false
                }
//                scope.launchIO {
//                    (0..3).map {
//                        async {
//                            if (this@RecentsPresenter.viewType != it) {
//                                runRecents(oldQuery, customViewType = it)
//                            }
//                        }
//                    }.awaitAll()
//                }
            }
        }
    }

    private fun getNextChapter(manga: Manga): Chapter? {
        val chapters = db.getChapters(manga).executeAsBlocking()
        return chapters.sortedByDescending { it.source_order }.find { !it.read }
    }

    private fun getFirstUpdatedChapter(manga: Manga, chapter: Chapter): Chapter? {
        val chapters = db.getChapters(manga).executeAsBlocking()
        return chapters.sortedByDescending { it.source_order }.find {
            !it.read && abs(it.date_fetch - chapter.date_fetch) <= TimeUnit.HOURS.toMillis(12)
        }
    }

    fun onDestroy() {
        downloadManager.removeListener(this)
        LibraryUpdateService.removeListener(this)
        lastRecents = recentItems
    }

    fun cancelScope() {
        scope.cancel()
    }

    fun toggleGroupRecents(pref: Int, updatePref: Boolean = true) {
        if (updatePref) {
            preferences.recentsViewType().set(pref)
        }
        viewType = pref
        resetOffsets()
        getRecents()
    }

    /**
     * Finds and assigns the list of downloaded chapters.
     *
     * @param chapters the list of chapter from the database.
     */
    private fun setDownloadedChapters(chapters: List<RecentMangaItem>) {
        for (item in chapters.filter { it.chapter.id != null }) {
            if (downloadManager.isChapterDownloaded(item.chapter, item.mch.manga)) {
                item.status = Download.DOWNLOADED
            } else if (downloadManager.hasQueue()) {
                item.status = downloadManager.queue.find { it.chapter.id == item.chapter.id }
                    ?.status ?: 0
            }
        }
    }

    override fun updateDownload(download: Download) {
        recentItems.find { it.chapter.id == download.chapter.id }?.download = download
        scope.launch(Dispatchers.Main) {
            controller?.updateChapterDownload(download)
        }
    }

    override fun updateDownloads() {
        scope.launch {
            setDownloadedChapters(recentItems)
            withContext(Dispatchers.Main) {
                controller?.showLists(recentItems, true)
            }
        }
    }

    override fun onUpdateManga(manga: Manga?) {
        if (manga == null && !LibraryUpdateService.isRunning()) {
            scope.launch(Dispatchers.Main) { controller?.setRefreshing(false) }
        } else if (manga == null) {
            scope.launch(Dispatchers.Main) { controller?.setRefreshing(true) }
        } else {
            getRecents()
        }
    }

    /**
     * Deletes the given list of chapter.
     * @param chapter the chapter to delete.
     */
    fun deleteChapter(chapter: Chapter, manga: Manga, update: Boolean = true) {
        val source = Injekt.get<SourceManager>().getOrStub(manga.source)
        downloadManager.deleteChapters(listOf(chapter), manga, source)

        if (update) {
            val item = recentItems.find { it.chapter.id == chapter.id } ?: return
            item.apply {
                status = Download.NOT_DOWNLOADED
                download = null
            }

            controller?.showLists(recentItems, true)
        }
    }

    /**
     * Get date as time key
     *
     * @param date desired date
     * @return date as time key
     */
    private fun getMapKey(date: Long): Date {
        val cal = Calendar.getInstance()
        cal.time = Date(date)
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal.time
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param chapter the chapter to download.
     */
    fun downloadChapter(manga: Manga, chapter: Chapter) {
        downloadManager.downloadChapters(manga, listOf(chapter))
    }

    fun startDownloadChapterNow(chapter: Chapter) {
        downloadManager.startDownloadNow(chapter)
    }

    /**
     * Mark the selected chapter list as read/unread.
     * @param selectedChapters the list of selected chapters.
     * @param read whether to mark chapters as read or unread.
     */
    fun markChapterRead(
        chapter: Chapter,
        read: Boolean,
        lastRead: Int? = null,
        pagesLeft: Int? = null
    ) {
        scope.launch(Dispatchers.IO) {
            chapter.apply {
                this.read = read
                if (!read) {
                    last_page_read = lastRead ?: 0
                    pages_left = pagesLeft ?: 0
                }
            }
            db.updateChaptersProgress(listOf(chapter)).executeAsBlocking()
            getRecents()
        }
    }

    // History
    /**
     * Reset last read of chapter to 0L
     * @param history history belonging to chapter
     */
    fun removeFromHistory(history: History) {
        history.last_read = 0L
        db.updateHistoryLastRead(history).executeAsBlocking()
        getRecents()
    }

    /**
     * Removes all chapters belonging to manga from history.
     * @param mangaId id of manga
     */
    fun removeAllFromHistory(mangaId: Long) {
        val history = db.getHistoryByMangaId(mangaId).executeAsBlocking()
        history.forEach { it.last_read = 0L }
        db.updateHistoryLastRead(history).executeAsBlocking()
        getRecents()
    }

    fun requestNext() {
        if (!isLoading) {
            isLoading = true
            getRecents(true)
        }
    }

    companion object {
        private var lastRecents: List<RecentMangaItem>? = null

        const val VIEW_TYPE_GROUP_ALL = 0
        const val VIEW_TYPE_UNGROUP_ALL = 1
        const val VIEW_TYPE_ONLY_HISTORY = 2
        const val VIEW_TYPE_ONLY_UPDATES = 3
        var ENDLESS_LIMIT = 50

        suspend fun getRecentManga(): List<Pair<Manga, Long>> {
            val presenter = RecentsPresenter(null)
            presenter.viewType = 1
            presenter.runRecents(limit = true)
            return presenter.recentItems.filter { it.mch.manga.id != null }.map { it.mch.manga to it.mch.history.last_read }
        }
    }
}
