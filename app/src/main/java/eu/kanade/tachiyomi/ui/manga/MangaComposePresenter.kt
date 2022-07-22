package eu.kanade.tachiyomi.ui.manga

import android.os.Build
import android.os.Environment
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
    val chapterFilter: ChapterItemFilter = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val statusHandler: StatusHandler = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    val mangaRepository: MangaDetailsRepository = Injekt.get(),
) : BaseCoroutinePresenter<MangaComposeController>(), DownloadQueue.DownloadListener {

    private val _currentManga = MutableStateFlow(db.getManga(mangaId).executeAsBlocking()!!)
    val manga: StateFlow<Manga> = _currentManga.asStateFlow()

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
            url = "",
            mangaId = mangaId,
            inLibrary = manga.value.favorite,
            originalArtwork = manga.value.thumbnail_url ?: "",
            vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId),
        ),
    )
    val currentArtwork: StateFlow<Artwork> = _currentArtwork.asStateFlow()

    private val _altTitles = MutableStateFlow<List<String>>(emptyList())
    val altTitles: StateFlow<List<String>> = _altTitles.asStateFlow()

    private val _allChapters = MutableStateFlow<List<ChapterItem>>(emptyList())
    val allChapters: StateFlow<List<ChapterItem>> = _allChapters.asStateFlow()

    private val _activeChapters = MutableStateFlow<List<ChapterItem>>(emptyList())
    val activeChapters: StateFlow<List<ChapterItem>> = _activeChapters.asStateFlow()

    private val chapterSort = ChapterItemSort(manga.value, chapterFilter, preferences)

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
        updateChapterFlows()
        updateCategoryFlows()
        updateTrackingFlows()
        updateExternalFlows()
        updateMergeFlow()
        updateArtworkFlow()
        updateAltTitlesFlow()
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
                        updateArtworkFlow()
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
        presenterScope.launch {
            val categories = enabledCategories.map { MangaCategory.create(manga.value, it) }
            db.setMangaCategories(categories, listOf(manga.value))
            updateCategoryFlows()
        }
    }

    /**
     * Add New Category
     */
    fun addNewCategory(newCategory: String) {
        presenterScope.launch {
            val category = Category.create(newCategory)
            db.insertCategory(category).executeAsBlocking()
            updateCategoryFlows()
        }
    }

    /**
     * Update tracker with new status
     */
    fun updateTrackStatus(statusIndex: Int, trackAndService: TrackAndService) {
        presenterScope.launch {
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
        presenterScope.launch {

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
        presenterScope.launch {
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
        presenterScope.launch {
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
        presenterScope.launch {
            _trackSearchResult.value = TrackSearchResult.Loading
            runCatching {
                val results = service.search(title, manga.value, true)
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
    fun registerTracking(trackAndService: TrackAndService) {
        presenterScope.launch {
            runCatching {
                val trackItem = trackAndService.track.apply {
                    manga_id = mangaId
                }

                trackAndService.service.bind(trackItem)
            }.onSuccess { track ->
                db.insertTrack(track).executeOnIO()
                updateTrackingFlows()
            }.onFailure { exception ->
                //log the error and emit it to a snackbar
            }
        }
    }

    /**
     * Remove a tracker with an option to remove it from the tracking service
     */
    fun removeTracking(alsoRemoveFromTracker: Boolean, service: TrackService) {
        presenterScope.launch {
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
    suspend fun shareMangaCover(destDir: File, url: String = ""): File? {
        return withIOContext {
            return@withIOContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                destDir.deleteRecursively()
                try {
                    saveCover(destDir, url)
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
    suspend fun saveCover(url: String) {
        return withIOContext {
            try {
                val directory = File(
                    Environment.getExternalStorageDirectory().absolutePath +
                        File.separator + Environment.DIRECTORY_PICTURES +
                        File.separator + preferences.context.getString(R.string.app_name),
                )
                saveCover(directory, url)
            } catch (e: Exception) {
                XLog.e("warn", e)
                //toast
            }
        }
    }

    /**
     * Save Cover to directory, if given a url save that specific cover
     */
    private fun saveCover(directory: File, url: String = ""): File {
        val cover =
            if (url.isBlank()) {
                coverCache.getCustomCoverFile(manga.value).takeIf { it.exists() } ?: coverCache.getCoverFile(manga.value.thumbnail_url, manga.value.favorite)
            } else {
                coverCache.getCoverFile(url)
            }

        val type = ImageUtil.findImageType(cover.inputStream())
            ?: throw Exception("Not an image")

        directory.mkdirs()

        // Build destination file.
        val suffix = when (url.isNotBlank()) {
            true -> "-" + MdUtil.getMangaId(url)
            false -> ""

        }

        val filename = DiskUtil.buildValidFilename("${manga.value.title}${suffix}.${type.extension}")

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
    fun setCover(url: String) {
        coverCache.setCustomCoverToCache(manga.value, url)
        MangaCoverMetadata.remove(mangaId)
        updateCurrentArtworkFlow(url)
    }

    /**
     * Reset cover
     */
    fun resetCover() {
        coverCache.deleteCustomCover(manga.value)
        MangaCoverMetadata.remove(mangaId)
        updateCurrentArtworkFlow()
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
        presenterScope.launch {
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
    private fun updateCurrentArtworkFlow(url: String = "") {
        presenterScope.launch {
            _currentArtwork.value = Artwork(url = url, inLibrary = manga.value.favorite, originalArtwork = manga.value.thumbnail_url ?: "", mangaId = mangaId)
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
            /**
             * TODO make sure to copy any logic needed from getChapters() in the old presenter for all the filters and what not
             * then pass that to active chapters
             */
            allChapterScanlators = allChapters.flatMap { ChapterUtil.getScanlators(it.chapter.scanlator) }.toSet()
            if (allChapterScanlators.size == 1 && manga.value.filtered_scanlators.isNotNullOrEmpty()) {
                updateMangaScanlator(emptySet())
            }

            _activeChapters.value = chapterSort.getChaptersSorted(allChapters)

            //do this after so the texts gets updated
            updateNextUnreadChapter()
            updateMissingChapters()
        }
    }

    private fun updateMangaScanlator(filteredScanlators: Set<String>) {
        presenterScope.launch {
            db.getManga(mangaId).executeOnIO()?.apply {
                this.filtered_scanlators = when (filteredScanlators.isEmpty()) {
                    true -> null
                    false -> ChapterUtil.getScanlatorString(filteredScanlators)
                }
            }?.run {
                db.insertManga(this)
                updateMangaFlow()
            }
        }
    }

    /**
     * Updates the flows for all categories, and manga categories
     */
    private fun updateCategoryFlows() {
        presenterScope.launch {
            _allCategories.value = db.getCategories().executeOnIO()
            _mangaCategories.value = db.getCategoriesForManga(mangaId).executeOnIO()
        }
    }

    /**
     * Update flows for tracking
     */
    private fun updateTrackingFlows() {
        presenterScope.launch {
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
        }
    }

    /**
     * Update flows for external links
     */
    private fun updateExternalFlows() {
        presenterScope.launch {
            _externalLinks.value = manga.value.getExternalLinks()
        }
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
        presenterScope.launch {
            _isMerged.value = getIsMergedManga()
        }
    }

    /**
     * Update the current artwork with the vibrant color
     */
    fun updateMangaColor(vibrantColor: Int) {
        MangaCoverMetadata.addVibrantColor(mangaId, vibrantColor)
        _currentArtwork.value = currentArtwork.value.copy(vibrantColor = vibrantColor)
    }

    /**
     * Update flows for merge
     */
    private fun updateArtworkFlow() {
        presenterScope.launch {
            val simple = Artwork(
                url = "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/dfcaab7a-2c3c-4ea5-8641-abffd2a95b5f.jpg",
                inLibrary = manga.value.favorite,
                originalArtwork = manga.value.thumbnail_url ?: "",
                mangaId = mangaId,
            )
            _alternativeArtwork.value = listOf(
                simple,
                simple.copy(url = "https://mangadex.org/covers/6ebe8b8a-7bac-45f0-8652-4b9d52b95644/0263895a-3826-4d2d-af4a-2751413d2d7c.jpg"),
                simple.copy(url = "https://mangadex.org/covers/6ebe8b8a-7bac-45f0-8652-4b9d52b95644/3bd22d26-f22b-4d7c-8943-f22bd7302519.jpg"),
                simple.copy(url = "https://mangadex.org/covers/e0c9100e-287d-4417-8c17-b4e696254dc7/6e8d421d-3780-4b38-a377-63c4da4adb07.jpg"),
                simple.copy(url = "https://mangadex.org/covers/e0c9100e-287d-4417-8c17-b4e696254dc7/bb1f9608-7380-41cc-a1b1-144a7e20aa93.jpg"),
                simple.copy(url = "https://mangadex.org/covers/c28803a6-498a-42a4-8beb-a6edacbdd94c/ed8b03f5-70e5-432d-99aa-f22f1848d7c9.png"),
                simple.copy(url = "https://mangadex.org/covers/b1ad0635-702d-4762-b7bd-1f9475688b5d/9132f3a0-8d7d-4df0-b6bf-ebbde700bcd0.jpg"),
                simple.copy(url = "https://mangadex.org/covers/141015d2-e545-40f8-bb89-4903204fd34e/3c6ba9b7-12ed-4eec-8b60-47d8d74e7371.png"),
                simple.copy(url = "https://mangadex.org/covers/a2c1d849-af05-4bbc-b2a7-866ebb10331f/da0341d8-5526-452c-8bd3-dc8e3cd89f99.jpg"),
                simple.copy(url = "https://mangadex.org/covers/a2c1d849-af05-4bbc-b2a7-866ebb10331f/bf899731-d31e-4a44-9022-4cef07aac5cf.jpg"),
                simple.copy(url = "https://mangadex.org/covers/a2c1d849-af05-4bbc-b2a7-866ebb10331f/a6fc5965-280b-4b35-8040-9d6d9ac453a7.jpg"),
                simple.copy(url = "https://mangadex.org/covers/6e4ab519-495f-4dc9-9f8a-00f18de96fe8/afd51888-3e45-4101-8993-e0afc677c52e.jpg"),
            )
        }
    }

    /**
     * Update flows for external links
     */
    private fun updateMangaFlow() {
        presenterScope.launch {
            _currentManga.value = db.getManga(mangaId).executeOnIO()!!
        }
    }

    /**
     * Update flows for external links
     */
    private fun updateAltTitlesFlow() {
        presenterScope.launch {
            _altTitles.value = listOf("test1", "test2", "test3")
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
        presenterScope.launch {
            when (downloadAction) {
                is DownloadAction.Download -> downloadManager.downloadChapters(manga.value, chapterItems.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() })
                is DownloadAction.Remove -> deleteChapters(chapterItems, chapterItems.size == allChapters.value.size)
                is DownloadAction.Cancel -> deleteChapters(chapterItems, chapterItems.size == allChapters.value.size)
            }
        }
    }

    /**
     * Flips the bookmark status for the chapter
     */
    fun bookmarkChapter(chapterItem: ChapterItem) {
        presenterScope.launch {
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
        presenterScope.launch {
            val nextChapter = chapterSort.getNextUnreadChapter(activeChapters.value)?.chapter ?: null
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
        presenterScope.launch {
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
            _activeChapters.value = activeChapters.value.map {
                if (it.chapter.id == download.chapter.id) {
                    XLog.e("ESCO ${download.status} - ${download.progressFloat}")
                    it.copy(chapter = it.chapter, downloadState = download.status, downloadProgress = download.progressFloat)
                } else {
                    it
                }
            }
        }
    }

    //callback from Downloader
    override fun updateDownloads() {
        presenterScope.launch {
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
