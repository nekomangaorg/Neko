package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
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

    private val _allCategories = MutableStateFlow(emptyList<Category>())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    private val _mangaCategories = MutableStateFlow(emptyList<Category>())
    val mangaCategories: StateFlow<List<Category>> = _mangaCategories.asStateFlow()

    private val _loggedInTrackingService = MutableStateFlow(emptyList<TrackService>())
    val loggedInTrackingService: StateFlow<List<TrackService>> = _loggedInTrackingService.asStateFlow()

    private val _tracks = MutableStateFlow(emptyList<Track>())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        updateCategoryFlows()
        updateTrackingFlows()
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
    fun updateTrackStatus(statusIndex: Int, track: Track, service: TrackService) {
        presenterScope.launch {
            track.status = service.getStatusList()[statusIndex]
            if (service.isCompletedStatus(statusIndex) && track.total_chapters > 0) {
                track.last_chapter_read = track.total_chapters.toFloat()
            }
            updateTrackingService(track, service)
        }
    }

    /**
     * Update tracker with new score
     */
    fun updateTrackScore(scoreIndex: Int, track: Track, service: TrackService) {
        presenterScope.launch {
            track.score = service.indexToScore(scoreIndex)
            updateTrackingService(track, service)
        }
    }

    /**
     * Updates the remote tracking service with tracking changes
     */
    private fun updateTrackingService(track: Track, service: TrackService) {
        presenterScope.launch {
            val binding = try {
                service.update(track)
            } catch (e: Exception) {
                //trackError(e)
                null
            }
            if (binding != null) {
                withContext(Dispatchers.IO) { db.insertTrack(binding).executeAsBlocking() }
                updateTrackingFlows()
            } else {
                //trackRefreshDone()
            }
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
