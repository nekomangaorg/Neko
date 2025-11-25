package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onFailure
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadJob
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.MangaDetailChapterInformation
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.chapter.getChapterNum
import eu.kanade.tachiyomi.util.chapter.mergeSorted
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.shouldDownloadNewChapters
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.saveTimeTaken
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import eu.kanade.tachiyomi.util.system.withIOContext
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_BATTERY_NOT_LOW
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_CHARGING
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.manga.MangaUseCases
import org.nekomanga.util.system.mapAsync
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LibraryUpdateJob(private val context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    private val db by injectLazy<DatabaseHelper>()
    private val coverCache by injectLazy<CoverCache>()
    private val sourceManager by injectLazy<SourceManager>()
    private val preferences by injectLazy<PreferencesHelper>()

    private val mangaDexPreferences by injectLazy<MangaDexPreferences>()
    private val libraryPreferences by injectLazy<LibraryPreferences>()
    private val downloadManager by injectLazy<DownloadManager>()
    private val trackManager by injectLazy<TrackManager>()
    private val mangaDexLoginHelper by injectLazy<MangaDexLoginHelper>()
    private val statusHandler by injectLazy<StatusHandler>()

    private val mangaUseCases by injectLazy<MangaUseCases>()

    private var extraDeferredJobs = Collections.synchronizedList(mutableListOf<Deferred<Any>>())
    private val extraScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val emitScope = MainScope()

    private val mangaToUpdate = java.util.concurrent.CopyOnWriteArrayList<LibraryManga>()
    // List containing new updates
    private val newUpdates = mutableMapOf<LibraryManga, Array<Chapter>>()

    // List containing failed updates
    private val failedUpdates = mutableMapOf<Manga, String?>()

    // List containing skipped updates
    private val skippedUpdates = mutableMapOf<LibraryManga, String?>()

    val count = AtomicInteger(0)

    private val notificationMutex = Mutex()

    // Boolean to determine if user wants to automatically download new chapters.
    private val downloadNew: Boolean = preferences.downloadNewChapters().get()

    // Boolean to determine if DownloadManager has downloads
    private var hasDownloads = false

    // For updates delete removed chapters if not preference is set as well
    private val deleteRemoved by lazy { preferences.deleteRemovedChapters().get() != 1 }

    private val notifier = LibraryUpdateNotifier(context)

    override suspend fun doWork(): Result {

        libraryPreferences.lastUpdateAttemptTimestamp().set(Date().time)

        // Find a running manual worker. If exists, try again later
        if (WORK_NAME_AUTO in tags && instance != null) {
            return Result.retry()
        }

        tryToSetForeground()

        instance = WeakReference(this)

        val allLibraryManga = db.getLibraryMangaList().executeOnIO()
        val allTracks = db.getAllTracks().executeOnIO()
        val tracksByMangaId = allTracks.groupBy { it.manga_id }

        val savedMangaList =
            when (inputData.getBoolean(KEY_MANGA_LIST, false)) {
                true -> {
                    val set =
                        libraryPreferences
                            .libraryUpdateIds()
                            .get()
                            .splitToSequence("||")
                            .map { it.toLong() }
                            .toSet()
                    libraryPreferences.libraryUpdateIds().delete()
                    set
                }
                false -> null
            }

        val mangaListToFilter =
            (if (savedMangaList != null) {
                allLibraryManga.filter { it.id in savedMangaList }
            } else {
                allLibraryManga
            })

        val categoryId = inputData.getInt(KEY_CATEGORY, -1)

        val mangaList = getAndFilterMangaToUpdate(mangaListToFilter, tracksByMangaId, categoryId)

        return withIOContext {
            try {
                libraryPreferences.lastUpdateTimestamp().set(Date().time)
                updateMangaJob(mangaList)
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Assume success although cancelled
                    finishUpdates(true)
                    Result.success()
                } else {
                    TimberKt.e(e)
                    Result.failure()
                }
            } finally {
                instance = null
                sendUpdate(null)
                notifier.cancelProgressNotification()
            }
        }
    }

    private fun sendUpdate(mangaId: Long?) {
        if (isStopped) {
            updateMutableFlow.tryEmit(mangaId)
        } else {
            emitScope.launch { updateMutableFlow.emit(mangaId) }
        }
    }

    fun getAndFilterMangaToUpdate(
        libraryMangaList: List<LibraryManga>,
        tracksByMangaId: Map<Long, List<Track>>,
        categoryId: Int,
    ): List<LibraryManga> {

        val categoriesToUpdate =
            libraryPreferences.whichCategoriesToUpdate().get().map(String::toInt)
        val categoriesToExclude =
            libraryPreferences.whichCategoriesToExclude().get().map(String::toInt)
        val restrictions = libraryPreferences.autoUpdateMangaRestrictions().get()

        return libraryMangaList
            .asSequence()
            .distinctBy { it.id }
            .filter { libraryManga ->
                if (categoryId != -1) {
                    // Specific category
                    libraryManga.category == categoryId
                } else {
                    // General categories
                    val included =
                        if (categoriesToUpdate.isNotEmpty()) {
                            libraryManga.category in categoriesToUpdate
                        } else {
                            true // Included by default
                        }
                    val excluded =
                        if (categoriesToExclude.isNotEmpty()) {
                            libraryManga.category in categoriesToExclude
                        } else {
                            false // Not excluded by default
                        }
                    included && !excluded
                }
            }
            .filter { libraryManga ->
                when {
                    LibraryPreferences.MANGA_HAS_UNREAD in restrictions &&
                        libraryManga.unread != 0 -> {
                        skippedUpdates[libraryManga] =
                            context.getString(R.string.skipped_reason_has_unread)
                        false // Filter out
                    }
                    LibraryPreferences.MANGA_NOT_STARTED in restrictions &&
                        libraryManga.totalChapters > 0 &&
                        !libraryManga.hasStarted -> {
                        skippedUpdates[libraryManga] =
                            context.getString(R.string.skipped_reason_not_started)
                        false
                    }
                    LibraryPreferences.MANGA_NOT_COMPLETED in restrictions &&
                        libraryManga.status == SManga.COMPLETED -> {
                        skippedUpdates[libraryManga] =
                            context.getString(R.string.skipped_reason_completed)
                        false
                    }

                    // --- Optimized Tracking Checks ---
                    LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ in restrictions &&
                        hasTrackWithGivenStatus(
                            libraryManga,
                            context.getString(R.string.follows_plan_to_read),
                            tracksByMangaId,
                        ) -> {
                        skippedUpdates[libraryManga] =
                            context.getString(R.string.skipped_reason_tracking_plan_to_read)
                        false
                    }
                    LibraryPreferences.MANGA_TRACKING_DROPPED in restrictions &&
                        hasTrackWithGivenStatus(
                            libraryManga,
                            context.getString(R.string.follows_dropped),
                            tracksByMangaId,
                        ) -> {
                        skippedUpdates[libraryManga] =
                            context.getString(R.string.skipped_reason_tracking_dropped)
                        false
                    }
                    LibraryPreferences.MANGA_TRACKING_ON_HOLD in restrictions &&
                        hasTrackWithGivenStatus(
                            libraryManga,
                            context.getString(R.string.follows_on_hold),
                            tracksByMangaId,
                        ) -> {
                        skippedUpdates[libraryManga] =
                            context.getString(R.string.skipped_reason_tracking_on_hold)
                        false
                    }
                    LibraryPreferences.MANGA_TRACKING_COMPLETED in restrictions &&
                        hasTrackWithGivenStatus(
                            libraryManga,
                            context.getString(R.string.follows_completed),
                            tracksByMangaId,
                        ) -> {
                        skippedUpdates[libraryManga] =
                            context.getString(R.string.skipped_reason_tracking_completed)
                        false
                    }

                    // If no restriction matched, keep the manga
                    else -> true
                }
            }
            .toList()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notifier.progressNotificationBuilder.build()
        val id = Notifications.Id.Library.Progress
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    private suspend fun updateMangaJob(mangaToAdd: List<LibraryManga>) {
        // Initialize the variables holding the progress of the updates.
        saveTimeTaken(libraryPreferences) {
            emitScope.launch { mangaToAdd.forEach { mangaToUpdateMutableFlow.emit(it.id!!) } }

            mangaToUpdate.addAll(mangaToAdd)

            coroutineScope {
                val semaphoreAmount =
                    when (libraryPreferences.prioritizeLibraryUpdates().get()) {
                        true -> 20
                        false -> 5
                    }

                val requestSemaphore = Semaphore(semaphoreAmount)
                val downloadResults =
                    mangaToAdd
                        .map { manga ->
                            async {
                                requestSemaphore.withPermit {
                                    val shouldDownload =
                                        manga.shouldDownloadNewChapters(db, preferences)
                                    updateMangaChapters(manga, shouldDownload)
                                }
                            }
                        }
                        .awaitAll()

                if (!hasDownloads) {
                    hasDownloads = downloadResults.any { it }
                }

                launchIO { updateReadingStatus(mangaToAdd) }

                finishUpdates()
            }
        }
    }

    private fun hasTrackWithGivenStatus(
        libraryManga: LibraryManga,
        globalStatus: String,
        tracksByMangaId: Map<Long, List<Track>>,
    ): Boolean {
        val tracks = tracksByMangaId[libraryManga.id] ?: return false
        return tracks.any { track ->
            val status = trackManager.getService(track.sync_id)?.getGlobalStatus(track.status)
            if (status.isNullOrBlank()) {
                false
            } else {
                status == globalStatus
            }
        }
    }

    private suspend fun updateMangaChapters(manga: LibraryManga, shouldDownload: Boolean): Boolean =
        coroutineScope {
            return@coroutineScope runCatching {
                    try {
                        var hasDownloads = false
                        ensureActive()

                        var errorFromMerged = false

                        val source = sourceManager.mangaDex

                        val holder = withIOContext {
                            if (libraryPreferences.skipMangaMetadataDuringUpdate().get()) {
                                MangaDetailChapterInformation(
                                    null,
                                    emptyList(),
                                    source.fetchChapterList(manga).getOrThrow {
                                        Exception(it.message())
                                    },
                                )
                            } else {
                                source.fetchMangaAndChapterDetails(manga, true).getOrThrow {
                                    Exception(it.message())
                                }
                            }
                        }
                        val mergeMangaList = db.getMergeMangaList(manga).executeOnIO()
                        val mergedList =
                            when (mergeMangaList.isNotEmpty()) {
                                true -> {
                                    withIOContext {
                                        mergeMangaList.map { mergeManga ->
                                            // in the future check the merge type
                                            MergeType.getSource(mergeManga.mergeType, sourceManager)
                                                .fetchChapters(mergeManga.url)
                                                .onFailure {
                                                    errorFromMerged = true
                                                    failedUpdates[manga] =
                                                        "Merged Chapter --${mergeManga.mergeType}-- ${it.message()}"
                                                }
                                                .getOrElse { emptyList() }
                                                .map { (sChapter, status) ->
                                                    val sameVolume =
                                                        sChapter.vol == "" ||
                                                            manga.last_volume_number == null ||
                                                            sChapter.vol ==
                                                                manga.last_volume_number.toString()
                                                    if (
                                                        manga.last_chapter_number != null &&
                                                            sChapter.chapter_number ==
                                                                manga.last_chapter_number
                                                                    ?.toFloat() &&
                                                            sameVolume
                                                    ) {
                                                        sChapter.name += " [END]"
                                                    }
                                                    sChapter to status
                                                }
                                        }
                                    }
                                }

                                false -> emptyList()
                            }

                        val blockedGroups = mangaDexPreferences.blockedGroups().get()
                        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

                        val fetchedChapters =
                            (listOf(holder.sChapters) +
                                    mergedList.map { it.map { pair -> pair.first } })
                                .mergeSorted(
                                    compareBy<SChapter> { getChapterNum(it) != null }
                                        .thenBy { getChapterNum(it) }
                                )
                                .filter {
                                    val scanlators = ChapterUtil.getScanlators(it.scanlator)
                                    scanlators.none { scanlator -> scanlator in blockedGroups } &&
                                        (Constants.NO_GROUP !in scanlators ||
                                            it.uploader !in blockedUploaders)
                                }

                        // delete cover cache image if the thumbnail from network is not empty
                        // note: we preload the covers here so we can view everything offline if
                        // they
                        // change

                        holder.sManga?.let {
                            val thumbnailUrl = manga.thumbnail_url
                            manga.copyFrom(it)
                            manga.initialized = true

                            withIOContext {
                                // dont refresh covers while using cached source
                                if (
                                    manga.thumbnail_url != null &&
                                        libraryPreferences.updateCovers().get()
                                ) {
                                    coverCache.deleteFromCache(thumbnailUrl, manga.favorite)
                                    // load new covers in background
                                    val request =
                                        ImageRequest.Builder(applicationContext)
                                            .data(manga)
                                            .memoryCachePolicy(CachePolicy.DISABLED)
                                            .build()
                                    context.imageLoader.execute(request)
                                }
                            }
                            db.insertManga(manga).executeOnIO()

                            if (holder.sourceArtwork.isNotEmpty()) {
                                holder.sourceArtwork
                                    .map { sourceArt -> sourceArt.toArtworkImpl(manga.id!!) }
                                    .let { art ->
                                        runCatching {
                                            db.deleteArtworkForManga(manga).executeOnIO()
                                            db.insertArtWorkList(art).executeOnIO()
                                        }
                                    }
                            }

                            // add mdlist tracker if manga in library has it missing
                            withIOContext {
                                val tracks = db.getTracks(manga).executeOnIO().toMutableList()

                                if (
                                    tracks.isEmpty() ||
                                        !tracks.any { it.sync_id == trackManager.mdList.id }
                                ) {
                                    val track = trackManager.mdList.createInitialTracker(manga)
                                    db.insertTrack(track).executeOnIO()
                                    if (mangaDexLoginHelper.isLoggedIn()) {
                                        trackManager.mdList.bind(track)
                                    }
                                }
                            }
                        }

                        if (fetchedChapters.isNotEmpty()) {
                            val newChapters =
                                syncChaptersWithSource(db, fetchedChapters, manga, errorFromMerged)

                            if (newChapters.first.isNotEmpty()) {
                                if (shouldDownload) {
                                    var chaptersToDl =
                                        newChapters.first.sortedBy { it.chapter_number }

                                    if (manga.filtered_scanlators != null) {
                                        //  Ignored sources, groups and uploaders
                                        val toIgnore =
                                            ChapterUtil.getScanlators(manga.filtered_scanlators)
                                                .toMutableSet()

                                        // only download scanlators not filtered out
                                        chaptersToDl =
                                            chaptersToDl.filterNot {
                                                val scanlators =
                                                    ChapterUtil.getScanlators(it.scanlator)

                                                val scanlatorMatchAll =
                                                    libraryPreferences
                                                        .chapterScanlatorFilterOption()
                                                        .get() == 0
                                                ChapterUtil.filterByScanlator(
                                                    scanlators,
                                                    it.uploader ?: "",
                                                    scanlatorMatchAll,
                                                    toIgnore,
                                                )
                                            }
                                    }

                                    downloadChapters(manga, chaptersToDl)
                                    hasDownloads = true
                                }
                                newUpdates[manga] =
                                    newChapters.first.sortedBy { it.chapter_number }.toTypedArray()
                            }
                            if (deleteRemoved && newChapters.second.isNotEmpty()) {
                                val removedChapters =
                                    newChapters.second.filter {
                                        downloadManager.isChapterDownloaded(it, manga) &&
                                            newChapters.first.none { newChapter ->
                                                newChapter.chapter_number == it.chapter_number &&
                                                    it.scanlator.isNullOrBlank()
                                            }
                                    }
                                if (removedChapters.isNotEmpty()) {
                                    downloadManager.deleteChapters(manga, removedChapters)
                                }
                            }
                            if (newChapters.first.size + newChapters.second.size > 0) {
                                sendUpdate(manga.id)
                            }
                        }

                        coroutineScope {
                            launch {
                                if (mangaDexPreferences.readingSync().get()) {
                                    val dbChapters = db.getChapters(manga).executeOnIO()
                                    val (mergedChapters, nonMergedChapters) =
                                        dbChapters.partition { it.isMergedChapter() }
                                    if (mangaDexLoginHelper.isLoggedIn()) {
                                        statusHandler
                                            .getReadChapterIds(MdUtil.getMangaUUID(manga.url))
                                            .collect { chapterIds ->
                                                val markRead =
                                                    nonMergedChapters
                                                        .filter {
                                                            chapterIds.contains(
                                                                it.mangadex_chapter_id
                                                            )
                                                        }
                                                        .filter { !it.read }
                                                        .map {
                                                            it.read = true
                                                            it.last_page_read = 0
                                                            it.pages_left = 0
                                                            it
                                                        }
                                                        .toList()
                                                db.updateChaptersProgress(markRead).executeOnIO()
                                            }
                                    }
                                    if (mergedChapters.isNotEmpty()) {
                                        val readChapters =
                                            mergedList
                                                .flatten()
                                                .filter { it.second }
                                                .map { Pair(it.first.scanlator, it.first.url) }
                                        val markRead =
                                            mergedChapters
                                                .filter {
                                                    readChapters.contains(
                                                        Pair(it.scanlator, it.url)
                                                    )
                                                }
                                                .filter { !it.read }
                                                .map {
                                                    it.read = true
                                                    it.last_page_read = 0
                                                    it.pages_left = 0
                                                    it
                                                }
                                                .toList()
                                        db.updateChaptersProgress(markRead).executeOnIO()
                                    }
                                }
                            }
                            launch { updateMissingChapterCount(manga) }
                        }

                        hasDownloads
                    } finally {
                        notificationMutex.withLock {
                            notifier.showProgressNotification(
                                manga = manga,
                                current = this@LibraryUpdateJob.count.andIncrement,
                                total = mangaToUpdate.size,
                            )
                        }
                    }
                }
                .getOrElse { e ->
                    notificationMutex.withLock {
                        notifier.showProgressNotification(
                            manga = manga,
                            current = this@LibraryUpdateJob.count.andIncrement,
                            total = mangaToUpdate.size,
                        )
                    }
                    if (e !is CancellationException) {
                        failedUpdates[manga] = e.message ?: "unknown error"
                        TimberKt.e(e) { "Failed updating: ${manga.title}" }
                    }
                    return@coroutineScope false
                }
        }

    private suspend fun updateMissingChapterCount(manga: LibraryManga) {
        mangaUseCases.updateMangaStatusAndMissingCount(manga)
    }

    suspend fun updateReadingStatus(mangaList: List<LibraryManga>?) = coroutineScope {
        TimberKt.d { "Attempting to update reading statuses" }
        if (mangaList.isNullOrEmpty()) return@coroutineScope
        ensureActive()
        if (mangaDexLoginHelper.isLoggedIn()) {
            runCatching {
                    val readingStatus = statusHandler.fetchReadingStatusForAllManga()
                    if (readingStatus.isNotEmpty()) {
                        TimberKt.d { "Updating follow statuses" }
                        mangaList.mapAsync { libraryManga ->
                            runCatching {
                                    db.getTracks(libraryManga)
                                        .executeOnIO()
                                        .toMutableList()
                                        .firstOrNull { it.sync_id == trackManager.mdList.id }
                                        ?.apply {
                                            val result =
                                                readingStatus[MdUtil.getMangaUUID(libraryManga.url)]
                                            if (this.status != FollowStatus.fromDex(result).int) {
                                                this.status = FollowStatus.fromDex(result).int
                                                db.insertTrack(this).executeOnIO()
                                            }
                                        }
                                }
                                .onFailure { TimberKt.e(it) { "Error refreshing tracking" } }
                        }
                    }
                }
                .onFailure { TimberKt.e(it) { "error getting reading status" } }
        }
    }

    private suspend fun finishUpdates(wasStopped: Boolean = false) {

        if (!wasStopped && !isStopped) {
            extraDeferredJobs.awaitAll()
        }
        if (newUpdates.isNotEmpty()) {
            notifier.showResultNotification(newUpdates)
            if (downloadNew && hasDownloads) {
                DownloadJob.start(applicationContext)
            }
        }
        newUpdates.clear()
        if (
            skippedUpdates.isNotEmpty() &&
                Notifications.isNotificationChannelEnabled(
                    context,
                    Notifications.Channel.Library.Skipped,
                )
        ) {
            val skippedFile =
                writeErrorFile(skippedUpdates.map { it.key.title to it.value }.toMap(), "skipped")
                    .getUriCompat(context)
            notifier.showUpdateSkippedNotification(skippedUpdates.map { it.key.title }, skippedFile)
        }
        if (
            failedUpdates.isNotEmpty() &&
                Notifications.isNotificationChannelEnabled(
                    context,
                    Notifications.Channel.Library.Error,
                )
        ) {
            val errorFile = writeErrorFile(failedUpdates.map { it.key.title to it.value }.toMap())
            notifier.showUpdateErrorNotification(
                failedUpdates.map { it.key.title },
                errorFile.getUriCompat(context),
            )
        }
        failedUpdates.clear()
        skippedUpdates.clear()

        notifier.cancelProgressNotification()
    }

    private fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        downloadManager.downloadChapters(manga, chapters, false)
    }

    /** Writes basic file of update errors to cache dir. */
    private fun writeErrorFile(errors: Map<String, String?>, fileName: String = "errors"): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("neko_update_$fileName.txt")
                file.bufferedWriter().use { out ->
                    // Error file format:
                    // ! Error
                    //   # Source
                    //     - Manga
                    errors.toList().groupBy({ it.second }, { it.first }).forEach {
                        (error, mangaList) ->
                        out.write("! ${error}\n")
                        mangaList.forEach { out.write("    - ${it}\n") }
                    }
                }
                return file
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }

    private fun addMangaToQueue(manga: List<LibraryManga>) {
        extraScope.launch {
            val tracksByMangaId = db.getAllTracks().executeOnIO().groupBy { it.manga_id }
            val mangaToAdd =
                getAndFilterMangaToUpdate(manga, tracksByMangaId = tracksByMangaId, categoryId = -1)

            addManga(mangaToAdd)
        }
    }

    private fun addCategory(categoryId: Int) {
        extraScope.launch {
            val allLibraryManga = db.getLibraryMangaList().executeOnIO()
            val tracksByMangaId = db.getAllTracks().executeOnIO().groupBy { it.manga_id }
            val mangaToAdd =
                getAndFilterMangaToUpdate(
                    allLibraryManga,
                    tracksByMangaId = tracksByMangaId,
                    categoryId = categoryId,
                )
            addManga(mangaToAdd)
        }
    }

    private fun addManga(mangaToAdd: List<LibraryManga>) {
        val distinctManga = mangaToAdd.filter { it !in mangaToUpdate }
        if (distinctManga.isEmpty()) return

        emitScope.launch { mangaToAdd.forEach { mangaToUpdateMutableFlow.emit(it.id!!) } }

        mangaToUpdate.addAll(distinctManga)
        extraScope.launch {
            val jobs =
                distinctManga.map { manga ->
                    async(Dispatchers.IO) {
                        val shouldDownload = manga.shouldDownloadNewChapters(db, preferences)
                        val hasDLs = updateMangaChapters(manga, shouldDownload)

                        if (hasDLs && !hasDownloads) {
                            hasDownloads = true
                        }
                        return@async hasDLs
                    }
                }

            extraDeferredJobs.addAll(jobs)
        }
    }

    companion object {
        const val TAG = "LibraryUpdate"
        private const val WORK_NAME_AUTO = "LibraryUpdate-auto"
        private const val WORK_NAME_MANUAL = "LibraryUpdate-manual"

        /** Key for category to update. */
        const val KEY_CATEGORY = "category"
        const val STARTING_UPDATE_SOURCE = -5L
        /** Key for list of manga to be updated. (For dynamic categories) */
        const val KEY_MANGA_LIST = "mangaList"

        private var instance: WeakReference<LibraryUpdateJob>? = null

        val updateMutableFlow =
            MutableSharedFlow<Long?>(
                extraBufferCapacity = 10,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val updateFlow = updateMutableFlow.asSharedFlow()

        private val mangaToUpdateMutableFlow =
            MutableSharedFlow<Long>(
                extraBufferCapacity = 10,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val mangaToUpdateFlow = mangaToUpdateMutableFlow.asSharedFlow()

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val libraryPreferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: libraryPreferences.updateInterval().get()
            if (interval > 0) {
                val restrictions = libraryPreferences.autoUpdateDeviceRestrictions().get()

                val networkType =
                    if (DEVICE_ONLY_ON_WIFI in restrictions) {
                        NetworkType.UNMETERED // wifi only
                    } else {
                        NetworkType.CONNECTED // Any network
                    }

                val constraints =
                    Constraints.Builder()
                        .setRequiredNetworkType(networkType)
                        .setRequiresCharging(DEVICE_CHARGING in restrictions)
                        .setRequiresBatteryNotLow(DEVICE_BATTERY_NOT_LOW in restrictions)
                        .build()

                val request =
                    PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                            interval.toLong(),
                            TimeUnit.HOURS,
                            1L,
                            TimeUnit.HOURS,
                        )
                        .addTag(TAG)
                        .addTag(WORK_NAME_AUTO)
                        .setConstraints(constraints)
                        .build()

                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        WORK_NAME_AUTO,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request,
                    )
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(WORK_NAME_AUTO)
            }
        }

        fun isRunning(context: Context): Boolean {
            val list = WorkManager.getInstance(context).getWorkInfosByTag(TAG).get()
            return list.any { it.state == WorkInfo.State.RUNNING }
        }

        fun startNow(
            context: Context,
            category: Category? = null,
            mangaToUse: List<LibraryManga>? = null,
            mangaIdsToUse: List<Long>? = null,
        ): Boolean {
            if (isRunning(context)) {
                category?.id?.let {
                    if (mangaToUse != null) {
                        instance?.get()?.addMangaToQueue(mangaToUse)
                    } else {
                        instance?.get()?.addCategory(it)
                    }
                }

                // Already running either as a scheduled or manual job
                return false
            }
            val builder = Data.Builder()

            val libraryPreferences = Injekt.injectLazy<LibraryPreferences>()
            if (mangaToUse != null) {
                builder.putBoolean(KEY_MANGA_LIST, true)
                libraryPreferences.value
                    .libraryUpdateIds()
                    .set(mangaToUse.mapNotNull { it.id }.toLongArray().joinToString("||"))
            } else if (mangaIdsToUse != null) {
                builder.putBoolean(KEY_MANGA_LIST, true)
                libraryPreferences.value
                    .libraryUpdateIds()
                    .set(mangaIdsToUse.toLongArray().joinToString("||"))
            } else {
                category?.id?.let { id -> builder.putInt(KEY_CATEGORY, id) }
            }
            val inputData = builder.build()
            val request =
                OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                    .addTag(TAG)
                    .addTag(WORK_NAME_MANUAL)
                    .setInputData(inputData)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)

            return true
        }

        fun stop(context: Context) {
            val wm = WorkManager.getInstance(context)
            val workQuery =
                WorkQuery.Builder.fromTags(listOf(TAG))
                    .addStates(listOf(WorkInfo.State.RUNNING))
                    .build()
            wm.getWorkInfos(workQuery)
                .get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)

                    // Re-enqueue cancelled scheduled work
                    if (it.tags.contains(WORK_NAME_AUTO)) {
                        setupTask(context)
                    }
                }
        }
    }
}
