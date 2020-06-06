package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import android.graphics.Bitmap
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.library.CustomMangaManager
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.external.ExternalItem
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val source: Source,
    val preferences: PreferencesHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get()
) : DownloadQueue.DownloadListener, LibraryServiceListener {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    private val customMangaManager: CustomMangaManager by injectLazy()

    var isLockedFromSearch = false
    var hasRequested = false
    var isLoading = false
    var scrollType = 0
    private val volumeRegex = Regex("""(vol|volume)\.? *([0-9]+)?""", RegexOption.IGNORE_CASE)
    private val seasonRegex = Regex("""(Season |S)([0-9]+)?""")

    private val trackManager: TrackManager by lazy { Injekt.get<TrackManager>() }

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged || it.isMdList() } }
    var tracks = emptyList<Track>()

    var trackList: List<TrackItem> = emptyList()

    var externalLinksList: List<ExternalItem> = emptyList()

    var chapters: List<ChapterItem> = emptyList()
        private set

    var allChapterScanlators: List<String> = emptyList()
        private set

    var filteredScanlators: List<String> = emptyList()

    var headerItem = MangaHeaderItem(manga, controller.fromCatalogue)

    fun onCreate() {

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
            scope.launch {
                isLoading = true
                withContext(Dispatchers.IO) {
                    manga.scanlator_filter?.let {
                        filteredScanlators = MdUtil.getScanlators(it)
                    }
                    updateChapters()
                    isLoading = false
                }

                withContext(Dispatchers.Main) { controller.updateChapters(this@MangaDetailsPresenter.chapters) }
            }
        }
        fetchExternalLinks()
        setTrackItems()
        refreshTracking(false)
        similarToolTip()
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
            if (andTracking) fetchTracks()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    private suspend fun getChapters() {
        val chapters = db.getChapters(manga).executeOnIO().map { it.toModel() }

        //update all scanlators
        updateScanlators(chapters)

        // Find downloaded chapters
        setDownloadedChapters(chapters)

        // Store the last emission
        this.chapters = applyChapterFilters(chapters)
    }

    private fun updateScanlators(chapters: List<ChapterItem>) {
        allChapterScanlators = chapters.flatMap { it -> it.chapter.scanlatorList() }.distinct().sorted()
        if (filteredScanlators.isEmpty()) {
            filteredScanlators = allChapterScanlators
        }
    }

    fun filterScanlatorsClicked(selectedScanlators: List<String>) {
        filteredScanlators = allChapterScanlators.filter { selectedScanlators.contains(it) }
        if (filteredScanlators.size == allChapterScanlators.size) {
            manga.scanlator_filter = null
        } else {
            manga.scanlator_filter = MdUtil.getScanlatorString(filteredScanlators)
        }
        asyncUpdateMangaAndChapters(true)
    }

    private fun updateChapters(fetchedChapters: List<Chapter>? = null) {
        val chapters =
            (fetchedChapters ?: db.getChapters(manga).executeAsBlocking()).map { it.toModel() }

        //update all scanlators
        updateScanlators(chapters)

        // Find downloaded chapters
        setDownloadedChapters(chapters)

        // Store the last emission
        this.chapters = applyChapterFilters(chapters)
    }

    /**
     * Finds and assigns the list of downloaded chapters.
     *
     * @param chapters the list of chapter from the database.
     */
    private fun setDownloadedChapters(chapters: List<ChapterItem>) {
        for (chapter in chapters) {
            if (downloadManager.isChapterDownloaded(chapter, manga)) {
                chapter.status = Download.DOWNLOADED
            } else if (downloadManager.hasQueue()) {
                chapter.status = downloadManager.queue.find { it.chapter.id == chapter.id }
                    ?.status ?: 0
            }
        }
    }

    override fun updateDownload(download: Download) {
        chapters.find { it.id == download.chapter.id }?.download = download
        scope.launch(Dispatchers.Main) {
            controller.updateChapterDownload(download)
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
     * Sets the active display mode.
     * @param hide set title to hidden
     */
    fun hideTitle(hide: Boolean) {
        manga.displayMode = if (hide) Manga.DISPLAY_NUMBER else Manga.DISPLAY_NAME
        db.updateFlags(manga).executeAsBlocking()
        controller.refreshAdapter()
    }

    /**
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyDownloaded() = manga.downloadedFilter == Manga.SHOW_DOWNLOADED

    /**
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyBookmarked() = manga.bookmarkedFilter == Manga.SHOW_BOOKMARKED

    /**
     * Whether the display only unread filter is enabled.
     */
    fun onlyUnread() = manga.readFilter == Manga.SHOW_UNREAD

    /**
     * Whether the display only read filter is enabled.
     */
    fun onlyRead() = manga.readFilter == Manga.SHOW_READ

    /**
     * Whether the sorting method is descending or ascending.
     */
    fun sortDescending() = manga.sortDescending(globalSort())

    /**
     * Applies the view filters to the list of chapters obtained from the database.
     * @param chapterList the list of chapters from the database
     * @return an observable of the list of chapters filtered and sorted.
     */
    private fun applyChapterFilters(chapterList: List<ChapterItem>): List<ChapterItem> {
        if (isLockedFromSearch)
            return chapterList
        var chapters = chapterList
        if (onlyUnread()) {
            chapters = chapters.filter { !it.read }
        } else if (onlyRead()) {
            chapters = chapters.filter { it.read }
        }
        if (onlyDownloaded()) {
            chapters = chapters.filter { it.isDownloaded }
        }
        if (onlyBookmarked()) {
            chapters = chapters.filter { it.bookmark }
        }

        chapters = chapters.filter {
            if (it.scanlator == null) {
                false
            } else {
                val chapterScanlators = MdUtil.getScanlators(it.scanlator!!)
                filteredScanlators.any {
                    chapterScanlators.contains(it)
                }
            }
        }

        val sortFunction: (Chapter, Chapter) -> Int = when (manga.sorting) {
            Manga.SORTING_SOURCE -> when (sortDescending()) {
                true -> { c1, c2 -> c1.source_order.compareTo(c2.source_order) }
                false -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            }
            Manga.SORTING_NUMBER -> when (sortDescending()) {
                true -> { c1, c2 -> c2.chapter_number.compareTo(c1.chapter_number) }
                false -> { c1, c2 -> c1.chapter_number.compareTo(c2.chapter_number) }
            }
            else -> { c1, c2 -> c1.source_order.compareTo(c2.source_order) }
        }
        chapters = chapters.sortedWith(Comparator(sortFunction))
        getScrollType(chapters)
        return chapters
    }

    private fun getScrollType(chapters: List<ChapterItem>) {
        scrollType = when {
            hasMultipleVolumes(chapters) -> MULTIPLE_VOLUMES
            hasMultipleSeasons(chapters) -> MULTIPLE_SEASONS
            hasTensOfChapters(chapters) -> TENS_OF_CHAPTERS
            else -> 0
        }
    }

    fun getGroupNumber(chapter: ChapterItem): Int? {
        val groups = volumeRegex.find(chapter.name)?.groups
        if (groups != null) return groups[2]?.value?.toIntOrNull()
        val seasonGroups = seasonRegex.find(chapter.name)?.groups
        if (seasonGroups != null) return seasonGroups[2]?.value?.toIntOrNull()
        return null
    }

    private fun getVolumeNumber(chapter: ChapterItem): Int? {
        val groups = volumeRegex.find(chapter.name)?.groups
        if (groups != null) return groups[2]?.value?.toIntOrNull()
        return null
    }

    private fun getSeasonNumber(chapter: ChapterItem): Int? {
        val groups = seasonRegex.find(chapter.name)?.groups
        if (groups != null) return groups[2]?.value?.toIntOrNull()
        return null
    }

    private fun hasMultipleVolumes(chapters: List<ChapterItem>): Boolean {
        val volumeSet = mutableSetOf<Int>()
        chapters.forEach {
            val volNum = getVolumeNumber(it)
            if (volNum != null) {
                volumeSet.add(volNum)
                if (volumeSet.size >= 2) return true
            }
        }
        return false
    }

    private fun hasMultipleSeasons(chapters: List<ChapterItem>): Boolean {
        val volumeSet = mutableSetOf<Int>()
        chapters.forEach {
            val volNum = getSeasonNumber(it)
            if (volNum != null) {
                volumeSet.add(volNum)
                if (volumeSet.size >= 2) return true
            }
        }
        return false
    }

    private fun hasTensOfChapters(chapters: List<ChapterItem>): Boolean {
        return chapters.size > 20
    }

    /**
     * Returns the next unread chapter or null if everything is read.
     */
    fun getNextUnreadChapter(): ChapterItem? {
        return chapters.sortedByDescending { it.source_order }.find { !it.read }
    }

    fun anyRead(): Boolean = chapters.any { it.read }
    fun hasBookmark(): Boolean = chapters.any { it.bookmark }
    fun hasDownloads(): Boolean = chapters.any { it.isDownloaded }

    fun getUnreadChaptersSorted() =
        chapters.filter { !it.read && it.status == Download.NOT_DOWNLOADED }.distinctBy { it.name }
            .sortedByDescending { it.source_order }

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
        val downloads = downloadManager.queue.toMutableList()
        downloads.remove(chapter.download)
        downloadManager.reorderQueue(downloads)

        this.chapters.find { it.id == chapter.id }?.apply {
            status = Download.NOT_DOWNLOADED
            download = null
        }

        controller.updateChapters(this.chapters)
    }

    /**
     * Deletes the given list of chapter.
     * @param chapters the list of chapters to delete.
     */
    fun deleteChapters(chapters: List<ChapterItem>, update: Boolean = true) {
        downloadManager.deleteChapters(chapters, manga, source)

        chapters.forEach { chapter ->
            this.chapters.find { it.id == chapter.id }?.apply {
                status = Download.NOT_DOWNLOADED
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

    /** Refresh Manga Info and Chapter List (not tracking) */
    fun refreshAll() {
        if (controller.isNotOnline()) return
        scope.launch {
            isLoading = true

            var errorFromNetwork: java.lang.Exception? = null
            val thumbnailUrl = manga.thumbnail_url

            val nPair = async(Dispatchers.IO) {
                try {
                    source.fetchMangaAndChapterDetails(manga)
                } catch (e: Exception) {
                    errorFromNetwork = e
                    Pair(null, emptyList<SChapter>())
                }
            }

            val networkPair = nPair.await()
            val networkManga = networkPair.first
            val mangaWasInitalized = manga.initialized
            if (networkManga != null) {
                manga.copyFrom(networkManga)
                manga.initialized = true

                if (thumbnailUrl != networkManga.thumbnail_url) {
                    coverCache.deleteFromCache(thumbnailUrl)
                }
                withContext(Dispatchers.Main) {
                    controller.setPaletteColor()
                }
                db.insertManga(manga).executeAsBlocking()
            }
            fetchExternalLinks()
            val finChapters = networkPair.second

            val newChapters = syncChaptersWithSource(db, finChapters, manga)
            if (newChapters.first.isNotEmpty()) {
                val downloadNew = preferences.downloadNew().getOrDefault()
                if (downloadNew && !controller.fromCatalogue && mangaWasInitalized) {
                    val categoriesToDownload = preferences.downloadNewCategories().getOrDefault().map(String::toInt)
                    val shouldDownload = categoriesToDownload.isEmpty() || getMangaCategoryIds().any { it in categoriesToDownload }
                    if (shouldDownload) {
                        downloadChapters(newChapters.first.sortedBy { it.chapter_number }
                            .map { it.toModel() })
                    }
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
            withContext(Dispatchers.IO) { updateChapters() }

            isLoading = false
            if (errorFromNetwork == null) {
                withContext(Dispatchers.Main) { controller.updateChapters(this@MangaDetailsPresenter.chapters) }
            } else {
                withContext(Dispatchers.Main) {
                    controller.showError(
                        trimException(errorFromNetwork!!)
                    )
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
            } ?: listOf()
            isLoading = false
            try {
                syncChaptersWithSource(db, chapters, manga)

                updateChapters()
                withContext(Dispatchers.Main) { controller.updateChapters(this@MangaDetailsPresenter.chapters) }
            } catch (e: java.lang.Exception) {
                withContext(Dispatchers.Main) {
                    controller.showError(trimException(e))
                }
            }
        }
    }

    private fun trimException(e: java.lang.Exception): String {
        return (if (e.message?.contains(": ") == true) e.message?.split(": ")?.drop(1)
            ?.joinToString(": ")
        else e.message) ?: preferences.context.getString(R.string.unknown_error)
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
        pagesLeft: Int? = null
    ) {
        scope.launch(Dispatchers.IO) {
            selectedChapters.forEach {
                it.read = read
                if (!read) {
                    it.last_page_read = lastRead ?: 0
                    it.pages_left = pagesLeft ?: 0
                }
            }
            db.updateChaptersProgress(selectedChapters).executeAsBlocking()
            if (read && deleteNow && preferences.removeAfterMarkedAsRead()) {
                deleteChapters(selectedChapters, false)
            }
            getChapters()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    /**
     * Sets the sorting order and requests an UI update.
     */
    fun setSortOrder(descend: Boolean) {
        manga.setChapterOrder(if (descend) Manga.SORT_DESC else Manga.SORT_ASC)
        asyncUpdateMangaAndChapters()
    }

    fun globalSort(): Boolean = preferences.chaptersDescAsDefault().getOrDefault()

    fun setGlobalChapterSort(descend: Boolean) {
        preferences.chaptersDescAsDefault().set(descend)
        manga.setSortToGlobal()
        asyncUpdateMangaAndChapters()
    }

    /**
     * Sets the sorting method and requests an UI update.
     */
    fun setSortMethod(bySource: Boolean) {
        manga.sorting = if (bySource) Manga.SORTING_SOURCE else Manga.SORTING_NUMBER
        asyncUpdateMangaAndChapters()
    }

    fun setScanlatorFilter(selectedScanlators: MutableList<String>?) {
        if (!selectedScanlators.isNullOrEmpty()) {
            chapters.filter {
                val scanlatorString = it.chapter.scanlator
                var found = false
                if (!scanlatorString.isNullOrBlank()) {
                    found = MdUtil.getScanlators(scanlatorString)
                        .any { group -> selectedScanlators.contains(group) }
                }
                found
            }.toList()
        }
    }

    /**
     * Removes all filters and requests an UI update.
     */
    fun setFilters(read: Boolean, unread: Boolean, downloaded: Boolean, bookmarked: Boolean) {
        manga.readFilter = when {
            read -> Manga.SHOW_READ
            unread -> Manga.SHOW_UNREAD
            else -> Manga.SHOW_ALL
        }
        manga.downloadedFilter = if (downloaded) Manga.SHOW_DOWNLOADED else Manga.SHOW_ALL
        manga.bookmarkedFilter = if (bookmarked) Manga.SHOW_BOOKMARKED else Manga.SHOW_ALL
        asyncUpdateMangaAndChapters()
    }

    private fun asyncUpdateMangaAndChapters(justChapters: Boolean = false) {
        scope.launch {
            if (!justChapters) {
                db.updateFlags(manga).executeOnIO()
                db.updateScanlatorFilterFlag(manga).executeOnIO()
            }
            updateChapters()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    fun currentFilters(): String {
        val filtersId = mutableListOf<Int?>()
        filtersId.add(if (onlyRead()) R.string.read else null)
        filtersId.add(if (onlyUnread()) R.string.unread else null)
        filtersId.add(if (onlyDownloaded()) R.string.downloaded else null)
        filtersId.add(if (onlyBookmarked()) R.string.bookmarked else null)
        filtersId.add(if (filteredScanlators.size != allChapterScanlators.size) R.string.scanlator_groups else null)
        return filtersId.filterNotNull().joinToString(", ") { preferences.context.getString(it) }
    }

    fun toggleFavorite(): Boolean {
        manga.favorite = !manga.favorite

        when (manga.favorite) {
            true -> manga.date_added = Date().time
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

    /**
     * Move the given manga to the category.
     *
     * @param manga the manga to move.
     * @param category the selected category, or null for default category.
     */
    fun moveMangaToCategory(category: Category?) {
        moveMangaToCategories(listOfNotNull(category))
    }

    /**
     * Move the given manga to categories.
     *
     * @param manga the manga to move.
     * @param categories the selected categories.
     */
    fun moveMangaToCategories(categories: List<Category>) {
        val mc = categories.filter { it.id != 0 }.map { MangaCategory.create(manga, it) }
        db.setMangaCategories(mc, listOf(manga))
    }

    /**
     * Gets the category id's the manga is in, if the manga is not in a category, returns the default id.
     *
     * @param manga the manga to get categories from.
     * @return Array of category ids the manga is in, if none returns default id
     */
    fun getMangaCategoryIds(): Array<Int> {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking()
        return categories.mapNotNull { it.id }.toTypedArray()
    }

    fun confirmDeletion() {
        coverCache.deleteFromCache(manga)
        db.resetMangaInfo(manga).executeAsBlocking()
        downloadManager.deleteManga(manga, source)
        customMangaManager.saveMangaInfo(CustomMangaManager.MangaJson(manga.id!!))
        asyncUpdateMangaAndChapters(true)
    }

    fun setFavorite(favorite: Boolean) {
        if (manga.favorite == favorite) {
            return
        }
        toggleFavorite()
    }

    override fun onUpdateManga(manga: LibraryManga) {
        if (manga.id == this.manga.id) {
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

    fun isTracked(): Boolean =
        loggedServices.any { service -> tracks.any { it.sync_id == service.id } }

    fun similarEnabled(): Boolean = preferences.similarEnabled()

    // Tracking
    private fun setTrackItems() {
        trackList = loggedServices.map { service ->
            TrackItem(tracks.find { it.sync_id == service.id }, service)
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

                            if (item.service.isMdList() && manga.status == SManga.COMPLETED && trackItem.total_chapters == 0) {
                                chapters.firstOrNull { it.name.contains("[END]") }?.let {
                                    trackItem.total_chapters = it.chapter.chapter_number.toInt()
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

    fun trackSearch(query: String, service: TrackService) {
        if (!controller.isNotOnline()) {
            scope.launch(Dispatchers.IO) {
                val results = try {
                    service.search(query)
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
                    trackError(e)
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
        if (item.service.isCompletedStatus(index) && track.total_chapters > 0)
            track.last_chapter_read = track.total_chapters
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

    fun similarToolTip() {
        if (similarEnabled() && !preferences.shownSimilarTutorial().get()) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    delay(1500)
                    withContext(Dispatchers.Main) {
                        controller.showSimilarToopTip()
                        preferences.shownSimilarTutorial().set(true)
                    }
                }
            }
        }
    }

    companion object {
        const val MULTIPLE_VOLUMES = 1
        const val TENS_OF_CHAPTERS = 2
        const val MULTIPLE_SEASONS = 3
    }
}
