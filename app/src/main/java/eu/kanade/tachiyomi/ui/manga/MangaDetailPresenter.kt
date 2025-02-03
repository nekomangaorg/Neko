package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.ui.state.ToggleableState
import androidx.core.text.isDigitsOnly
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.matchingTrack
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapterOfType
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.merged.komga.Komga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
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
import eu.kanade.tachiyomi.util.getMissingChapters
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.openInWebView
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.network.message
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.domain.track.toTrackItem
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaDetailPresenter(
    private val mangaId: Long,
    val preferences: PreferencesHelper = Injekt.get(),
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    chapterItemFilter: ChapterItemFilter = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    private val loginHelper: MangaDexLoginHelper = Injekt.get(),
    private val statusHandler: StatusHandler = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val mangaUpdateCoordinator: MangaUpdateCoordinator = Injekt.get(),
    private val trackingCoordinator: TrackingCoordinator = Injekt.get(),
    private val storageManager: StorageManager = Injekt.get(),
    private val chapterUseCases: ChapterUseCases = Injekt.get(),
) : BaseCoroutinePresenter<MangaDetailController>() {

    private val _currentManga = MutableStateFlow<Manga?>(null)
    val manga: StateFlow<Manga?> = _currentManga.asStateFlow()

    private fun currentManga(): Manga {
        if (_currentManga.value == null) {
            val dbManga = db.getManga(mangaId).executeAsBlocking()
            _currentManga.value = dbManga
        }
        return _currentManga.value!!
    }

    private val _generalState = MutableStateFlow(MangaConstants.MangaScreenGeneralState())
    val generalState: StateFlow<MangaConstants.MangaScreenGeneralState> =
        _generalState.asStateFlow()

    private val _mangaState =
        MutableStateFlow(
            MangaConstants.MangaScreenMangaState(currentArtwork = Artwork(mangaId = mangaId))
        )
    val mangaState: StateFlow<MangaConstants.MangaScreenMangaState> = _mangaState.asStateFlow()

    private val _trackMergeState = MutableStateFlow(MangaConstants.MangaScreenTrackMergeState())
    val trackMergeState: StateFlow<MangaConstants.MangaScreenTrackMergeState> =
        _trackMergeState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _snackbarState = MutableSharedFlow<SnackbarState>()
    val snackBarState: SharedFlow<SnackbarState> = _snackbarState.asSharedFlow()

    private val chapterSort = ChapterItemSort(chapterItemFilter, preferences)

    override fun onCreate() {
        super.onCreate()

        LibraryUpdateJob.updateFlow
            .filter { it == currentManga().id }
            .onEach(::onUpdateManga)
            .launchIn(presenterScope)
        presenterScope.launch {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            _currentManga.value = dbManga
            val validMergeTypes =
                when (sourceManager.komga.hasCredentials()) {
                    true ->
                        MergeType.entries.filterNot { it == MergeType.MangaLife }.toPersistentList()
                    false ->
                        MergeType.entries
                            .filterNot { it == MergeType.Komga }
                            .filterNot { it == MergeType.MangaLife }
                            .toPersistentList()
                }

            _generalState.value =
                MangaConstants.MangaScreenGeneralState(
                    hasDefaultCategory = preferences.defaultCategory().get() != -1,
                    hideButtonText = mangaDetailsPreferences.hideButtonText().get(),
                    extraLargeBackdrop = mangaDetailsPreferences.extraLargeBackdrop().get(),
                    forcePortrait = mangaDetailsPreferences.forcePortrait().get(),
                    themeBasedOffCovers = mangaDetailsPreferences.autoThemeByCover().get(),
                    wrapAltTitles = mangaDetailsPreferences.wrapAltTitles().get(),
                    validMergeTypes = validMergeTypes,
                    vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId),
                )
            if (!currentManga().initialized) {
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
        observeDownloads()
    }

    /** Update all flows */
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
                        mangaState.copy(
                            currentArtwork = currentArtwork,
                            alternativeArtwork = altArtwork,
                        )
                    }
                    updateChapterFlows()
                    updateFilterFlow()
                }
                .onFailure { TimberKt.e(it) { "Error trying to update manga in all flows" } }
        }
        updateTrackingFlows(true)
    }

    /** Refresh manga info, and chapters */
    fun onRefresh() {
        presenterScope.launchIO {
            if (!isOnline()) {
                _snackbarState.emit(SnackbarState(messageRes = R.string.no_network_connection))
                return@launchIO
            }

            _isRefreshing.value = true

            mangaUpdateCoordinator.update(currentManga(), presenterScope).collect { result ->
                when (result) {
                    is MangaResult.Error -> {
                        _snackbarState.emit(
                            SnackbarState(message = result.text, messageRes = result.id)
                        )
                        _isRefreshing.value = false
                    }
                    is MangaResult.Success -> {
                        updateAllFlows()
                        syncChaptersReadStatus()
                        _isRefreshing.value = false
                    }
                    is MangaResult.UpdatedChapters -> Unit
                    is MangaResult.UpdatedManga -> {
                        updateMangaFlow()
                    }
                    is MangaResult.UpdatedArtwork -> {
                        updateArtworkFlow()
                    }
                    is MangaResult.ChaptersRemoved -> {
                        val removedChapters =
                            generalState.value.allChapters.filter {
                                it.chapter.id in result.chapterIdsRemoved && it.isDownloaded
                            }

                        if (removedChapters.isNotEmpty()) {
                            when (preferences.deleteRemovedChapters().get()) {
                                2 -> deleteChapters(removedChapters)
                                1 -> Unit
                                else -> {
                                    _generalState.update {
                                        it.copy(removedChapters = removedChapters.toImmutableList())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Search filtered chapters */
    fun onSearch(search: String?) {
        presenterScope.launchIO {
            val searchActive = search != null

            _isSearching.value = searchActive

            val filteredChapters =
                when (searchActive) {
                    true -> {
                        _generalState.value.activeChapters.filter {
                            it.chapter.chapterTitle.contains(search!!, true) ||
                                it.chapter.scanlator.contains(search, true) ||
                                it.chapter.name.contains(search, true)
                        }
                    }
                    false -> emptyList()
                }

            _generalState.update { it.copy(searchChapters = filteredChapters.toPersistentList()) }
        }
    }

    /** Updates the database with categories for the manga */
    fun updateMangaCategories(enabledCategories: List<CategoryItem>) {
        presenterScope.launchIO {
            val categories =
                enabledCategories.map { MangaCategory.create(currentManga(), it.toDbCategory()) }
            db.setMangaCategories(categories, listOf(currentManga()))
            updateCategoryFlows()
        }
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        presenterScope.launchIO {
            val category = Category.create(newCategory)
            db.insertCategory(category).executeAsBlocking()
            updateCategoryFlows()
        }
    }

    /** Update tracker with new status */
    fun updateTrackStatus(statusIndex: Int, trackAndService: TrackAndService) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackStatus(statusIndex, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Update tracker with new score */
    fun updateTrackScore(scoreIndex: Int, trackAndService: TrackAndService) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackScore(scoreIndex, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Update the tracker with the new chapter information */
    fun updateTrackChapter(newChapterNumber: Int, trackAndService: TrackAndService) {
        presenterScope.launchIO {
            val trackingUpdate =
                trackingCoordinator.updateTrackChapter(newChapterNumber, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Handle the TrackingUpdate */
    private suspend fun handleTrackingUpdate(
        trackingUpdate: TrackingUpdate,
        updateTrackFlows: Boolean = true,
    ) {
        when (trackingUpdate) {
            is TrackingUpdate.Error -> {
                TimberKt.e(trackingUpdate.exception) { "handle tracking update had error" }
                _snackbarState.emit(SnackbarState(message = trackingUpdate.message))
            }
            is TrackingUpdate.Success -> {
                if (updateTrackFlows) {
                    updateTrackingFlows()
                }
            }
        }
    }

    /** Figures out the suggested reading dates */
    private fun getSuggestedDate() {
        presenterScope.launchIO {
            val chapters = db.getHistoryByMangaId(mangaId).executeOnIO()

            _generalState.update { state ->
                state.copy(
                    trackingSuggestedDates =
                        TrackingSuggestedDates(
                            startDate = chapters.minOfOrNull { it.last_read } ?: 0L,
                            finishedDate = chapters.maxOfOrNull { it.last_read } ?: 0L,
                        )
                )
            }
        }
    }

    /** Update the tracker with the start/finished date */
    fun updateTrackDate(trackDateChange: TrackDateChange) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackDate(trackDateChange)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Refresh tracking from trackers */
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

            val asyncList =
                trackMergeState.value.tracks
                    .filter { trackManager.getService(it.trackServiceId) != null }
                    .filter { trackManager.getService(it.trackServiceId)!!.isLogged() }
                    .map { trackItem ->
                        val service = trackManager.getService(trackItem.trackServiceId)!!
                        async(Dispatchers.IO) {
                            kotlin
                                .runCatching { service.refresh(trackItem.toDbTrack()) }
                                .onFailure {
                                    if (it !is CancellationException) {
                                        TimberKt.e(it) { "error refreshing tracker" }
                                        delay(3000)
                                        _snackbarState.emit(
                                            SnackbarState(
                                                message = it.message,
                                                fieldRes = service.nameRes(),
                                                messageRes = R.string.error_refreshing_,
                                            )
                                        )
                                    }
                                }
                                .onSuccess { track -> db.insertTrack(track).executeOnIO() }
                        }
                    }

            asyncList.awaitAll()
            updateTrackingFlows(false)
        }
    }

    private fun syncChaptersReadStatus() {
        presenterScope.launchIO {
            if (!preferences.readingSync().get() || !loginHelper.isLoggedIn() || !isOnline())
                return@launchIO

            runCatching {
                    statusHandler.getReadChapterIds(currentManga().uuid()).collect { chapterIds ->
                        val chaptersToMarkRead =
                            generalState.value.allChapters
                                .asSequence()
                                .filter { !it.chapter.isMergedChapter() }
                                .filter { chapterIds.contains(it.chapter.mangaDexChapterId) }
                                .toList()
                        if (chaptersToMarkRead.isNotEmpty()) {
                            markChapters(
                                chaptersToMarkRead,
                                ChapterMarkActions.Read(),
                                skipSync = true,
                            )
                        }
                    }
                }
                .onFailure {
                    TimberKt.e(it) { "Error trying to mark chapters read from MangaDex" }
                    presenterScope.launch {
                        delay(3000)
                        _snackbarState.emit(
                            SnackbarState("Error trying to mark chapters read from MangaDex $it")
                        )
                    }
                }
        }
    }

    /** Search Tracker */
    fun searchTracker(title: String, service: TrackServiceItem) {
        presenterScope.launchIO {
            val previouslyTracked =
                trackMergeState.value.tracks.firstOrNull { service.id == it.trackServiceId } != null
            trackingCoordinator
                .searchTracker(title, service, currentManga(), previouslyTracked)
                .collect { result ->
                    _trackMergeState.update { it.copy(trackSearchResult = result) }
                }
        }
    }

    /** Register tracker with service */
    fun registerTracking(trackAndService: TrackAndService, skipTrackFlowUpdate: Boolean = false) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.registerTracking(trackAndService, mangaId)
            handleTrackingUpdate(trackingUpdate, !skipTrackFlowUpdate)
        }
    }

    /** Remove a tracker with an option to remove it from the tracking service */
    fun removeTracking(alsoRemoveFromTracker: Boolean, service: TrackServiceItem) {
        presenterScope.launchIO {
            val trackingUpdate =
                trackingCoordinator.removeTracking(alsoRemoveFromTracker, service, mangaId)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /**
     * share the cover that is written in the destination folder. If a url is passed in then share
     * that one instead of the manga thumbnail url one
     */
    suspend fun shareMangaCover(destDir: UniFile, artwork: Artwork): Uri? {
        return withIOContext {
            return@withIOContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    saveCover(destDir, artwork)
                } catch (e: java.lang.Exception) {
                    TimberKt.e(e) { "share manga cover exception" }
                    null
                }
            } else {
                // returns null because before Q, the share sheet can't show the cover
                null
            }
        }
    }

    /** Save the given url cover to file */
    fun saveCover(artwork: Artwork, destDir: UniFile? = null) {
        presenterScope.launchIO {
            try {
                val directory = destDir ?: storageManager.getCoverDirectory()!!

                val destinationUri = saveCover(directory, artwork)
                launchUI {
                    view?.applicationContext?.let { context ->
                        DiskUtil.scanMedia(context, destinationUri)
                        view?.applicationContext?.toast(R.string.cover_saved)
                    }
                }
            } catch (e: Exception) {
                TimberKt.e(e) { "error saving cover" }
                launchUI { view?.applicationContext?.toast("Error saving cover") }
            }
        }
    }

    /** Save Cover to directory, if given a url save that specific cover */
    private fun saveCover(directory: UniFile, artwork: Artwork): Uri {
        val cover =
            when (artwork.url.isBlank() || currentManga().thumbnail_url == artwork.url) {
                true ->
                    coverCache.getCustomCoverFile(currentManga()).takeIf { it.exists() }
                        ?: coverCache.getCoverFile(
                            currentManga().thumbnail_url,
                            currentManga().favorite,
                        )
                false -> coverCache.getCoverFile(artwork.url)
            }

        val type = ImageUtil.findImageType(cover.inputStream()) ?: throw Exception("Not an image")

        // Build destination file.
        val fileNameNoExtension =
            listOfNotNull(
                    currentManga().title,
                    artwork.volume.ifEmpty { null },
                    currentManga().uuid(),
                )
                .joinToString("-")

        val filename = DiskUtil.buildValidFilename("$fileNameNoExtension.${type.extension}")

        val destFile = directory.createFile(filename)!!

        cover.inputStream().use { input ->
            destFile.openOutputStream().use { output -> input.copyTo(output) }
        }
        return destFile.uri
    }

    /** Set custom cover */
    fun setCover(artwork: Artwork) {
        presenterScope.launchIO {
            coverCache.setCustomCoverToCache(currentManga(), artwork.url)
            MangaCoverMetadata.remove(mangaId)
            val manga = currentManga()
            manga.user_cover = artwork.url
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateArtworkFlow()
        }
    }

    /** Reset cover */
    fun resetCover() {
        presenterScope.launchIO {
            coverCache.deleteCustomCover(currentManga())
            MangaCoverMetadata.remove(mangaId)
            val manga = currentManga()
            manga.user_cover = null
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateArtworkFlow()
        }
    }

    /** Set custom title or resets if given null */
    fun setAltTitle(title: String?) {
        presenterScope.launchIO {
            val previousTitle = mangaState.value.currentTitle
            val newTitle = title ?: currentManga().originalTitle
            _mangaState.update { it.copy(currentTitle = newTitle) }

            val manga = currentManga()
            manga.user_title = title
            db.insertManga(manga).executeOnIO()
            updateMangaFlow()

            _snackbarState.emit(
                SnackbarState(
                    messageRes = R.string.updated_title_to_,
                    message = newTitle,
                    actionLabelRes = R.string.undo,
                    action = { setAltTitle(previousTitle) },
                )
            )
        }
    }

    /** Remove merged manga entry */
    fun removeMergedManga(mergeType: MergeType) {
        presenterScope.launchIO {
            db.deleteMergeMangaForType(mangaId, mergeType).executeAsBlocking()
            updateMangaFlow()

            val mergedChapters =
                db.getChapters(currentManga()).executeOnIO().filter {
                    it.isMergedChapterOfType(mergeType)
                }

            downloadManager.deleteChapters(mergedChapters, currentManga())
            db.deleteChapters(mergedChapters).executeOnIO()
            updateAllFlows()
        }
    }

    fun searchMergedManga(query: String, mergeType: MergeType) {
        presenterScope.launchIO {
            _trackMergeState.update { it.copy(mergeSearchResult = MergeSearchResult.Loading) }

            runCatching {
                    MergeType.getSource(mergeType, sourceManager)
                    val mergedMangaResults =
                        MergeType.getSource(mergeType, sourceManager).searchManga(query).map {
                            SourceMergeManga(
                                coverUrl = it.thumbnail_url ?: "",
                                title = it.title,
                                url = it.url,
                                mergeType = mergeType,
                            )
                        }

                    _trackMergeState.update {
                        when (mergedMangaResults.isEmpty()) {
                            true ->
                                trackMergeState.value.copy(
                                    mergeSearchResult = MergeSearchResult.NoResult
                                )
                            false ->
                                trackMergeState.value.copy(
                                    mergeSearchResult =
                                        MergeSearchResult.Success(mergedMangaResults)
                                )
                        }
                    }
                }
                .getOrElse { error ->
                    TimberKt.e(error) { "Error searching merged manga" }
                    _trackMergeState.update {
                        it.copy(
                            mergeSearchResult =
                                MergeSearchResult.Error(
                                    error.message ?: "Error looking up information"
                                )
                        )
                    }
                }
        }
    }

    /** Attach the selected merge manga entry */
    fun addMergedManga(mergeManga: SourceMergeManga) {
        presenterScope.launchIO {
            val newMergedManga = mergeManga.toMergeMangaImpl(mangaId)
            db.insertMergeManga(newMergedManga).executeOnIO()
            updateMangaFlow()
            onRefresh()
        }
    }

    /** Updates the artwork flow */
    private fun updateArtworkFlow() {
        presenterScope.launchIO {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            val currentArtwork = createCurrentArtwork(dbManga)

            val altArtwork = createAltArtwork(currentManga(), currentArtwork)
            _mangaState.update {
                it.copy(
                    alternativeArtwork = altArtwork.toImmutableList(),
                    currentArtwork = currentArtwork,
                )
            }
        }
    }

    private fun createCurrentArtwork(manga: Manga): Artwork {
        return Artwork(
            url = manga.user_cover ?: "",
            inLibrary = manga.favorite,
            originalArtwork = manga.thumbnail_url ?: "",
            mangaId = mangaId,
        )
    }

    private fun createAltArtwork(manga: Manga, currentArtwork: Artwork): ImmutableList<Artwork> {
        val quality = preferences.thumbnailQuality().get()

        return db.getArtwork(mangaId)
            .executeAsBlocking()
            .map { aw ->
                Artwork(
                    mangaId = aw.mangaId,
                    url = MdUtil.cdnCoverUrl(manga.uuid(), aw.fileName, quality),
                    volume = aw.volume,
                    description = aw.description,
                    active =
                        currentArtwork.url.contains(aw.fileName) ||
                            (currentArtwork.url.isBlank() &&
                                currentArtwork.originalArtwork.contains(aw.fileName)),
                )
            }
            .toImmutableList()
    }

    private fun updateVibrantColorFlow() {
        presenterScope.launch {
            _generalState.update {
                it.copy(vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId))
            }
        }
    }

    /** Updates the visible chapters for a manga */
    private fun updateChapterFlows() {
        presenterScope.launchIO {
            // possibly move this into a chapter repository
            val blockedScanlators = preferences.blockedScanlators().get()
            val allChapters =
                db.getChapters(mangaId)
                    .executeOnIO()
                    .mapNotNull { it.toSimpleChapter() }
                    .filter {
                        it.scanlatorList().none { scanlator ->
                            blockedScanlators.contains(scanlator)
                        }
                    }
                    .map { chapter ->
                        val downloadState =
                            when {
                                downloadManager.isChapterDownloaded(
                                    chapter.toDbChapter(),
                                    currentManga(),
                                ) -> Download.State.DOWNLOADED
                                else -> {
                                    val download =
                                        downloadManager.getQueuedDownloadOrNull(chapter.id)
                                    when (download == null) {
                                        true -> Download.State.NOT_DOWNLOADED
                                        false -> download.status
                                    }
                                }
                            }

                        ChapterItem(
                            chapter = chapter,
                            downloadState = downloadState,
                            downloadProgress =
                                when (downloadState == Download.State.DOWNLOADING) {
                                    true ->
                                        downloadManager
                                            .getQueuedDownloadOrNull(chapter.id)!!
                                            .progress
                                    false -> 0
                                },
                        )
                    }
            _generalState.update { it.copy(allChapters = allChapters.toImmutableList()) }

            val allChapterScanlators =
                allChapters.flatMap { ChapterUtil.getScanlators(it.chapter.scanlator) }.toSet()
            if (
                allChapterScanlators.size == 1 &&
                    !currentManga().filtered_scanlators.isNullOrEmpty()
            ) {
                updateMangaScanlator(emptySet())
            }

            val allLanguages =
                allChapters.flatMap { ChapterUtil.getLanguages(it.chapter.language) }.toSet()

            _generalState.update {
                it.copy(
                    activeChapters =
                        chapterSort
                            .getChaptersSorted(currentManga(), allChapters)
                            .toImmutableList(),
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

    /** Updates the filtered languages */
    private fun updateMangaScanlator(filteredScanlators: Set<String>) {
        presenterScope.launchIO {
            val manga = currentManga()
            manga.filtered_scanlators =
                when (filteredScanlators.isEmpty()) {
                    true -> null
                    false -> ChapterUtil.getScanlatorString(filteredScanlators)
                }

            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateFilterFlow()
        }
    }

    /** Updates the filtered scanlators */
    private fun updateMangaFilteredLanguages(filteredLanguages: Set<String>) {
        presenterScope.launchIO {
            val manga = currentManga()
            manga.filtered_language =
                when (filteredLanguages.isEmpty()) {
                    true -> null
                    false -> ChapterUtil.getLanguageString(filteredLanguages)
                }

            db.insertManga(manga).executeOnIO()
            updateMangaFlow()
            updateFilterFlow()
        }
    }

    /** Updates the flows for all categories, and manga categories */
    private fun updateCategoryFlows() {
        presenterScope.launch {
            val categories = db.getCategories().executeAsBlocking()
            val mangaCategories = db.getCategoriesForManga(mangaId).executeAsBlocking()

            _generalState.update { state ->
                state.copy(
                    allCategories = categories.map { it.toCategoryItem() }.toImmutableList(),
                    currentCategories =
                        mangaCategories.map { it.toCategoryItem() }.toImmutableList(),
                )
            }
        }
    }

    /** Update flows for tracking */
    private fun updateTrackingFlows(checkForMissingTrackers: Boolean = false) {
        presenterScope.launchIO {
            _trackMergeState.update { mergeState ->
                mergeState.copy(
                    loggedInTrackService =
                        trackManager.services
                            .filter { it.value.isLogged() }
                            .map { it.value.toTrackServiceItem() }
                            .toImmutableList(),
                    tracks =
                        db.getTracks(mangaId)
                            .executeAsBlocking()
                            .map { it.toTrackItem() }
                            .toImmutableList(),
                )
            }

            if (checkForMissingTrackers) {
                val autoAddTracker = preferences.autoAddTracker().get()

                // Always add the mdlist initial unfollowed tracker, also add it as PTR if need be
                trackMergeState.value.loggedInTrackService
                    .firstOrNull { it.isMdList }
                    ?.let { _ ->
                        val mdList = trackManager.mdList

                        var track =
                            trackMergeState.value.tracks
                                .firstOrNull { mdList.matchingTrack(it) }
                                ?.toDbTrack()

                        if (track == null) {
                            track = mdList.createInitialTracker(currentManga())
                            db.insertTrack(track).executeOnIO()
                            if (isOnline()) {
                                runCatching { mdList.bind(track) }
                                    .onFailure { exception ->
                                        TimberKt.e(exception) {
                                            "Error trying to bind tracking info for mangadex"
                                        }
                                    }
                            }
                            db.insertTrack(track).executeOnIO()
                        }
                        val shouldAddAsPlanToRead =
                            currentManga().favorite &&
                                preferences.addToLibraryAsPlannedToRead().get() &&
                                FollowStatus.isUnfollowed(track.status)
                        if (shouldAddAsPlanToRead && isOnline()) {
                            track.status = FollowStatus.PLAN_TO_READ.int
                            trackingCoordinator.updateTrackingService(
                                track.toTrackItem(),
                                trackManager.mdList.toTrackServiceItem(),
                            )
                        }
                    }

                if (autoAddTracker.size > 1 && currentManga().favorite) {
                    val validContentRatings = preferences.autoTrackContentRatingSelections().get()
                    val contentRating = currentManga().getContentRating()
                    if (
                        (contentRating == null) ||
                            validContentRatings.contains(contentRating.lowercase())
                    ) {
                        autoAddTracker
                            .map { it.toInt() }
                            .map { autoAddTrackerId ->
                                async {
                                    trackMergeState.value.loggedInTrackService
                                        .firstOrNull { it.id == autoAddTrackerId }
                                        ?.let { trackService ->
                                            val id =
                                                trackManager.getIdFromManga(
                                                    trackService,
                                                    currentManga(),
                                                )
                                            if (
                                                id != null &&
                                                    !trackMergeState.value.tracks.any {
                                                        trackService.id == it.trackServiceId
                                                    }
                                            ) {
                                                if (!isOnline()) {
                                                    launchUI {
                                                        _snackbarState.emit(
                                                            SnackbarState(
                                                                message =
                                                                    "No network connection, cannot autolink tracker"
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    val trackResult =
                                                        trackingCoordinator.searchTrackerNonFlow(
                                                            "",
                                                            trackManager
                                                                .getService(trackService.id)!!
                                                                .toTrackServiceItem(),
                                                            currentManga(),
                                                            false,
                                                        )

                                                    if (
                                                        trackResult
                                                            is
                                                            TrackingConstants.TrackSearchResult.Success
                                                    ) {
                                                        val trackSearchItem =
                                                            trackResult.trackSearchResult[0]
                                                        val trackingUpdate =
                                                            trackingCoordinator.registerTracking(
                                                                TrackAndService(
                                                                    trackSearchItem.trackItem,
                                                                    trackService,
                                                                ),
                                                                mangaId,
                                                            )
                                                        handleTrackingUpdate(trackingUpdate, false)
                                                    } else if (
                                                        trackResult
                                                            is
                                                            TrackingConstants.TrackSearchResult.Error
                                                    ) {
                                                        launchUI {
                                                            _snackbarState.emit(
                                                                SnackbarState(
                                                                    prefixRes =
                                                                        trackResult.trackerNameRes,
                                                                    message =
                                                                        " error trying to autolink tracking.  ${trackResult.errorMessage}",
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                            .awaitAll()
                    }
                }
                // update the tracks incase they were updated above
                _trackMergeState.update { mergeState ->
                    mergeState.copy(
                        tracks =
                            db.getTracks(mangaId)
                                .executeAsBlocking()
                                .map { it.toTrackItem() }
                                .toImmutableList()
                    )
                }
            }

            getSuggestedDate()

            val trackCount =
                trackMergeState.value.loggedInTrackService.count { trackServiceItem ->
                    val trackService = trackManager.getService(trackServiceItem.id)!!
                    trackMergeState.value.tracks.any { track ->
                        // return true if track matches and not MDList
                        // or track matches and MDlist is anything but Unfollowed
                        trackService.matchingTrack(track) &&
                            (!trackService.isMdList() ||
                                (trackService.isMdList() &&
                                    !FollowStatus.isUnfollowed(track.status)))
                    }
                }

            _generalState.update { it.copy(trackServiceCount = trackCount) }
        }
    }

    /** Get Manga Description */
    private fun getDescription(): String {
        return when {
            currentManga().uuid().isDigitsOnly() -> "THIS MANGA IS NOT MIGRATED TO V5"
            !currentManga().description.isNullOrEmpty() -> currentManga().description!!
            !currentManga().initialized -> ""
            else -> "No description"
        }
    }

    /** Get current sort filter */
    private fun getSortFilter(): MangaConstants.SortFilter {
        val manga = currentManga()
        val sortOrder = manga.chapterOrder(mangaDetailsPreferences)
        val status =
            when (manga.sortDescending(mangaDetailsPreferences)) {
                true -> MangaConstants.SortState.Descending
                false -> MangaConstants.SortState.Ascending
            }

        val matchesDefaults = mangaSortMatchesDefault(manga)

        return when (sortOrder) {
            Manga.CHAPTER_SORTING_NUMBER ->
                MangaConstants.SortFilter(
                    chapterNumberSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )
            Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                MangaConstants.SortFilter(
                    uploadDateSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )
            else ->
                MangaConstants.SortFilter(
                    sourceOrderSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )
        }
    }

    /** Get current sort filter */
    private fun getFilter(): MangaConstants.ChapterDisplay {
        val manga = currentManga()
        val read =
            when (manga.readFilter(mangaDetailsPreferences)) {
                Manga.CHAPTER_SHOW_UNREAD -> ToggleableState.On
                Manga.CHAPTER_SHOW_READ -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }
        val bookmark =
            when (manga.bookmarkedFilter(mangaDetailsPreferences)) {
                Manga.CHAPTER_SHOW_BOOKMARKED -> ToggleableState.On
                Manga.CHAPTER_SHOW_NOT_BOOKMARKED -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }

        val downloaded =
            when (manga.downloadedFilter(mangaDetailsPreferences)) {
                Manga.CHAPTER_SHOW_DOWNLOADED -> ToggleableState.On
                Manga.CHAPTER_SHOW_NOT_DOWNLOADED -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }

        val hideTitle =
            when (manga.hideChapterTitle(mangaDetailsPreferences)) {
                true -> ToggleableState.On
                else -> ToggleableState.Off
            }

        val all =
            read == ToggleableState.Off &&
                bookmark == ToggleableState.Off &&
                downloaded == ToggleableState.Off

        val matchesDefaults = mangaFilterMatchesDefault(manga)

        return MangaConstants.ChapterDisplay(
            showAll = all,
            unread = read,
            downloaded = downloaded,
            bookmarked = bookmark,
            hideChapterTitles = hideTitle,
            matchesGlobalDefaults = matchesDefaults,
        )
    }

    /** Get scanlator filter */
    private fun getScanlatorFilter(): MangaConstants.ScanlatorFilter {
        val filteredScanlators =
            ChapterUtil.getScanlators(currentManga().filtered_scanlators).toSet()
        val scanlatorOptions =
            generalState.value.allScanlators
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .map { scanlator ->
                    MangaConstants.ScanlatorOption(
                        name = scanlator,
                        disabled = filteredScanlators.contains(scanlator),
                    )
                }
        return MangaConstants.ScanlatorFilter(scanlators = scanlatorOptions.toImmutableList())
    }

    /** Get scanlator filter */
    private fun getLangaugeFilter(): MangaConstants.LanguageFilter {
        val filteredLanguages = ChapterUtil.getLanguages(currentManga().filtered_language).toSet()
        val languageOptions =
            generalState.value.allLanguages
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .map { language ->
                    MangaConstants.LanguageOption(
                        name = language,
                        disabled = filteredLanguages.contains(language),
                    )
                }
        return MangaConstants.LanguageFilter(languages = languageOptions.toImmutableList())
    }

    /** Get hide titles */
    private fun getHideTitlesFilter(): Boolean {
        return currentManga().hideChapterTitle(mangaDetailsPreferences)
    }

    private fun getFilterText(
        chapterDisplay: MangaConstants.ChapterDisplay,
        chapterScanlatorFilter: MangaConstants.ScanlatorFilter,
    ): String {
        val hasDisabledScanlators = chapterScanlatorFilter.scanlators.any { it.disabled }
        val filtersId = mutableListOf<Int?>()
        filtersId.add(
            if (chapterDisplay.unread == ToggleableState.Indeterminate) R.string.read else null
        )
        filtersId.add(if (chapterDisplay.unread == ToggleableState.On) R.string.unread else null)
        filtersId.add(
            if (chapterDisplay.downloaded == ToggleableState.On) R.string.downloaded else null
        )
        filtersId.add(
            if (chapterDisplay.downloaded == ToggleableState.Indeterminate) R.string.not_downloaded
            else null
        )
        filtersId.add(
            if (chapterDisplay.bookmarked == ToggleableState.On) R.string.bookmarked else null
        )
        filtersId.add(
            if (chapterDisplay.bookmarked == ToggleableState.Indeterminate) R.string.not_bookmarked
            else null
        )
        filtersId.add(if (hasDisabledScanlators) R.string.scanlators else null)
        return filtersId.filterNotNull().joinToString(", ") { preferences.context.getString(it) }
    }

    /** Get change Sort option */
    fun changeSortOption(sortOption: SortOption?) {
        presenterScope.launchIO {
            val manga = currentManga()

            if (sortOption == null) {
                manga.setSortToGlobal()
            } else {
                val sortInt =
                    when (sortOption.sortType) {
                        MangaConstants.SortType.ChapterNumber -> Manga.CHAPTER_SORTING_NUMBER
                        MangaConstants.SortType.SourceOrder -> Manga.CHAPTER_SORTING_SOURCE
                        MangaConstants.SortType.UploadDate -> Manga.CHAPTER_SORTING_UPLOAD_DATE
                    }
                val descInt =
                    when (sortOption.sortState) {
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

    /** Get current merge result */
    fun changeFilterOption(filterOption: MangaConstants.ChapterDisplayOptions?) {
        presenterScope.launchIO {
            val manga = currentManga()

            if (
                !manga.usesLocalFilter &&
                    manga.readFilter(mangaDetailsPreferences) == Manga.SHOW_ALL &&
                    manga.downloadedFilter(mangaDetailsPreferences) == Manga.SHOW_ALL &&
                    manga.bookmarkedFilter(mangaDetailsPreferences) == Manga.SHOW_ALL
            ) {
                manga.readFilter = Manga.SHOW_ALL
                manga.bookmarkedFilter = Manga.SHOW_ALL
                manga.downloadedFilter = Manga.SHOW_ALL
            }

            if (filterOption == null) {
                manga.setFilterToGlobal()
            } else {
                when (filterOption.displayType) {
                    MangaConstants.ChapterDisplayType.All -> {
                        manga.readFilter = Manga.SHOW_ALL
                        manga.bookmarkedFilter = Manga.SHOW_ALL
                        manga.downloadedFilter = Manga.SHOW_ALL
                    }
                    MangaConstants.ChapterDisplayType.Unread -> {
                        manga.readFilter =
                            when (filterOption.displayState) {
                                ToggleableState.On -> Manga.CHAPTER_SHOW_UNREAD
                                ToggleableState.Indeterminate -> Manga.CHAPTER_SHOW_READ
                                else -> Manga.SHOW_ALL
                            }
                    }
                    MangaConstants.ChapterDisplayType.Bookmarked -> {
                        manga.bookmarkedFilter =
                            when (filterOption.displayState) {
                                ToggleableState.On -> Manga.CHAPTER_SHOW_BOOKMARKED
                                ToggleableState.Indeterminate -> Manga.CHAPTER_SHOW_NOT_BOOKMARKED
                                else -> Manga.SHOW_ALL
                            }
                    }
                    MangaConstants.ChapterDisplayType.Downloaded -> {
                        manga.downloadedFilter =
                            when (filterOption.displayState) {
                                ToggleableState.On -> Manga.CHAPTER_SHOW_DOWNLOADED
                                ToggleableState.Indeterminate -> Manga.CHAPTER_SHOW_NOT_DOWNLOADED
                                else -> Manga.SHOW_ALL
                            }
                    }
                    MangaConstants.ChapterDisplayType.HideTitles -> {
                        manga.displayMode =
                            when (filterOption.displayState) {
                                ToggleableState.On -> Manga.CHAPTER_DISPLAY_NUMBER
                                else -> Manga.CHAPTER_DISPLAY_NAME
                            }
                    }
                }
                manga.setFilterToLocal()
                if (mangaFilterMatchesDefault(manga)) {
                    manga.setFilterToGlobal()
                }
            }

            db.updateChapterFlags(manga).executeOnIO()
            updateMangaFlow()
            updateFilterFlow()
            updateChapterFlows()
        }
    }

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun changeScanlatorOption(scanlatorOption: MangaConstants.ScanlatorOption?) {
        presenterScope.launchIO {
            val newFilteredScanlators =
                if (scanlatorOption != null) {
                    val filteredScanlators =
                        ChapterUtil.getScanlators(currentManga().filtered_scanlators).toMutableSet()
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

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun changeLanguageOption(languageOptions: MangaConstants.LanguageOption?) {
        presenterScope.launchIO {
            val manga = currentManga()

            val newFilteredLanguages =
                if (languageOptions != null) {
                    val filteredLanguages =
                        ChapterUtil.getLanguages(manga.filtered_language).toMutableSet()
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

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun setGlobalOption(option: MangaConstants.SetGlobal) {
        presenterScope.launchIO {
            val manga = currentManga()
            when (option) {
                MangaConstants.SetGlobal.Sort -> {
                    mangaDetailsPreferences.sortChapterOrder().set(manga.sorting)
                    mangaDetailsPreferences.chaptersDescAsDefault().set(manga.sortDescending)
                    manga.setSortToGlobal()
                }
                MangaConstants.SetGlobal.Filter -> {
                    mangaDetailsPreferences.filterChapterByRead().set(manga.readFilter)
                    mangaDetailsPreferences.filterChapterByDownloaded().set(manga.downloadedFilter)
                    mangaDetailsPreferences.filterChapterByBookmarked().set(manga.bookmarkedFilter)
                    mangaDetailsPreferences
                        .hideChapterTitlesByDefault()
                        .set(manga.hideChapterTitles)
                    manga.setFilterToGlobal()
                }
            }
            db.updateChapterFlags(manga).executeOnIO()
            updateMangaFlow()
            updateFilterFlow()
        }
    }

    private fun mangaSortMatchesDefault(manga: Manga): Boolean {
        return (manga.sortDescending == mangaDetailsPreferences.chaptersDescAsDefault().get() &&
            manga.sorting == mangaDetailsPreferences.sortChapterOrder().get()) ||
            !manga.usesLocalSort
    }

    private fun mangaFilterMatchesDefault(manga: Manga): Boolean {
        return (manga.readFilter == mangaDetailsPreferences.filterChapterByRead().get() &&
            manga.downloadedFilter == mangaDetailsPreferences.filterChapterByDownloaded().get() &&
            manga.bookmarkedFilter == mangaDetailsPreferences.filterChapterByBookmarked().get() &&
            manga.hideChapterTitles ==
                mangaDetailsPreferences.hideChapterTitlesByDefault().get()) ||
            !manga.usesLocalFilter
    }

    /** Update the current artwork with the vibrant color */
    fun updateMangaColor(vibrantColor: Int) {
        MangaCoverMetadata.addVibrantColor(mangaId, vibrantColor)
        updateVibrantColorFlow()
    }

    /** Update flows for manga */
    private fun updateMangaFlow() {
        presenterScope.launch {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            _currentManga.value = dbManga
            _mangaState.update { getMangaStateCopyFromManga(_currentManga.value!!) }
        }
    }

    private fun getMangaStateCopyFromManga(m: Manga): MangaConstants.MangaScreenMangaState {
        return mangaState.value.copy(
            alternativeTitles = m.getAltTitles().toImmutableList(),
            artist = m.artist ?: "",
            author = m.author ?: "",
            currentDescription = getDescription(),
            currentTitle = m.title,
            externalLinks = m.getExternalLinks().toImmutableList(),
            genres = (m.getGenres(true) ?: emptyList()).toImmutableList(),
            initialized = m.initialized,
            inLibrary = m.favorite,
            isMerged = isMerged(m),
            isPornographic =
                m.getContentRating()
                    ?.equals(MdConstants.ContentRating.pornographic, ignoreCase = true) ?: false,
            langFlag = m.lang_flag,
            missingChapters = m.missing_chapters,
            originalTitle = m.originalTitle,
            stats =
                Stats(
                    rating = m.rating,
                    follows = m.users,
                    threadId = m.thread_id,
                    repliesCount = m.replies_count,
                ),
            status = m.status,
        )
    }

    private fun isMerged(manga: Manga): MergeConstants.IsMergedManga {
        val mergeMangaList = db.getMergeMangaList(manga).executeAsBlocking()
        return when (mergeMangaList.isNotEmpty()) {
            true -> {
                val mergeManga = mergeMangaList.first()
                val baseUrl =
                    when (val source = MergeType.getSource(mergeManga.mergeType, sourceManager)) {
                        is Komga -> source.hostUrl()
                        else -> source.baseUrl
                    }
                Yes(
                    url = baseUrl + mergeManga.url,
                    title = mergeManga.title,
                    mergeType = mergeManga.mergeType,
                )
            }
            false -> No
        }
    }

    /** Update filterflow */
    private fun updateFilterFlow() {
        presenterScope.launchIO {
            val filter = getFilter()
            val scanlatorFilter = getScanlatorFilter()
            val languageFilter = getLangaugeFilter()
            val hideTitle = getHideTitlesFilter()

            _generalState.update {
                it.copy(
                    chapterSortFilter = getSortFilter(),
                    chapterFilter = filter,
                    chapterFilterText = getFilterText(filter, scanlatorFilter),
                    chapterScanlatorFilter = scanlatorFilter,
                    chapterLanguageFilter = languageFilter,
                )
            }
        }
    }

    /** Toggle a manga as favorite */
    fun toggleFavorite(shouldAddToDefaultCategory: Boolean) {
        presenterScope.launch {
            val editManga = currentManga()
            editManga.apply {
                favorite = !favorite
                date_added =
                    when (favorite) {
                        true -> Date().time
                        false -> 0
                    }
            }

            _mangaState.update { it.copy(inLibrary = editManga.favorite) }

            db.insertManga(editManga).executeAsBlocking()
            updateMangaFlow()
            // add to the default category if it exists and the user has the option set
            if (shouldAddToDefaultCategory && generalState.value.hasDefaultCategory) {
                val defaultCategoryId = preferences.defaultCategory().get()
                generalState.value.allCategories
                    .firstOrNull { defaultCategoryId == it.id }
                    ?.let { updateMangaCategories(listOf(it)) }
            }
            if (editManga.favorite) {
                // this is called for the add as plan to read/auto sync tracking,
                updateTrackingFlows(true)
            }
        }
    }

    /** Delete the list of chapters */
    fun deleteChapters(
        chapterItems: List<ChapterItem>,
        isEverything: Boolean = false,
        canUndo: Boolean = false,
    ) {
        presenterScope.launchNonCancellable {
            if (chapterItems.isNotEmpty()) {
                val delete = {
                    if (isEverything) {
                        downloadManager.deleteManga(currentManga())
                    } else {
                        downloadManager.deleteChapters(
                            chapterItems.map { it.chapter.toDbChapter() },
                            currentManga(),
                        )
                    }
                }
                if (canUndo) {
                    _snackbarState.emit(
                        SnackbarState(
                            messageRes = R.string.deleted_downloads,
                            actionLabelRes = R.string.undo,
                            action = {},
                            dismissAction = { delete() },
                        )
                    )
                } else {
                    delete()
                }
            } else {
                _snackbarState.emit(SnackbarState(messageRes = R.string.no_chapters_to_delete))
            }
            updateChapterFlows()
        }
    }

    /** Checks if a manga is favorited, if not then snack action to add to library */
    private fun addToLibrarySnack() {
        if (!currentManga().favorite) {
            presenterScope.launch {
                _snackbarState.emit(
                    SnackbarState(
                        messageRes = R.string.add_to_library,
                        actionLabelRes = R.string.add,
                        action = { toggleFavorite(true) },
                    )
                )
            }
        }
    }

    /** Delete the list of chapters */
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
                    downloadManager.downloadChapters(
                        currentManga(),
                        generalState.value.activeChapters
                            .filter { !it.isDownloaded }
                            .map { it.chapter.toDbChapter() },
                    )
                }
                is DownloadAction.Download -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(
                        currentManga(),
                        chapterItems.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() },
                    )
                }
                is DownloadAction.DownloadNextUnread -> {
                    val filteredChapters =
                        generalState.value.activeChapters
                            .filter { !it.chapter.read && it.isNotDownloaded }
                            .sortedWith(chapterSort.sortComparator(currentManga(), true))
                            .take(downloadAction.numberToDownload)
                            .map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(currentManga(), filteredChapters)
                }
                is DownloadAction.DownloadUnread -> {
                    val filteredChapters =
                        generalState.value.activeChapters
                            .filter { !it.chapter.read && !it.isDownloaded }
                            .sortedWith(chapterSort.sortComparator(currentManga(), true))
                            .map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(currentManga(), filteredChapters)
                }
                is DownloadAction.Remove ->
                    deleteChapters(chapterItems, chapterItems.size == allChapterSize)
                is DownloadAction.RemoveAll ->
                    deleteChapters(
                        generalState.value.activeChapters,
                        generalState.value.activeChapters.size == allChapterSize,
                        true,
                    )
                is DownloadAction.RemoveRead -> {
                    val filteredChapters =
                        generalState.value.activeChapters.filter {
                            it.chapter.read && it.isDownloaded
                        }
                    deleteChapters(filteredChapters, filteredChapters.size == allChapterSize, true)
                }
                is DownloadAction.Cancel ->
                    deleteChapters(chapterItems, chapterItems.size == allChapterSize)
            }
        }
    }

    fun markChapters(
        chapterItems: List<ChapterItem>,
        markAction: ChapterMarkActions,
        skipSync: Boolean = false,
    ) {
        presenterScope.launchIO {
            val updatedChapterList =
                if (
                    markAction is ChapterMarkActions.PreviousRead ||
                        markAction is ChapterMarkActions.PreviousUnread
                ) {
                    when (currentManga().sortDescending(mangaDetailsPreferences)) {
                        true ->
                            (markAction as? ChapterMarkActions.PreviousRead)?.altChapters
                                ?: (markAction as ChapterMarkActions.PreviousUnread).altChapters
                        false -> chapterItems
                    }
                } else {
                    chapterItems
                }

            val nameRes =
                when (markAction) {
                    is ChapterMarkActions.Bookmark -> R.string.bookmarked
                    is ChapterMarkActions.UnBookmark -> R.string.removed_bookmark
                    is ChapterMarkActions.Read -> R.string.marked_as_read
                    is ChapterMarkActions.PreviousRead -> R.string.marked_as_read
                    is ChapterMarkActions.PreviousUnread -> R.string.marked_as_unread
                    is ChapterMarkActions.Unread -> R.string.marked_as_unread
                }

            chapterUseCases.markChapters(markAction, updatedChapterList)

            updateChapterFlows()

            fun finalizeChapters() {
                if (markAction is ChapterMarkActions.Read) {
                    if (preferences.removeAfterMarkedAsRead().get()) {
                        // dont delete bookmarked chapters
                        deleteChapters(
                            updatedChapterList
                                .filter { !it.chapter.bookmark }
                                .map { ChapterItem(chapter = it.chapter) },
                            updatedChapterList.size == generalState.value.allChapters.size,
                        )
                    }
                    // get the highest chapter number and update tracking for it
                    updatedChapterList
                        .maxByOrNull { it.chapter.chapterNumber.toInt() }
                        ?.let {
                            kotlin
                                .runCatching {
                                    updateTrackChapterMarkedAsRead(
                                        it.chapter.toDbChapter(),
                                        mangaId,
                                    ) {
                                        updateTrackingFlows()
                                    }
                                }
                                .onFailure {
                                    TimberKt.e(it) {
                                        "Failed to update track chapter marked as read"
                                    }
                                    presenterScope.launch {
                                        _snackbarState.emit(
                                            SnackbarState(
                                                "Error trying to update tracked chapter marked as read ${it.message}"
                                            )
                                        )
                                    }
                                }
                        }
                }

                // sync with dex if marked read or marked unread
                val syncRead =
                    when (markAction) {
                        is ChapterMarkActions.Read -> true
                        is ChapterMarkActions.Unread -> false
                        else -> null
                    }

                if (syncRead != null && !skipSync && preferences.readingSync().get()) {
                    val chapterIds =
                        updatedChapterList
                            .filter { !it.chapter.isMergedChapter() }
                            .map { it.chapter.mangaDexChapterId }
                    if (chapterIds.isNotEmpty()) {
                        presenterScope.launchNonCancellable {
                            statusHandler.marksChaptersStatus(
                                currentManga().uuid(),
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
                                val originalDbChapters =
                                    updatedChapterList.map { it.chapter }.map { it.toDbChapter() }
                                db.updateChaptersProgress(originalDbChapters).executeOnIO()
                                updateChapterFlows()
                            }
                        },
                        dismissAction = { finalizeChapters() },
                    )
                )
            } else {
                finalizeChapters()
            }
        }
    }

    /** clears the removedChapter flow */
    fun clearRemovedChapters() {
        _generalState.update { it.copy(removedChapters = persistentListOf()) }
    }

    /** Get Quick read text for the button */
    private fun updateNextUnreadChapter() {
        presenterScope.launchIO {
            val nextChapter =
                chapterSort
                    .getNextUnreadChapter(currentManga(), generalState.value.activeChapters)
                    ?.chapter
            _generalState.update {
                it.copy(
                    nextUnreadChapter =
                        when (nextChapter == null) {
                            true -> NextUnreadChapter()
                            false -> {
                                val id =
                                    when (nextChapter.lastPageRead > 0) {
                                        true -> R.string.continue_reading_
                                        false -> R.string.start_reading_
                                    }
                                val readTxt =
                                    if (
                                        nextChapter.isMergedChapter() ||
                                            (nextChapter.volume.isEmpty() &&
                                                nextChapter.chapterText.isEmpty())
                                    ) {
                                        nextChapter.name
                                    } else if (nextChapter.volume.isNotEmpty()) {
                                        "Vol. " + nextChapter.volume + " " + nextChapter.chapterText
                                    } else {
                                        nextChapter.chapterText
                                    }

                                NextUnreadChapter(id, readTxt, nextChapter)
                            }
                        }
                )
            }
        }
    }

    /** updates the missing chapter count on a manga if needed */
    private fun updateMissingChapters() {
        presenterScope.launchIO {
            val missingChapterHolder = generalState.value.allChapters.getMissingChapters()
            _mangaState.update {
                it.copy(estimatedMissingChapters = missingChapterHolder.estimatedChapters)
            }
            val currentMissingChapters = missingChapterHolder.count
            if (currentMissingChapters != currentManga().missing_chapters) {
                val editManga = currentManga()
                editManga.apply { this.missing_chapters = currentMissingChapters }
                db.insertManga(editManga)
                updateMangaFlow()
            }
        }
    }

    fun openComment(context: Context, chapterId: String) {
        presenterScope.launch {
            when (!isOnline()) {
                true ->
                    _snackbarState.emit(
                        SnackbarState(message = "No network connection, cannot open comments")
                    )
                false -> {
                    _isRefreshing.value = true
                    val threadId =
                        sourceManager.mangaDex
                            .getChapterCommentId(chapterId)
                            .onFailure { TimberKt.e { it.message() } }
                            .getOrElse { null }
                    _isRefreshing.value = false
                    if (threadId == null) {
                        _snackbarState.emit(
                            SnackbarState(messageRes = R.string.comments_unavailable)
                        )
                    } else {
                        context.openInWebView(MdConstants.forumUrl + threadId)
                    }
                }
            }
        }
    }

    fun blockScanlator(scanlator: String) {
        presenterScope.launchIO {
            val scanlatorImpl = db.getScanlatorByName(scanlator).executeAsBlocking()
            if (scanlatorImpl == null) {
                launchIO { mangaUpdateCoordinator.updateScanlator(scanlator) }
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
                            val allBlockedScanlators =
                                preferences.blockedScanlators().get().toMutableSet()
                            allBlockedScanlators.remove(scanlator)
                            preferences.blockedScanlators().set(allBlockedScanlators)
                            updateChapterFlows()
                        }
                    },
                )
            )
        }
    }

    // This is already filtered before reaching here, so directly update the chapters
    private fun onUpdateManga(mangaId: Long?) {
        updateChapterFlows()
    }

    fun copiedToClipboard(message: String) {
        presenterScope.launchIO {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                _snackbarState.emit(
                    SnackbarState(messageRes = R.string._copied_to_clipboard, message = message)
                )
            }
        }
    }

    /** Check if can access internet */
    private fun isOnline(): Boolean {
        return view?.activity?.isOnline() == true
    }

    /**
     * This method request updates after the activity resumed (usually after a return from the
     * reader)
     */
    override fun onResume() {
        presenterScope.launch {
            updateMangaFlow()
            updateChapterFlows()
            updateTrackingFlows()
            observeDownloads()
        }
    }

    private fun observeDownloads() {
        pausablePresenterScope.launchIO {
            downloadManager
                .statusFlow()
                .filter { it.mangaItem.id == currentManga().id }
                .catch { error -> TimberKt.e(error) }
                .collect { updateDownloadState(it) }
        }

        pausablePresenterScope.launchIO {
            downloadManager
                .progressFlow()
                .filter { it.mangaItem.id == currentManga().id }
                .catch { error -> TimberKt.e(error) }
                .collect { updateDownloadState(it) }
        }
    }

    // callback from Downloader
    private fun updateDownloadState(download: Download) {
        presenterScope.launchIO {
            val currentChapters = generalState.value.activeChapters
            val index = currentChapters.indexOfFirst { it.chapter.id == download.chapterItem.id }
            if (index >= 0) {
                val mutableChapters = currentChapters.toMutableList()
                val updateChapter =
                    currentChapters[index].copy(
                        downloadState = download.status,
                        downloadProgress = download.progress,
                    )
                mutableChapters[index] = updateChapter
                _generalState.update { it.copy(activeChapters = mutableChapters.toImmutableList()) }
            }
        }
    }

    fun getChapterUrl(chapter: SimpleChapter): String {
        return chapter.getHttpSource(sourceManager).getChapterUrl(chapter)
    }
}
