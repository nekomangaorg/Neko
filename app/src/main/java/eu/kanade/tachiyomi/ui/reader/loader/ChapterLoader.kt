package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.getHttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.nekomanga.R
import org.nekomanga.logging.TimberKt

/** Loader used to retrieve the [PageLoader] for a given chapter. */
class ChapterLoader(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val manga: Manga,
    private val sourceManager: SourceManager,
) {

    private val activeLoads = ConcurrentHashMap<Long, Deferred<Unit>>()

    /**
     * Assigns the chapter's page loader and loads the its pages. Returns immediately if the chapter
     * is already loaded. Deduplicates concurrent loads for the same chapter.
     */
    suspend fun loadChapter(chapter: ReaderChapter) {
        if (chapterIsReady(chapter)) {
            return
        }

        val chapterId =
            chapter.chapter.id
                ?: throw IllegalStateException("Chapter ID cannot be null for loading")

        coroutineScope {
            val deferred =
                activeLoads.compute(chapterId) { _, existing ->
                    if (existing != null && existing.isActive) {
                        existing
                    } else {
                        async(Dispatchers.IO) {
                            try {
                                if (chapterIsReady(chapter)) {
                                    return@async
                                }
                                chapter.state = ReaderChapter.State.Loading
                                TimberKt.d { "Loading pages for ${chapter.chapter.name}" }
                                val loader = getPageLoader(chapter)
                                chapter.pageLoader = loader

                                val pages = loader.getPages().onEach { it.chapter = chapter }

                                if (pages.isEmpty()) {
                                    throw Exception(context.getString(R.string.no_pages_found))
                                }

                                // If the chapter is partially read, set the starting page to the
                                // last the user read
                                // otherwise use the requested page.
                                if (!chapter.chapter.read) {
                                    chapter.requestedPage = chapter.chapter.last_page_read
                                }

                                chapter.state = ReaderChapter.State.Loaded(pages)
                            } catch (e: Throwable) {
                                if (e !is CancellationException) {
                                    chapter.state = ReaderChapter.State.Error(e)
                                }
                                throw e
                            } finally {
                                activeLoads.remove(chapterId)
                            }
                        }
                    }
                }

            deferred?.await()
        }
    }

    /** Checks [chapter] to be loaded based on present pages and loader in addition to state. */
    private fun chapterIsReady(chapter: ReaderChapter): Boolean {
        return chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null
    }

    /** Returns the page loader to use for this [chapter]. */
    private fun getPageLoader(chapter: ReaderChapter): PageLoader {
        val isDownloaded = downloadManager.isChapterDownloaded(chapter.chapter, manga, true)
        val source = chapter.chapter.getHttpSource(sourceManager)
        return when {
            isDownloaded -> DownloadPageLoader(chapter, manga, downloadManager, downloadProvider)
            else -> HttpPageLoader(chapter, source)
        }
    }
}
