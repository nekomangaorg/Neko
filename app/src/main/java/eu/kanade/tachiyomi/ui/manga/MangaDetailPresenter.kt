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
import eu.kanade.tachiyomi.data.database.models.Chapter
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
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMergedChapterOfType
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.MergedServerSource
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DownloadAction
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.No
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.Yes
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingSuggestedDates
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsState
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.chapter.getChapterNum
import eu.kanade.tachiyomi.util.chapter.getVolumeNum
import eu.kanade.tachiyomi.util.chapter.updateTrackChapterMarkedAsRead
import eu.kanade.tachiyomi.util.getMissingChapters
import eu.kanade.tachiyomi.util.isAvailable
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
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
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
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

    private val _mangaDetailsState =
        MutableStateFlow(
            MangaDetailsState(
                currentArtwork = Artwork(mangaId = mangaId),
            ),
        )
    val mangaDetailsState: StateFlow<MangaDetailsState> = _mangaDetailsState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _snackbarState = MutableSharedFlow<SnackbarState>()
    val snackBarState: SharedFlow<SnackbarState> = _snackbarState.asSharedFlow()

    private val chapterSort = ChapterItemSort(chapterItemFilter, preferences)

    override fun onCreate() {
        super.onCreate()

        val mangaFlow = db.getManga(mangaId).asFlow().onEach {
            if (!it.initialized) {
                onRefresh()
            }
        }
        val chapterFlow = db.getChapters(mangaId).asFlow()
        val artworkFlow = db.getArtwork(mangaId).asFlow()
        val trackFlow = db.getTracks(mangaId).asFlow()
        val categoryFlow = db.getCategoriesForManga(mangaId).asFlow()
        val allCategoriesFlow = db.getCategories().asFlow()
        val mergeMangaFlow = db.getMergeMangaList(mangaId).asFlow()


        combine(
            mangaFlow,
            chapterFlow,
            artworkFlow,
            trackFlow,
            categoryFlow,
            allCategoriesFlow,
            mergeMangaFlow,
            LibraryUpdateJob.updateFlow.filter { it == mangaId },
            mangaDetailsPreferences.hideButtonText().asFlow(),
            mangaDetailsPreferences.extraLargeBackdrop().asFlow(),
            mangaDetailsPreferences.forcePortrait().asFlow(),
            mangaDetailsPreferences.autoThemeByCover().asFlow(),
            mangaDetailsPreferences.wrapAltTitles().asFlow(),
            mangaDexPreferences.coverQuality().asFlow(),
            mangaDexPreferences.blockedGroups().asFlow(),
            mangaDexPreferences.blockedUploaders().asFlow(),
        ) { manga, chapters, artwork, tracks, mangaCategories, allCategories, mergeManga, _, hideButtonText, extraLargeBackdrop, forcePortrait, themeByCover, wrapAltTitles, coverQuality, blockedGroups, blockedUploaders ->
            MangaDetailsState(
                manga = manga,
                chapters = chapters,
                artwork = artwork,
                tracks = tracks,
                mangaCategories = mangaCategories,
                allCategories = allCategories,
                mergeManga = mergeManga,
                hideButtonText = hideButtonText,
                extraLargeBackdrop = extraLargeBackdrop,
                forcePortrait = forcePortrait,
                themeByCover = themeByCover,
                wrapAltTitles = wrapAltTitles,
                coverQuality = coverQuality,
                blockedGroups = blockedGroups,
                blockedUploaders = blockedUploaders,
                vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId),
                sourceManager = sourceManager,
                downloadManager = downloadManager,
                chapterItemSort = chapterSort,
            )
        }
            .onEach {
                _mangaDetailsState.value = it
            }
            .launchIn(presenterScope)


        observeDownloads()
    }

    /** Refresh manga info, and chapters */
    fun onRefresh(isMerging: Boolean = false) {
        presenterScope.launchIO {
            if (!isOnline()) {
                _snackbarState.emit(SnackbarState(messageRes = R.string.no_network_connection))
                return@launchIO
            }

            _isRefreshing.value = true

            mangaUpdateCoordinator.update(mangaDetailsState.value.manga!!, presenterScope, isMerging).collect {
                result ->
                when (result) {
                    is MangaResult.Error -> {
                        _snackbarState.emit(
                            SnackbarState(message = result.text, messageRes = result.id)
                        )
                        _isRefreshing.value = false
                    }
                    is MangaResult.Success -> {
                        refreshTracking()
                        syncChaptersReadStatus()
                        _isRefreshing.value = false
                    }
                    is MangaResult.UpdatedChapters, is MangaResult.UpdatedManga, is MangaResult.UpdatedArtwork -> Unit
                    is MangaResult.ChaptersRemoved -> {
                        val removedChapters =
                            mangaDetailsState.value.chapters.filter {
                                it.chapter.id in result.chapterIdsRemoved && it.isDownloaded
                            }

                        if (removedChapters.isNotEmpty()) {
                            when (preferences.deleteRemovedChapters().get()) {
                                2 -> deleteChapters(removedChapters)
                                1 -> Unit
                                else -> {
                                    _mangaDetailsState.update {
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
                        mangaDetailsState.value.chapters.filter {
                            it.chapter.chapterTitle.contains(search!!, true) ||
                                it.chapter.scanlator.contains(search, true) ||
                                it.chapter.name.contains(search, true)
                        }
                    }
                    false -> emptyList()
                }

            _mangaDetailsState.update { it.copy(searchChapters = filteredChapters.toPersistentList()) }
        }
    }

    /** Updates the database with categories for the manga */
    fun updateMangaCategories(enabledCategories: List<CategoryItem>) {
        presenterScope.launchIO {
            val categories =
                enabledCategories.map { MangaCategory.create(mangaDetailsState.value.manga!!, it.toDbCategory()) }
            db.setMangaCategories(categories, listOf(mangaDetailsState.value.manga!!))
        }
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        presenterScope.launchIO {
            val category = Category.create(newCategory)
            db.insertCategory(category).executeAsBlocking()
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
    ) {
        when (trackingUpdate) {
            is TrackingUpdate.Error -> {
                TimberKt.e(trackingUpdate.exception) { "handle tracking update had error" }
                _snackbarState.emit(SnackbarState(message = trackingUpdate.message))
            }
            is TrackingUpdate.Success -> {

            }
        }
    }

    /** Figures out the suggested reading dates */
    private fun getSuggestedDate() {
        presenterScope.launchIO {
            val chapters = db.getHistoryByMangaId(mangaId).executeOnIO()

            _mangaDetailsState.update { state ->
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
            while (count < 5 && mangaDetailsState.value.tracks.isEmpty()) {
                delay(1000)
                count++
            }

            val asyncList =
                mangaDetailsState.value.tracks
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
        }
    }

    private fun syncChaptersReadStatus() {
        presenterScope.launchIO {
            if (
                !mangaDexPreferences.readingSync().get() || !loginHelper.isLoggedIn() || !isOnline()
            )
                return@launchIO

            runCatching {
                    statusHandler.getReadChapterIds(mangaDetailsState.value.manga!!.uuid()).collect { chapterIds ->
                        val chaptersToMarkRead =
                            mangaDetailsState.value.chapters
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
                mangaDetailsState.value.tracks.firstOrNull { service.id == it.trackServiceId } != null
            trackingCoordinator
                .searchTracker(title, service, mangaDetailsState.value.manga!!, previouslyTracked)
                .collect { result ->
                    _mangaDetailsState.update { it.copy(trackSearchResult = result) }
                }
        }
    }

    /** Register tracker with service */
    fun registerTracking(trackAndService: TrackAndService, skipTrackFlowUpdate: Boolean = false) {
        presenterScope.launchIO {
            val trackingUpdate = trackingCoordinator.registerTracking(trackAndService, mangaId)
            handleTrackingUpdate(trackingUpdate)
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
        val manga = mangaDetailsState.value.manga!!
        val cover =
            when (artwork.url.isBlank() || manga.thumbnail_url == artwork.url) {
                true ->
                    coverCache.getCustomCoverFile(manga).takeIf { it.exists() }
                        ?: coverCache.getCoverFile(
                            manga.thumbnail_url,
                            manga.favorite,
                        )
                false -> coverCache.getCoverFile(artwork.url)
            }

        val type = ImageUtil.findImageType(cover.inputStream()) ?: throw Exception("Not an image")

        // Build destination file.
        val fileNameNoExtension =
            listOfNotNull(
                    manga.title,
                    artwork.volume.ifEmpty { null },
                    manga.uuid(),
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
            val manga = mangaDetailsState.value.manga!!
            coverCache.setCustomCoverToCache(manga, artwork.url)
            MangaCoverMetadata.remove(mangaId)
            manga.user_cover = artwork.url
            db.insertManga(manga).executeOnIO()
        }
    }

    /** Reset cover */
    fun resetCover() {
        presenterScope.launchIO {
            val manga = mangaDetailsState.value.manga!!
            coverCache.deleteCustomCover(manga)
            MangaCoverMetadata.remove(mangaId)
            manga.user_cover = null
            db.insertManga(manga).executeOnIO()
        }
    }

    /** Set custom title or resets if given null */
    fun setAltTitle(title: String?) {
        presenterScope.launchIO {
            val manga = mangaDetailsState.value.manga!!
            val previousTitle = manga.title
            val newTitle = title ?: manga.originalTitle

            manga.user_title = title
            db.insertManga(manga).executeOnIO()


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
            val manga = mangaDetailsState.value.manga!!
            db.deleteMergeMangaForType(mangaId, mergeType).executeAsBlocking()
            val (mergedChapters, nonMergedChapters) =
                db.getChapters(manga).executeOnIO().partition {
                    it.isMergedChapterOfType(mergeType)
                }
            if (!libraryPreferences.enableLocalChapters().get()) {
                downloadManager.deleteChapters(mergedChapters, manga)
            }
            db.deleteChapters(mergedChapters).executeOnIO()
        }
    }

    fun searchMergedManga(query: String, mergeType: MergeType) {
        presenterScope.launchIO {
            _mangaDetailsState.update { it.copy(mergeSearchResult = MergeSearchResult.Loading) }

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

                    _mangaDetailsState.update {
                        when (mergedMangaResults.isEmpty()) {
                            true ->
                                mangaDetailsState.value.copy(
                                    mergeSearchResult = MergeSearchResult.NoResult
                                )
                            false ->
                                mangaDetailsState.value.copy(
                                    mergeSearchResult =
                                        MergeSearchResult.Success(mergedMangaResults)
                                )
                        }
                    }
                }
                .getOrElse { error ->
                    TimberKt.e(error) { "Error searching merged manga" }
                    _mangaDetailsState.update {
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
            onRefresh(true)
        }
    }

    /** Toggle a manga as favorite */
    fun toggleFavorite(shouldAddToDefaultCategory: Boolean) {
        presenterScope.launch {
            val manga = mangaDetailsState.value.manga!!
            manga.apply {
                favorite = !favorite
                date_added =
                    when (favorite) {
                        true -> Date().time
                        false -> 0
                    }
            }

            db.insertManga(manga).executeAsBlocking()

            // add to the default category if it exists and the user has the option set
            if (shouldAddToDefaultCategory && mangaDetailsState.value.hasDefaultCategory) {
                val defaultCategoryId = libraryPreferences.defaultCategory().get()
                mangaDetailsState.value.allCategories
                    .firstOrNull { defaultCategoryId == it.id }
                    ?.let { updateMangaCategories(listOf(it)) }
            }
            if (manga.favorite) {
                // this is called for the add as plan to read/auto sync tracking,
                // updateTrackingFlows(true)
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
            val manga = mangaDetailsState.value.manga!!
            if (chapterItems.isNotEmpty()) {
                val delete = {
                    if (isEverything) {
                        downloadManager.deleteManga(manga)
                    } else {
                        downloadManager.deleteChapters(
                            chapterItems.map { it.chapter.toDbChapter() },
                            manga,
                        )
                        val localDbChapters =
                            chapterItems
                                .filter { it.chapter.isLocalSource() }
                                .map { it.chapter.toDbChapter() }
                        if (localDbChapters.isNotEmpty()) {
                            db.deleteChapters(localDbChapters).executeAsBlocking()
                        }
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
        }
    }

    /** Checks if a manga is favorited, if not then snack action to add to library */
    private fun addToLibrarySnack() {
        if (!mangaDetailsState.value.inLibrary) {
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
            val manga = mangaDetailsState.value.manga!!
            val allChapterSize = mangaDetailsState.value.chapters.size
            when (downloadAction) {
                is DownloadAction.ImmediateDownload -> {
                    addToLibrarySnack()
                    downloadManager.startDownloadNow(chapterItems.first().chapter.toDbChapter())
                }
                is DownloadAction.DownloadAll -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(
                        manga,
                        mangaDetailsState.value.chapters
                            .filter { !it.isDownloaded }
                            .map { it.chapter.toDbChapter() },
                    )
                }
                is DownloadAction.Download -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(
                        manga,
                        chapterItems.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() },
                    )
                }
                is DownloadAction.DownloadNextUnread -> {
                    val filteredChapters =
                        mangaDetailsState.value.chapters
                            .filter { !it.chapter.read && it.isNotDownloaded }
                            .sortedWith(chapterSort.sortComparator(manga, true))
                            .take(downloadAction.numberToDownload)
                            .map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(manga, filteredChapters)
                }
                is DownloadAction.DownloadUnread -> {
                    val filteredChapters =
                        mangaDetailsState.value.chapters
                            .filter { !it.chapter.read && !it.isDownloaded }
                            .sortedWith(chapterSort.sortComparator(manga, true))
                            .map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(manga, filteredChapters)
                }
                is DownloadAction.Remove ->
                    deleteChapters(chapterItems, chapterItems.size == allChapterSize)
                is DownloadAction.RemoveAll ->
                    deleteChapters(
                        mangaDetailsState.value.chapters,
                        mangaDetailsState.value.chapters.size == allChapterSize,
                        true,
                    )
                is DownloadAction.RemoveRead -> {
                    val filteredChapters =
                        mangaDetailsState.value.chapters.filter {
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
            val manga = mangaDetailsState.value.manga!!
            val updatedChapterList =
                if (
                    markAction is ChapterMarkActions.PreviousRead ||
                        markAction is ChapterMarkActions.PreviousUnread
                ) {
                    when (manga.sortDescending(mangaDetailsPreferences)) {
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


            suspend fun finalizeChapters() {
                if (markAction is ChapterMarkActions.Read) {
                    if (preferences.removeAfterMarkedAsRead().get()) {
                        // dont delete bookmarked chapters
                        deleteChapters(
                            updatedChapterList
                                .filter { it.chapter.canDeleteChapter() }
                                .map { ChapterItem(chapter = it.chapter) },
                            updatedChapterList.size == mangaDetailsState.value.chapters.size,
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
                                    )
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

                chapterUseCases.markChaptersRemote(
                    markAction,
                    manga.uuid(),
                    updatedChapterList,
                    skipSync,
                )
            }

            if (markAction.canUndo) {
                _snackbarState.emit(
                    SnackbarState(
                        messageRes = nameRes,
                        actionLabelRes = R.string.undo,
                        action = {
                            presenterScope.launchIO {
                                val originalDbChapters =
                                    updatedChapterList.map { it.chapter }.map { it.toDbChapter() }
                                db.updateChaptersProgress(originalDbChapters).executeOnIO()
                            }
                        },
                        dismissAction = { presenterScope.launchIO { finalizeChapters() } },
                    )
                )
            } else {
                finalizeChapters()
            }
        }
    }

    /** clears the removedChapter flow */
    fun clearRemovedChapters() {
        _mangaDetailsState.update { it.copy(removedChapters = persistentListOf()) }
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

    fun blockScanlator(blockType: BlockType, blocked: String) {
        presenterScope.launchIO {
            when (blockType) {
                BlockType.Group -> {
                    val scanlatorGroupImpl = db.getScanlatorGroupByName(blocked).executeAsBlocking()
                    if (scanlatorGroupImpl == null) {
                        launchIO { mangaUpdateCoordinator.updateGroup(blocked) }
                    }
                    val blockedGroups = mangaDexPreferences.blockedGroups().get().toMutableSet()
                    blockedGroups.add(blocked)
                    mangaDexPreferences.blockedGroups().set(blockedGroups)
                }
                BlockType.Uploader -> {
                    val uploaderImpl = db.getUploaderByName(blocked).executeAsBlocking()
                    if (uploaderImpl == null) {
                        launchIO { mangaUpdateCoordinator.updateUploader(blocked) }
                    }
                    val blockedUploaders =
                        mangaDexPreferences.blockedUploaders().get().toMutableSet()
                    blockedUploaders.add(blocked)
                    mangaDexPreferences.blockedUploaders().set(blockedUploaders)
                }
            }
            _snackbarState.emit(
                SnackbarState(
                    messageRes = R.string.globally_blocked_group_,
                    message = blocked,
                    actionLabelRes = R.string.undo,
                    action = {
                        presenterScope.launch {
                            when (blockType) {
                                BlockType.Group -> {
                                    db.deleteScanlatorGroup(blocked).executeOnIO()
                                    val allBlockedGroups =
                                        mangaDexPreferences.blockedGroups().get().toMutableSet()
                                    allBlockedGroups.remove(blocked)
                                    mangaDexPreferences.blockedGroups().set(allBlockedGroups)
                                }

                                BlockType.Uploader -> {
                                    db.deleteUploader(blocked).executeOnIO()
                                    val allBlockedUploaders =
                                        mangaDexPreferences.blockedUploaders().get().toMutableSet()
                                    allBlockedUploaders.remove(blocked)
                                    mangaDexPreferences.blockedUploaders().set(allBlockedUploaders)
                                }
                            }
                        }
                    },
                )
            )
        }
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
        super.onResume()
        observeDownloads()
    }

    private fun observeDownloads() {
        pausablePresenterScope.launchIO {
            downloadManager
                .statusFlow()
                .filter { it.manga.id == mangaId }
                .catch { error -> TimberKt.e(error) }
                .collect { updateDownloadState(it) }
        }

        pausablePresenterScope.launchIO {
            downloadManager
                .progressFlow()
                .filter { it.manga.id == mangaId }
                .catch { error -> TimberKt.e(error) }
                .collect { updateDownloadState(it) }
        }
    }

    // callback from Downloader
    private fun updateDownloadState(download: Download) {
        presenterScope.launchIO {
            val currentChapters = mangaDetailsState.value.chapters
            val index = currentChapters.indexOfFirst { it.chapter.id == download.chapter.id }
            if (index >= 0) {
                val mutableChapters = currentChapters.toMutableList()
                val updateChapter =
                    currentChapters[index].copy(
                        downloadState = download.status,
                        downloadProgress = download.progress,
                    )
                mutableChapters[index] = updateChapter
                _mangaDetailsState.update { it.copy(chapters = mutableChapters.toImmutableList()) }
            }
        }
    }

    fun getChapterUrl(chapter: SimpleChapter): String {
        return chapter.getHttpSource(sourceManager).getChapterUrl(chapter)
    }
}
