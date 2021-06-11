package eu.kanade.tachiyomi.source.online.handlers.dto

import kotlinx.serialization.Serializable

@Serializable
data class AtHomeDto(
    val baseUrl: String,
)

@Serializable
data class ImageReportResultDto(
    val url: String,
    val success: Boolean,
    val bytes: Int?,
    val cached: Boolean,
    val duration: Long,
)
