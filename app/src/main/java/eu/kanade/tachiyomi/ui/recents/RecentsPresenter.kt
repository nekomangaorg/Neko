package eu.kanade.tachiyomi.ui.recents

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.executeOnIO
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RecentsPresenter(
    val controller: RecentsController,
    val preferences: PreferencesHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get()
) : DownloadQueue.DownloadListener, LibraryServiceListener {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    private var recentItems = listOf<RecentMangaItem>()
    var groupedRecentItems = listOf<RecentsItem>()
    var query = ""

    fun onCreate() {
        downloadManager.addListener(this)
        LibraryUpdateService.setListener(this)
        getRecents()
    }

    fun getRecents() {
        scope.launch {
            val cal = Calendar.getInstance()
            cal.time = Date()
            if (query.isNotEmpty()) cal.add(Calendar.YEAR, -50)
            else cal.add(Calendar.MONTH, -1)
            val cReading =
                if (query.isEmpty())
                    db.getRecentsWithUnread(cal.time, query).executeOnIO()
                else
                    db.getRecentMangaLimit(cal.time, 8, query).executeOnIO()
            val rUpdates = db.getUpdatedManga(cal.time, query).executeOnIO()
            rUpdates.forEach {
                it.history.last_read = it.chapter.date_upload
            }
            val mangaList = (cReading + rUpdates).sortedByDescending {
                it.history.last_read
            }.distinctBy {
                if (query.isEmpty()) it.manga.id else it.chapter.id }
            recentItems = mangaList.mapNotNull {
                val chapter = if (it.chapter.read) getNextChapter(it.manga)
                else it.chapter
                if (chapter == null) if (query.isNotEmpty()) RecentMangaItem(it, it.chapter)
                else null
                else RecentMangaItem(it, chapter)
            }
            setDownloadedChapters(recentItems)
            if (query.isEmpty()) {
                val nChaptersItems = RecentsItem(
                    RecentsItem.NEW_CHAPTERS,
                    recentItems.filter { it.mch.history.id == null }.take(4)
                )
                val cReadingItems = RecentsItem(
                    RecentsItem.CONTINUE_READING,
                    recentItems.filter { it.mch.history.id != null }.take(
                        8 - nChaptersItems.mangaList.size
                    )
                )
                // TODO: Add Date Added
                groupedRecentItems = listOf(cReadingItems, nChaptersItems).sortedByDescending {
                    it.mangaList.firstOrNull()?.mch?.history?.last_read ?: 0
                }
            } else {
                groupedRecentItems = listOf(RecentsItem(RecentsItem.SEARCH, recentItems))
            }
            withContext(Dispatchers.Main) { controller.showLists(groupedRecentItems) }
        }
    }

    private fun getNextChapter(manga: Manga): Chapter? {
        val chapters = db.getChapters(manga).executeAsBlocking()
        return chapters.sortedByDescending { it.source_order }.find { !it.read }
    }

    fun onDestroy() {
        downloadManager.removeListener(this)
        LibraryUpdateService.removeListener(this)
    }

    fun cancelScope() {
        scope.cancel()
    }

    /**
     * Finds and assigns the list of downloaded chapters.
     *
     * @param chapters the list of chapter from the database.
     */
    private fun setDownloadedChapters(chapters: List<RecentMangaItem>) {
        for (item in chapters) {
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
            controller.updateChapterDownload(download)
        }
    }

    override fun onUpdateManga(manga: LibraryManga) {
        getRecents()
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

            controller.showLists(groupedRecentItems)
        }
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
}
