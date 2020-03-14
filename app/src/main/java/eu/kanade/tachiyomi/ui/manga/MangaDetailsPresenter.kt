package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Date
import kotlin.coroutines.CoroutineContext

class MangaDetailsPresenter(private val controller: MangaDetailsController,
    val manga: Manga,
    val source: Source,
    val preferences: PreferencesHelper = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get()):
    CoroutineScope,
    DownloadQueue.DownloadListener,
    LibraryServiceListener   {

    override var coroutineContext:CoroutineContext = Job() + Dispatchers.Default

    var isLockedFromSearch = false
    var hasRequested = false
    var isLoading = false

    private val loggedServices by lazy { Injekt.get<TrackManager>().services.filter { it.isLogged } }
    var tracks = emptyList<Track>()

    var trackList: List<TrackItem> = emptyList()

    var chapters:List<ChapterItem> = emptyList()
        private set

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
        }
        else {
            updateChapters()
            controller.updateChapters(this.chapters)
        }
        fetchTrackings()
    }

    fun onDestroy() {
        downloadManager.removeListener(this)
        LibraryUpdateService.removeListener(this)
    }

    fun fetchChapters() {
        launch {
            getChapters()
            refreshTracking()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    private suspend fun getChapters() {
        val chapters = withContext(Dispatchers.IO) {
            db.getChapters(manga).executeAsBlocking().map { it.toModel() }
        }

        // Find downloaded chapters
        setDownloadedChapters(chapters)

        // Store the last emission
        this.chapters = applyChapterFilters(chapters)

    }

    private fun updateChapters(fetchedChapters: List<Chapter>? = null) {
        val chapters = (fetchedChapters ?:
        db.getChapters(manga).executeAsBlocking()).map { it.toModel() }

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
            }
        }
    }

    override fun updateDownload(download: Download) {
        chapters.find { it.id == download.chapter.id }?.download = download
        launch(Dispatchers.Main) {
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
     * Sets the sorting method and requests an UI update.
     * @param sort the sorting mode.
     */
    fun setSorting(sort: Int) {
        manga.sorting = sort
        db.updateFlags(manga).executeAsBlocking()
       // refreshChapters()
    }

    /**
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyDownloaded(): Boolean {
        return manga.downloadedFilter == Manga.SHOW_DOWNLOADED
    }

    /**
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyBookmarked(): Boolean {
        return manga.bookmarkedFilter == Manga.SHOW_BOOKMARKED
    }

    /**
     * Whether the display only unread filter is enabled.
     */
    fun onlyUnread(): Boolean {
        return manga.readFilter == Manga.SHOW_UNREAD
    }

    /**
     * Whether the display only read filter is enabled.
     */
    fun onlyRead(): Boolean {
        return manga.readFilter == Manga.SHOW_READ
    }

    /**
     * Whether the sorting method is descending or ascending.
     */
    fun sortDescending(): Boolean {
        return manga.sortDescending()
    }

    /**
     * Applies the view filters to the list of chapters obtained from the database.
     * @param chapterList the list of chapters from the database
     * @return an observable of the list of chapters filtered and sorted.
     */
    private fun applyChapterFilters(chapterList: List<ChapterItem>): List<ChapterItem> {
        var chapters = chapterList
        if (onlyUnread()) {
            chapters = chapters.filter { !it.read }
        } else if (onlyRead()) {
            chapters = chapters.filter { it.read }
        }
        if (onlyDownloaded()) {
            chapters = chapters.filter { it.isDownloaded || it.manga.source == LocalSource.ID }
        }
        if (onlyBookmarked()) {
            chapters = chapters.filter { it.bookmark }
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
        return chapters
    }

    /**
     * Returns the next unread chapter or null if everything is read.
     */
    fun getNextUnreadChapter(): ChapterItem? {
        return chapters.sortedByDescending { it.source_order }.find { !it.read }
    }

    fun allUnread(): Boolean = chapters.none { it.read }

    fun getUnreadChaptersSorted() = chapters
        .filter { !it.read && it.status == Download.NOT_DOWNLOADED }
        .distinctBy { it.name }
        .sortedByDescending { it.source_order }

    /**
     * Returns the next unread chapter or null if everything is read.
     */
    fun getNewestChapterTime(): Long? {
        return chapters.maxBy { it.date_upload }?.date_upload
    }

    fun getLatestChapter(): Float? {
        return chapters.maxBy { it.chapter_number }?.chapter_number
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param chapters the list of chapters to download.
     */
    fun downloadChapters(chapters: List<ChapterItem>) {
        downloadManager.downloadChapters(manga, chapters)
    }

    fun restartDownloads() {
        if (downloadManager.isPaused())
            downloadManager.startDownloads()
    }

    /**
     * Deletes the given list of chapter.
     * @param chapters the list of chapters to delete.
     */
    fun deleteChapters(chapters: List<ChapterItem>) {
        deleteChaptersInternal(chapters)

        chapters.forEach { chapter ->
            this.chapters.find { it.id == chapter.id }?.download?.status = Download.NOT_DOWNLOADED
        }

        controller.updateChapters(this.chapters)
         //  if (onlyDownloaded()) refreshChapters() }
    }

    /**
     * Deletes a list of chapters from disk. This method is called in a background thread.
     * @param chapters the chapters to delete.
     */
    private fun deleteChaptersInternal(chapters: List<ChapterItem>) {
        downloadManager.deleteChapters(chapters, manga, source)
        chapters.forEach {
            it.status = Download.NOT_DOWNLOADED
            it.download = null
        }
    }

    fun refreshAll() {
        launch {
            isLoading = true
            var mangaError: java.lang.Exception? = null
            var chapterError: java.lang.Exception? = null
            val chapters = async(Dispatchers.IO) {
                try {
                    source.fetchChapterList(manga).toBlocking().single()

                } catch (e: Exception) {
                    chapterError = e
                    emptyList<SChapter>()
                } ?: emptyList()
            }
            val thumbnailUrl = manga.thumbnail_url
            val nManga = async(Dispatchers.IO) {
                try {
                    source.fetchMangaDetails(manga).toBlocking().single()
                } catch (e: java.lang.Exception) {
                    mangaError = e
                    null
                }
            }

            val networkManga = nManga.await()
            if (networkManga != null) {
                manga.copyFrom(networkManga)
                manga.initialized = true
                db.insertManga(manga).executeAsBlocking()
                if (thumbnailUrl != networkManga.thumbnail_url)
                    MangaImpl.setLastCoverFetch(manga.id!!, Date().time)
            }
            val finChapters = chapters.await()
            if (finChapters.isNotEmpty()) {
                syncChaptersWithSource(db, finChapters, manga, source)
                withContext(Dispatchers.IO) {  updateChapters() }
            }
            isLoading = false
            if (chapterError == null)
                withContext(Dispatchers.Main) { controller.updateChapters(this@MangaDetailsPresenter.chapters) }
            if (mangaError != null)
                withContext(Dispatchers.Main) { controller.showError(trimException(mangaError!!)) }
        }
    }

    /**
     * Requests an updated list of chapters from the source.
     */
    fun fetchChaptersFromSource() {
        hasRequested = true
        isLoading = true

        launch(Dispatchers.IO) {
            val chapters = try {
                source.fetchChapterList(manga).toBlocking().single()
            }
            catch(e: Exception) {
                withContext(Dispatchers.Main) { controller.showError(trimException(e)) }
                return@launch
            } ?: listOf()
            isLoading = false
            try {
                syncChaptersWithSource(db, chapters, manga, source)

                updateChapters()
                withContext(Dispatchers.Main) { controller.updateChapters(this@MangaDetailsPresenter.chapters) }
            }
            catch(e: java.lang.Exception) {
                controller.showError(trimException(e))
            }
        }
    }

    private fun trimException(e: java.lang.Exception): String {
        return (if (e.message?.contains(": ") == true)
            e.message?.split(": ")?.drop(1)?.joinToString(": ")
        else e.message) ?: preferences.context.getString(R.string.unknown_error)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param selectedChapters the list of chapters to bookmark.
     */
    fun bookmarkChapters(selectedChapters: List<ChapterItem>, bookmarked: Boolean) {
        launch(Dispatchers.IO) {
            selectedChapters.forEach {
                it.bookmark = bookmarked
            }
            db.updateChaptersProgress(selectedChapters).executeAsBlocking()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    /**
     * Mark the selected chapter list as read/unread.
     * @param selectedChapters the list of selected chapters.
     * @param read whether to mark chapters as read or unread.
     */
    fun markChaptersRead(selectedChapters: List<ChapterItem>, read: Boolean) {
        launch(Dispatchers.IO) {
            selectedChapters.forEach {
                it.read = read
                if (!read) {
                    it.last_page_read = 0
                    it.pages_left = 0
                }
            }
            db.updateChaptersProgress(selectedChapters).executeAsBlocking()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    /**
     * Sets the sorting order and requests an UI update.
     */
    fun setSortOrder(desend: Boolean) {
        manga.setChapterOrder(if (desend) Manga.SORT_ASC else Manga.SORT_DESC)
        asyncUpdateMangaAndChapters()
    }

    /**
     * Sets the sorting method and requests an UI update.
     */
    fun setSortMethod(bySource: Boolean) {
        manga.sorting = if (bySource) Manga.SORTING_SOURCE else Manga.SORTING_NUMBER
        asyncUpdateMangaAndChapters()
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

    private fun asyncUpdateMangaAndChapters(justChapters:Boolean = false) {
        launch {
            if (!justChapters)
                withContext(Dispatchers.IO) { db.updateFlags(manga).executeAsBlocking() }
            updateChapters()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    fun currentFilters(): String {
        val filtersId = mutableListOf<Int?>()
        filtersId.add(if (onlyRead()) R.string.action_filter_read else null)
        filtersId.add(if (onlyUnread()) R.string.action_filter_unread else null)
        filtersId.add(if (onlyDownloaded()) R.string.action_filter_downloaded else null)
        filtersId.add(if (onlyBookmarked()) R.string.action_filter_bookmarked else null)
        return filtersId.filterNotNull().joinToString(", ") { preferences.context.getString(it) }
    }

    fun toggleFavorite(): Boolean {
        manga.favorite = !manga.favorite
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
    fun moveMangaToCategory(manga: Manga, category: Category?) {
        moveMangaToCategories(manga, listOfNotNull(category))
    }

    /**
     * Move the given manga to categories.
     *
     * @param manga the manga to move.
     * @param categories the selected categories.
     */
    fun moveMangaToCategories(manga: Manga, categories: List<Category>) {
        val mc = categories.filter { it.id != 0 }.map { MangaCategory.create(manga, it) }
        db.setMangaCategories(mc, listOf(manga))
    }

    /**
     * Gets the category id's the manga is in, if the manga is not in a category, returns the default id.
     *
     * @param manga the manga to get categories from.
     * @return Array of category ids the manga is in, if none returns default id
     */
    fun getMangaCategoryIds(manga: Manga): Array<Int> {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking()
        return categories.mapNotNull { it.id }.toTypedArray()
    }

    fun confirmDeletion() {
        coverCache.deleteFromCache(manga.thumbnail_url)
        db.resetMangaInfo(manga).executeAsBlocking()
        downloadManager.deleteManga(manga, source)
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

        launch(Dispatchers.IO) {
            destDir.deleteRecursively()
            try {
                val image = saveImage(cover, destDir, manga)
                if (image != null)
                    controller.shareManga(image)
                else controller.shareManga()
            }
            catch (e:java.lang.Exception) { }
        }
    }

    private fun saveImage(cover:Bitmap, directory: File, manga: Manga): File? {
        directory.mkdirs()

        // Build destination file.
        val filename = DiskUtil.buildValidFilename("${manga.originalTitle()} - Cover.jpg")

        val destFile = File(directory, filename)
        val stream: OutputStream = FileOutputStream(destFile)
        cover.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        stream.flush()
        stream.close()
        return destFile
    }

    fun updateManga(title:String?, author:String?, artist: String?, uri: Uri?,
        description: String?, tags: Array<String>?) {
        if (manga.source == LocalSource.ID) {
            manga.title = if (title.isNullOrBlank()) manga.url else title.trim()
            manga.author = author?.trim()
            manga.artist = artist?.trim()
            manga.description = description?.trim()
            val tagsString = tags?.joinToString(", ") { it.capitalize() }
            manga.genre = if (tags.isNullOrEmpty()) null else tagsString?.trim()
            LocalSource(downloadManager.context).updateMangaInfo(manga)
            db.updateMangaInfo(manga).executeAsBlocking()
        }
        else {
            var changed = false
            val title = title?.trim()
            if (!title.isNullOrBlank() && manga.originalTitle().isBlank()) {
                manga.title = title
                changed = true
            }
            else if (title.isNullOrBlank() && manga.currentTitle() != manga.originalTitle()) {
                manga.title = manga.originalTitle()
                changed = true
            } else if (!title.isNullOrBlank() && title != manga.currentTitle()) {
                manga.title = "${title}${SManga.splitter}${manga.originalTitle()}"
                changed = true
            }

            val author = author?.trim()
            if (author.isNullOrBlank() && manga.currentAuthor() != manga.originalAuthor()) {
                manga.author = manga.originalAuthor()
                changed = true
            } else if (!author.isNullOrBlank() && author != manga.currentAuthor()) {
                manga.author = "${author}${SManga.splitter}${manga.originalAuthor() ?: ""}"
                changed = true
            }

            val artist = artist?.trim()
            if (artist.isNullOrBlank() && manga.currentArtist() != manga.originalArtist()) {
                manga.artist = manga.originalArtist()
                changed = true
            } else if (!artist.isNullOrBlank() && artist != manga.currentArtist()) {
                manga.artist = "${artist}${SManga.splitter}${manga.originalArtist() ?: ""}"
                changed = true
            }

            val description = description?.trim()
            if (description.isNullOrBlank() && manga.currentDesc() != manga.originalDesc()) {
                manga.description = manga.originalDesc()
                changed = true
            } else if (!description.isNullOrBlank() && description != manga.currentDesc()) {
                manga.description = "${description}${SManga.splitter}${manga.originalDesc() ?: ""}"
                changed = true
            }

            var tagsString = tags?.joinToString(", ")
            if ((tagsString.isNullOrBlank() && manga.currentGenres() != manga.originalGenres())
                || tagsString == manga.originalGenres()) {
                manga.genre = manga.originalGenres()
                changed = true
            } else if (!tagsString.isNullOrBlank() && tagsString != manga.currentGenres()) {
                tagsString = tags?.joinToString(", ") { it.capitalize() }
                manga.genre = "${tagsString}${SManga.splitter}${manga.originalGenres() ?: ""}"
                changed = true
            }
            if (changed) db.updateMangaInfo(manga).executeAsBlocking()
        }
        if (uri != null) editCoverWithStream(uri)
        controller.updateHeader()
    }

    private fun editCoverWithStream(uri: Uri): Boolean {
        val inputStream = downloadManager.context.contentResolver.openInputStream(uri) ?:
        return false
        if (manga.source == LocalSource.ID) {
            LocalSource.updateCover(downloadManager.context, manga, inputStream)
            return true
        }

        if (manga.thumbnail_url != null && manga.favorite) {
            Injekt.get<PreferencesHelper>().refreshCoversToo().set(false)
            coverCache.copyToCache(manga.thumbnail_url!!, inputStream)
            MangaImpl.setLastCoverFetch(manga.id!!, Date().time)
            return true
        }
        return false
    }

    fun isTracked(): Boolean = loggedServices.any { service -> tracks.any { it.sync_id == service.id } }

    fun hasTrackers(): Boolean = loggedServices.isNotEmpty()


    // Tracking

    private fun fetchTrackings() {
        launch {
            trackList = loggedServices.map { service ->
                TrackItem(tracks.find { it.sync_id == service.id }, service)
            }
        }
    }

    private suspend fun refreshTracking() {
        tracks = withContext(Dispatchers.IO) { db.getTracks(manga).executeAsBlocking() }
        trackList = loggedServices.map { service ->
            TrackItem(tracks.find { it.sync_id == service.id }, service)
        }
        withContext(Dispatchers.Main) { controller.refreshTracking(trackList) }
    }

    fun refreshTrackers() {
        launch {
            val list = trackList.filter { it.track != null }.map { item ->
                withContext(Dispatchers.IO) {
                    val trackItem = try {
                        item.service.refresh(item.track!!).toBlocking().single()
                    } catch (e: Exception) {
                        trackError(e)
                        null
                    }
                    if (trackItem != null) {
                        db.insertTrack(trackItem).executeAsBlocking()
                        trackItem
                    }
                    else
                        item.track
                }
            }
            refreshTracking()
        }
    }

    fun trackSearch(query: String, service: TrackService) {
        launch(Dispatchers.IO) {
            val results = try {service.search(query).toBlocking().single() }
            catch (e: Exception) {
                withContext(Dispatchers.Main) { controller.trackSearchError(e) }
                null }
             if (!results.isNullOrEmpty()) {
                 withContext(Dispatchers.Main) { controller.onTrackSearchResults(results) }
             }
        }
    }

    fun registerTracking(item: Track?, service: TrackService) {
        if (item != null) {
            item.manga_id = manga.id!!

            launch {
                val binding =  try { service.bind(item).toBlocking().single() }
                catch (e: Exception) {
                    trackError(e)
                    null
                }
                withContext(Dispatchers.IO) {
                    if (binding != null) db.insertTrack(binding).executeAsBlocking() }
                refreshTracking()
            }
        } else {
            launch {
                withContext(Dispatchers.IO) { db.deleteTrackForManga(manga, service)
                    .executeAsBlocking() }
                refreshTracking()
            }
        }
    }

    private fun updateRemote(track: Track, service: TrackService) {
        launch {
            val binding = try { service.update(track).toBlocking().single() }
            catch (e: Exception) {
                trackError(e)
                null
            }
            if (binding != null) {
                withContext(Dispatchers.IO) { db.insertTrack(binding).executeAsBlocking() }
                refreshTracking()
            }
            else trackRefreshDone()
        }
    }

    private suspend fun trackRefreshDone() {
        async(Dispatchers.Main) { controller.trackRefreshDone() }
    }

    private suspend fun trackError(error: Exception) {
        async(Dispatchers.Main) { controller.trackRefreshError(error) }
    }

    fun setStatus(item: TrackItem, index: Int) {
        val track = item.track!!
        track.status = item.service.getStatusList()[index]
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
}