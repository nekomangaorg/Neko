package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
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
import org.nekomanga.domain.manga.toManga
import org.nekomanga.domain.manga.toMangaItem
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

    private val _snackbarState = MutableSharedFlow<SnackbarState>()
    val snackBarState: SharedFlow<SnackbarState> = _snackbarState.asSharedFlow()



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
        initialLoad()

        presenterScope.launchIO {

            combine(mangaFlow, allChapterFlow, allCategoriesFlow, mangaCategoriesFlow) { mangaItem, allChapters, allCategories, mangaCategories ->
                if(mangaItem.initialized) {
                    _mangaDetailScreenState.update { it.copy(isRefreshing = false, ) }

                    val chapterInfo = createAllChapterInfo(mangaItem, allChapters.toPersistentList())

                    AllInfo(mangaItem = mangaItem, allCategories = allCategories.toPersistentList(), mangaCategories = mangaCategories.toPersistentList(), allChapterInfo = chapterInfo)
                }else{
                    AllInfo(isRefreshing = true, mangaItem = mangaItem)
                }



            }.distinctUntilChanged().collectLatest {allInfo ->
                _mangaDetailScreenState.update { it.copy(
                    alternativeTitles = allInfo.mangaItem.getAltTitles().toPersistentList(),
                    artist = allInfo.mangaItem.artist,
                    author = allInfo.mangaItem.author,
                    currentDescription = if(allInfo.mangaItem.initialized),
                    currentTitle = m.title,
                    externalLinks = m.getExternalLinks().toPersistentList(),
                    genres = (m.getGenres(true) ?: emptyList()).toPersistentList(),
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
                    lastVolume = m.last_volume_number,
                    lastChapter = m.last_chapter_number,
                    isRefreshing = allInfo.isRefreshing,
                    allChapters = allInfo.allChapterInfo.allChapters,
                    allSources = allInfo.allChapterInfo.allSources,
                    allCategories = allInfo.allCategories,
                    currentCategories = allInfo.mangaCategories,
                ) }

            }
        }





    }

    fun initialLoad(){
    //refresh tracking
        presenterScope.launchIO {
            val tracks =
                _mangaDetailScreenState.map { it.tracks }.distinctUntilChanged().firstOrNull()
            tracks ?: return@launchIO
            val isOnline =
                downloadManager.networkStateFlow().map { it }.distinctUntilChanged().firstOrNull()
            isOnline ?: return@launchIO

            if (isOnline.isOnline) {
                tracks.filter {
                    val service = trackManager.getService(it.trackServiceId)
                    service != null && service.isLogged()
                }.mapAsync { trackItem ->
                    val service = trackManager.getService(trackItem.trackServiceId)!!
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
        }


        presenterScope.launchIO {
            val validMergeTypes = MergeType.entries.filter { mergeType ->
                when (mergeType) {
                    MergeType.MangaLife, MergeType.Comick -> false
                    // Conditionally keep these types if they are configured
                    MergeType.Komga -> sourceManager.komga.isConfigured()
                    MergeType.Suwayomi -> sourceManager.suwayomi.isConfigured()
                    // Keep all other types
                    else -> true
                }
            }.toPersistentList()


            val loggedInServices = trackManager.services
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

    val mangaFlow = db.getManga(mangaId).asRxObservable().asFlow().map { it.toMangaItem() }.distinctUntilChanged()
    val allCategoriesFlow = db.getCategories().asFlow().map{ categories -> categories.map { it.toCategoryItem() } }.distinctUntilChanged()
    val mangaCategoriesFlow = db.getCategoriesForManga(mangaId).asFlow().map { categories -> categories.map { it.toCategoryItem() } }.distinctUntilChanged()

    val tracksFlow = db.getTracks(mangaId).asFlow().map { tracks-> tracks.map { it.toTrackItem() } }.distinctUntilChanged()


    val allChapterFlow = combine(db.getChapters(mangaId).asFlow().distinctUntilChanged(), mangaDexPreferences.blockedGroups().changes(), mangaDexPreferences.blockedUploaders().changes()){ dbChapters, blockedGroups, blockedUploaders ->
        val dbManga = db.getManga(mangaId).executeAsBlocking()!!
        dbChapters.mapNotNull { it.toSimpleChapter() }
                .filter { chapter ->
                    val scanlators = chapter.scanlatorList()
                    scanlators.none { scanlator -> blockedGroups.contains(scanlator) } &&
                        (Constants.NO_GROUP !in scanlators || chapter.uploader !in blockedUploaders)
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

    private fun createAllChapterInfo(mangaItem: MangaItem, allChapters: PersistentList<ChapterItem>): AllChapterInfo {
        val allSources = mutableSetOf(MdConstants.name)

        val allChapterScanlators =
            allChapters
                .flatMap { ChapterUtil.getScanlators(it.chapter.scanlator) }
                .toMutableSet()
        val allChapterUploaders =
            allChapters
                .mapNotNull {
                    if (it.chapter.uploader.isEmpty()) return@mapNotNull null
                    if (it.chapter.scanlator != Constants.NO_GROUP) return@mapNotNull null
                    it.chapter.uploader
                }
                .toPersistentSet()

        if (
            allChapterScanlators.size == 1 &&
            !mangaItem.filtered_scanlators.isEmpty()
        ) {
            val manga = mangaItem.copy(filtered_scanlators = "").toManga()
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


        return AllChapterInfo(
            allChapters = allChapters,
            allScanlators = allChapterScanlators.toPersistentSet(),
            allUploaders = allChapterUploaders,
            allSources = allSources.toPersistentSet(),
            allLanguages = allLanguages
        )
    }




}

private data class AllInfo(
    val mangaItem: MangaItem,
    val isRefreshing: Boolean = false,
    val allCategories : PersistentList<CategoryItem> = persistentListOf(),
    val mangaCategories : PersistentList<CategoryItem> = persistentListOf(),
    val allChapterInfo: AllChapterInfo = AllChapterInfo(),
)
private data class AllChapterInfo(
    val allChapters: PersistentList<ChapterItem> = persistentListOf(),
    val allScanlators : PersistentSet<String> = persistentSetOf(),
    val allUploaders : PersistentSet<String> = persistentSetOf(),
    val allSources : PersistentSet<String> = persistentSetOf(),
    val allLanguages : PersistentSet<String> = persistentSetOf(),

    )
