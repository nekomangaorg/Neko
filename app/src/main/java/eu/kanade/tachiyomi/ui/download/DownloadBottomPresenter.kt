package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

/** Presenter of [DownloadBottomSheet]. */
class DownloadBottomPresenter : BaseCoroutinePresenter<DownloadBottomSheet>() {

    /** Download manager. */
    val downloadManager: DownloadManager by injectLazy()
    var items = listOf<DownloadHeaderItem>()

    /** Property to get the queue from the download manager. */
    val queueState
        get() = downloadManager.queueState

    override fun onCreate() {
        super.onCreate()

        presenterScope.launchIO {
            downloadManager
                .progressFlow()
                .catch { error -> TimberKt.e(error) }
                .collect {
                    withUIContext {
                        view?.onUpdateProgress(it)
                        view?.onUpdateDownloadedPages(it)
                    }
                }
            downloadManager
                .statusFlow()
                .catch { error -> TimberKt.e(error) }
                .collect {
                    withUIContext {
                        view?.onUpdateProgress(it)
                        view?.onUpdateDownloadedPages(it)
                    }
                }
        }

        presenterScope.launchIO {
            downloadManager
                .progressFlow()
                .catch { error -> TimberKt.e(error) }
                .collect {
                    withUIContext {
                        view?.onUpdateProgress(it)
                        view?.onUpdateDownloadedPages(it)
                    }
                }
        }

        queueState
            .onEach { downloads ->
                val items =
                    downloads
                        .groupBy { it.source }
                        .map { entry ->
                            DownloadHeaderItem(entry.key.id, entry.key.name, entry.value.size)
                                .apply {
                                    addSubItems(0, entry.value.map { DownloadItem(it, this) })
                                }
                        }

                val hasChanged =
                    if (this@DownloadBottomPresenter.items.size != items.size ||
                        this@DownloadBottomPresenter.items.sumOf { it.subItemsCount } !=
                            items.sumOf { it.subItemsCount }) {
                        true
                    } else {
                        val oldItemsIds =
                            this@DownloadBottomPresenter.items
                                .map { header ->
                                    header.subItems.mapNotNull { it.download.chapter.id }
                                }
                                .flatten()
                                .toLongArray()
                        val newItemsIds =
                            items
                                .map { header ->
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
            .launchIn(presenterScope)
    }

    fun flipDownloads() {
        if (!downloadManager.isRunning) {
            downloadManager.startDownloads()
        } else {
            downloadManager.pauseDownloads()
        }
    }

    /** Clears the download queue. */
    fun clearQueue() {
        downloadManager.clearQueue()
    }

    fun reorder(downloads: List<Download>) {
        downloadManager.reorderQueue(downloads)
    }

    fun cancelDownload(download: Download) {
        downloadManager.deletePendingDownloads(listOf(download))
    }

    fun cancelDownloads(downloads: List<Download>) {
        downloadManager.deletePendingDownloads(downloads)
    }
}
