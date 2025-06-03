package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.system.NetworkState
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.networkStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import org.nekomanga.R
import org.nekomanga.domain.download.DownloadItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to manage chapter downloads in the application. It must be instantiated once
 * and retrieved through dependency injection. You can use this class to queue new chapters or query
 * downloaded chapters.
 *
 * @param context the application context.
 */
class DownloadManager(val context: Context) {

    val isRunning: Boolean
        get() = downloader.isRunning

    val isPaused: Boolean
        get() = downloader.isPaused

    /** The sources manager. */
    private val sourceManager by injectLazy<SourceManager>()

    private val db = Injekt.get<DatabaseHelper>()

    /**
     * Downloads provider, used to retrieve the folders where the chapters are or should be stored.
     */
    private val provider = DownloadProvider(context)

    /** Cache of downloaded chapters. */
    private val cache = DownloadCache(context, provider, sourceManager)

    /** Downloader whose only task is to download chapters. */
    private val downloader = Downloader(context, provider, cache, sourceManager)

    /** Queue to delay the deletion of a list of chapters until triggered. */
    private val pendingDeleter = DownloadPendingDeleter(context)

    val queueState
        get() = downloader.queueState

    // For use by DownloadJob only
    fun downloaderStart() = downloader.start()

    fun downloaderStop(reason: String? = null) = downloader.stop(reason)

    val isDownloaderRunning
        get() = DownloadJob.isRunningFlow(context)

    fun isDownloadRunningTemp() = downloader.isRunning

    /**
     * Tells the downloader to begin downloads.
     *
     * @return true if it's started, false otherwise (empty queue).
     */
    fun startDownloads() {
        if (downloader.isRunning) return
        if (DownloadJob.isRunning(context)) {
            downloader.start()
        } else {
            DownloadJob.start(context)
        }
    }

    /**
     * Tells the downloader to stop downloads.
     *
     * @param reason an optional reason for being stopped, used to notify the user.
     */
    fun stopDownloads(reason: String? = null) {
        downloader.stop(reason)
    }

    /** Tells the downloader to pause downloads. */
    fun pauseDownloads() {
        downloader.pause()
        downloader.stop()
    }

    /**
     * Empties the download queue.
     *
     * @param isNotification value that determines if status is set (needed for view updates)
     */
    fun clearQueue() {
        deletePendingDownloads(queueState.value)
        downloader.clearQueue()
        downloader.stop()
    }

    /**
     * Returns the download from queue if the chapter is queued for download else it will return
     * null which means that the chapter is not queued for download
     *
     * @param chapterId the chapter to check.
     */
    fun getQueuedDownloadOrNull(chapterId: Long): Download? {
        return queueState.value.find { it.chapterItem.id == chapterId }
    }

    fun startDownloadNow(chapter: Chapter) {
        chapter.id ?: return
        val existingDownload = getQueuedDownloadOrNull(chapter.id!!)
        // If not in queue try to start a new download
        val toAdd =
            existingDownload ?: runBlocking { Download.fromChapterId(chapter.id!!) } ?: return
        queueState.value.toMutableList().apply {
            existingDownload?.let { remove(it) }
            add(0, toAdd)
            reorderQueue(this)
        }
        startDownloads()
    }

    /**
     * Reorders the download queue.
     *
     * @param downloads value to set the download queue to
     */
    fun reorderQueue(downloads: List<Download>) {
        downloader.updateQueue(downloads)
    }

    /**
     * Tells the downloader to enqueue the given list of chapters.
     *
     * @param manga the manga of the chapters.
     * @param chapters the list of chapters to enqueue.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    fun downloadChapters(manga: Manga, chapters: List<Chapter>, autoStart: Boolean = true) {
        downloader.queueChapters(manga, chapters, autoStart)
    }

    /**
     * Tells the downloader to enqueue the given list of downloads at the start of the queue.
     *
     * @param downloads the list of downloads to enqueue.
     */
    fun addDownloadsToStartOfQueue(downloads: List<Download>) {
        if (downloads.isEmpty()) return
        queueState.value.toMutableList().apply {
            addAll(0, downloads)
            reorderQueue(this)
        }
        startDownloads()
    }

    /**
     * Builds the page list of a downloaded chapter.
     *
     * @param manga the manga of the chapter.
     * @param chapter the downloaded chapter.
     * @return the list of pages from the chapter.
     */
    fun buildPageList(manga: Manga, chapter: Chapter): List<Page> {
        val chapterDir = provider.findChapterDir(chapter, manga)
        val files = chapterDir?.listFiles().orEmpty().filter { "image" in it.type.orEmpty() }

        if (files.isEmpty()) {
            throw Exception(context.getString(R.string.no_pages_found))
        }

        return files
            .sortedBy { it.name }
            .mapIndexed { i, file -> Page(i, uri = file.uri).apply { status = Page.State.READY } }
    }

