package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class AtHomeDto(
    val baseUrl: String,
    val chapter: AtHomeChapterDto,
)

@Serializable
data class AtHomeChapterDto(
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>,
)

@Serializable
data class AtHomeImageReportDto(
    val url: String,
    val success: Boolean,
    val bytes: Long = 0,
    val cached: Boolean? = null,
    val duration: Long,
)
