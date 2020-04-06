package eu.kanade.tachiyomi.ui.recents

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class RecentsPresenter(
    val controller: RecentsController,
    val preferences: PreferencesHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get()
) : DownloadQueue.DownloadListener, LibraryServiceListener {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    private var recentItems = listOf<RecentMangaItem>()
    var query = ""
    private val newAdditionsHeader = RecentMangaHeaderItem(RecentMangaHeaderItem.NEWLY_ADDED)
    private val newChaptersHeader = RecentMangaHeaderItem(RecentMangaHeaderItem.NEW_CHAPTERS)
    private val continueReadingHeader = RecentMangaHeaderItem(RecentMangaHeaderItem
        .CONTINUE_READING)

    fun onCreate() {
        downloadManager.addListener(this)
        LibraryUpdateService.setListener(this)
        getRecents()
    }

    fun getRecents() {
        val oldQuery = query
        scope.launch {
            val cal = Calendar.getInstance()
            cal.time = Date()
            if (query.isNotEmpty()) cal.add(Calendar.YEAR, -50)
            else cal.add(Calendar.MONTH, -1)

            val calWeek = Calendar.getInstance()
            calWeek.time = Date()
            if (query.isNotEmpty()) calWeek.add(Calendar.YEAR, -50)
            else calWeek.add(Calendar.DAY_OF_YEAR, -1)

            val cReading =
                if (query.isEmpty()) db.getRecentsWithUnread(cal.time, query).executeOnIO()
                else db.getRecentMangaLimit(cal.time, 8, query).executeOnIO()
            val rUpdates = db.getUpdatedManga(cal.time, query).executeOnIO()
            rUpdates.forEach {
                it.history.last_read = it.chapter.date_fetch
            }
            val nAdditions = db.getRecentlyAdded(calWeek.time, query).executeOnIO()
            nAdditions.forEach {
                it.history.last_read = it.manga.date_added
            }
            if (query != oldQuery) return@launch
            val mangaList = (cReading + rUpdates + nAdditions).sortedByDescending {
                it.history.last_read
            }.distinctBy {
                if (query.isEmpty()) it.manga.id else it.chapter.id
            }
            val pairs = mangaList.mapNotNull {
                val chapter = if (it.chapter.read || it.chapter.id == null) getNextChapter(it.manga)
                    else if (it.history.id == null) getFirstUpdatedChapter(it.manga, it.chapter)
                    else it.chapter
                if (chapter == null) if (query.isNotEmpty() && it.chapter.id != null) Pair(
                    it, it.chapter
                )
                else null
                else Pair(it, chapter)
            }
            if (query.isEmpty()) {
                val nChaptersItems =
                    pairs.filter { it.first.history.id == null && it.first.chapter.id != null }
                        .sortedWith(Comparator<Pair<MangaChapterHistory, Chapter>> { f1, f2 ->
                            if (abs(f1.second.date_fetch - f2.second.date_fetch) <=
                                TimeUnit.HOURS.toMillis(12))
                                f2.second.date_upload.compareTo(f1.second.date_upload)
                            else
                                f2.second.date_fetch.compareTo(f1.second.date_fetch)
                        })
                        .take(4).map {
                            RecentMangaItem(
                                it.first,
                                it.second,
                                newChaptersHeader
                            )
                        } +
                        RecentMangaItem(header = newChaptersHeader)
                val cReadingItems =
                    pairs.filter { it.first.history.id != null }.take(9 - nChaptersItems.size).map {
                            RecentMangaItem(
                                it.first,
                                it.second,
                                continueReadingHeader
                            )
                        } + RecentMangaItem(header = continueReadingHeader)
                val nAdditionsItems = pairs.filter { it.first.chapter.id == null }.take(4)
                    .map { RecentMangaItem(it.first, it.second, newAdditionsHeader) }
                recentItems =
                    listOf(nChaptersItems, cReadingItems, nAdditionsItems).sortedByDescending {
                            it.firstOrNull()?.mch?.history?.last_read ?: 0L
                        }.flatten()
            } else {
                recentItems = pairs.map { RecentMangaItem(it.first, it.second, null) }
            }
            setDownloadedChapters(recentItems)
            withContext(Dispatchers.Main) { controller.showLists(recentItems) }
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
        if (manga.id == null) scope.launch(Dispatchers.Main) { controller.reEnableSwipe() }
        else getRecents()
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

            controller.showLists(recentItems)
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
