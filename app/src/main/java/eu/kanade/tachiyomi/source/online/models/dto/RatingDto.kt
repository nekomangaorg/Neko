package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RatingResponseDto(
    val ratings: JsonElement,
)

@Serializable data class RatingDto(val rating: Int)
