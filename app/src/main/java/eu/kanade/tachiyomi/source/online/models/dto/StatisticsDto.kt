package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticResponseDto(
    val result: String,
    val statistics: Map<String, StatisticsDto>,
)

@Serializable
data class StatisticsDto(
    val rating: StatisticRatingDto,
    val follows: Int?,
)

@Serializable
data class StatisticRatingDto(
    val average: Double?,
)
