package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaPresenter(private val controller: MangaChaptersController,
    val manga: Manga,
    val source: Source,
    val preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get()) {


    var isLockedFromSearch = false

    var chapters:List<ChapterItem> = emptyList()
        private set
    fun onCreate() {
        isLockedFromSearch = SecureActivityDelegate.shouldBeLocked()

        val chapters = db.getChapters(manga).executeAsBlocking().map { it.toModel() }


        // Store the last emission
        this.chapters = applyChapterFilters(chapters)

        // Find downloaded chapters
        setDownloadedChapters(chapters)

        controller.updateChapters(this.chapters)

        // Listen for download status changes
        //observeDownloads()

        // Emit the number of chapters to the info tab.
        //chapterCountRelay.call(chapters.maxBy { it.chapter_number }?.chapter_number ?: 0f)

        // Emit the upload date of the most recent chapter
        /*lastUpdateRelay.call(
                    Date(chapters.maxBy { it.date_upload }?.date_upload ?: 0)
                )*/

        /*       // Prepare the relay.
        chaptersRelay.flatMap { applyChapterFilters(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                ChaptersController::onNextChapters
            ) { _, error -> Timber.e(error) }

        // Add the subscription that retrieves the chapters from the database, keeps subscribed to
        // changes, and sends the list of chapters to the relay.
        add(db.getChapters(manga).asRxObservable()
            .map { chapters ->
                // Convert every chapter to a model.
                chapters.map { it.toModel() }
            }
            .doOnNext { chapters ->
                // Find downloaded chapters
                setDownloadedChapters(chapters)

                // Store the last emission
                this.chapters = chapters

                // Listen for download status changes
                observeDownloads()

                // Emit the number of chapters to the info tab.
                chapterCountRelay.call(chapters.maxBy { it.chapter_number }?.chapter_number
                    ?: 0f)

                // Emit the upload date of the most recent chapter
                lastUpdateRelay.call(
                    Date(chapters.maxBy { it.date_upload }?.date_upload
                        ?: 0)
                )

            }
            .subscribe { chaptersRelay.call(it) })*/
    }

    /*private fun observeDownloads() {
        observeDownloadsSubscription?.let { remove(it) }
        observeDownloadsSubscription = downloadManager.queue.getStatusObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .filter { download -> download.manga.id == manga.id }
            .doOnNext { onDownloadStatusChange(it) }
            .subscribeLatestCache(ChaptersController::onChapterStatusChange) {
                    _, error -> Timber.e(error)
            }
    }*/
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
}