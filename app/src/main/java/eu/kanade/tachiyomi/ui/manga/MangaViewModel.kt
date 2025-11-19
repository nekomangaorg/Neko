package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
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
import eu.kanade.tachiyomi.ui.main.AppSnackbarManager
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DownloadAction
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortOption
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.No
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.Yes
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackDateChange
import eu.kanade.tachiyomi.util.MissingChapterHolder
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
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.openInWebView
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Date
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.security.SecurityPreferences
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
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.manga.getDescription
import org.nekomanga.domain.manga.toManga
import org.nekomanga.domain.manga.toMangaItem
import org.nekomanga.domain.manga.uuid
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.snackbar.SnackbarColor
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.domain.track.toTrackItem
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.UiText
import org.nekomanga.usecases.chapters.ChapterUseCases
import org.nekomanga.usecases.manga.MangaUseCases
import org.nekomanga.util.system.mapAsync
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaViewModel(val mangaId: Long) : ViewModel() {

    class Factory(private val mangaId: Long) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MangaViewModel(mangaId) as T
        }
    }

    val preferences: PreferencesHelper = Injekt.get()
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get()
    val libraryPreferences: LibraryPreferences = Injekt.get()
    val securityPreferences: SecurityPreferences = Injekt.get()
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    val coverCache: CoverCache = Injekt.get()
    val db: DatabaseHelper = Injekt.get()
    val downloadManager: DownloadManager = Injekt.get()

    val appSnackbarManager: AppSnackbarManager = Injekt.get()
    val chapterItemFilter: ChapterItemFilter = Injekt.get()
    val sourceManager: SourceManager = Injekt.get()
    private val loginHelper: MangaDexLoginHelper = Injekt.get()
    private val statusHandler: StatusHandler = Injekt.get()
    private val trackManager: TrackManager = Injekt.get()
    private val mangaUpdateCoordinator: MangaUpdateCoordinator = Injekt.get()
    private val trackingCoordinator: TrackingCoordinator = Injekt.get()
    private val storageManager: StorageManager = Injekt.get()
    private val chapterUseCases: ChapterUseCases = Injekt.get()

    private val mangaUseCases: MangaUseCases = Injekt.get()

    private val _mangaDetailScreenState =
        MutableStateFlow(
            MangaConstants.MangaDetailScreenState(currentArtwork = createInitialCurrentArtwork())
        )

    val mangaDetailScreenState: StateFlow<MangaConstants.MangaDetailScreenState> =
        _mangaDetailScreenState.asStateFlow()

    private val chapterSort = ChapterItemSort(chapterItemFilter, preferences)

    fun <T1, T2, T3, T4, T5, T6, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
    ): Flow<R> =
        combine(combine(flow, flow2, flow3, ::Triple), combine(flow4, flow5, flow6, ::Triple)) {
            t1,
            t2 ->
            transform(t1.first, t1.second, t1.third, t2.first, t2.second, t2.third)
        }

    val mangaFlow =
        db.getManga(mangaId)
            .asRxObservable()
            .asFlow()
            .map { it.toMangaItem() }
            .distinctUntilChanged()
    val allCategoriesFlow =
        db.getCategories()
            .asFlow()
            .map { categories -> categories.map { it.toCategoryItem() } }
            .distinctUntilChanged()

    val mangaCategoriesFlow =
        combine(db.getCategories().asFlow(), db.getMangaCategory(mangaId).asFlow()) {
                allCategories,
                mangaCategories ->
                val mangaCategorySet = mangaCategories.map { it.category_id }.toSet()
                allCategories
                    .filter { it.id != null && it.id in mangaCategorySet }
                    .map { it.toCategoryItem() }
            }
            .distinctUntilChanged()

    val tracksFlow =
        db.getTracks(mangaId)
            .asFlow()
            .map { tracks -> tracks.map { it.toTrackItem() }.toPersistentList() }
            .distinctUntilChanged()

    val mergedFlow =
        db.getMergeMangaList(mangaId)
            .asFlow()
            .map { mergeMangaList ->
                when (mergeMangaList.isNotEmpty()) {
                    true -> {
                        val mergeManga = mergeMangaList.first()
                        val source = MergeType.getSource(mergeManga.mergeType, sourceManager)
                        val url =
                            when (source) {
                                is MergedServerSource -> source.getMangaUrl(mergeManga.url)
                                else -> source.baseUrl + mergeManga.url
                            }
                        Yes(url, title = mergeManga.title, mergeType = mergeManga.mergeType)
                    }
                    false -> No
                }
            }
            .distinctUntilChanged()

    val allChapterFlow =
        combine(
                db.getChapters(mangaId).asFlow(),
                mangaDexPreferences.blockedGroups().changes(),
                mangaDexPreferences.blockedUploaders().changes(),
            ) { dbChapters, blockedGroups, blockedUploaders ->
                val dbManga = db.getManga(mangaId).executeAsBlocking()!!
                dbChapters
                    .mapNotNull { it.toSimpleChapter() }
                    .filter { chapter ->
                        val scanlators = chapter.scanlatorList()
                        scanlators.none { scanlator -> blockedGroups.contains(scanlator) } &&
                            (Constants.NO_GROUP !in scanlators ||
                                chapter.uploader !in blockedUploaders)
                    }
                    .map { chapter ->
                        val downloadState =
                            when {
                                downloadManager.isChapterDownloaded(
                                    chapter.toDbChapter(),
                                    dbManga,
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
            }
            .distinctUntilChanged()

    init {
        viewModelScope.launchIO {
            if (!db.getManga(mangaId).executeAsBlocking()!!.initialized) {
                onRefresh()
            }
        }

        initialLoad()
        observeDownloads()

        viewModelScope.launchIO {
            combine(
                    mangaFlow,
                    allChapterFlow,
                    allCategoriesFlow,
                    mangaCategoriesFlow,
                    tracksFlow,
                    mergedFlow,
                ) { mangaItem, allChapters, allCategories, mangaCategories, tracks, IsMerged ->
                    val artwork = createCurrentArtwork(mangaItem)

                    if (!mangaItem.initialized) {
                        AllInfo(mangaItem = MangaItem(title = mangaItem.title), artwork = artwork)
                    } else {
                        val alternativeArtwork =
                            createAltArtwork(
                                mangaItem,
                                artwork,
                                db.getArtwork(mangaId).executeAsBlocking(),
                            )

                        val chapterInfo =
                            createAllChapterInfo(mangaItem, allChapters.toPersistentList())

                        val mangaStatusCompleted =
                            isMangaStatusCompleted(
                                mangaItem,
                                chapterInfo.missingChapters.count,
                                chapterInfo.allChapters,
                            )

                        val loggedInTrackerService =
                            trackManager.services
                                .filter { it.value.isLogged() }
                                .map { it.value.toTrackServiceItem() }
                                .toPersistentList()

                        val trackCount =
                            loggedInTrackerService.count { service ->
                                tracks.any { track ->
                                    service.id == track.trackServiceId &&
                                        (!service.isMdList ||
                                            !FollowStatus.isUnfollowed(track.status))
                                }
                            }

                        val displayFilter = getChapterDisplay(mangaItem)
                        val sortFilter = getSortFilter(mangaItem)
                        val sourceFilter =
                            getChapterScanlatorFilter(mangaItem, chapterInfo.allSources)
                        val scanlatorFilter =
                            getChapterScanlatorFilter(
                                mangaItem,
                                (chapterInfo.allScanlators + chapterInfo.allUploaders)
                                    .toPersistentSet(),
                            )
                        val languageFilter =
                            getChapterLanguageFilter(mangaItem, chapterInfo.allLanguages)
                        val chapterFilterText =
                            getFilterText(
                                displayFilter,
                                sourceFilter,
                                scanlatorFilter,
                                languageFilter,
                            )

                        AllInfo(
                            mangaItem = mangaItem,
                            isMerged = IsMerged,
                            mangaStatusCompleted = mangaStatusCompleted,
                            chapterDisplay = displayFilter,
                            chapterSourceFilter = sourceFilter,
                            chapterScanlatorFilter = scanlatorFilter,
                            chapterLanguageFilter = languageFilter,
                            chapterSortFilter = sortFilter,
                            chapterFilterText = chapterFilterText,
                            allCategories = allCategories.toPersistentList(),
                            mangaCategories = mangaCategories.toPersistentList(),
                            allChapterInfo = chapterInfo,
                            artwork = artwork,
                            altArtwork = alternativeArtwork,
                            tracks = tracks,
                            loggedInTrackerService = loggedInTrackerService,
                            trackServiceCount = trackCount,
                        )
                    }
                }
                .distinctUntilChanged()
                .collectLatest { allInfo ->
                    _mangaDetailScreenState.update {
                        it.copy(
                            vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId),
                            alternativeTitles = allInfo.mangaItem.altTitles,
                            artist = allInfo.mangaItem.artist,
                            author = allInfo.mangaItem.author,
                            alternativeArtwork = allInfo.altArtwork,
                            currentArtwork = allInfo.artwork,
                            currentDescription = allInfo.mangaItem.getDescription(),
                            currentTitle =
                                allInfo.mangaItem.userTitle.ifEmpty { allInfo.mangaItem.title },
                            externalLinks = allInfo.mangaItem.externalLinks,
                            genres = allInfo.mangaItem.genre,
                            initialized = allInfo.mangaItem.initialized,
                            inLibrary = allInfo.mangaItem.favorite,
                            isMerged = allInfo.isMerged,
                            isPornographic =
                                allInfo.mangaItem.contentRating.equals(
                                    MdConstants.ContentRating.pornographic,
                                    ignoreCase = true,
                                ),
                            langFlag = allInfo.mangaItem.langFlag,
                            missingChapters = allInfo.mangaItem.missingChapters,
                            originalTitle = allInfo.mangaItem.title,
                            stats =
                                Stats(
                                    rating = allInfo.mangaItem.rating,
                                    follows = allInfo.mangaItem.users,
                                    threadId = allInfo.mangaItem.threadId,
                                    repliesCount = allInfo.mangaItem.repliesCount,
                                ),
                            status = allInfo.mangaItem.status,
                            lastVolume = allInfo.mangaItem.lastVolumeNumber,
                            lastChapter = allInfo.mangaItem.lastChapterNumber,
                            isRefreshing = allInfo.isRefreshing,
                            activeChapters = allInfo.allChapterInfo.activeChapters,
                            nextUnreadChapter = allInfo.allChapterInfo.nextUnread,
                            estimatedMissingChapters =
                                allInfo.allChapterInfo.missingChapters.estimatedChapters,
                            chapterFilter = allInfo.chapterDisplay,
                            chapterFilterText = allInfo.chapterFilterText,
                            chapterSortFilter = allInfo.chapterSortFilter,
                            chapterScanlatorFilter = allInfo.chapterScanlatorFilter,
                            chapterSourceFilter = allInfo.chapterSourceFilter,
                            chapterLanguageFilter = allInfo.chapterLanguageFilter,
                            allChapters = allInfo.allChapterInfo.allChapters,
                            allSources = allInfo.allChapterInfo.allSources,
                            allCategories = allInfo.allCategories,
                            currentCategories = allInfo.mangaCategories,
                            tracks = allInfo.tracks,
                            loggedInTrackService = allInfo.loggedInTrackerService,
                            trackServiceCount = allInfo.trackServiceCount,
                        )
                    }

                    if (_mangaDetailScreenState.value.firstLoad) {
                        syncChaptersReadStatus()
                        autoAddTrackers(
                            allInfo.mangaItem,
                            allInfo.loggedInTrackerService,
                            allInfo.tracks,
                        )
                        _mangaDetailScreenState.update { it.copy(firstLoad = false) }
                    }

                    if (allInfo.mangaItem.initialized) {
                        launchIO {
                            mangaUseCases.updateMangaStatusAndMissingCount(
                                allInfo.mangaItem.toManga()
                            )
                        }
                    }
                }
        }
    }

    fun onRefresh(isMerging: Boolean = false) {
        TimberKt.d { "On Refresh called" }
        viewModelScope.launchIO {
            if (!isOnline()) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string.no_network_connection,
                        snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                    )
                )
                return@launchIO
            }

            _mangaDetailScreenState.update { it.copy(isRefreshing = true) }

            val mangaItem = db.getManga(mangaId).executeAsBlocking()!!.toMangaItem()

            mangaUpdateCoordinator
                .update(mangaItem = mangaItem, isMerging = isMerging)
                .onCompletion { _mangaDetailScreenState.update { it.copy(isRefreshing = false) } }
                .catch { e ->
                    e.message?.let {
                        appSnackbarManager.showSnackbar(
                            SnackbarState(
                                message = it,
                                snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                            )
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        is MangaResult.Error -> {
                            appSnackbarManager.showSnackbar(
                                SnackbarState(
                                    message = result.text,
                                    messageRes = result.id,
                                    snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                                )
                            )
                        }

                        is MangaResult.Success -> {
                            syncChaptersReadStatus()
                            val mangaItem = db.getManga(mangaId).executeAsBlocking()!!.toMangaItem()

                            autoAddTrackers(
                                mangaItem,
                                mangaDetailScreenState.value.loggedInTrackService,
                                mangaDetailScreenState.value.tracks,
                            )
                        }

                        is MangaResult.ChaptersRemoved -> {
                            val removedChapters =
                                mangaDetailScreenState.value.allChapters.filter {
                                    it.chapter.id in result.chapterIdsRemoved && it.isDownloaded
                                }

                            if (removedChapters.isNotEmpty()) {
                                when (preferences.deleteRemovedChapters().get()) {
                                    2 -> deleteChapters(removedChapters)
                                    1 -> Unit
                                    else -> {
                                        _mangaDetailScreenState.update {
                                            it.copy(
                                                removedChapters = removedChapters.toPersistentList()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> Unit
                    }
                }
        }
    }

    /** Delete the list of chapters */
    fun deleteChapters(
        chapterItems: List<ChapterItem>,
        isEverything: Boolean = false,
        canUndo: Boolean = false,
    ) {
        viewModelScope.launchNonCancellable {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            if (chapterItems.isNotEmpty()) {
                val delete = {
                    if (isEverything) {
                        downloadManager.deleteManga(dbManga)
                    } else {
                        downloadManager.deleteChapters(
                            dbManga,
                            chapterItems.map { it.chapter.toDbChapter() },
                        )
                        val localDbChapters =
                            chapterItems
                                .filter { it.chapter.isLocalSource() }
                                .map { it.chapter.toDbChapter() }
                        if (localDbChapters.isNotEmpty()) {
                            db.deleteChapters(localDbChapters).executeAsBlocking()
                        }
                    }
                    updateRemovedDownload(chapterItems)
                }
                if (canUndo) {
                    appSnackbarManager.showSnackbar(
                        SnackbarState(
                            messageRes = R.string.deleted_downloads,
                            actionLabelRes = R.string.undo,
                            action = {},
                            dismissAction = { delete() },
                            snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                        )
                    )
                } else {
                    delete()
                }
            } else {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string.no_chapters_to_delete,
                        snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                    )
                )
            }
        }
    }

    fun createMangaFolder() {
        viewModelScope.launchIO { downloadManager.createMangaFolder(getManga()) }
    }

    fun markChapters(
        chapterItems: List<ChapterItem>,
        markAction: ChapterMarkActions,
        skipSync: Boolean = false,
    ) {
        viewModelScope.launchIO {
            val manga = db.getManga(mangaId).executeAsBlocking()!!
            val updatedChapterList =
                if (
                    markAction is ChapterMarkActions.PreviousRead ||
                        markAction is ChapterMarkActions.PreviousUnread
                ) {
                    when (manga.sortDescending(mangaDetailsPreferences)) {
                        true -> chapterItems
                        false ->
                            (markAction as? ChapterMarkActions.PreviousRead)?.altChapters
                                ?: (markAction as ChapterMarkActions.PreviousUnread).altChapters
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
                            updatedChapterList.size == mangaDetailScreenState.value.allChapters.size,
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
                                    viewModelScope.launchIO {
                                        appSnackbarManager.showSnackbar(
                                            SnackbarState(
                                                "Error trying to update tracked chapter marked as read ${it.message}",
                                                snackBarColor =
                                                    _mangaDetailScreenState.value.snackbarColor,
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
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = nameRes,
                        actionLabelRes = R.string.undo,
                        action = {
                            viewModelScope.launchIO {
                                val originalDbChapters =
                                    updatedChapterList.map { it.chapter }.map { it.toDbChapter() }
                                db.updateChaptersProgress(originalDbChapters).executeOnIO()
                            }
                        },
                        dismissAction = { viewModelScope.launchIO { finalizeChapters() } },
                        snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                    )
                )
            } else {
                finalizeChapters()
            }
        }
    }

    /** Delete the list of chapters */
    fun downloadChapters(chapterItems: List<ChapterItem>, downloadAction: DownloadAction) {
        viewModelScope.launchIO {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            val allChapterSize = mangaDetailScreenState.value.allChapters.size
            when (downloadAction) {
                is DownloadAction.ImmediateDownload -> {
                    addToLibrarySnack()
                    downloadManager.startDownloadNow(chapterItems.first().chapter.toDbChapter())
                }
                is DownloadAction.DownloadAll -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(
                        dbManga,
                        mangaDetailScreenState.value.activeChapters
                            .filter { !it.isDownloaded }
                            .map { it.chapter.toDbChapter() },
                    )
                }
                is DownloadAction.Download -> {
                    addToLibrarySnack()
                    downloadManager.downloadChapters(
                        dbManga,
                        chapterItems.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() },
                    )
                }
                is DownloadAction.DownloadNextUnread -> {
                    val filteredChapters =
                        mangaDetailScreenState.value.activeChapters
                            .filter {
                                !it.chapter.read && it.isNotDownloaded && !it.chapter.isUnavailable
                            }
                            .sortedWith(chapterSort.sortComparator(dbManga, true))
                            .take(downloadAction.numberToDownload)
                            .map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(dbManga, filteredChapters)
                }
                is DownloadAction.DownloadUnread -> {
                    val filteredChapters =
                        mangaDetailScreenState.value.activeChapters
                            .filter {
                                !it.chapter.read && !it.isDownloaded && !it.chapter.isUnavailable
                            }
                            .map { it.chapter.toDbChapter() }
                    downloadManager.downloadChapters(dbManga, filteredChapters)
                }
                is DownloadAction.Remove ->
                    deleteChapters(chapterItems, chapterItems.size == allChapterSize)
                is DownloadAction.RemoveAll ->
                    deleteChapters(
                        mangaDetailScreenState.value.activeChapters,
                        mangaDetailScreenState.value.activeChapters.size == allChapterSize,
                        true,
                    )
                is DownloadAction.RemoveRead -> {
                    val filteredChapters =
                        mangaDetailScreenState.value.activeChapters.filter {
                            it.chapter.read && it.isDownloaded
                        }
                    deleteChapters(filteredChapters, filteredChapters.size == allChapterSize, true)
                }
                is DownloadAction.Cancel ->
                    deleteChapters(chapterItems, chapterItems.size == allChapterSize)
            }
        }
    }

    /** Checks if a manga is favorited, if not then snack action to add to library */
    private fun addToLibrarySnack() {
        viewModelScope.launchIO {
            if (!_mangaDetailScreenState.value.inLibrary) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string.add_to_library,
                        actionLabelRes = R.string.add,
                        action = { toggleFavorite(true) },
                        snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                    )
                )
            }
        }
    }

    suspend fun isOnline(): Boolean {
        val networkState =
            downloadManager.networkStateFlow().map { it }.distinctUntilChanged().firstOrNull()
        networkState ?: return false

        return networkState.isOnline
    }

    fun initialLoad() {
        // refresh tracking
        viewModelScope.launchIO {
            val networkState =
                downloadManager.networkStateFlow().map { it }.distinctUntilChanged().firstOrNull()
            networkState ?: return@launchIO
            if (isOnline()) {
                val tracks = db.getTracks(mangaId).executeOnIO()

                tracks
                    .map { it.toTrackItem() }
                    .mapNotNull { track ->
                        trackManager
                            .getService(track.trackServiceId)
                            ?.takeIf { it.isLogged() }
                            ?.let { service -> track to service }
                    }
                    .mapAsync { (trackItem, service) ->
                        kotlin
                            .runCatching { service.refresh(trackItem.toDbTrack()) }
                            .onFailure {
                                if (it !is CancellationException) {
                                    TimberKt.e(it) {
                                        "Error refreshing tracker: ${service.nameRes()}"
                                    }
                                    delay(3000)
                                    appSnackbarManager.showSnackbar(
                                        SnackbarState(
                                            message = it.message,
                                            fieldRes = service.nameRes(),
                                            messageRes = R.string.error_refreshing_,
                                            snackBarColor =
                                                _mangaDetailScreenState.value.snackbarColor,
                                        )
                                    )
                                }
                            }
                            .onSuccess { updatedTrack ->
                                db.insertTrack(updatedTrack).executeOnIO()
                            }
                    }
            }
        }

        viewModelScope.launchIO {
            val validMergeTypes =
                MergeType.entries
                    .filter { mergeType ->
                        when (mergeType) {
                            MergeType.Invalid,
                            MergeType.Kagane -> false
                            // Conditionally keep these types if they are configured
                            MergeType.Komga -> sourceManager.komga.isConfigured()
                            MergeType.Suwayomi -> sourceManager.suwayomi.isConfigured()
                            // Keep all other types
                            else -> true
                        }
                    }
                    .toPersistentList()

            val loggedInServices =
                trackManager.services
                    .filter { service -> service.value.isLogged() }
                    .map { service -> service.value.toTrackServiceItem() }
                    .toPersistentList()

            _mangaDetailScreenState.update {
                it.copy(
                    incognitoMode = securityPreferences.incognitoMode().get(),
                    hasDefaultCategory = libraryPreferences.defaultCategory().get() != -1,
                    hideButtonText = mangaDetailsPreferences.hideButtonText().get(),
                    backdropSize = mangaDetailsPreferences.backdropSize().get(),
                    forcePortrait = mangaDetailsPreferences.forcePortrait().get(),
                    themeBasedOffCovers = mangaDetailsPreferences.autoThemeByCover().get(),
                    wrapAltTitles = mangaDetailsPreferences.wrapAltTitles().get(),
                    validMergeTypes = validMergeTypes,
                    loggedInTrackService = loggedInServices,
                )
            }
        }
    }

    private fun createAllChapterInfo(
        mangaItem: MangaItem,
        allChapters: PersistentList<ChapterItem>,
    ): AllChapterInfo {

        val missingChapterHolder = allChapters.getMissingChapters()

        val allSources = mutableSetOf(MdConstants.name)

        val allChapterScanlators =
            allChapters.flatMap { ChapterUtil.getScanlators(it.chapter.scanlator) }.toMutableSet()
        val allChapterUploaders =
            allChapters
                .mapNotNull {
                    if (it.chapter.uploader.isEmpty()) return@mapNotNull null
                    if (it.chapter.scanlator != Constants.NO_GROUP) return@mapNotNull null
                    it.chapter.uploader
                }
                .toPersistentSet()

        if (allChapterScanlators.size == 1 && !mangaItem.filteredScanlators.isEmpty()) {
            val manga = mangaItem.copy(filteredScanlators = persistentListOf()).toManga()
            db.insertManga(manga).executeAsBlocking()
        }

        SourceManager.mergeSourceNames.forEach { name ->
            val removed = allChapterScanlators.remove(name)
            if (removed) {
                allSources.add(name)
            }
        }

        val allLanguages =
            allChapters.flatMap { ChapterUtil.getLanguages(it.chapter.language) }.toPersistentSet()

        val activeChapters =
            chapterSort.getChaptersSorted(mangaItem.toManga(), allChapters).toPersistentList()

        val nextUnread = getNextUnread(mangaItem, activeChapters)

        return AllChapterInfo(
            nextUnread = nextUnread,
            activeChapters = activeChapters,
            allChapters = allChapters,
            missingChapters = missingChapterHolder,
            allScanlators = allChapterScanlators.toPersistentSet(),
            allUploaders = allChapterUploaders,
            allSources = allSources.toPersistentSet(),
            allLanguages = allLanguages,
        )
    }

    private fun createInitialCurrentArtwork(): Artwork {
        val manga = db.getManga(mangaId).executeAsBlocking()!!.toMangaItem()
        return Artwork(
            url = manga.userCover,
            inLibrary = manga.favorite,
            originalArtwork = manga.coverUrl,
            mangaId = mangaId,
        )
    }

    private fun createCurrentArtwork(manga: MangaItem): Artwork {
        return Artwork(
            url = manga.userCover,
            inLibrary = manga.favorite,
            originalArtwork = manga.coverUrl,
            mangaId = mangaId,
        )
    }

    private fun createAltArtwork(
        manga: MangaItem,
        currentArtwork: Artwork,
        dbArtwork: List<ArtworkImpl>,
    ): PersistentList<Artwork> {
        val quality = mangaDexPreferences.coverQuality().get()

        return dbArtwork
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
            .toPersistentList()
    }

    /** Get current sort filter */
    private fun getChapterDisplay(mangaItem: MangaItem): MangaConstants.ChapterDisplay {
        val manga = mangaItem.toManga()
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

        val available =
            when (manga.availableFilter(mangaDetailsPreferences)) {
                Manga.CHAPTER_SHOW_AVAILABLE -> ToggleableState.On
                Manga.CHAPTER_SHOW_UNAVAILABLE -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }

        val all =
            read == ToggleableState.Off &&
                bookmark == ToggleableState.Off &&
                downloaded == ToggleableState.Off &&
                available == ToggleableState.Off

        val matchesDefaults = mangaFilterMatchesDefault(manga)

        return MangaConstants.ChapterDisplay(
            showAll = all,
            unread = read,
            downloaded = downloaded,
            bookmarked = bookmark,
            hideChapterTitles = hideTitle,
            available = available,
            matchesGlobalDefaults = matchesDefaults,
        )
    }

    /** Used to filter sources, or scanlators/uploaders */
    private fun getChapterScanlatorFilter(
        mangaItem: MangaItem,
        allScanlators: ImmutableSet<String>,
    ): MangaConstants.ScanlatorFilter {
        val scanlatorSet = mangaItem.filteredScanlators.toSet()
        val scanlatorOptions =
            allScanlators
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .map { scanlator ->
                    MangaConstants.ScanlatorOption(
                        name = scanlator,
                        disabled = scanlator in scanlatorSet,
                    )
                }
                .toPersistentList()

        return MangaConstants.ScanlatorFilter(scanlators = scanlatorOptions.toPersistentList())
    }

    /** Used to filter sources, or scanlators/uploaders */
    private fun getChapterLanguageFilter(
        mangaItem: MangaItem,
        allLanguages: ImmutableSet<String>,
    ): MangaConstants.LanguageFilter {
        val languageSet = mangaItem.filteredLanguage.toSet()
        val languageOptions =
            allLanguages
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .map { language ->
                    MangaConstants.LanguageOption(
                        name = language,
                        disabled = language in languageSet,
                    )
                }
                .toPersistentList()

        return MangaConstants.LanguageFilter(languages = languageOptions.toPersistentList())
    }

    private fun getFilterText(
        chapterDisplay: MangaConstants.ChapterDisplay,
        chapterSourceFilter: MangaConstants.ScanlatorFilter,
        chapterScanlatorFilter: MangaConstants.ScanlatorFilter,
        languageFilter: MangaConstants.LanguageFilter,
    ): String {
        val hasDisabledScanlators = chapterScanlatorFilter.scanlators.any { it.disabled }
        val hasDisabledSources = chapterSourceFilter.scanlators.any { it.disabled }
        val hasDisabledLanguageFilters = languageFilter.languages.any { it.disabled }
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
        filtersId.add(
            if (chapterDisplay.available == ToggleableState.On) R.string.available else null
        )
        filtersId.add(
            if (chapterDisplay.available == ToggleableState.Indeterminate) R.string.unavailable
            else null
        )
        filtersId.add(if (hasDisabledLanguageFilters) R.string.language else null)
        filtersId.add(if (hasDisabledScanlators) R.string.scanlators else null)
        filtersId.add(if (hasDisabledSources) R.string.sources else null)

        return filtersId.filterNotNull().joinToString(", ") { preferences.context.getString(it) }
    }

    private fun getSortFilter(mangaItem: MangaItem): MangaConstants.SortFilter {
        val manga = mangaItem.toManga()
        val sortOrder = manga.chapterOrder(mangaDetailsPreferences)
        val status =
            when (manga.sortDescending(mangaDetailsPreferences)) {
                true -> MangaConstants.SortState.Descending
                false -> MangaConstants.SortState.Ascending
            }

        val matchesDefaults = mangaSortMatchesDefault(manga)

        return when (sortOrder) {
            Manga.CHAPTER_SORTING_SOURCE ->
                MangaConstants.SortFilter(
                    sourceOrderSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )
            Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                MangaConstants.SortFilter(
                    uploadDateSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )
            else ->
                MangaConstants.SortFilter(
                    smartOrderSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )
        }
    }

    private fun getNextUnread(
        mangaItem: MangaItem,
        activeChapters: PersistentList<ChapterItem>,
    ): NextUnreadChapter {
        val nextChapter =
            chapterSort.getNextUnreadChapter(mangaItem.toManga(), activeChapters)?.chapter
        return nextChapter?.let { chapter ->
            val id =
                if (chapter.lastPageRead > 0) {
                    R.string.continue_reading_
                } else {
                    R.string.start_reading_
                }

            val chapterText =
                when {
                    chapter.isMergedChapter() ||
                        (chapter.volume.isEmpty() && chapter.chapterText.isEmpty()) -> {
                        chapter.name
                    }
                    chapter.volume.isNotEmpty() -> "Vol. ${chapter.volume} ${chapter.chapterText}"
                    else -> chapter.chapterText
                }

            NextUnreadChapter(
                text = UiText.StringResource(id, chapterText),
                simpleChapter = chapter,
            )
        } ?: NextUnreadChapter()
    }

    private fun isMangaStatusCompleted(
        mangaItem: MangaItem,
        missingChapterCount: String,
        allChapters: List<ChapterItem>,
    ): Boolean {
        val cancelledOrCompleted =
            mangaItem.status == SManga.PUBLICATION_COMPLETE || mangaItem.status == SManga.CANCELLED
        if (
            cancelledOrCompleted && missingChapterCount == "" && mangaItem.lastChapterNumber != null
        ) {
            val final =
                allChapters
                    .filter { it.isAvailable() }
                    .filter {
                        getChapterNum(it.chapter.toSChapter())?.toInt() ==
                            mangaItem.lastChapterNumber
                    }
                    .filter {
                        getVolumeNum(it.chapter.toSChapter()) == mangaItem.lastVolumeNumber ||
                            getVolumeNum(it.chapter.toSChapter()) == null ||
                            mangaItem.lastVolumeNumber == null
                    }
            if (final.isNotEmpty()) {
                return true
            }
        }
        return false
    }

    fun onSearch(searchQuery: String?) {
        viewModelScope.launchIO {
            val searchActive = searchQuery != null

            _mangaDetailScreenState.update { it.copy(isSearching = searchActive) }

            val filteredChapters =
                when (searchActive) {
                    true -> {
                        mangaDetailScreenState.value.activeChapters.filter {
                            it.chapter.chapterTitle.contains(searchQuery, true) ||
                                it.chapter.scanlator.contains(searchQuery, true) ||
                                it.chapter.name.contains(searchQuery, true)
                        }
                    }
                    false -> emptyList()
                }

            _mangaDetailScreenState.update {
                it.copy(searchChapters = filteredChapters.toPersistentList())
            }
        }
    }

    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            val category = Category.create(newCategory)
            category.order =
                (_mangaDetailScreenState.value.allCategories.maxOfOrNull { it.order } ?: 0) + 1
            db.insertCategory(category).executeAsBlocking()
        }
    }

    fun updateMangaCategories(enabledCategories: List<CategoryItem>) {
        viewModelScope.launchIO {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            val categories =
                enabledCategories.map { MangaCategory.create(dbManga, it.toDbCategory()) }
            db.setMangaCategories(categories, listOf(dbManga))
        }
    }

    fun copiedToClipboard(message: String) {
        viewModelScope.launchIO {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string._copied_to_clipboard,
                        message = message,
                        snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                    )
                )
            }
        }
    }

    fun setAltTitle(title: String?) {
        viewModelScope.launchIO {
            val previousTitle = mangaDetailScreenState.value.currentTitle
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            dbManga.user_title = title ?: dbManga.title
            db.insertManga(dbManga).executeOnIO()
            appSnackbarManager.showSnackbar(
                SnackbarState(
                    messageRes = R.string.updated_title_to_,
                    message = dbManga.user_title,
                    actionLabelRes = R.string.undo,
                    action = { setAltTitle(previousTitle) },
                    snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                )
            )
        }
    }

    /** Toggle a manga as favorite */
    fun toggleFavorite(shouldAddToDefaultCategory: Boolean) {
        viewModelScope.launchIO {
            val editManga = db.getManga(mangaId).executeAsBlocking()!!
            editManga.apply {
                favorite = !favorite
                date_added =
                    when (favorite) {
                        true -> Date().time
                        false -> 0
                    }
            }

            db.insertManga(editManga).executeAsBlocking()
            // add to the default category if it exists and the user has the option set
            if (shouldAddToDefaultCategory && mangaDetailScreenState.value.hasDefaultCategory) {
                val defaultCategoryId = libraryPreferences.defaultCategory().get()
                mangaDetailScreenState.value.allCategories
                    .firstOrNull { defaultCategoryId == it.id }
                    ?.let { updateMangaCategories(listOf(it)) }
            }
        }
    }

    /** Update tracker with new status */
    fun updateTrackStatus(statusIndex: Int, trackAndService: TrackAndService) {
        viewModelScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackStatus(statusIndex, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Update tracker with new score */
    fun updateTrackScore(scoreIndex: Int, trackAndService: TrackAndService) {
        viewModelScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackScore(scoreIndex, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Update the tracker with the new chapter information */
    fun updateTrackChapter(newChapterNumber: Int, trackAndService: TrackAndService) {
        viewModelScope.launchIO {
            val trackingUpdate =
                trackingCoordinator.updateTrackChapter(newChapterNumber, trackAndService)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Update the tracker with the start/finished date */
    fun updateTrackDate(trackDateChange: TrackDateChange) {
        viewModelScope.launchIO {
            val trackingUpdate = trackingCoordinator.updateTrackDate(trackDateChange)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Search Tracker */
    fun searchTracker(title: String, service: TrackServiceItem) {
        viewModelScope.launchIO {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            val previouslyTracked =
                mangaDetailScreenState.value.tracks.firstOrNull {
                    service.id == it.trackServiceId
                } != null

            val result =
                trackingCoordinator.searchTrackerNonFlow(title, service, dbManga, previouslyTracked)
            _mangaDetailScreenState.update { it.copy(trackSearchResult = result) }
        }
    }

    /** Register tracker with service */
    fun registerTracking(trackAndService: TrackAndService, skipTrackFlowUpdate: Boolean = false) {
        viewModelScope.launchIO {
            val trackingUpdate = trackingCoordinator.registerTracking(trackAndService, mangaId)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Remove a tracker with an option to remove it from the tracking service */
    fun removeTracking(alsoRemoveFromTracker: Boolean, service: TrackServiceItem) {
        viewModelScope.launchIO {
            val trackingUpdate =
                trackingCoordinator.removeTracking(alsoRemoveFromTracker, service, mangaId)
            handleTrackingUpdate(trackingUpdate)
        }
    }

    /** Handle the TrackingUpdate */
    private suspend fun handleTrackingUpdate(trackingUpdate: TrackingUpdate) {
        if (trackingUpdate is TrackingUpdate.Error) {
            TimberKt.e(trackingUpdate.exception) { "handle tracking update had error" }
            appSnackbarManager.showSnackbar(
                SnackbarState(
                    message = trackingUpdate.message,
                    snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                )
            )
        }
    }

    /** Remove merged manga entry */
    fun removeMergedManga(mergeType: MergeType) {
        viewModelScope.launchIO {
            val dbManga = db.getManga(mangaId).executeOnIO()!!
            db.deleteMergeManga(mangaId).executeAsBlocking()
            val (mergedChapters, _) =
                db.getChapters(dbManga).executeOnIO().partition {
                    it.isMergedChapterOfType(mergeType)
                }
            if (!libraryPreferences.enableLocalChapters().get()) {
                downloadManager.deleteChapters(dbManga, mergedChapters)
            }
            db.deleteChapters(mergedChapters).executeOnIO()
        }
    }

    fun searchMergedManga(query: String, mergeType: MergeType) {
        viewModelScope.launchIO {
            _mangaDetailScreenState.update {
                it.copy(mergeSearchResult = MergeSearchResult.Loading)
            }

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

                    _mangaDetailScreenState.update {
                        when (mergedMangaResults.isEmpty()) {
                            true -> it.copy(mergeSearchResult = MergeSearchResult.NoResult)
                            false ->
                                it.copy(
                                    mergeSearchResult =
                                        MergeSearchResult.Success(mergedMangaResults)
                                )
                        }
                    }
                }
                .getOrElse { error ->
                    TimberKt.e(error) { "Error searching merged manga" }
                    _mangaDetailScreenState.update {
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
        viewModelScope.launchIO {
            val newMergedManga = mergeManga.toMergeMangaImpl(mangaId)
            db.insertMergeManga(newMergedManga).executeOnIO()
            onRefresh(true)
        }
    }

    /** Get change Sort option */
    fun changeSortOption(sortOption: SortOption?) {
        viewModelScope.launchIO {
            val manga = db.getManga(mangaId).executeAsBlocking()!!

            if (sortOption == null) {
                manga.setSortToGlobal()
            } else {
                val sortInt =
                    when (sortOption.sortType) {
                        MangaConstants.SortType.ChapterNumber -> Manga.CHAPTER_SORTING_SMART
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
        }
    }

    /** Get current merge result */
    fun changeFilterOption(filterOption: MangaConstants.ChapterDisplayOptions?) {
        viewModelScope.launchIO {
            val manga = db.getManga(mangaId).executeAsBlocking()!!

            if (
                !manga.usesLocalFilter &&
                    manga.readFilter(mangaDetailsPreferences) == Manga.SHOW_ALL &&
                    manga.downloadedFilter(mangaDetailsPreferences) == Manga.SHOW_ALL &&
                    manga.bookmarkedFilter(mangaDetailsPreferences) == Manga.SHOW_ALL &&
                    manga.availableFilter(mangaDetailsPreferences) == Manga.SHOW_ALL
            ) {
                manga.readFilter = Manga.SHOW_ALL
                manga.bookmarkedFilter = Manga.SHOW_ALL
                manga.downloadedFilter = Manga.SHOW_ALL
                manga.availableFilter = Manga.SHOW_ALL
            }

            if (filterOption == null) {
                manga.setFilterToGlobal()
            } else {
                when (filterOption.displayType) {
                    MangaConstants.ChapterDisplayType.All -> {
                        manga.readFilter = Manga.SHOW_ALL
                        manga.bookmarkedFilter = Manga.SHOW_ALL
                        manga.downloadedFilter = Manga.SHOW_ALL
                        manga.availableFilter = Manga.SHOW_ALL
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
                    MangaConstants.ChapterDisplayType.Available -> {
                        manga.availableFilter =
                            when (filterOption.displayState) {
                                ToggleableState.On -> Manga.CHAPTER_SHOW_AVAILABLE
                                ToggleableState.Indeterminate -> Manga.CHAPTER_SHOW_UNAVAILABLE
                                else -> Manga.SHOW_ALL
                            }
                    }
                }
                manga.setFilterToLocal()
                if (mangaFilterMatchesDefault(manga)) {
                    manga.setFilterToGlobal()
                }
            }

            db.updateChapterFlags(manga).executeOnIO()
        }
    }

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun changeScanlatorOption(scanlatorOption: MangaConstants.ScanlatorOption?) {
        viewModelScope.launchIO {
            val manga = db.getManga(mangaId).executeAsBlocking()!!

            val newFilteredScanlators =
                if (scanlatorOption != null) {
                    val filteredScanlators =
                        ChapterUtil.getScanlators(manga.filtered_scanlators).toMutableSet()
                    when (scanlatorOption.disabled) {
                        true -> filteredScanlators.add(scanlatorOption.name)
                        false -> filteredScanlators.remove(scanlatorOption.name)
                    }
                    filteredScanlators
                } else {
                    emptySet()
                }

            updateMangaScanlator(manga, newFilteredScanlators)
        }
    }

    private fun updateMangaScanlator(manga: Manga, filteredScanlators: Set<String>) {
        viewModelScope.launchIO {
            manga.filtered_scanlators =
                when (filteredScanlators.isEmpty()) {
                    true -> null
                    false -> ChapterUtil.getScanlatorString(filteredScanlators)
                }

            db.insertManga(manga).executeOnIO()
        }
    }

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun changeLanguageOption(languageOptions: MangaConstants.LanguageOption?) {
        viewModelScope.launchIO {
            val manga = db.getManga(mangaId).executeAsBlocking()!!

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

            updateMangaFilteredLanguages(manga, newFilteredLanguages)
        }
    }

    /** Updates the filtered scanlators */
    private fun updateMangaFilteredLanguages(manga: Manga, filteredLanguages: Set<String>) {
        viewModelScope.launchIO {
            manga.filtered_language =
                when (filteredLanguages.isEmpty()) {
                    true -> null
                    false -> ChapterUtil.getLanguageString(filteredLanguages)
                }

            db.insertManga(manga).executeOnIO()
        }
    }

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun setGlobalOption(option: MangaConstants.SetGlobal) {
        viewModelScope.launchIO {
            val manga = db.getManga(mangaId).executeAsBlocking()!!
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
                    mangaDetailsPreferences.filterChapterByAvailable().set(manga.availableFilter)
                    manga.setFilterToGlobal()
                }
            }
            db.updateChapterFlags(manga).executeOnIO()
        }
    }

    /** Update the current artwork with the vibrant color */
    fun updateMangaColor(vibrantColor: Int) {
        viewModelScope.launchIO {
            MangaCoverMetadata.addVibrantColor(mangaId, vibrantColor)
            _mangaDetailScreenState.update {
                it.copy(vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId))
            }
        }
    }

    fun clearRemovedChapters() {
        viewModelScope.launchIO {
            _mangaDetailScreenState.update { it.copy(removedChapters = persistentListOf()) }
        }
    }

    fun autoAddTrackers(
        mangaItem: MangaItem,
        loggedInTrackerService: List<TrackServiceItem>,
        tracks: List<TrackItem>,
    ) {
        viewModelScope.launchIO {
            val autoAddTracker = preferences.autoAddTracker().get()
            val manga = mangaItem.toManga()
            loggedInTrackerService
                .firstOrNull { it.isMdList }
                ?.let {
                    val mdList = trackManager.mdList
                    var track = tracks.firstOrNull { mdList.matchingTrack(it) }?.toDbTrack()
                    if (track == null) {
                        track = mdList.createInitialTracker(manga)

                        if (isOnline()) {
                            // Try to bind the new track to the remote service (e.g., get a remote
                            // ID)
                            runCatching {
                                    mdList.bind(track)
                                } // Assumes bind() mutates the 'track' object
                                .onFailure { exception ->
                                    TimberKt.e(exception) { "Error binding new MangaDex track" }
                                }
                        }
                        // Save the new track (with or without remote data) to the local DB *once*
                        db.insertTrack(track).executeOnIO()
                    }
                    val autoAddStatus = mangaDexPreferences.autoAddToMangaDexLibrary().get()
                    val canAutoAdd =
                        mangaItem.favorite && FollowStatus.isUnfollowed(track.status) && isOnline()

                    if (canAutoAdd) {
                        val newStatus =
                            when (autoAddStatus) {
                                1 -> FollowStatus.PLAN_TO_READ.int
                                3 -> FollowStatus.READING.int
                                2 -> FollowStatus.ON_HOLD.int
                                else -> null // Preference is not set to auto-add
                            }

                        if (newStatus != null) {
                            track.status = newStatus
                            trackingCoordinator.updateTrackingService(
                                track.toTrackItem(),
                                mdList.toTrackServiceItem(),
                            )
                        }
                    }
                }

            if (autoAddTracker.size <= 1 || !mangaItem.favorite) return@launchIO

            val validContentRatings = preferences.autoTrackContentRatingSelections().get()
            val contentRating = manga.getContentRating()

            if (contentRating != null && !validContentRatings.contains(contentRating.lowercase()))
                return@launchIO

            if (!isOnline()) {
                launchUI {
                    appSnackbarManager.showSnackbar(
                        SnackbarState(
                            message = "No network connection, cannot autolink tracker",
                            snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                        )
                    )
                }
                return@launchIO
            }

            val existingTrackIds = tracks.map { it.trackServiceId }.toSet()

            autoAddTracker
                .mapNotNull { it.toIntOrNull() } // Safely convert preference strings to Ints
                .map { autoAddTrackerId ->
                    async {
                        val trackService =
                            loggedInTrackerService.firstOrNull { it.id == autoAddTrackerId }
                                ?: return@async // Not logged in to this service, skip

                        if (trackService.id in existingTrackIds)
                            return@async // Already tracked, skip

                        // Check if the manga has a remote ID for this service
                        trackManager.getIdFromManga(trackService, manga)
                            ?: return@async // No ID found, skip

                        // --- 4. Perform the search and register the track ---

                        // We are online, not tracked, and have a remote ID. Proceed.
                        val trackResult =
                            trackingCoordinator.searchTrackerNonFlow(
                                title = "",
                                service =
                                    trackManager.getService(trackService.id)!!.toTrackServiceItem(),
                                manga = manga,
                                previouslyTracker = false,
                            )

                        when (trackResult) {
                            is TrackingConstants.TrackSearchResult.Success -> {
                                val trackSearchItem = trackResult.trackSearchResult.firstOrNull()
                                if (trackSearchItem != null) {
                                    // Found a match, register it
                                    val trackingUpdate =
                                        trackingCoordinator.registerTracking(
                                            TrackAndService(
                                                trackSearchItem.trackItem,
                                                trackService,
                                            ),
                                            mangaId,
                                        )
                                    handleTrackingUpdate(trackingUpdate)
                                } else {
                                    TimberKt.w {
                                        "Auto-track search for ${trackService.id} was successful but returned no results."
                                    }
                                }
                            }
                            is TrackingConstants.TrackSearchResult.Error -> {
                                // Show a specific error for *this* tracker
                                launchUI {
                                    appSnackbarManager.showSnackbar(
                                        SnackbarState(
                                            prefixRes = trackResult.trackerNameRes,
                                            message =
                                                " error trying to autolink tracking. ${trackResult.errorMessage}",
                                            snackBarColor =
                                                _mangaDetailScreenState.value.snackbarColor,
                                        )
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                }
                .awaitAll()
        }
    }

    /** Save the given url cover to file */
    fun saveCover(artwork: Artwork, destDir: UniFile? = null) {
        viewModelScope.launchIO {
            try {
                val directory = destDir ?: storageManager.getCoverDirectory()!!

                val destinationUri = saveCover(directory, artwork)
                launchUI {
                    /*view?.applicationContext?.let { context ->
                        DiskUtil.scanMedia(context, destinationUri)
                        view?.applicationContext?.toast(R.string.cover_saved)
                    }*/
                }
            } catch (e: Exception) {
                TimberKt.e(e) { "error saving cover" }
                /* launchUI { view?.applicationContext?.toast("Error saving cover") }*/
            }
        }
    }

    /** Save Cover to directory, if given a url save that specific cover */
    private fun saveCover(directory: UniFile, artwork: Artwork): Uri {
        val dbManga = db.getManga(mangaId).executeAsBlocking()!!
        val cover =
            when (artwork.url.isBlank() || dbManga.thumbnail_url == artwork.url) {
                true ->
                    coverCache.getCustomCoverFile(dbManga).takeIf { it.exists() }
                        ?: coverCache.getCoverFile(dbManga.thumbnail_url, dbManga.favorite)
                false -> coverCache.getCoverFile(artwork.url)
            }

        val type = ImageUtil.findImageType(cover.inputStream()) ?: throw Exception("Not an image")

        // Build destination file.
        val fileNameNoExtension =
            listOfNotNull(dbManga.title, artwork.volume.ifEmpty { null }, dbManga.uuid())
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
        viewModelScope.launchIO {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            coverCache.setCustomCoverToCache(dbManga, artwork.url)
            MangaCoverMetadata.remove(mangaId)
            dbManga.user_cover = artwork.url
            db.insertManga(dbManga).executeOnIO()
        }
    }

    /** Reset cover */
    fun resetCover() {
        viewModelScope.launchIO {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            coverCache.deleteCustomCover(dbManga)
            MangaCoverMetadata.remove(mangaId)
            dbManga.user_cover = null
            db.insertManga(dbManga).executeOnIO()
        }
    }

    /**
     * share the cover that is written in the destination folder. If a url is passed in then share
     * that one instead of the manga thumbnail url one
     */
    suspend fun shareCover(destDir: UniFile, artwork: Artwork): Uri? {
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

    fun openComment(context: Context, chapterId: String) {
        viewModelScope.launchIO {
            when (!isOnline()) {
                true ->
                    appSnackbarManager.showSnackbar(
                        SnackbarState(
                            message = "No network connection, cannot open comments",
                            snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                        )
                    )
                false -> {
                    _mangaDetailScreenState.update { it.copy(isRefreshing = true) }
                    val threadId =
                        sourceManager.mangaDex
                            .getChapterCommentId(chapterId)
                            .onFailure { TimberKt.e { it.message() } }
                            .getOrElse { null }
                    _mangaDetailScreenState.update { it.copy(isRefreshing = false) }
                    if (threadId == null) {
                        appSnackbarManager.showSnackbar(
                            SnackbarState(messageRes = R.string.comments_unavailable)
                        )
                    } else {
                        context.openInWebView(MdConstants.forumUrl + threadId, title = "Comments")
                    }
                }
            }
        }
    }

    private fun syncChaptersReadStatus() {
        viewModelScope.launchIO {
            if (
                !mangaDexPreferences.readingSync().get() || !loginHelper.isLoggedIn() || !isOnline()
            ) {
                return@launchIO
            }

            runCatching {
                    val dbManga = db.getManga(mangaId).executeAsBlocking()!!
                    statusHandler.getReadChapterIds(dbManga.uuid()).collect { chapterIds ->
                        val chaptersToMarkRead =
                            mangaDetailScreenState.value.allChapters
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
                    viewModelScope.launchIO {
                        delay(3000)
                        appSnackbarManager.showSnackbar(
                            SnackbarState(
                                "Error trying to mark chapters read from MangaDex $it",
                                snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                            )
                        )
                    }
                }
        }
    }

    private fun observeDownloads() {
        viewModelScope.launchIO {
            downloadManager
                .statusFlow()
                .filter { it.mangaItem.id == mangaId }
                .catch { error -> TimberKt.e(error) }
                .collect { updateDownloadState(it) }
        }

        viewModelScope.launchIO {
            downloadManager
                .progressFlow()
                .filter { it.mangaItem.id == mangaId }
                .catch { error -> TimberKt.e(error) }
                .collect { updateDownloadState(it) }
        }
    }

    private fun updateDownloadState(download: Download) {
        viewModelScope.launchIO {
            if (download.status == Download.State.ERROR && download.errorMessage != null) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        message = download.errorMessage,
                        snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                    )
                )
            }
            _mangaDetailScreenState.update { state ->
                state.copy(
                    allChapters =
                        updateChapterListForDownloadState(
                            chapterId = download.chapterItem.id,
                            downloadStatus = download.status,
                            downloadProgress = download.progress,
                            chapters = state.allChapters,
                        ),
                    activeChapters =
                        updateChapterListForDownloadState(
                            chapterId = download.chapterItem.id,
                            downloadStatus = download.status,
                            downloadProgress = download.progress,
                            chapters = state.activeChapters,
                        ),
                    searchChapters =
                        updateChapterListForDownloadState(
                            chapterId = download.chapterItem.id,
                            downloadStatus = download.status,
                            downloadProgress = download.progress,
                            chapters = state.searchChapters,
                        ),
                )
            }
        }
    }

    fun updateChapterListForDownloadState(
        chapterId: Long,
        downloadStatus: Download.State,
        downloadProgress: Int,
        chapters: PersistentList<ChapterItem>,
    ): PersistentList<ChapterItem> {
        val index = chapters.indexOfFirst { it.chapter.id == chapterId }
        return if (index >= 0) {
            val updatedChapter =
                chapters[index].copy(
                    downloadState = downloadStatus,
                    downloadProgress = downloadProgress,
                )
            chapters.set(index, updatedChapter)
        } else {
            chapters
        }
    }

    private fun updateRemovedDownload(chapterItems: List<ChapterItem>) {
        viewModelScope.launchIO {
            chapterItems.forEach { chapterItem ->
                _mangaDetailScreenState.update { state ->
                    state.copy(
                        allChapters =
                            updateChapterListForDownloadState(
                                chapterId = chapterItem.chapter.id,
                                downloadStatus = Download.State.NOT_DOWNLOADED,
                                downloadProgress = 0,
                                chapters = state.allChapters,
                            ),
                        activeChapters =
                            updateChapterListForDownloadState(
                                chapterId = chapterItem.chapter.id,
                                downloadStatus = Download.State.NOT_DOWNLOADED,
                                downloadProgress = 0,
                                chapters = state.activeChapters,
                            ),
                        searchChapters =
                            updateChapterListForDownloadState(
                                chapterId = chapterItem.chapter.id,
                                downloadStatus = Download.State.NOT_DOWNLOADED,
                                downloadProgress = 0,
                                chapters = state.searchChapters,
                            ),
                    )
                }
            }
        }
    }

    fun getManga(): Manga {
        return db.getManga(mangaId).executeAsBlocking()!!
    }

    fun getChapterUrl(chapter: SimpleChapter): String {
        return chapter.getHttpSource(sourceManager).getChapterUrl(chapter)
    }

    fun blockScanlator(blockType: MangaConstants.BlockType, name: String) {
        viewModelScope.launchIO {
            when (blockType) {
                MangaConstants.BlockType.Group -> {
                    val scanlatorGroupImpl = db.getScanlatorGroupByName(name).executeAsBlocking()
                    if (scanlatorGroupImpl == null) {
                        launchIO { mangaUpdateCoordinator.updateGroup(name) }
                    }
                    val blockedGroups = mangaDexPreferences.blockedGroups().get().toMutableSet()
                    blockedGroups.add(name)
                    mangaDexPreferences.blockedGroups().set(blockedGroups)
                }
                MangaConstants.BlockType.Uploader -> {
                    val uploaderImpl = db.getUploaderByName(name).executeAsBlocking()
                    if (uploaderImpl == null) {
                        launchIO { mangaUpdateCoordinator.updateUploader(name) }
                    }
                    val blockedUploaders =
                        mangaDexPreferences.blockedUploaders().get().toMutableSet()
                    blockedUploaders.add(name)
                    mangaDexPreferences.blockedUploaders().set(blockedUploaders)
                }
            }
            appSnackbarManager.showSnackbar(
                SnackbarState(
                    messageRes = R.string.globally_blocked_group_,
                    message = name,
                    actionLabelRes = R.string.undo,
                    action = {
                        viewModelScope.launchIO {
                            when (blockType) {
                                MangaConstants.BlockType.Group -> {
                                    db.deleteScanlatorGroup(name).executeOnIO()
                                    val allBlockedGroups =
                                        mangaDexPreferences.blockedGroups().get().toMutableSet()
                                    allBlockedGroups.remove(name)
                                    mangaDexPreferences.blockedGroups().set(allBlockedGroups)
                                }

                                MangaConstants.BlockType.Uploader -> {
                                    db.deleteUploader(name).executeOnIO()
                                    val allBlockedUploaders =
                                        mangaDexPreferences.blockedUploaders().get().toMutableSet()
                                    allBlockedUploaders.remove(name)
                                    mangaDexPreferences.blockedUploaders().set(allBlockedUploaders)
                                }
                            }
                        }
                    },
                    snackBarColor = _mangaDetailScreenState.value.snackbarColor,
                )
            )
        }
    }

    fun updateSnackbarColor(snackbarColor: SnackbarColor) {
        viewModelScope.launch {
            _mangaDetailScreenState.update { it.copy(snackbarColor = snackbarColor) }
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
                mangaDetailsPreferences.hideChapterTitlesByDefault().get()) &&
            manga.availableFilter == mangaDetailsPreferences.filterChapterByAvailable().get() ||
            !manga.usesLocalFilter
    }
}

private data class AllInfo(
    val mangaItem: MangaItem,
    val isMerged: MergeConstants.IsMergedManga = No,
    val isRefreshing: Boolean = false,
    val mangaStatusCompleted: Boolean = false,
    val chapterDisplay: MangaConstants.ChapterDisplay = MangaConstants.ChapterDisplay(),
    val chapterSourceFilter: MangaConstants.ScanlatorFilter = MangaConstants.ScanlatorFilter(),
    val chapterScanlatorFilter: MangaConstants.ScanlatorFilter = MangaConstants.ScanlatorFilter(),
    val chapterLanguageFilter: MangaConstants.LanguageFilter = MangaConstants.LanguageFilter(),
    val chapterSortFilter: MangaConstants.SortFilter = MangaConstants.SortFilter(),
    val chapterFilterText: String = "",
    val allCategories: PersistentList<CategoryItem> = persistentListOf(),
    val mangaCategories: PersistentList<CategoryItem> = persistentListOf(),
    val allChapterInfo: AllChapterInfo = AllChapterInfo(),
    val artwork: Artwork,
    val altArtwork: PersistentList<Artwork> = persistentListOf(),
    val tracks: PersistentList<TrackItem> = persistentListOf(),
    val loggedInTrackerService: PersistentList<TrackServiceItem> = persistentListOf(),
    val trackServiceCount: Int = 0,
)

private data class AllChapterInfo(
    val nextUnread: NextUnreadChapter = NextUnreadChapter(),
    val missingChapters: MissingChapterHolder = MissingChapterHolder(),
    val activeChapters: PersistentList<ChapterItem> = persistentListOf(),
    val allChapters: PersistentList<ChapterItem> = persistentListOf(),
    val allScanlators: PersistentSet<String> = persistentSetOf(),
    val allUploaders: PersistentSet<String> = persistentSetOf(),
    val allSources: PersistentSet<String> = persistentSetOf(),
    val allLanguages: PersistentSet<String> = persistentSetOf(),
)
