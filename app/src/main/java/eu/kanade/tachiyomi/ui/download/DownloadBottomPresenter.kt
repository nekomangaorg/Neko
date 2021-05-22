package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of [DownloadBottomSheet].
 */
class DownloadBottomPresenter(val sheet: DownloadBottomSheet) {

    /**
     * Download manager.
     */
    val downloadManager: DownloadManager by injectLazy()
    var items = listOf<DownloadItem>()

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    /**
     * Property to get the queue from the download manager.
     */
    val downloadQueue: DownloadQueue
        get() = downloadManager.queue

    fun getItems() {
        scope.launch {
            val items = downloadQueue.map(::DownloadItem)
            val hasChanged = if (this@DownloadBottomPresenter.items.size != items.size) true
            else {
                val oldItemsIds = this@DownloadBottomPresenter.items.mapNotNull {
                    it.download.chapter.id
                }.toLongArray()
                val newItemsIds = items.mapNotNull { it.download.chapter.id }.toLongArray()
                !oldItemsIds.contentEquals(newItemsIds)
            }
            this@DownloadBottomPresenter.items = items
            if (hasChanged) {
                withContext(Dispatchers.Main) { sheet.onNextDownloads(items) }
            }
        }
    }

    /**
     * Pauses the download queue.
     */
    fun pauseDownloads() {
        downloadManager.pauseDownloads()
    }

    /**
     * Clears the download queue.
     */
    fun clearQueue() {
        downloadManager.clearQueue()
    }

    fun reorder(downloads: List<Download>) {
        downloadManager.reorderQueue(downloads)
    }

    fun cancelDownload(download: Download) {
        downloadManager.deletePendingDownloads(download)
    }

    fun cancelDownloads(downloads: List<Download>) {
        downloadManager.deletePendingDownloads(*downloads.toTypedArray())
    }
}
