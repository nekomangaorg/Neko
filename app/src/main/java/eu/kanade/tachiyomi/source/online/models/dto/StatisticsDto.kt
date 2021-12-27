package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticResponseDto(
    val result: String,
    val statistics: Map<String, StatisticsDto>,
)

@Serializable
data class StatisticsDto(
    val rating: RatingDto,
)

@Serializable
data class RatingDto(
    val average: Double?,
)