    /**
     * Returns true if the chapter is downloaded.
     *
     * @param chapter the chapter to check.
     * @param manga the manga of the chapter.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isChapterDownloaded(chapter: Chapter, manga: Manga, skipCache: Boolean = false): Boolean {
        return cache.isChapterDownloaded(chapter, manga, skipCache)
    }

    fun downloadedChapterName(chapter: Chapter, manga: Manga, skipCache: Boolean = false): String {
        return cache.findChapterDirName(chapter, manga)
    }

    /**
     * Returns the amount of downloaded chapters for a manga.
     *
     * @param manga the manga to check.
     */
    fun getDownloadCount(manga: Manga): Int {
        return cache.getDownloadCount(manga)
    }

    /** Returns the list of downloaded file names */
    fun getAllDownloads(manga: Manga): List<String> {
        return cache.getAllDownloads(manga)
    }

    /**
     * Calls delete chapter, which deletes temp downloads
     *
     * @param downloads list of downloads to cancel
     */
    fun deletePendingDownloads(downloads: List<Download>) {
        val downloadsByManga = downloads.groupBy { it.chapterItem.mangaId }
        downloadsByManga.forEach { entry ->
            val manga = entry.value.first().mangaItem
            val dbManga = db.getManga(manga.id).executeAsBlocking() ?: return
            deleteChapters(entry.value.map { it.chapterItem.toDbChapter() }, dbManga)
        }
    }

    /**
     * Calls delete chapter, which deletes temp downloads
     *
     * @param downloads list of downloads to cancel
     */
    fun deletePendingDownloadsItems(downloads: List<DownloadItem>) {
        val downloadsByManga = downloads.groupBy { it.mangaItem.id }
        downloadsByManga.forEach { entry ->
            val manga = entry.value.first().mangaItem
            val dbManga = db.getManga(manga.id).executeAsBlocking() ?: return
            deleteChapters(entry.value.map { it.chapterItem.chapter.toDbChapter() }, dbManga)
        }
    }

    /**
     * Deletes the directories of a list of partially downloaded chapters.
     *
     * @param chapters the list of chapters to delete.
     * @param manga the manga of the chapters.
     * @param source the source of the chapters.
     */
    fun deleteChapters(chapters: List<Chapter>, manga: Manga) {
        GlobalScope.launchNonCancellable {
            launchNonCancellable { cache.removeChapters(chapters, manga) }
            launchNonCancellable { removeFromDownloadQueue(chapters) }

            try {

                val mangaDir = provider.findMangaDir(manga)

                val chapterDirs =
                    provider.findChapterDirs(chapters, manga) +
                        provider.findTempChapterDirs(chapters, manga)

                if (chapterDirs.isEmpty()) {
                    launchNonCancellable { pendingDeleter.deletePendingChapter(manga, chapters) }
                } else {

                    launchNonCancellable {
                        chapterDirs.forEach {
                            it.delete()
                            pendingDeleter.deletePendingChapter(manga, chapters)
                        }

                        if (
                            cache.getDownloadCount(manga, true) == 0
                        ) { // Delete manga directory if empty
                            chapterDirs.firstOrNull()?.parentFile?.delete()
                        }

                        // Delete manga directory if empty
                        if (mangaDir?.listFiles()?.isEmpty() == true) {
                            deleteManga(manga, removeQueued = false)
                        }
                    }
                }
            } catch (e: Exception) {
                TimberKt.e(e) { "error deleting chapters" }
            }
        }
    }

    private fun removeFromDownloadQueue(chapters: List<Chapter>) {
        val wasRunning = downloader.isRunning
        if (wasRunning) {
            downloader.pause()
        }

        downloader.removeFromQueue(chapters)

        if (wasRunning) {
            if (queueState.value.isEmpty()) {
                downloader.stop()
            } else if (queueState.value.isNotEmpty()) {
                downloader.start()
            }
        }
    }

    /*
        private suspend fun getChaptersToDelete(chapters: List<Chapter>, manga: Manga): List<Chapter> {
            // Retrieve the categories that are set to exclude from being deleted on read
            val categoriesToExclude = pr.removeExcludeCategories().get().map(String::toLong)

            val categoriesForManga = getCategories.await(manga.id)
                .map { it.id }
                .ifEmpty { listOf(0) }
            val filteredCategoryManga = if (categoriesForManga.intersect(categoriesToExclude).isNotEmpty()) {
                chapters.filterNot { it.read }
            } else {
                chapters
            }

            return if (!downloadPreferences.removeBookmarkedChapters().get()) {
                filteredCategoryManga.filterNot { it.bookmark }
            } else {
                filteredCategoryManga
            }
        }
    */

