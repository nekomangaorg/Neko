package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChapterDto(
    val result: String,
    val data: ChapterDataDto,
)

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
)

@Serializable
data class GroupListDto(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val result: String,
    val data: List<GroupDataDto>,
)

@Serializable
data class GroupDto(
    val result: String,
    val data: GroupDataDto,
)

@Serializable
data class GroupDataDto(
    val id: String,
    val attributes: GroupAttributesDto,
)

@Serializable
data class GroupAttributesDto(
    val name: String,
    val description: String?,
)
