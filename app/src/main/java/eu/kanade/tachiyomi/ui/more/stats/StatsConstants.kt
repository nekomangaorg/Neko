package eu.kanade.tachiyomi.ui.more.stats

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaStatus
import org.nekomanga.domain.manga.MangaType

object StatsConstants {
    data class SimpleState(
        val screenState: ScreenState = ScreenState.Loading,
        val mangaCount: Int = 0,
        val chapterCount: Int = 0,
        val readCount: Int = 0,
        val bookmarkCount: Int = 0,
        val trackedCount: Int = 0,
        val globalUpdateCount: Int = 0,
        val downloadCount: Int = 0,
        val tagCount: Int = 0,
        val mergeCount: Int = 0,
        val averageMangaRating: Double = 0.0,
        val averageUserRating: Double = 0.0,
        val trackerCount: Int = 0,
        val readDuration: String = "",
        val lastLibraryUpdate: String = "",
        val lastLibraryUpdateAttempt: String = "",
    )

    data class DetailedState(
        val isLoading: Boolean = true,
        val manga: ImmutableList<DetailedStatManga> = persistentListOf(),
        val categories: ImmutableList<String> = persistentListOf(),
        val tags: ImmutableList<String> = persistentListOf(),
        val detailTagState: DetailedTagState = DetailedTagState(),
    )

    data class DetailedTagState(
        val totalReadDuration: Long = 0L,
        val totalChapters: Int = 0,
        val sortedTagPairs: ImmutableList<Pair<String, ImmutableList<DetailedStatManga>>> =
            persistentListOf(),
    )

    data class DetailedStatManga(
        val id: Long,
        val title: String,
        val status: MangaStatus,
        val contentRating: MangaContentRating,
        val type: MangaType,
        val totalChapters: Int,
        val readChapters: Int,
        val bookmarkedChapters: Int,
        val readDuration: Long,
        val rating: Double?,
        val userScore: Double?,
        val startYear: Int?,
        val trackers: ImmutableList<String>,
        val tags: ImmutableList<String>,
        val categories: ImmutableList<String>,
    )

    sealed class ScreenState {
        object Loading : ScreenState()

        object Simple : ScreenState()

        object Detailed : ScreenState()

        object NoResults : ScreenState()
    }

    data class StatusDistribution(val status: MangaStatus, val distribution: Int)

    data class ContentRatingDistribution(val rating: MangaContentRating, val distribution: Int)
}
