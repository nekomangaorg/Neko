package eu.kanade.tachiyomi.ui.manga

import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.MergedServerSource
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.No
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga.Yes
import eu.kanade.tachiyomi.util.MissingChapterHolder
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.chapter.getChapterNum
import eu.kanade.tachiyomi.util.chapter.getVolumeNum
import eu.kanade.tachiyomi.util.getMissingChapters
import eu.kanade.tachiyomi.util.isAvailable
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.chapter.ChapterItem
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
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.domain.track.toTrackItem
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases
import org.nekomanga.util.system.mapAsync
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaDetailPresenter(
    private val mangaId: Long,
    val preferences: PreferencesHelper = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    val securityPreferences: SecurityPreferences = Injekt.get(),
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

    private val _mangaDetailScreenState =
        MutableStateFlow(
            MangaConstants.MangaDetailScreenState(currentArtwork = Artwork(mangaId = mangaId))
        )

    val mangaDetailScreenState: StateFlow<MangaConstants.MangaDetailScreenState> =
        _mangaDetailScreenState.asStateFlow()

    private val _snackbarState = MutableSharedFlow<SnackbarState>()
    val snackBarState: SharedFlow<SnackbarState> = _snackbarState.asSharedFlow()

    private val chapterSort = ChapterItemSort(chapterItemFilter, preferences)

    fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        flow7: Flow<T7>,
        transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
    ): Flow<R> =
        combine(
            combine(flow, flow2, flow3, ::Triple),
            combine(flow4, flow5, flow6, ::Triple),
            flow7,
        ) { t1, t2, f7 ->
            transform(t1.first, t1.second, t1.third, t2.first, t2.second, t2.third, f7)
        }

    override fun onCreate() {
        super.onCreate()
        presenterScope.launchIO {
            if (!db.getManga(mangaId).executeAsBlocking()!!.initialized) {
                onRefresh()
            }
        }

        initialLoad()

        presenterScope.launchIO {
            combine(mangaFlow, allChapterFlow, allCategoriesFlow, mangaCategoriesFlow) {
                    mangaItem,
                    allChapters,
                    allCategories,
                    mangaCategories ->
                    val artwork = createCurrentArtwork(mangaItem)

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

                    AllInfo(
                        mangaItem = mangaItem,
                        mangaStatusCompleted = mangaStatusCompleted,
                        chapterDisplay = getChapterDisplay(mangaItem),
                        chapterSourceFilter =
                            getChapterScanlatorFilter(mangaItem, chapterInfo.allSources),
                        chapterScanlatorFilter =
                            getChapterScanlatorFilter(
                                mangaItem,
                                (chapterInfo.allScanlators + chapterInfo.allUploaders)
                                    .toPersistentSet(),
                            ),
                        chapterLanguageFilter =
                            getChapterLanguageFilter(mangaItem, chapterInfo.allLanguages),
                        chapterSortFilter = getSortFilter(mangaItem),
                        allCategories = allCategories.toPersistentList(),
                        mangaCategories = mangaCategories.toPersistentList(),
                        allChapterInfo = chapterInfo,
                        artwork = artwork,
                        altArtwork = alternativeArtwork,
                    )
                }
                .distinctUntilChanged()
                .collectLatest { allInfo ->
                    _mangaDetailScreenState.update {
                        it.copy(
                            alternativeTitles = allInfo.mangaItem.altTitles,
                            artist = allInfo.mangaItem.artist,
                            author = allInfo.mangaItem.author,
                            alternativeArtwork = allInfo.altArtwork,
                            currentArtwork = allInfo.artwork,
                            currentDescription = allInfo.mangaItem.getDescription(),
                            currentTitle = allInfo.mangaItem.title,
                            externalLinks = allInfo.mangaItem.externalLinks,
                            genres = allInfo.mangaItem.genre,
                            initialized = allInfo.mangaItem.initialized,
                            inLibrary = allInfo.mangaItem.favorite,
                            isMerged = isMerged(),
                            isPornographic =
                                allInfo.mangaItem.contentRating.equals(
                                    MdConstants.ContentRating.pornographic,
                                    ignoreCase = true,
                                ),
                            langFlag = allInfo.mangaItem.langFlag,
                            missingChapters = allInfo.mangaItem.missingChapters,
                            originalTitle = allInfo.mangaItem.ogTitle,
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
                            chapterSortFilter = allInfo.chapterSortFilter,
                            chapterScanlatorFilter = allInfo.chapterScanlatorFilter,
                            chapterSourceFilter = allInfo.chapterSourceFilter,
                            chapterLanguageFilter = allInfo.chapterLanguageFilter,
                            allChapters = allInfo.allChapterInfo.allChapters,
                            allSources = allInfo.allChapterInfo.allSources,
                            allCategories = allInfo.allCategories,
                            currentCategories = allInfo.mangaCategories,
                        )
                    }

                    val chapterCountChanged =
                        allInfo.mangaItem.missingChapters !=
                            allInfo.allChapterInfo.missingChapters.count
                    val statusNeedsUpdate =
                        allInfo.mangaStatusCompleted && allInfo.mangaItem.status != SManga.COMPLETED

                    if (chapterCountChanged || statusNeedsUpdate) {
                        val finalManga =
                            allInfo.mangaItem
                                .let { manga ->
                                    if (chapterCountChanged)
                                        manga.copy(
                                            missingChapters =
                                                allInfo.allChapterInfo.missingChapters.count
                                        )
                                    else manga
                                }
                                .let { manga ->
                                    if (statusNeedsUpdate) manga.copy(status = SManga.COMPLETED)
                                    else manga
                                }

                        db.insertManga(finalManga.toManga()).executeAsBlocking()
                    }
                }
        }
    }

    fun onRefresh(isMerging: Boolean = false) {
        TimberKt.d { "On Refresh called" }
        presenterScope.launchIO {
            if (!isOnline()) {
                _snackbarState.emit(SnackbarState(messageRes = R.string.no_network_connection))
                return@launchIO
            }

            _mangaDetailScreenState.update { it.copy(isRefreshing = true) }

            val mangaItem = db.getManga(mangaId).executeAsBlocking()!!.toMangaItem()

            mangaUpdateCoordinator
                .update(mangaItem = mangaItem, isMerging = isMerging)
                .onCompletion { _mangaDetailScreenState.update { it.copy(isRefreshing = false) } }
                .catch { e -> e.message?.let { _snackbarState.emit(SnackbarState(message = it)) } }
                .collect { result ->
                    when (result) {
                        is MangaResult.Error -> {
                            _snackbarState.emit(
                                SnackbarState(message = result.text, messageRes = result.id)
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
        presenterScope.launchNonCancellable {
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

    suspend fun isOnline(): Boolean {
        val networkState =
            downloadManager.networkStateFlow().map { it }.distinctUntilChanged().firstOrNull()
        networkState ?: return false

        return networkState.isOnline
    }

    fun initialLoad() {
        // refresh tracking
        presenterScope.launchIO {
            val tracks =
                _mangaDetailScreenState.map { it.tracks }.distinctUntilChanged().firstOrNull()
            tracks ?: return@launchIO
            val networkState =
                downloadManager.networkStateFlow().map { it }.distinctUntilChanged().firstOrNull()
            networkState ?: return@launchIO

            if (isOnline()) {
                tracks
                    .filter {
                        val service = trackManager.getService(it.trackServiceId)
                        service != null && service.isLogged()
                    }
                    .mapAsync { trackItem ->
                        val service = trackManager.getService(trackItem.trackServiceId)!!
                        runCatching { service.refresh(trackItem.toDbTrack()) }
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
        }

        presenterScope.launchIO {
            val validMergeTypes =
                MergeType.entries
                    .filter { mergeType ->
                        when (mergeType) {
                            MergeType.MangaLife,
                            MergeType.Comick -> false
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
                    extraLargeBackdrop = mangaDetailsPreferences.extraLargeBackdrop().get(),
                    forcePortrait = mangaDetailsPreferences.forcePortrait().get(),
                    themeBasedOffCovers = mangaDetailsPreferences.autoThemeByCover().get(),
                    wrapAltTitles = mangaDetailsPreferences.wrapAltTitles().get(),
                    validMergeTypes = validMergeTypes,
                    loggedInTrackService = loggedInServices,
                    vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId),
                )
            }
        }
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
        db.getCategoriesForManga(mangaId)
            .asFlow()
            .map { categories -> categories.map { it.toCategoryItem() } }
            .distinctUntilChanged()

    val tracksFlow =
        db.getTracks(mangaId)
            .asFlow()
            .map { tracks -> tracks.map { it.toTrackItem() } }
            .distinctUntilChanged()

    val allChapterFlow =
        combine(
            db.getChapters(mangaId).asFlow().distinctUntilChanged(),
            mangaDexPreferences.blockedGroups().changes(),
            mangaDexPreferences.blockedUploaders().changes(),
        ) { dbChapters, blockedGroups, blockedUploaders ->
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            dbChapters
                .mapNotNull { it.toSimpleChapter() }
                .filter { chapter ->
                    val scanlators = chapter.scanlatorList()
                    scanlators.none { scanlator -> blockedGroups.contains(scanlator) } &&
                        (Constants.NO_GROUP !in scanlators || chapter.uploader !in blockedUploaders)
                }
                .map { chapter ->
                    val downloadState =
                        when {
                            downloadManager.isChapterDownloaded(chapter.toDbChapter(), dbManga) ->
                                Download.State.DOWNLOADED
                            else -> {
                                val download = downloadManager.getQueuedDownloadOrNull(chapter.id)
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
                                    downloadManager.getQueuedDownloadOrNull(chapter.id)!!.progress
                                false -> 0
                            },
                    )
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

    private fun isMerged(): MergeConstants.IsMergedManga {
        val mergeMangaList = db.getMergeMangaList(mangaId = mangaId).executeAsBlocking()
        return when (mergeMangaList.isNotEmpty()) {
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
        return when (nextChapter == null) {
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
                            (nextChapter.volume.isEmpty() && nextChapter.chapterText.isEmpty())
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
    val isRefreshing: Boolean = false,
    val mangaStatusCompleted: Boolean = false,
    val chapterDisplay: MangaConstants.ChapterDisplay = MangaConstants.ChapterDisplay(),
    val chapterSourceFilter: MangaConstants.ScanlatorFilter = MangaConstants.ScanlatorFilter(),
    val chapterScanlatorFilter: MangaConstants.ScanlatorFilter = MangaConstants.ScanlatorFilter(),
    val chapterLanguageFilter: MangaConstants.LanguageFilter = MangaConstants.LanguageFilter(),
    val chapterSortFilter: MangaConstants.SortFilter = MangaConstants.SortFilter(),
    val allCategories: PersistentList<CategoryItem> = persistentListOf(),
    val mangaCategories: PersistentList<CategoryItem> = persistentListOf(),
    val allChapterInfo: AllChapterInfo = AllChapterInfo(),
    val artwork: Artwork,
    val altArtwork: PersistentList<Artwork> = persistentListOf(),
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
