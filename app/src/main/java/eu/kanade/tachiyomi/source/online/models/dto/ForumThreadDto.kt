package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable data class ForumThreadResponseDto(val data: ForumThreadIdDto)

@Serializable data class ForumThreadIdDto(val id: Int)

@Serializable data class ForumThreadDto(val id: String, val type: String)
