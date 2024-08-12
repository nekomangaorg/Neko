package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/** Loader used to load chapters from an online source. */
class HttpPageLoader(
    private val chapter: ReaderChapter,
    private val source: HttpSource,
    private val chapterCache: ChapterCache = Injekt.get(),
) : PageLoader() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** A queue used to manage requests one by one while allowing priorities. */
    private val queue = PriorityBlockingQueue<PriorityPage>()

    private val readerPreferences by injectLazy<ReaderPreferences>()
    private var preloadSize = readerPreferences.preloadPageAmount().get()

    init {
        // Adding flow since we can reach reader settings after this is created
        readerPreferences.preloadPageAmount().changes().onEach { preloadSize = it }.launchIn(scope)

        scope.launchIO {
            flow {
                    while (true) {
                        emit(runInterruptible { queue.take() }.page)
                    }
                }
                .filter { it.status == Page.State.QUEUE }
                .collect { collectPage(it) }
        }
    }

    /** Recycles this loader and the active subscriptions and queue. */
    override fun recycle() {
        super.recycle()
        scope.cancel()
        queue.clear()

        // Cache current page list progress for online chapters to allow a faster reopen
        val pages = chapter.pages
        if (pages != null) {
            launchIO {
                try {
                    // Convert to pages without reader information
                    val pagesToSave = pages.map { Page(it.index, it.url, it.imageUrl) }
                    chapterCache.putPageListToCache(chapter.chapter, pagesToSave)
                } catch (e: Throwable) {
                    if (e is CancellationException) {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Returns the page list for a chapter. It tries to return the page list from the local cache,
     * otherwise fallbacks to network.
     */
    override suspend fun getPages(): List<ReaderPage> {
        val pages = source.getPageList(chapter.chapter)

        return pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderPage(index, page.url, page.imageUrl, page.mangaDexChapterId)
        }
    }

    /**
     * Loads a page through the queue. Handles re-enqueueing pages if they were evicted from the
     * cache.
     */
    override suspend fun loadPage(page: ReaderPage) {
        withIOContext {
            val imageUrl = page.imageUrl

            // Check if the image has been deleted
            if (page.status == Page.State.READY &&
                imageUrl != null &&
                !chapterCache.isImageInCache(imageUrl)) {
                page.status = Page.State.QUEUE
            }

            // Automatically retry failed pages when subscribed to this page
            if (page.status == Page.State.ERROR) {
                page.status = Page.State.QUEUE
            }

            val queuedPages = mutableListOf<PriorityPage>()
            if (page.status == Page.State.QUEUE) {
                queuedPages += PriorityPage(page, 1).also { queue.offer(it) }
            }
            queuedPages += preloadNextPages(page, preloadSize)

            suspendCancellableCoroutine<Nothing> { continuation ->
                continuation.invokeOnCancellation {
                    queuedPages.forEach {
                        if (it.page.status == Page.State.QUEUE) {
                            queue.remove(it)
                        }
                    }
                }
            }
        }
    }

    /**
     * Preloads the given [amount] of pages after the [currentPage] with a lower priority.
     *
     * @return a list of [PriorityPage] that were added to the [queue]
     */
    private fun preloadNextPages(currentPage: ReaderPage, amount: Int): List<PriorityPage> {
        val pageIndex = currentPage.index
        val pages = currentPage.chapter.pages ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        return pages.subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size)).mapNotNull {
            if (it.status == Page.State.QUEUE) {
                PriorityPage(it, 0).apply { queue.offer(this) }
            } else {
                null
            }
        }
    }

    /** Retries a page. This method is only called from user interaction on the viewer. */
    override fun retryPage(page: ReaderPage) {
        if (page.status == Page.State.ERROR) {
            page.status = Page.State.QUEUE
        }
        queue.offer(PriorityPage(page, 2))
    }

    /** Data class used to keep ordering of pages in order to maintain priority. */
    private class PriorityPage(
        val page: ReaderPage,
        val priority: Int,
    ) : Comparable<PriorityPage> {
        companion object {
            private val idGenerator = AtomicInteger()
        }

        private val identifier = idGenerator.incrementAndGet()

        override fun compareTo(other: PriorityPage): Int {
            val p = other.priority.compareTo(priority)
            return if (p != 0) p else identifier.compareTo(other.identifier)
        }
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary. Downloaded
     * images are stored in the chapter cache.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private suspend fun collectPage(page: ReaderPage) {
        try {
            val imageUrl = page.imageUrl!!
            if (!chapterCache.isImageInCache(imageUrl)) {
                page.status = Page.State.DOWNLOAD_IMAGE
                val imageResponse = source.getImage(page)
                chapterCache.putImageToCache(imageUrl, imageResponse)
            }

            page.stream = { chapterCache.getImageFile(imageUrl).inputStream() }
            page.status = Page.State.READY
        } catch (e: Throwable) {
            page.status = Page.State.ERROR
            if (e is CancellationException) {
                throw e
            } else {
                TimberKt.e(e) { "Error loading page" }
            }
        }
    }
}
