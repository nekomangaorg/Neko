package org.nekomanga.presentation.screens.stats

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.data.database.models.MergeType
import org.nekomanga.R
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaStatus
import org.nekomanga.domain.manga.MangaType
import org.nekomanga.presentation.components.UiText

object StatsConstants {
    @Immutable
    data class SimpleState(
        val screenState: ScreenState = ScreenState.Loading,
        val mangaCount: Int = 0,
        val chapterCount: Int = 0,
        val readCount: Int = 0,
        val bookmarkCount: Int = 0,
        val unavailableCount: Int = 0,
        val trackedCount: Int = 0,
        val globalUpdateCount: Int = 0,
        val downloadCount: Int = 0,
        val tagCount: Int = 0,
        val mergeCounts: List<Pair<MergeType, Int>> = listOf(),
        val averageMangaRating: Double = 0.0,
        val averageUserRating: Double = 0.0,
        val trackerCount: Int = 0,
        val readDuration: String = "",
        val lastLibraryUpdate: String = "",
        val lastLibraryUpdateAttempt: String = "",
        val lastLibraryUpdateDuration: String = "",
        val simpleStatMetrics: List<SimpleStatMetricUiModel> = listOf(),
    )

    @Immutable
    data class SimpleStatMetricUiModel(
        val title: UiText,
        val formattedValue: String,
    )

    @Immutable
    data class DetailedState(
        val isLoading: Boolean = true,
        val manga: List<DetailedStatManga> = listOf(),
        val categories: List<String> = listOf(),
        val tags: List<String> = listOf(),
        val detailTagState: DetailedTagState = DetailedTagState(),
        val distribution: DetailedDistributionUiModel = DetailedDistributionUiModel(),
    )

    @Immutable
    data class DetailedDistributionUiModel(
        val categoryDistribution: DetailedStatDistribution = DetailedStatDistribution(),
        val typeDistribution: DetailedStatDistribution = DetailedStatDistribution(),
        val statusDistribution: DetailedStatDistribution = DetailedStatDistribution(),
        val contentRatingDistribution: DetailedStatDistribution = DetailedStatDistribution(),
        val tagDistribution: DetailedStatDistribution = DetailedStatDistribution(),
        val startYearDistribution: DetailedStatDistribution = DetailedStatDistribution(),
    )

    @Immutable
    data class DetailedStatDistribution(
        val totalCount: Int = 0,
        val totalChapters: Int = 0,
        val totalReadDuration: Long = 0L,
        val groups: List<DetailedStatGroup> = emptyList(),
    ) {
        fun sortedBy(sortType: Sort): List<DetailedStatGroup> {
            return when (sortType) {
                Sort.Entries -> groups.sortedByDescending { it.count }
                Sort.Chapters -> groups.sortedByDescending { it.readChapters }
                Sort.Duration -> groups.sortedByDescending { it.readDuration }
            }
        }
    }

    @Immutable
    data class DetailedStatGroup(
        val key: String,
        val count: Int,
        val readChapters: Int,
        val totalChapters: Int,
        val readDuration: Long,
    )

    enum class Sort(@param:StringRes val stringRes: Int) {
        Entries(R.string.most_entries),
        Chapters(R.string.chapters_read),
        Duration(R.string.read_duration),
    }

    @Immutable
    data class DetailedTagState(
        val totalReadDuration: Long = 0L,
        val totalChapters: Int = 0,
        val sortedTagPairs: List<Pair<String, List<DetailedStatManga>>> = listOf(),
    )

    @Immutable
    data class DetailedStatManga(
        val id: Long,
        val title: String,
        val status: MangaStatus,
        val contentRating: MangaContentRating,
        val type: MangaType,
        val totalChapters: Int,
        val readChapters: Int,
        val bookmarkedChapters: Int,
        val unavailableChapters: Int,
        val readDuration: Long,
        val rating: Double?,
        val userScore: Double?,
        val startYear: Int?,
        val trackers: List<String>,
        val tags: List<String>,
        val categories: List<String>,
    )

    sealed class ScreenState {
        object Loading : ScreenState()

        object Simple : ScreenState()

        object Detailed : ScreenState()

        object NoResults : ScreenState()
    }

    @Immutable data class StatusDistribution(val status: MangaStatus, val distribution: Int)

    @Immutable
    data class ContentRatingDistribution(val rating: MangaContentRating, val distribution: Int)
}
