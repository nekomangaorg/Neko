package eu.kanade.tachiyomi.ui.reader.loader

import android.app.Application
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import java.io.File
import uy.kohesive.injekt.injectLazy

/**
 * Loader used to load a chapter from the downloaded chapters.
 */
class DownloadPageLoader(
    private val chapter: ReaderChapter,
    private val manga: Manga,
    private val source: Source,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
) : PageLoader() {

    // Needed to open input streams
    private val context: Application by injectLazy()

    private var zipPageLoader: ZipPageLoader? = null

    override fun recycle() {
        super.recycle()
        zipPageLoader?.recycle()
    }

    /**
     * Returns the pages found on this downloaded chapter.
     */
    override suspend fun getPages(): List<ReaderPage> {
        val dbChapter = chapter.chapter
        val chapterPath = downloadProvider.findChapterDir(dbChapter, manga)
        return if (chapterPath?.isFile == true) {
            getPagesFromArchive(chapterPath)
        } else {
            getPagesFromDirectory()
        }
    }

    private suspend fun getPagesFromArchive(chapterPath: UniFile): List<ReaderPage> {
        val loader = ZipPageLoader(File(chapterPath.filePath!!)).also { zipPageLoader = it }
        return loader.getPages()
    }

    private fun getPagesFromDirectory(): List<ReaderPage> {
        val pages = downloadManager.buildPageList(manga, chapter.chapter)
        return pages.map { page ->
            ReaderPage(
                page.index, page.url, page.imageUrl, page.mangaDexChapterId,
                stream = {
                    context.contentResolver.openInputStream(page.uri ?: Uri.EMPTY)!!
                },
            ).apply {
                status = Page.State.READY
            }
        }
    }

    override suspend fun loadPage(page: ReaderPage) {
        zipPageLoader?.loadPage(page)
    }
}
