package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticResponseDto(val result: String, val statistics: Map<String, StatisticsDto>)

@Serializable
data class StatisticsDto(
    val rating: StatisticRatingDto? = null,
    val comments: StatisticCommentsDto? = null,
    val follows: Int? = null,
)

@Serializable data class StatisticRatingDto(val average: Double?, val bayesian: Double?)

@Serializable data class StatisticCommentsDto(val threadId: Int, val repliesCount: Int)
