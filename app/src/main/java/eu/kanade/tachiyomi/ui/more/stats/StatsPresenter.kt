package eu.kanade.tachiyomi.ui.more.stats

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.History
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
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.DetailedStatManga
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.DetailedState
import eu.kanade.tachiyomi.ui.more.stats.StatsHelper.getReadDuration
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import java.util.Calendar
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaStatus
import org.nekomanga.domain.manga.MangaType
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class StatsPresenter(
    private val db: DatabaseHelper = Injekt.get(),
    private val prefs: PreferencesHelper = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
) : BaseCoroutinePresenter<StatsController>() {

    private val _simpleState = MutableStateFlow(StatsConstants.SimpleState())
    val simpleState: StateFlow<StatsConstants.SimpleState> = _simpleState.asStateFlow()

    private val _detailState = MutableStateFlow(DetailedState())
    val detailState: StateFlow<DetailedState> = _detailState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        getDetailState()
        getStatsState()
    }

    private fun getStatsState() {
        presenterScope.launchIO {
            val libraryList = getLibrary()
            if (libraryList.isEmpty()) {
                _simpleState.value = StatsConstants.SimpleState(
                    screenState = StatsConstants.ScreenState.NoResults,
                )
            } else {
                val tracks = getTracks(libraryList)
                val lastUpdate = prefs.libraryUpdateLastTimestamp().get()
                _simpleState.value = StatsConstants.SimpleState(
                    screenState = StatsConstants.ScreenState.Simple,
                    mangaCount = libraryList.count(),
                    chapterCount = libraryList.sumOf { it.totalChapters },
                    readCount = libraryList.sumOf { it.read },
                    trackedCount = getMangaByTrackCount(libraryList, tracks),
                    mergeCount = libraryList.mapNotNull { it.merge_manga_url }.count(),
                    globalUpdateCount = getGlobalUpdateManga(libraryList).count(),
                    downloadCount = libraryList.sumOf { getDownloadCount(it) },
                    tagCount = libraryList.mapNotNull { it.getGenres() }.flatten().distinct().count { !it.contains("content rating:", true) },
                    trackerCount = getLoggedTrackers().count(),
                    readDuration = getReadDuration(),
                    averageMangaRating = getAverageMangaRating(libraryList),
                    averageUserRating = getUserScore(tracks),
                    lastLibraryUpdate = if (lastUpdate == 0L) "" else lastUpdate.timeSpanFromNow,
                )
            }
        }
    }

    private fun getDetailState() {
        presenterScope.launchIO {
            val libraryList = getLibrary()
            if (libraryList.isNotEmpty()) {
                val detailedStatMangaList = libraryList.map {
                    async {
                        val history = db.getHistoryByMangaId(it.id!!).executeAsBlocking()
                        val tracks = getTracks(it)

                        DetailedStatManga(
                            id = it.id!!,
                            title = it.title,
                            type = MangaType.fromLangFlag(it.lang_flag),
                            status = MangaStatus.fromStatus(it.status),
                            contentRating = MangaContentRating.getContentRating(it.getContentRating()),
                            totalChapters = it.totalChapters,
                            readChapters = it.read,
                            readDuration = getReadDurationFromHistory(history),
                            startYear = getStartYear(history),
                            rating = it.rating?.toDoubleOrNull()?.roundToTwoDecimal(),
                            tags = (it.getGenres() ?: emptyList()).toImmutableList(),
                            userScore = getUserScore(tracks),
                            trackers = tracks.mapNotNull { trackManager.getService(it.sync_id) }.map { prefs.context.getString(it.nameRes()) }.toImmutableList(),
                            categories = (
                                db.getCategoriesForManga(it).executeAsBlocking().map { category -> category.name }.takeUnless { it.isEmpty() }
                                    ?: listOf(prefs.context.getString(R.string.default_value))
                                ).sorted().toImmutableList(),
                        )
                    }
                }.awaitAll().sortedBy { it.title }
                _detailState.value = DetailedState(
                    isLoading = false,
                    manga = detailedStatMangaList.toImmutableList(),
                    categories = (db.getCategories().executeAsBlocking().map { it.name } + listOf(prefs.context.getString(R.string.default_value))).toPersistentList(),
                    tags = detailedStatMangaList.asSequence().map { it.tags }.flatten().distinct().filter { !it.contains("content rating:", true) }.sortedBy { it }.toImmutableList(),
                )

                val sortedSeries =
                    _detailState.value.tags.map { tag -> tag to _detailState.value.manga.filter { it.tags.contains(tag) }.toImmutableList() }.sortedByDescending { it.second.count() }.toImmutableList()
                val totalCount = sortedSeries.sumOf { it.second.size }
                val totalDuration = sortedSeries.sumOf { pair -> pair.second.sumOf { it.readDuration } }

                _detailState.update {
                    it.copy(
                        detailTagState = StatsConstants.DetailedTagState(totalReadDuration = totalDuration, totalChapters = totalCount, sortedTagPairs = sortedSeries),
                    )
                }
            }
        }
    }

    fun switchState() {
        presenterScope.launchIO {
            val newState = when (simpleState.value.screenState) {
                is StatsConstants.ScreenState.Simple -> {
                    StatsConstants.ScreenState.Detailed
                }
                else -> {
                    StatsConstants.ScreenState.Simple
                }
            }

            _simpleState.value = simpleState.value.copy(screenState = newState)
        }
    }

    private fun getLibrary(): List<LibraryManga> {
        return db.getLibraryMangaList().executeAsBlocking().distinctBy { it.id }
    }

    private fun getTracks(mangaList: List<LibraryManga>): List<Track> {
        return db.getTracks(mangaList.map { it.id!! }).executeAsBlocking()
    }

    private fun getTracks(mangaList: LibraryManga): List<Track> {
        return db.getTracks(mangaList.id!!).executeAsBlocking()
    }

    private fun getMangaByTrackCount(mangaList: List<LibraryManga>, tracks: List<Track>): Int {
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

    private fun getReadDuration(): String {
        val chaptersTime = db.getTotalReadDuration()
        return chaptersTime.getReadDuration(prefs.context.getString(R.string.none))
    }

    private fun getReadDurationFromHistory(history: List<History>): Long {
        return history.sumOf { it.time_read }
    }

    private fun getStartYear(history: List<History>): Int? {
        val oldestDate = history.filter { it.last_read > 0 }.minOfOrNull { it.last_read }
        if (oldestDate == null || oldestDate <= 0L) {
            return null
        } else {
            return Calendar.getInstance().apply { timeInMillis = oldestDate }.get(Calendar.YEAR)
        }
    }

    private fun getAverageMangaRating(libraryList: List<LibraryManga>): Double {
        val ratings = libraryList.mapNotNull { it.rating?.toDoubleOrNull() }
        return when (ratings.isEmpty()) {
            true -> 0.0
            false -> ratings.average().roundToTwoDecimal()
        }
    }

    private fun getUserScore(mangaTracks: List<Track>): Double {
        val scores = mangaTracks.filter { track ->
            track.score > 0
        }.mapNotNull { track ->
            get10PointScore(track)
        }.filter {
            it > 0.0
        }

        return when (scores.isEmpty()) {
            true -> 0.0
            false -> scores.average().roundToTwoDecimal()
        }
    }
}
