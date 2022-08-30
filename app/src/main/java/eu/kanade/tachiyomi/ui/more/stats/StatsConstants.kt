package eu.kanade.tachiyomi.ui.more.stats

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

object StatsConstants {
    data class StatsState(
        val loading: Boolean,
        val mangaCount: Int = 0,
        val chapterCount: Int = 0,
        val readCount: Int = 0,
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
        val statusDistribution: ImmutableList<StatusDistribution> = persistentListOf(),
    )

    data class StatusDistribution(val status: Int, val distribution: Int)
}
