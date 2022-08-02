package eu.kanade.tachiyomi.ui.manga

import android.os.Build
import android.os.Environment
import androidx.compose.ui.state.ToggleableState
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
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.matchingTrack
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DownloadAction
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortOption
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.No
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.Yes
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.ReadingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange.EditTrackingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingSuggestedDates
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.getMissingCount
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.MergeManga
import org.threeten.bp.ZoneId
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date

class MangaComposePresenter(
    private val mangaId: Long,
    val preferences: PreferencesHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    val chapterItemFilter: ChapterItemFilter = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val statusHandler: StatusHandler = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    val mangaRepository: MangaDetailsRepository = Injekt.get(),
) : BaseCoroutinePresenter<MangaComposeController>(), DownloadQueue.DownloadListener {

    private val _currentManga = MutableStateFlow(db.getManga(mangaId).executeAsBlocking()!!)
    val manga: StateFlow<Manga> = _currentManga.asStateFlow()

    private val _currentTitle = MutableStateFlow(manga.value.title)
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _allCategories = MutableStateFlow(emptyList<Category>())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    private val _mangaCategories = MutableStateFlow(emptyList<Category>())
    val mangaCategories: StateFlow<List<Category>> = _mangaCategories.asStateFlow()

    private val _loggedInTrackingService = MutableStateFlow(emptyList<TrackService>())
    val loggedInTrackingService: StateFlow<List<TrackService>> = _loggedInTrackingService.asStateFlow()

    private val _trackServiceCount = MutableStateFlow(0)
    val trackServiceCount: StateFlow<Int> = _trackServiceCount.asStateFlow()

    private val _tracks = MutableStateFlow(emptyList<Track>())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _trackSearchResult = MutableStateFlow<TrackSearchResult>(TrackSearchResult.Loading)
    val trackSearchResult: StateFlow<TrackSearchResult> = _trackSearchResult.asStateFlow()

    private val _trackSuggestedDates = MutableStateFlow<TrackingSuggestedDates?>(null)
    val trackSuggestedDates: StateFlow<TrackingSuggestedDates?> = _trackSuggestedDates.asStateFlow()

    private val _externalLinks = MutableStateFlow(emptyList<ExternalLink>())
    val externalLinks: StateFlow<List<ExternalLink>> = _externalLinks.asStateFlow()

    private val _alternativeArtwork = MutableStateFlow(emptyList<Artwork>())
    val alternativeArtwork: StateFlow<List<Artwork>> = _alternativeArtwork.asStateFlow()

    private val _isMerged = MutableStateFlow(getIsMergedManga())
    val isMerged: StateFlow<IsMergedManga> = _isMerged.asStateFlow()

    private val _mergeSearchResult = MutableStateFlow<MergeSearchResult>(MergeSearchResult.Loading)
    val mergeSearchResult: StateFlow<MergeSearchResult> = _mergeSearchResult.asStateFlow()

    private val _removedChapters = MutableStateFlow(emptyList<ChapterItem>())
    val removedChapters: StateFlow<List<ChapterItem>> = _removedChapters.asStateFlow()

    private val _nextUnreadChapter = MutableStateFlow(NextUnreadChapter())
    val nextUnreadChapter: StateFlow<NextUnreadChapter> = _nextUnreadChapter.asStateFlow()

    private var allChapterScanlators: Set<String> = emptySet()

    private val _currentArtwork = MutableStateFlow(
        Artwork(
            url = manga.value.user_cover ?: "",
            mangaId = mangaId,
            inLibrary = manga.value.favorite,
            originalArtwork = manga.value.thumbnail_url ?: "",
        ),
    )
    val currentArtwork: StateFlow<Artwork> = _currentArtwork.asStateFlow()

    private val _vibrantColor = MutableStateFlow(MangaCoverMetadata.getVibrantColor(mangaId))
    val vibrantColor: StateFlow<Int?> = _vibrantColor.asStateFlow()

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

    private val _chapterFilterText = MutableStateFlow(getFilterText())
    val chapterFilterText: StateFlow<String> = _chapterFilterText.asStateFlow()

    private val chapterSort = ChapterItemSort(chapterItemFilter, preferences)

    override fun onCreate() {
        super.onCreate()
        downloadManager.addListener(this)
        if (!manga.value.initialized) {
            onRefresh()
        } else {
            updateAllFlows()
        }
    }

    /**
     * Update all flows that dont require network
     */
    private fun updateAllFlows() {
        updateMangaFlow()
        updateCurrentArtworkFlow()
        updateChapterFlows()
        updateCategoryFlows()
        updateTrackingFlows()
        updateExternalFlows()
        updateMergeFlow()
        updateAlternativeArtworkFlow()
        updateFilterFlow()
    }

    fun onRefresh() {
        _isRefreshing.value = true
        presenterScope.launchIO {
            mangaRepository.update(manga.value, true, presenterScope).collect { result ->
                when (result) {
                    is MangaResult.Error -> {
                        //send a toast
                        _isRefreshing.value = false
                    }
                    is MangaResult.Success -> {
                        updateAllFlows()
                        _isRefreshing.value = false
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
                                1 -> {}
                                else -> _removedChapters.value = removedChapters
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
            val track = trackAndService.track.apply {
                this.status = trackAndService.service.getStatusList()[statusIndex]
            }
            if (trackAndService.service.isCompletedStatus(statusIndex) && track.total_chapters > 0) {
                track.last_chapter_read = track.total_chapters.toFloat()
            }
            updateTrackingService(track, trackAndService.service)
        }
    }

    /**
     * Update tracker with new score
     */
    fun updateTrackScore(scoreIndex: Int, trackAndService: TrackAndService) {
        presenterScope.launchIO {

            val track = trackAndService.track.apply {
                this.score = trackAndService.service.indexToScore(scoreIndex)
            }
            if (trackAndService.service.isMdList()) {
                runCatching {
                    (trackAndService.service as MdList).updateScore(track)
                    updateTrackingFlows()
                }
            } else {
                updateTrackingService(track, trackAndService.service)
            }
        }
    }

    /** Figures out the suggested reading dates
     */
    private fun getSuggestedDate() {
        presenterScope.launchIO {
            val chapters = db.getHistoryByMangaId(mangaId).executeOnIO()

            _trackSuggestedDates.value = TrackingSuggestedDates(
                startDate = chapters.minOfOrNull { it.last_read } ?: 0L,
                finishedDate = chapters.maxOfOrNull { it.last_read } ?: 0L,
            )
        }
    }

    /**
     * Update the tracker with the new chapter information
     */
    fun updateTrackChapter(newChapterNumber: Int, trackAndService: TrackAndService) {
        val track = trackAndService.track.apply {
            this.last_chapter_read = newChapterNumber.toFloat()
        }

        updateTrackingService(track, trackAndService.service)
    }

    /**
     * Update the tracker with the start/finished date
     */
    fun updateTrackDate(trackDateChange: TrackDateChange) {
        val date = when (trackDateChange) {
            is EditTrackingDate -> {
                trackDateChange.newDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            else -> 0L
        }
        val track = trackDateChange.trackAndService.track.apply {
            when (trackDateChange.readingDate) {
                ReadingDate.Start -> this.started_reading_date = date
                ReadingDate.Finish -> this.finished_reading_date = date
            }
        }

        updateTrackingService(track, trackDateChange.trackAndService.service)
    }

    /**
     * Updates the remote tracking service with tracking changes
     */
    private fun updateTrackingService(track: Track, service: TrackService) {
        presenterScope.launchIO {
            runCatching {
                val updatedTrack = service.update(track)
                db.insertTrack(updatedTrack).executeOnIO()
                updateTrackingFlows()
            }.onFailure {
                //trackError(e)
                //TODO snackbar error the issue
            }
        }
    }

    /**
     * Search Tracker
     */
    fun searchTracker(title: String, service: TrackService) {
        presenterScope.launchIO {
            _trackSearchResult.value = TrackSearchResult.Loading
            runCatching {
                val previouslyTracked = _tracks.value.firstOrNull { service.matchingTrack(it) }
                val results = service.search(title, manga.value, previouslyTracked != null)
                _trackSearchResult.value = when (results.isEmpty()) {
                    true -> TrackSearchResult.NoResult
                    false -> TrackSearchResult.Success(results)
                }
            }.onFailure { e ->
                _trackSearchResult.value = TrackSearchResult.Error(e.message ?: "Error searching tracker")
            }
        }
    }

    /**
     * Register tracker
     */
    fun registerTracking(trackAndService: TrackAndService, skipTrackFlowUpdate: Boolean = false) {
        presenterScope.launchIO {
            runCatching {
                val trackItem = trackAndService.track.apply {
                    manga_id = mangaId
                }

                trackAndService.service.bind(trackItem)
            }.onSuccess { track ->
                db.insertTrack(track).executeOnIO()
                if (!skipTrackFlowUpdate) {
                    updateTrackingFlows()
                }
            }.onFailure { exception ->
                //log the error and emit it to a snackbar
            }
        }
    }

    /**
     * Remove a tracker with an option to remove it from the tracking service
     */
    fun removeTracking(alsoRemoveFromTracker: Boolean, service: TrackService) {
        presenterScope.launchIO {
            val tracks = db.getTracks(mangaId).executeOnIO().filter { it.sync_id == service.id }
            db.deleteTrackForManga(mangaId, service).executeOnIO()
            if (alsoRemoveFromTracker && service.canRemoveFromService()) {
                launchIO {
                    tracks.forEach {
                        service.removeFromService(it)
                    }
                }
            }
            updateTrackingFlows()
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
            } catch (e: Exception) {
                XLog.e("warn", e)
                //toast
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
            MdUtil.getMangaId(manga.value.url),
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
            updateVibrantColorFlow()
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
            updateVibrantColorFlow()
        }
    }

    /**
     * Set custom title or resets if given null
     */
    fun setAltTitle(title: String?) {
        presenterScope.launchIO {
            _currentTitle.value = title ?: manga.value.originalTitle

            val manga = manga.value
            manga.user_title = title
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
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
            db.insertManga(editManga).executeAsBlocking()
            updateMangaFlow()
            onRefresh()
        }
    }

    /**
     * Updates the artwork flow
     */
    private fun updateCurrentArtworkFlow() {
        presenterScope.launchIO {

            _currentArtwork.value = Artwork(url = manga.value.user_cover ?: "", inLibrary = manga.value.favorite, originalArtwork = manga.value.thumbnail_url ?: "", mangaId = mangaId)
        }
    }

    private fun updateVibrantColorFlow() {
        presenterScope.launchIO {
            _vibrantColor.value = MangaCoverMetadata.getVibrantColor(mangaId)
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
    private fun updateTrackingFlows() {
        presenterScope.launchIO {
            _loggedInTrackingService.value = trackManager.services.filter { it.isLogged() }
            _tracks.value = db.getTracks(manga.value).executeOnIO()

            getSuggestedDate()

            _trackServiceCount.value = _loggedInTrackingService.value.count { trackService ->
                _tracks.value.any { track ->
                    //return true if track matches and not MDList
                    //or track matches and MDlist is anything but Unfollowed
                    trackService.matchingTrack(track) &&
                        (trackService.isMdList().not() ||
                            (trackService.isMdList() && (trackService as MdList).isUnfollowed(track).not()))
                }
            }

            withIOContext {
                val autoAddTracker = preferences.autoAddTracker().get()
                var refreshRequired = false

                //Always add the mdlist initial unfollowed tracker
                _loggedInTrackingService.value.firstOrNull { it.isMdList() }?.let { mdList ->
                    mdList as MdList
                    if (!_tracks.value.any { mdList.matchingTrack(it) }) {
                        val track = mdList.createInitialTracker(manga.value)
                        db.insertTrack(track).executeOnIO()
                        mdList.bind(track)
                        refreshRequired = true
                    }
                }

                if (autoAddTracker.size > 1 && manga.value.favorite) {
                    autoAddTracker.map { it.toInt() }.forEach { autoAddTrackerId ->
                        _loggedInTrackingService.value.firstOrNull { it.id == autoAddTrackerId }?.let { trackService ->
                            val id = trackManager.getIdFromManga(trackService, manga.value)
                            if (id != null && !_tracks.value.any { trackService.matchingTrack(it) }) {
                                val trackResult = trackService.search("", manga.value, false)
                                trackResult.firstOrNull()?.let { track ->
                                    registerTracking(TrackAndService(track, trackService), true)
                                    refreshRequired = true
                                }
                            }
                        }
                    }
                    if (refreshRequired) {
                        updateTrackingFlows()
                    }
                }
            }
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
     * Get current sort filter
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
        //filtersId.add(if (manga.filtered_scanlators?.isNotEmpty() == true) R.string.scanlators else null)
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

            XLog.e("ESCO ${manga.readFilter(preferences)}, ${manga.downloadedFilter(preferences)}, ${manga.bookmarkedFilter(preferences)}")


            db.insertManga(manga).executeAsBlocking()
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

            manga.filtered_scanlators = if (newFilteredScanlators.isEmpty()) {
                null
            } else {
                ChapterUtil.getScanlatorString(newFilteredScanlators)
            }

            db.insertManga(manga).executeAsBlocking()
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
            }
            db.insertManga(manga).executeAsBlocking()
            updateMangaFlow()
            updateFilterFlow()
        }
    }

    fun mangaSortMatchesDefault(): Boolean {
        return (
            manga.value.sortDescending == preferences.chaptersDescAsDefault().get() &&
                manga.value.sorting == preferences.sortChapterOrder().get()
            ) || !manga.value.usesLocalSort
    }

    fun mangaFilterMatchesDefault(): Boolean {
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
            true -> Yes(sourceManager.getMergeSource().baseUrl + manga.value.merge_manga_url!!)
            false -> No
        }
    }

    /**
     * Update flows for merge
     */
    private fun updateMergeFlow() {
        presenterScope.launchIO {
            _isMerged.value = getIsMergedManga()
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
            val uuid = MdUtil.getMangaId(manga.value.url)
            val quality = preferences.thumbnailQuality()
            val currentUsed = currentArtwork.value

            _alternativeArtwork.value = db.getArtwork(manga.value).executeAsBlocking().map { aw ->
                Artwork(
                    mangaId = aw.mangaId,
                    url = MdUtil.cdnCoverUrl(uuid, aw.fileName, quality),
                    volume = aw.volume,
                    description = aw.description,
                    active = currentUsed.url.contains(aw.fileName) || (currentUsed.url.isBlank() && currentUsed.originalArtwork.contains(aw.fileName)),
                )
            }

        }
    }

    /**
     * Update flows for manga
     */
    private fun updateMangaFlow() {
        presenterScope.launchIO {
            _currentManga.value = db.getManga(mangaId).executeOnIO()!!
        }
    }

    /**
     * Update filterflow
     */
    private fun updateFilterFlow() {
        presenterScope.launchIO {
            _chapterSortFilter.value = getSortFilter()
            _chapterFilter.value = getFilter()
            _scanlatorFilter.value = getScanlatorFilter()
            _chapterFilterText.value = getFilterText()
        }
    }

    /**
     * Toggle a manga as favorite
     */
    fun toggleFavorite(): Boolean {
        val editManga = manga.value
        editManga.apply {
            favorite = !favorite
            when (favorite) {
                true -> {
                    date_added = Date().time
                    //TODO need to add to MDList as Plan to read if enabled see the other toggleFavorite in the detailsPResenter
                }
                false -> date_added = 0
            }

        }

        db.insertManga(editManga).executeAsBlocking()
        updateMangaFlow()
        return editManga.favorite
    }

    /**
     * Delete the list of chapters
     */
    fun deleteChapters(chapterItems: List<ChapterItem>, isEverything: Boolean = false) {
        //do on global scope cause we don't want exiting the manga to prevent the deletin
        launchIO {
            if (isEverything) {
                downloadManager.deleteManga(manga.value, sourceManager.getMangadex())
            } else {
                downloadManager.deleteChapters(chapterItems.map { it.chapter.toDbChapter() }, manga.value, sourceManager.getMangadex())
            }
        }

        updateChapterFlows()
    }

    /**
     * Delete the list of chapters
     */
    fun downloadChapters(chapterItems: List<ChapterItem>, downloadAction: DownloadAction) {
        presenterScope.launchIO {
            when (downloadAction) {
                is DownloadAction.DownloadAll -> downloadManager.downloadChapters(manga.value, activeChapters.value.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() })
                is DownloadAction.Download -> downloadManager.downloadChapters(manga.value, chapterItems.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() })
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
                is DownloadAction.RemoveAll -> deleteChapters(activeChapters.value, activeChapters.value.size == allChapters.value.size)
                is DownloadAction.RemoveRead -> {
                    val filteredChapters = activeChapters.value.filter { it.chapter.read && it.isDownloaded }
                    deleteChapters(filteredChapters, filteredChapters.size == allChapters.value.size)
                }
                is DownloadAction.Cancel -> deleteChapters(chapterItems, chapterItems.size == allChapters.value.size)
            }
        }
    }

    /**
     * Flips the bookmark status for the chapter
     */
    fun bookmarkChapter(chapterItem: ChapterItem) {
        presenterScope.launchIO {
            val chapter = chapterItem.chapter.toDbChapter()
            chapter.apply {
                this.bookmark = !this.bookmark
            }
            db.updateChapterProgress(chapter).executeOnIO()
            updateChapterFlows()
        }
    }

    /**
     * Marks the given chapters read or unread
     */
    fun markRead(chapterItems: List<ChapterItem>, read: Boolean) {
        presenterScope.launchIO {
            val chapters = chapterItems.map { it.chapter.toDbChapter() }
            chapters.forEach {
                it.read = read
                if (!read) {
                    //TODO see if we need
                    // lastRead: Int? = null,
                    //         pagesLeft: Int? = null,
                    // it.last_page_read = lastRead ?: 0
                    // it.pages_left = pagesLeft ?: 0
                }
            }

            //TODO rest of background stuff
            db.updateChaptersProgress(chapters).executeAsBlocking()
            updateChapterFlows()

            //TODO do the rest of the background stuffv

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

    //callback from Downloader
    override fun updateDownloads() {
        presenterScope.launchIO {
            updateChapterFlows()
        }
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
}
