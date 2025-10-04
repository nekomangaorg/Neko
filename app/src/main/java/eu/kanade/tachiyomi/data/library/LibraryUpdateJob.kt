package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.text.isDigitsOnly
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
import eu.kanade.tachiyomi.util.chapter.getVolumeNum
import eu.kanade.tachiyomi.util.chapter.mergeSorted
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.getMissingChapters
import eu.kanade.tachiyomi.util.isAvailable
import eu.kanade.tachiyomi.util.shouldDownloadNewChapters
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import eu.kanade.tachiyomi.util.system.withIOContext
import java.io.File
import java.lang.ref.WeakReference
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_BATTERY_NOT_LOW
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_CHARGING
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
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

    private var extraDeferredJobs = mutableListOf<Deferred<Any>>()

    private val extraScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val emitScope = MainScope()

    private val mangaToUpdate = mutableListOf<LibraryManga>()

    private val mangaToUpdateMap = mutableMapOf<Long, List<LibraryManga>>()

    private val categoryIds = mutableSetOf<Int>()
    // List containing new updates
    private val newUpdates = mutableMapOf<LibraryManga, Array<Chapter>>()

    // List containing failed updates
    private val failedUpdates = mutableMapOf<Manga, String?>()

    // List containing skipped updates
    private val skippedUpdates = mutableMapOf<Manga, String?>()

    val count = AtomicInteger(0)

    // Boolean to determine if user wants to automatically download new chapters.
    private val downloadNew: Boolean = preferences.downloadNewChapters().get()

    // Boolean to determine if DownloadManager has downloads
    private var hasDownloads = false

    private var requestSemaphore = Semaphore(5)

    // For updates delete removed chapters if not preference is set as well
    private val deleteRemoved by lazy { preferences.deleteRemovedChapters().get() != 1 }

    private val notifier = LibraryUpdateNotifier(context)

    override suspend fun doWork(): Result {

        if (WORK_NAME_AUTO in tags) {
            if (
                DEVICE_ONLY_ON_WIFI in libraryPreferences.autoUpdateDeviceRestrictions().get() &&
                    !context.isConnectedToWifi()
            ) {
                return Result.failure()
            }
            // Find a running manual worker. If exists, try again later
            if (instance != null) {
                return Result.retry()
            }
        }

        libraryPreferences.lastUpdateAttemptTimestamp().set(Date().time)

        tryToSetForeground()

        instance = WeakReference(this)

        val selectedScheme = libraryPreferences.updatePrioritization().get()

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

        val mangaList =
            (if (savedMangaList != null) {
                    val mangaList =
                        db.getLibraryMangaList()
                            .executeAsBlocking()
                            .filter { it.id in savedMangaList }
                            .distinctBy { it.id }
                    val categoryId = inputData.getInt(KEY_CATEGORY, -1)
                    if (categoryId > -1) {
                        addCategoryToQueue(categoryId)
                    }
                    mangaList
                } else {
                    getMangaToUpdate()
                })
                .sortedWith(LibraryUpdateRanker.rankingScheme[selectedScheme])

        return withIOContext {
            try {
                libraryPreferences.lastUpdateTimestamp().set(Date().time)
                updateMangaJob(filterMangaToUpdate(mangaList))
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

    private suspend fun sendUpdate(mangaId: Long?) {
        if (isStopped) {
            updateMutableFlow.tryEmit(mangaId)
        } else {
            emitScope.launch { updateMutableFlow.emit(mangaId) }
        }
    }

    private fun filterMangaToUpdate(mangaToAdd: List<LibraryManga>): List<LibraryManga> {
        val restrictions = libraryPreferences.autoUpdateMangaRestrictions().get()
        return mangaToAdd.filter { libraryManga ->
            when {
                LibraryPreferences.MANGA_HAS_UNREAD in restrictions && libraryManga.unread != 0 -> {
                    skippedUpdates[libraryManga] =
                        context.getString(R.string.skipped_reason_has_unread)
                    false
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
                LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ in restrictions &&
                    hasTrackWithGivenStatus(
                        libraryManga,
                        context.getString(R.string.follows_plan_to_read),
                    ) -> {
                    skippedUpdates[libraryManga] =
                        context.getString(R.string.skipped_reason_tracking_plan_to_read)
                    return@filter false
                }
                LibraryPreferences.MANGA_TRACKING_DROPPED in restrictions &&
                    hasTrackWithGivenStatus(
                        libraryManga,
                        context.getString(R.string.follows_dropped),
                    ) -> {
                    skippedUpdates[libraryManga] =
                        context.getString(R.string.skipped_reason_tracking_dropped)
                    return@filter false
                }
                LibraryPreferences.MANGA_TRACKING_ON_HOLD in restrictions &&
                    hasTrackWithGivenStatus(
                        libraryManga,
                        context.getString(R.string.follows_on_hold),
                    ) -> {
                    skippedUpdates[libraryManga] =
                        context.getString(R.string.skipped_reason_tracking_on_hold)
                    return@filter false
                }
                LibraryPreferences.MANGA_TRACKING_COMPLETED in restrictions &&
                    hasTrackWithGivenStatus(
                        libraryManga,
                        context.getString(R.string.follows_completed),
                    ) -> {
                    skippedUpdates[libraryManga] =
                        context.getString(R.string.skipped_reason_tracking_completed)
                    return@filter false
                }
                else -> true
            }
        }
    }

    private fun getMangaToUpdate(): List<LibraryManga> {
        val categoryId = inputData.getInt(KEY_CATEGORY, -1)
        return getMangaToUpdate(categoryId)
    }

    /**
     * Returns the list of manga to be updated.
     *
     * @param categoryId the category to update
     * @return a list of manga to update
     */
    private fun getMangaToUpdate(categoryId: Int): List<LibraryManga> {
        val libraryManga = db.getLibraryMangaList().executeAsBlocking()

        val listToUpdate =
            if (categoryId != -1) {
                addCategoryToQueue(categoryId)
                libraryManga.filter { it.category == categoryId }
            } else {
                val categoriesToUpdate =
                    libraryPreferences.whichCategoriesToUpdate().get().map(String::toInt)
                if (categoriesToUpdate.isNotEmpty()) {
                    addCategoriesToQueue(categoriesToUpdate)
                    libraryManga.filter { it.category in categoriesToUpdate }.distinctBy { it.id }
                } else {
                    val categories = db.getCategories().executeAsBlocking().mapNotNull { it.id } + 0
                    addCategoriesToQueue(categories)
                    libraryManga.distinctBy { it.id }
                }
            }

        val categoriesToExclude =
            libraryPreferences.whichCategoriesToExclude().get().map(String::toInt)
        val listToExclude =
            if (categoriesToExclude.isNotEmpty() && categoryId == -1) {
                libraryManga.filter { it.category in categoriesToExclude }.toSet()
            } else {
                emptySet()
            }

        return listToUpdate.minus(listToExclude)
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

        mangaToUpdate.addAll(mangaToAdd)

        mangaToUpdateMap.putAll(mangaToAdd.groupBy { it.source })

        coroutineScope {
            val results =
                mangaToUpdateMap.keys
                    .map { source ->
                        async {
                            try {
                                requestSemaphore.withPermit { updateMangaInSource(source) }
                            } catch (e: Exception) {
                                TimberKt.e(e) { "failed to update manga in source" }
                                false
                            }
                        }
                    }
                    .awaitAll()
            if (!hasDownloads) {
                hasDownloads = results.any { it }
            }
            updateReadingStatus(mangaToUpdate)
            finishUpdates()
        }
    }

    private fun hasTrackWithGivenStatus(libraryManga: LibraryManga, globalStatus: String): Boolean {
        val tracks = db.getTracks(libraryManga).executeAsBlocking()
        return tracks.any { track ->
            val status = trackManager.getService(track.sync_id)?.getGlobalStatus(track.status)
            return if (status.isNullOrBlank()) {
                false
            } else {
                status == globalStatus
            }
        }
    }

    private suspend fun updateMangaInSource(source: Long): Boolean {
        if (mangaToUpdateMap[source] == null) return false
        var currentCount = 0
        var hasDownloads = false

        while (currentCount < mangaToUpdateMap[source]!!.size) {
            val manga = mangaToUpdateMap[source]!![currentCount]
            val shouldDownload = manga.shouldDownloadNewChapters(db, preferences)
            logTimeTaken("library manga ${manga.title}") {
                if (MdUtil.getMangaUUID(manga.url).isDigitsOnly()) {
                    TimberKt.w { "Manga : ${manga.title} is not migrated to v5 skipping" }
                } else if (updateMangaChapters(manga, this.count.andIncrement, shouldDownload)) {
                    hasDownloads = true
                }
            }
            currentCount++
        }
        mangaToUpdateMap[source] = emptyList()
        return hasDownloads
    }

    private suspend fun updateMangaChapters(
        manga: LibraryManga,
        progress: Int,
        shouldDownload: Boolean,
    ): Boolean = coroutineScope {
        return@coroutineScope runCatching {
                var hasDownloads = false
                ensureActive()
                notifier.showProgressNotification(manga, progress, mangaToUpdate.size)

                var errorFromMerged = false

                val source = sourceManager.mangaDex

                val holder = withIOContext {
                    if (libraryPreferences.updateFaster().get()) {
                        MangaDetailChapterInformation(
                            null,
                            emptyList(),
                            source.fetchChapterList(manga).getOrThrow { Exception(it.message()) },
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
                                                        manga.last_chapter_number?.toFloat() &&
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
                    (listOf(holder.sChapters) + mergedList.map { it.map { pair -> pair.first } })
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
                // note: we preload the covers here so we can view everything offline if they change

                holder.sManga?.let {
                    val thumbnailUrl = manga.thumbnail_url
                    manga.copyFrom(it)
                    manga.initialized = true

                    withIOContext {
                        // dont refresh covers while using cached source
                        if (
                            manga.thumbnail_url != null && libraryPreferences.updateCovers().get()
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
                                kotlin.runCatching {
                                    db.deleteArtworkForManga(manga).executeOnIO()
                                    db.insertArtWorkList(art).executeOnIO()
                                }
                            }
                    }

                    // add mdlist tracker if manga in library has it missing
                    withIOContext {
                        val tracks = db.getTracks(manga).executeOnIO().toMutableList()

                        if (
                            tracks.isEmpty() || !tracks.any { it.sync_id == trackManager.mdList.id }
                        ) {
                            val track = trackManager.mdList.createInitialTracker(manga)
                            db.insertTrack(track).executeAsBlocking()
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
                            var chaptersToDl = newChapters.first.sortedBy { it.chapter_number }

                            if (manga.filtered_scanlators != null) {
                                //  Ignored sources, groups and uploaders
                                val toIgnore =
                                    ChapterUtil.getScanlators(manga.filtered_scanlators)
                                        .toMutableSet()

                                // only download scanlators not filtered out
                                chaptersToDl =
                                    chaptersToDl.filterNot {
                                        val scanlatorMatchAll =
                                            libraryPreferences
                                                .chapterScanlatorFilterOption()
                                                .get() == 0
                                        ChapterUtil.filterByScanlator(
                                            it.scanlator ?: "",
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
                            val dbChapters = db.getChapters(manga).executeAsBlocking()
                            val (mergedChapters, nonMergedChapters) =
                                dbChapters.partition { it.isMergedChapter() }
                            if (mangaDexLoginHelper.isLoggedIn()) {
                                statusHandler
                                    .getReadChapterIds(MdUtil.getMangaUUID(manga.url))
                                    .collect { chapterIds ->
                                        val markRead =
                                            nonMergedChapters
                                                .filter {
                                                    chapterIds.contains(it.mangadex_chapter_id)
                                                }
                                                .filter { !it.read }
                                                .map {
                                                    it.read = true
                                                    it.last_page_read = 0
                                                    it.pages_left = 0
                                                    it
                                                }
                                                .toList()
                                        db.updateChaptersProgress(markRead).executeAsBlocking()
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
                                            readChapters.contains(Pair(it.scanlator, it.url))
                                        }
                                        .filter { !it.read }
                                        .map {
                                            it.read = true
                                            it.last_page_read = 0
                                            it.pages_left = 0
                                            it
                                        }
                                        .toList()
                                db.updateChaptersProgress(markRead).executeAsBlocking()
                            }
                        }
                    }
                    launch { updateMissingChapterCount(manga) }
                }

                hasDownloads
            }
            .getOrElse { e ->
                if (e !is CancellationException) {
                    failedUpdates[manga] = e.message ?: "unknown error"
                    TimberKt.e(e) { "Failed updating: ${manga.title}" }
                }
                return@coroutineScope false
            }
    }

    private suspend fun updateMissingChapterCount(manga: LibraryManga): LibraryManga {
        val allChaps = db.getChapters(manga).executeAsBlocking()
        val missingChapters =
            allChaps.map { it.toSimpleChapter()!!.toChapterItem() }.getMissingChapters().count

        var updated = false
        if (missingChapters == null) {
            val status = updateMangaStatus(allChaps, manga)
            if (manga.status != status) {
                manga.status = status
                updated = true
            }
        }
        if (missingChapters != manga.missing_chapters) {
            manga.missing_chapters = missingChapters
            updated = true
        }

        if (updated) db.insertManga(manga).executeOnIO()
        return manga
    }

    private fun updateMangaStatus(chapters: List<Chapter>, manga: LibraryManga): Int {
        val cancelledOrCompleted =
            manga.status == SManga.PUBLICATION_COMPLETE || manga.status == SManga.CANCELLED
        if (
            cancelledOrCompleted &&
                manga.missing_chapters == null &&
                manga.last_chapter_number != null
        ) {
            val final =
                chapters
                    .filter { it.isAvailable(downloadManager, manga) }
                    .filter { getChapterNum(it)?.toInt() == manga.last_chapter_number }
                    .filter {
                        getVolumeNum(it) == manga.last_volume_number ||
                            getVolumeNum(it) == null ||
                            manga.last_volume_number == null
                    }
            if (final.isNotEmpty()) {
                return SManga.COMPLETED
            }
        }
        return manga.status
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
                        mangaList.map { libraryManga ->
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
            if (newUpdates.isNotEmpty()) {
                notifier.showResultNotification(newUpdates)
                if (downloadNew && hasDownloads) {
                    DownloadJob.start(applicationContext)
                }
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
            val skippedFile = writeErrorFile(skippedUpdates, "skipped").getUriCompat(context)
            notifier.showUpdateSkippedNotification(skippedUpdates.map { it.key.title }, skippedFile)
        }
        if (
            failedUpdates.isNotEmpty() &&
                Notifications.isNotificationChannelEnabled(
                    context,
                    Notifications.Channel.Library.Error,
                )
        ) {
            val errorFile = writeErrorFile(failedUpdates)
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
    private fun writeErrorFile(errors: Map<Manga, String?>, fileName: String = "errors"): File {
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
                        mangaList.forEach { out.write("    - ${it.title}\n") }
                    }
                }
                return file
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }

    private fun addMangaToQueue(categoryId: Int, manga: List<LibraryManga>) {
        val mangas = filterMangaToUpdate(manga).sortedBy { it.title }
        addCategoryToQueue(categoryId)
        addManga(mangas)
    }

    private fun addCategory(categoryId: Int) {
        val mangas = filterMangaToUpdate(getMangaToUpdate(categoryId)).sortedBy { it.title }
        addCategoryToQueue(categoryId)
        addManga(mangas)
    }

    private fun addCategoryToQueue(categoryId: Int) {
        if (categoryIds.add(categoryId)) {
            emitScope.launch { categoryUpdateMutableFlow.emit(categoryId) }
        }
    }

    private fun addCategoriesToQueue(categoryIds: List<Int>) {
        val newIds = categoryIds.filter { this.categoryIds.add(it) }
        if (newIds.isNotEmpty()) {
            emitScope.launch { newIds.forEach { categoryUpdateMutableFlow.emit(it) } }
        }
    }

    private fun addManga(mangaToAdd: List<LibraryManga>) {
        val distinctManga = mangaToAdd.filter { it !in mangaToUpdate }
        mangaToUpdate.addAll(distinctManga)
        distinctManga
            .groupBy { it.source }
            .forEach {
                // if added queue items is a new source not in the async list or an async list has
                // finished running
                if (mangaToUpdateMap[it.key].isNullOrEmpty()) {
                    mangaToUpdateMap[it.key] = it.value
                    extraScope.launch {
                        extraDeferredJobs.add(
                            async(Dispatchers.IO) {
                                val hasDLs =
                                    try {
                                        requestSemaphore.withPermit { updateMangaInSource(it.key) }
                                    } catch (e: Exception) {
                                        false
                                    }
                                if (!hasDownloads) {
                                    hasDownloads = hasDLs
                                }
                            }
                        )
                    }
                } else {
                    val list = mangaToUpdateMap[it.key] ?: emptyList()
                    mangaToUpdateMap[it.key] = (list + it.value)
                }
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

        private val categoryUpdateMutableFlow = MutableSharedFlow<Int>()
        val categoryUpdateFlow = categoryUpdateMutableFlow.asSharedFlow()

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val libraryPreferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: libraryPreferences.updateInterval().get()
            if (interval > 0) {
                val restrictions = libraryPreferences.autoUpdateDeviceRestrictions().get()
                val constraints =
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresCharging(DEVICE_CHARGING in restrictions)
                        .setRequiresBatteryNotLow(DEVICE_BATTERY_NOT_LOW in restrictions)
                        .build()

                val request =
                    PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                            interval.toLong(),
                            TimeUnit.HOURS,
                            10,
                            TimeUnit.MINUTES,
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

        fun categoryInQueue(id: Int?) = instance?.get()?.categoryIds?.contains(id) ?: false

        fun startNow(
            context: Context,
            category: Category? = null,
            mangaToUse: List<LibraryManga>? = null,
            mangaIdsToUse: List<Long>? = null,
        ): Boolean {
            if (isRunning(context)) {
                category?.id?.let {
                    if (mangaToUse != null) {
                        instance?.get()?.addMangaToQueue(it, mangaToUse)
                    } else {
                        instance?.get()?.addCategory(it)
                    }
                }

                // Already running either as a scheduled or manual job
                return false
            }
            val builder = Data.Builder()
            category?.id?.let { id -> builder.putInt(KEY_CATEGORY, id) }

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
