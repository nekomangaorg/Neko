package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of [DownloadBottomSheet].
 */
class DownloadBottomPresenter : BaseCoroutinePresenter<DownloadBottomSheet>() {

    /**
     * Download manager.
     */
    val downloadManager: DownloadManager by injectLazy()
    var items = listOf<DownloadHeaderItem>()

    /**
     * Property to get the queue from the download manager.
     */
    val downloadQueue: DownloadQueue
        get() = downloadManager.queue

    fun getItems() {
        presenterScope.launch {
            val items = downloadQueue
                .groupBy { it.source }
                .map { entry ->
                    DownloadHeaderItem(entry.key.id, entry.key.name, entry.value.size).apply {
                        addSubItems(0, entry.value.map { DownloadItem(it, this) })
                    }
                }
            val hasChanged = if (this@DownloadBottomPresenter.items.size != items.size ||
                this@DownloadBottomPresenter.items.sumOf { it.subItemsCount } != items.sumOf { it.subItemsCount }
            ) {
                true
            } else {
                val oldItemsIds = this@DownloadBottomPresenter.items.map { header ->
                    header.subItems.mapNotNull { it.download.chapter.id }
                }
                    .flatten()
                    .toLongArray()
                val newItemsIds = items.map { header ->
                    header.subItems.mapNotNull { it.download.chapter.id }
                }
                    .flatten()
                    .toLongArray()
                !oldItemsIds.contentEquals(newItemsIds)
            }
            this@DownloadBottomPresenter.items = items
            if (hasChanged) {
                withContext(Dispatchers.Main) { view?.onNextDownloads(items) }
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
