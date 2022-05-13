package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class MarkStatusDto(
    val chapterIdsRead: List<String> = emptyList(),
    val chapterIdsUnread: List<String> = emptyList(),
)