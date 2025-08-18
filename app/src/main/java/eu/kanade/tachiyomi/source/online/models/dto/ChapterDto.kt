package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable data class ChapterDto(val result: String, val data: ChapterDataDto)

@Serializable
data class ChapterListDto(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val result: String,
    val data: List<ChapterDataDto>,
)

@Serializable
data class ChapterDataDto(
    val id: String,
    val type: String,
    val attributes: ChapterAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class ChapterAttributesDto(
    val title: String?,
    val volume: String?,
    val chapter: String?,
    val translatedLanguage: String,
    val publishAt: String,
    val readableAt: String,
    val pages: Int,
    val externalUrl: String? = null,
    val isUnavailable: Boolean? = null,
)

@Serializable
data class GroupListDto(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val result: String,
    val data: List<GroupDto>,
)

@Serializable data class GroupDto(val id: String, val attributes: GroupAttributesDto)

@Serializable data class GroupAttributesDto(val name: String, val description: String?)

@Serializable
data class UserListDto(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val result: String,
    val data: List<UserDto>,
)

@Serializable data class UserResultDto(val result: String, val data: UserDto)

@Serializable data class UserDto(val id: String, val attributes: UserAttributesDto)

@Serializable data class UserAttributesDto(val username: String)
