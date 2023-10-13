package eu.kanade.tachiyomi.data.download.model

import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadStore
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rx.subjects.PublishSubject

class DownloadQueue(
    private val store: DownloadStore,
    private val queue: MutableList<Download> = CopyOnWriteArrayList<Download>(),
) :
    List<Download> by queue {

    private val statusSubject = PublishSubject.create<Download>()

    private val updatedRelay = PublishRelay.create<Unit>()

    private val downloadListeners = mutableListOf<DownloadListener>()

    private var scope = MainScope()

    fun addAll(downloads: List<Download>) {
        downloads.forEach { download ->
            download.setStatusSubject(statusSubject)
            download.setStatusCallback(::setPagesFor)
            download.status = Download.State.QUEUE
        }
        downloadListeners.forEach { it.updateDownloads() }

        queue.addAll(downloads)
        store.addAll(downloads)
        updatedRelay.call(Unit)
    }

    fun remove(download: Download) {
        val removed = queue.remove(download)
        store.remove(download)
        download.setStatusSubject(null)
        download.setStatusCallback(null)
        if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
            download.status = Download.State.NOT_DOWNLOADED
        }
        downloadListeners.forEach { it.updateDownload(download) }
        if (removed) {
            updatedRelay.call(Unit)
        }
    }

    fun updateListeners() {
        val listeners = downloadListeners.toList()
        listeners.forEach { it.updateDownloads() }
    }

    fun remove(chapter: Chapter) {
        find { it.chapter.id == chapter.id }?.let { remove(it) }
    }

    fun remove(chapters: List<Chapter>) {
        for (chapter in chapters) {
            remove(chapter)
        }
    }

    fun remove(manga: Manga) {
        filter { it.manga.id == manga.id }.forEach { remove(it) }
    }

    fun clear() {
        queue.forEach { download ->
            download.setStatusSubject(null)
            download.setStatusCallback(null)
            if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
                download.status = Download.State.NOT_DOWNLOADED
            }
            downloadListeners.forEach { it.updateDownload(download) }
        }
        queue.clear()
        store.clear()
        updatedRelay.call(Unit)
    }

    private fun setPagesFor(download: Download) {
        if (download.status == Download.State.DOWNLOADING) {
            if (download.pages != null) {
                for (page in download.pages!!)
                    scope.launch {
                        page.statusFlow.collectLatest {
                            callListeners(download)
                        }
                    }
            }
            callListeners(download)
        } else if (download.status == Download.State.DOWNLOADED || download.status == Download.State.ERROR) {
            if (download.status == Download.State.ERROR) {
                callListeners(download)
            }
        } else {
            callListeners(download)
        }
    }

    private fun callListeners(download: Download) {
        downloadListeners.forEach { it.updateDownload(download) }
    }

    fun addListener(listener: DownloadListener) {
        downloadListeners.add(listener)
    }

    fun removeListener(listener: DownloadListener) {
        downloadListeners.remove(listener)
    }

    interface DownloadListener {
        fun updateDownload(download: Download)
        fun updateDownloads()
    }
}
