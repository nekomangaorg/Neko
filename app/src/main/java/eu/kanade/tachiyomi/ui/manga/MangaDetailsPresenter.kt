package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import android.graphics.Bitmap
import android.os.Environment
import coil.Coil
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Parameters
import coil.request.SuccessResult
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.image.coil.MangaFetcher
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.MergeSource
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.external.ExternalItem
import eu.kanade.tachiyomi.ui.manga.track.SetTrackReadingDatesDialog
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.getNewScanlatorsConditionalResetFilter
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.shouldDownloadNewChapters
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.widget.TriStateCheckBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Date

class MangaDetailsPresenter(
    private val controller: MangaDetailsController,
    val manga: Manga,
    val preferences: PreferencesHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    private val chapterFilter: ChapterFilter = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val statusHandler: StatusHandler = Injekt.get(),
) : DownloadQueue.DownloadListener, LibraryServiceListener {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    private val mangaShortcutManager: MangaShortcutManager by injectLazy()

    private val chapterSort = ChapterSort(manga, chapterFilter, preferences)

    val source = sourceManager.getMangadex()

    private var hasMergeChaptersInitially = manga.isMerged()

    var isLockedFromSearch = false
    var hasRequested = false
    var isLoading = false
    var scrollType = 0

    private val trackManager: TrackManager by injectLazy()

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged } }
    
    var tracks = emptyList<Track>()

    var trackList: List<TrackItem> = emptyList()

    var externalLinksList: List<ExternalItem> = emptyList()

    var chapters: List<ChapterItem> = emptyList()
        private set

    var allChapters: List<ChapterItem> = emptyList()
        private set

    var allChapterScanlators: Set<String> = emptySet()
        private set

    var filteredScanlators: Set<String> = emptySet()

    var headerItem = MangaHeaderItem(manga, controller.fromCatalogue)
    var tabletChapterHeaderItem: MangaHeaderItem? = null

    fun onCreate() {
        headerItem.isTablet = controller.isTablet
        if (controller.isTablet) {
            tabletChapterHeaderItem = MangaHeaderItem(manga, false)
            tabletChapterHeaderItem?.isChapterHeader = true
        }
        isLockedFromSearch = SecureActivityDelegate.shouldBeLocked()
        headerItem.isLocked = isLockedFromSearch
        downloadManager.addListener(this)
        LibraryUpdateService.setListener(this)
        tracks = db.getTracks(manga).executeAsBlocking()
        if (!manga.initialized) {
            isLoading = true
            controller.setRefresh(true)
            controller.updateHeader()
            refreshAll()
        } else {
            runBlocking {
                getChapters()
                manga.scanlator_filter?.let {
                    filteredScanlators = MdUtil.getScanlators(it).toSet()
                }
            }
            controller.updateChapters(this.chapters)
        }
        fetchExternalLinks()
        setTrackItems()
        refreshTracking(false)
        syncChapterReadStatus()
    }

    fun onDestroy() {
        downloadManager.removeListener(this)
        LibraryUpdateService.removeListener(this)
    }

    fun cancelScope() {
        scope.cancel()
    }

    fun fetchChapters(andTracking: Boolean = true) {
        scope.launch {
            getChapters()
            if (andTracking) {
                fetchTracks()
            }
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    private suspend fun getChapters() {
        val chapters = db.getChapters(manga).executeOnIO().map { it.toModel() }

        // update all scanlators
        updateScanlators(chapters)

        // Find downloaded chapters
        setDownloadedChapters(chapters)

        // Store the last emission
        allChapters = chapters
        this.chapters = applyChapterFilters(chapters)
    }

    private fun updateScanlators(chapters: List<ChapterItem>) {
        allChapterScanlators = chapters.flatMap { it -> it.chapter.scanlatorList() }.toSet()
        if (filteredScanlators.contains(MergeSource.name) && !allChapterScanlators.contains(
                MergeSource.name
            )
        ) {
            val tempSet = filteredScanlators.toMutableSet()
            tempSet.remove(MergeSource.name)
            filteredScanlators = tempSet.toSet()
            manga.scanlator_filter = MdUtil.getScanlatorString(filteredScanlators)
            db.insertManga(manga)
        }
        if (filteredScanlators.isEmpty()) {
            filteredScanlators = allChapterScanlators
        }
    }

    fun filterScanlatorsClicked(selectedScanlators: List<String>) {
        allChapterScanlators.filter { selectedScanlators.contains(it) }.toSet()

        filteredScanlators = allChapterScanlators.filter { selectedScanlators.contains(it) }.toSet()

        if (filteredScanlators.size == allChapterScanlators.size) {
            manga.scanlator_filter = null
        } else {
            manga.scanlator_filter = MdUtil.getScanlatorString(filteredScanlators)
        }
        asyncUpdateMangaAndChapters()
    }

    /**
     * Finds and assigns the list of downloaded chapters.
     *
     * @param chapters the list of chapter from the database.
     */
    private fun setDownloadedChapters(chapters: List<ChapterItem>) {
        for (chapter in chapters) {
            if (downloadManager.isChapterDownloaded(chapter, manga)) {
                chapter.status = Download.State.DOWNLOADED
            } else if (downloadManager.hasQueue()) {
                chapter.status = downloadManager.queue.find { it.chapter.id == chapter.id }
                    ?.status ?: Download.State.default
            }
        }
    }

    override fun updateDownload(download: Download) {
        chapters.find { it.id == download.chapter.id }?.download = download
        scope.launch(Dispatchers.Main) {
            controller.updateChapterDownload(download)
        }
    }

    override fun updateDownloads() {
        scope.launch(Dispatchers.Default) {
            getChapters()
            withContext(Dispatchers.Main) {
                controller.updateChapters(chapters)
            }
        }
    }

    /**
     * Converts a chapter from the database to an extended model, allowing to store new fields.
     */
    private fun Chapter.toModel(): ChapterItem {
        // Create the model object.
        val model = ChapterItem(this, manga)
        model.isLocked = isLockedFromSearch

        // Find an active download for this chapter.
        val download = downloadManager.queue.find { it.chapter.id == id }

        if (download != null) {
            // If there's an active download, assign it.
            model.download = download
        }
        return model
    }

    /**
     * Whether the sorting method is descending or ascending.
     */
    fun sortDescending() = manga.sortDescending(preferences)

    fun sortingOrder() = manga.chapterOrder(preferences)

    /**
     * Applies the view filters to the list of chapters obtained from the database.
     * @param chapterList the list of chapters from the database
     * @return an observable of the list of chapters filtered and sorted.
     */
    private fun applyChapterFilters(chapterList: List<ChapterItem>): List<ChapterItem> {
        if (isLockedFromSearch) {
            return chapterList
        }
        getScrollType(chapterList)
        return chapterSort.getChaptersSorted(chapterList)
    }

    private fun getScrollType(chapters: List<ChapterItem>) {
        scrollType = when {
            ChapterUtil.hasMultipleVolumes(chapters) -> MULTIPLE_VOLUMES
            ChapterUtil.hasMultipleSeasons(chapters) -> MULTIPLE_SEASONS
            ChapterUtil.hasTensOfChapters(chapters) -> TENS_OF_CHAPTERS
            else -> 0
        }
    }

    /**
     * Returns the next unread chapter or null if everything is read.
     */
    fun getNextUnreadChapter(): ChapterItem? {
        return chapterSort.getNextUnreadChapter(chapters)
    }

    fun anyRead(): Boolean = allChapters.any { it.read }
    fun hasBookmark(): Boolean = allChapters.any { it.bookmark }
    fun hasDownloads(): Boolean = allChapters.any { it.isDownloaded }

    fun getUnreadChaptersSorted() =
        allChapters.filter { !it.read && it.status == Download.State.NOT_DOWNLOADED }
            .distinctBy { it.name }
            .sortedWith(chapterSort.sortComparator(true))

    fun startDownloadingNow(chapter: Chapter) {
        downloadManager.startDownloadNow(chapter)
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param chapters the list of chapters to download.
     */
    fun downloadChapters(chapters: List<ChapterItem>) {
        downloadManager.downloadChapters(manga, chapters.filter { !it.isDownloaded })
    }

    /**
     * Deletes the given list of chapter.
     * @param chapter the chapter to delete.
     */
    fun deleteChapter(chapter: ChapterItem) {
        downloadManager.deleteChapters(listOf(chapter), manga, source)
        this.chapters.find { it.id == chapter.id }?.apply {
            status = Download.State.QUEUE
            download = null
        }

        controller.updateChapters(this.chapters)
    }

    /**
     * Deletes the given list of chapter.
     * @param chapters the list of chapters to delete.
     */
    fun deleteChapters(
        chapters: List<ChapterItem>,
        update: Boolean = true,
        isEverything: Boolean = false,
    ) {
        launchIO {
            if (isEverything) {
                downloadManager.deleteManga(manga, source)
            } else {
                downloadManager.deleteChapters(chapters, manga, source)
            }
        }
        chapters.forEach { chapter ->
            this.chapters.find { it.id == chapter.id }?.apply {
                status = Download.State.QUEUE
                download = null
            }
        }

        if (update) controller.updateChapters(this.chapters)
    }

    fun refreshMangaFromDb(): Manga {
        val dbManga = db.getManga(manga.id!!).executeAsBlocking()
        manga.copyFrom(dbManga!!)
        return dbManga
    }

    fun removeMerged() {
        scope.launch {
            withContext(Dispatchers.IO) {
                manga.merge_manga_url = null
                hasMergeChaptersInitially = false
                db.insertManga(manga)
                val dbChapters =
                    db.getChapters(manga).executeAsBlocking().partition { it.isMergedChapter() }
                val mergedChapters = dbChapters.first
                val nonMergedChapters = dbChapters.second
                downloadManager.deleteChapters(mergedChapters, manga, sourceManager.getMangadex())
                db.deleteChapters(mergedChapters).executeAsBlocking()
                allChapterScanlators =
                    nonMergedChapters.flatMap { it -> it.scanlatorList() }.toSet()
                if (filteredScanlators.isNotEmpty()) {
                    val newSet = filteredScanlators.toMutableSet()
                    newSet.remove(sourceManager.getMergeSource().name)
                    filteredScanlators = newSet.toSet()
                }
                refreshAll()
            }
        }
    }

    fun attachMergeManga(mergeManga: SManga?) {
        manga.merge_manga_url = mergeManga?.url
        manga.merge_manga_image_url = mergeManga?.thumbnail_url
        val tempSet = filteredScanlators.toMutableSet()
        tempSet.add(MergeSource.name)
        filteredScanlators = tempSet
        manga.scanlator_filter = MdUtil.getScanlatorString(filteredScanlators)
        db.insertManga(manga)
    }

    /** Refresh Manga Info and Chapter List (not tracking) */
    fun refreshAll() {
        if (controller.isNotOnline()) return
        XLog.d("refreshing all")

        scope.launch {
            isLoading = true
            var errorFromNetwork: Throwable? = null
            var errorFromMerged: Throwable? = null
            var error = false
            val thumbnailUrl = manga.thumbnail_url

            if (source.checkIfUp().not()) {
                withContext(Dispatchers.Main) {
                    controller.showError("MangaDex appears to be down, or under heavy load")
                }
                return@launch
            }
            XLog.d("begin processing chapters and manga refresh")

            val deferredManga = async {
                runCatching {
                    source.fetchMangaDetails(manga)
                }.getOrElse { e ->
                    XLog.e("error with mangadex getting manga", e)
                    error = true
                    errorFromNetwork = e
                    null
                }
            }

            val deferredChapters = async {
                runCatching {
                    source.fetchChapterList(manga)
                }.getOrElse { e ->
                    XLog.e("error with mangadex getting chapters", e)
                    error = true
                    errorFromNetwork = e
                    emptyList()
                }
            }

            val deferredMergedChapters = async {
                if (manga.isMerged()) {
                    kotlin.runCatching {
                        sourceManager.getMergeSource()
                            .fetchChapters(manga.merge_manga_url!!)
                    }.getOrElse { e ->
                        XLog.e("error with mergedsource", e)
                        error = true
                        errorFromMerged = e
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

            val networkManga = deferredManga.await()
            val mangaWasInitalized = manga.initialized

            if (networkManga != null) {
                launchIO {
                    // only copy if it had no data
                    manga.copyFrom(networkManga)
                    manga.initialized = true

                    // force new cover if it exists
                    if (networkManga.thumbnail_url != null || preferences.refreshCoversToo()
                            .getOrDefault()
                    ) {
                        coverCache.deleteFromCache(thumbnailUrl)
                    }

                    db.insertManga(manga).executeOnIO()

                    fetchExternalLinks()

                    launchIO {
                        val request =
                            ImageRequest.Builder(preferences.context).data(manga)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .parameters(
                                    Parameters.Builder().set(MangaFetcher.onlyFetchRemotely, true)
                                        .build()
                                )
                                .build()

                        if (Coil.imageLoader(preferences.context)
                                .execute(request) is SuccessResult
                        ) {
                            preferences.context.imageLoader.memoryCache.remove(MemoryCache.Key(manga.key()))
                            withContext(Dispatchers.Main) {
                                controller.setPaletteColor()
                            }
                        }
                    }
                }
            }
            val finChapters = deferredChapters.await() + deferredMergedChapters.await()
            if (!error) {
                val originalChapters = db.getChapters(manga).executeAsBlocking()
                val newChapters = syncChaptersWithSource(db, finChapters, manga)
                if (newChapters.first.isNotEmpty()) {
                    val downloadNew = preferences.downloadNew().get()
                    if (downloadNew && !controller.fromCatalogue && mangaWasInitalized) {
                        if (!hasMergeChaptersInitially && manga.isMerged()) {
                            hasMergeChaptersInitially = true
                        } else {
                            if (manga.shouldDownloadNewChapters(db, preferences)) {
                                downloadChapters(
                                    newChapters.first.sortedBy { it.chapter_number }
                                        .map { it.toModel() }
                                )
                            }
                        }
                    }
                    mangaShortcutManager.updateShortcuts()

                    val allChaps = db.getChapters(manga).executeOnIO()
                    launch {
                        manga.getNewScanlatorsConditionalResetFilter(
                            db,
                            originalChapters,
                            newChapters.first
                        )
                        updateScanlators(allChaps.map { it.toModel() })
                    }
                }
                if (newChapters.second.isNotEmpty()) {
                    val removedChaptersId = newChapters.second.map { it.id }
                    val removedChapters = this@MangaDetailsPresenter.chapters.filter {
                        it.id in removedChaptersId && it.isDownloaded
                    }
                    if (removedChapters.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            controller.showChaptersRemovedPopup(
                                removedChapters
                            )
                        }
                    }
                }
            }
            getChapters()

            launchIO {
                launch {
                    val allChaps = db.getChapters(manga).executeOnIO()
                    val missingChapters = MdUtil.getMissingChapterCount(allChaps, manga.status)
                    if (missingChapters != manga.missing_chapters) {
                        manga.missing_chapters = missingChapters
                        db.insertManga(manga).executeOnIO()
                    }
                }
                refreshTracking(false)
                syncChapterReadStatus()
            }
            launchIO {
                getChapters()
                withContext(Dispatchers.Main) {
                    isLoading = false

                    if (errorFromNetwork == null) {
                        controller.updateChapters(this@MangaDetailsPresenter.chapters)
                    } else {
                        controller.showError("MangaDex error: " + trimException(errorFromNetwork!!))
                    }
                    if (errorFromMerged != null) {
                        controller.showError("MergedSource error: " + trimException(errorFromMerged!!))
                    }
                }
            }
        }
    }

    /**
     * Requests an updated list of chapters from the source.
     */
    fun fetchChaptersFromSource() {
        hasRequested = true
        isLoading = true

        scope.launch(Dispatchers.IO) {
            val chapters = try {
                source.fetchChapterList(manga)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { controller.showError(trimException(e)) }
                return@launch
            }
            isLoading = false
            try {
                syncChaptersWithSource(db, chapters, manga)

                getChapters()
                withContext(Dispatchers.Main) { controller.updateChapters(this@MangaDetailsPresenter.chapters) }
            } catch (e: java.lang.Exception) {
                withContext(Dispatchers.Main) {
                    controller.showError(trimException(e))
                }
            }
        }
    }

    private fun trimException(e: Throwable): String {
        return (
            if (e.message?.contains(": ") == true) e.message?.split(": ")?.drop(1)
                ?.joinToString(": ")
            else e.message
            ) ?: preferences.context.getString(R.string.unknown_error)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param selectedChapters the list of chapters to bookmark.
     */
    fun bookmarkChapters(selectedChapters: List<ChapterItem>, bookmarked: Boolean) {
        scope.launch(Dispatchers.IO) {
            selectedChapters.forEach {
                it.bookmark = bookmarked
            }
            db.updateChaptersProgress(selectedChapters).executeAsBlocking()
            getChapters()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    /**
     * Mark the selected chapter list as read/unread.
     * @param selectedChapters the list of selected chapters.
     * @param read whether to mark chapters as read or unread.
     */
    fun markChaptersRead(
        selectedChapters: List<ChapterItem>,
        read: Boolean,
        deleteNow: Boolean = true,
        lastRead: Int? = null,
        pagesLeft: Int? = null,
        skipReadingSync: Boolean = false,
    ) {
        scope.launch(Dispatchers.IO) {
            selectedChapters.forEach {
                it.read = read
                if (!read) {
                    it.last_page_read = lastRead ?: 0
                    it.pages_left = pagesLeft ?: 0
                }
                if (preferences.readingSync() && it.chapter.isMergedChapter()
                        .not() && skipReadingSync.not()
                ) {
                    launchIO {
                        when (read) {
                            true -> statusHandler.markChapterRead(it.chapter.mangadex_chapter_id)
                            false -> statusHandler.markChapterUnRead(it.chapter.mangadex_chapter_id)
                        }
                    }
                }
            }
            db.updateChaptersProgress(selectedChapters).executeAsBlocking()

            val numberOfNonBookmarkedChapters =
                selectedChapters.filter { it.bookmark.not() }.toList()
            if (read && deleteNow && preferences.removeAfterMarkedAsRead() && numberOfNonBookmarkedChapters.size > 0) {
                deleteChapters(numberOfNonBookmarkedChapters, false)
            }

            getChapters()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    /**
     * Sets the sorting order and requests an UI update.
     */
    fun setSortOrder(sort: Int, descend: Boolean) {
        manga.setChapterOrder(sort,
            if (descend) Manga.CHAPTER_SORT_DESC else Manga.CHAPTER_SORT_ASC)
        if (mangaSortMatchesDefault()) {
            manga.setSortToGlobal()
        }
        asyncUpdateMangaAndChapters()
    }

    fun setGlobalChapterSort(sort: Int, descend: Boolean) {
        preferences.sortChapterOrder().set(sort)
        preferences.chaptersDescAsDefault().set(descend)
        manga.setSortToGlobal()
        asyncUpdateMangaAndChapters()
    }

    fun mangaSortMatchesDefault(): Boolean {
        return (
            manga.sortDescending == preferences.chaptersDescAsDefault().get() &&
                manga.sorting == preferences.sortChapterOrder().get()
            ) || !manga.usesLocalSort
    }

    fun mangaFilterMatchesDefault(): Boolean {
        return (
            manga.readFilter == preferences.filterChapterByRead().get() &&
                manga.downloadedFilter == preferences.filterChapterByDownloaded().get() &&
                manga.bookmarkedFilter == preferences.filterChapterByBookmarked().get() &&
                manga.hideChapterTitles == preferences.hideChapterTitlesByDefault().get()
            ) || !manga.usesLocalFilter
    }

    fun resetSortingToDefault() {
        manga.setSortToGlobal()
        asyncUpdateMangaAndChapters()
    }

    /**
     * Removes all filters and requests an UI update.
     */
    fun setFilters(
        unread: TriStateCheckBox.State,
        downloaded: TriStateCheckBox.State,
        bookmarked: TriStateCheckBox.State,
    ) {
        manga.readFilter = when (unread) {
            TriStateCheckBox.State.CHECKED -> Manga.CHAPTER_SHOW_UNREAD
            TriStateCheckBox.State.INVERSED -> Manga.CHAPTER_SHOW_READ
            else -> Manga.SHOW_ALL
        }
        manga.downloadedFilter = when (downloaded) {
            TriStateCheckBox.State.CHECKED -> Manga.CHAPTER_SHOW_DOWNLOADED
            TriStateCheckBox.State.INVERSED -> Manga.CHAPTER_SHOW_NOT_DOWNLOADED
            else -> Manga.SHOW_ALL
        }
        manga.bookmarkedFilter = when (bookmarked) {
            TriStateCheckBox.State.CHECKED -> Manga.CHAPTER_SHOW_BOOKMARKED
            TriStateCheckBox.State.INVERSED -> Manga.CHAPTER_SHOW_NOT_BOOKMARKED
            else -> Manga.SHOW_ALL
        }
        manga.setFilterToLocal()
        if (mangaFilterMatchesDefault()) {
            manga.setFilterToGlobal()
        }
        asyncUpdateMangaAndChapters()
    }

    /**
     * Sets the active display mode.
     * @param hide set title to hidden
     */
    fun hideTitle(hide: Boolean) {
        manga.displayMode = if (hide) Manga.CHAPTER_DISPLAY_NUMBER else Manga.CHAPTER_DISPLAY_NAME
        db.updateChapterFlags(manga).executeAsBlocking()
        manga.setFilterToLocal()
        if (mangaFilterMatchesDefault()) {
            manga.setFilterToGlobal()
        }
        controller.refreshAdapter()
    }

    fun resetFilterToDefault() {
        manga.setFilterToGlobal()
        asyncUpdateMangaAndChapters()
    }

    fun setGlobalChapterFilters(
        unread: TriStateCheckBox.State,
        downloaded: TriStateCheckBox.State,
        bookmarked: TriStateCheckBox.State,
    ) {
        preferences.filterChapterByRead().set(
            when (unread) {
                TriStateCheckBox.State.CHECKED -> Manga.CHAPTER_SHOW_UNREAD
                TriStateCheckBox.State.INVERSED -> Manga.CHAPTER_SHOW_READ
                else -> Manga.SHOW_ALL
            }
        )
        preferences.filterChapterByDownloaded().set(
            when (downloaded) {
                TriStateCheckBox.State.CHECKED -> Manga.CHAPTER_SHOW_DOWNLOADED
                TriStateCheckBox.State.INVERSED -> Manga.CHAPTER_SHOW_NOT_DOWNLOADED
                else -> Manga.SHOW_ALL
            }
        )
        preferences.filterChapterByBookmarked().set(
            when (bookmarked) {
                TriStateCheckBox.State.CHECKED -> Manga.CHAPTER_SHOW_BOOKMARKED
                TriStateCheckBox.State.INVERSED -> Manga.CHAPTER_SHOW_NOT_BOOKMARKED
                else -> Manga.SHOW_ALL
            }
        )
        preferences.hideChapterTitlesByDefault().set(manga.hideChapterTitles)
        manga.setFilterToGlobal()
        asyncUpdateMangaAndChapters()
    }

    private fun asyncUpdateMangaAndChapters(justChapters: Boolean = false) {
        scope.launch {
            if (!justChapters) db.updateChapterFlags(manga).executeOnIO()
            getChapters()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    fun currentFilters(): String {
        val filtersId = mutableListOf<Int?>()
        filtersId.add(if (manga.readFilter(preferences) == Manga.CHAPTER_SHOW_READ) R.string.read else null)
        filtersId.add(if (manga.readFilter(preferences) == Manga.CHAPTER_SHOW_UNREAD) R.string.unread else null)
        filtersId.add(if (manga.downloadedFilter(preferences) == Manga.CHAPTER_SHOW_DOWNLOADED) R.string.downloaded else null)
        filtersId.add(if (manga.downloadedFilter(preferences) == Manga.CHAPTER_SHOW_NOT_DOWNLOADED) R.string.not_downloaded else null)
        filtersId.add(if (manga.bookmarkedFilter(preferences) == Manga.CHAPTER_SHOW_BOOKMARKED) R.string.bookmarked else null)
        filtersId.add(if (manga.bookmarkedFilter(preferences) == Manga.CHAPTER_SHOW_NOT_BOOKMARKED) R.string.not_bookmarked else null)
        filtersId.add(if (filteredScanlators.size != allChapterScanlators.size) R.string.scanlator_groups else null)
        return filtersId.filterNotNull().joinToString(", ") { preferences.context.getString(it) }
    }

    fun toggleFavorite(): Boolean {
        manga.favorite = !manga.favorite

        when (manga.favorite) {
            true -> {
                manga.date_added = Date().time
                if (preferences.addToLibraryAsPlannedToRead()) {
                    val mdTrack = trackList.firstOrNull { it.service.isMdList() }?.track

                    mdTrack?.let {
                        if (FollowStatus.fromInt(it.status) == FollowStatus.UNFOLLOWED) {
                            it.status = FollowStatus.PLAN_TO_READ.int
                            scope.launch {
                                trackManager.getService(TrackManager.MDLIST)!!.update(it)
                            }
                        }
                    }
                }
            }
            false -> manga.date_added = 0
        }

        db.insertManga(manga).executeAsBlocking()
        controller.updateHeader()
        return manga.favorite
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    fun getCategories(): List<Category> {
        return db.getCategories().executeAsBlocking()
    }

    fun confirmDeletion() {
        launchIO {
            coverCache.deleteFromCache(manga)
            downloadManager.deleteManga(manga, source)
            asyncUpdateMangaAndChapters(true)
        }
    }

    fun setFavorite(favorite: Boolean) {
        if (manga.favorite == favorite) {
            return
        }
        toggleFavorite()
    }

    override fun onUpdateManga(manga: Manga?) {
        if (manga?.id == this.manga.id) {
            fetchChapters()
        }
    }

    fun shareManga(cover: Bitmap) {
        val context = Injekt.get<Application>()

        val destDir = File(context.cacheDir, "shared_image")

        scope.launch(Dispatchers.IO) {
            destDir.deleteRecursively()
            try {
                val image = saveImage(cover, destDir, manga)
                if (image != null) controller.shareManga(image)
                else controller.shareManga()
            } catch (e: java.lang.Exception) {
            }
        }
    }

    private fun saveImage(cover: Bitmap, directory: File, manga: Manga): File? {
        directory.mkdirs()

        // Build destination file.
        val filename = DiskUtil.buildValidFilename("${manga.title} - Cover.jpg")

        val destFile = File(directory, filename)
        val stream: OutputStream = FileOutputStream(destFile)
        cover.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        stream.flush()
        stream.close()
        return destFile
    }

/*    fun editCoverWithStream(uri: Uri): Boolean {
        val inputStream =
            downloadManager.context.contentResolver.openInputStream(uri) ?: return false
        if (manga.favorite) {
            coverCache.setCustomCoverToCache(manga, inputStream)
            controller.setPaletteColor()
            return true
        }
        return false
    }*/

    fun shareCover(): File? {
        return try {
            val destDir = File(coverCache.context.cacheDir, "shared_image")
            val file = saveCover(destDir)
            file
        } catch (e: Exception) {
            null
        }
    }

    fun saveCover(): Boolean {
        return try {
            val directory = File(
                Environment.getExternalStorageDirectory().absolutePath +
                    File.separator + Environment.DIRECTORY_PICTURES +
                    File.separator + preferences.context.getString(R.string.app_name)
            )
            saveCover(directory)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun saveCover(directory: File): File {
        val cover = coverCache.getCoverFile(manga)
        val type = ImageUtil.findImageType(cover.inputStream())
            ?: throw Exception("Not an image")

        directory.mkdirs()

        // Build destination file.
        val filename = DiskUtil.buildValidFilename("${manga.title}.${type.extension}")

        val destFile = File(directory, filename)
        cover.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    fun isTracked(): Boolean =
        loggedServices.any { service -> tracks.any { it.sync_id == service.id } }

    fun hasTrackers(): Boolean = loggedServices.isNotEmpty()

    // Tracking
    private fun setTrackItems() {
        scope.launch {
            trackList = loggedServices.map { service ->
                TrackItem(tracks.find { it.sync_id == service.id }, service)
            }
        }
    }

    private fun fetchExternalLinks() {
        scope.launch {
            externalLinksList = manga.getExternalLinks().map { external ->
                ExternalItem(external)
            }
        }
    }

    private suspend fun fetchTracks() {
        tracks = withContext(Dispatchers.IO) { db.getTracks(manga).executeAsBlocking() }
        trackList = loggedServices.map { service ->
            TrackItem(tracks.find { it.sync_id == service.id }, service)
        }
        withContext(Dispatchers.Main) { controller.refreshTracking(trackList) }
    }

    fun refreshTracking(showOfflineSnack: Boolean = false) {
        if (!controller.isNotOnline(showOfflineSnack)) {
            scope.launch {
                if (tracks.isEmpty() || !tracks.any { it.sync_id == trackManager.mdList.id }) {
                    val track = trackManager.mdList.createInitialTracker(manga)
                    db.insertTrack(track).executeAsBlocking()
                    tracks = db.getTracks(manga).executeAsBlocking()
                    setTrackItems()
                }
                val asyncList = trackList.filter { it.track != null }.map { item ->
                    async(Dispatchers.IO) {
                        val trackItem = try {
                            item.service.refresh(item.track!!)
                        } catch (e: Exception) {
                            trackError(e)
                            null
                        }
                        if (trackItem != null) {
                            if (item.service.isMdList()) {
                                if (manga.favorite && preferences.addToLibraryAsPlannedToRead() && trackItem.status == FollowStatus.UNFOLLOWED.int) {
                                    trackItem.status = FollowStatus.PLAN_TO_READ.int
                                    scope.launch {
                                        trackManager.getService(TrackManager.MDLIST)!!
                                            .update(trackItem)
                                    }
                                }

                                if (trackItem.total_chapters == 0 && manga.last_chapter_number != null && manga.last_chapter_number != 0) {
                                    trackItem.total_chapters = manga.last_chapter_number!!
                                }
                            }

                            db.insertTrack(trackItem).executeAsBlocking()
                            trackItem
                        } else item.track
                    }
                }
                asyncList.awaitAll()
                fetchTracks()
            }
        }
    }

    fun syncChapterReadStatus() {
        if (preferences.readingSync().not()) return

        scope.launch {
            statusHandler.getReadChapterIds(MdUtil.getMangaId(manga.url)).collect { chapterIds ->
                val chaptersToMark = chapters.asSequence().filter { it.isMergedChapter().not() }
                    .filter { chapterIds.contains(it.mangadex_chapter_id) }
                    .toList()
                markChaptersRead(chaptersToMark, true, skipReadingSync = true)
            }
        }
    }

    fun mergeSearch(query: String) {
        if (!controller.isNotOnline()) {
            scope.launch(Dispatchers.IO) {
                val result = async {
                    try {
                        sourceManager.getMergeSource().searchManga(query)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { controller.onMergeSearchError(e) }
                        null
                    }
                }

                val results = result.await()
                if (!results.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { controller.onMergeSearchResults(results) }
                } else {
                    withContext(Dispatchers.Main) {
                        controller.onMergeSearchError(
                            Exception(
                                preferences.context.getString(
                                    R.string.no_results_found
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun trackSearch(query: String, service: TrackService, wasPreviouslyTracked: Boolean) {
        if (!controller.isNotOnline()) {
            scope.launch(Dispatchers.IO) {
                val results = try {
                    service.search(query, manga, wasPreviouslyTracked)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { controller.trackSearchError(e) }
                    null
                }
                if (!results.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { controller.onTrackSearchResults(results) }
                } else {
                    withContext(Dispatchers.Main) {
                        controller.trackSearchError(
                            Exception(
                                preferences.context.getString(
                                    R.string.no_results_found
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun registerTracking(item: Track?, service: TrackService) {
        if (item != null) {
            item.manga_id = manga.id!!

            scope.launch {
                val binding = try {
                    service.bind(item)
                } catch (e: Exception) {
                    if (e.message != null && !e.message!!.contains("Not Logged in to MangaDex")) {
                        trackError(e)
                    }
                    null
                }
                withContext(Dispatchers.IO) {
                    if (binding != null) db.insertTrack(binding).executeAsBlocking()
                }
                fetchTracks()
            }
        }
    }

    fun removeTracker(trackItem: TrackItem, removeFromService: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                db.deleteTrackForManga(manga, trackItem.service).executeAsBlocking()
                if (removeFromService && trackItem.service.canRemoveFromService()) {
                    trackItem.service.removeFromService(trackItem.track!!)
                }
            }
            fetchTracks()
        }
    }

    private fun updateRemote(track: Track, service: TrackService) {
        scope.launch {
            val binding = try {
                service.update(track)
            } catch (e: Exception) {
                trackError(e)
                null
            }
            if (binding != null) {
                withContext(Dispatchers.IO) { db.insertTrack(binding).executeAsBlocking() }
                fetchTracks()
            } else trackRefreshDone()
        }
    }

    private fun trackRefreshDone() {
        scope.launch(Dispatchers.Main) { controller.trackRefreshDone() }
    }

    private fun trackError(error: Exception) {
        scope.launch(Dispatchers.Main) { controller.trackRefreshError(error) }
    }

    fun setStatus(item: TrackItem, index: Int) {
        val track = item.track!!
        track.status = item.service.getStatusList()[index]
        if (item.service.isCompletedStatus(index) && track.total_chapters > 0) {
            track.last_chapter_read = track.total_chapters
        }
        updateRemote(track, item.service)
    }

    fun setScore(item: TrackItem, index: Int) {
        val track = item.track!!
        track.score = item.service.indexToScore(index)
        updateRemote(track, item.service)
    }

    fun setLastChapterRead(item: TrackItem, chapterNumber: Int) {
        val track = item.track!!
        track.last_chapter_read = chapterNumber
        updateRemote(track, item.service)
    }

    fun setTrackerStartDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.started_reading_date = date
        updateRemote(track, item.service)
    }

    fun setTrackerFinishDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.finished_reading_date = date
        updateRemote(track, item.service)
    }

    fun getSuggestedDate(readingDate: SetTrackReadingDatesDialog.ReadingDate): Long? {
        val chapters = db.getHistoryByMangaId(manga.id ?: 0L).executeAsBlocking()
        val date = when (readingDate) {
            SetTrackReadingDatesDialog.ReadingDate.Start -> chapters.minOfOrNull { it.last_read }
            SetTrackReadingDatesDialog.ReadingDate.Finish -> chapters.maxOfOrNull { it.last_read }
        } ?: return null
        return if (date <= 0L) null else date
    }

    companion object {
        const val MULTIPLE_VOLUMES = 1
        const val TENS_OF_CHAPTERS = 2
        const val MULTIPLE_SEASONS = 3
    }
}