    /** return the list of all manga folders */
    fun getMangaFolders(): List<UniFile> {
        return provider.findSourceDir()?.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Deletes the directories of chapters that were read or have no match
     *
     * @param allChapters the list of chapters to delete.
     * @param manga the manga of the chapters.
     */
    fun cleanupChapters(
        allChapters: List<Chapter>,
        manga: Manga,
        removeRead: Boolean,
        removeNonFavorite: Boolean,
    ): Int {
        var cleaned = 0

        if (removeNonFavorite && !manga.favorite) {
            val mangaFolder = provider.getMangaDir(manga)
            cleaned += 1 + (mangaFolder.listFiles()?.size ?: 0)
            mangaFolder.delete()
            cache.removeManga(manga)
            return cleaned
        }

        val filesWithNoChapter = provider.findUnmatchedChapterDirs(allChapters, manga)
        cleaned += filesWithNoChapter.size
        cache.removeFolders(filesWithNoChapter.mapNotNull { it.name }, manga)
        filesWithNoChapter.forEach { it.delete() }

        if (removeRead) {
            val readChapters = allChapters.filter { it.read }
            val readChapterDirs = provider.findChapterDirs(readChapters, manga)
            readChapterDirs.forEach { it.delete() }
            cleaned += readChapterDirs.size
            cache.removeChapters(readChapters, manga)
        }

        if (cache.getDownloadCount(manga) == 0) {
            val mangaFolder = provider.getMangaDir(manga)
            val size = mangaFolder.listFiles()?.size ?: 0
            if (size == 0) {
                mangaFolder.delete()
                cache.removeManga(manga)
            } else {
                TimberKt.e { "Cache and download folder doesn't match for ${manga.title}" }
            }
        }
        return cleaned
    }

    /**
     * Deletes the directory of a downloaded manga.
     *
     * @param manga the manga to delete.
     * @param source the source of the manga.
     */
    fun deleteManga(manga: Manga, removeQueued: Boolean = true) {
        launchIO {
            if (removeQueued) {
                downloader.removeFromQueue(manga)
            }
            provider.findMangaDir(manga)?.delete()
            cache.removeManga(manga)
        }
    }

    /**
     * Adds a list of chapters to be deleted later.
     *
     * @param chapters the list of chapters to delete.
     * @param manga the manga of the chapters.
     */
    fun enqueueDeleteChapters(chapters: List<Chapter>, manga: Manga) {
        pendingDeleter.addChapters(chapters, manga)
    }

    /** Triggers the execution of the deletion of pending chapters. */
    fun deletePendingChapters() {
        val pendingChapters = pendingDeleter.getPendingChapters()
        for ((manga, chapters) in pendingChapters) {
            deleteChapters(chapters, manga)
        }
    }

    /**
     * Renames an already downloaded chapter
     *
     * @param manga the manga of the chapter.
     * @param oldChapter the existing chapter with the old name.
     * @param newChapter the target chapter with the new name.
     */
    fun renameChapter(manga: Manga, oldChapter: Chapter, newChapter: Chapter) {
        val oldName = provider.getChapterDirName(oldChapter)
        var newName = provider.getChapterDirName(newChapter)
        val mangaDir = provider.getMangaDir(manga)

        val oldDownload = mangaDir.findFile(oldName) ?: return

        if (oldDownload.isFile && oldDownload.name?.endsWith(".cbz") == true) {
            newName += ".cbz"
        }

        if (oldDownload.name == newName) return

        if (oldDownload.renameTo(newName)) {
            cache.removeChapters(listOf(oldChapter), manga)
            cache.addChapter(newName, manga)
        } else {
            TimberKt.e { "Could not rename downloaded chapter: $oldName" }
        }
    }

    // forceRefresh the cache
    fun refreshCache() {
        cache.forceRenewCache()
    }

    fun statusFlow(): Flow<Download> =
        queueState
            .flatMapLatest { downloads ->
                downloads.map { download -> download.statusFlow.map { download } }.merge()
            }
            .onStart {
                emitAll(
                    queueState.value
                        .filter { download -> download.status == Download.State.DOWNLOADING }
                        .asFlow()
                )
            }

    fun progressFlow(): Flow<Download> =
        queueState
            .flatMapLatest { downloads ->
                downloads.map { download -> download.progressFlow.drop(1).map { download } }.merge()
            }
            .onStart {
                emitAll(
                    queueState.value
                        .filter { download -> download.status == Download.State.DOWNLOADING }
                        .asFlow()
                )
            }

    fun networkStateFlow(): Flow<NetworkState> {
        return context.networkStateFlow()
    }
}
