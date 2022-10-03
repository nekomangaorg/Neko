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
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.matchingTrack
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DownloadAction
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortOption
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.No
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.Yes
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange
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
import java.io.File
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.MergeManga
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.domain.track.toTrackItem
import org.nekomanga.domain.track.toTrackServiceItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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

    private val _currentManga = MutableStateFlow<Manga?>(null)
    val manga: StateFlow<Manga?> = _currentManga.asStateFlow()
    private val currentManga
        get() = _currentManga.value!!

    private val _generalState = MutableStateFlow(MangaConstants.MangaScreenGeneralState())
    val generalState: StateFlow<MangaConstants.MangaScreenGeneralState> = _generalState.asStateFlow()

    private val _mangaState = MutableStateFlow(MangaConstants.MangaScreenMangaState(currentArtwork = Artwork(mangaId = mangaId)))
    val mangaState: StateFlow<MangaConstants.MangaScreenMangaState> = _mangaState.asStateFlow()

    private val _trackMergeState = MutableStateFlow(MangaConstants.MangaScreenTrackMergeState())
    val trackMergeState: StateFlow<MangaConstants.MangaScreenTrackMergeState> = _trackMergeState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _snackbarState = MutableSharedFlow<SnackbarState>()
    val snackBarState: SharedFlow<SnackbarState> = _snackbarState.asSharedFlow()

    private val chapterSort = ChapterItemSort(chapterItemFilter, preferences)

    override fun onCreate() {
        super.onCreate()
        downloadManager.addListener(this)

        LibraryUpdateService.setListener(this)
        presenterScope.launch {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            _currentManga.value = dbManga
            _generalState.value = MangaConstants.MangaScreenGeneralState(
                hasDefaultCategory = preferences.defaultCategory() != -1,
                hideButtonText = preferences.hideButtonText().get(),
                extraLargeBackdrop = preferences.extraLargeBackdrop().get(),
                hideChapterTitles = getHideTitlesFilter(),
                themeBasedOffCovers = preferences.themeMangaDetails(),
                vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId),
            )
            if (!currentManga.initialized) {
                updateCategoryFlows()
                updateMangaFlow()
                updateArtworkFlow()
                onRefresh()
            } else {
                updateAllFlows()
                refreshTracking(false)
                syncChaptersReadStatus()
            }
        }
    }

    /**
     * Update all flows
     */
    private fun updateAllFlows() {
        presenterScope.launchIO {
            // immediately update the categories in case loading manga takes a second
            updateCategoryFlows()
            runCatching {
                val m = db.getManga(mangaId).executeAsBlocking()!!
                _currentManga.value = m
                val mangaState = getMangaStateCopyFromManga(m)
                val currentArtwork = createCurrentArtwork(m)
                val altArtwork = createAltArtwork(m, currentArtwork)
                _mangaState.update {
                    mangaState.copy(currentArtwork = currentArtwork, alternativeArtwork = altArtwork)
                }
                updateChapterFlows()
                updateFilterFlow()
            }.onFailure {
                XLog.e("Error trying to update manga in all flows", it)
            }
        }
        updateTrackingFlows(true)
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

            _isRefreshing.value = true

            mangaUpdateCoordinator.update(currentManga, presenterScope).collect { result ->
                when (result) {
                    is MangaResult.Error -> {
                        _snackbarState.emit(SnackbarState(message = result.text, messageRes = result.id))
                        _isRefreshing.value = false
                    }
                    is MangaResult.Success -> {
                        updateAllFlows()
                        _isRefreshing.value = false
                    }
                    is MangaResult.UpdatedChapters -> {
                        updateChapterFlows()
                        syncChaptersReadStatus()
                    }
                    is MangaResult.UpdatedManga -> {
                        updateMangaFlow()
                    }
                    is MangaResult.UpdatedArtwork -> {
                        updateArtworkFlow()
                    }
                    is MangaResult.ChaptersRemoved -> {
                        val removedChapters = generalState.value.allChapters.filter {
                            it.chapter.id in result.chapterIdsRemoved && it.isDownloaded
                        }

                        if (removedChapters.isNotEmpty()) {
                            when (preferences.deleteRemovedChapters().get()) {
                                2 -> deleteChapters(removedChapters)
                                1 -> Unit
                                else -> {
                                    _generalState.update {
                                        it.copy(
                                            removedChapters = removedChapters.toImmutableList(),
                                        )
                                    }
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
    fun updateMangaCategories(enabledCategories: List<CategoryItem>) {
        presenterScope.launchIO {
            val categories = enabledCategories.map { MangaCategory.create(currentManga, it.toDbCategory()) }
            db.setMangaCategories(categories, listOf(currentManga))
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

            _generalState.update {
                it.copy(
                    trackingSuggestedDates = TrackingSuggestedDates(
                        startDate = chapters.minOfOrNull { it.last_read } ?: 0L,
                        finishedDate = chapters.maxOfOrNull { it.last_read } ?: 0L,
                    ),
                )
            }
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
            // add a slight delay in case the tracking flow is slower

            var count = 0
            while (count < 5 && trackMergeState.value.tracks.isEmpty()) {
                delay(1000)
                count++
            }

            val asyncList = trackMergeState.value.tracks
                .filter { trackManager.getService(it.trackServiceId) != null }
                .filter { trackManager.getService(it.trackServiceId)!!.isLogged() }
                .map { trackItem ->
                    val service = trackManager.getService(trackItem.trackServiceId)!!
                    async(Dispatchers.IO) {
                        kotlin.runCatching { service.refresh(trackItem.toDbTrack()) }.onFailure {
                            if (it !is CancellationException) {
                                XLog.e("error refreshing tracker", it)
                                delay(3000)
                                _snackbarState.emit(SnackbarState(message = it.message, fieldRes = service.nameRes(), messageRes = R.string.error_refreshing_))
                            }
                        }.onSuccess { track ->
                            db.insertTrack(track).executeOnIO()
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
                statusHandler.getReadChapterIds(currentManga.uuid()).collect { chapterIds ->
                    val chaptersToMarkRead = generalState.value.allChapters.asSequence().filter { !it.chapter.isMergedChapter() }
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
    fun searchTracker(title: String, service: TrackServiceItem) {
        presenterScope.launchIO {
            val previouslyTracked = trackMergeState.value.tracks.firstOrNull { service.id == it.trackServiceId } != null
            trackingCoordinator.searchTracker(title, service, currentManga, previouslyTracked).collect { result ->
                _trackMergeState.update {
                    it.copy(trackSearchResult = result)
                }
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
    fun removeTracking(alsoRemoveFromTracker: Boolean, service: TrackServiceItem) {
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
                // returns null because before Q, the share sheet can't show the cover
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
            coverCache.getCustomCoverFile(currentManga).takeIf { it.exists() } ?: coverCache.getCoverFile(currentManga.thumbnail_url, currentManga.favorite)
        } else {
            coverCache.getCoverFile(artwork.url)
        }
        val type = ImageUtil.findImageType(cover.inputStream())
            ?: throw Exception("Not an image")

        directory.mkdirs()

        // Build destination file.
        val fileNameNoExtension = listOfNotNull(
            currentManga.title,
            artwork.volume.ifEmpty { null },
            currentManga.uuid(),
        ).joinToString("-")

        val filename = DiskUtil.buildValidFilename("$fileNameNoExtension.${type.extension}")

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
            coverCache.setCustomCoverToCache(currentManga, artwork.url)
            MangaCoverMetadata.remove(mangaId)
            val manga = currentManga
            manga.user_cover = artwork.url
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateArtworkFlow()
        }
    }

    /**
     * Reset cover
     */
    fun resetCover() {
        presenterScope.launchIO {
            coverCache.deleteCustomCover(currentManga)
            MangaCoverMetadata.remove(mangaId)
            val manga = currentManga
            manga.user_cover = null
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateArtworkFlow()
        }
    }

    /**
     * Set custom title or resets if given null
     */
    fun setAltTitle(title: String?) {
        presenterScope.launchIO {
            val previousTitle = mangaState.value.currentTitle
            val newTitle = title ?: currentManga.originalTitle
            _mangaState.update {
                it.copy(currentTitle = newTitle)
            }

            val manga = currentManga
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
            val editManga = currentManga
            editManga.apply {
                merge_manga_url = null
            }
            db.insertManga(editManga).executeOnIO()
            updateMangaFlow()

            val mergedChapters =
                db.getChapters(currentManga).executeOnIO().filter { it.isMergedChapter() }

            downloadManager.deleteChapters(mergedChapters, currentManga, sourceManager.getMangadex())
            db.deleteChapters(mergedChapters).executeOnIO()
            updateAllFlows()
        }
    }

    fun searchMergedManga(query: String) {
        presenterScope.launchIO {
            _trackMergeState.update {
                it.copy(mergeSearchResult = MergeSearchResult.Loading)
            }

            runCatching {
                val mergedMangaResults = sourceManager.getMergeSource()
                    .searchManga(query)
                    .map { MergeManga(thumbnail = it.thumbnail_url ?: "", title = it.title, url = it.url) }
                _trackMergeState.update {
                    when (mergedMangaResults.isEmpty()) {
                        true -> trackMergeState.value.copy(mergeSearchResult = MergeSearchResult.NoResult)
                        false -> trackMergeState.value.copy(mergeSearchResult = MergeSearchResult.Success(mergedMangaResults))
                    }
                }
            }.getOrElse { error ->
                _trackMergeState.update {
                    it.copy(mergeSearchResult = MergeSearchResult.Error(error.message ?: "Error looking up information"))
                }
            }
        }
    }

    /**
     * Attach the selected merge manga entry
     */
    fun addMergedManga(mergeManga: MergeManga) {
        presenterScope.launchIO {
            val editManga = currentManga
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
    private fun updateArtworkFlow() {
        presenterScope.launchIO {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            val currentArtwork = createCurrentArtwork(dbManga)

            val altArtwork = createAltArtwork(currentManga, currentArtwork)
            _mangaState.update {
                it.copy(alternativeArtwork = altArtwork.toImmutableList(), currentArtwork = currentArtwork)
            }
        }
    }

    private fun createCurrentArtwork(manga: Manga): Artwork {
        return Artwork(url = manga.user_cover ?: "", inLibrary = manga.favorite, originalArtwork = manga.thumbnail_url ?: "", mangaId = mangaId)
    }

    private fun createAltArtwork(manga: Manga, currentArtwork: Artwork): ImmutableList<Artwork> {
        val quality = preferences.thumbnailQuality()

        return db.getArtwork(mangaId).executeAsBlocking().map { aw ->
            Artwork(
                mangaId = aw.mangaId,
                url = MdUtil.cdnCoverUrl(manga.uuid(), aw.fileName, quality),
                volume = aw.volume,
                description = aw.description,
                active = currentArtwork.url.contains(aw.fileName) || (currentArtwork.url.isBlank() && currentArtwork.originalArtwork.contains(aw.fileName)),
            )
        }.toImmutableList()
    }

    private fun updateVibrantColorFlow() {
        presenterScope.launch {
            _generalState.update { it.copy(vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId)) }
        }
    }

    /**
     * Updates the visible chapters for a manga
     */
    private fun updateChapterFlows() {
        presenterScope.launchIO {
            // possibly move this into a chapter repository
            val blockedScanlators = preferences.blockedScanlators().get()
            val allChapters = db.getChapters(mangaId).executeOnIO().mapNotNull { it.toSimpleChapter() }
                .filter {
                    it.scanlatorList().none { scanlator -> blockedScanlators.contains(scanlator) }
                }
                .map { chapter ->
                    val downloadState = when {
                        downloadManager.isChapterDownloaded(chapter.toDbChapter(), currentManga) -> Download.State.DOWNLOADED
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
            _generalState.update {
                it.copy(allChapters = allChapters.toImmutableList())
            }

            val allChapterScanlators = allChapters.flatMap { ChapterUtil.getScanlators(it.chapter.scanlator) }.toSet()
            if (allChapterScanlators.size == 1 && currentManga.filtered_scanlators.isNotNullOrEmpty()) {
                updateMangaScanlator(emptySet())
            }

            val allLanguages = allChapters.flatMap { ChapterUtil.getLanguages(it.chapter.language) }.toSet()

            _generalState.update {
                it.copy(
                    activeChapters = chapterSort.getChaptersSorted(currentManga, allChapters).toImmutableList(),
                    allChapters = allChapters.toImmutableList(),
                    allScanlators = allChapterScanlators.toImmutableSet(),
                    allLanguages = allLanguages.toImmutableSet(),
                )
            }

            updateFilterFlow()
            // do this after so the texts gets updated
            updateNextUnreadChapter()
            updateMissingChapters()
        }
    }

    /**
     * Updates the filtered languages
     */
    private fun updateMangaScanlator(filteredScanlators: Set<String>) {
        presenterScope.launchIO {
            val manga = currentManga
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
     * Updates the filtered scanlators
     */
    private fun updateMangaFilteredLanguages(filteredLanguages: Set<String>) {
        presenterScope.launchIO {
            val manga = currentManga
            manga.filtered_language = when (filteredLanguages.isEmpty()) {
                true -> null
                false -> ChapterUtil.getLanguageString(filteredLanguages)
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
        presenterScope.launch {
            val categories = db.getCategories().executeAsBlocking()
            val mangaCategories = db.getCategoriesForManga(mangaId).executeAsBlocking()

            _generalState.update {
                it.copy(
                    allCategories = categories.map { it.toCategoryItem() }.toImmutableList(),
                    currentCategories = mangaCategories.map { it.toCategoryItem() }.toImmutableList(),
                )
            }
        }
    }

    /**
     * Update flows for tracking
     */
    private fun updateTrackingFlows(checkForMissingTrackers: Boolean = false) {
        presenterScope.launchIO {
            _trackMergeState.update {
                it.copy(
                    loggedInTrackService = trackManager.services.filter { it.value.isLogged() }.map { it.value.toTrackServiceItem() }.toImmutableList(),
                    tracks = db.getTracks(mangaId).executeAsBlocking().map { it.toTrackItem() }.toImmutableList(),
                )
            }

            if (checkForMissingTrackers) {
                val autoAddTracker = preferences.autoAddTracker().get()

                // Always add the mdlist initial unfollowed tracker, also add it as PTR if need be
                trackMergeState.value.loggedInTrackService.firstOrNull { it.isMdList }?.let { _ ->
                    val mdList = trackManager.mdList

                    var track = trackMergeState.value.tracks.firstOrNull { mdList.matchingTrack(it) }?.toDbTrack()

                    if (track == null) {
                        track = mdList.createInitialTracker(currentManga)
                        db.insertTrack(track).executeOnIO()
                        if (isOnline()) {
                            mdList.bind(track)
                        }
                        db.insertTrack(track).executeOnIO()
                    }
                    val shouldAddAsPlanToRead = currentManga.favorite && preferences.addToLibraryAsPlannedToRead() && FollowStatus.isUnfollowed(track.status)
                    if (shouldAddAsPlanToRead && isOnline()) {
                        track.status = FollowStatus.PLAN_TO_READ.int
                        trackingCoordinator.updateTrackingService(track.toTrackItem(), trackManager.mdList.toTrackServiceItem())
                    }
                }

                if (autoAddTracker.size > 1 && currentManga.favorite) {
                    val validContentRatings = preferences.autoTrackContentRatingSelections()
                    val contentRating = currentManga.getContentRating()
                    if (contentRating == null || validContentRatings.contains(contentRating.lowercase())) {
                        autoAddTracker.map { it.toInt() }.map { autoAddTrackerId ->
                            async {
                                trackMergeState.value.loggedInTrackService
                                    .firstOrNull { it.id == autoAddTrackerId }?.let { trackService ->
                                        val id = trackManager.getIdFromManga(trackService, currentManga)
                                        if (id != null && !trackMergeState.value.tracks.any { trackService.id == it.trackServiceId }) {
                                            if (!isOnline()) {
                                                launchUI { _snackbarState.emit(SnackbarState(message = "No network connection, cannot autolink tracker")) }
                                            } else {
                                                val trackResult = trackingCoordinator.searchTrackerNonFlow("", trackManager.getService(trackService.id)!!.toTrackServiceItem(), currentManga, false)

                                                if (trackResult is TrackingConstants.TrackSearchResult.Success) {
                                                    val trackSearchItem = trackResult.trackSearchResult[0]
                                                    val trackingUpdate = trackingCoordinator.registerTracking(TrackAndService(trackSearchItem.trackItem, trackService), mangaId)
                                                    handleTrackingUpdate(trackingUpdate, false)
                                                } else if (trackResult is TrackingConstants.TrackSearchResult.Error) {
                                                    launchUI {
                                                        _snackbarState.emit(
                                                            SnackbarState(
                                                                prefixRes = trackResult.trackerNameRes,
                                                                message = " error trying to autolink tracking.  ${trackResult.errorMessage}",
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        }.awaitAll()
                    }
                }
                // update the tracks incase they were updated above
                _trackMergeState.update {
                    it.copy(
                        tracks = db.getTracks(mangaId).executeAsBlocking().map { it.toTrackItem() }.toImmutableList(),
                    )
                }
            }

            getSuggestedDate()

            val trackCount = trackMergeState.value.loggedInTrackService.count { trackServiceItem ->
                val trackService = trackManager.getService(trackServiceItem.id)!!
                trackMergeState.value.tracks.any { track ->
                    // return true if track matches and not MDList
                    // or track matches and MDlist is anything but Unfollowed
                    trackService.matchingTrack(track) &&
                        (
                            trackService.isMdList().not() ||
                                (trackService.isMdList() && !FollowStatus.isUnfollowed(track.status))
                            )
                }
            }

            _generalState.update {
                it.copy(
                    trackServiceCount = trackCount,
                )
            }
        }
    }

    /**
     * Get Manga Description
     */
    private fun getDescription(): String {
        return when {
            currentManga.uuid().isDigitsOnly() -> "THIS MANGA IS NOT MIGRATED TO V5"
            currentManga.description.isNotNullOrEmpty() -> currentManga.description!!
            !currentManga.initialized -> ""
            else -> "No description"
        }
    }

    /**
     * Get current sort filter
     */
    private fun getSortFilter(): MangaConstants.SortFilter {
        val sortOrder = currentManga.chapterOrder(preferences)
        val status = when (currentManga.sortDescending(preferences)) {
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
        val read = when (currentManga.readFilter(preferences)) {
            Manga.CHAPTER_SHOW_UNREAD -> ToggleableState.On
            Manga.CHAPTER_SHOW_READ -> ToggleableState.Indeterminate
            else -> ToggleableState.Off
        }
        val bookmark = when (currentManga.bookmarkedFilter(preferences)) {
            Manga.CHAPTER_SHOW_BOOKMARKED -> ToggleableState.On
            Manga.CHAPTER_SHOW_NOT_BOOKMARKED -> ToggleableState.Indeterminate
            else -> ToggleableState.Off
        }

        val downloaded = when (currentManga.downloadedFilter(preferences)) {
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
        val filteredScanlators = ChapterUtil.getScanlators(currentManga.filtered_scanlators).toSet()
        val scanlatorOptions = generalState.value.allScanlators.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { it },
        )
            .map { scanlator ->
                MangaConstants.ScanlatorOption(name = scanlator, disabled = filteredScanlators.contains(scanlator))
            }
        return MangaConstants.ScanlatorFilter(scanlators = scanlatorOptions.toImmutableList())
    }

    /**
     * Get scanlator filter
     */
    private fun getLangaugeFilter(): MangaConstants.LanguageFilter {
        val filteredLanguages = ChapterUtil.getLanguages(currentManga.filtered_language).toSet()
        val languageOptions = generalState.value.allLanguages.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { it },
        )
            .map { language ->
                MangaConstants.LanguageOption(name = language, disabled = filteredLanguages.contains(language))
            }
        return MangaConstants.LanguageFilter(languages = languageOptions.toImmutableList())
    }

    /**
     * Get hide titles
     */
    private fun getHideTitlesFilter(): Boolean {
        return currentManga.hideChapterTitle(preferences)
    }

    private fun getFilterText(filter: MangaConstants.Filter, chapterScanlatorFilter: MangaConstants.ScanlatorFilter): String {
        val hasDisabledScanlators = chapterScanlatorFilter.scanlators.any { it.disabled }
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
            val manga = currentManga

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
            val manga = currentManga

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
            val newFilteredScanlators = if (scanlatorOption != null) {
                val filteredScanlators = ChapterUtil.getScanlators(currentManga.filtered_scanlators).toMutableSet()
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
     * Changes the filtered scanlators, if null then it resets the scanlator filter
     */
    fun changeLanguageOption(languageOptions: MangaConstants.LanguageOption?) {
        presenterScope.launchIO {
            val manga = currentManga

            val newFilteredLanguages = if (languageOptions != null) {
                val filteredLanguages = ChapterUtil.getLanguages(manga.filtered_language).toMutableSet()
                when (languageOptions.disabled) {
                    true -> filteredLanguages.add(languageOptions.name)
                    false -> filteredLanguages.remove(languageOptions.name)
                }
                filteredLanguages
            } else {
                emptySet()
            }

            updateMangaFilteredLanguages(newFilteredLanguages)
            updateChapterFlows()
        }
    }

    /**
     * Hides/Shows the titles of the chapters
     */
    fun hideTitlesOption(hide: Boolean) {
        presenterScope.launchIO {
            val manga = currentManga

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
            val manga = currentManga
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
            currentManga.sortDescending == preferences.chaptersDescAsDefault().get() &&
                currentManga.sorting == preferences.sortChapterOrder().get()
            ) || !currentManga.usesLocalSort
    }

    private fun mangaFilterMatchesDefault(): Boolean {
        return (
            currentManga.readFilter == preferences.filterChapterByRead().get() &&
                currentManga.downloadedFilter == preferences.filterChapterByDownloaded().get() &&
                currentManga.bookmarkedFilter == preferences.filterChapterByBookmarked().get() &&
                currentManga.hideChapterTitles == preferences.hideChapterTitlesByDefault().get()
            ) || !currentManga.usesLocalFilter
    }

    /**
     * Update the current artwork with the vibrant color
     */
    fun updateMangaColor(vibrantColor: Int) {
        MangaCoverMetadata.addVibrantColor(mangaId, vibrantColor)
        updateVibrantColorFlow()
    }

    /**
     * Update flows for manga
     */
    private fun updateMangaFlow() {
        presenterScope.launch {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            _currentManga.value = dbManga
            _mangaState.update {
                getMangaStateCopyFromManga(_currentManga.value!!)
            }
        }
    }

    private fun getMangaStateCopyFromManga(m: Manga): MangaConstants.MangaScreenMangaState {
        return mangaState.value.copy(
            alternativeTitles = m.getAltTitles().toImmutableList(),
            artist = m.artist ?: "",
            author = m.author ?: "",
            currentDescription = getDescription(),
            currentTitle = m.title,
            externalLinks = currentManga.getExternalLinks().toImmutableList(),
            genres = (m.getGenres(true) ?: emptyList()).toImmutableList(),
            initialized = m.initialized,
            inLibrary = m.favorite,
            isMerged = when (m.isMerged()) {
                true -> Yes(sourceManager.getMergeSource().baseUrl + currentManga.merge_manga_url!!, currentManga.title)
                false -> No
            },
            isPornographic = m.getContentRating()?.equals(MdConstants.ContentRating.pornographic, ignoreCase = true) ?: false,
            langFlag = m.lang_flag,
            missingChapters = m.missing_chapters,
            originalTitle = m.originalTitle,
            rating = m.rating,
            status = m.status,
            users = m.users,
        )
    }

    /**
     * Update filterflow
     */
    private fun updateFilterFlow() {
        presenterScope.launchIO {
            val filter = getFilter()
            val scanlatorFilter = getScanlatorFilter()
            val languageFilter = getLangaugeFilter()

            _generalState.update {
                it.copy(
                    chapterSortFilter = getSortFilter(),
                    chapterFilter = filter,
                    hideChapterTitles = getHideTitlesFilter(),
                    chapterFilterText = getFilterText(filter, scanlatorFilter),
                    chapterScanlatorFilter = scanlatorFilter,
                    chapterLanguageFilter = languageFilter,
                )
            }
        }
    }

    /**
     * Toggle a manga as favorite
     */
    fun toggleFavorite(shouldAddToDefaultCategory: Boolean) {
        presenterScope.launch {
            val editManga = currentManga
            editManga.apply {
                favorite = !favorite
                date_added = when (favorite) {
                    true -> Date().time
                    false -> 0
                }
            }

            _mangaState.update {
                it.copy(inLibrary = editManga.favorite)
            }

            db.insertManga(editManga).executeAsBlocking()
            updateMangaFlow()
            // add to the default category if it exists and the user has the option set
            if (shouldAddToDefaultCategory && generalState.value.hasDefaultCategory) {
                val defaultCategoryId = preferences.defaultCategory()
                generalState.value.allCategories.firstOrNull { defaultCategoryId == it.id }?.let {
                    updateMangaCategories(listOf(it))
                }
            }
            if (editManga.favorite) {
                // this is called for the add as plan to read/auto sync tracking,
                updateTrackingFlows(true)
            }
        }
    }

    /**
     * Delete the list of chapters
     */
    fun deleteChapters(chapterItems: List<ChapterItem>, isEverything: Boolean = false, canUndo: Boolean = false) {
        // do on global scope cause we don't want exiting the manga to prevent the deleting

        launchIO {
            if (chapterItems.isNotEmpty()) {
                val delete = {
                    if (isEverything) {
                        downloadManager.deleteManga(currentManga, sourceManager.getMangadex())
                    } else {
                        downloadManager.deleteChapters(chapterItems.map { it.chapter.toDbChapter() }, currentManga, sourceManager.getMangadex())
                    }
                }
                if (canUndo) {
                    _snackbarState.emit(
                        SnackbarState(
                            messageRes = R.string.deleted_downloads,
                            actionLabelRes = R.string.undo,
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
        if (!currentManga.favorite) {
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
            val allChapterSize = generalState.value.allChapters.size
            when (downloadAction) {
                is DownloadAction.ImmediateDownload -> {
                    addToLibrarySnack()
                    downloadManager.startDownloadNow(chapterItems.first().chapter.toDbChapter())
                }
                is DownloadAction.DownloadAll -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(currentManga, generalState.value.activeChapters.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() })
                }
                is DownloadAction.Download -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(currentManga, chapterItems.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() })
                }
                is DownloadAction.DownloadNextUnread -> {
                    val filteredChapters =
                        generalState.value.activeChapters.filter { !it.chapter.read && !it.isDownloaded }.sortedWith(chapterSort.sortComparator(currentManga, true))
                            .take(downloadAction.numberToDownload)
                            .map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(currentManga, filteredChapters)
                }
                is DownloadAction.DownloadUnread -> {
                    val filteredChapters =
                        generalState.value.activeChapters.filter { !it.chapter.read && !it.isDownloaded }.sortedWith(chapterSort.sortComparator(currentManga, true)).map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(currentManga, filteredChapters)
                }
                is DownloadAction.Remove -> deleteChapters(chapterItems, chapterItems.size == allChapterSize)
                is DownloadAction.RemoveAll -> deleteChapters(
                    generalState.value.activeChapters.filter { it.isNotDefaultDownload },
                    generalState.value.activeChapters.size == allChapterSize,
                    true,
                )
                is DownloadAction.RemoveRead -> {
                    val filteredChapters = generalState.value.activeChapters.filter { it.chapter.read && it.isDownloaded }
                    deleteChapters(filteredChapters, filteredChapters.size == allChapterSize, true)
                }
                is DownloadAction.Cancel -> deleteChapters(chapterItems, chapterItems.size == allChapterSize)
            }
        }
    }

    fun markChapters(chapterItems: List<ChapterItem>, markAction: MangaConstants.MarkAction, skipSync: Boolean = false) {
        presenterScope.launchIO {
            val initialChapterItems = if (markAction is MangaConstants.MarkAction.PreviousRead || markAction is MangaConstants.MarkAction.PreviousUnread) {
                when (currentManga.sortDescending(preferences)) {
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
                        // dont delete bookmarked chapters
                        deleteChapters(newChapterItems.filter { !it.bookmark }.map { ChapterItem(chapter = it) }, newChapterItems.size == generalState.value.allChapters.size)
                    }
                    // get the highest chapter number and update tracking for it
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

                // sync with dex if marked read or marked unread
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
                                currentManga.uuid(),
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
                        messageRes = nameRes,
                        actionLabelRes = R.string.undo,
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
        _generalState.update {
            it.copy(
                removedChapters = persistentListOf(),
            )
        }
    }

    /**
     * Get Quick read text for the button
     */
    private fun updateNextUnreadChapter() {
        presenterScope.launchIO {
            val nextChapter = chapterSort.getNextUnreadChapter(currentManga, generalState.value.activeChapters)?.chapter
            _generalState.update {
                it.copy(
                    nextUnreadChapter =
                    when (nextChapter == null) {
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
                    },
                )
            }
        }
    }

    /**
     * updates the missing chapter count on a manga if needed
     */
    private fun updateMissingChapters() {
        presenterScope.launchIO {
            val currentMissingChapters = generalState.value.allChapters.getMissingCount(currentManga.status)
            if (currentMissingChapters != currentManga.missing_chapters) {
                val editManga = currentManga
                editManga.apply {
                    this.missing_chapters = currentMissingChapters
                }
                db.insertManga(editManga)
                updateMangaFlow()
            }
        }
    }

    // callback from Downloader
    override fun updateDownload(download: Download) {
        presenterScope.launchIO {
            val currentChapters = generalState.value.activeChapters
            val index = currentChapters.indexOfFirst { it.chapter.id == download.chapter.id }
            if (index >= 0) {
                val mutableChapters = currentChapters.toMutableList()
                val updateChapter = currentChapters[index].copy(downloadState = download.status, downloadProgress = download.progressFloat)
                mutableChapters[index] = updateChapter
                _generalState.update {
                    it.copy(
                        activeChapters = mutableChapters.toImmutableList(),
                    )
                }
            }
        }
    }

    fun blockScanlator(scanlator: String) {
        presenterScope.launchIO {
            val scanlatorImpl = db.getScanlatorByName(scanlator).executeAsBlocking()
            if (scanlatorImpl == null) {
                launchIO {
                    mangaUpdateCoordinator.updateScanlator(scanlator)
                }
            }
            val blockedScanlators = preferences.blockedScanlators().get().toMutableSet()
            blockedScanlators.add(scanlator)
            preferences.blockedScanlators().set(blockedScanlators)
            updateChapterFlows()
            _snackbarState.emit(
                SnackbarState(
                    messageRes = R.string.globally_blocked_group_,
                    message = scanlator,
                    actionLabelRes = R.string.undo,
                    action = {
                        presenterScope.launch {
                            db.deleteScanlator(scanlator).executeOnIO()
                            val allBlockedScanlators = preferences.blockedScanlators().get().toMutableSet()
                            allBlockedScanlators.remove(scanlator)
                            preferences.blockedScanlators().set(allBlockedScanlators)
                            updateChapterFlows()
                        }
                    },

                ),
            )
        }
    }

    // callback from Downloader
    override fun updateDownloads() {
        presenterScope.launchIO {
            updateChapterFlows()
        }
    }

    // callback from library update listener
    override fun onUpdateManga(manga: Manga?) {
        if (manga?.id == mangaId) {
            updateChapterFlows()
        }
    }

    fun copiedToClipboard(message: String) {
        presenterScope.launchIO {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                _snackbarState.emit(SnackbarState(messageRes = R.string._copied_to_clipboard, message = message))
            }
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
