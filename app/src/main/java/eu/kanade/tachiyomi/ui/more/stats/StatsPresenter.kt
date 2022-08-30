package eu.kanade.tachiyomi.ui.more.stats

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.MANGA_HAS_UNREAD
import eu.kanade.tachiyomi.data.preference.MANGA_NON_COMPLETED
import eu.kanade.tachiyomi.data.preference.MANGA_NON_READ
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.more.stats.StatsHelper.STATUS_COLOR_MAP
import eu.kanade.tachiyomi.ui.more.stats.StatsHelper.getReadDuration
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class StatsPresenter(
    private val db: DatabaseHelper = Injekt.get(),
    private val prefs: PreferencesHelper = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
) : BaseCoroutinePresenter<StatsController>() {

    private val _statsState = MutableStateFlow(StatsConstants.StatsState(loading = true))
    val statsState: StateFlow<StatsConstants.StatsState> = _statsState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        presenterScope.launchIO {
            _statsState.value = getStatsState()
        }
    }

    private fun getStatsState(): StatsConstants.StatsState {
        val libraryList = getLibrary()
        val tracks = getTracks(libraryList)
        val lastUpdate = prefs.libraryUpdateLastTimestamp().get()

        return StatsConstants.StatsState(
            loading = false,
            mangaCount = libraryList.count(),
            chapterCount = libraryList.sumOf { it.totalChapters },
            readCount = libraryList.sumOf { it.read },
            trackedCount = getMangaByTrackCount(libraryList, tracks),
            mergeCount = libraryList.mapNotNull { it.merge_manga_url }.count(),
            globalUpdateCount = getGlobalUpdateManga(libraryList).count(),
            downloadCount = libraryList.sumOf { getDownloadCount(it) },
            tagCount = libraryList.mapNotNull { it.getGenres() }.flatten().distinct().count(),
            trackerCount = getLoggedTrackers().count(),
            readDuration = getReadDuration(libraryList),
            averageMangaRating = libraryList.mapNotNull { it.rating?.toDoubleOrNull() }.average().roundToTwoDecimal(),
            averageUserRating = getUserScore(tracks),
            lastLibraryUpdate = if (lastUpdate == 0L) "" else lastUpdate.timeSpanFromNow,
            statusDistribution = getStatusDistribution(libraryList),
        )
    }

    private fun getStatusDistribution(libraryList: MutableList<LibraryManga>): ImmutableList<StatsConstants.StatusDistribution> {
        val statuses = libraryList.map { it.status }
        return STATUS_COLOR_MAP.keys.map { status ->
            StatsConstants.StatusDistribution(status, statuses.count { it == status })
        }.toImmutableList()
    }

    private fun getLibrary(): MutableList<LibraryManga> {
        return db.getLibraryMangaList().executeAsBlocking()
    }

    fun getTracks(mangaList: List<LibraryManga>): List<Track> {
        return db.getTracks(mangaList.map { it.id!! }).executeAsBlocking()
    }

    fun getMangaByTrackCount(mangaList: List<LibraryManga>, tracks: List<Track>): Int {
        return mangaList.map { it.id!! }.map { mangaId ->
            tracks.filter { it.manga_id == mangaId }.any {
                !(it.sync_id == TrackManager.MDLIST && FollowStatus.isUnfollowed(it.status))
            }
        }.count { it }
    }

    private fun getLoggedTrackers(): List<TrackService> {
        return trackManager.services.values.filter { it.isLogged() }
    }

    private fun getGlobalUpdateManga(libraryManga: List<LibraryManga>): Map<Long?, List<LibraryManga>> {
        val includedCategories = prefs.libraryUpdateCategories().get().map(String::toInt)
        val excludedCategories = prefs.libraryUpdateCategoriesExclude().get().map(String::toInt)
        val restrictions = prefs.libraryUpdateDeviceRestriction().get()
        return libraryManga.groupBy { it.id }
            .filterNot { it.value.any { manga -> manga.category in excludedCategories } }
            .filter { includedCategories.isEmpty() || it.value.any { manga -> manga.category in includedCategories } }
            .filterNot {
                val manga = it.value.first()
                (MANGA_NON_COMPLETED in restrictions && manga.status == SManga.COMPLETED) ||
                    (MANGA_HAS_UNREAD in restrictions && manga.unread != 0) ||
                    (MANGA_NON_READ in restrictions && manga.totalChapters > 0 && !manga.hasRead)
            }
    }

    private fun getDownloadCount(manga: LibraryManga): Int {
        return downloadManager.getDownloadCount(manga)
    }

    private fun get10PointScore(track: Track): Float? {
        val service = trackManager.getService(track.sync_id)
        return service?.get10PointScore(track.score)
    }

    private fun getReadDuration(libraryManga: List<LibraryManga>): String {
        val chaptersTime = libraryManga.sumOf { manga ->
            db.getHistoryByMangaId(manga.id!!).executeAsBlocking().sumOf { it.time_read }
        }
        return chaptersTime.getReadDuration(prefs.context.getString(R.string.none))
    }

    private fun getUserScore(mangaTracks: List<Track>): Double {
        val scores = mangaTracks.filter { track ->
            track.score > 0
        }.mapNotNull { track ->
            get10PointScore(track)
        }.filter {
            it > 0.0
        }
        return if (scores.isEmpty()) {
            0.0
        } else {
            scores.average().roundToTwoDecimal()
        }
    }
}
