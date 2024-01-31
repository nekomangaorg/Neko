package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.hippo.unifile.UniFile
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.getHttpSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.storage.saveTo
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNow
import eu.kanade.tachiyomi.util.system.withIOContext
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import org.nekomanga.R
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.logging.TimberKt
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import tachiyomi.core.util.storage.DiskUtil
import tachiyomi.core.util.storage.DiskUtil.NOMEDIA_FILE
import uy.kohesive.injekt.injectLazy

/**
 * This class is the one in charge of downloading chapters.
 *
 * Its [queue] contains the list of chapters to download. In order to download them, the downloader
 * subscriptions must be running and the list of chapters must be sent to them by [downloadsRelay].
 *
 * The queue manipulation must be done in one thread (currently the main thread) to avoid unexpected
 * behavior, but it's safe to read it from multiple threads.
 *
 * @param context the application context.
 * @param provider the downloads directory provider.
 * @param cache the downloads cache, used to add the downloads to the cache after their completion.
 * @param sourceManager the source manager.
 */
class Downloader(
    private val context: Context,
    private val provider: DownloadProvider,
    private val cache: DownloadCache,
    private val sourceManager: SourceManager,
) {
    private val preferences: PreferencesHelper by injectLazy()
    private val readerPreferences: ReaderPreferences by injectLazy()
    private val chapterCache: ChapterCache by injectLazy()

    /** Store for persisting downloads across restarts. */
    private val store = DownloadStore(context, sourceManager)

    /** Queue where active downloads are kept. */
    val queue = DownloadQueue(store)

    private val handler = Handler(Looper.getMainLooper())

    /** Notifier for the downloader state and progress. */
    private val notifier by lazy { DownloadNotifier(context) }

    /** Downloader subscriptions. */
    private var subscription: Subscription? = null

    /** Relay to send a list of downloads to the downloader. */
    private val downloadsRelay = PublishRelay.create<List<Download>>()

    /** Whether the downloader is running. */
    val isRunning: Boolean
        get() = subscription != null

    /** Whether the downloader is paused */
    @Volatile var isPaused: Boolean = false

    init {
        launchNow {
            val chapters = async { store.restore() }
            queue.addAll(chapters.await())
            DownloadJob.callListeners()
        }
    }

    /**
     * Starts the downloader. It doesn't do anything if it's already running or there isn't anything
     * to download.
     *
     * @return true if the downloader is started, false otherwise.
     */
    fun start(): Boolean {
        if (subscription != null || queue.isEmpty()) {
            return false
        }
        initializeSubscription()

        val pending = queue.filter { it.status != Download.State.DOWNLOADED }
        pending.forEach { if (it.status != Download.State.QUEUE) it.status = Download.State.QUEUE }

        isPaused = false

        downloadsRelay.call(pending)

        return pending.isNotEmpty()
    }

    /** Stops the downloader. */
    fun stop(reason: String? = null) {
        destroySubscription()
        queue
            .filter { it.status == Download.State.DOWNLOADING }
            .forEach { it.status = Download.State.ERROR }

        if (reason != null) {
            notifier.onWarning(reason)
            return
        }

        DownloadJob.stop(context)
        if (isPaused && queue.isNotEmpty()) {
            handler.postDelayed({ notifier.onDownloadPaused() }, 150)
        } else {
            notifier.dismiss()
        }
        DownloadJob.callListeners(false)
        isPaused = false
    }

    /** Pauses the downloader */
    fun pause() {
        destroySubscription()
        queue
            .filter { it.status == Download.State.DOWNLOADING }
            .forEach { it.status = Download.State.QUEUE }
        isPaused = true
    }

    /**
     * Removes everything from the queue.
     *
     * @param isNotification value that determines if status is set (needed for view updates)
     */
    fun clearQueue(isNotification: Boolean = false) {
        destroySubscription()

        // Needed to update the chapter view
        if (isNotification) {
            queue
                .filter { it.status == Download.State.QUEUE }
                .forEach { it.status = Download.State.NOT_DOWNLOADED }
        }
        queue.clear()
        notifier.dismiss()
    }

    /**
     * Removes everything from the queue for a certain manga
     *
     * @param isNotification value that determines if status is set (needed for view updates)
     */
    fun clearQueue(manga: Manga, isNotification: Boolean = false) {
        // Needed to update the chapter view
        if (isNotification) {
            queue
                .filter { it.status == Download.State.QUEUE && it.manga.id == manga.id }
                .forEach { it.status = Download.State.NOT_DOWNLOADED }
        }
        queue.remove(manga)
        if (queue.isEmpty()) {
            if (DownloadJob.isRunning(context)) DownloadJob.stop(context)
            stop()
        }
        notifier.dismiss()
    }

    /** Prepares the subscriptions to start downloading. */
    private fun initializeSubscription() {
        if (isRunning) return

        subscription =
            downloadsRelay
                .concatMapIterable { it }
                .flatMap(
                    { download ->
                        Observable.fromCallable {
                            runBlocking { downloadChapter(download) }
                            download
                        }
                    },
                    2
                )
                .observeOn(AndroidSchedulers.mainThread())
                .onBackpressureLatest()
                .subscribe(
                    { completeDownload(it) },
                    { error ->
                        TimberKt.e(error)
                        notifier.onError(error.message)
                        stop()
                    },
                )
    }

    /** Destroys the downloader subscriptions. */
    private fun destroySubscription() {
        subscription?.unsubscribe()
        subscription = null
    }

    /**
     * Creates a download object for every chapter and adds them to the downloads queue.
     *
     * @param manga the manga of the chapters to download.
     * @param chapters the list of chapters to download.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    fun queueChapters(manga: Manga, chapters: List<Chapter>, autoStart: Boolean) = launchIO {
        if (chapters.isEmpty()) {
            return@launchIO
        }

        val wasEmpty = queue.isEmpty()
        // Called in background thread, the operation can be slow with SAF.
        val chaptersWithoutDir = async {
            chapters
                // Filter out those already downloaded.
                .filter { provider.findChapterDir(it, manga) == null }
                .filter { it.scanlator?.contains("Comikey")?.not() ?: true }
                // Add chapters to queue from the start.
                .sortedByDescending { it.source_order }
        }

        // Runs in main thread (synchronization needed).
        val chaptersToQueue =
            chaptersWithoutDir
                .await()
                // Filter out those already enqueued.
                .filter { chapter -> queue.none { it.chapter.id == chapter.id } }
                // Create a download for each one.
                .map {
                    val source = it.getHttpSource(sourceManager)
                    Download(source, manga, it)
                }

        if (chaptersToQueue.isNotEmpty()) {
            queue.addAll(chaptersToQueue)

            if (isRunning) {
                // Send the list of downloads to the downloader.
                downloadsRelay.call(chaptersToQueue)
            }

            // Start downloader if needed
            if (autoStart && wasEmpty) {
                DownloadJob.start(this@Downloader.context)
            } else if (!isRunning && !LibraryUpdateJob.isRunning(this@Downloader.context)) {
                notifier.onDownloadPaused()
            }
        }
    }

    /**
     * Returns the observable which downloads a chapter.
     *
     * @param download the chapter to be downloaded.
     */
    private suspend fun downloadChapter(download: Download) {
        val mangaDir = provider.getMangaDir(download.manga)

        val availSpace = DiskUtil.getAvailableStorageSpace(mangaDir)
        if (availSpace != -1L && availSpace < MIN_DISK_SPACE) {
            download.status = Download.State.ERROR
            notifier.onError(
                context.getString(R.string.couldnt_download_low_space),
                download.chapter.name
            )
            return
        }

        val chapterDirname = provider.getChapterDirName(download.chapter)
        val tmpDir = mangaDir.createDirectory(chapterDirname + TMP_DIR_SUFFIX)
        val pagesToDownload = if (download.source is MangaDex) 6 else 3

        try {
            // If the page list already exists, start from the file
            val pageList =
                download.pages
                    ?: run {
                        // Otherwise, pull page list from network and add them to download object
                        val pages = download.source.getPageList(download.chapter)

                        if (pages.isEmpty()) {
                            throw Exception(context.getString(R.string.no_pages_found))
                        }
                        // Don't trust index from source
                        val reIndexedPages =
                            pages.mapIndexed { index, page ->
                                Page(
                                    index,
                                    page.url,
                                    page.imageUrl,
                                    uri = page.uri,
                                )
                            }
                        download.pages = reIndexedPages
                        reIndexedPages
                    }

            // Delete all temporary (unfinished) files
            tmpDir.listFiles()?.filter { it.name!!.endsWith(".tmp") }?.forEach { it.delete() }

            download.status = Download.State.DOWNLOADING

            // Start downloading images, consider we can have downloaded images already
            // Concurrently do 2 pages at a time
            pageList
                .asFlow()
                .flatMapMerge(concurrency = pagesToDownload) { page ->
                    flow {
                            withIOContext { getOrDownloadImage(page, download, tmpDir) }
                            emit(page)
                        }
                        .flowOn(Dispatchers.IO)
                }
                .collect {
                    // Do when page is downloaded.
                    notifier.onProgressChange(download)
                }

            // Do after download completes
            ensureSuccessfulDownload(download, mangaDir, tmpDir, chapterDirname)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            // If the page list threw, it will resume here
            TimberKt.e(error)
            download.status = Download.State.ERROR
            notifier.onError(error.message, download.chapter.name, download.manga.title)
        }
    }

    /**
     * Returns the observable which gets the image from the filesystem if it exists or downloads it
     * otherwise.
     *
     * @param page the page to download.
     * @param download the download of the page.
     * @param tmpDir the temporary directory of the download.
     */
    private suspend fun getOrDownloadImage(
        page: Page,
        download: Download,
        tmpDir: UniFile,
    ) {
        // If the image URL is empty, do nothing
        if (page.imageUrl == null) {
            return
        }

        val digitCount = (download.pages?.size ?: 0).toString().length.coerceAtLeast(3)
        val filename = String.format("%0${digitCount}d", page.number)
        val tmpFile = tmpDir.findFile("$filename.tmp")

        // Delete temp file if it exists.
        tmpFile?.delete()

        // Try to find the image file
        val imageFile =
            tmpDir.listFiles()?.find {
                it.name!!.startsWith("$filename.") || it.name!!.startsWith("${filename}__001")
            }

        try {
            // If the image is already downloaded, do nothing. Otherwise download from network
            val file =
                when {
                    imageFile != null -> imageFile
                    chapterCache.isImageInCache(page.imageUrl!!) ->
                        moveImageFromCache(
                            chapterCache.getImageFile(
                                page.imageUrl!!,
                            ),
                            tmpDir,
                            filename,
                        )
                    else -> downloadImage(page, download.source, tmpDir, filename)
                }

            // When the page is ready, set page path, progress (just in case) and status
            val success = splitTallImageIfNeeded(page, tmpDir)
            if (!success) {
                notifier.onError(
                    context.getString(R.string.download_notifier_split_failed),
                    download.chapter.name,
                    download.manga.title,
                )
            }
            page.uri = file.uri
            page.progress = 100
            page.status = Page.State.READY
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            // Mark this page as error and allow to download the remaining
            page.progress = 0
            page.status = Page.State.ERROR
            notifier.onError(e.message, download.chapter.name, download.manga.title)
        }
    }

    /**
     * Downloads the image from network to a file in tmpDir.
     *
     * @param page the page to download.
     * @param source the source of the page.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the image.
     */
    private suspend fun downloadImage(
        page: Page,
        source: HttpSource,
        tmpDir: UniFile,
        filename: String,
    ): UniFile {
        page.status = Page.State.DOWNLOAD_IMAGE
        page.progress = 0
        return flow {
                val response = source.getImage(page)
                val file = tmpDir.createFile("$filename.tmp")
                try {
                    response.body.source().saveTo(file.openOutputStream())
                    val extension = getImageExtension(response, file)
                    file.renameTo("$filename.$extension")
                } catch (e: Exception) {
                    response.close()
                    file.delete()
                    throw e
                }
                emit(file)
            }
            // Retry 3 times, waiting 2, 4 and 8 seconds between attempts.
            .retryWhen { _, attempt ->
                if (attempt < 3) {
                    delay((2L shl attempt.toInt()) * 1000)
                    true
                } else {
                    false
                }
            }
            .first()
    }

    /**
     * Copies the image from cache to file in tmpDir.
     *
     * @param cacheFile the file from cache.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the image.
     */
    private fun moveImageFromCache(cacheFile: File, tmpDir: UniFile, filename: String): UniFile {
        val tmpFile = tmpDir.createFile("$filename.tmp")
        cacheFile.inputStream().use { input ->
            tmpFile.openOutputStream().use { output -> input.copyTo(output) }
        }
        val extension = ImageUtil.findImageType(cacheFile.inputStream()) ?: return tmpFile
        tmpFile.renameTo("$filename.${extension.extension}")
        cacheFile.delete()
        return tmpFile
    }

    /**
     * Returns the extension of the downloaded image from the network response, or if it's null,
     * analyze the file. If everything fails, assume it's a jpg.
     *
     * @param response the network response of the image.
     * @param file the file where the image is already downloaded.
     */
    private fun getImageExtension(response: Response, file: UniFile): String {
        // Read content type if available.
        val mime =
            response.body.contentType()?.let { ct -> "${ct.type}/${ct.subtype}" }
                // Else guess from the uri.
                ?: context.contentResolver.getType(file.uri)
                // Else read magic numbers.
                ?: ImageUtil.findImageType { file.openInputStream() }?.mime

        return ImageUtil.getExtensionFromMimeType(mime)
    }

    private fun splitTallImageIfNeeded(page: Page, tmpDir: UniFile): Boolean {
        if (!readerPreferences.splitTallImages().get()) return true

        val filename = String.format("%03d", page.number)
        val imageFile =
            tmpDir.listFiles()?.find { it.name!!.startsWith(filename) }
                ?: throw Error(
                    context.getString(R.string.download_notifier_split_page_not_found, page.number)
                )
        val imageFilePath =
            imageFile.filePath
                ?: throw Error(
                    context.getString(R.string.download_notifier_split_page_not_found, page.number)
                )

        // check if the original page was previously split before then skip.
        if (imageFile.name!!.contains("__")) return true

        return try {
            ImageUtil.splitTallImage(imageFile, imageFilePath)
        } catch (e: Exception) {
            TimberKt.e(e) { "Error splitting tall image" }
            false
        }
    }

    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param mangaDir the manga directory of the download.
     * @param tmpDir the directory where the download is currently stored.
     * @param dirname the real (non temporary) directory name of the download.
     */
    private fun ensureSuccessfulDownload(
        download: Download,
        mangaDir: UniFile,
        tmpDir: UniFile,
        dirname: String,
    ) {
        // Page list hasn't been initialized
        val downloadPageCount = download.pages?.size ?: return
        // Ensure that all pages has been downloaded
        if (download.downloadedImages < downloadPageCount) return
        // Ensure that the chapter folder has all the pages
        val downloadedImagesCount =
            tmpDir.listFiles().orEmpty().count {
                val fileName = it.name.orEmpty()
                when {
                    fileName in listOf(NOMEDIA_FILE) -> false
                    fileName.endsWith(".tmp") -> false
                    // Only count the first split page and not the others
                    fileName.contains("__") && !fileName.endsWith("__001.jpg") -> false
                    else -> true
                }
            }

        download.status =
            if (downloadedImagesCount == downloadPageCount) {
                // Only rename the directory if it's downloaded.
                if (preferences.saveChaptersAsCBZ().get()) {
                    archiveChapter(mangaDir, dirname, tmpDir)
                } else {
                    tmpDir.renameTo(dirname)
                }
                cache.addChapter(dirname, download.manga)

                DiskUtil.createNoMediaFile(tmpDir, context)

                Download.State.DOWNLOADED
            } else {
                Download.State.ERROR
            }
    }

    /** Archive the chapter pages as a CBZ. */
    private fun archiveChapter(
        mangaDir: UniFile,
        dirname: String,
        tmpDir: UniFile,
    ) {
        val zip = mangaDir.createFile("$dirname.cbz$TMP_DIR_SUFFIX")
        ZipOutputStream(BufferedOutputStream(zip.openOutputStream())).use { zipOut ->
            zipOut.setMethod(ZipEntry.STORED)

            tmpDir.listFiles()?.forEach { img ->
                img.openInputStream().use { input ->
                    val data = input.readBytes()
                    val size = img.length()
                    val entry =
                        ZipEntry(img.name).apply {
                            val crc = CRC32().apply { update(data) }
                            setCrc(crc.value)

                            compressedSize = size
                            setSize(size)
                        }
                    zipOut.putNextEntry(entry)
                    zipOut.write(data)
                }
            }
        }
        zip.renameTo("$dirname.cbz")
        tmpDir.delete()
    }

    /** Completes a download. This method is called in the main thread. */
    private fun completeDownload(download: Download) {
        // Delete successful downloads from queue
        if (download.status == Download.State.DOWNLOADED) {
            // Remove downloaded chapter from queue
            queue.remove(download)
        }
        if (areAllDownloadsFinished()) {
            stop()
        }
    }

    /** Returns true if all the queued downloads are in DOWNLOADED or ERROR state. */
    private fun areAllDownloadsFinished(): Boolean {
        return queue.none { it.status <= Download.State.DOWNLOADING }
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"

        // Arbitrary minimum required space to start a download: 200 MB
        const val MIN_DISK_SPACE = 200 * 1024 * 1024
    }
}
