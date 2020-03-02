package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class MangaPresenter(private val controller: MangaChaptersController,
    val manga: Manga,
    val source: Source,
    val preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get()) {


    var isLockedFromSearch = false
    var hasRequested = false

    var chapters:List<ChapterItem> = emptyList()
        private set
    fun onCreate() {
        isLockedFromSearch = SecureActivityDelegate.shouldBeLocked()
        if (!manga.initialized)
            fetchMangaFromSource()
        updateChapters()
        controller.updateChapters(this.chapters)
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

    private fun updateChapters(fetchedChapters: List<Chapter>? = null) {
        val chapters = (fetchedChapters ?:
        db.getChapters(manga).executeAsBlocking()).map { it.toModel() }

        // Store the last emission
        this.chapters = applyChapterFilters(chapters)

        // Find downloaded chapters
        setDownloadedChapters(chapters)

        /*
                        // Emit the number of chapters to the info tab.
                        chapterCountRelay.call(chapters.maxBy { it.chapter_number }?.chapter_number
                            ?: 0f)

                        // Emit the upload date of the most recent chapter
                        lastUpdateRelay.call(
                            Date(chapters.maxBy { it.date_upload }?.date_upload
                                ?: 0)
                        )*/
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

        setDownloadedChapters(chapters)

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
        fetchMangaFromSource()
        fetchChaptersFromSource()
    }

    /**
     * Requests an updated list of chapters from the source.
     */
    fun fetchChaptersFromSource() {
        hasRequested = true

        GlobalScope.launch(Dispatchers.IO) {
            val chapters = try {
                source.fetchChapterList(manga).toBlocking().single()
            }
            catch(e: Exception) {
                controller.showError(trimException(e))
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
}