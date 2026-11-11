package org.nekomanga.presentation.screens.stats

import android.icu.text.NumberFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import java.util.Calendar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.R
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.MergeMangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaStatus
import org.nekomanga.domain.manga.MangaType
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedStatManga
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedState
import org.nekomanga.presentation.screens.stats.StatsConstants.SimpleStatMetricUiModel
import org.nekomanga.presentation.screens.stats.StatsHelper.getReadDuration
import org.nekomanga.usecases.category.CategoryUseCases
import org.nekomanga.usecases.library.ShouldUpdateMangaUseCase
import org.nekomanga.usecases.stats.AggregateDetailedStatsUseCase
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class StatsViewModel() : ViewModel() {
    private val categoryUseCases: CategoryUseCases = Injekt.get()
    private val historyRepository: HistoryRepository = Injekt.get()
    private val mangaRepository: MangaRepository = Injekt.get()
    private val mergeMangaRepository: MergeMangaRepository = Injekt.get()
    private val chapterRepository: ChapterRepository = Injekt.get()

    private val trackRepository: TrackRepository = Injekt.get()

    private val prefs: PreferencesHelper = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val trackManager: TrackManager = Injekt.get()
    private val downloadManager: DownloadManager = Injekt.get()
    private val shouldUpdateMangaUseCase: ShouldUpdateMangaUseCase by injectLazy()
    private val aggregateDetailedStatsUseCase: AggregateDetailedStatsUseCase by injectLazy()

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

        val favoritedMangaIds = mangaRepository.getFavoriteMangaList().mapNotNull { it.id }.toSet()

        val mergedMangaList =
            mergeMangaRepository.getAllMergeManga().filter { mergedManga ->
                mergedManga.mangaId in favoritedMangaIds
            }

        val numberFormat = NumberFormat.getInstance(NumberFormat.NUMBERSTYLE)
        val na = prefs.context.getString(R.string.n_a)
        val never = prefs.context.getString(R.string.never)

        val mangaCount = libraryList.count()
        val chapterCount = libraryList.sumOf { it.totalChapters }
        val readCount = libraryList.sumOf { it.read }
        val bookmarkCount = libraryList.sumOf { it.bookmarkCount }
        val unavailableCount = libraryList.sumOf { it.unavailableCount }
        val trackedCount = getMangaByTrackCount(libraryList, tracks)
        val globalUpdateCount = getGlobalUpdateManga(libraryList, tracksByMangaId).count()
        val downloadCount = libraryList.sumOf { getDownloadCount(it) }
        val tagCount =
            libraryList
                .mapNotNull { it.getGenres() }
                .flatten()
                .distinct()
                .count { !it.contains("content rating:", true) }
        val trackerCount = getLoggedTrackers().count()
        val readDuration = getReadDuration()
        val averageMangaRating = getAverageMangaRating(libraryList)
        val userScore = getUserScore(tracks)

        val userScoreStr =
            when (userScore == 0.0) {
                true -> na
                false -> userScore.toString()
            }
        val libUpdates =
            when (lastUpdate == 0L) {
                true -> na
                false -> lastUpdate.timeSpanFromNow
            }
        val libUpdateAttempt =
            when (lastUpdateAttempt == 0L) {
                true -> never
                false -> lastUpdateAttempt.timeSpanFromNow
            }
        val lastUpdateDurationStr =
            when (lastUpdateDuration.isEmpty()) {
                true -> never
                false -> lastUpdateDuration
            }

        val mergeCounts =
            mergedMangaList.groupBy { it.mergeType }.map { it.key to it.value.size }.toList()

        val mergeMetrics = mergeCounts.mapNotNull {
            if (it.second > 0) {
                SimpleStatMetricUiModel(
                    title = UiText.String("${it.first.scanlatorName} merged"),
                    formattedValue = it.second.toString(),
                )
            } else {
                null
            }
        }

        val simpleMetrics =
            listOf(
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.total_manga),
                    formattedValue = numberFormat.format(mangaCount),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.total_chapters),
                    formattedValue = numberFormat.format(chapterCount),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.chapters_read),
                    formattedValue = numberFormat.format(readCount),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.chapters_bookmarked),
                    formattedValue = numberFormat.format(bookmarkCount),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.chapters_unavailable),
                    formattedValue = numberFormat.format(unavailableCount),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.read_duration),
                    formattedValue = readDuration,
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.last_library_update),
                    formattedValue = libUpdates,
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.last_library_update_duration),
                    formattedValue = lastUpdateDurationStr,
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.last_library_update_attempt),
                    formattedValue = libUpdateAttempt,
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.global_update_manga),
                    formattedValue = numberFormat.format(globalUpdateCount),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.average_score),
                    formattedValue = averageMangaRating.toString(),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.mean_score),
                    formattedValue = userScoreStr,
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.trackers),
                    formattedValue = trackerCount.toString(),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.manga_tracked),
                    formattedValue = numberFormat.format(trackedCount),
                ),
                SimpleStatMetricUiModel(
                    title = UiText.StringResource(R.string.total_tags),
                    formattedValue = numberFormat.format(tagCount),
                ),
            ) + mergeMetrics

        _simpleState.update {
            StatsConstants.SimpleState(
                screenState = StatsConstants.ScreenState.Simple,
                mangaCount = mangaCount,
                chapterCount = chapterCount,
                readCount = readCount,
                bookmarkCount = bookmarkCount,
                unavailableCount = unavailableCount,
                trackedCount = trackedCount,
                mergeCounts = mergeCounts,
                globalUpdateCount = globalUpdateCount,
                downloadCount = downloadCount,
                tagCount = tagCount,
                trackerCount = trackerCount,
                readDuration = readDuration,
                averageMangaRating = averageMangaRating,
                averageUserRating = userScore,
                lastLibraryUpdate = if (lastUpdate == 0L) "" else lastUpdate.timeSpanFromNow,
                lastLibraryUpdateAttempt =
                    if (lastUpdateAttempt == 0L) "" else lastUpdateAttempt.timeSpanFromNow,
                lastLibraryUpdateDuration = lastUpdateDuration,
                simpleStatMetrics = simpleMetrics,
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
        val mangaCategoriesByMangaId =
            categoryUseCases.getCategories.getMangaCategories(libraryList.mapNotNull { it.id })
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

        val detailedStatMangaList =
            libraryList
                .map {
                    val history = historiesByMangaId[it.id] ?: emptyList()
                    val tracks = tracksByMangaId[it.id] ?: emptyList()
                    val mangaCats = mangaCategoriesByMangaId[it.id] ?: emptyList()
                    val catNames = mangaCats.map { it.name }

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
                        tags = (it.getGenres() ?: emptyList()).toList(),
                        userScore = getUserScore(tracks),
                        trackers =
                            tracks
                                .mapNotNull { trackManager.getService(it.sync_id) }
                                .map { prefs.context.getString(it.nameRes()) }
                                .toList(),
                        categories =
                            (catNames.takeUnless { it.isEmpty() }
                                    ?: listOf(prefs.context.getString(R.string.default_value)))
                                .sorted()
                                .toList(),
                    )
                }
                .sortedBy { it.title }
        val allCategories =
            (categoryUseCases.getCategories.get().map { it.name } +
                    listOf(prefs.context.getString(R.string.default_value)))
                .toList()

        val distribution =
            aggregateDetailedStatsUseCase(
                mangaList = detailedStatMangaList,
                categories = allCategories,
                defaultCategoryName = prefs.context.getString(R.string.default_value),
                typeStringResolver = { prefs.context.getString(it.typeRes) },
                statusStringResolver = { prefs.context.getString(it.statusRes) },
            )

        _detailState.update {
            DetailedState(
                isLoading = false,
                manga = detailedStatMangaList.toList(),
                categories = allCategories,
                tags =
                    detailedStatMangaList
                        .asSequence()
                        .map { it.tags }
                        .flatten()
                        .distinct()
                        .filter { !it.contains("content rating:", true) }
                        .sortedBy { it }
                        .toList(),
                distribution = distribution,
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

    private fun getGlobalUpdateManga(
        libraryManga: List<LibraryManga>,
        tracksByMangaId: Map<Long, List<Track>>,
    ): Map<Long?, List<LibraryManga>> {
        val includedCategories =
            libraryPreferences.whichCategoriesToUpdate().get().map(String::toInt)
        val excludedCategories =
            libraryPreferences.whichCategoriesToExclude().get().map(String::toInt)
        val restrictions = libraryPreferences.autoUpdateMangaRestrictions().get()
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
                shouldUpdateMangaUseCase(manga, restrictions, mangaTracks) == null
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
