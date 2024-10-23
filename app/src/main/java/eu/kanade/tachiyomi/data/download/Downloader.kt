package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
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
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import okhttp3.Response
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import tachiyomi.core.util.storage.DiskUtil.NOMEDIA_FILE
import uy.kohesive.injekt.injectLazy

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
    private val _queueState = MutableStateFlow<List<Download>>(emptyList())
    val queueState = _queueState.asStateFlow()

    /** Notifier for the downloader state and progress. */
    private val notifier by lazy { DownloadNotifier(context) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloaderJob: Job? = null

    /** Whether the downloader is running. */
    val isRunning: Boolean
        get() = downloaderJob?.isActive ?: false

    /** Whether the downloader is paused */
    @Volatile var isPaused: Boolean = false

    init {
        launchNow {
            val chapters = async { store.restore() }
            addAllToQueue(chapters.await())
        }
    }

    /**
     * Starts the downloader. It doesn't do anything if it's already running or there isn't anything
     * to download.
     *
     * @return true if the downloader is started, false otherwise.
     */
    fun start(): Boolean {
        if (isRunning || queueState.value.isEmpty()) {
            return false
        }

        val pending = queueState.value.filter { it.status != Download.State.DOWNLOADED }
        pending.forEach { if (it.status != Download.State.QUEUE) it.status = Download.State.QUEUE }

        isPaused = false

        launchDownloaderJob()

        return pending.isNotEmpty()
    }

    /** Stops the downloader. */
    fun stop(reason: String? = null) {
        cancelDownloaderJob()
        queueState.value
            .filter { it.status == Download.State.DOWNLOADING }
            .forEach { it.status = Download.State.ERROR }

        if (reason != null) {
            notifier.onWarning(reason)
            return
        }

        if (isPaused && queueState.value.isNotEmpty()) {
            notifier.onPaused()
        } else {
            notifier.onComplete()
        }

        isPaused = false

        DownloadJob.stop(context)
    }

    /** Pauses the downloader */
    fun pause() {
        cancelDownloaderJob()
        queueState.value
            .filter { it.status == Download.State.DOWNLOADING }
            .forEach { it.status = Download.State.QUEUE }
        isPaused = true
    }

    /** Removes everything from the queue. */
    fun clearQueue() {
        cancelDownloaderJob()
        clearQueueState()
        notifier.dismissProgress()
    }

    /** Prepares to start downloader job for downloading. */
    private fun launchDownloaderJob() {
        if (isRunning) return

        downloaderJob =
            scope.launch {
                val activeDownloadsFlow =
                    queueState
                        .transformLatest { queue ->
                            while (true) {
                                val activeDownloads =
                                    queue
                                        .asSequence()
                                        .filter {
                                            it.status.value <= Download.State.DOWNLOADING.value
                                        } // Ignore completed downloads, leave them in the queue
                                        .groupBy { it.source }
                                        .toList()
                                        .map { (_, downloads) -> downloads.take(2) }
                                        .flatten()
                                emit(activeDownloads)

                                if (activeDownloads.isEmpty()) break
                                // Suspend until a download enters the ERROR state
                                val activeDownloadsErroredFlow =
                                    combine(activeDownloads.map(Download::statusFlow)) { states ->
                                            states.contains(Download.State.ERROR)
                                        }
                                        .filter { it }
                                activeDownloadsErroredFlow.first()
                            }
                        }
                        .distinctUntilChanged()

                // Use supervisorScope to cancel child jobs when the downloader job is cancelled
                supervisorScope {
                    val downloadJobs = mutableMapOf<Download, Job>()

                    activeDownloadsFlow.collectLatest { activeDownloads ->
                        val downloadJobsToStop = downloadJobs.filter { it.key !in activeDownloads }
                        downloadJobsToStop.forEach { (download, job) ->
                            job.cancel()
                            downloadJobs.remove(download)
                        }

                        val downloadsToStart = activeDownloads.filter { it !in downloadJobs }
                        downloadsToStart.forEach { download ->
                            downloadJobs[download] = launchDownloadJob(download)
                        }
                    }
                }
            }
    }

    private fun CoroutineScope.launchDownloadJob(download: Download) = launchIO {
        try {
            downloadChapter(download)

            // Remove successful download from queue
            if (download.status == Download.State.DOWNLOADED) {
                removeFromQueue(download)
            }
            if (areAllDownloadsFinished()) {
                stop()
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            TimberKt.e(e)
            notifier.onError(e.message)
            stop()
        }
    }

    /** Destroys the downloader job. */
    private fun cancelDownloaderJob() {
        downloaderJob?.cancel()
        downloaderJob = null
    }

    /**
     * Creates a download object for every chapter and adds them to the downloads queue.
     *
     * @param manga the manga of the chapters to download.
     * @param chapters the list of chapters to download.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    fun queueChapters(manga: Manga, chapters: List<Chapter>, autoStart: Boolean) {
        if (chapters.isEmpty()) return

        val wasEmpty = queueState.value.isEmpty()
        val chapterDirFiles = provider.findMangaDir(manga)?.listFiles()?.asList() ?: emptyList()

        val chaptersToQueue =
            chapters
                .asSequence()
                // Filter out those already downloaded.
                .filter { provider.chapterDirDoesNotExist(it, chapterDirFiles) }
                // filter out scanlators that aren't supported if they are official source
                .filter {
                    when (it.scanlator) {
                        null -> true
                        else -> !MdConstants.UnsupportedOfficialScanlators.contains(it.scanlator!!)
                    }
                }
                // Add chapters to queue from the start.
                .sortedByDescending { it.source_order }
                // Filter out those already enqueued.
                .filter { chapter -> queueState.value.none { it.chapter.id == chapter.id } }
                // Create a download for each one.
                .map {
                    val source = it.getHttpSource(sourceManager)
                    Download(source, manga, it)
                }
                .toList()

        if (chaptersToQueue.isNotEmpty()) {
            addAllToQueue(chaptersToQueue)

            // Start downloader if needed

            // Start downloader if needed
            if (autoStart && wasEmpty) {
                DownloadJob.start(context)
            } else if (!isRunning && !LibraryUpdateJob.isRunning(context)) {
                notifier.onPaused()
            }
        }
    }

    /**
     * Downloads a chapter.
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
                download.chapter.name,
            )
            return
        }

        val chapterDirname = provider.getChapterDirName(download.chapter)
        val tmpDir = mangaDir.createDirectory(chapterDirname + TMP_DIR_SUFFIX)!!

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
                                Page(index, page.url, page.imageUrl, uri = page.uri)
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

            if (!isDownloadSuccessful(download, tmpDir)) {
                download.status = Download.State.ERROR
                return
            }

            // Only rename the directory if it's downloaded
            if (preferences.saveChaptersAsCBZ().get()) {
                archiveChapter(mangaDir, chapterDirname, tmpDir)
            } else {
                tmpDir.renameTo(chapterDirname)
                DiskUtil.createNoMediaFile(tmpDir, context)
            }
            cache.addChapter(chapterDirname, download.manga)
            download.status = Download.State.DOWNLOADED
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            // If the page list threw, it will resume here
            TimberKt.e(error)
            download.status = Download.State.ERROR
            notifier.onError(error.message, download.chapter.name, download.manga.title)
        }
    }

    /**
     * Gets the image from the filesystem if it exists or downloads it otherwise.
     *
     * @param page the page to download.
     * @param download the download of the page.
     * @param tmpDir the temporary directory of the download.
     */
    private suspend fun getOrDownloadImage(page: Page, download: Download, tmpDir: UniFile) {
        // If the image URL is empty, do nothing
        if (page.imageUrl == null) {
            return
        }

        val digitCount = (download.pages?.size ?: 0).toString().length.coerceAtLeast(3)
        val filename = "%0${digitCount}d".format(Locale.ENGLISH, page.number)
        val tmpFile = tmpDir.findFile("$filename.tmp")

        // Delete temp file if it exists
        tmpFile?.delete()

        // Try to find the image file
        val imageFile =
            tmpDir.listFiles()?.firstOrNull {
                it.name!!.startsWith("$filename.") || it.name!!.startsWith("${filename}__001")
            }

        try {
            // If the image is already downloaded, do nothing. Otherwise download from network
            val file =
                when {
                    imageFile != null -> imageFile
                    chapterCache.isImageInCache(page.imageUrl!!) ->
                        copyImageFromCache(
                            chapterCache.getImageFile(page.imageUrl!!),
                            tmpDir,
                            filename,
                        )
                    else -> downloadImage(page, download.source, tmpDir, filename)
                }

            // When the page is ready, set page path, progress (just in case) and status
            splitTallImageIfNeeded(page, tmpDir)

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
                val file = tmpDir.createFile("$filename.tmp")!!
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
    private fun copyImageFromCache(cacheFile: File, tmpDir: UniFile, filename: String): UniFile {
        val tmpFile = tmpDir.createFile("$filename.tmp")!!
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
            response.body.contentType()?.run { if (type == "image") "image/$subtype" else null }
                // Else guess from the uri.
                ?: context.contentResolver.getType(file.uri)
                // Else read magic numbers.
                ?: ImageUtil.findImageType { file.openInputStream() }?.mime

        return ImageUtil.getExtensionFromMimeType(mime)
    }

    private fun splitTallImageIfNeeded(page: Page, tmpDir: UniFile) {
        if (!readerPreferences.splitTallImages().get()) return

        try {
            val fileName = "%03d".format(Locale.ENGLISH, page.number)
            val imageFile =
                tmpDir.listFiles()?.firstOrNull { it.name.orEmpty().startsWith(fileName) }
                    ?: throw Error(
                        context.getString(
                            R.string.download_notifier_split_page_not_found,
                            page.number,
                        )
                    )

            // Check if the original page was previously split before then skip.
            if (imageFile.name.orEmpty().startsWith("${fileName}__")) return

            ImageUtil.splitTallImage(tmpDir, imageFile, fileName)
        } catch (e: Exception) {
            TimberKt.e(e) { "Failed to split downloaded image" }
        }
    }

    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param tmpDir the directory where the download is currently stored.
     */
    private fun isDownloadSuccessful(download: Download, tmpDir: UniFile): Boolean {
        // Page list hasn't been initialized or all pages have not been downloaded
        if (download.pages?.size == null || download.pages!!.size != download.downloadedImages) {
            return false
        }

        val downloadPageCount = download.pages?.size

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
        return downloadedImagesCount == downloadPageCount
    }

    /** Archive the chapter pages as a CBZ. */
    private fun archiveChapter(mangaDir: UniFile, dirname: String, tmpDir: UniFile) {
        val zip = mangaDir.createFile("$dirname.cbz$TMP_DIR_SUFFIX")!!
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

    /** Returns true if all the queued downloads are in DOWNLOADED or ERROR state. */
    private fun areAllDownloadsFinished(): Boolean {
        return queueState.value.none { it.status.value <= Download.State.DOWNLOADING.value }
    }

    private fun addAllToQueue(downloads: List<Download>) {
        _queueState.update {
            // TODO make this not gross mutable
            downloads.forEach { download -> download.status = Download.State.QUEUE }
            store.addAll(downloads)
            it + downloads
        }
    }

    private fun removeFromQueue(download: Download) {
        _queueState.update {
            store.remove(download)
            if (
                download.status == Download.State.DOWNLOADING ||
                    download.status == Download.State.QUEUE
            ) {
                download.status = Download.State.NOT_DOWNLOADED
            }
            it - download
        }
    }

    private inline fun removeFromQueueIf(predicate: (Download) -> Boolean) {
        _queueState.update { queue ->
            val downloads = queue.filter { predicate(it) }
            store.removeAll(downloads)
            downloads.forEach { download ->
                if (
                    download.status == Download.State.DOWNLOADING ||
                        download.status == Download.State.QUEUE
                ) {
                    download.status = Download.State.NOT_DOWNLOADED
                }
            }
            queue - downloads
        }
    }

    fun removeFromQueue(chapters: List<Chapter>) {
        val chapterIds = chapters.map { it.id }
        removeFromQueueIf { it.chapter.id in chapterIds }
    }

    fun removeFromQueue(manga: Manga) {
        removeFromQueueIf { it.manga.id == manga.id }
    }

    private fun clearQueueState() {
        _queueState.update {
            it.forEach { download ->
                if (
                    download.status == Download.State.DOWNLOADING ||
                        download.status == Download.State.QUEUE
                ) {
                    download.status = Download.State.NOT_DOWNLOADED
                }
            }
            store.clear()
            emptyList()
        }
    }

    fun updateQueue(downloads: List<Download>) {
        val wasRunning = isRunning

        if (downloads.isEmpty()) {
            clearQueue()
            stop()
            return
        }

        pause()
        clearQueueState()
        addAllToQueue(downloads)

        if (wasRunning) {
            start()
        }
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
        const val MIN_DISK_SPACE = 200L * 1024 * 1024
    }
}
