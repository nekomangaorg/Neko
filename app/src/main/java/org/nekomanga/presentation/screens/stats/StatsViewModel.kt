package org.nekomanga.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import java.util.Calendar
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.R
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.MergeMangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_NOT_COMPLETED
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_NOT_STARTED
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_TRACKING_COMPLETED
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_TRACKING_DROPPED
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_TRACKING_ON_HOLD
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_TRACKING_PLAN_TO_READ
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaStatus
import org.nekomanga.domain.manga.MangaType
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedStatManga
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedState
import org.nekomanga.presentation.screens.stats.StatsHelper.getReadDuration
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class StatsViewModel() : ViewModel() {
    private val categoryRepository: CategoryRepository = Injekt.get()
    private val historyRepository: HistoryRepository = Injekt.get()
    private val mangaRepository: MangaRepository = Injekt.get()
    private val mergeMangaRepository: MergeMangaRepository = Injekt.get()
    private val chapterRepository: ChapterRepository = Injekt.get()

    private val trackRepository: TrackRepository = Injekt.get()

    private val prefs: PreferencesHelper = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val trackManager: TrackManager = Injekt.get()
    private val downloadManager: DownloadManager = Injekt.get()

    private val _simpleState = MutableStateFlow(StatsConstants.SimpleState())
    val simpleState: StateFlow<StatsConstants.SimpleState> = _simpleState.asStateFlow()

    private val _detailState = MutableStateFlow(DetailedState())
    val detailState: StateFlow<DetailedState> = _detailState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launchIO {
            val libraryList = getLibrary()
            if (libraryList.isEmpty()) {
                _simpleState.update {
                    StatsConstants.SimpleState(screenState = StatsConstants.ScreenState.NoResults)
                }
                _detailState.update { it.copy(isLoading = false) }
            } else {
                val tracks = getTracks(libraryList)
                val tracksByMangaId = tracks.groupBy { it.manga_id }

                getStatsState(libraryList, tracks, tracksByMangaId)
                getDetailState(libraryList, tracksByMangaId)
            }
        }
    }

    private suspend fun getStatsState(
        libraryList: List<LibraryManga>,
        tracks: List<Track>,
        tracksByMangaId: Map<Long, List<Track>>,
    ) {
        val lastUpdate = libraryPreferences.lastUpdateTimestamp().get()
        val lastUpdateAttempt = libraryPreferences.lastUpdateAttemptTimestamp().get()
        val lastUpdateDuration = libraryPreferences.lastUpdateDuration().get()

        val favoritedMangalist = mangaRepository.getFavoriteMangaList()

        val mergedMangaList =
            mergeMangaRepository.getAllMergeManga().mapNotNull { mergedManga ->
                when (
                    favoritedMangalist.firstOrNull { manga -> manga.id!! == mergedManga.mangaId } !=
                        null
                ) {
                    true -> mergedManga
                    false -> null
                }
            }

        mergedMangaList.groupBy { it.mergeType }.map { it.key to it.value.size }.toPersistentList()

        _simpleState.update {
            StatsConstants.SimpleState(
                screenState = StatsConstants.ScreenState.Simple,
                mangaCount = libraryList.count(),
                chapterCount = libraryList.sumOf { it.totalChapters },
                readCount = libraryList.sumOf { it.read },
                bookmarkCount = libraryList.sumOf { it.bookmarkCount },
                unavailableCount = libraryList.sumOf { it.unavailableCount },
                trackedCount = getMangaByTrackCount(libraryList, tracks),
                mergeCounts =
                    mergedMangaList
                        .groupBy { it.mergeType }
                        .map { it.key to it.value.size }
                        .toPersistentList(),
                globalUpdateCount = getGlobalUpdateManga(libraryList, tracksByMangaId).count(),
                downloadCount = libraryList.sumOf { getDownloadCount(it) },
                tagCount =
                    libraryList
                        .mapNotNull { it.getGenres() }
                        .flatten()
                        .distinct()
                        .count { !it.contains("content rating:", true) },
                trackerCount = getLoggedTrackers().count(),
                readDuration = getReadDuration(),
                averageMangaRating = getAverageMangaRating(libraryList),
                averageUserRating = getUserScore(tracks),
                lastLibraryUpdate = if (lastUpdate == 0L) "" else lastUpdate.timeSpanFromNow,
                lastLibraryUpdateAttempt =
                    if (lastUpdateAttempt == 0L) "" else lastUpdateAttempt.timeSpanFromNow,
                lastLibraryUpdateDuration = lastUpdateDuration,
            )
        }
    }

    /**
     * MACRO-LEVEL PERFORMANCE OPTIMIZATION (Overclock):
     *
     * Why: Previously, [getDetailState] performed N+1 database queries inside an async mapping loop
     * for every manga in the library. This included fetching history, tracking info, and categories
     * per manga ID individually. For a library with 1,000+ items, this generated 3,000 parallel
     * coroutine database queries, causing database connection pool thrashing, thread starvation,
     * high CPU overhead, and UI stutter.
     *
     * Architecture: Replaced N+1 synchronous and individual async queries with chunked, batch
     * database pre-fetching. We fetch all histories, tracks, category linkages, and full category
     * lists up-front, chunking the manga IDs to respect SQLite bind parameter limits (900 limits).
     * These collections are then mapped into memory using O(1) lookups via Map groupings.
     */
    private suspend fun getDetailState(
        libraryList: List<LibraryManga>,
        tracksByMangaId: Map<Long, List<Track>>,
    ) {
        val allHistories = getHistory(libraryList)
        val allMangaCategories = getMangaCategories(libraryList)
        val allCategories = categoryRepository.getCategories()
        val allChapters = getChapters(libraryList)

        val chapterToMangaMap =
            allChapters
                .mapNotNull { chapter ->
                    val id = chapter.id ?: return@mapNotNull null
                    val mangaId = chapter.manga_id ?: return@mapNotNull null
                    id to mangaId
                }
                .toMap()
        val historiesByMangaId =
            allHistories
                .filter { it.chapter_id in chapterToMangaMap }
                .groupBy { chapterToMangaMap[it.chapter_id]!! }
        val mangaCategoriesByMangaId = allMangaCategories.groupBy { it.manga_id }
        val categoriesById = allCategories.associateBy { it.id }

        val detailedStatMangaList =
            libraryList
                .map {
                    val history = historiesByMangaId[it.id] ?: emptyList()
                    val tracks = tracksByMangaId[it.id] ?: emptyList()
                    val mangaCats = mangaCategoriesByMangaId[it.id] ?: emptyList()
                    val catNames = mangaCats.mapNotNull { mc ->
                        categoriesById[mc.category_id]?.name
                    }

                    DetailedStatManga(
                        id = it.id!!,
                        title = it.title,
                        type = MangaType.fromLangFlag(it.lang_flag),
                        status = MangaStatus.fromStatus(it.status),
                        contentRating = MangaContentRating.getContentRating(it.getContentRating()),
                        totalChapters = it.totalChapters,
                        readChapters = it.read,
                        bookmarkedChapters = it.bookmarkCount,
                        unavailableChapters = it.unavailableCount,
                        readDuration = getReadDurationFromHistory(history),
                        startYear = getStartYear(history),
                        rating = it.rating?.toDoubleOrNull()?.roundToTwoDecimal(),
                        tags = (it.getGenres() ?: emptyList()).toPersistentList(),
                        userScore = getUserScore(tracks),
                        trackers =
                            tracks
                                .mapNotNull { trackManager.getService(it.sync_id) }
                                .map { prefs.context.getString(it.nameRes()) }
                                .toPersistentList(),
                        categories =
                            (catNames.takeUnless { it.isEmpty() }
                                    ?: listOf(prefs.context.getString(R.string.default_value)))
                                .sorted()
                                .toPersistentList(),
                    )
                }
                .sortedBy { it.title }
        _detailState.update {
            DetailedState(
                isLoading = false,
                manga = detailedStatMangaList.toPersistentList(),
                categories =
                    (categoryRepository.getCategories().map { it.name } +
                            listOf(prefs.context.getString(R.string.default_value)))
                        .toPersistentList(),
                tags =
                    detailedStatMangaList
                        .asSequence()
                        .map { it.tags }
                        .flatten()
                        .distinct()
                        .filter { !it.contains("content rating:", true) }
                        .sortedBy { it }
                        .toPersistentList(),
            )
        }

        val sortedSeries =
            _detailState.value.tags
                .map { tag ->
                    tag to
                        _detailState.value.manga.filter { it.tags.contains(tag) }.toPersistentList()
                }
                .sortedByDescending { it.second.count() }
                .toPersistentList()
        val totalCount = sortedSeries.sumOf { it.second.size }
        val totalDuration = sortedSeries.sumOf { pair -> pair.second.sumOf { it.readDuration } }

        _detailState.update {
            it.copy(
                detailTagState =
                    StatsConstants.DetailedTagState(
                        totalReadDuration = totalDuration,
                        totalChapters = totalCount,
                        sortedTagPairs = sortedSeries,
                    )
            )
        }
    }

    fun switchState() {
        viewModelScope.launchIO {
            val newState =
                when (simpleState.value.screenState) {
                    is StatsConstants.ScreenState.Simple -> {
                        StatsConstants.ScreenState.Detailed
                    }
                    else -> {
                        StatsConstants.ScreenState.Simple
                    }
                }

            _simpleState.update { it.copy(screenState = newState) }
        }
    }

    private suspend fun getLibrary(): List<LibraryManga> {
        return mangaRepository.getLibraryList().distinctBy { it.id }
    }

    private suspend fun getTracks(mangaList: List<LibraryManga>): List<Track> {
        return coroutineScope {
            mangaList
                .asSequence()
                .mapNotNull { it.id }
                .chunked(900)
                .map { chunk -> async { trackRepository.getTracksForMangaByIds(chunk) } }
                .toList()
                .awaitAll()
                .flatten()
        }
    }

    private suspend fun getTracks(mangaList: LibraryManga): List<Track> {
        return trackRepository.getTracksForManga(mangaList.id!!)
    }

    private fun getMangaByTrackCount(mangaList: List<LibraryManga>, tracks: List<Track>): Int {
        val trackedMangaIds =
            tracks
                .mapNotNull { track ->
                    if (
                        track.sync_id == TrackManager.MDLIST &&
                            FollowStatus.isUnfollowed(track.status)
                    ) {
                        return@mapNotNull null
                    }
                    track.manga_id
                }
                .toSet()
        return mangaList.count { it.id in trackedMangaIds }
    }

    private fun getLoggedTrackers(): List<TrackService> {
        return trackManager.services.values.filter { it.isLogged() }
    }

    private fun hasTrackWithGivenStatus(tracks: List<Track>, globalStatusId: Int): Boolean {
        return tracks.any { track ->
            val status = trackManager.getService(track.sync_id)?.getGlobalStatus(track.status)
            if (status.isNullOrBlank()) {
                false
            } else {
                globalStatusId == trackManager.getGlobalStatusResId(status)
            }
        }
    }

    private fun getGlobalUpdateManga(
        libraryManga: List<LibraryManga>,
        tracksByMangaId: Map<Long, List<Track>>,
    ): Map<Long?, List<LibraryManga>> {
        val includedCategories =
            libraryPreferences.whichCategoriesToUpdate().get().map(String::toInt)
        val excludedCategories =
            libraryPreferences.whichCategoriesToExclude().get().map(String::toInt)
        val restrictions = libraryPreferences.autoUpdateDeviceRestrictions().get()
        return libraryManga
            .groupBy { it.id }
            .filterNot { it.value.any { manga -> manga.category in excludedCategories } }
            .filter {
                includedCategories.isEmpty() ||
                    it.value.any { manga -> manga.category in includedCategories }
            }
            .filter {
                val manga = it.value.first()
                val mangaTracks = tracksByMangaId[manga.id] ?: emptyList()
                when {
                    MANGA_HAS_UNREAD in restrictions && manga.unread != 0 -> true
                    MANGA_NOT_STARTED in restrictions &&
                        manga.totalChapters > 0 &&
                        !manga.hasStarted -> true
                    MANGA_NOT_COMPLETED in restrictions && manga.status == SManga.COMPLETED -> true
                    MANGA_TRACKING_PLAN_TO_READ in restrictions &&
                        hasTrackWithGivenStatus(mangaTracks, R.string.follows_plan_to_read) -> false
                    MANGA_TRACKING_DROPPED in restrictions &&
                        hasTrackWithGivenStatus(mangaTracks, R.string.follows_dropped) -> false
                    MANGA_TRACKING_ON_HOLD in restrictions &&
                        hasTrackWithGivenStatus(mangaTracks, R.string.follows_on_hold) -> false
                    MANGA_TRACKING_COMPLETED in restrictions &&
                        hasTrackWithGivenStatus(mangaTracks, R.string.follows_completed) -> false
                    else -> true
                }
            }
    }

    private fun getDownloadCount(manga: LibraryManga): Int {
        return downloadManager.getDownloadCount(manga)
    }

    private fun get10PointScore(track: Track): Float? {
        val service = trackManager.getService(track.sync_id)
        return service?.get10PointScore(track.score)
    }

    private suspend fun getReadDuration(): String {
        val chaptersTime = historyRepository.getTotalReadDuration()
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
        val scores = mangaTracks.mapNotNull { track ->
            // perf: combine filter/map operations to reduce intermediate list allocations
            if (track.score <= 0) return@mapNotNull null
            get10PointScore(track)?.takeIf { it > 0.0 }
        }

        return when (scores.isEmpty()) {
            true -> 0.0
            false -> scores.average().roundToTwoDecimal()
        }
    }

    private suspend fun getHistory(mangaList: List<LibraryManga>): List<History> {
        return coroutineScope {
            mangaList
                .asSequence()
                .mapNotNull { it.id }
                .chunked(900)
                .map { chunk -> async { historyRepository.getHistoryByMangaIds(chunk) } }
                .toList()
                .awaitAll()
                .flatten()
        }
    }

    private suspend fun getMangaCategories(mangaList: List<LibraryManga>): List<MangaCategory> {
        return coroutineScope {
            mangaList
                .asSequence()
                .mapNotNull { it.id }
                .chunked(900)
                .map { chunk -> async { categoryRepository.getMangaCategories(chunk) } }
                .toList()
                .awaitAll()
                .flatten()
        }
    }

    private suspend fun getChapters(mangaList: List<LibraryManga>): List<Chapter> {
        return coroutineScope {
            mangaList
                .asSequence()
                .mapNotNull { it.id }
                .chunked(900)
                .map { chunk -> async { chapterRepository.getChaptersForMangaIds(chunk) } }
                .toList()
                .awaitAll()
                .flatten()
        }
    }
}
