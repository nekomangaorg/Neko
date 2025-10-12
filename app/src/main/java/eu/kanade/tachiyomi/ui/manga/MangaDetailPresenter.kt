package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.asFlow
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.domain.track.toTrackItem
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.usecases.chapters.ChapterUseCases
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

        combine(chapterFlow(mangaId), allCategoriesFlow(), mangaCategoriesFlow(mangaId)){

        }





    }

    fun initialLoad(){
        presenterScope.launchIO {
            val dbManga = db.getManga(mangaId).executeAsBlocking()!!
            val categories = db.getCategories().executeAsBlocking().map { it.toCategoryItem() }.toPersistentList()
            val mangaCategories = db.getCategoriesForManga(mangaId).executeAsBlocking().map { it.toCategoryItem() }.toPersistentList()

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
                    allCategories = categories,
                    currentCategories = mangaCategories,
                )
            }

            if(dbManga.initialized){
               // onRefresh()
            }


        }
    }

    val mangaFlow = db.getManga(mangaId).asRxObservable().asFlow().map { it.toMangaItem() }.distinctUntilChanged()
    val chapterFlow = db.getChapters(mangaId).asFlow().distinctUntilChanged()
    val allCategoriesFlow = db.getCategories().asFlow().distinctUntilChanged()
    val mangaCategoriesFlow = db.getCategoriesForManga(mangaId).asFlow().map { categories -> categories.map { it.toCategoryItem() } }.distinctUntilChanged()

    val tracksFlow = db.getTracks(mangaId).asFlow().map { tracks-> tracks.map { it.toTrackItem() } }.distinctUntilChanged()


}
