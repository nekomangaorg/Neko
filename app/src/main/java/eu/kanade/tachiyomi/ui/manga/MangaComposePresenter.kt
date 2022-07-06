package eu.kanade.tachiyomi.ui.manga

import android.os.Build
import android.os.Environment
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.matchingTrack
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.ReadingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange.EditTrackingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingSuggestedDates
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nekomanga.domain.manga.Artwork
import org.threeten.bp.ZoneId
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date

class MangaComposePresenter(
    private val manga: Manga,
    val preferences: PreferencesHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    val chapterFilter: ChapterFilter = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val statusHandler: StatusHandler = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
) : BaseCoroutinePresenter<MangaComposeController>() {

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

    private val _isMerged = MutableStateFlow(manga.isMerged())
    val isMerged: StateFlow<Boolean> = _isMerged.asStateFlow()

    private val _currentArtwork = MutableStateFlow(
        Artwork(
            url = "",
            mangaId = manga.id!!,
            inLibrary = manga.favorite,
            originalArtwork = manga.thumbnail_url ?: "",
            vibrantColor = MangaCoverMetadata.getVibrantColor(manga.id!!),
        ),
    )
    val currentArtwork: StateFlow<Artwork> = _currentArtwork.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        if (!manga.initialized) {
            _isRefreshing.value = true
            //do stuff
            updateAllFlows()
            _isRefreshing.value = false
        } else {
            updateAllFlows()
        }
    }

    /**
     * Update all flows that dont require network
     */
    fun updateAllFlows() {
        updateCategoryFlows()
        updateTrackingFlows()
        updateExternalFlows()
        updateMergeFlow()
        updateArtworkFlow()
    }

    fun onRefresh() {
        _isRefreshing.value = true
        //do stuff
        presenterScope.launch {
            delay(1000L)
            _isRefreshing.value = false

        }
    }

    /**
     * Updates the database with categories for the manga
     */
    fun updateMangaCategories(manga: Manga, enabledCategories: List<Category>) {
        presenterScope.launch {
            val categories = enabledCategories.map { MangaCategory.create(manga, it) }
            db.setMangaCategories(categories, listOf(manga))
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
    fun getSuggestedDate() {
        presenterScope.launch {
            val chapters = db.getHistoryByMangaId(manga.id ?: 0L).executeOnIO()

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
                val results = service.search(title, manga, true)
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
                    manga_id = manga.id!!
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
            val tracks = db.getTracks(manga).executeOnIO().filter { it.sync_id == service.id }
            db.deleteTrackForManga(manga, service).executeOnIO()
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
                coverCache.getCustomCoverFile(manga).takeIf { it.exists() } ?: coverCache.getCoverFile(manga.thumbnail_url, manga.favorite)
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

        val filename = DiskUtil.buildValidFilename("${manga.title}${suffix}.${type.extension}")

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
        coverCache.setCustomCoverToCache(manga, url)
        MangaCoverMetadata.remove(manga)
        updateArtwork(url)
    }

    /**
     * Reset cover
     */
    fun resetCover() {
        coverCache.deleteCustomCover(manga)
        MangaCoverMetadata.remove(manga)
        updateArtwork()
    }

    /**
     * Updates the artwork flow
     */
    private fun updateArtwork(url: String = "") {
        presenterScope.launch {
            _currentArtwork.value = Artwork(url = url, inLibrary = manga.favorite, originalArtwork = manga.thumbnail_url ?: "", mangaId = manga.id!!)
        }
    }

    /**
     * Updates the flows for all categories, and manga categories
     */
    private fun updateCategoryFlows() {
        presenterScope.launch {
            _allCategories.value = db.getCategories().executeOnIO()
            _mangaCategories.value = db.getCategoriesForManga(manga).executeOnIO()
        }
    }

    /**
     * Update flows for tracking
     */
    private fun updateTrackingFlows() {
        presenterScope.launch {
            _loggedInTrackingService.value = trackManager.services.filter { it.isLogged() }
            _tracks.value = db.getTracks(manga).executeOnIO()

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
            _externalLinks.value = manga.getExternalLinks()
        }
    }

    /**
     * Update flows for merge
     */
    private fun updateMergeFlow() {
        presenterScope.launch {
            _isMerged.value = manga.isMerged()
        }
    }

    /**
     * Update the current artwork with the vibrant color
     */
    fun updateMangaColor(vibrantColor: Int) {
        MangaCoverMetadata.addVibrantColor(manga.id!!, vibrantColor)
        _currentArtwork.value = currentArtwork.value.copy(vibrantColor = vibrantColor)
    }

    /**
     * Update flows for merge
     */
    private fun updateArtworkFlow() {
        presenterScope.launch {
            _alternativeArtwork.value = listOf(
                Artwork(
                    url = "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/dfcaab7a-2c3c-4ea5-8641-abffd2a95b5f.jpg",
                    inLibrary = manga.favorite,
                    originalArtwork = manga.thumbnail_url ?: "",
                    mangaId = manga.id!!,
                ),
                Artwork(
                    url = "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/512496fb-6e57-483f-9380-aa6027d4f157.jpg",
                    inLibrary = manga.favorite,
                    originalArtwork = manga.thumbnail_url ?: "",
                    mangaId = manga.id!!,
                ),
                Artwork(
                    url = "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/d9497f0d-3bd7-42d9-832c-696ff39a6a28.jpg",
                    inLibrary = manga.favorite,
                    originalArtwork = manga.thumbnail_url ?: "",
                    mangaId = manga.id!!,
                ),
                Artwork(
                    url = "https://mangadex.org/covers/a96676e5-8ae2-425e-b549-7f15dd34a6d8/e393ec1a-320d-4ef7-92de-ca84b0d20309.jpg",
                    inLibrary = manga.favorite,
                    originalArtwork = manga.thumbnail_url ?: "",
                    mangaId = manga.id!!,
                ),
            )
        }
    }

    /**
     * Toggle a manga as favorite
     */
    fun toggleFavorite(): Boolean {
        manga.favorite = !manga.favorite

        when (manga.favorite) {
            true -> {
                manga.date_added = Date().time
                //TODO need to add to MDList as Plan to read if enabled see the other toggleFavorite in the detailsPResenter
            }
            false -> manga.date_added = 0
        }

        db.insertManga(manga).executeAsBlocking()
        return manga.favorite
    }
}
