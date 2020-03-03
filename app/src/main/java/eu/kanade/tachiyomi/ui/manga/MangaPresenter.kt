package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.coroutines.CoroutineContext

class MangaPresenter(private val controller: MangaChaptersController,
    val manga: Manga,
    val source: Source,
    val preferences: PreferencesHelper = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get()):
    CoroutineScope,
    DownloadQueue.DownloadListener {

    override var coroutineContext:CoroutineContext = Job() + Dispatchers.Default

    var isLockedFromSearch = false
    var hasRequested = false

    var chapters:List<ChapterItem> = emptyList()
        private set

    fun onCreate() {
        isLockedFromSearch = SecureActivityDelegate.shouldBeLocked()
        downloadManager.addListener(this)
        if (!manga.initialized)
            fetchMangaFromSource()
        updateChapters()
        controller.updateChapters(this.chapters)
    }

    fun onDestroy() {
        downloadManager.removeListener(this)
    }

    fun fetchMangaFromSource() {
        GlobalScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                controller.setRefresh(true)
            }
            val thumbnailUrl = manga.thumbnail_url
            val networkManga = try {
                source.fetchMangaDetails(manga).toBlocking().single()
            } catch (e: java.lang.Exception) {
                controller.showError(trimException(e))
                return@launch
            }
            if (networkManga != null) {
                manga.copyFrom(networkManga)
                manga.initialized = true
                db.insertManga(manga).executeAsBlocking()
                if (thumbnailUrl != networkManga.thumbnail_url)
                    MangaImpl.setLastCoverFetch(manga.id!!, Date().time)
                withContext(Dispatchers.Main) {
                    controller.updateHeader()
                }
            }
        }
    }

    fun fetchChapters() {
        launch {
            getChapters()
            withContext(Dispatchers.Main) { controller.updateChapters(chapters) }
        }
    }

    private suspend fun getChapters() {
        val chapters = withContext(Dispatchers.IO) {
            db.getChapters(manga).executeAsBlocking().map { it.toModel() }
        }
        // Store the last emission
        this.chapters = applyChapterFilters(chapters)

        // Find downloaded chapters
        setDownloadedChapters(chapters)
    }

    private fun updateChapters(fetchedChapters: List<Chapter>? = null) {
        val chapters = (fetchedChapters ?:
        db.getChapters(manga).executeAsBlocking()).map { it.toModel() }

        // Store the last emission
        this.chapters = applyChapterFilters(chapters)

        // Find downloaded chapters
        setDownloadedChapters(chapters)
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
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Int) {
        manga.displayMode = mode
        db.updateFlags(manga).executeAsBlocking()
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
            else -> throw NotImplementedError("Unimplemented sorting method")
        }
        chapters = chapters.sortedWith(Comparator(sortFunction))
        //if (sortDescending())
           // chapters = chapters.reversed()
        return chapters
    }

    /**
     * Returns the next unread chapter or null if everything is read.
     */
    fun getNextUnreadChapter(): ChapterItem? {
        return chapters.sortedByDescending { it.source_order }.find { !it.read }
    }

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
            if (chapterError == null)
                withContext(Dispatchers.Main) { controller.updateChapters(this@MangaPresenter.chapters) }
            if (mangaError != null)
                withContext(Dispatchers.Main) { controller.showError(trimException(mangaError!!)) }
        }
    }

    /**
     * Requests an updated list of chapters from the source.
     */
    fun fetchChaptersFromSource() {
        hasRequested = true

        launch(Dispatchers.IO) {
            val chapters = try {
                source.fetchChapterList(manga).toBlocking().single()
            }
            catch(e: Exception) {
                withContext(Dispatchers.Main) { controller.showError(trimException(e)) }
                return@launch
            } ?: listOf()
            try {
                syncChaptersWithSource(db, chapters, manga, source)

                updateChapters()
                withContext(Dispatchers.Main) { controller.updateChapters(this@MangaPresenter.chapters) }
            }
            catch(e: java.lang.Exception) {
                controller.showError(trimException(e))
            }
        }
    }

    private fun trimException(e: java.lang.Exception): String {
        return e.message?.split(": ")?.drop(1)?.joinToString(": ") ?: "Error"
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
     * Reverses the sorting and requests an UI update.
     */
    fun setSortOrder(desend: Boolean) {
        manga.setChapterOrder(if (desend) Manga.SORT_ASC else Manga.SORT_DESC)
        db.updateFlags(manga).executeAsBlocking()
        updateChapters()
        controller.updateChapters(chapters)
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
    }

    fun setFavorite(favorite: Boolean) {
        if (manga.favorite == favorite) {
            return
        }
        toggleFavorite()
    }
}