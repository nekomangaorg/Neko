package eu.kanade.tachiyomi.data.library

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.library.LibraryUpdateRanker.rankingScheme
import eu.kanade.tachiyomi.data.library.LibraryUpdateService.Companion.start
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.getNewScanlatorsConditionalResetFilter
import eu.kanade.tachiyomi.util.shouldDownloadNewChapters
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class will take care of updating the chapters of the manga from the library. It can be
 * started calling the [start] method. If it's already running, it won't do anything.
 * While the library is updating, a [PowerManager.WakeLock] will be held until the update is
 * completed, preventing the device from going to sleep mode. A notification will display the
 * progress of the update, and if case of an unexpected error, this service will be silently
 * destroyed.
 */
class LibraryUpdateService(
    val db: DatabaseHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    val trackManager: TrackManager = Injekt.get(),
) : Service() {

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var notifier: LibraryUpdateNotifier

    private var job: Job? = null

    private val mangaToUpdate = mutableListOf<LibraryManga>()

    private val mangaToUpdateMap = mutableMapOf<Long, List<LibraryManga>>()

    private val categoryIds = mutableSetOf<Int>()

    // List containing new updates
    private val newUpdates = mutableMapOf<LibraryManga, Array<Chapter>>()

    // List containing failed updates
    private val failedUpdates = mutableMapOf<Manga, String?>()

    private val statusHandler: StatusHandler by injectLazy()

    val count = AtomicInteger(0)
    val jobCount = AtomicInteger(0)

    // List containing categories that get included in downloads.
    private val categoriesToDownload =
        preferences.downloadNewCategories().get().map(String::toInt)

    // Boolean to determine if user wants to automatically download new chapters.
    private val downloadNew: Boolean = preferences.downloadNew().get()

    // Boolean to determine if DownloadManager has downloads
    private var hasDownloads = false

    private var requestSemaphore = Semaphore(5)

    // For updates delete removed chapters if not preference is set as well
    private val deleteRemoved by lazy {
        preferences.deleteRemovedChapters().get() != 1
    }

    private fun addManga(mangaToAdd: List<LibraryManga>) {
        val distinctManga = mangaToAdd.filter { it !in mangaToUpdate }
        mangaToUpdate.addAll(distinctManga)
        distinctManga.groupBy { it.source }.forEach {
            // if added queue items is a new source not in the async list or an async list has
            // finished running
            if (mangaToUpdateMap[it.key].isNullOrEmpty()) {
                mangaToUpdateMap[it.key] = it.value
                jobCount.andIncrement
                val handler = CoroutineExceptionHandler { _, exception ->
                    XLog.e("Exception handler is handling exception")
                    XLog.e(exception)
                }
                GlobalScope.launch(handler) {
                    val hasDLs = try {
                        requestSemaphore.withPermit {
                            updateMangaInSource(
                                it.key,
                            )
                        }
                    } catch (e: Exception) {
                        false
                    }
                    hasDownloads = hasDownloads || hasDLs
                    jobCount.andDecrement
                    finishUpdates()
                }
            } else {
                val list = mangaToUpdateMap[it.key] ?: emptyList()
                mangaToUpdateMap[it.key] = (list + it.value)
            }
        }
    }

    private fun addMangaToQueue(categoryId: Int, manga: List<LibraryManga>) {
        val selectedScheme = preferences.libraryUpdatePrioritization().getOrDefault()
        val mangaList = manga.sortedWith(rankingScheme[selectedScheme])
        categoryIds.add(categoryId)
        addManga(mangaList)
    }

    private fun addCategory(categoryId: Int) {
        val selectedScheme = preferences.libraryUpdatePrioritization().getOrDefault()
        val mangaList =
            getMangaToUpdate(categoryId, Target.CHAPTERS).sortedWith(
                rankingScheme[selectedScheme]
            )
        categoryIds.add(categoryId)
        addManga(mangaList)
    }

    /**
     * Returns the list of manga to be updated.
     *
     * @param intent the update intent.
     * @param target the target to update.
     * @return a list of manga to update
     */
    private fun getMangaToUpdate(categoryId: Int, target: Target): List<LibraryManga> {
        val libraryManga = db.getLibraryMangaList().executeAsBlocking()

        var listToUpdate = if (categoryId != -1) {
            categoryIds.add(categoryId)
            libraryManga.filter { it.category == categoryId }
        } else {
            val categoriesToUpdate =
                preferences.libraryUpdateCategories().get().map(String::toInt)
            if (categoriesToUpdate.isNotEmpty()) {
                categoryIds.addAll(categoriesToUpdate)
                libraryManga.filter { it.category in categoriesToUpdate }.distinctBy { it.id }
            } else {
                categoryIds.addAll(db.getCategories().executeAsBlocking().mapNotNull { it.id } + 0)
                libraryManga.distinctBy { it.id }
            }
        }
        if (target == Target.CHAPTERS && preferences.updateOnlyNonCompleted()) {
            listToUpdate = listToUpdate.filter { it.status != SManga.COMPLETED }
        }

        val categoriesToExclude =
            preferences.libraryUpdateCategoriesExclude().get().map(String::toInt)
        val listToExclude = if (categoriesToExclude.isNotEmpty()) {
            libraryManga.filter { it.category in categoriesToExclude }
        } else {
            emptyList()
        }

        return listToUpdate.minus(listToExclude)
    }

    private fun getMangaToUpdate(intent: Intent, target: Target): List<LibraryManga> {
        val categoryId = intent.getIntExtra(KEY_CATEGORY, -1)
        return getMangaToUpdate(categoryId, target)
    }

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        notifier = LibraryUpdateNotifier(this)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LibraryUpdateService:WakeLock"
        )
        wakeLock.acquire(TimeUnit.MINUTES.toMillis(30))
        startForeground(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.progressNotificationBuilder.build()
        )
    }

    /**
     * Method called when the service is destroyed. It cancels jobs and releases the wake lock.
     */
    override fun onDestroy() {
        job?.cancel()
        if (instance == this) {
            instance = null
        }
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        listener?.onUpdateManga(LibraryManga())
        super.onDestroy()
    }

    /**
     * This method needs to be implemented, but it's not used/needed.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Method called when the service receives an intent.
     *
     * @param intent the start intent from.
     * @param flags the flags of the command.
     * @param startId the start id of this command.
     * @return the start value of the command.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val target = intent.getSerializableExtra(KEY_TARGET) as? Target ?: return START_NOT_STICKY

        instance = this

        val selectedScheme = preferences.libraryUpdatePrioritization().getOrDefault()
        val savedMangaList = intent.getLongArrayExtra(KEY_MANGAS)?.asList()

        val mangaList = (
            if (savedMangaList != null) {
                val mangaList = db.getLibraryMangaList().executeAsBlocking().filter {
                    it.id in savedMangaList
                }.distinctBy { it.id }
                val categoryId = intent.getIntExtra(KEY_CATEGORY, -1)
                if (categoryId > -1) categoryIds.add(categoryId)
                mangaList
            } else {
                getMangaToUpdate(intent, target)
            }
            ).sortedWith(rankingScheme[selectedScheme])
        // Update favorite manga. Destroy service when completed or in case of an error.
        launchTarget(target, mangaList, startId)
        return START_REDELIVER_INTENT
    }

    private fun launchTarget(target: Target, mangaToAdd: List<LibraryManga>, startId: Int) {
        val handler = CoroutineExceptionHandler { _, exception ->
            XLog.e(exception)
            stopSelf(startId)
        }
        if (target == Target.CHAPTERS) {
            listener?.onUpdateManga(LibraryManga())
        }

        job = GlobalScope.launch(handler) {
            when (target) {
                Target.CHAPTERS -> updateChaptersJob(mangaToAdd)
            }
        }

        job?.invokeOnCompletion { stopSelf(startId) }
    }

    private suspend fun updateChaptersJob(mangaToAdd: List<LibraryManga>) {
        // Initialize the variables holding the progress of the updates.

        mangaToUpdate.addAll(mangaToAdd)

        mangaToUpdateMap.putAll(mangaToAdd.groupBy { it.source })

        val isDexUp = sourceManager.getMangadex().checkIfUp()

        jobCount.andIncrement
        if (isDexUp) {
            val results = mangaToUpdateMap.keys.map { source ->
                try {
                    updateMangaInSource(source)
                } catch (e: Exception) {
                    XLog.e(e)
                    false
                }
            }
            hasDownloads = hasDownloads || results.any { it }
        } else {
            mangaToUpdateMap.clear()
        }
        jobCount.andDecrement
        finishUpdates(isDexUp)
    }

    private fun finishUpdates(isDexUp: Boolean = true) {
        if (jobCount.get() != 0) return
        if (newUpdates.isNotEmpty()) {
            notifier.showResultNotification(newUpdates)
            if (downloadNew && hasDownloads) {
                DownloadService.start(this)
            }
            newUpdates.clear()
        }
        if (preferences.showLibraryUpdateErrors() && failedUpdates.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdates)
            notifier.showUpdateErrorNotification(
                failedUpdates.map { it.key.title },
                errorFile.getUriCompat(this)
            )
        }
        if (isDexUp.not()) {
            notifier.showUpdateErrorNotification(
                listOf("Skipping library update"),
                null
            )
        }
        failedUpdates.clear()
        notifier.cancelProgressNotification()
    }

    private suspend fun updateMangaInSource(
        source: Long,
    ): Boolean {
        if (mangaToUpdateMap[source] == null) return false
        var currentCount = 0
        var hasDownloads = false
        while (currentCount < mangaToUpdateMap[source]!!.size) {

            val manga = mangaToUpdateMap[source]!![currentCount]
            val shouldDownload = manga.shouldDownloadNewChapters(db, preferences)
            logTimeTaken("library manga ${manga.title}") {
                if (updateMangaChapters(manga, this.count.andIncrement, shouldDownload)) {
                    hasDownloads = true
                }
            }
            currentCount++
        }
        GlobalScope.launchIO {
            updateReadingStatus(mangaToUpdateMap[source])
        }
        mangaToUpdateMap[source] = emptyList()
        return hasDownloads
    }

    private suspend fun updateMangaChapters(
        manga: LibraryManga,
        progress: Int,
        shouldDownload: Boolean,
    ):
        Boolean {
        return runCatching {
            var hasDownloads = false
            if (job?.isCancelled == true) {
                return@runCatching false
            }

            notifier.showProgressNotification(manga, progress, mangaToUpdate.size)

            var errorFromMerged = false

            val source = sourceManager.getMangadex()

            val details = withIOContext {
                source.fetchMangaAndChapterDetails(manga)
            }

            val merged = if (manga.isMerged()) {
                withIOContext {
                    runCatching {
                        sourceManager.getMergeSource().fetchChapters(manga.merge_manga_url!!)
                    }
                }.getOrElse { e ->
                    XLog.e("Error with mergedsource", e)
                    errorFromMerged = true
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            val fetchedChapters = details.second.toMutableList() + merged

            // delete cover cache image if the thumbnail from network is not empty
            // note: we preload the covers here so we can view everything offline if they change

            val thumbnailUrl = manga.thumbnail_url
            manga.copyFrom(details.first)
            manga.initialized = true

            withIOContext {
                // dont refresh covers while using cached source
                if (manga.thumbnail_url != null && preferences.refreshCoversToo()
                        .getOrDefault()
                ) {
                    coverCache.deleteFromCache(thumbnailUrl)
                    // load new covers in background
                    val request =
                        ImageRequest.Builder(this@LibraryUpdateService).data(manga)
                            .memoryCachePolicy(CachePolicy.DISABLED).build()
                    Coil.imageLoader(this@LibraryUpdateService).enqueue(request)
                }
            }
            db.insertManga(manga).executeOnIO()
            // add mdlist tracker if manga in library has it missing
            withIOContext {
                val tracks = db.getTracks(manga).executeOnIO().toMutableList()

                if (tracks.isEmpty() || !tracks.any { it.sync_id == trackManager.mdList.id }) {
                    val track = trackManager.mdList.createInitialTracker(manga)
                    db.insertTrack(track).executeAsBlocking()
                }
            }

            if (fetchedChapters.isNotEmpty()) {
                val originalChapters = db.getChapters(manga).executeAsBlocking()
                val newChapters =
                    syncChaptersWithSource(db, fetchedChapters, manga, errorFromMerged)

                if (newChapters.first.isNotEmpty()) {
                    if (shouldDownload) {
                        var chaptersToDl = newChapters.first.sortedBy { it.chapter_number }

                        if (manga.scanlator_filter != null) {
                            val scanlatorsToDownload =
                                MdUtil.getScanlators(manga.scanlator_filter!!).toMutableSet()

                            val newScanlators =
                                manga.getNewScanlatorsConditionalResetFilter(
                                    db,
                                    originalChapters,
                                    newChapters.first
                                )

                            scanlatorsToDownload.addAll(newScanlators)

                            // only download scanlators we have filtered, unless a new scanlator starts uploading
                            chaptersToDl =
                                chaptersToDl.filter { scanlatorsToDownload.contains(it.scanlator) }
                        }

                        downloadChapters(manga, chaptersToDl)
                        hasDownloads = true
                    }
                    newUpdates[manga] =
                        newChapters.first.sortedBy { it.chapter_number }.toTypedArray()
                }
                if (deleteRemoved && newChapters.second.isNotEmpty()) {
                    val removedChapters = newChapters.second.filter {
                        downloadManager.isChapterDownloaded(it, manga)
                    }
                    if (removedChapters.isNotEmpty()) {
                        downloadManager.deleteChapters(removedChapters, manga, source)
                    }
                }
                if (newChapters.first.size + newChapters.second.size > 0) listener?.onUpdateManga(
                    manga
                )
            }

            coroutineScope {
                launch {
                    updateMissingChapterCount(manga)
                }
            }

            // no reason to do this when using cache
            /*if (preferences.markChaptersReadFromMDList() && preferences.useCacheSource().not()) {
            tracks.firstOrNull { it.sync_id == trackManager.mdList.id }?.let {
                if (FollowStatus.fromInt(it.status) == FollowStatus.READING && it.last_chapter_read > 0) {
                    val chapters = db.getChapters(manga).executeAsBlocking()
                    val filteredChp = chapters.filter { chp -> ceil(chp.chapter_number.toDouble()).toInt() <= it.last_chapter_read && !chp.read && chp.chapter_number.toInt() != 0 }
                    if (filteredChp.isNotEmpty()) {
                        filteredChp.forEach {
                            it.read = true
                        }
                        db.updateChaptersProgress(filteredChp).executeAsBlocking()

                        if (preferences.removeAfterMarkedAsRead()) {
                            downloadManager.deleteChapters(filteredChp, manga, source)
                        }
                    }
                }
            }
        }*/
            hasDownloads
        }.getOrElse { e ->
            if (e !is CancellationException) {
                failedUpdates[manga] = e.message ?: "unknown error"
                XLog.e("Failed updating: ${manga.title}", e)
            }
            false
        }
    }

    private suspend fun updateMissingChapterCount(manga: LibraryManga): LibraryManga {
        val allChaps = db.getChapters(manga).executeAsBlocking()
        val missingChapters = MdUtil.getMissingChapterCount(allChaps, manga.status)
        if (missingChapters != manga.missing_chapters) {
            manga.missing_chapters = missingChapters
            db.insertManga(manga).executeOnIO()
        }
        return manga
    }

    suspend fun updateReadingStatus(mangaList: List<LibraryManga>?) {
        if (mangaList.isNullOrEmpty()) return
        if (sourceManager.getMangadex().isLogged() && job?.isCancelled == false) {
            runCatching {
                val readingStatus = statusHandler.fetchReadingStatusForAllManga()
                if (readingStatus.isNotEmpty()) {
                    XLog.d("Updating follow statuses")
                    mangaList!!.map { libraryManga ->
                        runCatching {
                            db.getTracks(libraryManga).executeOnIO()
                                .toMutableList()
                                .firstOrNull { it.sync_id == trackManager.mdList.id }
                                ?.apply {
                                    val result =
                                        readingStatus[MdUtil.getMangaId(libraryManga.url)]
                                    if (this.status != FollowStatus.fromDex(result).int) {
                                        this.status = FollowStatus.fromDex(result).int
                                        db.insertTrack(this).executeOnIO()
                                    }
                                }
                        }.onFailure {
                            XLog.e("Error refreshing tracking", it)
                        }
                    }
                }
            }.onFailure {
                XLog.e("error getting reading status", it)
            }
        }
    }

    private fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        downloadManager.downloadChapters(manga, chapters, false)
    }

    /**
     * Writes basic file of update errors to cache dir.
     */
    private fun writeErrorFile(errors: Map<Manga, String?>): File {
        try {
            if (errors.isNotEmpty()) {
                val destFile = File(externalCacheDir, "neko_update_errors.txt")

                destFile.bufferedWriter().use { out ->
                    errors.forEach { (manga, error) ->
                        out.write("${manga.title}: $error\n")
                    }
                }
                return destFile
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        CHAPTERS, // Manga meta data and  chapters
    }

    companion object {

        /**
         * Key for category to update.
         */
        const val KEY_CATEGORY = "category"

        fun categoryInQueue(id: Int?) = instance?.categoryIds?.contains(id) ?: false
        private var instance: LibraryUpdateService? = null

        /**
         * Key that defines what should be updated.
         */
        const val KEY_TARGET = "target"

        /**
         * Key for list of manga to be updated. (For dynamic categories)
         */
        const val KEY_MANGAS = "mangaList"

        /**
         * Returns the status of the service.
         *
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(): Boolean {
            return instance != null
        }

        /**
         * Starts the service. It will be started only if there isn't another instance already
         * running.
         *
         * @param context the application context.
         * @param category a specific category to update, or null for global update.
         * @param target defines what should be updated.
         */
        fun start(
            context: Context,
            category: Category? = null,
            target: Target = Target.CHAPTERS,
            mangaToUse: List<LibraryManga>? = null,
        ) {
            if (!isRunning()) {
                val intent = Intent(context, LibraryUpdateService::class.java).apply {
                    putExtra(KEY_TARGET, target)
                    category?.id?.let { id ->
                        putExtra(KEY_CATEGORY, id)
                        if (mangaToUse != null) putExtra(
                            KEY_MANGAS,
                            mangaToUse.mapNotNull { it.id }.toLongArray()
                        )
                    }
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(intent)
                } else {
                    context.startForegroundService(intent)
                }
            } else {
                if (target == Target.CHAPTERS) category?.id?.let {
                    if (mangaToUse != null) instance?.addMangaToQueue(it, mangaToUse)
                    else instance?.addCategory(it)
                }
            }
        }

        /**
         * Stops the service.
         *
         * @param context the application context.
         */
        fun stop(context: Context) {
            instance?.job?.cancel()
            GlobalScope.launch {
                instance?.jobCount?.set(0)
                instance?.finishUpdates()
            }
            context.stopService(Intent(context, LibraryUpdateService::class.java))
        }

        private var listener: LibraryServiceListener? = null

        fun setListener(listener: LibraryServiceListener) {
            this.listener = listener
        }

        fun removeListener(listener: LibraryServiceListener) {
            if (this.listener == listener) this.listener = null
        }

        fun callListener(manga: Manga) {
            listener?.onUpdateManga(manga)
        }
    }
}

interface LibraryServiceListener {
    fun onUpdateManga(manga: Manga? = null)
}
