package eu.kanade.tachiyomi.ui.manga

import android.os.Build
import android.os.Environment
import androidx.compose.ui.state.ToggleableState
import androidx.core.text.isDigitsOnly
import com.crazylegend.string.isNotNullOrEmpty
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.matchingTrack
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DownloadAction
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SnackbarState
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortOption
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.No
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.Yes
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingSuggestedDates
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.chapter.updateTrackChapterMarkedAsRead
import eu.kanade.tachiyomi.util.getMissingCount
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.MergeManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date

class MangaDetailPresenter(
    private val mangaId: Long,
    val preferences: PreferencesHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    chapterItemFilter: ChapterItemFilter = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val statusHandler: StatusHandler = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val mangaUpdateCoordinator: MangaUpdateCoordinator = Injekt.get(),
    private val trackingCoordinator: TrackingCoordinator = Injekt.get(),
) : BaseCoroutinePresenter<MangaDetailController>(), DownloadQueue.DownloadListener, LibraryServiceListener {

    private val _currentManga = MutableStateFlow(db.getManga(mangaId).executeAsBlocking()!!)
    val manga: StateFlow<Manga> = _currentManga.asStateFlow()

    private val _mangaScreenState = MutableStateFlow(getInitialMangaScreenState())
    val mangaScreenState: StateFlow<MangaConstants.MangaScreenState> = _mangaScreenState.asStateFlow()

    private val _allCategories = MutableStateFlow(emptyList<Category>())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    private val _mangaCategories = MutableStateFlow(emptyList<Category>())
    val mangaCategories: StateFlow<List<Category>> = _mangaCategories.asStateFlow()

    private val _loggedInTrackingService = MutableStateFlow(emptyList<TrackService>())
    val loggedInTrackingService: StateFlow<List<TrackService>> = _loggedInTrackingService.asStateFlow()

    private val _tracks = MutableStateFlow(emptyList<Track>())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _trackSearchResult = MutableStateFlow<TrackSearchResult>(TrackSearchResult.Loading)
    val trackSearchResult: StateFlow<TrackSearchResult> = _trackSearchResult.asStateFlow()

    private val _externalLinks = MutableStateFlow(emptyList<ExternalLink>())
    val externalLinks: StateFlow<List<ExternalLink>> = _externalLinks.asStateFlow()

    private val _mergeSearchResult = MutableStateFlow<MergeSearchResult>(MergeSearchResult.Loading)
    val mergeSearchResult: StateFlow<MergeSearchResult> = _mergeSearchResult.asStateFlow()

    private val _removedChapters = MutableStateFlow(emptyList<ChapterItem>())
    val removedChapters: StateFlow<List<ChapterItem>> = _removedChapters.asStateFlow()

    private val _nextUnreadChapter = MutableStateFlow(NextUnreadChapter())
    val nextUnreadChapter: StateFlow<NextUnreadChapter> = _nextUnreadChapter.asStateFlow()

    private val _snackbarState = MutableSharedFlow<SnackbarState>()
    val snackBarState: SharedFlow<SnackbarState> = _snackbarState.asSharedFlow()

    private var allChapterScanlators: Set<String> = emptySet()

    private val _allChapters = MutableStateFlow<List<ChapterItem>>(emptyList())
    val allChapters: StateFlow<List<ChapterItem>> = _allChapters.asStateFlow()

    private val _activeChapters = MutableStateFlow<List<ChapterItem>>(emptyList())
    val activeChapters: StateFlow<List<ChapterItem>> = _activeChapters.asStateFlow()

    private val _chapterSortFilter = MutableStateFlow(getSortFilter())
    val chapterSortFilter: StateFlow<MangaConstants.SortFilter> = _chapterSortFilter.asStateFlow()

    private val _chapterFilter = MutableStateFlow(getFilter())
    val chapterFilter: StateFlow<MangaConstants.Filter> = _chapterFilter.asStateFlow()

    private val _scanlatorFilter = MutableStateFlow(getScanlatorFilter())
    val scanlatorFilter: StateFlow<MangaConstants.ScanlatorFilter> = _scanlatorFilter.asStateFlow()

    private val _hideTitlesFilter = MutableStateFlow(getHideTitlesFilter())
    val hideTitlesFilter: StateFlow<Boolean> = _hideTitlesFilter.asStateFlow()

    private val _chapterFilterText = MutableStateFlow(getFilterText())
    val chapterFilterText: StateFlow<String> = _chapterFilterText.asStateFlow()

    private val chapterSort = ChapterItemSort(chapterItemFilter, preferences)

    override fun onCreate() {
        super.onCreate()
        downloadManager.addListener(this)
        LibraryUpdateService.setListener(this)
        if (!manga.value.initialized) {
            onRefresh()
        } else {
            updateAllFlows()
            refreshTracking(false)
            syncChaptersReadStatus()
        }
    }

    /**
     * Update all flows
     */
    private fun updateAllFlows() {
        updateMangaFlow()
        updateCurrentArtworkFlow()
        updateChapterFlows()
        updateCategoryFlows()
        updateTrackingFlows(true)
        updateExternalFlows()
        updateMergeFlow()
        updateAlternativeArtworkFlow()
        updateFilterFlow()
    }

    /**
     * Refresh manga info, and chapters
     */
    fun onRefresh() {
        presenterScope.launchIO {
            if (!isOnline()) {
                _snackbarState.emit(SnackbarState(messageRes = R.string.no_network_connection))
                return@launchIO
            }

            _mangaScreenState.value = mangaScreenState.value.copy(isRefreshing = true)

            mangaUpdateCoordinator.update(manga.value, presenterScope).collect { result ->
                when (result) {
                    is MangaResult.Error -> {
                        _snackbarState.emit(SnackbarState(message = result.text, messageRes = result.id))
                        _mangaScreenState.value = mangaScreenState.value.copy(isRefreshing = false)
                    }
                    is MangaResult.Success -> {
                        updateAllFlows()
                        _mangaScreenState.value = mangaScreenState.value.copy(isRefreshing = false)
                    }
                    is MangaResult.UpdatedChapters -> {
                        updateChapterFlows()
                        syncChaptersReadStatus()
                    }
                    is MangaResult.UpdatedManga -> {
                        updateMangaFlow()
                    }
                    is MangaResult.UpdatedArtwork -> {
                        updateAlternativeArtworkFlow()
                    }
                    is MangaResult.ChaptersRemoved -> {
                        val removedChapters = allChapters.value.filter {
                            it.chapter.id in result.chapterIdsRemoved && it.isDownloaded
                        }

                        if (removedChapters.isNotEmpty()) {
                            when (preferences.deleteRemovedChapters().get()) {
                                2 -> deleteChapters(removedChapters)
                                1 -> Unit
                                else -> {
                                    _removedChapters.value = removedChapters
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * Updates the database with categories for the manga
     */
    fun updateMangaCategories(enabledCategories: List<Category>) {
        presenterScope.launchIO {
            val categories = enabledCategories.map { MangaCategory.create(manga.value, it) }
            db.setMangaCategories(categories, listOf(manga.value))
            updateCategoryFlows()
        }
    }

    /**
     * Add New Category
     */
    fun addNewCategory(newCategory: String) {
        presenterScope.launchIO {
            val category = Category.create(newCategory)
            db.insertCategory(category).executeAsBlocking()
            updateCategoryFlows()
        }
    }

    /**
     * Update tracker with new status
     */
    fun updateTrackStatus(statusIndex: Int, trackAndService: TrackAndService) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackStatus(statusIndex, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /**
     * Update tracker with new score
     */
    fun updateTrackScore(scoreIndex: Int, trackAndService: TrackAndService) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackScore(scoreIndex, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /**
     * Update the tracker with the new chapter information
     */
    fun updateTrackChapter(newChapterNumber: Int, trackAndService: TrackAndService) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackChapter(newChapterNumber, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /**
     * Handle the TrackingUpdate
     */
    private suspend fun handleTrackingUpdate(trackingUpdate: TrackingUpdate, updateTrackFlows: Boolean = true) {
        when (trackingUpdate) {
            is TrackingUpdate.Error -> {
                XLog.e("Error", trackingUpdate.exception)
                _snackbarState.emit(SnackbarState(message = trackingUpdate.message))
            }
            is TrackingUpdate.Success -> {
                if (updateTrackFlows) {
                    updateTrackingFlows()
                }
            }
        }
    }

    /** Figures out the suggested reading dates
     */
    private fun getSuggestedDate() {
        presenterScope.launchIO {
            val chapters = db.getHistoryByMangaId(mangaId).executeOnIO()

            _mangaScreenState.value = mangaScreenState.value.copy(
                trackingSuggestedDates = TrackingSuggestedDates(
                    startDate = chapters.minOfOrNull { it.last_read } ?: 0L,
                    finishedDate = chapters.maxOfOrNull { it.last_read } ?: 0L,
                ),
            )

        }
    }

    /**
     * Update the tracker with the start/finished date
     */
    fun updateTrackDate(trackDateChange: TrackDateChange) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackDate(trackDateChange)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /**
     * Refresh tracking from trackers
     */
    private fun refreshTracking(showOfflineSnack: Boolean = false) {
        presenterScope.launchIO {
            if (!isOnline()) {
                if (showOfflineSnack) {
                    _snackbarState.emit(SnackbarState(messageRes = R.string.no_network_connection))
                }
                return@launchIO
            }
            //add a slight delay in case the tracking flow is slower

            var count = 0
            while (count < 5 && tracks.value.isEmpty()) {
                delay(1000)
                count++
            }

            val asyncList = tracks.value
                .mapNotNull { track ->
                    val service = trackManager.services.find { it.id == track.sync_id }
                    if (service == null) {
                        XLog.e("Error finding track service for track.sync_id ${track.sync_id}")
                        null
                    } else {
                        TrackingConstants.TrackItem(track, service)
                    }
                }
                .filter { it.service.isLogged() }.map { item ->
                    async(Dispatchers.IO) {
                        kotlin.runCatching { item.service.refresh(item.track!!) }.onFailure {
                            XLog.e("error refreshing tracker", it)
                            delay(3000)
                            _snackbarState.emit(SnackbarState(message = it.message, fieldRes = item.service.nameRes(), messageRes = R.string.error_refreshing_))
                        }
                    }
                }
            asyncList.awaitAll()
            updateTrackingFlows(false)

        }
    }

    private fun syncChaptersReadStatus() {
        presenterScope.launchIO {
            if (!preferences.readingSync() || !sourceManager.getMangadex().isLogged() || !isOnline()) return@launchIO

            runCatching {
                statusHandler.getReadChapterIds(MdUtil.getMangaUUID(manga.value.url)).collect { chapterIds ->
                    val chaptersToMarkRead = allChapters.value.asSequence().filter { !it.chapter.isMergedChapter() }
                        .filter { chapterIds.contains(it.chapter.mangaDexChapterId) }
                        .toList()
                    if (chaptersToMarkRead.isNotEmpty()) {
                        markChapters(chaptersToMarkRead, MangaConstants.MarkAction.Read(), skipSync = true)
                    }
                }
            }.onFailure {
                XLog.e("Error trying to mark chapters read from MangaDex", it)
                presenterScope.launch {
                    delay(3000)
                    _snackbarState.emit(SnackbarState("Error trying to mark chapters read from MangaDex $it"))
                }

            }

        }
    }

    /**
     * Search Tracker
     */
    fun searchTracker(title: String, service: TrackService) {
        presenterScope.launchIO {
            val previouslyTracked = _tracks.value.firstOrNull { service.matchingTrack(it) } != null
            trackingCoordinator.searchTracker(title, service, manga.value, previouslyTracked).collect { result ->
                _trackSearchResult.value = result
            }
        }
    }

    /**
     * Register tracker with service
     */
    fun registerTracking(trackAndService: TrackAndService, skipTrackFlowUpdate: Boolean = false) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.registerTracking(trackAndService, mangaId)
            handleTrackingUpdate(trackingUpdate, !skipTrackFlowUpdate)
        }
    }

    /**
     * Remove a tracker with an option to remove it from the tracking service
     */
    fun removeTracking(alsoRemoveFromTracker: Boolean, service: TrackService) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.removeTracking(alsoRemoveFromTracker, service, mangaId)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /**
     * share the cover that is written in the destination folder.  If a url is  passed in then share that one instead of the
     * manga thumbnail url one
     */
    suspend fun shareMangaCover(destDir: File, artwork: Artwork): File? {
        return withIOContext {
            return@withIOContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                destDir.deleteRecursively()
                try {
                    saveCover(destDir, artwork)
                } catch (e: java.lang.Exception) {
                    XLog.e("warn", e)
                    null
                }
            } else {
                //returns null because before Q, the share sheet can't show the cover
                null
            }

        }
    }

    /**
     * Save the given url cover to file
     */
    fun saveCover(artwork: Artwork) {
        presenterScope.launchIO {
            try {
                val directory = File(
                    Environment.getExternalStorageDirectory().absolutePath +
                        File.separator + Environment.DIRECTORY_PICTURES +
                        File.separator + preferences.context.getString(R.string.app_name),
                )
                saveCover(directory, artwork)
                launchUI {
                    controller?.applicationContext?.toast(R.string.cover_saved)
                }
            } catch (e: Exception) {
                XLog.e("error saving cover", e)
                launchUI {
                    controller?.applicationContext?.toast("Error saving cover")
                }
            }
        }
    }

    /**
     * Save Cover to directory, if given a url save that specific cover
     */
    private fun saveCover(directory: File, artwork: Artwork): File {
        val cover = if (artwork.url.isBlank()) {
            coverCache.getCustomCoverFile(manga.value).takeIf { it.exists() } ?: coverCache.getCoverFile(manga.value.thumbnail_url, manga.value.favorite)
        } else {
            coverCache.getCoverFile(artwork.url)
        }
        val type = ImageUtil.findImageType(cover.inputStream())
            ?: throw Exception("Not an image")

        directory.mkdirs()

        // Build destination file.
        val fileNameNoExtension = listOfNotNull(
            manga.value.title,
            artwork.volume.ifEmpty { null },
            MdUtil.getMangaUUID(manga.value.url),
        ).joinToString("-")

        val filename = DiskUtil.buildValidFilename("${fileNameNoExtension}.${type.extension}")

        val destFile = File(directory, filename)
        cover.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    /**
     * Set custom cover
     */
    fun setCover(artwork: Artwork) {
        presenterScope.launchIO {
            coverCache.setCustomCoverToCache(manga.value, artwork.url)
            MangaCoverMetadata.remove(mangaId)
            val manga = manga.value
            manga.user_cover = artwork.url
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateCurrentArtworkFlow()
            updateAlternativeArtworkFlow()
        }
    }

    /**
     * Reset cover
     */
    fun resetCover() {
        presenterScope.launchIO {
            coverCache.deleteCustomCover(manga.value)
            MangaCoverMetadata.remove(mangaId)
            val manga = manga.value
            manga.user_cover = null
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateCurrentArtworkFlow()
            updateAlternativeArtworkFlow()
        }
    }

    /**
     * Set custom title or resets if given null
     */
    fun setAltTitle(title: String?) {
        presenterScope.launchIO {
            val previousTitle = mangaScreenState.value.currentTitle
            val newTitle = title ?: manga.value.originalTitle
            _mangaScreenState.value = mangaScreenState.value.copy(currentTitle = newTitle)

            val manga = manga.value
            manga.user_title = title
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()

            _snackbarState.emit(
                SnackbarState(
                    messageRes = R.string.updated_title_to_,
                    message = newTitle,
                    actionLabelRes = R.string.undo,
                    action = {
                        setAltTitle(previousTitle)
                    },
                ),
            )
        }
    }

    /**
     * Remove merged manga entry
     */
    fun removeMergedManga() {
        presenterScope.launchIO {

            val editManga = manga.value
            editManga.apply {
                merge_manga_url = null
            }
            db.insertManga(editManga).executeOnIO()
            updateMangaFlow()

            val mergedChapters =
                db.getChapters(manga.value).executeOnIO().filter { it.isMergedChapter() }

            downloadManager.deleteChapters(mergedChapters, manga.value, sourceManager.getMangadex())
            db.deleteChapters(mergedChapters).executeOnIO()
            updateAllFlows()
        }
    }

    fun searchMergedManga(query: String) {
        presenterScope.launchIO {
            _mergeSearchResult.value = MergeSearchResult.Loading

            runCatching {
                val mergedMangaResults = sourceManager.getMergeSource()
                    .searchManga(query)
                    .map { MergeManga(thumbnail = it.thumbnail_url ?: "", title = it.title, url = it.url) }
                _mergeSearchResult.value = when (mergedMangaResults.isEmpty()) {
                    true -> MergeSearchResult.NoResult
                    false -> MergeSearchResult.Success(mergedMangaResults)
                }
            }.getOrElse {
                _mergeSearchResult.value = MergeSearchResult.Error(it.message ?: "Error looking up information")
            }
        }
    }

    /**
     * Attach the selected merge manga entry
     */
    fun addMergedManga(mergeManga: MergeManga) {
        presenterScope.launchIO {
            val editManga = manga.value
            editManga.apply {
                merge_manga_url = mergeManga.url
                merge_manga_image_url = merge_manga_image_url
            }
            db.insertManga(editManga).executeOnIO()
            updateMangaFlow()
            onRefresh()
        }
    }

    /**
     * Updates the artwork flow
     */
    private fun updateCurrentArtworkFlow() {
        presenterScope.launchIO {
            _mangaScreenState.value = mangaScreenState.value.copy(
                currentArtwork = Artwork(url = manga.value.user_cover ?: "", inLibrary = manga.value.favorite, originalArtwork = manga.value.thumbnail_url ?: "", mangaId = mangaId),
            )
        }
    }

    private fun updateVibrantColorFlow() {
        presenterScope.launch {
            _mangaScreenState.value = mangaScreenState.value.copy(vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId))
        }
    }

    /**
     * Updates the visible chapters for a manga
     */
    private fun updateChapterFlows() {
        presenterScope.launchIO {
            //possibly move this into a chapter repository
            val allChapters = db.getChapters(mangaId).executeOnIO().mapNotNull { it.toSimpleChapter() }.map { chapter ->
                val downloadState = when {
                    downloadManager.isChapterDownloaded(chapter.toDbChapter(), manga.value) -> Download.State.DOWNLOADED
                    downloadManager.hasQueue() -> downloadManager.queue.find { it.chapter.id == chapter.id }?.status ?: Download.State.default
                    else -> Download.State.default
                }

                ChapterItem(
                    chapter = chapter,
                    downloadState = downloadState,
                    downloadProgress = when (downloadState == Download.State.DOWNLOADING) {
                        true -> downloadManager.queue.find { it.chapter.id == chapter.id }!!.progressFloat
                        false -> 0f
                    },
                )

            }
            _allChapters.value = allChapters

            val needToRefreshFilters = allChapterScanlators.isEmpty()

            allChapterScanlators = allChapters.flatMap { ChapterUtil.getScanlators(it.chapter.scanlator) }.toSet()
            if (allChapterScanlators.size == 1 && manga.value.filtered_scanlators.isNotNullOrEmpty()) {
                updateMangaScanlator(emptySet())
            }
            //this is only really needed on initial load since all chapter scanlators is empty and the scanlator filter sheet would be also
            if (needToRefreshFilters) {
                updateFilterFlow()
            }

            _activeChapters.value = chapterSort.getChaptersSorted(manga.value, allChapters)

            //do this after so the texts gets updated
            updateNextUnreadChapter()
            updateMissingChapters()
        }
    }

    /**
     * Updates the filtered scanlators
     */
    private fun updateMangaScanlator(filteredScanlators: Set<String>) {
        presenterScope.launchIO {
            val manga = manga.value
            manga.filtered_scanlators = when (filteredScanlators.isEmpty()) {
                true -> null
                false -> ChapterUtil.getScanlatorString(filteredScanlators)
            }

            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateFilterFlow()

        }
    }

    /**
     * Updates the flows for all categories, and manga categories
     */
    private fun updateCategoryFlows() {
        presenterScope.launchIO {
            _allCategories.value = db.getCategories().executeOnIO()
            _mangaCategories.value = db.getCategoriesForManga(mangaId).executeOnIO()
        }
    }

    /**
     * Update flows for tracking
     */
    private fun updateTrackingFlows(checkForMissingTrackers: Boolean = false) {
        presenterScope.launchIO {

            _loggedInTrackingService.value = trackManager.services.filter { it.isLogged() }
            _tracks.value = db.getTracks(mangaId).executeAsBlocking()

            if (checkForMissingTrackers) {
                val autoAddTracker = preferences.autoAddTracker().get()

                //Always add the mdlist initial unfollowed tracker, also add it as PTR if need be
                _loggedInTrackingService.value.firstOrNull { it.isMdList() }?.let { mdList ->
                    mdList as MdList

                    var track = _tracks.value.firstOrNull { mdList.matchingTrack(it) }

                    if (track == null) {
                        track = mdList.createInitialTracker(manga.value)
                        db.insertTrack(track).executeOnIO()
                        if (isOnline()) {
                            mdList.bind(track)
                        }
                        db.insertTrack(track).executeOnIO()
                    }
                    val shouldAddAsPlanToRead = manga.value.favorite && preferences.addToLibraryAsPlannedToRead() && FollowStatus.isUnfollowed(track.status)
                    if (shouldAddAsPlanToRead && isOnline()) {
                        track.status = FollowStatus.PLAN_TO_READ.int
                        trackingCoordinator.updateTrackingService(track, trackManager.mdList)
                    }
                }

                if (autoAddTracker.size > 1 && manga.value.favorite) {
                    val validContentRatings = preferences.autoTrackContentRatingSelections()
                    val contentRating = manga.value.getContentRating()
                    if (contentRating == null || validContentRatings.contains(contentRating.lowercase())) {
                        autoAddTracker.map { it.toInt() }.map { autoAddTrackerId ->
                            async {
                                _loggedInTrackingService.value.firstOrNull { it.id == autoAddTrackerId }?.let { trackService ->
                                    val id = trackManager.getIdFromManga(trackService, manga.value)
                                    if (id != null && !_tracks.value.any { trackService.matchingTrack(it) }) {
                                        if (!isOnline()) {
                                            launchUI { _snackbarState.emit(SnackbarState(message = "No network connection, cannot autolink tracker")) }
                                        } else {
                                            val trackResult = trackService.search("", manga.value, false)
                                            trackResult.firstOrNull()?.let { track ->
                                                val trackingUpdate = trackingCoordinator.registerTracking(TrackAndService(track, trackService), mangaId)
                                                handleTrackingUpdate(trackingUpdate, false)
                                            }
                                        }
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                }
                //update the tracks incase they were updated above
                _tracks.value = db.getTracks(manga.value).executeAsBlocking()
            }

            getSuggestedDate()

            val trackCount = _loggedInTrackingService.value.count { trackService ->
                _tracks.value.any { track ->
                    //return true if track matches and not MDList
                    //or track matches and MDlist is anything but Unfollowed
                    trackService.matchingTrack(track) &&
                        (trackService.isMdList().not() ||
                            (trackService.isMdList() && !FollowStatus.isUnfollowed(track.status)))
                }
            }

            _mangaScreenState.value = mangaScreenState.value.copy(trackServiceCount = trackCount)
        }
    }

    /**
     * Update flows for external links
     */
    private fun updateExternalFlows() {
        presenterScope.launchIO {
            _externalLinks.value = manga.value.getExternalLinks()
        }
    }

    /**
     * Get Manga Description
     */
    private fun getDescription(): String {
        return when {
            MdUtil.getMangaUUID(manga.value.url).isDigitsOnly() -> "THIS MANGA IS NOT MIGRATED TO V5"
            manga.value.description.isNotNullOrEmpty() -> manga.value.description!!
            else -> "No description"
        }
    }

    /**
     * Get current sort filter
     */
    private fun getSortFilter(): MangaConstants.SortFilter {
        val sortOrder = manga.value.chapterOrder(preferences)
        val status = when (manga.value.sortDescending(preferences)) {
            true -> MangaConstants.SortState.Descending
            false -> MangaConstants.SortState.Ascending
        }

        val matchesDefaults = mangaSortMatchesDefault()

        return when (sortOrder) {
            Manga.CHAPTER_SORTING_NUMBER -> MangaConstants.SortFilter(chapterNumberSort = status, matchesGlobalDefaults = matchesDefaults)
            Manga.CHAPTER_SORTING_UPLOAD_DATE -> MangaConstants.SortFilter(uploadDateSort = status, matchesGlobalDefaults = matchesDefaults)
            else -> MangaConstants.SortFilter(sourceOrderSort = status, matchesGlobalDefaults = matchesDefaults)
        }
    }

    /**
     * Get current sort filter
     */
    private fun getFilter(): MangaConstants.Filter {

        val read = when (manga.value.readFilter(preferences)) {
            Manga.CHAPTER_SHOW_UNREAD -> ToggleableState.On
            Manga.CHAPTER_SHOW_READ -> ToggleableState.Indeterminate
            else -> ToggleableState.Off
        }
        val bookmark = when (manga.value.bookmarkedFilter(preferences)) {
            Manga.CHAPTER_SHOW_BOOKMARKED -> ToggleableState.On
            Manga.CHAPTER_SHOW_NOT_BOOKMARKED -> ToggleableState.Indeterminate
            else -> ToggleableState.Off
        }

        val downloaded = when (manga.value.downloadedFilter(preferences)) {
            Manga.CHAPTER_SHOW_DOWNLOADED -> ToggleableState.On
            Manga.CHAPTER_SHOW_NOT_DOWNLOADED -> ToggleableState.Indeterminate
            else -> ToggleableState.Off
        }
        val all = read == ToggleableState.Off && bookmark == ToggleableState.Off && downloaded == ToggleableState.Off

        val matchesDefaults = mangaFilterMatchesDefault()

        return MangaConstants.Filter(showAll = all, unread = read, downloaded = downloaded, bookmarked = bookmark, matchesGlobalDefaults = matchesDefaults)
    }

    /**
     * Get scanlator filter
     */
    private fun getScanlatorFilter(): MangaConstants.ScanlatorFilter {
        val filteredScanlators = ChapterUtil.getScanlators(manga.value.filtered_scanlators).toSet()
        val scanlatorOptions = allChapterScanlators.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { it },
        )
            .map { scanlator ->
                MangaConstants.ScanlatorOption(name = scanlator, disabled = filteredScanlators.contains(scanlator))
            }
        return MangaConstants.ScanlatorFilter(scanlators = scanlatorOptions)
    }

    /**
     * Get hide titles
     */
    private fun getHideTitlesFilter(): Boolean {
        return manga.value.hideChapterTitle(preferences)
    }

    private fun getFilterText(): String {
        val filter = _chapterFilter.value
        val hasDisabledScanlators = _scanlatorFilter.value.scanlators.any { it.disabled }
        val filtersId = mutableListOf<Int?>()
        filtersId.add(if (filter.unread == ToggleableState.Indeterminate) R.string.read else null)
        filtersId.add(if (filter.unread == ToggleableState.On) R.string.unread else null)
        filtersId.add(if (filter.downloaded == ToggleableState.On) R.string.downloaded else null)
        filtersId.add(if (filter.downloaded == ToggleableState.Indeterminate) R.string.not_downloaded else null)
        filtersId.add(if (filter.bookmarked == ToggleableState.On) R.string.bookmarked else null)
        filtersId.add(if (filter.bookmarked == ToggleableState.Indeterminate) R.string.not_bookmarked else null)
        filtersId.add(if (hasDisabledScanlators) R.string.scanlators else null)
        return filtersId.filterNotNull().joinToString(", ") { preferences.context.getString(it) }
    }

    /**
     * Get change Sort option
     */
    fun changeSortOption(sortOption: SortOption?) {
        presenterScope.launchIO {
            val manga = _currentManga.value

            if (sortOption == null) {
                manga.setSortToGlobal()
            } else {

                val sortInt = when (sortOption.sortType) {
                    MangaConstants.SortType.ChapterNumber -> Manga.CHAPTER_SORTING_NUMBER
                    MangaConstants.SortType.SourceOrder -> Manga.CHAPTER_SORTING_SOURCE
                    MangaConstants.SortType.UploadDate -> Manga.CHAPTER_SORTING_UPLOAD_DATE
                }
                val descInt = when (sortOption.sortState) {
                    MangaConstants.SortState.Ascending -> Manga.CHAPTER_SORT_ASC
                    else -> Manga.CHAPTER_SORT_DESC
                }

                manga.setChapterOrder(sortInt, descInt)
            }

            db.insertManga(manga).executeAsBlocking()
            updateMangaFlow()
            updateFilterFlow()
            updateChapterFlows()
        }
    }

    /**
     * Get current merge result
     */
    fun changeFilterOption(filterOption: MangaConstants.FilterOption?) {
        presenterScope.launchIO {
            val manga = _currentManga.value

            if (!manga.usesLocalFilter && manga.readFilter(preferences) == Manga.SHOW_ALL && manga.downloadedFilter(preferences) == Manga.SHOW_ALL && manga.bookmarkedFilter(preferences) == Manga.SHOW_ALL) {
                manga.readFilter = Manga.SHOW_ALL
                manga.bookmarkedFilter = Manga.SHOW_ALL
                manga.downloadedFilter = Manga.SHOW_ALL
            }

            if (filterOption == null) {
                manga.setFilterToGlobal()
            } else {
                when (filterOption.filterType) {
                    MangaConstants.FilterType.All -> {
                        manga.readFilter = Manga.SHOW_ALL
                        manga.bookmarkedFilter = Manga.SHOW_ALL
                        manga.downloadedFilter = Manga.SHOW_ALL
                    }
                    MangaConstants.FilterType.Unread -> {
                        manga.readFilter = when (filterOption.filterState) {
                            ToggleableState.On -> Manga.CHAPTER_SHOW_UNREAD
                            ToggleableState.Indeterminate -> Manga.CHAPTER_SHOW_READ
                            else -> Manga.SHOW_ALL
                        }
                    }
                    MangaConstants.FilterType.Bookmarked -> {
                        manga.bookmarkedFilter = when (filterOption.filterState) {
                            ToggleableState.On -> Manga.CHAPTER_SHOW_BOOKMARKED
                            ToggleableState.Indeterminate -> Manga.CHAPTER_SHOW_NOT_BOOKMARKED
                            else -> Manga.SHOW_ALL
                        }
                    }
                    MangaConstants.FilterType.Downloaded -> {
                        manga.downloadedFilter = when (filterOption.filterState) {
                            ToggleableState.On -> Manga.CHAPTER_SHOW_DOWNLOADED
                            ToggleableState.Indeterminate -> Manga.CHAPTER_SHOW_NOT_DOWNLOADED
                            else -> Manga.SHOW_ALL
                        }
                    }
                }
                manga.setFilterToLocal()
                if (mangaFilterMatchesDefault()) {
                    manga.setFilterToGlobal()
                }
            }

            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateFilterFlow()
            updateChapterFlows()
        }
    }

    /**
     * Changes the filtered scanlators, if null then it resets the scanlator filter
     */
    fun changeScanlatorOption(scanlatorOption: MangaConstants.ScanlatorOption?) {
        presenterScope.launchIO {
            val manga = manga.value

            val newFilteredScanlators = if (scanlatorOption != null) {
                val filteredScanlators = ChapterUtil.getScanlators(manga.filtered_scanlators).toMutableSet()
                when (scanlatorOption.disabled) {
                    true -> filteredScanlators.add(scanlatorOption.name)
                    false -> filteredScanlators.remove(scanlatorOption.name)
                }
                filteredScanlators
            } else {
                emptySet()
            }

            updateMangaScanlator(newFilteredScanlators)
            updateChapterFlows()

        }
    }

    /**
     * Hides/Shows the titles of the chapters
     */
    fun hideTitlesOption(hide: Boolean) {
        presenterScope.launchIO {
            val manga = manga.value

            manga.displayMode = if (hide) Manga.CHAPTER_DISPLAY_NUMBER else Manga.CHAPTER_DISPLAY_NAME
            manga.setFilterToLocal()
            if (mangaFilterMatchesDefault()) {
                manga.setFilterToGlobal()
            }
            db.updateChapterFlags(manga).executeAsBlocking()
            updateMangaFlow()
            updateFilterFlow()
            updateChapterFlows()

        }
    }

    /**
     * Changes the filtered scanlators, if null then it resets the scanlator filter
     */
    fun setGlobalOption(option: MangaConstants.SetGlobal) {
        presenterScope.launchIO {
            val manga = manga.value
            when (option) {
                MangaConstants.SetGlobal.Sort -> {
                    preferences.sortChapterOrder().set(manga.sorting)
                    preferences.chaptersDescAsDefault().set(manga.sortDescending)
                    manga.setSortToGlobal()
                }
                MangaConstants.SetGlobal.Filter -> {
                    preferences.filterChapterByRead().set(manga.readFilter)
                    preferences.filterChapterByDownloaded().set(manga.downloadedFilter)
                    preferences.filterChapterByBookmarked().set(manga.bookmarkedFilter)
                    manga.setFilterToGlobal()
                }
                else -> Unit
            }
            db.insertManga(manga).executeAsBlocking()
            updateMangaFlow()
            updateFilterFlow()
        }
    }

    private fun mangaSortMatchesDefault(): Boolean {
        return (
            manga.value.sortDescending == preferences.chaptersDescAsDefault().get() &&
                manga.value.sorting == preferences.sortChapterOrder().get()
            ) || !manga.value.usesLocalSort
    }

    private fun mangaFilterMatchesDefault(): Boolean {
        return (
            manga.value.readFilter == preferences.filterChapterByRead().get() &&
                manga.value.downloadedFilter == preferences.filterChapterByDownloaded().get() &&
                manga.value.bookmarkedFilter == preferences.filterChapterByBookmarked().get() &&
                manga.value.hideChapterTitles == preferences.hideChapterTitlesByDefault().get()
            ) || !manga.value.usesLocalFilter
    }

    /**
     * Get current merge result
     */
    private fun getIsMergedManga(): IsMergedManga {
        return when (manga.value.isMerged()) {
            true -> Yes(sourceManager.getMergeSource().baseUrl + manga.value.merge_manga_url!!, manga.value.title)
            false -> No
        }
    }

    /**
     * Update flows for merge
     */
    private fun updateMergeFlow() {
        presenterScope.launch {
            _mangaScreenState.value = mangaScreenState.value.copy(isMergedManga = getIsMergedManga())
        }
    }

    /**
     * Update the current artwork with the vibrant color
     */
    fun updateMangaColor(vibrantColor: Int) {
        MangaCoverMetadata.addVibrantColor(mangaId, vibrantColor)
        updateVibrantColorFlow()
    }

    /**
     * Update flows for merge
     */
    private fun updateAlternativeArtworkFlow() {
        presenterScope.launchIO {
            val uuid = MdUtil.getMangaUUID(manga.value.url)
            val quality = preferences.thumbnailQuality()
            val currentUsed = mangaScreenState.value.currentArtwork

            val altArtwork = db.getArtwork(manga.value).executeAsBlocking().map { aw ->
                Artwork(
                    mangaId = aw.mangaId,
                    url = MdUtil.cdnCoverUrl(uuid, aw.fileName, quality),
                    volume = aw.volume,
                    description = aw.description,
                    active = currentUsed.url.contains(aw.fileName) || (currentUsed.url.isBlank() && currentUsed.originalArtwork.contains(aw.fileName)),
                )
            }

            _mangaScreenState.value = mangaScreenState.value.copy(alternativeArtwork = altArtwork.toImmutableList())

        }
    }

    /**
     * Update flows for manga
     */
    private fun updateMangaFlow() {
        presenterScope.launchIO {
            _currentManga.value = db.getManga(mangaId).executeOnIO()!!
            _mangaScreenState.value =
                mangaScreenState.value.copy(currentTitle = manga.value.title, alternativeTitles = manga.value.getAltTitles().toImmutableList(), currentDescription = getDescription())
        }
    }

    /**
     * Update filterflow
     */
    private fun updateFilterFlow() {
        presenterScope.launchIO {
            _chapterSortFilter.value = getSortFilter()
            _chapterFilter.value = getFilter()
            _hideTitlesFilter.value = getHideTitlesFilter()
            _scanlatorFilter.value = getScanlatorFilter()
            _chapterFilterText.value = getFilterText()
        }
    }

    /**
     * Toggle a manga as favorite
     * TODO rework this
     */
    fun toggleFavorite(shouldAddToDefaultCategory: Boolean): Boolean {

        val editManga = manga.value
        editManga.apply {
            favorite = !favorite
            date_added = when (favorite) {
                true -> Date().time
                false -> 0
            }
        }
        presenterScope.launch {
            db.insertManga(editManga).executeAsBlocking()
            updateMangaFlow()
            //add to the default category if it exists and the user has the option set
            if (shouldAddToDefaultCategory && mangaScreenState.value.hasDefaultCategory) {
                val defaultCategoryId = preferences.defaultCategory()
                _allCategories.value.firstOrNull { defaultCategoryId == it.id }?.let {
                    updateMangaCategories(listOf(it))
                }
            }
            if (editManga.favorite) {
                //this is called for the add as plan to read/auto sync tracking,
                updateTrackingFlows(true)
            }
        }

        return editManga.favorite
    }

    /**
     * Delete the list of chapters
     */
    fun deleteChapters(chapterItems: List<ChapterItem>, isEverything: Boolean = false, canUndo: Boolean = false) {
        //do on global scope cause we don't want exiting the manga to prevent the deleting

        launchIO {
            if (chapterItems.isNotEmpty()) {
                val delete = {
                    if (isEverything) {
                        downloadManager.deleteManga(manga.value, sourceManager.getMangadex())
                    } else {
                        downloadManager.deleteChapters(chapterItems.map { it.chapter.toDbChapter() }, manga.value, sourceManager.getMangadex())
                    }
                }
                if (canUndo) {
                    _snackbarState.emit(
                        SnackbarState(
                            messageRes = R.string.deleted_downloads, actionLabelRes = R.string.undo,
                            action = {
                            },
                            dismissAction = {
                                delete()
                            },
                        ),
                    )
                } else {
                    delete()
                }
            } else {
                _snackbarState.emit(
                    SnackbarState(
                        messageRes = R.string.no_chapters_to_delete,
                    ),
                )
            }
        }

        updateChapterFlows()
    }

    /**
     * Checks if a manga is favorited, if not then snack action to add to library
     */
    private fun addToLibrarySnack() {
        if (!manga.value.favorite) {
            presenterScope.launch {
                _snackbarState.emit(
                    SnackbarState(
                        messageRes = R.string.add_to_library,
                        actionLabelRes = R.string.add,
                        action = {
                            toggleFavorite(true)
                        },
                    ),
                )
            }
        }
    }

    /**
     * Delete the list of chapters
     */
    fun downloadChapters(chapterItems: List<ChapterItem>, downloadAction: DownloadAction) {
        presenterScope.launchIO {
            when (downloadAction) {
                is DownloadAction.ImmediateDownload -> {
                    addToLibrarySnack()
                    downloadManager.startDownloadNow(chapterItems.first().chapter.toDbChapter())
                }
                is DownloadAction.DownloadAll -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(manga.value, activeChapters.value.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() })
                }
                is DownloadAction.Download -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(manga.value, chapterItems.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() })
                }
                is DownloadAction.DownloadNextUnread -> {
                    val filteredChapters =
                        activeChapters.value.filter { !it.chapter.read && !it.isDownloaded }.sortedWith(chapterSort.sortComparator(manga.value, true)).take(downloadAction.numberToDownload)
                            .map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(manga.value, filteredChapters)
                }
                is DownloadAction.DownloadUnread -> {
                    val filteredChapters =
                        activeChapters.value.filter { !it.chapter.read && !it.isDownloaded }.sortedWith(chapterSort.sortComparator(manga.value, true)).map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(manga.value, filteredChapters)
                }
                is DownloadAction.Remove -> deleteChapters(chapterItems, chapterItems.size == allChapters.value.size)
                is DownloadAction.RemoveAll -> deleteChapters(activeChapters.value.filter { it.isNotDefaultDownload }, activeChapters.value.size == allChapters.value.size, true)
                is DownloadAction.RemoveRead -> {
                    val filteredChapters = activeChapters.value.filter { it.chapter.read && it.isDownloaded }
                    deleteChapters(filteredChapters, filteredChapters.size == allChapters.value.size, true)
                }
                is DownloadAction.Cancel -> deleteChapters(chapterItems, chapterItems.size == allChapters.value.size)
            }
        }
    }

    fun markChapters(chapterItems: List<ChapterItem>, markAction: MangaConstants.MarkAction, skipSync: Boolean = false) {
        presenterScope.launchIO {

            val initialChapterItems = if (markAction is MangaConstants.MarkAction.PreviousRead || markAction is MangaConstants.MarkAction.PreviousUnread) {
                when (manga.value.sortDescending(preferences)) {
                    true -> (markAction as? MangaConstants.MarkAction.PreviousRead)?.altChapters ?: (markAction as MangaConstants.MarkAction.PreviousUnread).altChapters
                    false -> chapterItems
                }
            } else {
                chapterItems
            }


            val (newChapterItems, nameRes) = when (markAction) {
                is MangaConstants.MarkAction.Bookmark -> {
                    initialChapterItems.map { it.chapter }.map { it.copy(bookmark = true) } to R.string.bookmarked
                }
                is MangaConstants.MarkAction.UnBookmark -> {
                    initialChapterItems.map { it.chapter }.map { it.copy(bookmark = false) } to R.string.removed_bookmark
                }
                is MangaConstants.MarkAction.Read -> {
                    initialChapterItems.map { it.chapter }.map { it.copy(read = true) } to R.string.marked_as_read
                }

                is MangaConstants.MarkAction.PreviousRead -> {
                    initialChapterItems.map { it.chapter }.map { it.copy(read = true) } to R.string.marked_as_read
                }
                is MangaConstants.MarkAction.PreviousUnread -> {
                    initialChapterItems.map { it.chapter }.map {
                        it.copy(read = false, lastPageRead = 0, pagesLeft = 0)
                    } to R.string.marked_as_unread
                }
                is MangaConstants.MarkAction.Unread -> {
                    initialChapterItems.map { it.chapter }.map {
                        it.copy(read = false, lastPageRead = markAction.lastRead ?: 0, pagesLeft = markAction.pagesLeft ?: 0)
                    } to R.string.marked_as_unread
                }
            }

            db.updateChaptersProgress(newChapterItems.map { it.toDbChapter() }).executeOnIO()
            updateChapterFlows()


            fun finalizeChapters() {
                if (markAction is MangaConstants.MarkAction.Read) {
                    if (preferences.removeAfterMarkedAsRead()) {
                        //dont delete bookmarked chapters
                        deleteChapters(newChapterItems.filter { !it.bookmark }.map { ChapterItem(chapter = it) }, newChapterItems.size == allChapters.value.size)
                    }
                    //get the highest chapter number and update tracking for it
                    newChapterItems.maxByOrNull { it.chapterNumber.toInt() }?.let {
                        kotlin.runCatching {
                            updateTrackChapterMarkedAsRead(db, preferences, it.toDbChapter(), mangaId) {
                                updateTrackingFlows()
                            }
                        }.onFailure {
                            XLog.e("Failed to update track chapter marked as read", it)
                            presenterScope.launch {
                                _snackbarState.emit(SnackbarState("Error trying to update tracked chapter marked as read ${it.message}"))
                            }
                        }

                    }
                }

                //sync with dex if marked read or marked unread
                val syncRead = when (markAction) {
                    is MangaConstants.MarkAction.Read -> true
                    is MangaConstants.MarkAction.Unread -> false
                    else -> null
                }

                if (syncRead != null && !skipSync && preferences.readingSync()) {
                    val chapterIds = newChapterItems.filter { !it.isMergedChapter() }.map { it.mangaDexChapterId }
                    if (chapterIds.isNotEmpty()) {
                        GlobalScope.launchIO {
                            statusHandler.marksChaptersStatus(
                                MdUtil.getMangaUUID(manga.value.url),
                                chapterIds,
                                syncRead,
                            )
                        }
                    }
                }
            }

            if (markAction.canUndo) {
                _snackbarState.emit(
                    SnackbarState(
                        messageRes = nameRes, actionLabelRes = R.string.undo,
                        action = {
                            presenterScope.launch {
                                val originalDbChapters = initialChapterItems.map { it.chapter }.map { it.toDbChapter() }
                                db.updateChaptersProgress(originalDbChapters).executeOnIO()
                                updateChapterFlows()
                            }
                        },
                        dismissAction = {
                            finalizeChapters()
                        },
                    ),
                )
            } else {
                finalizeChapters()
            }
        }
    }

    /**
     * clears the removedChapter flow
     */
    fun clearRemovedChapters() {
        _removedChapters.value = emptyList()
    }

    /**
     * Get Quick read text for the button
     */
    private fun updateNextUnreadChapter() {
        presenterScope.launchIO {
            val nextChapter = chapterSort.getNextUnreadChapter(manga.value, activeChapters.value)?.chapter
            _nextUnreadChapter.value = when (nextChapter == null) {
                true -> NextUnreadChapter()
                false -> {
                    val id = when (nextChapter.lastPageRead > 0) {
                        true -> R.string.continue_reading_
                        false -> R.string.start_reading_
                    }
                    val readTxt =
                        if (nextChapter.isMergedChapter() || (nextChapter.volume.isEmpty() && nextChapter.chapterText.isEmpty())) {
                            nextChapter.name
                        } else if (nextChapter.volume.isNotEmpty()) {
                            "Vol. " + nextChapter.volume + " " + nextChapter.chapterText
                        } else {
                            nextChapter.chapterText
                        }

                    NextUnreadChapter(id, readTxt, nextChapter)
                }
            }
        }
    }

    /**
     * updates the missing chapter count on a manga if needed
     */
    private fun updateMissingChapters() {
        presenterScope.launchIO {
            val currentMissingChapters = allChapters.value.getMissingCount(manga.value.status)
            if (currentMissingChapters != manga.value.missing_chapters) {
                val editManga = manga.value
                editManga.apply {
                    this.missing_chapters = currentMissingChapters
                }
                db.insertManga(editManga)
                updateMangaFlow()
            }
        }
    }

    //callback from Downloader
    override fun updateDownload(download: Download) {
        presenterScope.launchIO {
            val currentChapters = activeChapters.value
            val index = currentChapters.indexOfFirst { it.chapter.id == download.chapter.id }
            if (index >= 0) {
                val mutableChapters = currentChapters.toMutableList()
                val updateChapter = currentChapters[index].copy(downloadState = download.status, downloadProgress = download.progressFloat)
                mutableChapters[index] = updateChapter
                _activeChapters.value = mutableChapters.toList()
            }
        }
    }

    private fun getInitialMangaScreenState(): MangaConstants.MangaScreenState {
        val manga = manga.value
        return MangaConstants.MangaScreenState(
            alternativeArtwork = persistentListOf(),
            currentArtwork = Artwork(
                url = manga.user_cover ?: "",
                mangaId = mangaId,
                inLibrary = manga.favorite,
                originalArtwork = manga.thumbnail_url ?: "",
            ),
            currentDescription = getDescription(),
            currentTitle = manga.title,
            hasDefaultCategory = preferences.defaultCategory() != -1,
            hideButtonText = preferences.hideButtonText().get(),
            isMergedManga = getIsMergedManga(),
            isRefreshing = false,
            originalTitle = manga.originalTitle,
            alternativeTitles = manga.getAltTitles().toImmutableList(),
            trackServiceCount = 0,
            trackingSuggestedDates = null,
            vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId),
        )
    }

    //callback from Downloader
    override fun updateDownloads() {
        presenterScope.launchIO {
            updateChapterFlows()
        }
    }

    //callback from library update listener
    override fun onUpdateManga(manga: Manga?) {
        if (manga?.id == mangaId) {
            updateChapterFlows()
        }
    }

    fun copiedToClipboard(message: String) {
        presenterScope.launchIO {
            _snackbarState.emit(SnackbarState(messageRes = R.string._copied_to_clipboard, message = message))
        }
    }

    /**
     * Check if can access internet
     */
    private fun isOnline(): Boolean {
        return controller?.activity?.isOnline() == true
    }

    /**
     * This method request updates after the actitivy resumed (usually after a return from the reader)
     */
    fun resume() {
        presenterScope.launch {
            updateMangaFlow()
            updateChapterFlows()
            updateTrackingFlows()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadManager.removeListener(this)
        LibraryUpdateService.removeListener(this)
    }
}
